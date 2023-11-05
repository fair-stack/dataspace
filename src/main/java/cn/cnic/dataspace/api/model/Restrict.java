package cn.cnic.dataspace.api.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

/**
 * Restrictions on modifying passwords, forgetting password restrictions, publishing submission restrictions
 */
@Data
@Document(collection = "restrict")
public class Restrict {

    @Id
    private String id;

    // Email or IP
    private String main;

    // 0 Retrieve password, 1 Publish submission
    private int type;

    // Number of times used on that day
    private long count;

    private String date;

    private Boolean result;

    private Date createTime;
}
