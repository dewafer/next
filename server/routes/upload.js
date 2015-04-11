/*
 * 该router处理上传及上传页面的显示。
 * 注意，该router在app.js中挂载在/upload下，所以请通过
 * http://localhost:3000/upload 访问
 * 在该router中使用的所有中间件及router的相对路径都是在/upload下
 */
var path = require('path');
var express = require('express');
var router = express.Router();
var multer = require('multer');

// 上传需要用到multer中间件
router.use(multer({ dest: './uploads/'}));
// 公开上传后的文件的路径到uploads目录下
router.use('/uploads', express.static(path.join(__dirname, '../uploads')));

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
		, uploadfilepath: req.baseUrl + '/' + req.files.file.path
		, uploadfilesize: req.files.file.size + '字节'
	});
});

// export出去给大家用。
module.exports = router;