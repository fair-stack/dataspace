package cn.cnic.dataspace.api.controller.network;

import cn.cnic.dataspace.api.model.network.FileImportRequest;
import cn.cnic.dataspace.api.service.network.FileTransferService;
import cn.cnic.dataspace.api.util.*;
import com.github.benmanes.caffeine.cache.Cache;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Network disk authentication
 */
@RestController
@Api(tags = "网盘登录认证")
@Slf4j
@RequestMapping("/network")
public class NetAuthController {

    @Autowired
    private FileTransferService fileTransferService;

    @Autowired
    private JwtTokenUtils jwtTokenUtils;

    private final Cache<String, String> network = CaffeineUtil.getThirdParty();

    @GetMapping("/auth")
    public void request(HttpServletRequest request, HttpServletResponse response) {
        fileTransferService.auth(request, response);
        return;
    }

    @RequestMapping("/callback")
    public void callback(HttpServletRequest request, HttpServletResponse response) {
        fileTransferService.callback(request, response);
        return;
    }

    @ApiOperation("网盘信息获取")
    @GetMapping("/userinfo")
    public ResponseResult<Object> userinfo(HttpServletRequest request) {
        return fileTransferService.userinfo(request);
    }

    @ApiOperation("当前用户验证")
    @GetMapping("/verify")
    public ResponseResult<Object> verify(HttpServletRequest request) {
        Token user = jwtTokenUtils.getToken(CommonUtils.getUser(request, Constants.TOKEN));
        String ifPresent = network.getIfPresent(Constants.LoginWay.NETWORK + user.getEmailAccounts());
        return ResultUtil.success(ifPresent != null);
    }

    @ApiOperation("文件获取")
    @GetMapping("/getFileList")
    public ResponseResult<Object> fileList(@RequestParam(name = "path", required = false) String path, HttpServletRequest request) {
        return fileTransferService.fileList(request, path);
    }

    @ApiOperation("导入")
    @PostMapping("/import")
    public ResponseResult<Object> networkImport(@RequestBody FileImportRequest fileImportRequest, HttpServletRequest request) {
        return fileTransferService.networkImport(request, fileImportRequest);
    }
}
