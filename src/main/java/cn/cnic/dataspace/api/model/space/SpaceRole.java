package cn.cnic.dataspace.api.model.space;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;
import java.util.List;

/**
 * Space permissions
 */
@Data
@Document(collection = "space_role")
public class SpaceRole {

    @Id
    private String id;

    private String spaceId;

    private String roleName;

    private List<String> menus;

    private Date updateTime;

    private Date createTime;
}
