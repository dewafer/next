var request = require('request');

function dbdriver(options) {
	return new Driver(options);
}

function Driver(options){

	// this.username = options.username;
	// this.password = options.password;
	// this.host = options.host;
	// this.port = options.port;
	this.url = options.url;
	this.db = options.db
	this.dbrequest = request.defaults({
		  baseUrl: this.url
		, json: true
	});
	this.docIdKeys = ['id', '_id'];
	this.docRevKeys = ['rev', '_rev'];

}

function fetchDoc(id, fn, include_docs){
	var docid;
	var fetchOption = {};
	if (!id) {
		docid = '_all_docs';
		fetchOption.qs = { include_docs: include_docs};
	} else {
		docid = id;
	}
	this.dbrequest.get(
		  this.db + '/'+ docid
		, fetchOption
		, handleResponse(fn));
}

Driver.prototype.fetch = function(id, fn) {
	if(id instanceof Function){
		fn = id;
		id = null;
	} else if(id instanceof Object) {
		id = parseDocProp(id, this.docIdKeys);
	}
	fetchDoc.call(this, id, fn, true);
}
Driver.prototype.list = function(fn) {
	fetchDoc.call(this, null, fn, false);
}

function saveDoc(doc, fn){
	if(!doc) {
		throw new Error('嗨，你想干什么呢？');
	}

	var docid = parseDocProp(doc, this.docIdKeys);
	this.dbrequest.put(this.db + '/' + docid
		, { body: doc }
		, handleResponse(fn));
}

Driver.prototype.save = saveDoc;
Driver.prototype.insert = saveDoc;
Driver.prototype.update = saveDoc;

function removeDoc(doc, fn) {
	if(!doc) {
		throw new Error('嗨，你想干什么呢？');
	}

	var docid = parseDocProp(doc, this.docIdKeys);
	var docrev = parseDocProp(doc, this.docRevKeys);

	this.dbrequest.del(this.db + '/' + docid
		, { headers: { 'If-Match': docrev } }
		, handleResponse(fn));
}

Driver.prototype.remove = removeDoc;
Driver.prototype.delete = removeDoc;

function parseDocProp(doc, names) {

	for(var i in names) {
		var prop = doc[names[i]];
		if(prop){
			return prop;
		}
	}
	throw new Error('行行好给个' + names + '吧！');
}

Driver.prototype.prepare = function (next){
	var req = this.dbrequest;
	var self = this;
	req.head(self.db, function(err, res, body){
		if(err) {
			throw err;
		}		
		if(res.statusCode == 404){
			req.put(self.db, function(err, res, body) {
				if(err) {
					throw err;
				}
				if(res.statusCode == 201 && body["ok"] == true) {
					if(next)
						next();
				} else {
					throw new Error(body);
				}
			});
		} else if(res.statusCode == 200) {
			if(next)
				next();
		} else {
			throw new Error("Error status code:" + res.statusCode + " " + res.statusMessage);
		}
	});
}

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

module.exports = dbdriver;
