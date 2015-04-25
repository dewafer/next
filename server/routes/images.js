var express = require('express');
var router = express.Router();
var driver = require('../lib/dbdriver');
var multer = require('multer');
var fs = require('fs');

// 上传需要用到multer中间件
router.use(multer({ dest: './public/uploads/'}));

var db = driver({
	// 数据库url
	url: 'http://localhost:5984/'
	// 数据库名称
	, db: 'images'	
})

// 准备images的数据库，如没有则创建
db.prepare();

// 使用showAllImages方法列出所有memo文档
router.get('/', function(req, res){
	showAllImages(res);
});

// 创建一个image文档
router.post('/', function(req, res){

	console.log(req.files);

	if(!req.files.file){
		showAllImages(res);
		return;
	}

	// image文档的格式，date指向当前日期
	var image = { 
		title: req.body.imagetitle
		, date: new Date()
		, file: req.files.file.originalname 
	};

	if (!image.title) {
		image.title = image.file;
	}

	var type = req.files.file.mimetype;
	var inputStream = fs.createReadStream(req.files.file.path);
	// 插入数据库
	db.insert(image, function(data){
		var docid = data.id;
		var docrev = data.rev;

		db.upload(docid, docrev, image.file, type, inputStream, function(data){
			console.log("=uploaded=" );
			console.log(data);
			showAllImages(res);
		});
	});
});

// 删除指定memo的delete API
router.get('/delete/:id/:rev/', function(req, res){
	// 使用url中指定的id和rev来删除
	db.remove({ id: req.params.id, rev: req.params.rev }, function(){
		// 删除完（无论成功与否）跳转到（当前router的）根目录下
		res.redirect(req.baseUrl);
	});
});

// 列出所有memo的方法
function showAllImages(res) {
	db.fetch(function(data) {
		// console.log(data);
		var all = [];
		// 把数据库中查到的data一行一行地加入到上面这个数组中去
		for(var i=0; i < data.rows.length; i++) {
			var m = { 
				title   : data.rows[i].doc.title
				, date : new Date(data.rows[i].doc.date).toLocaleString()
				, id   : data.rows[i].doc._id
				, rev  : data.rows[i].doc._rev
				, file : data.rows[i].doc.file
			};
			all.push(m);
		}

		// console.log(all);

		// 然后使用memo view来渲染
		res.render('images', { images: all });
	});
}

module.exports = router;