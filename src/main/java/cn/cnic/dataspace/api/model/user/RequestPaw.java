package cn.cnic.dataspace.api.model.user;

import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
public class RequestPaw {

    private String origPwd;

    @NotNull(message = "新密码 不能为空")
    private String newPwd;

    @NotNull(message = "确认密码 不能为空")
    private String conPwd;
}
