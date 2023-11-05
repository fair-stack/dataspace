package cn.cnic.dataspace.api.controller.release;

import cn.cnic.dataspace.api.service.ReleaseService;
import cn.cnic.dataspace.api.util.ResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Release Management
 */
@RestController
@Api(tags = "发布管理")
@RequestMapping("/release")
public class ReleaseController {

    @Autowired
    private ReleaseService releaseService;

    @ApiOperation("资源查询")
    @GetMapping("/search")
    public ResponseResult<Object> releaseSearch(@RequestHeader("Authorization") String token, @RequestParam(name = "page", defaultValue = "1") int page, @RequestParam(name = "size", defaultValue = "10") int size, @RequestParam(name = "state") int state, @RequestParam(name = "releaseName", required = false) String releaseName) {
        return releaseService.releaseSearch(token, page, size, state, releaseName);
    }

    @ApiOperation("资源发布数量统计")
    @GetMapping("/count")
    public ResponseResult<Object> releaseCount(@RequestHeader("Authorization") String token) {
        return releaseService.releaseCount(token);
    }

    @ApiOperation("草稿箱删除")
    @GetMapping("/resource.delete")
    public ResponseResult<Object> resourceDelete(@RequestHeader("Authorization") String token, @RequestParam(name = "id") String id) {
        return releaseService.resourceDelete(token, id);
    }

    // @ApiOperation ("Template Resolution")
    // @GetMapping("/template.parsing")
    // public ResponseResult<Object> templateParsing(@RequestParam(name = "orgId",required = false) String orgId,
    // @RequestParam(name = "url",required = false) String url){
    // return releaseService.templateParsing(orgId,url);
    // }
    @ApiOperation("获取版本")
    @GetMapping("/getVersion")
    public ResponseResult<Object> getVersion(@RequestParam(name = "resourceId", required = false) String resourceId) {
        return releaseService.getVersion(resourceId);
    }

    @ApiOperation("学科领域")
    @GetMapping("/getSubjectList")
    public ResponseResult<Object> getSubjectList(@RequestParam(name = "param", required = false) String param) {
        return releaseService.getSubjectList(param);
    }

    @ApiOperation("判断发布机构是否有发布账号")
    @GetMapping("judge.release")
    public ResponseResult<Object> judgeRelease(@RequestParam(name = "orgId") String orgId) {
        return releaseService.judgeRelease(orgId);
    }
}
