package cn.cnic.dataspace.api.model.user;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Map;

/**
 * @ Description Email Notification Configuration
 */
@Data
@Document(collection = "email_role")
public class UserEmailRole {

    @Id
    private String id;

    private String userId;

    private Map<String, Boolean> emailRole;
}
