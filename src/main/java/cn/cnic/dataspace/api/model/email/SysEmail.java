package cn.cnic.dataspace.api.model.email;

import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
public class SysEmail {

    @NotNull(message = "邮件服务器 不能为空")
    private String host;

    @NotNull(message = "邮件服务器端口 不能为空")
    private int port;

    @NotNull(message = "邮箱账号 不能为空")
    private String username;

    @NotNull(message = "邮箱密码 不能为空")
    private String password;

    @NotNull(message = "邮箱协议 不能为空")
    private String protocol;

    private int upload;

    private String from;
}
