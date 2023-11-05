package cn.cnic.dataspace.api.datax.admin.controller;

import cn.cnic.dataspace.api.config.space.SpaceControlConfig;
import cn.cnic.dataspace.api.datax.admin.aop.HasSpacePermission;
import cn.cnic.dataspace.api.datax.admin.aop.SpacePermission;
import cn.cnic.dataspace.api.datax.admin.dto.*;
import cn.cnic.dataspace.api.datax.admin.service.DataMappingOperService;
import cn.cnic.dataspace.api.util.SpaceRoleEnum;
import com.baomidou.mybatisplus.extension.api.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@Api(tags = "结构化数据更改操作")
@RestController
@RequestMapping("data_mapping_oper")
@HasSpacePermission
public class DataMappingOperController extends BaseController {

    @Resource
    private DataMappingOperService dataMappingOperService;

    @Resource
    private SpaceControlConfig spaceControlConfig;

    @ApiOperation("获取字段分组的值")
    @PostMapping("/group")
    public R<List<Map<String, String>>> getGroupVal(HttpServletRequest request, @RequestBody QueryDataMappingGroupVO queryDataMappingGroupVO) {
        String spaceId = getSpaceId(request);
        String currentUserId = getCurrentUserId(request);
        return dataMappingOperService.getGroupVal(spaceId, currentUserId, queryDataMappingGroupVO);
    }

    @ApiOperation("更新数据")
    @PostMapping("/updateData")
    @ApiImplicitParams({ @ApiImplicitParam(paramType = "form", dataType = "Long", name = "dataMappingId", value = "结构化数据ID", required = true), @ApiImplicitParam(paramType = "form", dataType = "String", name = "primaryKeyVal", value = "数据主键ID值", required = true), @ApiImplicitParam(paramType = "form", dataType = "String", name = "col", value = "要更新列"), @ApiImplicitParam(paramType = "form", dataType = "String", name = "data", value = "要更新数据列的值") })
    @HasSpacePermission(SpacePermission.T_EDIT)
    public R<Boolean> updateData(HttpServletRequest request, Long dataMappingId, String primaryKeyVal, String col, String data) {
        String spaceId = getSpaceId(request);
        String currentUserId = getCurrentUserId(request);
        return dataMappingOperService.updateData(spaceId, currentUserId, dataMappingId, primaryKeyVal, col, data);
    }

    @ApiOperation("全局搜索")
    @PostMapping("/searchData")
    public R<Map<String, Object>> searchData(HttpServletRequest request, String dataMappingId, String searchVal, @RequestParam(required = false, defaultValue = "1") Integer current, @RequestParam(required = false, defaultValue = "10") Integer size) {
        String spaceId = getSpaceId(request);
        String userId = getCurrentUserId(request);
        return dataMappingOperService.searchData(spaceId, userId, dataMappingId, searchVal, current, size);
    }

    @ApiOperation("添加一行数据")
    @PostMapping("/addLine")
    @HasSpacePermission(SpacePermission.T_EDIT)
    public R<Boolean> addLine(HttpServletRequest request, @RequestBody AddLineVo addLineVo) {
        String spaceId = getSpaceId(request);
        String userId = getCurrentUserId(request);
        return dataMappingOperService.addLine(spaceId, userId, addLineVo);
    }

    @ApiOperation("复制一行或多行数据")
    @PostMapping("/copyLine")
    @HasSpacePermission(SpacePermission.T_EDIT)
    public R<Boolean> copyLine(HttpServletRequest request, @RequestBody CopyLineVO copyLineVO) {
        String spaceId = getSpaceId(request);
        String userId = getCurrentUserId(request);
        return dataMappingOperService.copyLine(spaceId, userId, copyLineVO);
    }

    @ApiOperation("删除一行或者多行数据")
    @PostMapping("/deleteLine")
    @HasSpacePermission(SpacePermission.T_EDIT)
    public R<Boolean> deleteLine(HttpServletRequest request, @RequestBody DeleteLineVO deleteLineVO) {
        String spaceId = getSpaceId(request);
        String userId = getCurrentUserId(request);
        return dataMappingOperService.deleteLine(spaceId, userId, deleteLineVO);
    }

    @ApiOperation("更新列名列类型")
    @PostMapping("/alterColNameAndType")
    @ApiImplicitParams({ @ApiImplicitParam(paramType = "form", dataType = "Long", name = "dataMappingId", value = "结构化数据ID", required = true), @ApiImplicitParam(paramType = "form", dataType = "String", name = "oldColName", value = "原来列名称", required = true), @ApiImplicitParam(paramType = "form", dataType = "String", name = "newColName", value = "新的列名称"), @ApiImplicitParam(paramType = "form", dataType = "String", name = "newColType", value = "新的列类型") })
    @HasSpacePermission(SpacePermission.T_EDIT)
    public R<Boolean> alterColNameAndType(HttpServletRequest request, Long dataMappingId, String oldColName, String newColName, String newColType) {
        String spaceId = getSpaceId(request);
        String currentUserId = getCurrentUserId(request);
        return dataMappingOperService.alterColNameAndType(spaceId, currentUserId, dataMappingId, oldColName, newColName, newColType);
    }

    @ApiOperation("转大写")
    @PostMapping("/toUpper")
    @HasSpacePermission(SpacePermission.T_EDIT)
    public R<Boolean> toUpper(HttpServletRequest request, Long dataMappingId, String colName) {
        String spaceId = getSpaceId(request);
        String currentUserId = getCurrentUserId(request);
        return dataMappingOperService.toUpper(spaceId, currentUserId, dataMappingId, colName);
    }

    @ApiOperation("转小写")
    @PostMapping("/toLower")
    @HasSpacePermission(SpacePermission.T_EDIT)
    public R<Boolean> toLower(HttpServletRequest request, Long dataMappingId, String colName) {
        String spaceId = getSpaceId(request);
        String currentUserId = getCurrentUserId(request);
        return dataMappingOperService.toLower(spaceId, currentUserId, dataMappingId, colName);
    }

    @ApiOperation("添加前缀")
    @PostMapping("/addPrex")
    @HasSpacePermission(SpacePermission.T_EDIT)
    public R<Boolean> addPrex(HttpServletRequest request, Long dataMappingId, String colName, String prex) {
        String spaceId = getSpaceId(request);
        String currentUserId = getCurrentUserId(request);
        return dataMappingOperService.addPrex(spaceId, currentUserId, dataMappingId, colName, prex);
    }

    @ApiOperation("添加后缀")
    @PostMapping("/addSuffix")
    @HasSpacePermission(SpacePermission.T_EDIT)
    public R<Boolean> addSuffix(HttpServletRequest request, Long dataMappingId, String colName, String suffix) {
        String spaceId = getSpaceId(request);
        String currentUserId = getCurrentUserId(request);
        return dataMappingOperService.addSuffix(spaceId, currentUserId, dataMappingId, colName, suffix);
    }

    @ApiOperation("替换")
    @PostMapping("/replace")
    @HasSpacePermission(SpacePermission.T_EDIT)
    public R<Boolean> replace(HttpServletRequest request, Long dataMappingId, String colName, String search, String replace) {
        String spaceId = getSpaceId(request);
        String currentUserId = getCurrentUserId(request);
        return dataMappingOperService.replace(spaceId, currentUserId, dataMappingId, colName, search, replace);
    }

    @ApiOperation("清空一列或者多列")
    @PostMapping("/setCol2Null")
    @HasSpacePermission(SpacePermission.T_EDIT)
    public R<Boolean> setCol2Null(HttpServletRequest request, @RequestBody SetCol2NullVO setCol2NullVO) {
        String spaceId = getSpaceId(request);
        String currentUserId = getCurrentUserId(request);
        return dataMappingOperService.setCol2Null(spaceId, currentUserId, setCol2NullVO);
    }

    @ApiOperation("删除一列或者多列")
    @PostMapping("/dropCol")
    @HasSpacePermission(SpacePermission.T_EDIT)
    public R<Boolean> dropCol(HttpServletRequest request, @RequestBody DropColVO dropColVO) {
        String spaceId = getSpaceId(request);
        String currentUserId = getCurrentUserId(request);
        return dataMappingOperService.dropCol(spaceId, currentUserId, dropColVO);
    }

    @ApiOperation("添加一个或者多个空列")
    @PostMapping("/addEmptyCol")
    @HasSpacePermission(SpacePermission.T_EDIT)
    public R<Boolean> addEmptyCol(HttpServletRequest request, @RequestBody AddEmptyColVO addEmptyColVO) {
        String spaceId = getSpaceId(request);
        String currentUserId = getCurrentUserId(request);
        return dataMappingOperService.addEmptyCol(spaceId, currentUserId, addEmptyColVO);
    }

    @ApiOperation("复制一列或者多列")
    @PostMapping("/copyAddCol")
    @HasSpacePermission(SpacePermission.T_EDIT)
    public R<Boolean> copyAddCol(HttpServletRequest request, @RequestBody CopyAddColVO copyAddColVO) {
        String spaceId = getSpaceId(request);
        String currentUserId = getCurrentUserId(request);
        return dataMappingOperService.copyAddCol(spaceId, currentUserId, copyAddColVO);
    }

    @ApiOperation("合并列")
    @PostMapping("/mergeCol")
    @HasSpacePermission(SpacePermission.T_EDIT)
    public R<Boolean> mergeCol(HttpServletRequest request, @RequestBody MergeColVO mergeColVO) {
        String spaceId = getSpaceId(request);
        String currentUserId = getCurrentUserId(request);
        return dataMappingOperService.mergeCol(spaceId, currentUserId, mergeColVO);
    }

    @ApiOperation("拆分列")
    @PostMapping("/split")
    @HasSpacePermission(SpacePermission.T_EDIT)
    public R<Boolean> split(HttpServletRequest request, Long dataMappingId, String splitCol, String split, String left, String right) {
        String spaceId = getSpaceId(request);
        String currentUserId = getCurrentUserId(request);
        return dataMappingOperService.split(spaceId, currentUserId, dataMappingId, splitCol, split, left, right);
    }
}
