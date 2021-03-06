var express = require('express');
var router = express.Router();
var debug = require('debug')('server:index');

/* GET home page. */
router.get('/', function(req, res, next) {
  debug('--index request--');
  res.render('index.ejs', { title: 'Express', meta_description: '一个能上传并浏览图片的，一点也不神奇的网站。', meta_author: 'dewafer@gmail.com' });
});

// 根据后缀名自动判断引擎
// 因为不是默认引擎，所以必须指定后缀名，嗯。
router.get('/test1.html', function(req, res, next) {
  res.render('test1.ejs', { value: 'this is value' });
});

router.get('/uploadslist', function(req, res, next) {
    res.render('uploadslist.ejs', { title: 'List of uploaded files.' });
});

module.exports = router;
