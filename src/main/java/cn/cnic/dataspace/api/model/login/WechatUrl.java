package cn.cnic.dataspace.api.model.login;

import org.springframework.stereotype.Component;

/**
 * WeChat login management
 */
@Component
public class WechatUrl {

    private final static String authUrl = "https://open.weixin.qq.com/connect/qrconnect?";

    public final static String tokenUrl = "https://api.weixin.qq.com/sns/oauth2/access_token?";

    private final static String userUrl = "https://api.weixin.qq.com/sns/userinfo?";

    public static String getAuthUrl(String appId, String callbackUrl, String state) {
        return // Assigned appId
        // token url
        // Fixed parameter, must be basic, netdisk
        // How to return to page opening
        authUrl + "appid=" + appId + "&" + "redirect_uri=" + callbackUrl + "&" + "scope=snsapi_login&" + "response_type=code&" + "state=" + state;
    }

    public static String getTokenUrl(String appId, String code, String secretKey) {
        return "appid=" + appId + "&" + "code=" + code + "&" + "secret=" + secretKey + "&" + "grant_type=authorization_code";
    }

    public static String getUserInfoUrl(String openId, String accToken) {
        return userUrl + "access_token=" + accToken + "&" + "openid=" + openId + "&" + "lang=zh_CN";
    }
}
