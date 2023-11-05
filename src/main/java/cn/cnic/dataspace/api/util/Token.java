package cn.cnic.dataspace.api.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.List;

/**
 * Created by songdz on 2020/4/10.
 *
 * @author songdz
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Token implements Serializable {

    private String userId;

    private String name;

    private String emailAccounts;

    private String avatar;

    private String accessToken;

    private String refreshToken;

    private List<String> roles;
}
