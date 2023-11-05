package cn.cnic.dataspace.api.controller;

import cn.cnic.dataspace.api.service.HomeService;
import cn.cnic.dataspace.api.util.ResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpServletRequest;

/**
 * Home page management
 */
@RestController
@Api(tags = "home")
public class HomeController {

    @Autowired
    private HomeService homeService;

    // @Autowired
    // private ElfinderStorageService elfinderStorageService;
    @ApiOperation("底部空间列表")
    @GetMapping("/space.search")
    public ResponseResult<Object> spaceSearch(@RequestParam(name = "page", defaultValue = "1") int page, @RequestParam(name = "size", defaultValue = "10") int size, HttpServletRequest request) {
        return homeService.spaceSearch(page, size, request);
    }

    @ApiOperation("浏览当前受欢迎的空间")
    @GetMapping("/space.active")
    public ResponseResult<Object> spaceActive(HttpServletRequest request) {
        return homeService.spaceActive(request);
    }

    @ApiOperation("信息统计")
    @GetMapping("/information.statistics")
    public ResponseResult<Object> informationStatistics() {
        return homeService.informationStatistics();
    }

    @ApiOperation("最新最热空间")
    @GetMapping("/hotNew")
    public ResponseResult<Object> hotNew(HttpServletRequest request) {
        return homeService.hotWordsList(request);
    }

    @ApiOperation("空间tag统计")
    @GetMapping("/tag")
    public ResponseResult<Object> tagCount() {
        return homeService.tagCount();
    }

    @ApiOperation(value = "总中心账号管理-添加")
    @PostMapping("/set.acc")
    public ResponseResult<Object> setAcc(@RequestParam(name = "acc") String acc, @RequestParam(name = "pwd") String pwd, @RequestParam(name = "isOpen", defaultValue = "false") boolean isOpen) {
        return homeService.setAcc(acc, pwd, isOpen);
    }

    @PostMapping("/v.info")
    public ResponseResult<Object> versionInfo(@RequestParam(name = "code") String code, @RequestParam(name = "version") String version, @RequestParam(name = "details") String details) {
        return homeService.versionInfo(code, version, details);
    }
    // @ApiOperation ("hash conversion")
    // @GetMapping("/test")
    // public ResponseResult<Object> test(String spaceId,String hash,
    // HttpServletRequest request){
    // String spacePath;
    // try {
    // ElfinderStorage elfinderStorage = elfinderStorageService.getElfinderStorage(request, spaceId);
    // Target target = elfinderStorage.fromHash(hash);
    // spacePath = target.toString();
    // } catch (Exception e) {
    // e.printStackTrace();
    // return ResultUtil.errorInternational(messageInternational("FILE_SPACE_ERROR"));
    // }
    // System.out.println(spacePath);
    // return null;
    // }
}
