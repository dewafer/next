var express = require('express');
var router = express.Router();
var driver = require('../lib/dbdriver');

var db = driver({
	// 数据库url
	url: 'http://localhost:5984/'
	// 数据库名称
	, db: 'memo'	
})

// 准备memo的数据库，如没有则创建
db.prepare();

// 使用showAllMemo方法列出所有memo文档
router.get('/', function(req, res){
	showAllMemo(res);
});

// 创建一个memo文档
router.post('/', function(req, res){
	console.log('memo:' + req.body.memo);
	// memo文档的格式，content指向内容，date指向当前日期
	var postMemo = { content: req.body.memo, date: new Date() };
	// 插入数据库
	db.insert(postMemo, function(){
		// 完成后列出所有memo文档
		showAllMemo(res);
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
function showAllMemo(res) {
	db.fetch(function(data) {
		console.log(data);
		var allMemos = [];
		// 把数据库中查到的data一行一行地加入到上面这个数组中去
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

		// 然后使用memo view来渲染
		res.render('memo', { memos: allMemos });
	});
}

module.exports = router;