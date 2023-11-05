package cn.cnic.dataspace.api.controller.space;

import cn.cnic.dataspace.api.model.manage.ReleaseAccount;
import cn.cnic.dataspace.api.model.apply.Apply;
import cn.cnic.dataspace.api.model.manage.BasicSetting;
import cn.cnic.dataspace.api.model.manage.Identify;
import cn.cnic.dataspace.api.model.user.ManualAdd;
import cn.cnic.dataspace.api.model.user.RequestPaw;
import cn.cnic.dataspace.api.service.ExternalInterService;
import cn.cnic.dataspace.api.service.space.MessageService;
import cn.cnic.dataspace.api.service.space.SettingService;
import cn.cnic.dataspace.api.util.Constants;
import cn.cnic.dataspace.api.util.ResponseResult;
import cn.cnic.dataspace.api.util.ResultUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;

/**
 * SettingController
 *
 * @author wangCc
 * @date 2021-04-06 15:31
 */
@RestController
@Api(tags = "个人中心")
@RequestMapping("/setting")
public class SettingController {

    @Autowired
    private SettingService settingService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private ExternalInterService externalInterService;

    @GetMapping("/check")
    @ApiOperation("密码强度校验")
    public ResponseResult<Object> checkPassword(String password) {
        return settingService.checkPassword(password);
    }

    @GetMapping("/verification")
    @ApiOperation("安全设置-密码强度")
    public ResponseResult<Object> verification(@RequestHeader("Authorization") String token) {
        return settingService.verification(token);
    }

    @PostMapping("/updatePwd")
    @ApiOperation("安全设置-修改密码")
    public ResponseResult<Object> updatePwd(@RequestHeader("Authorization") String token, @RequestBody RequestPaw requestPaw) {
        return settingService.updatePwd(token, requestPaw);
    }

    @GetMapping("/spareEmail")
    @ApiOperation("安全设置-备用邮箱")
    public ResponseResult<Object> spareEmail(@RequestHeader("Authorization") String token) {
        return settingService.spareEmail(token);
    }

    @PostMapping("/setEmail")
    @ApiOperation("安全设置-设置备用邮箱")
    public ResponseResult<Object> setEmail(@RequestHeader("Authorization") String token, String spareEmail) {
        return settingService.setEmail(token, spareEmail);
    }

    @GetMapping("/apply")
    @ApiOperation("我的申请")
    @ApiImplicitParams({ @ApiImplicitParam(paramType = "query", name = "pageOffset", dataType = "String", required = true, value = "页码", defaultValue = "0"), @ApiImplicitParam(paramType = "query", name = "pageSize", dataType = "String", required = true, value = "行数", defaultValue = "10"), @ApiImplicitParam(paramType = "query", name = "type", dataType = "String", value = "类型：申请空间-空间扩容-加入空间"), @ApiImplicitParam(paramType = "query", name = "submitDateStart", dataType = "String", value = "提交时间开始"), @ApiImplicitParam(paramType = "query", name = "submitDateEnd", dataType = "String", value = "提交时间结束"), @ApiImplicitParam(paramType = "query", name = "description", dataType = "String") })
    public ResponseResult<Object> applyList(@RequestHeader("Authorization") String token, HttpServletRequest request) {
        return settingService.apply(token, request);
    }

    @ApiOperation("创建申请--扩容")
    @PostMapping("/cteApply")
    public ResponseResult<Object> createApply(@RequestHeader("Authorization") String token, @RequestBody Apply apply) {
        return settingService.createApply(token, apply);
    }

    @ApiOperation("空间申请扩容检查")
    @PostMapping("/applyCheck")
    public ResponseResult<Object> applyCheck(@RequestHeader("Authorization") String token, String spaceId) {
        return ResultUtil.success(settingService.applyCheck(token, spaceId));
    }

    @GetMapping("/approveList")
    @ApiOperation("我的审批")
    @ApiImplicitParams({ @ApiImplicitParam(paramType = "query", name = "pageOffset", dataType = "String", required = true, value = "页码", defaultValue = "0"), @ApiImplicitParam(paramType = "query", name = "pageSize", dataType = "String", required = true, value = "行数", defaultValue = "10"), @ApiImplicitParam(paramType = "query", name = "approvedStates", dataType = "String", value = "审批状态：待审批-已审批"), @ApiImplicitParam(paramType = "query", name = "type", dataType = "String", value = "类型：申请空间-空间扩容"), @ApiImplicitParam(paramType = "query", name = "applicant.personName", dataType = "String", value = "申请人"), @ApiImplicitParam(paramType = "query", name = "submitDateStart", dataType = "String", value = "提交时间开始"), @ApiImplicitParam(paramType = "query", name = "submitDateEnd", dataType = "String", value = "提交时间结束"), @ApiImplicitParam(paramType = "query", name = "description", dataType = "String", value = "检索内容") })
    public ResponseResult<Object> approveList(@RequestHeader("Authorization") String token, HttpServletRequest request) {
        return settingService.approveList(token, request);
    }

    @ApiOperation("审批操作")
    @PostMapping("/approve")
    @ApiImplicitParam(paramType = "query", name = "result", dataType = "String", required = true, value = "通过/不通过")
    public ResponseResult<Object> approve(@RequestHeader("Authorization") String token, String applyId, String result, String opinion, String size) {
        return settingService.approve(token, applyId, result, opinion, size);
    }

    @ApiOperation("用户消息")
    @GetMapping("/message")
    public ResponseResult<Object> message(@RequestHeader("Authorization") String token, @RequestParam(name = "pageOffset") String pageOffset, @RequestParam(name = "pageSize") String pageSize) {
        return messageService.message(token, pageOffset, pageSize);
    }

    @ApiOperation("未读消息数量")
    @GetMapping("/msgUnread")
    public ResponseResult<Object> msgUnread(@RequestHeader("Authorization") String token) {
        return messageService.msgUnread(token);
    }

    @ApiOperation("设置新消息已读")
    @GetMapping("/read")
    public ResponseResult<Object> read(@RequestHeader("Authorization") String token, String msgId) {
        return messageService.read(token, msgId);
    }

    @ApiOperation("第三方账号-微信绑定信息")
    @GetMapping("/we.conf")
    public ResponseResult<Object> weConf(@RequestHeader("Authorization") String token) {
        return settingService.weConf(token);
    }

    @ApiOperation("第三方账号-微信解除绑定")
    @GetMapping("/we.relieve")
    public ResponseResult<Object> weRelieve(@RequestHeader("Authorization") String token) {
        return settingService.weRelieve(token);
    }

    // ***********************
    @ApiOperation("基础配置")
    @GetMapping("/basic")
    public ResponseResult<Object> basic() {
        return settingService.basic();
    }

    @ApiOperation("修改基础配置")
    @PostMapping("/setBasic")
    public ResponseResult<Object> setBasic(@RequestHeader("Authorization") String token, @RequestBody BasicSetting basicSetting) {
        return settingService.setBasic(token, basicSetting);
    }

    // ***********************
    @ApiOperation("查询用户信息")
    @GetMapping("/userList")
    public ResponseResult<Object> userList(@RequestParam(value = "pageOffset", defaultValue = "1") int pageOffset, @RequestParam(value = "pageSize", defaultValue = "10") int pageSize, @RequestParam(value = "name", required = false) String name, @RequestParam(value = "email", required = false) String email, @RequestParam(value = "org", required = false) String org, @RequestParam(value = "role", required = false) String role) {
        return settingService.userList(pageOffset, pageSize, name, email, org, role);
    }

    @ApiOperation(value = "禁用/启用用户")
    @GetMapping("/disable")
    public ResponseResult<Object> disable(@RequestParam(value = "userId") String userId, @RequestParam(value = "disable") int disable) {
        return settingService.disable(userId, disable);
    }

    @ApiOperation(value = "用户添加")
    @PostMapping("/adminUserAdd")
    public ResponseResult<Object> adminUserAdd(@RequestHeader("Authorization") String token, @RequestBody ManualAdd manualAdd) {
        return settingService.adminUserAdd(token, manualAdd);
    }

    @ApiOperation(value = "用户修改")
    @PostMapping("/adminUserUpdate")
    public ResponseResult<Object> adminUserUpdate(@RequestHeader("Authorization") String token, @RequestBody ManualAdd manualAdd) {
        return settingService.adminUserUpdate(token, manualAdd);
    }

    @ApiOperation(value = "用户删除")
    @GetMapping("/del")
    public ResponseResult<Object> userDelete(@RequestHeader("Authorization") String token, String userId) {
        return settingService.userDelete(token, userId);
    }

    @ApiOperation(value = "角色列表")
    @GetMapping("/roleList")
    public ResponseResult<Object> roleList() {
        return settingService.roleList();
    }

    @ApiOperation(value = "识别邮箱后缀-添加/修改")
    @PostMapping("/identify.addOrUpdate")
    public ResponseResult<Object> identify(@RequestBody Identify identify) {
        return settingService.identify(identify);
    }

    @ApiOperation(value = "识别邮箱后缀-删除")
    @GetMapping("/identify.delete")
    public ResponseResult<Object> identifyDelete(@RequestParam(name = "id") String id) {
        return settingService.identifyDelete(id);
    }

    @ApiOperation(value = "邮箱后缀-查询")
    @PostMapping("/identify.query")
    public ResponseResult<Object> identifyQuery(@RequestParam(value = "pageOffset", defaultValue = "1") int pageOffset, @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        return settingService.identifyQuery(pageOffset, pageSize);
    }

    @ApiOperation(value = "发布账号管理-查询")
    @GetMapping("/releaseAccount.query")
    public ResponseResult<Object> releaseAccountQuery(@RequestParam(value = "pageOffset", defaultValue = "1") int pageOffset, @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        return settingService.releaseAccountQuery(pageOffset, pageSize);
    }

    @ApiOperation(value = "发布账号管理-添加/修改")
    @PostMapping("/releaseAccount.addOrUpdate")
    public ResponseResult<Object> releaseAccountAddOrUpdate(@RequestBody ReleaseAccount releaseAccount) {
        return settingService.releaseAccountAddOrUpdate(releaseAccount);
    }

    @ApiOperation(value = "发布账号管理-删除")
    @GetMapping("/releaseAccount.delete")
    public ResponseResult<Object> releaseAccountDelete(@RequestParam(value = "id") String id) {
        return settingService.releaseAccountDelete(id);
    }

    @ApiOperation(value = "科技云账户密码校验")
    @GetMapping("/umt.check")
    public ResponseResult<Object> umtCheck(@RequestHeader("Authorization") String token) {
        return settingService.umtCheck(token);
    }

    @ApiOperation(value = "百度网盘配置校验")
    @GetMapping("/net.check")
    public ResponseResult<Object> netCheck() {
        return settingService.netCheck();
    }

    @ApiOperation(value = "科技云、微信等配置校验")
    @GetMapping("/umt.ck")
    public ResponseResult<Object> umtCk() {
        return settingService.umtCk();
    }

    @ApiOperation(value = "总中心账号管理-获取")
    @GetMapping("/set.accQuery")
    public ResponseResult<Object> setAccQuery() {
        return settingService.setAccQuery();
    }

    @ApiOperation(value = "总中心账号管理-更改状态")
    @GetMapping("/set.accType")
    public ResponseResult<Object> setAccType(@RequestParam(name = "id") String id, @RequestParam(name = "isOpen", defaultValue = "false") boolean isOpen) {
        return settingService.setAccType(id, isOpen);
    }

    @ApiOperation(value = "总中心账号管理-添加")
    @PostMapping("/set.acc")
    public ResponseResult<Object> setAcc(@RequestParam(name = "acc") String acc, @RequestParam(name = "pwd") String pwd, @RequestParam(name = "isOpen", defaultValue = "false") boolean isOpen) {
        return settingService.setAcc(acc, pwd, isOpen);
    }

    @ApiOperation(value = "科技云配置-获取")
    @GetMapping("/umt.get")
    public ResponseResult<Object> umtGet() {
        return settingService.umtGet();
    }

    @ApiOperation(value = "科技云配置-更新/添加")
    @PostMapping("/umt.update")
    public ResponseResult<Object> umtUpdate(@RequestParam(name = "id", required = false) String id, @RequestParam(name = "appKey") String appKey, @RequestParam(name = "appSecret") String appSecret, @RequestParam(name = "page") String page, @RequestParam(name = "isOpen", defaultValue = "false") boolean isOpen) {
        return settingService.umtUpdate(id, appKey, appSecret, page, isOpen);
    }

    @ApiOperation(value = "百度网盘配置-获取")
    @GetMapping("/net.get")
    public ResponseResult<Object> netGet() {
        return settingService.netGet();
    }

    @ApiOperation(value = "百度网盘配置-更新/添加")
    @PostMapping("/net.update")
    public ResponseResult<Object> netUpdate(@RequestParam(name = "id", required = false) String id, @RequestParam(name = "appKey") String appKey, @RequestParam(name = "secretKey") String appSecret, @RequestParam(name = "page") String page, @RequestParam(name = "isOpen", defaultValue = "false") boolean isOpen) {
        return settingService.netUpdate(id, appKey, appSecret, page, isOpen);
    }

    @ApiOperation(value = "微信配置-获取")
    @GetMapping("/wechat.get")
    public ResponseResult<Object> wechatGet() {
        return settingService.wechatGet();
    }

    @ApiOperation(value = "微信配置-更新/添加")
    @PostMapping("/wechat.update")
    public ResponseResult<Object> wechatUpdate(@RequestParam(name = "id", required = false) String id, @RequestParam(name = "appId") String appId, @RequestParam(name = "secretKey") String secretKey, @RequestParam(name = "page") String page, @RequestParam(name = "isOpen", defaultValue = "false") boolean isOpen) {
        return settingService.wechatUpdate(id, appId, secretKey, page, isOpen);
    }

    @ApiOperation(value = "共享网配置-获取")
    @GetMapping("/esc.get")
    public ResponseResult<Object> escGet() {
        return settingService.escGet();
    }

    @ApiOperation(value = "共享网-更新/添加")
    @PostMapping("/esc.update")
    public ResponseResult<Object> escUpdate(@RequestParam(name = "id", required = false) String id, @RequestParam(name = "clientId") String clientId, @RequestParam(name = "clientSecret") String clientSecret, @RequestParam(name = "page") String page, @RequestParam(name = "isOpen", defaultValue = "false") boolean isOpen) {
        return settingService.escUpdate(id, clientId, clientSecret, page, isOpen);
    }

    // @ApiOperation (value="Change image settings")
    // @PostMapping("/picture")
    // public ResponseResult<Object> picture(MultipartFile blobAvatar) throws IOException {
    // return settingService.picture(blobAvatar);
    // }
    @ApiOperation(value = "获取用户详细")
    @GetMapping("/us.get")
    public ResponseResult<Object> usGet(String userId) {
        return settingService.usGet(userId);
    }

    @ApiOperation("获取授权机构列表")
    @GetMapping("/orgList")
    public ResponseResult<Object> accessOrgList(@RequestParam(value = "name", required = false) String name) {
        return externalInterService.accessOrgList(name, Constants.ADMIN);
    }
}
