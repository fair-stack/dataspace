package cn.cnic.dataspace.api.model.space;

import cn.cnic.dataspace.api.util.SpaceRoleEnum;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Space Permissions Menu
 */
@Data
@Document(collection = "space_menu")
public class SpaceMenu implements Serializable {

    @Id
    private String id;

    private String cla;

    private List<Action> actionList;

    private Date createTime;

    @Data
    public static class Action {

        private String actionKey;

        private String actionName;

        private List<Role> roleList;

        public Action() {
        }

        public Action(SpaceRoleEnum spaceRoleEnum) {
            this.actionKey = spaceRoleEnum.getAction();
            this.actionName = spaceRoleEnum.getAction_desc();
        }
    }

    @Data
    public static class Role {

        private String roleKey;

        private String roleName;

        private boolean disable = false;

        public Role() {
        }

        public Role(SpaceRoleEnum spaceRoleEnum) {
            this.roleKey = spaceRoleEnum.getRole();
            this.roleName = spaceRoleEnum.getRole_desc();
        }
    }
}
