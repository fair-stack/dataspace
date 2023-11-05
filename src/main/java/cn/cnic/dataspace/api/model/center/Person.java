package cn.cnic.dataspace.api.model.center;

import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
public class Person {

    // For updating
    private String id;

    @NotNull(message = "中文名不能为空")
    private String zh_Name;

    @NotNull(message = "机构不能为空")
    private String orgId;

    @NotNull(message = "邮箱不能为空")
    private String email;

    // English name
    private String en_Name;

    // Professional title
    private String title;

    // educational background
    private String education;

    // orcid
    private String orcid;

    // Contact number
    private String telephone;

    // Permission account
    private String account;

    // password
    private String password;
}
