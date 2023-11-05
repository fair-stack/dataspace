package cn.cnic.dataspace.api.model.release.stemcells;

import lombok.Data;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.Map;

@Data
public class RequestSample {

    private String sampleId;

    private String id;

    @NotBlank(message = "发布机构 不能为空")
    private String orgId;

    @NotBlank(message = "模板标识 不能为空")
    private String templateId;

    @NotBlank(message = "iri 不能为空")
    private String iri;

    @NotBlank(message = "空间信息 不能为空")
    private String spaceId;

    @Valid
    @NotEmpty(message = "基本信息 不能为空")
    private Map<String, Object> sampleData;
}
