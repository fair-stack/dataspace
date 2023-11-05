package cn.cnic.dataspace.api.model.space;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
public class SpaceRoleRequest {

    @NotBlank(message = "spaceId 不能为空")
    private String spaceId;

    @NotBlank(message = "roleName 不能为空")
    private String roleName;

    private List<String> roles;
}
