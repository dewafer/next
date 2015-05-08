/*
 *
 * 这个API除了router.get('/:id/:file', ...)以外还没有用到。。。
 *
 */
var express = require('express');
var router = express.Router();
var driver = require('../lib/dbdriver');

var db = driver({
	// 数据库url
	url: driver.readDbConfigFromVCAP().url || 'http://localhost:5984/'
	// 数据库名称
	, db: 'images'	
})

// 准备images的数据库，如没有则创建
router.use(function(req, res, next){
	db.prepare(next);
});

router.get('/', function(req, res){
	db.fetch(function(data) {
		// console.log(data);
		var all = [];
		// 把数据库中查到的data一行一行地加入到上面这个数组中去
		for(var i=0; i < data.rows.length; i++) {
			var m = { 
				  title   : data.rows[i].doc.title
				, file  : data.rows[i].doc.file
				, date   : new Date(data.rows[i].doc.date).toLocaleString()
				, id     : data.rows[i].doc._id
				, rev    : data.rows[i].doc._rev
				, _files : data.rows[i].doc._attachments
			};
			all.push(m);
		}

		// console.log(all);

		res.send(JSON.stringify(all));
	});
});

router.get('/:id', function(req, res) {
	db.fetch(req.params.id, function(data) {
		// console.log(data);
		var detail = {};

		if(data){
			detail.title = data.title;
			detail.file = data.file;
			detail.date = data.date;
			detail.id = data._id;
			detail.rev = data._rev;
			detail._files = data._attachments;
		}

		res.send(JSON.stringify(detail));
	});
});

router.get('/:id/_files', function(req, res) {
	db.fetch(req.params.id, function(data) {
		// console.log(data);
		res.send(JSON.stringify(data._attachments));
	});
});

router.get('/:id/:file', function(req, res) {
	db.download(req.params.id, req.params.file, res);
});


module.exports = router;