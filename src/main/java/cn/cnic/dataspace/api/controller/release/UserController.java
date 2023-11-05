package cn.cnic.dataspace.api.controller.release;

import cn.cnic.dataspace.api.model.user.ConsumerDTO;
import cn.cnic.dataspace.api.model.user.ConsumerInfoDTO;
import cn.cnic.dataspace.api.model.user.ManualAddList;
import cn.cnic.dataspace.api.service.UserService;
import cn.cnic.dataspace.api.util.ResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;

/**
 * User Management
 */
@RestController
@RequestMapping("user")
@Api(tags = "用户管理")
public class UserController {

    @Autowired
    private UserService userService;

    @ApiOperation(value = "注册")
    @PostMapping("/add")
    public ResponseResult<Object> add(@RequestBody ConsumerDTO consumerDTO, HttpServletRequest request) {
        return userService.add(consumerDTO, request);
    }

    @ApiOperation(value = "完善用户信息")
    @PostMapping("/update")
    public ResponseResult<Object> add(@RequestHeader("Authorization") String token, @RequestBody ConsumerInfoDTO consumerInfoDTO) {
        return userService.update(token, consumerInfoDTO);
    }

    @ApiOperation(value = "查询用户信息")
    @PostMapping("/query")
    public ResponseResult<Object> query(@RequestHeader("Authorization") String token) {
        return userService.query(token);
    }

    @ApiOperation(value = "获取设置邮箱接收列表")
    @GetMapping("/setEmail.list")
    public ResponseResult<Object> setEmailList(@RequestHeader("Authorization") String token) {
        return userService.setEmailList(token);
    }

    @ApiOperation(value = "配置邮箱接收/关闭 value (true/开启/false/关闭)")
    @GetMapping("/setEmail")
    public ResponseResult<Object> setEmail(@RequestHeader("Authorization") String token, @RequestParam("type") String type, @RequestParam(value = "value", defaultValue = "true") Boolean value) {
        return userService.setEmail(token, type, value);
    }

    @ApiOperation(value = "批量导入用户")
    @PostMapping("/import.user")
    public ResponseResult<Object> importUser(@RequestHeader("Authorization") String token, @RequestBody ManualAddList manualAddList) {
        return userService.importUser(token, manualAddList);
    }
}
