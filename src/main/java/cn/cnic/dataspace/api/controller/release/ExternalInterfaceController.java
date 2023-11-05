package cn.cnic.dataspace.api.controller.release;

import cn.cnic.dataspace.api.model.center.*;
import cn.cnic.dataspace.api.service.ExternalInterService;
import cn.cnic.dataspace.api.util.Constants;
import cn.cnic.dataspace.api.util.ResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * External interface
 */
@RestController
@Api(tags = "外部接口调用")
@RequestMapping("access")
public class ExternalInterfaceController {

    @Autowired
    private ExternalInterService externalInterService;

    @ApiOperation("发布-获取机构和模板")
    @GetMapping("/orgList")
    public ResponseResult<Object> accessOrgList() {
        return externalInterService.accessOrgList(null, Constants.GENERAL);
    }

    @ApiOperation("作者搜索")
    @GetMapping("/authorList")
    public ResponseResult<Object> authorSearch(@RequestParam(name = "param", required = false) String param) {
        return externalInterService.authorSearch(param);
    }

    @ApiOperation("发布-申请DOI")
    @GetMapping("/apply.DOI")
    public ResponseResult<Object> applyDOI() {
        return externalInterService.applyDOI();
    }

    @ApiOperation("发布-申请CSTR")
    @PostMapping("/apply.CSTR")
    public ResponseResult<Object> applyCSTR(@RequestBody CSTR cstr) {
        return externalInterService.applyCSTR(cstr);
    }

    @ApiOperation("发布-校验CSTR是否可用")
    @GetMapping("/check.CSTR")
    public ResponseResult<Object> checkCstr(@RequestParam(name = "cstrCode") String cstrCode) {
        return externalInterService.checkCstr(cstrCode);
    }

    @ApiOperation("发布-项目查询")
    @GetMapping("/projectList")
    public ResponseResult<Object> accessProjectList(@RequestParam(name = "projectName", required = false) String projectName) {
        return externalInterService.accessProjectList(projectName);
    }

    @ApiOperation("发布-论文查询")
    @GetMapping("/paperList")
    public ResponseResult<Object> accessPaperList(@RequestParam(name = "paperName", required = false) String paperName) {
        return externalInterService.accessPaperList(paperName);
    }

    @ApiOperation("作者添加-机构列表")
    @GetMapping("/queryOrg")
    public ResponseResult<Object> orgList(@RequestParam(name = "orgName", required = false) String orgName) {
        return externalInterService.orgList(orgName);
    }

    @ApiOperation("作者添加")
    @PostMapping("/personAdd")
    public ResponseResult<Object> personAdd(@RequestBody Person person) {
        return externalInterService.personAdd(person);
    }

    @ApiOperation("机构添加")
    @PostMapping("/orgAdd")
    public ResponseResult<Object> orgAdd(@RequestBody Org org) {
        return externalInterService.orgAdd(org);
    }

    @ApiOperation("项目添加")
    @PostMapping("/projectAdd")
    public ResponseResult<Object> projectAdd(@RequestBody Project project) {
        return externalInterService.projectAdd(project);
    }

    @ApiOperation("获取项目类型")
    @GetMapping("/proType")
    public ResponseResult<Object> getProType(@RequestParam(name = "type", required = false) String type) {
        return externalInterService.getProType(type);
    }

    @ApiOperation("论文添加")
    @PostMapping("/paperAdd")
    public ResponseResult<Object> paperAdd(@RequestBody Paper paper) {
        return externalInterService.paperAdd(paper);
    }

    @ApiOperation("发布-查询详情")
    @GetMapping("/forDetails")
    public ResponseResult<Object> forDetails(@RequestParam(name = "type") String type, @RequestParam(name = "ids") String... ids) {
        return externalInterService.forDetails(type, ids);
    }

    @ApiOperation("许可协议")
    @PostMapping("/licenseList")
    public ResponseResult<Object> licenseList(@RequestParam(name = "orgId", required = false) String orgId, @RequestParam(name = "url", required = false) String url) {
        return externalInterService.licenseList(orgId, url);
    }
}
