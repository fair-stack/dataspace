package cn.cnic.dataspace.api.ftp.authen;

import cn.cnic.dataspace.api.filehandle.Control;
import cn.cnic.dataspace.api.filehandle.ControlImpl;
import cn.cnic.dataspace.api.ftp.minimalftp.FTPConnection;
import cn.cnic.dataspace.api.ftp.minimalftp.api.IUserAuthenticator;
import cn.cnic.dataspace.api.model.harvest.FTPShort;
import cn.cnic.dataspace.api.model.space.Space;
import cn.cnic.dataspace.api.model.harvest.FtpUser;
import cn.cnic.dataspace.api.model.user.ConsumerDO;
import cn.cnic.dataspace.api.service.impl.UserServiceImpl;
import cn.cnic.dataspace.api.util.CaffeineUtil;
import cn.cnic.dataspace.api.util.RSAEncrypt;
import cn.cnic.dataspace.api.util.SpaceUrl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.io.File;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple user base which encodes passwords in MD5 (not really for security, it's just as an example)
 *
 * @author chl
 */
@Service
@Slf4j
public class UserbaseAuthenticator implements IUserAuthenticator {

    private final MongoTemplate mongoTemplate;

    private final SpaceUrl spaceUrl;

    public UserbaseAuthenticator(MongoTemplate mongoTemplate, SpaceUrl spaceUrl) {
        this.mongoTemplate = mongoTemplate;
        this.spaceUrl = spaceUrl;
    }

    @Override
    public boolean needsUsername(FTPConnection con) {
        return true;
    }

    @Override
    public boolean needsPassword(FTPConnection con, String username, InetAddress address) {
        return true;
    }

    @Override
    public Control<File> authenticate(FTPConnection con, InetAddress address, String username, String password) throws AuthException {
        // Check for a user with that username in the database
        // Socket socket = con.getSocket();
        // InetAddress inetAddress = socket.getInetAddress();
        // System.out.println(inetAddress.getHostAddress());
        // IP restrictions
        boolean ftp = CaffeineUtil.spaceFull(username);
        if (!ftp) {
            auth(username, password);
        } else {
            String pass = CaffeineUtil.getSpaceFull(username);
            if (StringUtils.isEmpty(password)) {
                throw new AuthException();
            } else if (!password.equals(RSAEncrypt.decrypt(pass))) {
                auth(username, password);
            }
        }
        String rootDir = spaceUrl.getRootDir();
        File path = new File(rootDir);
        return new ControlImpl(path, con);
    }

    public void auth(String emailAccounts, String password) {
        if (StringUtils.isEmpty(emailAccounts) || StringUtils.isEmpty(password)) {
            throw new AuthException();
        }
        Query query = new Query();
        query.addCriteria(Criteria.where("emailAccounts").is(emailAccounts));
        ConsumerDO user = null;
        try {
            user = mongoTemplate.findOne(query, ConsumerDO.class, UserServiceImpl.COLLECTION_NAME);
        } catch (Exception e) {
            e.printStackTrace();
            throw new AuthException();
        }
        boolean type = false;
        if (user == null) {
            FtpUser username = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("username").is(emailAccounts)), FtpUser.class);
            if (null == username) {
                throw new AuthException();
            }
            user = new ConsumerDO();
            user.setEmailAccounts(username.getUsername());
            user.setPassword(username.getPassword());
            user.setState(1);
            user.setId(username.getId());
            type = true;
        } else if (user.getState() != 1) {
            throw new AuthException();
        } else if (user.getDisable() != 0) {
            throw new AuthException();
        } else {
            String userPassword = user.getPassword();
            if (StringUtils.isEmpty(password) || !password.equals(RSAEncrypt.decrypt(userPassword))) {
                throw new AuthException();
            }
        }
        Map<String, String> shortMap = new HashMap<>(8);
        if (type) {
            Query query1 = new Query().addCriteria(Criteria.where("userId").is(user.getId()));
            List<FTPShort> ftpShorts = mongoTemplate.find(query1, FTPShort.class);
            for (FTPShort ftpShort : ftpShorts) {
                shortMap.put(ftpShort.getShortChain(), ftpShort.getSpaceId());
            }
        } else {
            Query query1 = new Query().addCriteria(Criteria.where("authorizationList.userId").is(user.getId()).and("state").is("1"));
            List<Space> spaceList = mongoTemplate.find(query1, Space.class);
            for (Space space : spaceList) {
                shortMap.put(space.getSpaceShort(), space.getSpaceId());
            }
        }
        CaffeineUtil.setSpaceFull(emailAccounts, user.getPassword());
        CaffeineUtil.setShortChain(emailAccounts, shortMap);
    }
}
