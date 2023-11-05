package cn.cnic.dataspace.api.model.center;

import lombok.Data;
import javax.validation.constraints.NotNull;

/**
 * Institutional entities corresponding to the main center
 */
@Data
public class Org {

    // For updating
    private String id;

    @NotNull(message = "中文名不能为空")
    private String // Chinese name
    zh_Name;

    @NotNull(message = "机构地址不能为空")
    private String // Institution address
    address;

    @NotNull(message = "机构性质不能为空")
    private String // Institutional nature
    nature;

    private String logo;

    private String logoBase64;

    private String logoSuffix;

    // English name
    private String en_Name;

    // Institution code
    private String number;

    // Institutional Introduction
    private String description;

    // Contact number
    private String telephone;

    // Contact email
    private String email;

    // Permission account
    private String account;

    // password
    private String password;
}
