package cn.cnic.dataspace.api.model.space;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.io.Serializable;

/**
 * space share
 *
 * @author wangCc
 * @date 2021-03-22 16:35
 */
@Data
@Builder
@Document(collection = "share")
public class SpaceShare implements Serializable {

    @Id
    private String shareId;

    // Link Owner
    private String userId;

    private String spaceId;

    private String spaceName;

    // Creation time
    private String createDate;

    // Expiration time
    private String expireDate;

    // Sharing connection status 1 is valid and 0 is invalid
    private Integer status;
}
