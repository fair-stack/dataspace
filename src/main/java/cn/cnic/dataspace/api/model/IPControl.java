package cn.cnic.dataspace.api.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

/**
 * IP control
 */
@Data
@Document(collection = "ip_control")
public class IPControl {

    @Id
    private String id;

    private String ip;

    // Policy restrictions on whitelist and blacklist systems
    private String type;

    private String remark;

    // creator
    private String creator;

    // Logical deletion 0 exists 1 deletion
    private int isDelete;

    private Date createTime;

    private Date deleteTime;
}
