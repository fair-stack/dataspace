package cn.cnic.dataspace.api.elfinder.service;

import cn.cnic.dataspace.api.cacheLoading.CacheLoading;
import cn.cnic.dataspace.api.elfinder.config.ElFinderConstants;
import cn.cnic.dataspace.api.elfinder.config.ElfinderConfiguration;
import cn.cnic.dataspace.api.elfinder.core.Volume;
import cn.cnic.dataspace.api.elfinder.core.VolumeSecurity;
import cn.cnic.dataspace.api.elfinder.core.impl.DefaultVolumeSecurity;
import cn.cnic.dataspace.api.elfinder.core.impl.NIO2FileSystemVolume;
import cn.cnic.dataspace.api.elfinder.core.impl.SecurityConstraint;
import cn.cnic.dataspace.api.elfinder.param.Node;
import cn.cnic.dataspace.api.elfinder.service.impl.DefaultElfinderStorage;
import cn.cnic.dataspace.api.elfinder.service.impl.DefaultElfinderStorageFactory;
import cn.cnic.dataspace.api.elfinder.support.locale.LocaleUtils;
import cn.cnic.dataspace.api.exception.CommonException;
import cn.cnic.dataspace.api.model.space.SpaceSimple;
import cn.cnic.dataspace.api.model.harvest.ShareLink;
import cn.cnic.dataspace.api.model.space.Space;
import cn.cnic.dataspace.api.model.space.child.AuthorizationPerson;
import cn.cnic.dataspace.api.util.CommonUtils;
import cn.cnic.dataspace.api.util.Constants;
import cn.cnic.dataspace.api.util.JwtTokenUtils;
import cn.cnic.dataspace.api.util.Token;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import sun.misc.BASE64Decoder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.*;

/**
 * Elfinder Storage Service
 *
 * @author wangcc
 * @date 2021-3-26 14:34:42
 */
@Service
public class ElfinderStorageService {

    @Autowired
    private ElfinderConfiguration elfinderConfiguration;

    @Autowired
    private JwtTokenUtils jwtTokenUtils;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private CacheLoading cacheLoading;

    private final BASE64Decoder decoder = new BASE64Decoder();

    @SneakyThrows
    public ElfinderStorageFactory getElfinderStorageFactory(HttpServletRequest request, HttpServletResponse response) {
        DefaultElfinderStorageFactory elfinderStorageFactory = new DefaultElfinderStorageFactory();
        String spaceId = request.getParameter("spaceId");
        String url = request.getParameter("url");
        String linkId = request.getParameter("linkId");
        if (StringUtils.isNotBlank(spaceId)) {
            request.getSession().setAttribute("spaceId", spaceId);
        } else if (StringUtils.isNotBlank(url)) {
            spaceId = url;
            request.getSession().setAttribute("spaceId", url);
        } else if (StringUtils.isNotBlank(linkId)) {
            String link = new String(decoder.decodeBuffer(linkId), "UTF-8");
            Query query = new Query().addCriteria(Criteria.where("link").is(link));
            ShareLink shareLink = mongoTemplate.findOne(query, ShareLink.class);
            spaceId = shareLink.getSpaceId();
            request.getSession().setAttribute("spaceId", spaceId);
        } else {
            final Object spaceId1 = request.getSession().getAttribute("spaceId");
            if (!Objects.isNull(spaceId1)) {
                spaceId = spaceId1.toString();
            }
        }
        // if (mongoTemplate.find(new Query().addCriteria(new Criteria().orOperator(
        // Criteria.where("spaceId").is(spaceId), Criteria.where("homeUrl").is(spaceId))), Space.class).get(0).getIsPublic() != 1) {
        // if (!spaceService.isSpaceMember(spaceId, jwtTokenUtils.getUserIdFromToken(jwtTokenUtils.getToken(request)))) {
        // response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        // Map<String, Object> result = new HashMap<>(16);
        // result.put("code", "403");
        // result.put("data", CommonUtils.messageInternational("PERMISSION_DENIED"));
        // response.getWriter().write(msgUtil.mapToString(result));
        // }
        // }
        elfinderStorageFactory.setElfinderStorage(getElfinderStorage(request, spaceId));
        return elfinderStorageFactory;
    }

    public ElfinderStorage getElfinderStorage(HttpServletRequest request, String spaceId) {
        DefaultElfinderStorage defaultElfinderStorage = new DefaultElfinderStorage();
        // creates volumes, volumeIds, volumeLocale and volumeSecurities
        char defaultVolumeId = 'A';
        List<Node> elfinderConfigurationVolumes = elfinderConfiguration.getVolumes();
        List<Volume> elfinderVolumes = new ArrayList<>(elfinderConfigurationVolumes.size());
        Map<Volume, String> elfinderVolumeIds = new HashMap<>(elfinderConfigurationVolumes.size());
        Map<Volume, Locale> elfinderVolumeLocales = new HashMap<>(elfinderConfigurationVolumes.size());
        List<VolumeSecurity> elfinderVolumeSecurities = new ArrayList<>();
        String authToken = jwtTokenUtils.getToken(request);
        // creates volumes
        for (Node elfinderConfigurationVolume : elfinderConfigurationVolumes) {
            final List<Space> spaces = mongoTemplate.find(new Query(new Criteria().orOperator(Criteria.where("spaceId").is(spaceId), Criteria.where("homeUrl").is(spaceId))), Space.class);
            if (spaces.size() == 0) {
                throw new CommonException(500, CommonUtils.messageInternational("RESOURCE_DOES_NOT_EXIST"));
            }
            Space space = spaces.get(0);
            if (StringUtils.equals(space.getState(), "2")) {
                throw new CommonException(500, CommonUtils.messageInternational("SPACE_OFFLINE_FORBIDDEN"));
            }
            // if (StringUtils.equals(space.getHomeUrl(), spaceId)) {
            // if (space.getIsPublic() != 1) {
            // throw new CommonException(404, CommonUtils.messageInternational("SPACE_PUBLIC_FORBIDDEN"));
            // }
            // }
            final String path = space.getFilePath();
            final String alias = space.getSpaceName();
            final String locale = elfinderConfigurationVolume.getLocale();
            final boolean isLocked = elfinderConfigurationVolume.getConstraint().isLocked();
            final boolean isReadable = elfinderConfigurationVolume.getConstraint().isReadable();
            final boolean isWritable = elfinderConfigurationVolume.getConstraint().isWritable();
            // creates new volume
            Volume volume = new NIO2FileSystemVolume(alias, new File(path).toPath());
            elfinderVolumes.add(volume);
            elfinderVolumeIds.put(volume, Character.toString(defaultVolumeId));
            elfinderVolumeLocales.put(volume, LocaleUtils.toLocale(locale));
            // creates security constraint
            SecurityConstraint securityConstraint = new SecurityConstraint();
            securityConstraint.setLocked(isLocked);
            if (StringUtils.isBlank(authToken)) {
                securityConstraint.setLocked(true);
                securityConstraint.setWritable(isReadable);
                securityConstraint.setReadable(isWritable);
            } else {
                boolean flag = false;
                securityConstraint.setReadable(true);
                securityConstraint.setLocked(true);
                securityConstraint.setWritable(isWritable);
                Token utilsToken = jwtTokenUtils.getToken(authToken);
                String userId = utilsToken.getUserId();
                boolean judge = utilsToken.getRoles().contains(Constants.ADMIN);
                if (StringUtils.equals(space.getUserId(), userId) || judge) {
                    flag = true;
                } else {
                    for (AuthorizationPerson authorizationPerson : space.getAuthorizationList()) {
                        if (StringUtils.equals(authorizationPerson.getUserId(), userId) || judge) {
                            flag = true;
                            break;
                        }
                    }
                }
                if (flag) {
                    securityConstraint.setLocked(false);
                    securityConstraint.setReadable(true);
                    securityConstraint.setWritable(true);
                }
            }
            // creates volume pattern and volume security
            final String volumePattern = defaultVolumeId + ElFinderConstants.ELFINDER_VOLUME_SERCURITY_REGEX;
            elfinderVolumeSecurities.add(new DefaultVolumeSecurity(volumePattern, securityConstraint));
            // prepare next volumeId character
            defaultVolumeId++;
        }
        defaultElfinderStorage.setVolumes(elfinderVolumes);
        defaultElfinderStorage.setVolumeIds(elfinderVolumeIds);
        defaultElfinderStorage.setVolumeLocales(elfinderVolumeLocales);
        defaultElfinderStorage.setVolumeSecurities(elfinderVolumeSecurities);
        return defaultElfinderStorage;
    }

    /**
     * Revise - Replace
     */
    public ElfinderStorage getElfinderStorageSimple(String spaceId) {
        DefaultElfinderStorage defaultElfinderStorage = new DefaultElfinderStorage();
        char defaultVolumeId = 'A';
        List<Node> elfinderConfigurationVolumes = elfinderConfiguration.getVolumes();
        List<Volume> elfinderVolumes = new ArrayList<>(elfinderConfigurationVolumes.size());
        Map<Volume, String> elfinderVolumeIds = new HashMap<>(elfinderConfigurationVolumes.size());
        Map<Volume, Locale> elfinderVolumeLocales = new HashMap<>(elfinderConfigurationVolumes.size());
        List<VolumeSecurity> elfinderVolumeSecurities = new ArrayList<>();
        // creates volumes
        for (Node elfinderConfigurationVolume : elfinderConfigurationVolumes) {
            SpaceSimple space = cacheLoading.getSpaceSimple(spaceId);
            if (space == null) {
                throw new RuntimeException("空间未找到!");
            }
            final String path = space.getFilePath();
            final String alias = space.getSpaceName();
            final String locale = elfinderConfigurationVolume.getLocale();
            final boolean isLocked = elfinderConfigurationVolume.getConstraint().isLocked();
            final boolean isReadable = elfinderConfigurationVolume.getConstraint().isReadable();
            final boolean isWritable = elfinderConfigurationVolume.getConstraint().isWritable();
            // creates new volume
            Volume volume = new NIO2FileSystemVolume(alias, new File(path).toPath());
            elfinderVolumes.add(volume);
            elfinderVolumeIds.put(volume, Character.toString(defaultVolumeId));
            elfinderVolumeLocales.put(volume, LocaleUtils.toLocale(locale));
            // creates security constraint
            SecurityConstraint securityConstraint = new SecurityConstraint();
            securityConstraint.setLocked(isLocked);
            securityConstraint.setLocked(true);
            securityConstraint.setWritable(isReadable);
            securityConstraint.setReadable(isWritable);
            final String volumePattern = defaultVolumeId + ElFinderConstants.ELFINDER_VOLUME_SERCURITY_REGEX;
            elfinderVolumeSecurities.add(new DefaultVolumeSecurity(volumePattern, securityConstraint));
            defaultVolumeId++;
        }
        defaultElfinderStorage.setVolumes(elfinderVolumes);
        defaultElfinderStorage.setVolumeIds(elfinderVolumeIds);
        defaultElfinderStorage.setVolumeLocales(elfinderVolumeLocales);
        defaultElfinderStorage.setVolumeSecurities(elfinderVolumeSecurities);
        return defaultElfinderStorage;
    }
}
