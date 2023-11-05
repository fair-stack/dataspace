package cn.cnic.dataspace.api.controller;

import cn.cnic.dataspace.api.model.user.Channel;
import cn.cnic.dataspace.api.service.AuthService;
import cn.cnic.dataspace.api.service.FileService;
import cn.cnic.dataspace.api.service.interaction.DSPublicService;
import cn.cnic.dataspace.api.util.*;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

/**
 * External callback interface
 */
@RestController
@Api(tags = "回调接口")
@Slf4j
public class CallbackController {

    @Autowired
    private AuthService authService;

    @Autowired
    private DSPublicService dsPublicService;

    @Autowired
    private FileService fileService;

    /**
     * Audit result notification
     */
    @PostMapping(value = "/audit.callback")
    public ResponseResult<Object> releaseCallback(@RequestParam(name = "approvalStatus") String approvalStatus, @RequestParam(name = "reason", required = false) String reason, @RequestParam(name = "rejectApproval", required = false) String rejectApproval, @RequestParam(name = "resourceId") String resourceId, @RequestParam(name = "DOI", required = false) String DOI, @RequestParam(name = "CSTR", required = false) String CSTR, @RequestParam(name = "detailsUrl", required = false) String detailsUrl, @RequestParam(value = "file", required = false) MultipartFile file) {
        return dsPublicService.releaseCallback(approvalStatus, reason, rejectApproval, resourceId, DOI, CSTR, detailsUrl, file);
    }

    @GetMapping("/email.activation")
    public void emailActivation(@RequestParam(name = "code") String code, HttpServletResponse response) throws IOException {
        authService.emailActivation(code, response);
        return;
    }

    @GetMapping("/ps.av")
    public void updatePwd(@RequestParam(name = "code") String code, HttpServletRequest request, HttpServletResponse response) throws IOException {
        authService.passActivation(code, request, response);
        return;
    }

    @GetMapping("/dwn/{code}")
    public void download(@PathVariable String code, HttpServletRequest request, HttpServletResponse response) {
        fileService.download(code, request, response);
    }

    /**
     * Technology Cloud Login Callback
     */
    @GetMapping("/ump/callback")
    public void umpCallback(HttpServletRequest request, HttpServletResponse response, @RequestParam(name = "code", required = false) String code) throws IOException {
        log.info("-- way:ump callback success --");
        if (StringUtils.isEmpty(code)) {
            response.sendError(500);
            response.addHeader("Content-Type", "application/json;charset=UTF-8");
            response.getOutputStream().print("param code error");
            response.flushBuffer();
            return;
        }
        authService.umtCallback(code, response);
        return;
    }

    /**
     * WeChat callback
     */
    @GetMapping("/wechat/callback")
    public void wechatCallback(HttpServletRequest request, HttpServletResponse response, @RequestParam(name = "code", required = false) String code, @RequestParam(name = "state", required = false) String state) throws IOException {
        log.info("-- wechat:ump callback success --");
        if (StringUtils.isEmpty(code) || StringUtils.isEmpty(state)) {
            response.sendError(500);
            response.addHeader("Content-Type", "application/json;charset=UTF-8");
            response.getOutputStream().print("param code error");
            response.flushBuffer();
            return;
        }
        authService.wechatCallback(code, state, request, response);
        return;
    }

    /**
     * Shared network callback
     */
    @GetMapping("/escience/callback")
    public void escCallback(HttpServletResponse response, @RequestParam(name = "code", required = false) String code) throws IOException {
        log.info("-- escience: callback success --");
        if (StringUtils.isEmpty(code)) {
            response.sendError(500);
            response.addHeader("Content-Type", "application/json;charset=UTF-8");
            response.getOutputStream().print("param code error");
            response.flushBuffer();
            return;
        }
        authService.escCallback(code, response);
        return;
    }

    /**
     * User synchronization
     */
    @PostMapping(value = "/channel")
    public ResponseResult<Object> channel(@RequestBody Channel channel) {
        return authService.channel(channel);
    }

    /**
     * Automatic login jump
     */
    @GetMapping(value = "/channel.log")
    public void channelLogin(String id, HttpServletResponse response) throws ServletException, IOException {
        authService.channelLogin(id, response);
        return;
    }
}
