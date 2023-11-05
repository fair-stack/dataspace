package cn.cnic.dataspace.api.datax.admin.controller;

import cn.cnic.dataspace.api.datax.admin.dto.DataMappingTaskVO;
import cn.cnic.dataspace.api.datax.admin.service.DataMappingTaskService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.api.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Api(tags = "结构化数据任务相关")
@RestController
@RequestMapping("/datax/task")
public class DataMappingTaskController extends BaseController {

    @Resource
    private DataMappingTaskService dataMappingTaskService;

    @ApiOperation("分页查询空间下所有任务记录")
    @GetMapping("/getPaging")
    public R<IPage<DataMappingTaskVO>> getPaging(HttpServletRequest request, @RequestParam(required = false) Long dataMappingId, @RequestParam(required = false) Integer status, @RequestParam(required = false, defaultValue = "1") Integer current, @RequestParam(required = false, defaultValue = "10") Integer size) {
        String spaceId = getSpaceId(request);
        IPage<DataMappingTaskVO> result = dataMappingTaskService.getPaging(spaceId, dataMappingId, status, current, size);
        return success(result);
    }

    @ApiOperation("查询空间下所有任务记录")
    @GetMapping("/getList")
    public R<List<DataMappingTaskVO>> getList(HttpServletRequest request, @RequestParam(required = false) Long dataMappingId, @RequestParam(required = false) Integer status) {
        String spaceId = getSpaceId(request);
        List<DataMappingTaskVO> all = dataMappingTaskService.getAll(spaceId, dataMappingId, status);
        return success(all);
    }
}
