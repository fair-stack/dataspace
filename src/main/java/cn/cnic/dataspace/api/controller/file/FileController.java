package cn.cnic.dataspace.api.controller.file;

import cn.cnic.dataspace.api.currentlimiting.Limit;
import cn.cnic.dataspace.api.model.file.FileCheck;
import cn.cnic.dataspace.api.model.file.FileDelete;
import cn.cnic.dataspace.api.model.file.FileRequest;
import cn.cnic.dataspace.api.service.file.FileHandService;
import cn.cnic.dataspace.api.util.FileUtils;
import cn.cnic.dataspace.api.util.ResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

/**
 * File processing interface
 */
@RestController
@Api(tags = "文件处理接口")
@RequestMapping("/file")
public class FileController {

    @Autowired
    private FileHandService fileHandService;

    @ApiOperation("文件查询")
    @GetMapping("/cmd")
    public ResponseResult<Object> cmd(@RequestHeader("Authorization") String token, @RequestParam(name = "page", defaultValue = "1") Integer page, @RequestParam(name = "size", defaultValue = "10") Integer size, @RequestParam(name = "direction", defaultValue = "desc") String direction, @RequestParam(name = "sort", defaultValue = "createTime") String sort, @RequestParam(name = "target", required = false) String target, @RequestParam("spaceId") String spaceId) {
        return fileHandService.cmd(token, page, size, direction, sort, target, spaceId);
    }

    @ApiOperation("文件查询-不分页")
    @GetMapping("/cmd_to")
    public ResponseResult<Object> cmdTo(@RequestHeader("Authorization") String token, @RequestParam(name = "target", required = false) String target, @RequestParam("spaceId") String spaceId) {
        return fileHandService.cmdTo(token, target, spaceId);
    }

    @ApiOperation("空间文件检索")
    @GetMapping("/search")
    public ResponseResult<Object> search(@RequestHeader("Authorization") String token, @RequestParam(name = "q") String q, @RequestParam(name = "page", defaultValue = "1") Integer page, @RequestParam(name = "size", defaultValue = "10") Integer size, @RequestParam(name = "direction", defaultValue = "desc") String direction, @RequestParam(name = "sort", defaultValue = "createTime") String sort, @RequestParam(name = "spaceId") String spaceId, @RequestParam(name = "target", required = false) String target) {
        return fileHandService.search(token, spaceId, q, target, page, size, direction, sort);
    }

    @ApiOperation("获取所选文件夹层级目录")
    @GetMapping("/hierarchy")
    public ResponseResult<Object> hierarchy(@RequestHeader("Authorization") String token, @RequestParam(name = "spaceId") String spaceId, @RequestParam(name = "source") String source, @RequestParam(name = "target") String target) {
        return fileHandService.hierarchy(token, spaceId, source, target);
    }

    @ApiOperation("文件上传校验")
    @PostMapping("/upload/check")
    public ResponseResult<Object> uploadCheck(@RequestHeader("Authorization") String token, @Valid FileCheck fileCheck) {
        return fileHandService.uploadCheck(token, fileCheck);
    }

    @ApiOperation("文件分片上传")
    @PostMapping("/upload")
    public ResponseResult<Object> fileUpload(@RequestHeader("Authorization") String token, @Valid FileRequest fileRequest, @RequestParam("file") MultipartFile file) throws NoSuchAlgorithmException {
        return fileHandService.fileUpload(token, fileRequest, file);
    }

    @ApiOperation("文件分片校验")
    @GetMapping("/upload/ver")
    public ResponseResult<Object> uploadVer(@RequestHeader("Authorization") String token, @RequestParam("fileMd5") String fileMd5, @RequestParam("spaceId") String spaceId, @RequestParam("hash") String hash) {
        return fileHandService.uploadVer(token, fileMd5, spaceId, hash);
    }

    @ApiOperation("文件合并通知-分片都上传结束")
    @GetMapping("/upload/merge")
    public ResponseResult<Object> uploadMerge(@RequestHeader("Authorization") String token, @RequestParam("fileMd5") String fileMd5, @RequestParam("spaceId") String spaceId, @RequestParam("hash") String hash) {
        return fileHandService.uploadMerge(token, fileMd5, spaceId, hash);
    }

    @ApiOperation("文件-创建文件夹-层级")
    @PostMapping("/create")
    public ResponseResult<Object> createFolder(@RequestHeader("Authorization") String token, @RequestParam("spaceId") String spaceId, @RequestParam("hash") String hash, @RequestParam("path") String path) throws IOException {
        return fileHandService.createFolder(token, spaceId, hash, path);
    }

    @ApiOperation("文件-文件删除")
    @PostMapping("/delete")
    public ResponseResult<Object> delete(@RequestHeader("Authorization") String token, @RequestBody FileDelete fileDelete) {
        return fileHandService.delete(token, fileDelete);
    }

    @ApiOperation("文件下载")
    @Limit(key = "download", permitsPerSecond = 3, timeout = 1000, msg = "当前请求过于频繁，请稍后再试...")
    @GetMapping("/download")
    public void download(@RequestParam("hash") String hash, @RequestParam("spaceId") String spaceId, HttpServletRequest request, HttpServletResponse response) {
        fileHandService.download(hash, spaceId, request, response);
        return;
    }

    @ApiOperation("zip文件压缩")
    @Limit(key = "zipReduce", permitsPerSecond = 3, timeout = 1000, msg = "当前请求过于频繁，请稍后再试...")
    @GetMapping("/zip/reduce")
    public ResponseResult<Object> zipReduce(@RequestHeader("Authorization") String token, @RequestParam("hash") String hash, @RequestParam("spaceId") String spaceId, HttpServletResponse response) {
        return fileHandService.zipReduce(token, hash, spaceId, response);
    }

    @ApiOperation("zip文件解压")
    @Limit(key = "zipUnpack", permitsPerSecond = 3, timeout = 1000, msg = "当前请求过于频繁，请稍后再试...")
    @GetMapping("/zip/unpack")
    public ResponseResult<Object> zipUnpack(@RequestHeader("Authorization") String token, @RequestParam("hash") String hash, @RequestParam("spaceId") String spaceId, HttpServletResponse response) {
        return fileHandService.zipUnpack(token, hash, spaceId, response);
    }

    @ApiOperation("shp文件下载压缩包")
    @GetMapping("/shpFileDown")
    public void shpFileDown(@RequestHeader("Authorization") String token, @RequestParam("hash") String hash, @RequestParam("fileName") String fileName, @RequestParam("spaceId") String spaceId, HttpServletRequest request, HttpServletResponse response) throws UnsupportedEncodingException {
        fileHandService.shpFileDown(token, hash, fileName, spaceId, request, response);
        return;
    }

    @ApiOperation("shp文件下载")
    @GetMapping("/shpDown")
    public void shpDown(@RequestHeader("Authorization") String token, @RequestParam("hash") String hash, @RequestParam("spaceId") String spaceId, HttpServletResponse response) {
        fileHandService.shpDown(token, hash, spaceId, response);
        return;
    }

    @ApiOperation("获取文件夹大小")
    @GetMapping("/folderSize")
    public ResponseResult<Object> folderSize(@RequestHeader("Authorization") String token, @RequestParam("hash") String hash, @RequestParam("spaceId") String spaceId) {
        return fileHandService.folderSize(token, hash, spaceId);
    }
}
