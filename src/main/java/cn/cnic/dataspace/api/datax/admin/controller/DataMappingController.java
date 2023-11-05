package cn.cnic.dataspace.api.datax.admin.controller;

import cn.cnic.dataspace.api.config.space.SpaceControlConfig;
import cn.cnic.dataspace.api.datax.admin.aop.HasSpacePermission;
import cn.cnic.dataspace.api.datax.admin.aop.SpacePermission;
import cn.cnic.dataspace.api.datax.admin.core.cron.CronExpression;
import cn.cnic.dataspace.api.datax.admin.core.util.I18nUtil;
import cn.cnic.dataspace.api.datax.admin.dto.DataMappingDto;
import cn.cnic.dataspace.api.datax.admin.entity.DataMappingMeta;
import cn.cnic.dataspace.api.datax.admin.dto.ImportFromDataSourceVo;
import cn.cnic.dataspace.api.datax.admin.dto.QueryDataMappingDataVO;
import cn.cnic.dataspace.api.datax.admin.service.DataMappingService;
import cn.cnic.dataspace.api.datax.core.biz.model.ReturnT;
import cn.cnic.dataspace.api.datax.core.util.DateUtil;
import cn.cnic.dataspace.api.util.Token;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.api.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/data_mapping")
@HasSpacePermission
@Api(tags = "结构化数据")
public class DataMappingController extends BaseController {

    @Resource
    private DataMappingService dataMappingService;

    @Resource
    private SpaceControlConfig spaceControlConfig;

    @ApiOperation("获取表格元数据")
    @GetMapping("/getColumnMeta")
    public R<DataMappingMeta> getColumnMeta(HttpServletRequest request, String dataMappingId) {
        String spaceId = getSpaceId(request);
        String currentUserId = getCurrentUserId(request);
        return dataMappingService.getColumnMeta(currentUserId, spaceId, dataMappingId);
    }

    @ApiOperation("设置表格元数据")
    @PostMapping("/setColumnMeta")
    public R setColumnMeta(HttpServletRequest request, @RequestBody DataMappingMeta dataMappingMeta) {
        String spaceId = getSpaceId(request);
        String currentUserId = getCurrentUserId(request);
        dataMappingService.setColumnMeta(currentUserId, spaceId, dataMappingMeta);
        return success("ok");
    }

    @ApiOperation("更新表格元数据")
    @PutMapping("/setColumnMeta")
    public R updateColumnMeta(HttpServletRequest request, @RequestBody DataMappingMeta dataMappingMeta) {
        String spaceId = getSpaceId(request);
        String currentUserId = getCurrentUserId(request);
        dataMappingService.updateColumnMeta(currentUserId, spaceId, dataMappingMeta);
        return success("ok");
    }

    /**
     * Paging to query all data
     */
    @PostMapping("/selectByPage")
    @ApiOperation("分页查询结构化数据列表")
    @ApiImplicitParams({ @ApiImplicitParam(paramType = "query", dataType = "String", name = "current", value = "当前页", defaultValue = "1", required = true), @ApiImplicitParam(paramType = "query", dataType = "String", name = "size", value = "一页大小", defaultValue = "10", required = true), @ApiImplicitParam(paramType = "query", dataType = "String", name = "name", value = "名称") })
    public R<IPage<DataMappingDto>> selectByPage(HttpServletRequest request, @RequestParam(required = false) String name, @RequestParam(required = false, defaultValue = "1") Integer current, @RequestParam(required = false, defaultValue = "10") Integer size) {
        String spaceId = getSpaceId(request);
        String currentUserId = getCurrentUserId(request);
        IPage<DataMappingDto> data = dataMappingService.selectByPage(spaceId, currentUserId, name, current, size);
        return success(data);
    }

    @GetMapping("/getStaInfo")
    @ApiOperation("getStaInfo")
    public R<Map<String, Object>> getStaInfo(HttpServletRequest request) {
        String spaceId = getSpaceId(request);
        String currentUserId = getCurrentUserId(request);
        return dataMappingService.getStaInfo(spaceId);
    }

    /**
     * Query all data
     */
    @PostMapping("/selectAll")
    @ApiOperation("查询结构化数据列表")
    @ApiImplicitParams({ @ApiImplicitParam(paramType = "query", dataType = "String", name = "name", value = "名称") })
    public R<List<DataMappingDto>> selectAll(HttpServletRequest request, @RequestParam(required = false) String name) {
        String spaceId = getSpaceId(request);
        String currentUserId = getCurrentUserId(request);
        List<DataMappingDto> data = dataMappingService.selectAll(spaceId, currentUserId, name);
        return success(data);
    }

    /**
     * Query all data
     */
    @PostMapping("/selectAllWithBasicInfo")
    @ApiOperation("查询结构化数据列表,返回基础的信息和数据量")
    @ApiImplicitParams({ @ApiImplicitParam(paramType = "query", dataType = "String", name = "name", value = "名称") })
    public R<List<DataMappingDto>> selectAllWithBasicInfo(HttpServletRequest request, @RequestParam(required = false) String name) {
        String spaceId = getSpaceId(request);
        String currentUserId = getCurrentUserId(request);
        List<DataMappingDto> data = dataMappingService.selectAll(spaceId, currentUserId, name);
        return success(data);
    }

    @ApiOperation(value = "获取结构化数据基本信息")
    @GetMapping("/getBasicInfo")
    public R<DataMappingDto> getBasicInfo(HttpServletRequest request, Long dataMappingId) {
        String spaceId = getSpaceId(request);
        return dataMappingService.getBasicInfo(spaceId, dataMappingId);
    }

    @ApiOperation("根据ID删除结构化数据")
    @DeleteMapping("/delete")
    @HasSpacePermission(value = SpacePermission.T_DELETE)
    public R<Boolean> delete(HttpServletRequest request, String dataMappingId) {
        String spaceId = getSpaceId(request);
        String userId = getCurrentUserId(request);
        return dataMappingService.delete(spaceId, userId, dataMappingId);
    }

    @ApiOperation("根据ID对结构化数据重命名")
    @PostMapping("/rename")
    @HasSpacePermission(value = SpacePermission.T_EDIT)
    public R<Boolean> rename(HttpServletRequest request, String dataMappingId, String newName) {
        String spaceId = getSpaceId(request);
        String userId = getCurrentUserId(request);
        return dataMappingService.rename(spaceId, userId, dataMappingId, newName);
    }

    @ApiOperation("根据ID更新是否开放")
    @PostMapping("/updatePublic")
    @HasSpacePermission(value = SpacePermission.T_EDIT)
    public R<Boolean> updatePublic(HttpServletRequest request, String dataMappingId, String isPublic) {
        String spaceId = getSpaceId(request);
        String userId = getCurrentUserId(request);
        return dataMappingService.updatePublic(spaceId, userId, dataMappingId, isPublic);
    }

    @ApiOperation("导入excel,默认按照excel名称建表")
    @PostMapping("/import")
    @HasSpacePermission(value = SpacePermission.T_CREATE)
    public R<Boolean> importExcel(HttpServletRequest request, MultipartFile file, String isHeader, String sheetName, Integer sheetNum) {
        String spaceId = getSpaceId(request);
        String userId = getCurrentUserId(request);
        return dataMappingService.importExcel(spaceId, userId, file, isHeader, sheetName, sheetNum);
    }

    @ApiOperation("从空间导入excel")
    @PostMapping("/importExcelFromSpaceFile")
    @HasSpacePermission(value = SpacePermission.T_CREATE)
    public R<Boolean> importExcelFromSpaceFile(HttpServletRequest request, String hash, String isHeader, String sheetName, Integer sheetNum, int fromType) {
        String spaceId = getSpaceId(request);
        String userId = getCurrentUserId(request);
        return dataMappingService.importExcelFromSpaceFile(request, spaceId, userId, hash, isHeader, sheetName, sheetNum, fromType);
    }

    @ApiOperation("追加导入excel")
    @PostMapping("/incrementImportExcel")
    @HasSpacePermission(value = SpacePermission.T_EDIT)
    public R<Boolean> incrementImportExcel(HttpServletRequest request, String hash, String isHeader, String sheetName, int fromType, Long dataMappingId) {
        String spaceId = getSpaceId(request);
        String userId = getCurrentUserId(request);
        return dataMappingService.incrementImportExcelFromFile(request, spaceId, userId, hash, isHeader, sheetName, fromType, dataMappingId);
    }

    @ApiOperation("导出Excel")
    @GetMapping("/exportExcel")
    @HasSpacePermission(SpacePermission.NO_VALID)
    public void exportExcel(HttpServletRequest request, HttpServletResponse response, Long dataMappingId, String spaceId) throws IOException {
        String userId = getCurrentUserId(request);
        dataMappingService.exportExcel(response, spaceId, userId, dataMappingId);
    }

    @ApiOperation("导出Excel到空间")
    @PostMapping("/exportExcelToSpace")
    public R<Boolean> exportExcelToSpace(HttpServletRequest request, Long dataMappingId, String hash) {
        String spaceId = getSpaceId(request);
        Token currentUser = getCurrentUser(request);
        return dataMappingService.exportExcelToSpace(request, spaceId, currentUser, dataMappingId, hash);
    }

    @PostMapping("/copy")
    @ApiOperation("复制一份")
    @HasSpacePermission(SpacePermission.T_CREATE)
    public R<Boolean> copy(HttpServletRequest request, Long dataMappingId, String newName) {
        String currentUserId = getCurrentUserId(request);
        String spaceId = getSpaceId(request);
        return dataMappingService.copy(spaceId, currentUserId, dataMappingId, newName);
    }

    @ApiOperation("获取数据")
    @GetMapping("/getData")
    public R<Map<String, Object>> getData(HttpServletRequest request, String dataMappingId, @RequestParam(required = false, defaultValue = "1") Integer isReturnId, @RequestParam(required = false, defaultValue = "1") Integer current, @RequestParam(required = false, defaultValue = "10") Integer size) {
        String spaceId = getSpaceId(request);
        String userId = getCurrentUserId(request);
        return dataMappingService.getData(spaceId, userId, dataMappingId, isReturnId, current, size);
    }

    @ApiOperation("根据查询条件和排序条件获取数据")
    @PostMapping("/getDataBySortAndFilter")
    public R<Map<String, Object>> getDataBySortAndFilter(HttpServletRequest request, @RequestBody QueryDataMappingDataVO queryDataMappingDataVO) {
        String spaceId = getSpaceId(request);
        String userId = getCurrentUserId(request);
        if (queryDataMappingDataVO.getCurrent() == null) {
            queryDataMappingDataVO.setCurrent(1);
        }
        if (queryDataMappingDataVO.getSize() == null) {
            queryDataMappingDataVO.setSize(10);
        }
        return dataMappingService.getDataBySortAndFilter(spaceId, userId, queryDataMappingDataVO);
    }

    @ApiOperation("获取schema")
    @GetMapping("/getSchema")
    public R<Map<String, Object>> getSchema(HttpServletRequest request, String dataMappingId) {
        String spaceId = getSpaceId(request);
        String userId = getCurrentUserId(request);
        return dataMappingService.getSchema(spaceId, userId, dataMappingId);
    }

    @ApiOperation("根据文件hash值获取excel的sheetNames")
    @GetMapping("/getSheetNames")
    public R<List<String>> getSheetNames(HttpServletRequest request, String hash) {
        String spaceId = getSpaceId(request);
        return dataMappingService.getSheetNames(request, spaceId, hash);
    }

    @ApiOperation("上传文件获取excel的sheetNames")
    @PostMapping("/getSheetNamesByFile")
    public R<Map<String, Object>> getSheetNamesByFile(HttpServletRequest request, MultipartFile file) {
        String spaceId = getSpaceId(request);
        return dataMappingService.getSheetNames(request, spaceId, file);
    }

    @ApiOperation("配置从数据源导入结构化数据规则")
    @PostMapping("/importFromDataSource")
    @HasSpacePermission(SpacePermission.T_CREATE)
    public R<Boolean> importFromDataSource(HttpServletRequest request, @RequestBody ImportFromDataSourceVo importFromDataSourceVo) {
        String spaceId = getSpaceId(request);
        String currentUserId = getCurrentUserId(request);
        return dataMappingService.importFromDataSource(spaceId, currentUserId, importFromDataSourceVo);
    }

    @ApiOperation("获取从数据源导入结构化数据规则信息")
    @GetMapping("/getImportFromDataSource")
    public R<ImportFromDataSourceVo> getImportFromDataSource(HttpServletRequest request, Long dataMappingId) {
        String spaceId = getSpaceId(request);
        String currentUserId = getCurrentUserId(request);
        return dataMappingService.getImportFromDataSource(spaceId, currentUserId, dataMappingId);
    }

    @ApiOperation("执行一次")
    @GetMapping("/triggerImportFromDataSource")
    @HasSpacePermission(SpacePermission.T_EDIT)
    public R<Boolean> triggerImportFromDataSource(HttpServletRequest request, Long dataMappingId) {
        String spaceId = getSpaceId(request);
        String currentUserId = getCurrentUserId(request);
        return dataMappingService.triggerImportFromDataSource(spaceId, currentUserId, dataMappingId);
    }

    @ApiOperation("停止定时任务从数据源导入结构化数据")
    @GetMapping("/stopImportFromDataSource")
    @HasSpacePermission(SpacePermission.T_EDIT)
    public R<Boolean> stopImportFromDataSource(HttpServletRequest request, Long dataMappingId) {
        String spaceId = getSpaceId(request);
        String currentUserId = getCurrentUserId(request);
        return dataMappingService.stopImportFromDataSource(spaceId, currentUserId, dataMappingId);
    }

    @ApiOperation("开始定时任务从数据源导入结构化数据")
    @GetMapping("/startImportFromDataSource")
    @HasSpacePermission(SpacePermission.T_EDIT)
    public R<Boolean> startImportFromDataSource(HttpServletRequest request, Long dataMappingId) {
        String spaceId = getSpaceId(request);
        String currentUserId = getCurrentUserId(request);
        return dataMappingService.startImportFromDataSource(spaceId, currentUserId, dataMappingId);
    }

    @ApiOperation("更新从数据源导入数据规则")
    @PostMapping("/updateImportFromDataSource")
    @HasSpacePermission(SpacePermission.T_EDIT)
    public R<Boolean> updateImportFromDataSource(HttpServletRequest request, @RequestBody ImportFromDataSourceVo importFromDataSourceVo) {
        String spaceId = getSpaceId(request);
        String currentUserId = getCurrentUserId(request);
        return dataMappingService.updateImportFromDataSource(spaceId, currentUserId, importFromDataSourceVo);
    }

    @GetMapping("/nextTriggerTime")
    @ApiOperation("获取近5次触发时间")
    public ReturnT<List<String>> nextTriggerTime(String cron) {
        List<String> result = new ArrayList<>();
        try {
            CronExpression cronExpression = new CronExpression(cron);
            Date lastTime = new Date();
            for (int i = 0; i < 5; i++) {
                lastTime = cronExpression.getNextValidTimeAfter(lastTime);
                if (lastTime != null) {
                    result.add(DateUtil.formatDateTime(lastTime));
                } else {
                    break;
                }
            }
        } catch (ParseException e) {
            return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("jobinfo_field_cron_invalid"));
        }
        return new ReturnT<>(result);
    }
}
