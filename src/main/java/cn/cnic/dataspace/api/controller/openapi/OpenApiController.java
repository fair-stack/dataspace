package cn.cnic.dataspace.api.controller.openapi;

import cn.cnic.dataspace.api.currentlimiting.Limit;
import cn.cnic.dataspace.api.exception.CommonException;
import cn.cnic.dataspace.api.model.open.RequestRegister;
import cn.cnic.dataspace.api.model.open.RequestSpace;
import cn.cnic.dataspace.api.service.AuthService;
import cn.cnic.dataspace.api.service.open.OpenService;
import cn.cnic.dataspace.api.util.ResponseResult;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@Api(tags = "开放接口")
@RequestMapping("/ds.open")
public class OpenApiController {

    /*Registration, login, space creation, and space entry details*/
    @Autowired
    private OpenService openService;

    @Autowired
    private AuthService authService;

    /**
     * User registration
     */
    @Limit(key = "register", permitsPerSecond = 5, timeout = 1000, msg = "当前请求过于频繁，请稍后再试...")
    @PostMapping(value = "/register")
    public ResponseResult<Object> openRegister(@RequestBody RequestRegister requestRegister) {
        return openService.openRegister(requestRegister);
    }

    /**
     * Space Creation
     */
    @Limit(key = "spaceCreate", permitsPerSecond = 5, timeout = 1000, msg = "当前请求过于频繁，请稍后再试...")
    @PostMapping(value = "/space/create")
    public ResponseResult<Object> spaceCreate(@RequestBody RequestSpace requestSpace) {
        return openService.spaceCreate(requestSpace);
    }

    /**
     * Automatically log in and jump to space details - hyperlink open
     */
    @Limit(key = "spaceDetails", permitsPerSecond = 5, timeout = 1000, msg = "当前请求过于频繁，请稍后再试...")
    @GetMapping(value = "/space/details")
    public void spaceDetails(String appId, String userId, String spaceId, String version, String timestamp, String sign, HttpServletResponse response) throws IOException {
        ResponseResult<Object> result = openService.validation(appId, userId, spaceId, version, timestamp, sign);
        if (result.getCode() == 0) {
            authService.spaceDetails(appId, userId, spaceId, response);
        } else {
            throw new CommonException((String) result.getMessage());
        }
        return;
    }

    /**
     * Detailed acquisition of all spatial data volumes
     */
    @Limit(key = "spaceDataList", permitsPerSecond = 5, timeout = 1000, msg = "当前请求过于频繁，请稍后再试...")
    @GetMapping(value = "/space/dataList")
    public ResponseResult<Object> spaceDataList(String appId, String version, String timestamp, String sign) {
        return openService.spaceDataList(appId, version, timestamp, sign);
    }

    /**
     * Obtain user information
     */
    @Limit(key = "userInfo", permitsPerSecond = 5, timeout = 1000, msg = "当前请求过于频繁，请稍后再试...")
    @GetMapping(value = "/userInfo")
    public ResponseResult<Object> userInfo(String appId, String version, String timestamp, String sign, String email) {
        return openService.userInfo(appId, version, timestamp, sign, email);
    }

    /**
     * Get space details
     */
    @Limit(key = "spaceInfo", permitsPerSecond = 5, timeout = 1000, msg = "当前请求过于频繁，请稍后再试...")
    @GetMapping(value = "/space/info")
    public ResponseResult<Object> spaceInfo(String appId, String version, String timestamp, String sign, String spaceId) {
        return openService.spaceInfo(appId, version, timestamp, sign, spaceId);
    }

    /**
     * Invite space members
     */
    @Limit(key = "spaceInvite", permitsPerSecond = 5, timeout = 1000, msg = "当前请求过于频繁，请稍后再试...")
    @GetMapping(value = "/space/invite")
    public ResponseResult<Object> spaceInvite(String appId, String version, String timestamp, String sign, String operaId, String spaceId, String userId, String role) {
        return openService.spaceInvite(appId, version, timestamp, sign, operaId, spaceId, userId, role);
    }
}
