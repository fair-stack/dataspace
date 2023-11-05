package cn.cnic.dataspace.api.model.user;

import cn.cnic.dataspace.api.model.IpInfo;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

/**
 * User login log
 */
@Data
@Document(collection = "user_login_log")
public class UserLoginLog {

    public UserLoginLog() {
    }

    public UserLoginLog(String userId, String email, String ip, IpInfo ipInfo, String loginType, String type) {
        this.userId = userId;
        this.email = email;
        this.ip = ip;
        if (null != ipInfo) {
            this.ipCountry = ipInfo.getCountry();
            this.ipProvince = ipInfo.getProvince();
            this.ipCity = ipInfo.getCity();
            this.ipIsp = ipInfo.getIsp();
        }
        this.loginType = loginType;
        this.type = type;
        this.time = new Date();
    }

    @Id
    private String id;

    private String userId;

    private String email;

    private String ip;

    private String ipCountry;

    private String ipProvince;

    private String ipCity;

    private String ipIsp;

    // WeChat System Technology Cloud Sharing Network
    private String loginType;

    // Login login logout logout
    private String type;

    private Date time;
}
