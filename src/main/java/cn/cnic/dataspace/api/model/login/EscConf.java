package cn.cnic.dataspace.api.model.login;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

/**
 * Shared Network
 */
@Data
@Document(collection = "escience_conf")
public class EscConf {

    @Id
    private String id;

    private String clientId;

    private String clientSecret;

    private String hongPage;

    private Boolean isOpen;

    private Date createTime;

    private Date lastUpdateTime;
}
