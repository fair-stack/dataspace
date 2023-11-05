package cn.cnic.dataspace.api.model.open;

import lombok.Data;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class RequestSpace extends Parent {

    @NotNull(message = "用户不能为空")
    private String userId;

    @NotNull(message = "空间名不能为空")
    private String spaceName;

    @NotNull(message = "描述不能为空")
    private String // Description of space document
    description;

    @NotNull(message = "封面不能为空")
    private String // Space logo
    spaceLogo;

    private String spaceCode;

    // Space - Document Label
    private List<String> tags;

    // Space size
    private Long spaceSize;

    @NotNull(message = "空间类型不能为空")
    private String // Space types private, limited, public
    type;
}
