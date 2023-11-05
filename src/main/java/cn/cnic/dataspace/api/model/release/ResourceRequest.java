package cn.cnic.dataspace.api.model.release;

import lombok.Data;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;

@Data
public class ResourceRequest implements Serializable {

    private String resourceId;

    private int type;

    @NotBlank(message = "版本 不能为空")
    private String version;

    @NotBlank(message = "发布机构 不能为空")
    private String orgId;

    @NotBlank(message = "发布机构 不能为空")
    private String // Publishing agency
    orgName;

    @NotBlank(message = "模板 不能为空")
    private String // Template
    template;

    @NotBlank(message = "模板标识 不能为空")
    private String templateId;

    @NotBlank(message = "资源类型 不能为空")
    private String resourceType;

    @NotBlank(message = "所属空间 不能为空")
    private String // Belonging space
    spaceId;

    // Private String templateUrl// Institutional Domain Name
    // Private String releaseUrl// Activation code
    private List<ResourceDo> resourceDoList;

    /**
     * Data type 0 Unstructured (with file) 1 Structured (representing grid) 2 (file+table)
     */
    private int dataType;

    private List<RequestFile> fileList;

    private List<String> tableList;

    private String plan;
}
