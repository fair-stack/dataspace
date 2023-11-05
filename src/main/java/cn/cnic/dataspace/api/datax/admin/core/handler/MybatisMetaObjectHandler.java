package cn.cnic.dataspace.api.datax.admin.core.handler;

import cn.cnic.dataspace.api.datax.admin.util.ServletUtils;
import cn.cnic.dataspace.api.util.JwtTokenUtils;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;

/**
 * Universal field filling, such as automatic filling of fields such as createBy createDate
 */
@Component
@Slf4j
public class MybatisMetaObjectHandler implements MetaObjectHandler {

    @Resource
    private JwtTokenUtils jwtTokenUtils;

    @Override
    public void insertFill(MetaObject metaObject) {
        setFieldValByName("createDate", new Date(), metaObject);
        setFieldValByName("createBy", getCurrentUser(), metaObject);
        setFieldValByName("updateDate", new Date(), metaObject);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        setFieldValByName("updateDate", new Date(), metaObject);
        // setFieldValByName("updateBy", getCurrentUser(), metaObject);
    }

    private String getCurrentUser() {
        HttpServletRequest request = ServletUtils.getRequest();
        String token = jwtTokenUtils.getToken(request);
        String userIdFromToken = jwtTokenUtils.getUserIdFromToken(token);
        return userIdFromToken;
    }
}
