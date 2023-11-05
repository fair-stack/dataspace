package cn.cnic.dataspace.api.model.network;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

/**
 * Network disk user information
 */
@Data
@Document(collection = "network_user")
public class NetworkUser {

    @Id
    private String id;

    private String avatar_url;

    private String baidu_name;

    private String netdisk_name;

    private String acc_token;

    private String ref_token;

    private Date lastUpdateTime;

    private Date createTime;
}
