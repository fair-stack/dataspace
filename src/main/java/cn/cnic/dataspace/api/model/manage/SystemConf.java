package cn.cnic.dataspace.api.model.manage;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

/**
 * System configuration (email, etc.)
 */
@Data
@Document(collection = "system_conf")
public class SystemConf {

    @Id
    private String id;

    private String type;

    private Object conf;

    private String lastModifier;

    private Date lastUpdateTime;

    private Date createTime;

    public SystemConf(String type, Object conf) {
        this.type = type;
        this.conf = conf;
        this.lastUpdateTime = new Date();
        this.createTime = new Date();
    }

    public SystemConf() {
    }
}
