package cn.cnic.dataspace.api.model.network;

import org.springframework.stereotype.Component;

@Component
public class NetworkUrl {

    private final static String authUrl = "https://openapi.baidu.com/oauth/2.0/authorize?";

    private final static String tokenUrl = "https://openapi.baidu.com/oauth/2.0/token?";

    private final static String userUrl = "https://pan.baidu.com/rest/2.0/xpan/nas?method=uinfo&";

    private final static String fileUrl = "https://pan.baidu.com/rest/2.0/xpan/file?";

    private final static String fileDetailUrl = "https://pan.baidu.com/rest/2.0/xpan/multimedia?method=filemetas&dlink=1&";

    public static String getAuthUrl(String appKey, String netCallbackUrl, String token) {
        return // Fixed type
        // Assigned appkey
        // token url
        // Fixed parameter, must be basic, netdisk
        // How to return to page opening
        authUrl + "response_type=code&" + "client_id=" + appKey + "&" + "redirect_uri=" + netCallbackUrl + "&" + "scope=netdisk&" + "display=dialog&" + "qrcode=1&" + "force_login=1&" + "state=" + token;
    }

    public static String getTokenUrl(String code, String netCallbackUrl, String appKey, String secretKey) {
        return tokenUrl + "grant_type=authorization_code&" + "code=" + code + "&" + "client_id=" + appKey + "&" + "client_secret=" + secretKey + "&" + "redirect_uri=" + netCallbackUrl;
    }

    public static String getRefreshTokenUrl(String refreshToken, String appKey, String secretKey) {
        return tokenUrl + "grant_type=refresh_token&" + "refresh_token=" + refreshToken + "&" + "client_id=" + appKey + "&" + "client_secret=" + secretKey;
    }

    public static String getUserUrl(String token) {
        return userUrl + "access_token=" + token;
    }

    public static String getFileUrl(String token, String path) {
        return fileUrl + "access_token=" + token + "&" + "method=list&order=time&start=0&limit=1000&web=web&folder=0&desc=1&" + "dir=" + path;
    }

    public static String getFileDetailUrl(String token, String fsIds) {
        return fileDetailUrl + "access_token=" + token + "&fsids=" + fsIds;
    }
}
