package cn.cnic.dataspace.api.controller.space;

import cn.cnic.dataspace.api.model.manage.Component;
import cn.cnic.dataspace.api.model.backup.FtpHost;
import cn.cnic.dataspace.api.model.manage.ComponentUpdate;
import cn.cnic.dataspace.api.model.space.SpaceRoleRequest;
import cn.cnic.dataspace.api.model.open.Application;
import cn.cnic.dataspace.api.model.email.SysEmail;
import cn.cnic.dataspace.api.service.space.ManagementService;
import cn.cnic.dataspace.api.util.CommonUtils;
import cn.cnic.dataspace.api.util.ResponseResult;
import cn.cnic.dataspace.api.util.ResultUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

/**
 * management controller
 *
 * @author wangCc
 * @date 2021-10-09 15:47
 */
@RestController
@Api(tags = "系统管理")
@RequestMapping("/common")
public class ManagementController {

    private final ManagementService managementService;

    private static final String[] LANGUAGES = { "zh_CN", "en_US" };

    public ManagementController(ManagementService managementService) {
        this.managementService = managementService;
    }

    @GetMapping("/language")
    @ApiOperation("语言切换")
    public ResponseResult<Object> language(String lang) {
        if (Arrays.asList(LANGUAGES).contains(lang)) {
            return ResultUtil.success(CommonUtils.messageInternational("SWITCH_LANGUAGE"));
        } else {
            return ResultUtil.error("error");
        }
    }

    @GetMapping("/recent")
    @ApiOperation("最近浏览")
    public ResponseResult<Object> recent(@RequestHeader("Authorization") String token) {
        return managementService.recent(token);
    }

    @GetMapping("/searchAnywhere")
    @ApiOperation("检索")
    public ResponseResult<Object> searchAnywhere(@RequestHeader("Authorization") String token, String content) {
        return managementService.searchAnywhere(token, content);
    }

    @GetMapping("/homeIndex")
    @ApiOperation("空间主页")
    public ResponseResult<Object> homeIndex(String index) {
        return managementService.homeIndex(index);
    }

    @ApiOperation("所有空间-管理员权限")
    @GetMapping("/all")
    @ApiImplicitParams({ @ApiImplicitParam(paramType = "query", name = "pageOffset", dataType = "String", required = true, defaultValue = "0"), @ApiImplicitParam(paramType = "query", name = "pageSize", dataType = "String", required = true, defaultValue = "10"), @ApiImplicitParam(paramType = "query", name = "spaceName", dataType = "String"), @ApiImplicitParam(paramType = "query", name = "member", dataType = "String"), @ApiImplicitParam(paramType = "query", name = "tag", dataType = "String"), @ApiImplicitParam(paramType = "query", name = "start", dataType = "String"), @ApiImplicitParam(paramType = "query", name = "end", dataType = "String") })
    public ResponseResult<Object> spaceSelfList(@RequestHeader("Authorization") String token, String pageOffset, String pageSize, String order, String spaceName, String member, String tag, String start, String end) {
        return managementService.allSpace(token, pageOffset, pageSize, order, spaceName, member, tag, start, end);
    }

    @GetMapping("/member")
    @ApiOperation("判断是否是该空间的成员")
    public ResponseResult<Object> member(@RequestHeader("Authorization") String token, String spaceId) {
        return ResultUtil.success(managementService.member(token, spaceId));
    }

    @GetMapping("/role")
    @ApiOperation("管理员进入空间角色校验")
    public ResponseResult<Object> roleCheck(@RequestHeader("Authorization") String token, String spaceId) {
        return managementService.roleCheck(token, spaceId);
    }

    @GetMapping("/enterSpace")
    @ApiOperation("管理员进入空间变为高级用户")
    public ResponseResult<Object> enterSpace(@RequestHeader("Authorization") String token, String spaceId) {
        return managementService.enterSpace(token, spaceId);
    }

    @PostMapping("/upDown")
    @ApiOperation("空间上线下线")
    public ResponseResult<Object> upDown(@RequestHeader("Authorization") String token, String spaceId) {
        return managementService.upDown(token, spaceId);
    }

    @PostMapping("/sp.capacity")
    @ApiOperation("空间容量修改")
    public ResponseResult<Object> spaceCapacity(@RequestHeader("Authorization") String token, String spaceId, String capacity) {
        return managementService.spaceCapacity(token, spaceId, capacity);
    }

    @GetMapping("/pwdEmail")
    @ApiOperation("找回密码-发邮件")
    public ResponseResult<Object> pwdEmail(@RequestParam(name = "email") String email, @RequestParam(name = "name", required = false) String name, @RequestParam(name = "verificationCode", required = false) String verificationCode, HttpServletRequest request) {
        return managementService.pwdEmail(email, name, verificationCode, request);
    }

    @PostMapping("/restPwd")
    @ApiOperation("找回密码-更新密码")
    public ResponseResult<Object> restPwd(String pwd, String confirmPwd, HttpServletRequest request) {
        return managementService.restPwd(pwd, confirmPwd, request);
    }

    @GetMapping("/approve")
    @ApiOperation("审批配置")
    public ResponseResult<Object> approve(@RequestHeader("Authorization") String token) {
        return managementService.approve(token);
    }

    @PostMapping("/approved")
    @ApiOperation("修改审批配置")
    public ResponseResult<Object> approved(@RequestHeader("Authorization") String token, String approved, String gb) {
        return managementService.approved(token, approved, gb);
    }

    @GetMapping("/getSpaceRatio")
    @ApiOperation("空间扩容审批-使用量/容量")
    public ResponseResult<Object> getSpaceRatio(String spaceId) {
        return managementService.getSpaceRatio(spaceId);
    }

    @GetMapping("/svnControl")
    @ApiOperation("svn控制入口查询")
    public ResponseResult<Object> svnControl(@RequestHeader("Authorization") String token) {
        return managementService.svnControl(token);
    }

    @PostMapping("/updateSvn")
    @ApiOperation("svn控制入口修改")
    public ResponseResult<Object> updateSvn(@RequestHeader("Authorization") String token) {
        return managementService.updateSvn(token);
    }

    @ApiOperation("获取系统邮箱配置")
    @GetMapping("/getSysEmail")
    public ResponseResult<Object> getSysEmail(@RequestHeader("Authorization") String token) {
        return managementService.getSysEmail(token);
    }

    @ApiOperation("设置系统邮箱配置")
    @PostMapping("/setSysEmail")
    public ResponseResult<Object> setSysEmail(@RequestHeader("Authorization") String token, @RequestBody SysEmail sysEmail) {
        return managementService.setSysEmail(token, sysEmail);
    }

    @ApiOperation("测试邮件发送")
    @GetMapping("/testSend")
    public ResponseResult<Object> testSend(@RequestHeader("Authorization") String token, @RequestParam("email") String email) {
        return managementService.testSend(email);
    }

    @ApiOperation("配置空间总储存量")
    @GetMapping("/setSpace.to")
    public ResponseResult<Object> setSpaceTotal(@RequestHeader("Authorization") String token, @RequestParam("total") String total) {
        return managementService.setSpaceTotal(token, total);
    }

    @ApiOperation("系统使用储存明细")
    @GetMapping("/sys.det")
    public ResponseResult<Object> sysDet(@RequestHeader("Authorization") String token) {
        return managementService.sysDet(token, true);
    }

    @ApiOperation("获取灾备的ftp账号信息")
    @GetMapping("/getFtp")
    public ResponseResult<Object> getFtp(@RequestHeader("Authorization") String token) {
        return managementService.getFtp(token);
    }

    @ApiOperation("配置灾备的ftp账号信息")
    @PostMapping("/setFtp")
    public ResponseResult<Object> setFtp(@RequestHeader("Authorization") String token, @RequestBody FtpHost ftpHost) {
        return managementService.setFtp(token, ftpHost);
    }

    @ApiOperation("空间备份列表")
    @GetMapping("/recoveryList")
    public ResponseResult<Object> recoveryList(@RequestHeader("Authorization") String token, @RequestParam(value = "page", defaultValue = "1") Integer page, @RequestParam(value = "size", defaultValue = "10") Integer size, @RequestParam(value = "spaceName", required = false) String spaceName) {
        return managementService.recoveryList(token, page, size, spaceName);
    }

    @ApiOperation("空间备份-子任务列表")
    @GetMapping("/subtaskList")
    public ResponseResult<Object> subtaskList(@RequestHeader("Authorization") String token, @RequestParam(value = "page", defaultValue = "1") Integer page, @RequestParam(value = "size", defaultValue = "10") Integer size, @RequestParam(value = "jobId") String jobId, @RequestParam(value = "state", defaultValue = "all") String state, @RequestParam(value = "startTime", required = false) String startTime, @RequestParam(value = "endTime", required = false) String endTime) {
        return managementService.subtaskList(token, page, size, jobId, state, startTime, endTime);
    }

    @ApiOperation("空间统计-概览1-空间信息")
    @GetMapping("/statistics/preview/space")
    public ResponseResult<Object> statisticsPreviewSpace(@RequestHeader("Authorization") String token) {
        return managementService.statisticsPreview(token);
    }

    @ApiOperation("空间统计-概览2-用户信息")
    @GetMapping("/statistics/preview/user")
    public ResponseResult<Object> statisticsPreviewUser(@RequestHeader("Authorization") String token) {
        return managementService.statisticsPreviewUser(token);
    }

    @ApiOperation("空间统计-概览3456-数据量信息")
    @GetMapping("/statistics/preview/data")
    public ResponseResult<Object> statisticsPreviewData(@RequestHeader("Authorization") String token) {
        return managementService.statisticsPreviewData(token);
    }

    @ApiOperation("空间统计-空间排行")
    @GetMapping("/statistics/preview/top")
    public ResponseResult<Object> statisticsPreviewTop(@RequestHeader("Authorization") String token, @RequestParam(value = "sort", defaultValue = "personNum") String sort) {
        return managementService.statisticsPreviewTop(token, sort);
    }

    @ApiOperation("空间统计-空间增长")
    @GetMapping("/statistics/preview/growth")
    public ResponseResult<Object> statisticsPreviewGrowth(@RequestHeader("Authorization") String token, @RequestParam(value = "startTime", required = false) String startTime, @RequestParam(value = "endTime", required = false) String endTime, @RequestParam(value = "type", defaultValue = "0") int type) {
        return managementService.statisticsPreviewGrowth(token, startTime, endTime, type);
    }

    @ApiOperation("配置授权信息")
    @PostMapping("/setApp")
    public ResponseResult<Object> setApplication(@RequestHeader("Authorization") String token, @RequestBody Application application) {
        return managementService.setApplication(token, application);
    }

    @ApiOperation("授权信息列表")
    @GetMapping("/appList")
    public ResponseResult<Object> appList(@RequestHeader("Authorization") String token, @RequestParam(value = "page", defaultValue = "1") int page, @RequestParam(value = "size", defaultValue = "10") int size, @RequestParam(value = "state", defaultValue = "2") int state, @RequestParam(value = "name", required = false) String name, @RequestParam(value = "secret", required = false) String secret) {
        return managementService.appList(token, page, size, state, name, secret);
    }

    @ApiOperation("授权信息-删除")
    @GetMapping("/appDel")
    public ResponseResult<Object> appDelete(@RequestHeader("Authorization") String token, @RequestParam("id") String id) {
        return managementService.appDelete(token, id);
    }

    @ApiOperation("授权信息-禁用/启用")
    @GetMapping("/appDis")
    public ResponseResult<Object> appDis(@RequestHeader("Authorization") String token, @RequestParam("id") String id, @RequestParam("dis") Boolean dis) {
        return managementService.appDis(token, id, dis);
    }

    @ApiOperation("open_api列表")
    @GetMapping("/apiList")
    public ResponseResult<Object> apiList(@RequestHeader("Authorization") String token, @RequestParam(value = "page", defaultValue = "1") int page, @RequestParam(value = "size", defaultValue = "10") int size, @RequestParam(value = "state", defaultValue = "all") String state, @RequestParam(value = "apiName", required = false) String apiName, @RequestParam(value = "app", required = false) String app) {
        return managementService.apiList(token, page, size, state, apiName, app);
    }

    @ApiOperation("open_api下线/上线")
    @GetMapping("/setApiState")
    public ResponseResult<Object> setApiState(@RequestHeader("Authorization") String token, @RequestParam(value = "apiId") String apiId, @RequestParam(value = "state") String state) {
        return managementService.setApiState(token, apiId, state);
    }

    @ApiOperation("授权应用的下拉框数据")
    @GetMapping("/appSimple")
    public ResponseResult<Object> appSimple(@RequestHeader("Authorization") String token, @RequestParam(value = "app", required = false) String app) {
        return managementService.appSimple(token, app);
    }

    @ApiOperation("open_api 配置授权信息")
    @PostMapping("/setApiAuth")
    public ResponseResult<Object> setApiAuth(@RequestHeader("Authorization") String token, @RequestParam(value = "apiId") String apiId, @RequestParam(value = "authType") String authType, @RequestParam(value = "authTime", required = false) String authTime, @RequestParam(value = "appId") String appId, @RequestParam(value = "appName") String appName) {
        return managementService.setApiAuth(token, apiId, authType, authTime, appId, appName);
    }

    @ApiOperation("已安装市场组件列表")
    @GetMapping("/ist.com")
    public ResponseResult<Object> installList(@RequestHeader("Authorization") String token, @RequestParam(value = "page", defaultValue = "1") Integer page, @RequestParam(value = "size", defaultValue = "10") Integer size, @RequestParam(value = "category", required = false) String category, @RequestParam(value = "name", required = false) String name) {
        return managementService.installList(token, page, size, category, name);
    }

    @ApiOperation("获取市场组件列表")
    @GetMapping("/component")
    public ResponseResult<Object> component(@RequestHeader("Authorization") String token, @RequestParam(value = "page", defaultValue = "1") Integer page, @RequestParam(value = "size", defaultValue = "10") Integer size, @RequestParam(value = "sort", defaultValue = "0") Integer sort, @RequestParam(value = "category", required = false) String category, @RequestParam(value = "name", required = false) String name) {
        return managementService.component(token, page, size, sort, category, name);
    }

    @ApiOperation("市场组件左侧聚合统计数据")
    @GetMapping("/aggData")
    public ResponseResult<Object> aggData(@RequestHeader("Authorization") String token) {
        return managementService.aggData(token);
    }

    @ApiOperation("安装市场组件")
    @PostMapping("/component.install")
    public ResponseResult<Object> componentInstall(@RequestHeader("Authorization") String token, @RequestBody Component component) {
        return managementService.componentInstall(token, component);
    }

    @ApiOperation("编辑已安装市场组件配置信息")
    @PutMapping("/component.edit")
    public ResponseResult<Object> componentEdit(@RequestHeader("Authorization") String token, @RequestBody ComponentUpdate component) {
        return managementService.componentEdit(token, component);
    }

    @ApiOperation("移除已安装市场组件")
    @GetMapping("/component.rm")
    public ResponseResult<Object> componentRemove(@RequestHeader("Authorization") String token, @RequestParam(value = "id") String id) {
        return managementService.componentRemove(token, id);
    }

    @ApiOperation("获取空间默认权限配置")
    @GetMapping("/default.spaceRole")
    public ResponseResult<Object> defaultSpaceRole(@RequestHeader("Authorization") String token) {
        return managementService.defaultSpaceRole(token);
    }

    @ApiOperation("配置空间默认角色权限")
    @PostMapping("/setSpaceRole")
    public ResponseResult<Object> setSpaceRole(@RequestHeader("Authorization") String token, @RequestBody List<SpaceRoleRequest> spaceRoleRequests) {
        return managementService.setSpaceRole(token, spaceRoleRequests);
    }
}
