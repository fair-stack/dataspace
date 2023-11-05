package cn.cnic.dataspace.api.controller.harvest;

import cn.cnic.dataspace.api.currentlimiting.Limit;
import cn.cnic.dataspace.api.model.harvest.FilePathRequest;
import cn.cnic.dataspace.api.service.harvest.HarvestService;
import cn.cnic.dataspace.api.service.space.ShareService;
import cn.cnic.dataspace.api.util.ResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Data intersection and harvesting - spatial (ftp)
 */
@RestController
@Api(tags = "数据汇交和收割-对外api")
@RequestMapping("/harvest")
public class ForeignController {

    @Autowired
    private HarvestService harvestService;

    @Autowired
    private ShareService shareService;

    @ApiOperation("校验是否有密码")
    @Limit(key = "isPwd", permitsPerSecond = 5, timeout = 1000, msg = "当前请求过于频繁，请稍后再试...")
    @GetMapping("/isPwd")
    public ResponseResult<Object> isPwd(String linkId) {
        return shareService.isPwd(linkId);
    }

    @ApiOperation("获取空间文件")
    @Limit(key = "cmd-file", permitsPerSecond = 5, timeout = 1000, msg = "当前请求过于频繁，请稍后再试...")
    @GetMapping("/file")
    public ResponseResult<Object> open(String linkId, @RequestParam(name = "password", required = false) String password, @RequestParam(name = "page", defaultValue = "1") Integer page, @RequestParam(name = "size", defaultValue = "10") Integer size, @RequestParam(name = "direction", defaultValue = "desc") String direction, @RequestParam(name = "sort", defaultValue = "createTime") String sort, HttpServletRequest request) {
        return shareService.open(linkId, password, page, size, direction, sort, request);
    }

    @ApiOperation("获取分享的文件列表")
    @Limit(key = "read-file", permitsPerSecond = 5, timeout = 1000, msg = "当前请求过于频繁，请稍后再试...")
    @GetMapping("/fileList")
    public ResponseResult<Object> fileList(String linkId, @RequestParam(name = "password", required = false) String password, HttpServletRequest request) {
        return shareService.fileList(linkId, password, request);
    }

    @ApiOperation("空间的详情信息")
    @Limit(key = "space-detail", permitsPerSecond = 5, timeout = 1000, msg = "当前请求过于频繁，请稍后再试...")
    @GetMapping("/detail")
    public ResponseResult<Object> detail(String linkId, @RequestParam(name = "password", required = false) String password, HttpServletRequest request) {
        return shareService.detail(linkId, password, request);
    }

    @ApiOperation("根据链接获取 ftp账户信息 下载地址")
    @PostMapping("/ftp")
    public ResponseResult<Object> getFtpInfo(@RequestBody FilePathRequest filePathRequest) {
        return harvestService.getFtpInfo(filePathRequest);
    }
}
