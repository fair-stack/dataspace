package cn.cnic.dataspace.api.model.space.child;

import cn.cnic.dataspace.api.model.user.ConsumerDO;
import cn.cnic.dataspace.api.util.Token;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Operator
 *
 * @author wangCc
 * @date 2021-04-13 11:01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Operator {

    public Operator(ConsumerDO consumerDO) {
        this.personId = consumerDO.getId();
        this.personName = consumerDO.getName();
        this.email = consumerDO.getEmailAccounts();
        this.avatar = consumerDO.getAvatar();
    }

    public Operator(Token token) {
        this.personId = token.getUserId();
        this.personName = token.getName();
        this.email = token.getEmailAccounts();
        this.avatar = token.getAvatar();
    }

    private String personId;

    private String personName;

    private String email;

    private String avatar;
}
