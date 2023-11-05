package cn.cnic.dataspace.api.model.release.stemcells;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class RequestSampleBatch {

    private String sampleId;

    @NotBlank(message = "发布机构 不能为空")
    private String orgId;

    @NotBlank(message = "模板标识 不能为空")
    private String templateId;

    @NotBlank(message = "iri 不能为空")
    private String iri;

    @NotBlank(message = "空间信息 不能为空")
    private String spaceId;

    @NotEmpty(message = "请选择文件在发布")
    private List<String> fileList;

    private MultipartFile file;

    private String fileHash;
}
