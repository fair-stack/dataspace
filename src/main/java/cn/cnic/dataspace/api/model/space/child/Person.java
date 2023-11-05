package cn.cnic.dataspace.api.model.space.child;

import cn.cnic.dataspace.api.model.user.ConsumerDO;
import cn.cnic.dataspace.api.util.Token;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Person
 *
 * @author wangCc
 * @date 2021-03-22 10:07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Person {

    private String personId;

    private String personName;

    private String email;

    private String avatar;

    public Person(ConsumerDO consumerDO) {
        this.personId = consumerDO.getId();
        this.personName = consumerDO.getName();
        this.email = consumerDO.getEmailAccounts();
        this.avatar = consumerDO.getAvatar();
    }

    public Person(Token token) {
        this.personId = token.getUserId();
        this.personName = token.getName();
        this.email = token.getEmailAccounts();
        this.avatar = token.getAvatar();
    }

    public Person(AuthorizationPerson person) {
        this.personId = person.getUserId();
        this.personName = person.getUserName();
        this.email = person.getEmail();
        this.avatar = person.getAvatar();
    }
}
