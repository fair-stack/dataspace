package cn.cnic.dataspace.api.model.user;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class ManualAddList {

    @NotNull(message = "单位不能为空")
    @ApiModelProperty(name = "org", value = "单位", example = "*****")
    private String org;

    @NotNull(message = "角色不能为空")
    @ApiModelProperty(name = "role", value = "角色", example = "*****")
    private String role;

    @NotNull(message = "用户信息不能为空")
    private List<Person> person;

    @Data
    public static class Person {

        @NotNull(message = "邮箱不能为空")
        @ApiModelProperty(name = "email", value = "邮箱", example = "*****")
        private String email;

        @NotNull(message = "姓名不能为空")
        @ApiModelProperty(name = "name", value = "姓名", example = "*****")
        private String name;
    }
}
