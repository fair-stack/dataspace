package cn.cnic.dataspace.api.model.login;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

/**
 * WeChat configuration
 */
@Data
@Document(collection = "wechat_conf")
public class WechatConf {

    @Id
    private String id;

    private String appId;

    private String secretKey;

    private String hongPage;

    private Boolean isOpen;

    private Date createTime;

    private Date lastUpdateTime;
}
