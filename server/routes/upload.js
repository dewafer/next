/*
 * 该router处理上传及上传页面的显示。
 * 注意，该router在app.js中挂载在/upload下，所以请通过
 * http://localhost:3000/upload 访问
 * 在该router中使用的所有中间件及router的相对路径都是在/upload下
 */
var express = require('express');
var router = express.Router();
var multer = require('multer');
var serveIndex = require('serve-index');

// 上传需要用到multer中间件
router.use(multer({ dest: './public/uploads/'}));

// 列出文件的中间件
router.use('../uploads', serveIndex('./public/uploads/', {'icons': true}));

// 处理get，不显示上传结果。
router.get('/', function(req, res){
	res.render('upload', { uploaded: false });
});

// 处理post，打印log并显示上传后的结果。
router.post('/', function(req, res){
	console.log(req.body)
	console.log(req.files)
	res.render('upload', { 
		uploaded: true
		, uploadfilename: req.files.file.originalname
		, uploadfilepath: '/uploads/' + req.files.file.name
		, uploadfilesize: req.files.file.size + '字节'
	});
});

// export出去给大家用。
module.exports = router;