package cn.cnic.dataspace.api.service.impl;

import cn.cnic.dataspace.api.cacheLoading.CacheLoading;
import cn.cnic.dataspace.api.exception.CommonException;
import cn.cnic.dataspace.api.model.center.*;
import cn.cnic.dataspace.api.model.release.ResultData;
import cn.cnic.dataspace.api.service.ExternalInterService;
import cn.cnic.dataspace.api.util.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Slf4j
@Service
public class ExternalInterServiceImpl implements ExternalInterService {

    private final static String queryUrl = "/api/v1/findDataByTips";

    private final static String typeUrl = "/api/v1/findAllProjectTypeTree";

    private final static String[] queryType = { "Person", "Organization", "Project", "Paper" };

    @Autowired
    private SpaceUrl spaceUrl;

    @Autowired
    private CacheLoading cacheLoading;

    private final Cache<String, Map<String, String>> publicOrgUrl = CaffeineUtil.getPublicOrgUrl();

    private final Cache<String, String> publicModel = CaffeineUtil.getPublicModel();

    @Override
    public ResponseResult<Object> accessOrgList(String name, String type) {
        HttpClient httpClient = new HttpClient();
        String result = "";
        try {
            String url = "/api/v1/findPubDataByTips";
            if (StringUtils.isNotEmpty(name)) {
                url += "?key=" + name;
            }
            result = httpClient.doGetWayTwo(spaceUrl.getCenterHost() + url);
        } catch (Exception e) {
            log.info(e.getMessage());
            return ResultUtil.errorInternational("EXTERNAL_ORG_LIST");
        }
        Map resultMap = JSONObject.parseObject(result, Map.class);
        if (resultMap == null) {
            return ResultUtil.errorInternational("EXTERNAL_PARSE");
        }
        if (StringUtils.isEmpty(result)) {
            return ResultUtil.errorInternational("EXTERNAL_PARSE");
        }
        Object code = resultMap.get("code");
        Object success = resultMap.get("success");
        if (code == null || success == null) {
            return ResultUtil.errorInternational("EXTERNAL_PARSE");
        }
        if ((int) code != 200 || !(boolean) success) {
            return ResultUtil.success(new ArrayList<>(0));
            // return ResultUtil.error((String)resultMap.get("message"));
        }
        List<Map> result1 = (List<Map>) resultMap.get("result");
        List<String> publicOrg;
        if (!type.equals(Constants.ADMIN)) {
            publicOrg = (List) cacheLoading.loadingOrg();
            if (publicOrg == null) {
                publicOrg = new ArrayList<>();
                result1 = new ArrayList<>();
            }
        } else {
            publicOrg = new ArrayList<>();
        }
        Iterator<Map> iterator = result1.iterator();
        while (iterator.hasNext()) {
            Map map = iterator.next();
            String id = map.get("id").toString();
            if (publicOrg.size() > 0 && !publicOrg.contains(id)) {
                iterator.remove();
                continue;
            }
            Map<String, String> urlMap = new HashMap<>();
            Map businessUrl = (Map) map.get("businessUrl");
            if (businessUrl != null) {
                String login = (String) businessUrl.get("host");
                try {
                    urlMap.put("licenseUrl", businessUrl.get("licenseUrl").toString().trim());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (login != null) {
                    urlMap.put("host", login.trim());
                }
            }
            // if(resourceType.equals("sciencedb")){
            // String dataAdd = businessUrl.get("dataAdd").toString().trim();
            // String dataSubmit = businessUrl.get("dataSubmit").toString().trim();
            // String queryState = businessUrl.get("queryState").toString().trim();
            // String ftp = businessUrl.get("ftp").toString().trim();
            // urlMap.put("dataAdd",dataAdd);
            // urlMap.put("dataSubmit",dataSubmit);
            // urlMap.put("queryState",queryState);
            // urlMap.put("ftp",ftp);
            // urlMap.put("ftpHost",businessUrl.get("ftpHost").toString().trim());
            // urlMap.put("addVersion",businessUrl.get("addVersion").toString().trim());
            // }
            // urlMap.put("resourceType",resourceType);
            publicOrgUrl.put(id, urlMap);
            map.remove("_id");
            map.remove("pubInterface");
            map.remove("staInterface");
            map.remove("licenseUrl");
            map.remove("resourceType");
            if (businessUrl != null) {
                map.remove("businessUrl");
            }
        }
        return ResultUtil.success(result1);
    }

    @Override
    public ResponseResult<Object> accessProjectList(String projectName) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("modelType", "Project");
        if (StringUtils.isNotEmpty(projectName)) {
            paramMap.put("keyword", projectName.trim());
        } else {
            return ResultUtil.success(new ArrayList<>(0));
        }
        return publicQuery(paramMap, spaceUrl.getCenterHost() + queryUrl);
    }

    @Override
    public ResponseResult<Object> accessPaperList(String paperName) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("modelType", "Paper");
        if (StringUtils.isNotEmpty(paperName)) {
            paramMap.put("keyword", paperName.trim());
        } else {
            return ResultUtil.success(new ArrayList<>(0));
        }
        return publicQuery(paramMap, spaceUrl.getCenterHost() + queryUrl);
    }

    @Override
    public ResponseResult<Object> authorSearch(String param) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("modelType", "Person");
        if (StringUtils.isNotEmpty(param)) {
            paramMap.put("keyword", param.trim());
        } else {
            return ResultUtil.success(new ArrayList<>(0));
        }
        return publicQuery(paramMap, spaceUrl.getCenterHost() + queryUrl);
    }

    @Override
    public ResponseResult<Object> objectDel(String id, String type) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("modelType", type);
        if (StringUtils.isNotEmpty(id)) {
            paramMap.put("id", id);
        } else {
            return ResultUtil.error("id is null");
        }
        return publicQuery(paramMap, spaceUrl.getCenterHost() + queryUrl);
    }

    @Override
    public ResponseResult<Object> applyDOI() {
        return ResultUtil.success(CommonUtils.generateUUID());
    }

    @Override
    public ResponseResult<Object> applyCSTR(CSTR cstr) {
        return ResultUtil.success(CommonUtils.generateUUID());
    }

    @Override
    public ResponseResult<Object> checkCstr(String cstrCode) {
        if (StringUtils.isEmpty(cstrCode)) {
            return ResultUtil.errorInternational("PARAMETER_ERROR");
        }
        HttpClient httpClient = new HttpClient();
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("cstrCode", cstrCode));
        String result = "";
        try {
            result = httpClient.doGetWayTwo(params, spaceUrl.getCenterHost() + "/api/v1/checkCSTR", null);
        } catch (Exception e) {
            log.info(e.getMessage());
            ResultUtil.errorInternational("EXTERNAL_API");
        }
        Map resultMap = JSONObject.parseObject(result, Map.class);
        int code = (int) resultMap.get("code");
        boolean success = (boolean) resultMap.get("success");
        if (code != 200 || !success) {
            return ResultUtil.error((String) resultMap.get("message"));
        }
        return ResultUtil.success();
    }

    @Override
    public ResponseResult<Object> licenseList(String orgId, String url) {
        HttpClient httpClient = new HttpClient();
        String httpUrl = "";
        if (StringUtils.isNotEmpty(orgId)) {
            Map<String, String> ifPresent = publicOrgUrl.getIfPresent(orgId);
            if (ifPresent == null) {
                this.accessOrgList(null, Constants.GENERAL);
                ifPresent = publicOrgUrl.getIfPresent(orgId);
            }
            if (ifPresent == null) {
                return ResultUtil.errorInternational("EXTERNAL_ORG_NOT_FOUND");
            }
            httpUrl = ifPresent.get("licenseUrl");
        } else if (StringUtils.isNotEmpty(url)) {
            httpUrl = url + "/api/getlicenseAgreement";
        } else {
            return ResultUtil.errorInternational("EXTERNAL_ORG_INFO");
        }
        String result = "";
        try {
            result = httpClient.doGetWayTwo(httpUrl);
        } catch (Exception e) {
            log.info(e.getMessage());
            return ResultUtil.errorInternational("EXTERNAL_DATA");
        }
        ResultData resultData = JSONObject.parseObject(result, ResultData.class);
        if (resultData == null) {
            return ResultUtil.errorInternational("EXTERNAL_PARSE");
        }
        if (resultData.getCode() != 200 && resultData.getCode() != 20000) {
            return ResultUtil.error(resultData.getMessage());
        }
        Object data = resultData.getData();
        if (data == null) {
            return ResultUtil.errorInternational("EXTERNAL_PARSE");
        }
        List<Map> licenses = (List<Map>) data;
        for (Map licens : licenses) {
            String name = orgId + licens.get("name").toString();
            publicModel.put(name, JSON.toJSONString(licens));
        }
        return ResultUtil.success(data);
    }

    @Override
    public ResponseResult<Object> getProType(String type) {
        Map<String, Object> paramMap = new HashMap<>();
        if (StringUtils.isNotEmpty(type)) {
            paramMap.put("name", type.trim());
        }
        return publicQuery(paramMap, spaceUrl.getCenterHost() + typeUrl);
    }

    @Override
    public ResponseResult<Object> forDetails(String type, String[] ids) {
        Map<String, Object> paramMap = new HashMap<>();
        List<String> strings = Arrays.asList(queryType);
        if (!strings.contains(type) && ids.length == 0) {
            return ResultUtil.errorInternational("system_type_error");
        }
        paramMap.put("modelType", type);
        paramMap.put("ids", ids);
        return publicQuery(paramMap, spaceUrl.getCenterHost() + queryUrl);
    }

    @Override
    public ResponseResult<Object> orgList(String orgName) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("modelType", "Organization");
        if (StringUtils.isNotEmpty(orgName)) {
            paramMap.put("keyword", orgName);
        } else {
            return ResultUtil.success(new ArrayList<>(0));
        }
        return publicQuery(paramMap, spaceUrl.getCenterHost() + queryUrl);
    }

    @Override
    public ResponseResult<Object> personAdd(Person person) {
        // Parameter verification
        List<String> validation = CommonUtils.validation(person);
        if (validation.size() > 0) {
            return ResultUtil.error(validation.toString());
        }
        Account account = getAccount();
        if (account == null) {
            return ResultUtil.errorInternational("EXTERNAL_NOT_JOINT");
        }
        person.setAccount(account.getAccount());
        person.setPassword(RSAEncrypt.decrypt(account.getPassword()));
        Map map = JSON.parseObject(JSON.toJSONString(person), Map.class);
        String orgId = map.get("orgId").toString();
        map.put("employment", new ArrayList<String>() {

            {
                add(orgId);
            }
        });
        map.remove("orgId");
        return publicAdd(JSON.toJSONString(map), spaceUrl.getCenterHost() + "/api/v1/addPersonModel");
    }

    @Override
    public ResponseResult<Object> orgAdd(Org org) {
        // Parameter verification
        List<String> validation = CommonUtils.validation(org);
        if (validation.size() > 0) {
            return ResultUtil.error(validation.toString());
        }
        Account account = getAccount();
        if (account == null) {
            return ResultUtil.errorInternational("EXTERNAL_NOT_JOINT");
        }
        org.setAccount(account.getAccount());
        org.setPassword(RSAEncrypt.decrypt(account.getPassword()));
        return publicAdd(JSON.toJSONString(org), spaceUrl.getCenterHost() + "/api/v1/addOrgModel");
    }

    @Override
    public ResponseResult<Object> projectAdd(Project project) {
        // Parameter verification
        List<String> validation = CommonUtils.validation(project);
        if (validation.size() > 0) {
            return ResultUtil.error(validation.toString());
        }
        Account account = getAccount();
        if (account == null) {
            return ResultUtil.errorInternational("EXTERNAL_NOT_JOINT");
        }
        project.setAccount(account.getAccount());
        project.setPassword(RSAEncrypt.decrypt(account.getPassword()));
        return publicAdd(JSON.toJSONString(project), spaceUrl.getCenterHost() + "/api/v1/addProjectModel");
    }

    @Override
    public ResponseResult<Object> paperAdd(Paper paper) {
        // Parameter verification
        List<String> validation = CommonUtils.validation(paper);
        if (validation.size() > 0) {
            return ResultUtil.error(validation.toString());
        }
        Account account = getAccount();
        if (account == null) {
            return ResultUtil.errorInternational("EXTERNAL_NOT_JOINT");
        }
        paper.setAccount(account.getAccount());
        paper.setPassword(RSAEncrypt.decrypt(account.getPassword()));
        return publicAdd(JSON.toJSONString(paper), spaceUrl.getCenterHost() + "/api/v1/addPaperModel");
    }

    /**
     * Public Query
     */
    private ResponseResult<Object> publicQuery(Map<String, Object> paramMap, String url) {
        HttpClient httpClient = new HttpClient();
        String result = "";
        try {
            Account account = getAccount();
            if (account == null) {
                return ResultUtil.success(new ArrayList<>());
            }
            paramMap.put("account", account.getAccount());
            paramMap.put("password", RSAEncrypt.decrypt(account.getPassword()));
            result = httpClient.doPostJsonWayTwo(JSON.toJSONString(paramMap), url);
        } catch (CommonException exception) {
            return ResultUtil.error(CommonUtils.messageInternational("EXTERNAL_API") + exception.getMsg());
        } catch (Exception e) {
            log.info(e.getMessage());
            return ResultUtil.errorInternational("EXTERNAL_API");
        }
        if (StringUtils.isEmpty(result)) {
            return ResultUtil.errorInternational("EXTERNAL_PARSE");
        }
        Map resultMap = JSONObject.parseObject(result, Map.class);
        if (resultMap == null) {
            return ResultUtil.errorInternational("EXTERNAL_PARSE");
        }
        int code = (int) resultMap.get("code");
        boolean success = (boolean) resultMap.get("success");
        if (code != 200 || !success) {
            return ResultUtil.error((String) resultMap.get("message"));
        }
        return ResultUtil.success(resultMap.get("result"));
    }

    private ResponseResult<Object> publicAdd(String paramJson, String path) {
        HttpClient httpClient = new HttpClient();
        String result = "";
        try {
            result = httpClient.doPostJsonWayTwo(paramJson, path);
        } catch (Exception e) {
            log.info(e.getMessage());
            ResultUtil.errorInternational("EXTERNAL_API");
        }
        Map resultMap = JSONObject.parseObject(result, Map.class);
        return ResultUtil.success((String) resultMap.get("message"), resultMap.get("result"));
    }

    private Account getAccount() {
        Object acc = cacheLoading.loadingCenterOpen();
        // if(acc == null){
        // Throw new CommonException (-1, "Not joined the scientific data center!");
        // }
        return (Account) acc;
    }
}
