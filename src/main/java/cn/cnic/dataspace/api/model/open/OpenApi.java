package cn.cnic.dataspace.api.model.open;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * API Entity
 */
@Data
@Document(collection = "open_api")
public class OpenApi {

    @Id
    private String id;

    private String name;

    private String method;

    private String path;

    private String version;

    private String state;

    private String desc;

    // Authorized quantity
    private int authNum;

    // Number of calls in the past week
    private int callNum;

    private List<ApiAuth> authApp;

    // Release time
    private Date publicTime;

    // Release time
    private Date CreateTime;
}
