package cn.cnic.dataspace.api.model.manage;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

/**
 * Configure mailbox suffix recognition
 */
@Data
@Document(collection = "identify")
public class Identify {

    @Id
    private String id;

    private String suffix;

    private String description;

    private Date createTime;
}
