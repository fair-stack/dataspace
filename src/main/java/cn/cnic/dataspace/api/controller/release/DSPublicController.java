package cn.cnic.dataspace.api.controller.release;

import cn.cnic.dataspace.api.model.release.stemcells.DoPostInfo;
import cn.cnic.dataspace.api.model.release.stemcells.RequestSample;
import cn.cnic.dataspace.api.model.release.ResourceRequest;
import cn.cnic.dataspace.api.service.interaction.DSPublicService;
import cn.cnic.dataspace.api.util.ResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Resource release review
 */
@RestController
@Api(tags = "资源发布提交")
@RequestMapping("/release")
public class DSPublicController {

    @Autowired
    private DSPublicService dsPublicService;

    @ApiOperation("发布时填写数据集名称验证")
    @GetMapping("/check")
    public ResponseResult<Object> check(@RequestHeader("Authorization") String token, @RequestParam(value = "resourceId", required = false) String resourceId, @RequestParam("resourceType") String resourceType, @RequestParam("spaceId") String spaceId, @RequestParam("title") String title) {
        return dsPublicService.check(token, resourceId, resourceType, spaceId, title);
    }

    @ApiOperation("发布提交")
    @PostMapping("/submit")
    public ResponseResult<Object> dsSubmit(@RequestHeader("Authorization") String token, @RequestBody ResourceRequest resourceRequest, HttpServletRequest request) {
        return dsPublicService.dsSubmit(token, resourceRequest, request);
    }

    @ApiOperation("获取上一个版本的数据内容")
    @GetMapping("/level.details")
    public ResponseResult<Object> levelVersionDetails(@RequestHeader("Authorization") String token, @RequestParam("id") String id) {
        return dsPublicService.levelVersionDetails(token, id);
    }

    @ApiOperation("草稿箱编辑查询")
    @GetMapping("/drafts.details")
    public ResponseResult<Object> draftsDetails(@RequestHeader("Authorization") String token, @RequestParam("resourceId") String resourceId) {
        return dsPublicService.draftsDetails(token, resourceId);
    }

    @ApiOperation("草稿修改")
    @PostMapping("/update.drafts")
    public ResponseResult<Object> updateDrafts(@RequestHeader("Authorization") String token, @RequestBody ResourceRequest resourceRequest, HttpServletRequest request) {
        return dsPublicService.updateDrafts(token, resourceRequest, request);
    }

    @ApiOperation("撤销审核")
    @GetMapping("/repeal")
    public ResponseResult<Object> releaseRepeal(@RequestHeader("Authorization") String token, @RequestParam(name = "resourceId") String resourceId, @RequestParam(name = "version") String version) {
        return dsPublicService.releaseRepeal(token, resourceId, version);
    }

    @ApiOperation("数据重新传输")
    @GetMapping("/retry")
    public ResponseResult<Object> retry(@RequestParam("id") String id, HttpServletRequest request) {
        return dsPublicService.retry(id, request);
    }

    @ApiOperation("获取模板列表")
    @GetMapping("/template.parsing")
    public ResponseResult<Object> templateList(@RequestParam(name = "orgId") String orgId) {
        return dsPublicService.templateList(orgId);
    }

    // @ApiOperation ("Get Template Details")
    // @GetMapping("/templateDe")
    // public ResponseResult<Object> templateDetails(@RequestParam(name = "tempId") String tempId){
    // return dsPublicService.templateDetails(tempId);
    // }
    /*Adapted stem cells*/
    @ApiOperation("下载样本数据模板")
    @GetMapping("/tem.down")
    public void tempDown(@RequestParam(name = "orgId") String orgId, @RequestParam(name = "templateId") String templateId, @RequestParam(name = "iri") String iri, HttpServletResponse response) throws IOException {
        dsPublicService.tempDown(orgId, templateId, iri, response);
        return;
    }

    @PostMapping("/sampleAdd")
    public ResponseResult<Object> sampleAdd(@RequestBody RequestSample requestSample, HttpServletRequest request) {
        return dsPublicService.sampleAdd(requestSample, request);
    }

    @GetMapping("/sampleQuery")
    public ResponseResult<Object> sampleQuery(@RequestParam(name = "page", defaultValue = "1") Integer page, @RequestParam(name = "size", defaultValue = "10") Integer size, @RequestParam(name = "type", defaultValue = "0") Integer type, @RequestParam(name = "iri") String iri, @RequestParam(name = "sampleId", required = false) String sampleId) {
        return dsPublicService.sampleQuery(page, size, type, iri, sampleId);
    }

    @GetMapping("/sampleDelete")
    public ResponseResult<Object> sampleDelete(@RequestParam(name = "id") String id) {
        return dsPublicService.sampleDelete(id);
    }

    @GetMapping("/sampleDel")
    public ResponseResult<Object> sampleDelete(@RequestParam(name = "ids") String... ids) {
        return dsPublicService.sampleDel(ids);
    }

    @PostMapping("sampleBatchAdd")
    public ResponseResult<Object> sampleBatchAdd(@RequestHeader("Authorization") String token, @RequestParam(name = "sampleId", required = false) String sampleId, @RequestParam(name = "spaceId") String spaceId, @RequestParam(name = "orgId") String orgId, @RequestParam(name = "templateId") String templateId, @RequestParam(name = "iri") String iri, @RequestParam(name = "fileList") String[] fileList, @RequestParam(name = "fileHash", required = false) String fileHash, @RequestParam(value = "file", required = false) MultipartFile file, HttpServletRequest request) throws IOException {
        return dsPublicService.sampleBatchAdd(token, sampleId, spaceId, orgId, templateId, iri, fileList, fileHash, file, request);
    }

    @GetMapping("/tempQuery")
    public ResponseResult<Object> templateQuery(@RequestParam(name = "page", defaultValue = "1") Integer page, @RequestParam(name = "size", defaultValue = "10") Integer size, @RequestParam(name = "type", defaultValue = "0") Integer type, @RequestParam(name = "iri") String iri, @RequestParam(name = "sampleId", required = false) String sampleId) {
        return dsPublicService.templateQuery(page, size, type, iri, sampleId);
    }

    @GetMapping("/operation")
    public ResponseResult<Object> operation(@RequestParam(name = "sampleId") String sampleId, @RequestParam(name = "type") String type) {
        return dsPublicService.operation(sampleId, type);
    }

    @GetMapping("/doGet")
    public Object doGet(@RequestParam(name = "url") String url) {
        return dsPublicService.doGet(url);
    }

    @PostMapping("/doPost")
    public Object doPost(@RequestBody DoPostInfo doPostInfo) {
        return dsPublicService.doPost(doPostInfo);
    }

    @ApiOperation("邮件补发")
    @GetMapping("/email.reissue")
    public ResponseResult<Object> emailReissue(@RequestHeader("Authorization") String token, @RequestParam(name = "id") String id) {
        return dsPublicService.emailReissue(token, id);
    }
}
