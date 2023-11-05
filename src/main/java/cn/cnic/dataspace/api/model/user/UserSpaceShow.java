package cn.cnic.dataspace.api.model.user;

import lombok.Data;
import org.springframework.data.annotation.Id;

/**
 * List return display
 */
@Data
public class UserSpaceShow {

    @Id
    private String userId;

    private String emailAccounts;

    private String name;

    private int state;

    private String stateShow;

    private String avatar;

    private boolean judge = false;

    public int getState() {
        return state;
    }

    public String getStateShow() {
        return state == 0 ? "未激活" : state == 1 ? "生效" : state == 2 ? "未注册" : state == 3 ? "禁用" : "其他";
    }
}
