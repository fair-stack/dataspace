package cn.cnic.dataspace.api.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

/**
 * Access Records
 */
@Data
@Document(collection = "access_record")
public class AccessRecord {

    @Id
    private String accessId;

    private String accessIP;

    private String email;

    private int accCount;

    private int year;

    private int month;

    private int day;

    private Date accessTime;

    private Date lastTime;
}
