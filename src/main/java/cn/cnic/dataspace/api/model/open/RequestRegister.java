package cn.cnic.dataspace.api.model.open;

import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
public class RequestRegister extends Parent {

    @NotNull(message = "邮箱账号不能为空")
    String emailAccounts;

    @NotNull(message = "姓名不能为空")
    String name;

    String password;

    @NotNull(message = "角色不能为空")
    String // 
    role;

    @NotNull(message = "单位不能为空")
    String org;
}
