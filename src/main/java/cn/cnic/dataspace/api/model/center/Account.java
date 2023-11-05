package cn.cnic.dataspace.api.model.center;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Data
@Document(collection = "center_account")
public class Account {

    @Id
    private String id;

    private String account;

    private String password;

    private Boolean isOpen;

    private String orgName;

    private Date createTime;

    private Date lastUpdateTime;
}
