package cn.cnic.dataspace.api.model.open;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
public class Parent {

    @NotNull(message = "appId不能为空")
    String appId;

    @NotNull(message = "请求参数的签名串不能为空")
    @ApiModelProperty(name = "sign", value = "请求参数的签名串")
    String sign;

    @NotNull(message = "发送请求的时间不能为空")
    @ApiModelProperty(name = "timestamp", value = "yyyy-MM-dd HH:mm:ss")
    String timestamp;

    @NotNull(message = "调用的接口版本不能为空")
    @ApiModelProperty(name = "version", value = "调用的接口版本，固定为：1.0")
    String version;
}
