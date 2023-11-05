package cn.cnic.dataspace.api.model.harvest;

import cn.cnic.dataspace.api.model.space.child.AuthorizationPerson;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Data
@Document(collection = "share_link")
public class ShareLink {

    @Id
    private String id;

    private String link;

    private String url;

    private String code;

    private String time;

    // No (no password), rand (random password), custom (custom)
    private String pasWay;

    private String password;

    // space、file
    private String type;

    private Object content;

    private String spaceId;

    private AuthorizationPerson founder;

    private long viewNum;

    private long dowNum;

    private int share;

    private String ftpUserId;

    private Date failureTime;

    private Date createTime;
}
