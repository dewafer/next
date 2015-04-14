var express = require('express');
var router = express.Router();
var driver = require('../lib/dbdriver');

var db = driver({
	// 数据库url
	url: 'http://localhost:5984/'
	// 数据库名称
	, db: 'memo'	
})

db.prepare();

router.get('/', function(req, res){
	showAllMemo(res);
});

router.post('/', function(req, res){
	console.log('memo:' + req.body.memo);
	var postMemo = { content: req.body.memo, date: new Date() };
	db.insert(postMemo, function(){
		showAllMemo(res);
	});
});

router.get('/delete/:id/:rev/', function(req, res){
	db.remove({ id: req.params.id, rev: req.params.rev }, function(){
		res.redirect('../../../');
	});
});

function showAllMemo(res) {
	db.fetch(function(data) {
		console.log(data);
		var allMemos = [];
		for(var i=0; i < data.rows.length; i++) {
			var m = { 
				content: data.rows[i].doc.content
				, date : new Date(data.rows[i].doc.date).toLocaleString()
				, id   : data.rows[i].doc._id
				, rev  : data.rows[i].doc._rev
			};
			allMemos.push(m);
		}

		console.log(allMemos);

		res.render('memo', { memos: allMemos });
	});
}

module.exports = router;