package cn.cnic.dataspace.api.model.open;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

/**
 * API call request record
 */
@Data
@Document(collection = "interface_record")
public class InterfaceRecord {

    @Id
    private String id;

    private String ip;

    private String appId;

    private String path;

    private int accCount;

    private int year;

    private int month;

    private int day;

    private Date startTime;

    private Date lastTime;
}
