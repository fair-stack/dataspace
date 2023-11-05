package cn.cnic.dataspace.api.controller.space;

import cn.cnic.dataspace.api.service.space.SvnService;
import cn.cnic.dataspace.api.util.ResponseResult;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;

/**
 * SvnController
 *
 * @author wangCc
 * @date 2021-08-26 19:12
 */
@RestController
@RequestMapping("/space")
public class SvnController {

    @Autowired
    private SvnService svnService;

    @PostMapping("/addPush")
    @ApiOperation("版本提交--直接提交")
    public ResponseResult<Object> addPush(@RequestHeader("Authorization") String token, String spaceId, String description) {
        return svnService.addPush(token, spaceId, description);
    }

    /*@PostMapping ("/pushLog")*/
    @PostMapping("/push")
    @ApiOperation("空间版本提交")
    public ResponseResult<Long> push(@RequestHeader("Authorization") String token, String spaceId, String description) {
        return svnService.push(token, spaceId, description);
    }

    @GetMapping("/prePublish")
    @ApiOperation("空间版本提交变更文件")
    public ResponseResult<Object> prePublish(@RequestHeader("Authorization") String token, String spaceId) {
        return svnService.prePublish(token, spaceId);
    }

    @GetMapping("/version")
    @ApiOperation("空间版本回滚")
    public ResponseResult<Object> version(@RequestHeader("Authorization") String token, String spaceId, String version) {
        return svnService.rollback(token, spaceId, version);
    }

    @GetMapping("/undoAdd")
    @ApiOperation("撤销添加")
    public ResponseResult<Object> undoAdd(@RequestHeader("Authorization") String token, String spaceId, String filePath) {
        return svnService.undoAdd(token, spaceId, filePath);
    }

    @GetMapping("/revert")
    @ApiOperation("文件版本回滚")
    public ResponseResult<Object> version(@RequestHeader("Authorization") String token, String spaceId, String... filePath) {
        return svnService.revert(token, spaceId, filePath);
    }

    @GetMapping("/svnChange")
    @ApiOperation("版本变更")
    public ResponseResult<Object> differ(HttpServletRequest request, String hash, String version) {
        return svnService.differ(request, hash, version);
    }

    @GetMapping("/svnCompare")
    @ApiOperation("版本对比")
    public ResponseResult<Object> compare(HttpServletRequest request, String hash, String compareVersion, String targetVersion) {
        return svnService.compare(request, hash, compareVersion, targetVersion);
    }

    @GetMapping("/svnLog")
    @ApiOperation("版本变更记录")
    public ResponseResult<Object> svnChangeLog(HttpServletRequest request, String hash) {
        return svnService.svnChangeLog(request, hash);
    }

    @GetMapping("/svnDifferLog")
    @ApiOperation("版本恢复受影响文件列表")
    public ResponseResult<Object> svnDifferLog(HttpServletRequest request, @RequestParam String spaceId, @RequestParam String revertVersion) {
        return svnService.svnDifferLog(request, spaceId, revertVersion);
    }

    @GetMapping("/update")
    @ApiOperation("svn update")
    public ResponseResult<Object> svnUpdate(@RequestHeader("Authorization") String token, @RequestParam String spaceId) {
        return svnService.svnUpdate(spaceId);
    }
}
