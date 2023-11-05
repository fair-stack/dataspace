package cn.cnic.dataspace.api.config.space;

import cn.cnic.dataspace.api.cacheLoading.CacheLoading;
import cn.cnic.dataspace.api.repository.SpaceRepository;
import cn.cnic.dataspace.api.repository.SvnSpaceLogRepository;
import cn.cnic.dataspace.api.repository.UserRepository;
import cn.cnic.dataspace.api.util.JwtTokenUtils;
import cn.cnic.dataspace.api.util.SpaceUrl;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

/**
 * FileOperationFactory
 * <p>
 * the factory to instance bean, aimed at injecting bean in object new created
 *
 * @author wangCc
 * @date 2021-07-09 11:15
 */
@Component
public class FileOperationFactory implements ApplicationContextAware {

    private static SvnSpaceLogRepository svnSpaceLogRepository;

    private static UserRepository userRepository;

    private static JwtTokenUtils jwtTokenUtils;

    private static SpaceRepository spaceRepository;

    private static MessageSource messageSource;

    private static SpaceControlConfig spaceControlConfig;

    private static SpaceUrl spaceUrl;

    private static FileMappingManage fileMappingManage;

    private static CacheLoading cacheLoading;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        svnSpaceLogRepository = applicationContext.getBean(SvnSpaceLogRepository.class);
        jwtTokenUtils = applicationContext.getBean(JwtTokenUtils.class);
        userRepository = applicationContext.getBean(UserRepository.class);
        spaceRepository = applicationContext.getBean(SpaceRepository.class);
        messageSource = applicationContext.getBean(MessageSource.class);
        spaceControlConfig = applicationContext.getBean(SpaceControlConfig.class);
        spaceUrl = applicationContext.getBean(SpaceUrl.class);
        fileMappingManage = applicationContext.getBean(FileMappingManage.class);
        cacheLoading = applicationContext.getBean(CacheLoading.class);
    }

    public static SvnSpaceLogRepository getSvnSpaceLogRepository() {
        return svnSpaceLogRepository;
    }

    public static UserRepository getUserRepository() {
        return userRepository;
    }

    public static SpaceRepository getSpaceRepository() {
        return spaceRepository;
    }

    public static JwtTokenUtils getJwtTokenUtils() {
        return jwtTokenUtils;
    }

    public static MessageSource getMessageSource() {
        return messageSource;
    }

    public static SpaceControlConfig getSpaceStatisticConfig() {
        return spaceControlConfig;
    }

    public static SpaceUrl getSpaceUrl() {
        return spaceUrl;
    }

    public static FileMappingManage getFileMappingManage() {
        return fileMappingManage;
    }

    public static CacheLoading getCacheLoading() {
        return cacheLoading;
    }
}
