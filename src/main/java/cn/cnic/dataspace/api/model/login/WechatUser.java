package cn.cnic.dataspace.api.model.login;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

/**
 * WeChat user binding information
 */
@Data
@Document(collection = "wechat_user")
public class WechatUser {

    @Id
    private String id;

    private String unionId;

    private String sex;

    private String province;

    private String city;

    private String country;

    private String realName;

    private String openId;

    private String headImgUrl;

    private Date lastUpdateTime;

    private Date createTime;
}
