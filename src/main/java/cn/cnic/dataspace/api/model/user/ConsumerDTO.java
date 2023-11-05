package cn.cnic.dataspace.api.model.user;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import javax.validation.constraints.NotNull;

/**
 * @ Description User Model Transfer Object
 */
@Data
public class ConsumerDTO {

    @NotNull(message = "邮箱账号不能为空")
    @ApiModelProperty(name = "emailAccounts", value = "邮箱账号", example = "admin", required = true)
    String emailAccounts;

    @NotNull(message = "姓名不能为空")
    @ApiModelProperty(name = "name", value = "姓名", example = "张**", required = true)
    String name;

    @NotNull(message = "密码不能为空")
    @ApiModelProperty(name = "password", value = "密码", example = "123456")
    String password;

    @NotNull(message = "确认密码不能为空")
    @ApiModelProperty(name = "confirmPassword", value = "密码", example = "123456")
    String confirmPassword;

    @NotNull(message = "单位不能为空")
    @ApiModelProperty(name = "org", value = "机构", example = "*****")
    String org;

    @ApiModelProperty(name = "code", value = "邀请用户标识", example = "code")
    String code;

    @NotNull(message = "验证码不能为空")
    String verificationCode;
}
