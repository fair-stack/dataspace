package cn.cnic.dataspace.api.model.user;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @ Description Character Model
 */
@Data
@Document(collection = "role")
public class RoleDO {

    @Id
    String roleId;

    /**
     * Role Name
     */
    String name;

    /**
     * Role identification
     */
    String logo;

    /**
     * Remarks
     */
    String remarks;

    /**
     * Permission Path
     */
    List<String> pathList;

    /**
     * Creation time
     */
    LocalDateTime createTime;

    /**
     * Modification time
     */
    LocalDateTime updateTime;

    public RoleDO() {
    }

    public RoleDO(String name, String logo, String remarks, List<String> pathList, LocalDateTime createTime) {
        this.name = name;
        this.logo = logo;
        this.remarks = remarks;
        this.pathList = pathList;
        this.createTime = createTime;
    }
}
