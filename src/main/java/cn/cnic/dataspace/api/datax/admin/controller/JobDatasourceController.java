package cn.cnic.dataspace.api.datax.admin.controller;

import cn.cnic.dataspace.api.datax.admin.aop.HasSpacePermission;
import cn.cnic.dataspace.api.datax.admin.util.AESUtil;
import cn.cnic.dataspace.api.util.CommonUtils;
import cn.cnic.dataspace.api.util.RSAEncrypt;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.api.R;
import cn.cnic.dataspace.api.datax.admin.core.util.LocalCacheUtil;
import cn.cnic.dataspace.api.datax.admin.entity.JobDatasource;
import cn.cnic.dataspace.api.datax.admin.service.JobDatasourceService;
import com.baomidou.mybatisplus.extension.enums.ApiErrorCode;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

/**
 * Jdbc data source configuration controller layer
 */
@RestController
@RequestMapping("/jobJdbcDatasource")
@Api(tags = "数据源配置接口")
@HasSpacePermission
public class JobDatasourceController extends BaseController {

    /**
     * Service object
     */
    @Autowired
    private JobDatasourceService jobJdbcDatasourceService;

    /**
     * Paging to query all data
     */
    @GetMapping
    @ApiOperation("分页查询所有数据")
    @ApiImplicitParams({ @ApiImplicitParam(paramType = "query", dataType = "String", name = "current", value = "当前页", defaultValue = "1", required = true), @ApiImplicitParam(paramType = "query", dataType = "String", name = "size", value = "一页大小", defaultValue = "10", required = true), @ApiImplicitParam(paramType = "query", dataType = "String", name = "spaceId", value = "空间ID", required = true) })
    public R<IPage<JobDatasource>> selectAll(@RequestParam(value = "current", defaultValue = "1", required = false) Integer current, @RequestParam(value = "size", defaultValue = "10", required = false) Integer size, @RequestParam(value = "spaceId") String spaceId) {
        IPage<JobDatasource> datasourceIPage = new Page<>(current, size);
        QueryWrapper<JobDatasource> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id,datasource_name,datasource,space_id");
        queryWrapper.eq("space_id", spaceId);
        queryWrapper.orderByDesc("create_date");
        return R.ok(jobJdbcDatasourceService.page(datasourceIPage, queryWrapper));
    }

    /**
     * Get all data sources
     */
    @ApiOperation("获取所有数据源")
    @GetMapping("/all")
    public R<List<JobDatasource>> selectAllDatasource(HttpServletRequest request) {
        String spaceId = getSpaceId(request);
        return success(this.jobJdbcDatasourceService.selectAllDatasource(spaceId));
    }

    /**
     * Obtain recently used data sources
     */
    // @ApiOperation ("Get Recently Used Data Sources")
    // @GetMapping("/selectHotDatasource")
    public R<List<JobDatasource>> selectHotDatasource(HttpServletRequest request) {
        String spaceId = getSpaceId(request);
        return success(this.jobJdbcDatasourceService.selectHotDatasource(spaceId));
    }

    /**
     * Query single data through primary key
     */
    @ApiOperation("通过主键查询单条数据")
    @GetMapping("{id}")
    public R<JobDatasource> selectOne(@PathVariable Serializable id) {
        JobDatasource byId = this.jobJdbcDatasourceService.getById(id);
        byId.setJdbcUsername(AESUtil.decrypt(byId.getJdbcUsername()));
        byId.setJdbcPassword(null);
        return success(byId);
    }

    /**
     * New Data Source
     */
    @ApiOperation("新建数据源")
    @PostMapping
    public R<Boolean> insert(HttpServletRequest request, @RequestBody JobDatasource entity) {
        String spaceId = getSpaceId(request);
        entity.setSpaceId(spaceId);
        try {
            String first = RSAEncrypt.decrypt(entity.getJdbcPassword());
            if (first == null) {
                return R.failed(CommonUtils.messageInternational("param_error"));
            }
            entity.setJdbcPassword(first);
            this.jobJdbcDatasourceService.save(entity);
        } catch (DuplicateKeyException e) {
            logger.error(e.getMessage(), e);
            return R.failed(CommonUtils.messageInternational("datasource_name_duplicate"));
        }
        return success(true);
    }

    /**
     * Create a new data source and create a table
     */
    @ApiOperation("新建数据源并创建一张表")
    @PostMapping("/insertAndImport")
    public R<Boolean> insertAndImport(HttpServletRequest request, @RequestBody JobDatasource entity) throws IOException {
        String spaceId = getSpaceId(request);
        String userId = getCurrentUserId(request);
        entity.setSpaceId(spaceId);
        R<Boolean> r = checkParam(entity);
        if (r.getCode() != ApiErrorCode.SUCCESS.getCode()) {
            return r;
        }
        return this.jobJdbcDatasourceService.saveAndImport(spaceId, userId, entity);
    }

    /**
     * Data source editing
     */
    @PutMapping
    @ApiOperation("数据源编辑")
    public R<Boolean> update(@RequestBody JobDatasource entity) {
        if (StringUtils.isEmpty(entity.getDatasourceName())) {
            return R.failed(CommonUtils.messageInternational("datasource_name_null"));
        }
        if (StringUtils.isEmpty(entity.getDatasource())) {
            return R.failed(CommonUtils.messageInternational("datasource_type_null"));
        }
        if (StringUtils.isEmpty(entity.getJdbcUrl())) {
            return R.failed(CommonUtils.messageInternational("datasource_url_null"));
        }
        JobDatasource old = jobJdbcDatasourceService.getById(entity.getId());
        LocalCacheUtil.remove(old.getCacheKey());
        String first = RSAEncrypt.decrypt(entity.getJdbcPassword());
        if (first == null) {
            return R.failed(CommonUtils.messageInternational("param_error"));
        }
        entity.setJdbcPassword(first);
        // if (null != old.getJdbcUsername() && entity.getJdbcUsername().equals(old.getJdbcUsername())) {
        // entity.setJdbcUsername(null);
        // }
        // if (null != entity.getJdbcPassword() && entity.getJdbcPassword().equals(old.getJdbcPassword())) {
        // entity.setJdbcPassword(null);
        // }
        // Modifying type not allowed
        entity.setDatasource(null);
        try {
            this.jobJdbcDatasourceService.updateById(entity);
        } catch (DuplicateKeyException e) {
            logger.error(e.getMessage(), e);
            return R.failed(CommonUtils.messageInternational("datasource_name_duplicate"));
        }
        return success(true);
    }

    /**
     * Delete data source based on ID
     */
    @DeleteMapping
    @ApiOperation("根据id删除数据源")
    public R<Boolean> delete(Long id) {
        JobDatasource jobDatasource = jobJdbcDatasourceService.getById(id);
        LocalCacheUtil.remove(jobDatasource.getCacheKey());
        jobJdbcDatasourceService.removeById(jobDatasource.getId());
        return success(true);
    }

    /**
     * Test Data Source
     */
    @PostMapping("/test")
    @ApiOperation("测试数据")
    public R<Boolean> dataSourceTest(HttpServletRequest request, @RequestBody JobDatasource jobJdbcDatasource) throws IOException {
        String spaceId = getSpaceId(request);
        jobJdbcDatasource.setSpaceId(spaceId);
        R r = checkParam(jobJdbcDatasource);
        if (r.getCode() != ApiErrorCode.SUCCESS.getCode()) {
            return r;
        }
        String first = RSAEncrypt.decrypt(jobJdbcDatasource.getJdbcPassword());
        if (first == null) {
            return R.failed(CommonUtils.messageInternational("param_error"));
        }
        jobJdbcDatasource.setJdbcPassword(first);
        return success(jobJdbcDatasourceService.dataSourceTest(jobJdbcDatasource));
    }

    /**
     * Test Data Source
     */
    @PostMapping("/dataSourceTestAndReturnTables")
    @ApiOperation("测试数据源并返回该数据源下所有表")
    public R dataSourceTestAndReturnTables(HttpServletRequest request, @RequestBody JobDatasource jobJdbcDatasource) throws IOException {
        String spaceId = getSpaceId(request);
        jobJdbcDatasource.setSpaceId(spaceId);
        R r = checkParam(jobJdbcDatasource);
        if (r.getCode() != ApiErrorCode.SUCCESS.getCode()) {
            return r;
        }
        String first = RSAEncrypt.decrypt(jobJdbcDatasource.getJdbcPassword());
        if (first == null) {
            return R.failed(CommonUtils.messageInternational("param_error"));
        }
        jobJdbcDatasource.setJdbcPassword(first);
        return success(jobJdbcDatasourceService.dataSourceTestAndReturnTables(jobJdbcDatasource));
    }

    /**
     * Test Data Source
     */
    @PostMapping("/dataSourceTestAndReturnTablesAndDataCount")
    @ApiOperation("测试数据源并返回该数据源下所有表以及表数据量")
    public R dataSourceTestAndReturnTablesAndDataCount(HttpServletRequest request, @RequestBody JobDatasource jobJdbcDatasource) throws IOException {
        String spaceId = getSpaceId(request);
        jobJdbcDatasource.setSpaceId(spaceId);
        R r = checkParam(jobJdbcDatasource);
        if (r.getCode() != ApiErrorCode.SUCCESS.getCode()) {
            return r;
        }
        String first = RSAEncrypt.decrypt(jobJdbcDatasource.getJdbcPassword());
        if (first == null) {
            return R.failed(CommonUtils.messageInternational("param_error"));
        }
        jobJdbcDatasource.setJdbcPassword(first);
        return success(jobJdbcDatasourceService.dataSourceTestAndReturnTablesAndDataCount(jobJdbcDatasource));
    }

    @GetMapping("/selectCountByTableName")
    @ApiOperation("返回数据源下某个表得数据量")
    public R<Long> selectCountByTableName(String datasourceId, String tableName) {
        return jobJdbcDatasourceService.selectCountByTableName(datasourceId, tableName);
    }

    private R<Boolean> checkParam(JobDatasource jobJdbcDatasource) {
        if (StringUtils.isEmpty(jobJdbcDatasource.getDatasourceName())) {
            return R.failed(CommonUtils.messageInternational("datasource_name_null"));
        }
        if (StringUtils.isEmpty(jobJdbcDatasource.getDatasource())) {
            return R.failed(CommonUtils.messageInternational("datasource_type_null"));
        }
        if (StringUtils.isAnyEmpty(jobJdbcDatasource.getJdbcUrl(), jobJdbcDatasource.getJdbcUsername(), jobJdbcDatasource.getJdbcPassword())) {
            return R.failed(CommonUtils.messageInternational("datasource_type_null"));
        }
        if (jobJdbcDatasource.getId() == null) {
            // When updating, do not verify if the name is duplicate here
            QueryWrapper<JobDatasource> jobDatasourceQueryWrapper = new QueryWrapper<>();
            jobDatasourceQueryWrapper.eq("space_id", jobJdbcDatasource.getSpaceId());
            jobDatasourceQueryWrapper.eq("datasource_name", jobJdbcDatasource.getDatasourceName());
            List<JobDatasource> list = jobJdbcDatasourceService.list(jobDatasourceQueryWrapper);
            if (!CollectionUtils.isEmpty(list)) {
                return R.failed(CommonUtils.messageInternational("datasource_name_duplicate"));
            }
        }
        return R.ok(true);
    }
}
