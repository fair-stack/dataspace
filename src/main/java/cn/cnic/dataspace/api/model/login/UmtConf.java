package cn.cnic.dataspace.api.model.login;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

/**
 * Technology Cloud Account Configuration
 */
@Data
@Document(collection = "umt_conf")
public class UmtConf {

    @Id
    private String id;

    private String appKey;

    private String appSecret;

    private String hongPage;

    private Boolean isOpen;

    private Date createTime;

    private Date lastUpdateTime;
}
