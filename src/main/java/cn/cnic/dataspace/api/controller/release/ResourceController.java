package cn.cnic.dataspace.api.controller.release;

import cn.cnic.dataspace.api.service.ResourceService;
import cn.cnic.dataspace.api.service.UserService;
import cn.cnic.dataspace.api.util.ResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Publishing Resource Management
 */
@RestController
@RequestMapping("resource")
@Api(tags = "发布资源管理")
public class ResourceController {

    @Autowired
    private ResourceService resourceService;

    @Autowired
    private UserService userService;

    @ApiOperation("空间资源列表")
    @GetMapping("/search")
    public ResponseResult<Object> resourceSearch(@RequestHeader("Authorization") String token, @RequestParam(name = "page", defaultValue = "1") int page, @RequestParam(name = "size", defaultValue = "10") int size, @RequestParam(name = "spaceId") String spaceId, @RequestParam(name = "resourceTitle", required = false) String resourceTitle) {
        return resourceService.resourceSearch(token, page, size, spaceId, resourceTitle);
    }

    @ApiOperation(value = "根据邮箱获取用户列表")
    @GetMapping("/find")
    public ResponseResult<Object> find(@RequestHeader("Authorization") String token, @RequestParam(name = "email", required = false) String email, @RequestParam(name = "spaceId") String spaceId) {
        return userService.find(token, email, spaceId);
    }
}
