package cn.cnic.dataspace.api.controller;

import cn.cnic.dataspace.api.service.impl.SafetyServiceImpl;
import cn.cnic.dataspace.api.util.*;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;

/**
 * Behavior lifting
 */
@RestController
@RequestMapping("/safety")
public class SafetyController {

    @Autowired
    private SafetyServiceImpl safetyService;

    @ApiOperation("用户密码错误限制解禁")
    @GetMapping("/lift.pwd")
    public ResponseResult<Object> liftPwd(@RequestParam("email") String email, HttpServletRequest request) {
        return safetyService.liftPwd(email, request);
    }

    @ApiOperation("手动同步空间文件数据")
    @GetMapping("/syn")
    public ResponseResult<Object> synFile(@RequestHeader("Authorization") String token, @RequestParam("spaceId") String spaceId) {
        return safetyService.synFile(token, spaceId);
    }
}
