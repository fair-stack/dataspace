package cn.cnic.dataspace.api.model.harvest;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Data
@Document(collection = "ftp_user")
public class FtpUser {

    @Id
    private String id;

    private String username;

    private String password;

    private Date createTime;
}
