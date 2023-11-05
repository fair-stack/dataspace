package cn.cnic.dataspace.api.util;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Data
public class SpaceUrl {

    /*System version*/
    @Value("${dataspace.version}")
    private String version;

    /*FTP configuration information*/
    @Value("${ftp.port}")
    private String port;

    @Value("${ftp.show}")
    private String show;

    @Value("${ftp.transmitSt}")
    private String transmitSt;

    @Value("${ftp.transmitEnd}")
    private String transmitEnd;

    @Value("${ftp.timeOut}")
    private String timeOut;

    @Value("${ftp.bufferSize}")
    private String bufferSize;

    @Value("${file.rootDir}")
    private String rootDir;

    /*Installation component address*/
    @Value("${file.install_component_source}")
    private String installComSource;

    @Value("${file.install_component_web}")
    private String installComWeb;

    @Value("${file.space_log_path}")
    private String spaceLogPath;

    /*call*/
    @Value("${call.webRegister}")
    private String webRegister;

    @Value("${call.webLogin}")
    private String webLogin;

    @Value("${call.successUrl}")
    private String successUrl;

    @Value("${call.emailActivation}")
    private String emailActivation;

    @Value("${call.getFtp}")
    private String getFtp;

    @Value("${call.harCon}")
    private String harCon;

    @Value("${call.upPwdUrl}")
    private String upPwdUrl;

    @Value("${call.auditUrl}")
    private String auditUrl;

    @Value("${call.spaceDetailUrl}")
    private String spaceDetailUrl;

    @Value("${call.resourceUrl}")
    private String resourceUrl;

    @Value("${call.umpWork}")
    private String umpWork;

    @Value("${acc.host}")
    private String accHost;

    @Value("${acc.port}")
    private String accPort;

    @Value("${call.spaceUrl}")
    private String spaceUrl;

    @Value("${call.webUrl}")
    private String webUrl;

    @Value("${ftp.ftpHost}")
    private String ftpHost;

    @Value("${call.spaceApplyUrl}")
    private String spaceApplyUrl;

    @Value("${call.applyUrl}")
    private String applyUrl;

    @Value("${call.publicUrl}")
    private String publicUrl;

    @Value("${call.shareSpaceUrl}")
    private String shareSpaceUrl;

    @Value("${call.shareFileUrl}")
    private String shareFileUrl;

    /*Publish File Storage*/
    @Value("${file.releaseStored}")
    private String releaseStored;

    /*Address of the main center*/
    @Value("${call.centerHost}")
    private String centerHost;

    @Value("${call.callbackUrl}")
    private String callbackUrl;

    @Value("${call.resourceUpdateUrl}")
    private String resourceUpdateUrl;

    @Value("${call.fairman_market_url}")
    private String fairManMarketUrl;

    @Value("${call.fairman_dataSend_url}")
    private String fairManDataSendUrl;

    /*Baidu Netdisk*/
    @Value("${call.netCallbackUrl}")
    private String netCallbackUrl;

    @Value("${call.netLoginSuccess}")
    private String netLoginSuccess;

    /*WeChat*/
    @Value("${call.wechatCallbackUrl}")
    private String wechatCallbackUrl;

    @Value("${call.wechatBindingUrl}")
    private String wechatBindingUrl;

    @Value("${call.wechatConfUrl}")
    private String wechatConfUrl;

    /*Shared Network*/
    @Value("${call.escienceCallbackUrl}")
    private String escienceCallbackUrl;

    /*Technology Cloud*/
    @Value("${call.casLoginUrl}")
    private volatile String casLoginUrl;

    @Value("${call.casLogoutUrl}")
    private volatile String casLogoutUrl;

    @Value("${call.authUrl}")
    private volatile String authUrl;

    @Value("${call.authParam}")
    private volatile String authParam;

    @Value("${call.umtCallbackUrl}")
    private volatile String umtCallbackUrl;

    private String callHost;

    /*interactive*/
    @Value("${call.getTokenUrl}")
    private String getTokenUrl;

    @Value("${swagger.enable}")
    private Boolean swaggerEnable;

    public String getFtpHost() {
        if (ftpHost == null) {
            return "localhost";
        } else {
            String ftp = "";
            if (ftpHost.contains("https://")) {
                ftp = ftpHost.replaceAll("https://", "");
            } else if (ftpHost.contains("http://")) {
                ftp = ftpHost.replaceAll("http://", "");
            } else {
                ftp = ftpHost;
            }
            String substring = ftp.substring(ftp.length() - 1);
            if (substring.equals("/")) {
                return ftp.substring(0, ftp.length() - 1);
            }
            return ftp;
        }
    }

    public String getCallHost() {
        String url = "";
        if (accHost == null) {
            url = "localhost";
        } else {
            url = accHost;
        }
        if (!url.contains("https") && !url.contains("http")) {
            url = "http://" + url;
        }
        String substring = url.substring(url.length() - 1);
        if (substring.equals("/")) {
            url = url.substring(0, url.length() - 1);
        }
        if (StringUtils.isNotEmpty(accPort)) {
            int port = 0;
            try {
                port = Integer.parseInt(accPort);
            } catch (Exception e) {
            }
            if (url.contains("https")) {
                if (port != 443 && port != 80 && port != 0) {
                    url = url + ":" + port;
                }
            } else {
                if (port != 80 && port != 0) {
                    url = url + ":" + port;
                }
            }
        }
        return url;
    }

    public String getWebUrl() {
        if (!webUrl.equals("/")) {
            return getCallHost() + webUrl;
        } else {
            return getCallHost();
        }
    }

    public String getSpaceUrl() {
        return getWebUrl() + spaceUrl;
    }

    public String getWebRegister() {
        return getWebUrl() + webRegister;
    }

    public String getWebLogin() {
        return getWebUrl() + webLogin;
    }

    public String getSuccessUrl() {
        return getWebUrl() + successUrl;
    }

    public String getEmailActivation() {
        return getCallHost() + emailActivation;
    }

    public String getFtpApi() {
        return getCallHost() + getFtp;
    }

    public String getHarConApi() {
        return getCallHost() + harCon;
    }

    public String getNetCallbackUrl() {
        return netCallbackUrl;
    }

    public String getNetLoginSuccess() {
        return getWebUrl() + netLoginSuccess;
    }

    public String getCallbackUrl() {
        return getCallHost() + callbackUrl;
    }

    public String getResourceUpdateUrl() {
        return getCallHost() + resourceUpdateUrl;
    }

    public String getUpPwdUrl() {
        return getWebUrl() + upPwdUrl;
    }

    public String getAuditUrl() {
        return getWebUrl() + auditUrl;
    }

    public String getSpaceDetailUrl() {
        return getWebUrl() + spaceDetailUrl;
    }

    public String getResourceUrl() {
        return getWebUrl() + resourceUrl;
    }

    public String getUmpWork() {
        return getWebUrl() + umpWork;
    }

    public String getWechatBindingUrl() {
        return getWebUrl() + wechatBindingUrl;
    }

    public String getWechatConfUrl() {
        return getWebUrl() + wechatConfUrl;
    }

    public String getSpaceApplyUrl() {
        return getWebUrl() + spaceApplyUrl;
    }

    public String getShareSpaceUrl() {
        return getWebUrl() + shareSpaceUrl;
    }

    public String getShareFileUrl() {
        return getWebUrl() + shareFileUrl;
    }
}
