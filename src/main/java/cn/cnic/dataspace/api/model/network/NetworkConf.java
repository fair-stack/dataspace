package cn.cnic.dataspace.api.model.network;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

/**
 * Netdisk
 */
@Data
@Document(collection = "network_conf")
public class NetworkConf {

    @Id
    private String id;

    private String appKey;

    private String secretKey;

    private String hongPage;

    private Boolean isOpen;

    private Date createTime;

    private Date lastUpdateTime;
}
