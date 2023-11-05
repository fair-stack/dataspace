package cn.cnic.dataspace.api.model.harvest;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Data
@Document(collection = "ftp_short")
public class FTPShort {

    @Id
    private String id;

    private String userId;

    private String spaceId;

    private String shortChain;

    private Date createTime;
}
