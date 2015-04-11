var express = require('express');
var router = express.Router();

/* GET home page. */
router.get('/', function(req, res, next) {
  res.render('index', { title: 'Express' });
});

// 根据后缀名自动判断引擎
// 因为不是默认引擎，所以必须指定后缀名，嗯。
router.get('/test1.html', function(req, res, next) {
  res.render('test1.html', { value: 'this is value' });
});

module.exports = router;
