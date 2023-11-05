package cn.cnic.dataspace.api.model.backup;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

/**
 * FTP account information for disaster recovery
 */
@Data
@Document(collection = "ftp_host")
public class FtpHost {

    @Id
    private String id;

    private String host;

    private String port;

    private String path;

    private String username;

    private String password;

    private String founder;

    private Boolean invoke;

    private Date lastUpdateTime;

    private Date createTime;
}
