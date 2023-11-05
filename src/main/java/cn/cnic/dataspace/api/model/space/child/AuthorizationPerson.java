package cn.cnic.dataspace.api.model.space.child;

import cn.cnic.dataspace.api.model.user.ConsumerDO;
import cn.cnic.dataspace.api.util.Token;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import static cn.cnic.dataspace.api.service.space.SpaceService.SPACE_GENERAL;

/**
 * authorization person
 *
 * @author wangCc
 * @date 2021-03-22 10:07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizationPerson {

    private String userId;

    private String userName;

    private String email;

    private String avatar;

    private String role;

    public AuthorizationPerson(ConsumerDO consumerDO) {
        this.userId = consumerDO.getId();
        this.userName = consumerDO.getName();
        this.email = consumerDO.getEmailAccounts();
        this.avatar = consumerDO.getAvatar();
        this.role = SPACE_GENERAL;
    }

    public AuthorizationPerson(ConsumerDO consumerDO, String role) {
        this.userId = consumerDO.getId();
        this.userName = consumerDO.getName();
        this.email = consumerDO.getEmailAccounts();
        this.avatar = consumerDO.getAvatar();
        this.role = role;
    }

    public AuthorizationPerson(Token consumerDO) {
        this.userId = consumerDO.getUserId();
        this.userName = consumerDO.getName();
        this.email = consumerDO.getEmailAccounts();
        this.avatar = consumerDO.getAvatar();
    }
}
