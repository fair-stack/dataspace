package cn.cnic.dataspace.api.controller.space;

import cn.cnic.dataspace.api.service.space.PublicService;
import cn.cnic.dataspace.api.util.ResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * PublicController
 *
 * @author wangCc
 * @date 2021-11-17 20:31
 */
@RestController
@Api(tags = "公开空间接口")
@RequestMapping("/public")
public class PublicController {

    @Autowired
    private PublicService publicService;

    @GetMapping("/detail")
    public ResponseResult<Object> detail(String url) {
        return publicService.detail(url);
    }

    @GetMapping("/recent")
    public ResponseResult<Object> recent(String url) {
        return publicService.recent(url);
    }

    @GetMapping("/publish")
    public ResponseResult<Object> publish(String url) {
        return publicService.publish(url);
    }

    @GetMapping("/file")
    public void files(String url, HttpServletRequest request, final HttpServletResponse response) {
        publicService.cmd(url, "file", request, response);
    }

    @GetMapping("/open")
    public ResponseResult<Object> open(String spaceId, @RequestParam(name = "page", defaultValue = "1") Integer page, @RequestParam(name = "size", defaultValue = "10") Integer size, @RequestParam(name = "direction", defaultValue = "desc") String direction, @RequestParam(name = "sort", defaultValue = "createTime") String sort, HttpServletRequest request) {
        return publicService.open(spaceId, page, size, direction, sort, request);
    }

    // @GetMapping("/dl")
    // public void dl(String spaceId, HttpServletRequest request, final HttpServletResponse response) {
    // publicService.cmd(spaceId, "zipdl", request, response);
    // }
    @GetMapping("/ju")
    public ResponseResult<Object> judge(String url, HttpServletRequest request) {
        return publicService.judge(url, request);
    }

    @GetMapping("/gc")
    public ResponseResult<Map<String, Object>> getComponent(@RequestParam("url") String url, @RequestParam("hash") String hash) {
        return publicService.getComponent(url, hash);
    }

    @GetMapping("/pd")
    public ResponseResult<Object> previewData(@RequestParam("url") String url, @RequestParam("hash") String hash, @RequestParam("componentId") String componentId, HttpServletRequest request) {
        return publicService.previewData(url, hash, componentId, request);
    }

    @GetMapping("/fd")
    public ResponseResult<Object> getFileData(@RequestParam("url") String url, @RequestParam("hash") String hash, HttpServletRequest request) {
        return publicService.getFileData(url, hash, request);
    }
}
