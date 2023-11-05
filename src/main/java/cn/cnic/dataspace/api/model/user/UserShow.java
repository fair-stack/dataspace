package cn.cnic.dataspace.api.model.user;

import lombok.Data;
import org.springframework.data.annotation.Id;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Return fields for users with complete @ Description
 */
@Data
public class UserShow {

    @Id
    private String id;

    private String emailAccounts;

    private String name;

    private String englishName;

    private String avatar;

    private String orgChineseName;

    private String orgEnglishName;

    private String telephone;

    private String introduction;

    private List<String> roles;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private int state;

    private int disable;

    // Number of password errors - disabled
    private Boolean disablePwd;

    // Add Method
    private String addWay;
}
