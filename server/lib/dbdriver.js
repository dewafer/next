var request = require('request');
var debug = require('debug')('server:dbdriver');

// Driver的工厂方法
function dbdriver(options) {
	return new Driver(options);
}

dbdriver.readDbConfigFromVCAP = function(){
	var config = {};
	if(process.env.VCAP_SERVICES) {
		var vcapServices = JSON.parse(process.env.VCAP_SERVICES);
		if(vcapServices.cloudantNoSQLDB) {
			config.host = vcapServices.cloudantNoSQLDB[0].credentials.host;
			config.port = vcapServices.cloudantNoSQLDB[0].credentials.port;
			config.user = vcapServices.cloudantNoSQLDB[0].credentials.username;
			config.password = vcapServices.cloudantNoSQLDB[0].credentials.password;
			config.url = vcapServices.cloudantNoSQLDB[0].credentials.url;
		}
		debug('dbdriver.readDbConfigFromVCAP Services: '+JSON.stringify(process.env.VCAP_SERVICES));
	}
	// console.log('config:' + JSON.stringify(config));
	return config;
}

// Driver构造器
function Driver(options){

	// TODO 以后将支持自定义下面这些选项
	// this.username = options.username;
	// this.password = options.password;
	// this.host = options.host;
	// this.port = options.port;

	// 暂时只用url
	this.url = options.url;
	// Database名称
	this.db = options.db
	// request的defaults方法设置默认值
	// 这样后面用到request就不用重复设置了
	this.dbrequest = request.defaults({
		  baseUrl: this.url
		, json: true
	});

	// id的关键字数组
	this.docIdKeys = ['id', '_id'];
	// rev的关键字数组
	this.docRevKeys = ['rev', '_rev'];
	// attachment的关键字数组
	this.docAttachmentKeys = ['file', 'filename'];

	this.docAttachmentTypeKeys = ['type', 'content-type'];

}

// 取文档的方法，这个方法将会应用在Driver的对象上
// 所以this指向Driver对象
// 参数说明：
//  id: 指定要取的文档的id
//  fn: 回调函数
//  include_docs: 列出所有文档时（id不指定）是否包含文档内容
function fetchDoc(id, fn, include_docs){
	var docid;
	var fetchOption = {};
	if (!id) {
		// 如果id不指定的话，使用_all_docs列出所有文档
		docid = '_all_docs';
		fetchOption.qs = { include_docs: include_docs};
	} else {
		docid = id;
	}
	// 发送request来获取数据
	this.dbrequest.get(
		  this.db + '/'+ docid
		, fetchOption
		, handleResponse(fn));
}

// Driver类的fetch方法
Driver.prototype.fetch = function(id, fn) {
	if(typeof(id) == "function"){
		// 如果没有指定id
		fn = id;
		id = null;
	} else if(typeof(id) == "object") {
		// 如果指定了对象的话，检查对象里是否有key
		id = parseDocProp(id, this.docIdKeys);
	}
	// 使用fetchDoc方法取值，应用Driver.this到fetchDoc里面去
	// 并且，如果fetch不指定id的话（取所有数据），返回的结果将包含文档内容
	fetchDoc.call(this, id, fn, true);
}
// Driver类的list方法
Driver.prototype.list = function(fn) {
	// 列出所有文档，但不包含文档内容
	fetchDoc.call(this, null, fn, false);
}

// 保存文档的方法
function saveDoc(doc, fn){
	if(!doc) {
		// 没有指定doc的时候出错
		throw new Error('嗨，你想干什么呢？');
	}

	try{
		// 检查doc对象内是否含有id
		var docid = parseDocProp(doc, this.docIdKeys);
		// 发送request保存文档
		this.dbrequest.put(this.db + '/' + docid
			, { body: doc }
			, handleResponse(fn));
	} catch(e) {
		if(e instanceof ParseDocPropError) {
			// 如果doc里面没有指定id的话
			// 也可以保存文档，将使用数据库自动生成的id
			this.dbrequest.post(this.db
			, { body: doc }
			, handleResponse(fn));
		} else {
			// 其他情况下就把例外抛出去
			throw e;
		}
	}
}

// Driver类中可以使用save、insert、update方法都行
Driver.prototype.save = saveDoc;
Driver.prototype.insert = saveDoc;
Driver.prototype.update = saveDoc;

// 删除文档的方法
function removeDoc(doc, fn) {
	if(!doc) {
		// 没有指定doc的时候出错
		throw new Error('嗨，你想干什么呢？');
	}

	// 如果指定了doc的话，检查doc对象内是否有id和rev
	// 更新数据库必须指定id和rev
	var docid = parseDocProp(doc, this.docIdKeys);
	var docrev = parseDocProp(doc, this.docRevKeys);

	// 发送del请求到数据库
	this.dbrequest.del(this.db + '/' + docid
		, { headers: { 'If-Match': docrev } }
		, handleResponse(fn));
}

// Driver类中可以使用remove和delete方法
Driver.prototype.remove = removeDoc;
Driver.prototype.delete = removeDoc;

// 检查并解析doc中是否有由names指定的key
// 如果没有将会抛出ParseDocPropError异常
function parseDocProp(doc, names) {

	if(doc instanceof Object) {
		for(var i in names) {
			var prop = doc[names[i]];
			if(prop){
				return prop;
			}
		}
	}
	throw new ParseDocPropError('行行好给个' + names + '吧！');
}

// ParseDocPropError异常的构造器
function ParseDocPropError(msg) {
	this.message = msg;
}
// ParseDocPropError异常，继承Error
ParseDocPropError.prototype = new Error();

// 准备数据库方法
// 如果这个方法被运行，将会发送一个请求到数据库
// 检查指定的数据库是否存在（由Driver.db指定）
// 如果不存在将会创建这个数据库。
Driver.prototype.prepare = function (next){
	var req = this.dbrequest;
	var self = this;
	// 发送head请求检查数据库是否存在
	req.head(self.db, function(err, res, body){
		if(err) {
			throw err;
		}
		if(res.statusCode == 404){
			// 如果不存在（404），创建这个数据库
			req.put(self.db, function(err, res, body) {
				if(err) {
					throw err;
				}
				if(res.statusCode == 201 && body["ok"] == true) {
					// 创建成功，执行回调方法
					if(next)
						next();
				} else {
					throw new Error(body);
				}
			});
		} else if(res.statusCode == 200) {
			// 如果已经存在，则直接执行回调方法
			if(next)
				next();
		} else {
			// 除404和200以外的返回值都抛出异常
			throw new Error("Error status code:" + res.statusCode + " " + res.statusMessage);
		}
	});
}

// 这个方法返回统一的function(err, res, body)
// 并且在这个统一的function中，如果有回调方法fn，则会执行
// 如果没有，则判断是否有err，如有，则抛出
// 专门用来创建dbrequest的回调方法
function handleResponse(fn) {
	return function (err, res, body) {
		if(fn) {
			fn(body, res, err);
		} else {
			if(err) {
				throw err;
			}
		}
	}
}

function downloadAttachment(doc, pipeTo, fn) {
	var docid = parseDocProp(doc, this.docIdKeys);
	var docrev;
	var attname = parseDocProp(doc, this.docAttachmentKeys);
	var reqHeaders = {};

	try {
		docrev = parseDocProp(doc, this.docRevKeys);
	} catch(e) {
		if(e instanceof ParseDocPropError) {
			// do nothing
		} else {
			// 其他情况下就把例外抛出去
			throw e;
		}
	}

	if(docrev) {
		reqHeaders['If-Match'] = docrev;
	}

	var callback;
	if(fn) {
		callback = fn;
	} else if(pipeTo instanceof Function) {
		callback = pipeTo;
	}

	// 发送get请求到数据库
	var req = this.dbrequest.get(this.db + '/' + docid + '/' + attname
		, { headers: reqHeaders }, handleResponse(callback));

	// duck typing
	if(pipeTo && typeof(pipeTo) == 'object' && pipeTo.write) {
		req.pipe(pipeTo);
	}
}

Driver.prototype.download = function(id, filename, pipeTo, fn) {
	var doc = { id: id, filename: filename };
	downloadAttachment.call(this, doc, pipeTo, fn);
}
Driver.prototype._downloadAttachment = downloadAttachment;

function uploadAttachment(doc, pipeFrom, fn) {
	var docid = parseDocProp(doc, this.docIdKeys);
	var docrev = parseDocProp(doc, this.docRevKeys);;
	var attname = parseDocProp(doc, this.docAttachmentKeys);
	var atttype = parseDocProp(doc, this.docAttachmentTypeKeys);

	if(!pipeFrom || !pipeFrom.pipe) {
		throw new Error('请提供一个stream.Readable的对象！');
	}

	var req = this.dbrequest.put(this.db + '/' + docid + '/' + attname
		, { headers: { 'If-Match': docrev, 'Content-Type': atttype } }
		, handleResponse(fn));

	pipeFrom.pipe(req);
}

Driver.prototype.upload = function(id, rev, filename, filetype, readstream, fn) {
	var doc = { id: id, rev: rev, file: filename, type: filetype };
	uploadAttachment.call(this, doc, readstream, fn);
}
Driver.prototype._uploadAttachment = uploadAttachment;

// 将工厂方法export出去
module.exports = dbdriver;
