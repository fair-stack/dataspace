package cn.cnic.dataspace.api.datax.admin.controller;

import cn.cnic.dataspace.api.util.CommonUtils;
import cn.cnic.dataspace.api.util.Constants;
import cn.cnic.dataspace.api.util.JwtTokenUtils;
import cn.cnic.dataspace.api.util.Token;
import com.baomidou.mybatisplus.extension.api.ApiController;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * base controller
 */
public class BaseController extends ApiController {

    @Resource
    private JwtTokenUtils jwtTokenUtils;

    public String getCurrentUserId(HttpServletRequest request) {
        Token user = jwtTokenUtils.getToken(request.getHeader("Authorization"));
        if (user == null) {
            user = jwtTokenUtils.getToken(CommonUtils.getUser(request, Constants.TOKEN));
        }
        return user.getUserId();
    }

    public Token getCurrentUser(HttpServletRequest request) {
        Token user = jwtTokenUtils.getToken(request.getHeader("Authorization"));
        if (user == null) {
            user = jwtTokenUtils.getToken(CommonUtils.getUser(request, Constants.TOKEN));
        }
        return user;
    }

    public String getCurrentUserEmail(HttpServletRequest request) {
        Token user = jwtTokenUtils.getToken(request.getHeader("Authorization"));
        if (user == null) {
            user = jwtTokenUtils.getToken(CommonUtils.getUser(request, Constants.TOKEN));
        }
        return user.getEmailAccounts();
    }

    public String getSpaceId(HttpServletRequest request) {
        String spaceId = request.getHeader("spaceId");
        return spaceId;
    }
}
