package cn.cnic.dataspace.api.datax.admin.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.api.R;
import cn.cnic.dataspace.api.datax.admin.entity.JobRegistry;
import cn.cnic.dataspace.api.datax.admin.service.JobRegistryService;
import cn.cnic.dataspace.api.datax.admin.util.PageUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

/**
 * Created by  on 2019/11/17
 */
@RestController
@RequestMapping("/api/jobRegistry")
@Api(tags = "执行器资源监控")
public class JobRegistryController extends BaseController {

    @Autowired
    private JobRegistryService jobRegistryService;

    /**
     * Paging to query all data
     */
    @GetMapping
    @ApiOperation("分页查询所有数据")
    @ApiImplicitParams({ @ApiImplicitParam(paramType = "query", dataType = "String", name = "current", value = "当前页", defaultValue = "1", required = true), @ApiImplicitParam(paramType = "query", dataType = "String", name = "size", value = "一页大小", defaultValue = "10", required = true), @ApiImplicitParam(paramType = "query", dataType = "Boolean", name = "ifCount", value = "是否查询总数", defaultValue = "true"), @ApiImplicitParam(paramType = "query", dataType = "String", name = "ascs", value = "升序字段，多个用逗号分隔"), @ApiImplicitParam(paramType = "query", dataType = "String", name = "descs", value = "降序字段，多个用逗号分隔") })
    public R<IPage<JobRegistry>> selectAll() {
        BaseForm baseForm = new BaseForm();
        return success(this.jobRegistryService.page(baseForm.getPlusPagingQueryEntity(), pageQueryWrapperCustom(baseForm.getParameters())));
    }

    /**
     * Custom Query Assembly
     */
    protected QueryWrapper<JobRegistry> pageQueryWrapperCustom(Map<String, Object> map) {
        // Parameters related to mybatis plus pagination
        Map<String, Object> pageHelperParams = PageUtils.filterPageParams(map);
        // Filter null values and query related parameters by page
        Map<String, Object> columnQueryMap = PageUtils.filterColumnQueryParams(map);
        QueryWrapper<JobRegistry> queryWrapper = new QueryWrapper<>();
        // Sort operation
        pageHelperParams.forEach((k, v) -> {
            switch(k) {
                case "ascs":
                    queryWrapper.orderByAsc(StrUtil.toUnderlineCase(StrUtil.toString(v)));
                    break;
                case "descs":
                    queryWrapper.orderByDesc(StrUtil.toUnderlineCase(StrUtil.toString(v)));
                    break;
            }
        });
        // Traverse to assemble field query conditions
        columnQueryMap.forEach((k, v) -> {
            switch(k) {
                case "datasourceName":
                    queryWrapper.like(StrUtil.toUnderlineCase(k), v);
                    break;
                default:
                    queryWrapper.eq(StrUtil.toUnderlineCase(k), v);
            }
        });
        return queryWrapper;
    }
}
