package cn.cnic.dataspace.api.controller.harvest;

import cn.cnic.dataspace.api.model.harvest.TaskImpRequest;
import cn.cnic.dataspace.api.service.harvest.HarvestService;
import cn.cnic.dataspace.api.util.ResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;

/**
 * Data intersection and harvesting - spatial (ftp)
 */
@RestController
@Api(tags = "数据汇交和收割-实时操作接口")
@RequestMapping("/harvest")
public class HarvestController {

    @Autowired
    private HarvestService harvestService;

    @ApiOperation("校验是否有密码")
    @GetMapping("/tran/isPwd")
    public ResponseResult<Object> isPwd(String linkId, String host) {
        return harvestService.spaceInfo(linkId, host);
    }

    @ApiOperation("获取空间文件")
    @GetMapping("/tran/file")
    public ResponseResult<Object> open(String linkId, String cmd, String target, String spaceId, @RequestParam(name = "page", defaultValue = "1") Integer page, @RequestParam(name = "size", defaultValue = "10") Integer size, @RequestParam(name = "direction", defaultValue = "desc") String direction, @RequestParam(name = "sort", defaultValue = "createTime") String sort, @RequestParam(name = "password", required = false) String password, String host) {
        return harvestService.cmd(linkId, cmd, target, spaceId, password, host, page, size, direction, sort);
    }

    @ApiOperation("获取分享的文件列表")
    @GetMapping("/tran/fileList")
    public ResponseResult<Object> fileList(String linkId, @RequestParam(name = "password", required = false) String password, String host) {
        return harvestService.fileList(linkId, password, host);
    }

    @ApiOperation("空间的详情信息")
    @GetMapping("/tran/detail")
    public ResponseResult<Object> detail(String linkId, @RequestParam(name = "password", required = false) String password, String host) {
        return harvestService.detail(linkId, password, host);
    }

    @ApiOperation("发起任务-空间分享导入")
    @PostMapping("/task.imp")
    public ResponseResult<Object> taskSpaceImp(@RequestHeader("Authorization") String token, @RequestBody TaskImpRequest taskImpRequest) {
        return harvestService.taskSpaceImp(token, taskImpRequest);
    }

    @ApiOperation("主任务-重试失败任务传输")
    @GetMapping("/mainTask.retry")
    public ResponseResult<Object> mainTaskRetry(@RequestHeader("Authorization") String token, @RequestParam(name = "mainTaskId") String mainTaskId) {
        return harvestService.mainTaskRetry(token, mainTaskId);
    }

    @ApiOperation("子任务-重试失败任务传输-可多选")
    @PostMapping("/subtasks.retry")
    public ResponseResult<Object> subtasksRetry(@RequestHeader("Authorization") String token, @RequestParam(name = "mainTaskId") String mainTaskId, @RequestParam(name = "subtaskIds") String... subtaskIds) {
        return harvestService.subtasksRetry(token, mainTaskId, subtaskIds);
    }

    @ApiOperation("主任务列表")
    @GetMapping("/task.list")
    public ResponseResult<Object> taskList(@RequestHeader("Authorization") String token, @RequestParam(name = "spaceId", required = false) String spaceId, @RequestParam(name = "page", defaultValue = "1") Integer page, @RequestParam(name = "size", defaultValue = "10") Integer size) {
        return harvestService.taskList(token, spaceId, page, size);
    }

    @ApiOperation("下级任务列表")
    @GetMapping("/level.list")
    public ResponseResult<Object> levelTaskList(@RequestHeader("Authorization") String token, @RequestParam(name = "taskId") String taskId, @RequestParam(name = "state", defaultValue = "0") Integer state, @RequestParam(name = "page", defaultValue = "1") Integer page, @RequestParam(name = "size", defaultValue = "10") Integer size) {
        return harvestService.levelTaskList(token, taskId, state, page, size);
    }

    @ApiOperation("主任务删除")
    @PostMapping("/task.del")
    public ResponseResult<Object> taskDel(@RequestHeader("Authorization") String token, @RequestParam(name = "taskIds") String... taskIds) {
        return harvestService.taskDel(token, taskIds);
    }

    @ApiOperation("子任务删除")
    @PostMapping("/level.del")
    public ResponseResult<Object> levelDel(@RequestHeader("Authorization") String token, @RequestParam(name = "mainTaskId") String mainTaskId, @RequestParam(name = "taskIds") String... taskIds) {
        return harvestService.levelDel(token, mainTaskId, taskIds);
    }

    @ApiOperation("待处理任务数量")
    @GetMapping("/task.count")
    public ResponseResult<Object> taskCount(@RequestHeader("Authorization") String token, @RequestParam(name = "spaceId", required = false) String spaceId) {
        return harvestService.taskCount(token, spaceId);
    }
}
