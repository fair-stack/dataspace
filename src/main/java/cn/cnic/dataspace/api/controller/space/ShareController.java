package cn.cnic.dataspace.api.controller.space;

import cn.cnic.dataspace.api.service.space.ShareService;
import cn.cnic.dataspace.api.util.ResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * ShareController
 *
 * @author chl
 * @date 2022-03-24 15:31
 */
@RestController
@Api(tags = "分享管理")
@RequestMapping("/share")
public class ShareController {

    @Autowired
    private ShareService shareService;

    @ApiOperation("空间分享-创建分享链接")
    @GetMapping("/space.link")
    public ResponseResult<Object> createSpaceLink(@RequestHeader("Authorization") String token, String spaceId, String time, String way, @RequestParam(name = "password", required = false) String password, HttpServletRequest request) {
        return shareService.createLink(token, spaceId, null, time, way, "space", password, request);
    }

    @ApiOperation("文件分享-创建分享链接")
    @GetMapping("/file.link")
    public ResponseResult<Object> createFileLink(@RequestHeader("Authorization") String token, String spaceId, String fileHash, String time, String way, @RequestParam(name = "password", required = false) String password, HttpServletRequest request) {
        return shareService.createLink(token, spaceId, fileHash, time, way, "file", password, request);
    }

    @ApiOperation("分享链接列表")
    @GetMapping("/list")
    public ResponseResult<Object> list(@RequestHeader("Authorization") String token, @RequestParam(name = "page", defaultValue = "1") Integer page, @RequestParam(name = "size", defaultValue = "10") Integer size, @RequestParam(name = "content", required = false) String content, @RequestParam(name = "type", defaultValue = "all") String type) {
        return shareService.list(token, page, size, content, type);
    }

    @ApiOperation("设置分享链接失效")
    @GetMapping("/set")
    public ResponseResult<Object> set(@RequestHeader("Authorization") String token, String shareId) {
        return shareService.set(token, shareId);
    }

    @ApiOperation("校验文件分享密码")
    @GetMapping("/getFileInfo")
    public ResponseResult<Object> getFileInfo(String linkId) {
        return shareService.isPwd(linkId);
    }

    @ApiOperation("获取分享的文件列表")
    @GetMapping("/fileList")
    public ResponseResult<Object> fileList(String linkId, @RequestParam(name = "password", required = false) String password, HttpServletRequest request) {
        return shareService.fileList(linkId, password, request);
    }

    @ApiOperation("校验空间分享密码")
    @GetMapping("/space.info")
    public ResponseResult<Object> spaceInfo(String linkId) {
        return shareService.isPwd(linkId);
    }

    @ApiOperation("分享空间的详情信息")
    @GetMapping("/detail")
    public ResponseResult<Object> detail(String linkId, @RequestParam(name = "password", required = false) String password, HttpServletRequest request) {
        return shareService.detail(linkId, password, request);
    }

    @ApiOperation("近期动态")
    @GetMapping("/recent")
    public ResponseResult<Object> recent(String linkId, @RequestParam(name = "password", required = false) String password) {
        return shareService.recent(linkId, password);
    }

    @ApiOperation("数据集发布")
    @GetMapping("/publish")
    public ResponseResult<Object> publish(String linkId, @RequestParam(name = "password", required = false) String password) {
        return shareService.publish(linkId, password);
    }

    @GetMapping("/file")
    public void files(String linkId, @RequestParam(name = "password", required = false) String password, HttpServletRequest request, final HttpServletResponse response) {
        shareService.cmd(linkId, password, "file", request, response);
    }

    @GetMapping("/open")
    public ResponseResult<Object> open(String linkId, @RequestParam(name = "page", defaultValue = "1") Integer page, @RequestParam(name = "size", defaultValue = "10") Integer size, @RequestParam(name = "direction", defaultValue = "desc") String direction, @RequestParam(name = "sort", defaultValue = "createTime") String sort, @RequestParam(name = "password", required = false) String password, HttpServletRequest request) {
        return shareService.open(linkId, password, page, size, direction, sort, request);
    }

    @GetMapping("/gc")
    public ResponseResult<Map<String, Object>> getComponent(String linkId, @RequestParam(name = "password", required = false) String password, @RequestParam("hash") String hash) {
        return shareService.getComponent(linkId, password, hash);
    }

    @GetMapping("/pd")
    public ResponseResult<Object> previewData(String linkId, @RequestParam(name = "password", required = false) String password, @RequestParam("hash") String hash, @RequestParam("componentId") String componentId, HttpServletRequest request) {
        return shareService.previewData(linkId, password, hash, componentId, request);
    }

    @GetMapping("/fd")
    public ResponseResult<Object> getFileData(String linkId, @RequestParam(name = "password", required = false) String password, @RequestParam("hash") String hash, HttpServletRequest request) {
        return shareService.getFileData(linkId, password, hash, request);
    }
}
