package cn.cnic.dataspace.api.controller.space;

import cn.cnic.dataspace.api.model.release.RequestLn;
import cn.cnic.dataspace.api.model.backup.RequestAdd;
import cn.cnic.dataspace.api.model.space.FileData;
import cn.cnic.dataspace.api.model.space.Space;
import cn.cnic.dataspace.api.model.space.SpaceRoleRequest;
import cn.cnic.dataspace.api.model.space.child.AuthorizationPerson;
import cn.cnic.dataspace.api.model.space.child.SimpleSpace;
import cn.cnic.dataspace.api.service.space.SpaceService;
import cn.cnic.dataspace.api.util.ResponseResult;
import cn.cnic.dataspace.api.util.ResultUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * SpaceController
 *
 * @author wangCc
 * @date 2021-03-19 17:24
 */
@RestController
@Api(tags = "空间管理")
@RequestMapping("/space")
public class SpaceController {

    @Autowired
    private SpaceService spaceService;

    @ApiOperation("我的空间")
    @GetMapping("/self")
    @ApiImplicitParams({ @ApiImplicitParam(paramType = "query", name = "pageOffset", dataType = "String", required = true, defaultValue = "0"), @ApiImplicitParam(paramType = "query", name = "pageSize", dataType = "String", required = true, defaultValue = "10") })
    public ResponseResult<Map<String, Object>> spaceList(@RequestHeader("Authorization") String token, String spaceName, String pageOffset, String pageSize, String order) {
        return spaceService.spaceList(token, spaceName, pageOffset, pageSize, order);
    }

    @ApiOperation("更多空间")
    @GetMapping("/public")
    @ApiImplicitParams({ @ApiImplicitParam(paramType = "query", name = "pageOffset", dataType = "String", required = true, defaultValue = "0"), @ApiImplicitParam(paramType = "query", name = "pageSize", dataType = "String", required = true, defaultValue = "10") })
    public ResponseResult<Map<String, Object>> publicSpaceList(@RequestHeader("Authorization") String token, String pageOffset, String pageSize, String order) {
        return spaceService.publicSpaceList(token, pageOffset, pageSize, order);
    }

    @ApiOperation("空间标识转换")
    @GetMapping("/transit")
    public ResponseResult<Object> transit(String code) {
        return spaceService.transit(code);
    }

    @ApiOperation("空间已用容量")
    @GetMapping("/usageSize")
    public ResponseResult<Long> usageSize(@RequestHeader("Authorization") String token, String spaceId) {
        return spaceService.usageSize(token, spaceId);
    }

    @ApiOperation("扩容时候的默认大小")
    @GetMapping("/gb")
    public ResponseResult<Long> Gb(@RequestHeader("Authorization") String token) {
        return spaceService.getGb(token);
    }

    @ApiOperation("空间浏览次数")
    @GetMapping("/viewCount")
    public ResponseResult<Object> viewCount(String spaceId) {
        return spaceService.viewCount(spaceId);
    }

    @ApiOperation("空间文件下载次数")
    @GetMapping("/downloadCount")
    public ResponseResult<Object> downloadCount(String spaceId) {
        return spaceService.downloadCount(spaceId);
    }

    @ApiOperation("空间详情")
    @GetMapping("/detail")
    public ResponseResult<SimpleSpace> detail(@RequestHeader("Authorization") String token, String spaceId) {
        return spaceService.detail(token, spaceId);
    }

    @ApiOperation("空间成员")
    @GetMapping("/member")
    public ResponseResult<Set<AuthorizationPerson>> member(@RequestHeader("Authorization") String token, String spaceId) {
        return spaceService.member(token, spaceId);
    }

    @ApiOperation("创建空间")
    @PostMapping("/createSpace")
    public ResponseResult<Object> createSpace(@RequestHeader("Authorization") String token, @RequestBody Space space) {
        return spaceService.createSpace(token, space);
    }

    @ApiOperation("修改空间")
    @PostMapping("/updateSpace")
    public ResponseResult<Object> updateSpace(@RequestHeader("Authorization") String token, @RequestBody Space space) {
        return spaceService.updateSpace(token, space);
    }

    @ApiOperation("空间主页地址校验")
    @GetMapping("/urlCheck")
    public ResponseResult<Object> urlCheck(String spaceId, String url) {
        return ResultUtil.success(spaceService.urlCheck(spaceId, url));
    }

    @ApiOperation("修改空间管理员角色")
    @PostMapping("/updateSpaceRole")
    public ResponseResult<Object> updateSpaceRole(@RequestHeader("Authorization") String token, String spaceId, String userId, String role) {
        return spaceService.updateSpaceRole(token, spaceId, userId, role);
    }

    @ApiOperation("删除空间成员")
    @PostMapping("/deleteSpaceAdmin")
    public ResponseResult<Object> deleteSpaceAdmin(@RequestHeader("Authorization") String token, String spaceId, String userId, String role) {
        return spaceService.deleteSpaceMember(token, spaceId, userId, role);
    }

    @ApiOperation("删除空间")
    @PostMapping("/deleteSpace")
    public ResponseResult<Object> deleteSpace(@RequestHeader("Authorization") String token, String spaceId) {
        return spaceService.deleteSpace(token, spaceId);
    }

    @ApiOperation("发布新版本")
    @GetMapping("/publish")
    public ResponseResult<Object> publish(@RequestHeader("Authorization") String token, String spaceId) {
        return spaceService.publish(token, spaceId);
    }

    @ApiOperation("邀请用户")
    @PostMapping("/invite")
    public ResponseResult<Object> spaceInvite(@RequestHeader("Authorization") String token, String spaceId, String userId, String role) {
        return spaceService.spaceInvite(token, spaceId, userId, role);
    }

    @ApiOperation("邀请用户列表")
    @PostMapping("/userList")
    @ApiImplicitParams({ @ApiImplicitParam(paramType = "query", name = "pageOffset", dataType = "String", required = true, defaultValue = "0"), @ApiImplicitParam(paramType = "query", name = "pageSize", dataType = "String", required = true, defaultValue = "10") })
    public ResponseResult<List<Map<String, String>>> userList(String spaceId, String pageOffset, String pageSize) {
        return spaceService.userList(spaceId, pageOffset, pageSize);
    }

    @ApiOperation("空间挂载url")
    @GetMapping("/web")
    public ResponseResult<Object> spaceWeb(@RequestHeader("Authorization") String token, String spaceId) {
        return spaceService.spaceWeb(token, spaceId);
    }

    @ApiOperation("版本权限")
    @GetMapping("/publishAuth")
    public ResponseResult<Object> publishAuth(@RequestHeader("Authorization") String token, String spaceId) {
        return spaceService.publishAuth(token, spaceId);
    }

    @ApiOperation("空间日志")
    @GetMapping("/log")
    @ApiImplicitParams({ @ApiImplicitParam(paramType = "query", name = "pageOffset", dataType = "String", required = true, defaultValue = "0"), @ApiImplicitParam(paramType = "query", name = "pageSize", dataType = "String", required = true, defaultValue = "10") })
    public ResponseResult<Object> spaceLog(@RequestHeader("Authorization") String token, String spaceId, String actionType, String pageOffset, String pageSize, String content) {
        return spaceService.spaceLog(token, spaceId, actionType, pageOffset, pageSize, content);
    }

    @ApiOperation("活跃成员")
    @GetMapping("/activeMember")
    public ResponseResult<Object> activeMember(@RequestHeader("Authorization") String token, String spaceId) {
        return spaceService.activeMember(token, spaceId);
    }

    @ApiOperation("近期发布")
    @GetMapping("/recentPublish")
    public ResponseResult<Object> recentPublish(@RequestHeader("Authorization") String token, @RequestParam(value = "page", defaultValue = "1") String page, @RequestParam(value = "size", defaultValue = "10") String size, @RequestParam(value = "releaseName", required = false) String releaseName, @RequestParam(value = "spaceId") String spaceId) {
        return spaceService.recentPublish(token, page, size, releaseName, spaceId);
    }

    @ApiOperation("ftp访问地址")
    @GetMapping("/ftpUrl")
    public ResponseResult<Object> stpUrl(@RequestHeader("Authorization") String token, String spaceId) {
        return spaceService.ftpUrl(token, spaceId);
    }

    @ApiOperation("跨空间文件复制")
    @PostMapping("/cp.ln")
    public ResponseResult<Object> cpLn(@RequestHeader("Authorization") String token, @RequestBody RequestLn requestLn, HttpServletRequest request) {
        return spaceService.cpLn(token, requestLn, request);
    }

    @ApiOperation("获取文件预览需要的地址")
    @GetMapping("/previewUrl")
    public ResponseResult<Object> previewUrl(@RequestParam(name = "spaceId", required = false) String spaceId, @RequestParam(name = "homeUrl", required = false) String homeUrl, String hash, HttpServletRequest request) {
        return spaceService.previewUrl(spaceId, homeUrl, hash, request);
    }

    @ApiOperation("导入空间列表")
    @GetMapping("/importList")
    public ResponseResult<Object> importSpaceList(@RequestHeader("Authorization") String token, String spaceId) {
        return spaceService.importSpaceList(token, spaceId);
    }

    @ApiOperation("空间申请加入信息展示获取")
    @GetMapping("/applyInfo")
    public ResponseResult<Object> applyInfo(@RequestHeader("Authorization") String token, String spaceId) {
        return spaceService.applyInfo(token, spaceId);
    }

    @ApiOperation("申请加入")
    @PostMapping("/applyJoin")
    public ResponseResult<Object> applyJoin(@RequestHeader("Authorization") String token, String spaceId, String reason) {
        return spaceService.applyJoin(token, spaceId, reason);
    }

    @ApiOperation("空间申请审批列表")
    @GetMapping("/applyList")
    public ResponseResult<Object> applyList(@RequestHeader("Authorization") String token, @RequestParam(name = "spaceId") String spaceId, @RequestParam(name = "state", defaultValue = "0") Integer state, @RequestParam(name = "page", defaultValue = "1") Integer page, @RequestParam(name = "size", defaultValue = "10") Integer size, @RequestParam(name = "applicant", required = false) String applicant, @RequestParam(name = "content", required = false) String content) {
        return spaceService.applyList(token, spaceId, state, page, size, applicant, content);
    }

    @ApiOperation("空间申请-审批")
    @PostMapping("/approve")
    public ResponseResult<Object> approve(@RequestHeader("Authorization") String token, String applyId, @RequestParam(name = "role", required = false) String role, String result, @RequestParam(name = "reason", required = false) String reason) {
        return spaceService.approve(token, applyId, role, result, reason);
    }

    @ApiOperation("空间备份校验")
    @GetMapping("/back-check")
    public ResponseResult<Object> backCheck() {
        return spaceService.backCheck();
    }

    @ApiOperation("空间备份获取")
    @PostMapping("/getRecovery")
    public ResponseResult<Object> getRecovery(@RequestHeader("Authorization") String token, @RequestParam("spaceId") String spaceId) {
        return spaceService.getRecovery(token, spaceId);
    }

    @ApiOperation("空间备份配置")
    @PostMapping("/recovery")
    public ResponseResult<Object> recovery(@RequestHeader("Authorization") String token, @RequestBody RequestAdd requestAdd) {
        return spaceService.recovery(token, requestAdd);
    }

    @ApiOperation("获取文件元数据")
    @GetMapping("/getFileData")
    public ResponseResult<Object> getFileData(@RequestHeader("Authorization") String token, @RequestParam("spaceId") String spaceId, @RequestParam("hash") String hash, HttpServletRequest request) {
        return spaceService.getFileData(token, spaceId, hash, request);
    }

    @ApiOperation("添加文件元数据")
    @PostMapping("/addFileData")
    public ResponseResult<Object> addFileData(@RequestHeader("Authorization") String token, HttpServletRequest request, @RequestBody FileData fileData) {
        return spaceService.addFileData(request, token, fileData);
    }

    @ApiOperation("空间用户权限列表")
    @GetMapping("/userSpaceRoles")
    public ResponseResult<Object> userSpaceRoles(@RequestHeader("Authorization") String token, @RequestParam("spaceId") String spaceId) {
        return spaceService.userSpaceRoles(token, spaceId);
    }

    @ApiOperation("空间权限菜单目录")
    @GetMapping("/menuList")
    public ResponseResult<Object> menuList(@RequestHeader("Authorization") String token, @RequestParam("spaceId") String spaceId, @RequestParam("spaceRole") String spaceRole) {
        return spaceService.menuList(token, spaceId, spaceRole);
    }

    @ApiOperation("设置空间角色权限")
    @PostMapping("/setSpaceRole")
    public ResponseResult<Object> setSpaceRole(@RequestHeader("Authorization") String token, @RequestBody List<SpaceRoleRequest> spaceRoleRequests) {
        return spaceService.setSpaceRole(token, spaceRoleRequests);
    }

    @ApiOperation("文件预览-获取组件接口")
    @GetMapping("/getComponent")
    public ResponseResult<Map<String, Object>> getComponent(@RequestHeader("Authorization") String token, @RequestParam("spaceId") String spaceId, @RequestParam("hash") String hash) {
        return spaceService.getComponent(token, spaceId, hash);
    }

    @ApiOperation("文件预览-组件预览数据")
    @GetMapping("/previewData")
    public ResponseResult<Object> previewData(@RequestHeader("Authorization") String token, @RequestParam("spaceId") String spaceId, @RequestParam("hash") String hash, @RequestParam("componentId") String componentId, HttpServletRequest request) {
        return spaceService.previewData(token, spaceId, hash, componentId, request);
    }

    @ApiOperation("空间日志下载-all")
    @GetMapping("/log.down")
    public void logDown(@RequestParam("spaceId") String spaceId, HttpServletRequest request, HttpServletResponse response) {
        spaceService.logDown(spaceId, request, response);
    }
}
