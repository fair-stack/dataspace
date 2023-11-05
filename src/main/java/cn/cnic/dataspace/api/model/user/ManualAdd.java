package cn.cnic.dataspace.api.model.user;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Administrator adds user model
 */
@Data
public class ManualAdd {

    @NotNull(message = "邮箱账号不能为空")
    @ApiModelProperty(name = "emailAccounts", value = "邮箱账号", example = "admin", required = true)
    String emailAccounts;

    @NotNull(message = "请输入姓名")
    @ApiModelProperty(name = "name", value = "姓名", example = "张**", required = true)
    String name;

    @NotNull(message = "请设置角色!")
    @ApiModelProperty(name = "roles", value = "角色logo")
    List<String> roles;

    @NotNull(message = "请输入单位!")
    @ApiModelProperty(name = "org", value = "单位", example = "****")
    String org;

    private String userId;
}
