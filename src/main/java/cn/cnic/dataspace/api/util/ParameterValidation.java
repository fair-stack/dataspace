package cn.cnic.dataspace.api.util;

import cn.cnic.dataspace.api.exception.CommonException;
import cn.cnic.dataspace.api.model.release.ResourceDo;
import cn.cnic.dataspace.api.model.release.ResourceRequest;
import cn.cnic.dataspace.api.model.release.stemcells.SampleDo;
import com.alibaba.fastjson.JSONObject;
import com.github.benmanes.caffeine.cache.Cache;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.units.qual.C;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.util.CollectionUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ParameterValidation {

    /**
     * Verification of resource publishing parameters
     */
    public static void DSValidation(ResourceRequest resourceRequest, Cache<String, String> publicModel, MongoTemplate mongoTemplate) {
        if (resourceRequest == null) {
            // Parameter verification
            throw new CommonException(-1, "输入信息无效");
        }
        if (resourceRequest.getType() == 1) {
            // Publish Submission
            if (resourceRequest.getDataType() == 0) {
                if (resourceRequest.getFileList() == null || resourceRequest.getFileList().size() <= 0) {
                    throw new CommonException(-1, "请选择文件");
                }
            } else if (resourceRequest.getDataType() == 1) {
                if (resourceRequest.getTableList() == null || resourceRequest.getTableList().size() <= 0) {
                    throw new CommonException(-1, "请选择结构化数据");
                }
            } else if (resourceRequest.getDataType() == 2) {
                if (resourceRequest.getFileList() == null || resourceRequest.getFileList().size() <= 0) {
                    throw new CommonException(-1, "请选择文件");
                }
                if (resourceRequest.getTableList() == null || resourceRequest.getTableList().size() <= 0) {
                    throw new CommonException(-1, "请选择结构化数据");
                }
            }
            String orgId = resourceRequest.getOrgId();
            if (StringUtils.isEmpty(orgId)) {
                throw new CommonException(-1, "请填写发布机构或发布地址!");
            }
            if (StringUtils.isEmpty(resourceRequest.getVersion())) {
                throw new CommonException(-1, "请选择版本!");
            }
            List<String> validation = CommonUtils.validation(resourceRequest);
            if (validation.size() > 0) {
                throw new CommonException(-1, validation.toString());
            }
            List<ResourceDo> resourceDoList = resourceRequest.getResourceDoList();
            if (CollectionUtils.isEmpty(resourceDoList)) {
                throw new CommonException(-1, "模板属性数据不能为空!");
            }
            paramCheck(resourceRequest.getTemplateId(), resourceDoList, publicModel, mongoTemplate);
        } else {
            if (StringUtils.isEmpty(resourceRequest.getSpaceId())) {
                throw new CommonException(-1, "无指定的空间!");
            }
        }
    }

    /**
     * Verification of parameters for publishing structured data resources
     */
    public static void DSValidation2DataMapping(ResourceRequest resourceRequest, Cache<String, String> publicModel, MongoTemplate mongoTemplate) {
        if (resourceRequest == null) {
            // Parameter verification
            throw new CommonException(-1, "输入信息无效");
        }
        if (resourceRequest.getFileList() == null || resourceRequest.getFileList().size() <= 0) {
            throw new CommonException(-1, "请选择结构化数据");
        }
        if (resourceRequest.getType() == 1) {
            // Publish Submission
            String orgId = resourceRequest.getOrgId();
            if (StringUtils.isEmpty(orgId)) {
                throw new CommonException(-1, "请填写发布机构或发布地址!");
            }
            List<String> validation = CommonUtils.validation(resourceRequest);
            if (validation.size() > 0) {
                throw new CommonException(-1, validation.toString());
            }
            List<ResourceDo> resourceDoList = resourceRequest.getResourceDoList();
            if (CollectionUtils.isEmpty(resourceDoList)) {
                throw new CommonException(-1, "模板属性数据不能为空!");
            }
            paramCheck(resourceRequest.getTemplateId(), resourceDoList, publicModel, mongoTemplate);
        } else {
            if (StringUtils.isEmpty(resourceRequest.getVersion())) {
                throw new CommonException(-1, "无版本信息!");
            }
            if (StringUtils.isEmpty(resourceRequest.getSpaceId())) {
                throw new CommonException(-1, "无指定的空间!");
            }
        }
    }

    /**
     * Verification of Field Rules for Resource Publishing Submission Template
     */
    private static void paramCheck(String modelId, List<ResourceDo> resourceDoList, Cache<String, String> publicModel, MongoTemplate mongoTemplate) {
        String ifPresent = publicModel.getIfPresent(modelId + Constants.CHECK);
        Map<String, String> paramMap = JSONObject.parseObject(ifPresent, HashMap.class);
        Map<String, String> modelMap = new HashMap<>(paramMap);
        // Privacy Policy
        String privacyPolicy = "";
        // license agreement
        String licenseTag = null;
        // license agreement
        Object licenseValue = null;
        for (ResourceDo resourceDo : resourceDoList) {
            String iri = resourceDo.getIri();
            String key = iri.substring(iri.lastIndexOf("/") + 1);
            String language = resourceDo.getLanguage();
            String tag = key + language;
            if (key.equals("privacyPolicy")) {
                Map value = (Map) resourceDo.getValue();
                privacyPolicy = value.get("type").toString();
            } else if (key.equals("license")) {
                licenseTag = tag;
                licenseValue = resourceDo.getValue();
                continue;
            }
            // Stem Cell Table Verification
            String type = resourceDo.getType();
            if (type.equals("table")) {
                tableCheck(modelMap, tag, resourceDo.getValue(), mongoTemplate);
            } else {
                iriCheck(modelMap, tag, resourceDo.getValue());
            }
        }
        // Special case verification
        if (privacyPolicy.equals("open") || privacyPolicy.equals("openDate")) {
            iriCheck(modelMap, licenseTag, licenseValue);
        } else {
            modelMap.remove(licenseTag);
        }
        paramIsNot(modelMap);
    }

    /**
     * Iri Overall Field Verification
     */
    public static void iriCheck(Map<String, String> modelMap, String tag, Object value) {
        if (modelMap.containsKey(tag)) {
            String[] split = modelMap.get(tag).split("~");
            String check = split[1];
            String title = split[0];
            // Value type conversion
            String[] control = check.split(":");
            try {
                String stringValue = (String) value;
                if (StringUtils.isEmpty(stringValue) && Integer.parseInt(control[0]) > 0) {
                    throw new CommonException(-1, title + " 请填写该项信息");
                }
            } catch (ClassCastException e1) {
                try {
                    List listValue = (List) value;
                    if (CollectionUtils.isEmpty(listValue) && Integer.parseInt(control[0]) > 0) {
                        throw new CommonException(-1, title + " 请填写该项信息");
                    }
                    if (listValue.size() < Integer.parseInt(control[0])) {
                        throw new CommonException(-1, title + " 该项信息不能少于" + Integer.parseInt(control[0]) + "条");
                    }
                    if (!control[1].equals("*")) {
                        int integer = Integer.parseInt(control[1]);
                        if (listValue.size() > integer) {
                            throw new CommonException(-1, title + " 该项信息不能超过" + integer + "条");
                        }
                    }
                } catch (ClassCastException e2) {
                    Map mapValue = (Map) value;
                    if (CollectionUtils.isEmpty(mapValue) && Integer.parseInt(control[0]) > 0) {
                        throw new CommonException(-1, title + " 请填写该项信息");
                    }
                }
            }
            modelMap.remove(tag);
        }
    }

    /**
     * Not participating in field leakage verification
     */
    public static void paramIsNot(Map<String, String> modelMap) {
        if (modelMap.size() > 0) {
            String result = "";
            for (String key : modelMap.keySet()) {
                String[] split = modelMap.get(key).split("~");
                String title = split[0];
                String check = split[1].split(":")[0];
                if (Integer.parseInt(check) > 0) {
                    result += title + "、";
                }
            }
            if (!result.equals("")) {
                throw new CommonException(-1, "请完善 " + result.trim() + "等信息");
            }
        }
    }

    /**
     * Template Single Field Verification
     */
    public static boolean paramCheck(String multiply, Object value, StringBuffer buffer) {
        // Value type conversion
        if (StringUtils.isEmpty(multiply) || multiply.trim().equals("")) {
            return true;
        }
        String[] control = multiply.split(":");
        try {
            String stringValue = (String) value;
            if (StringUtils.isEmpty(stringValue) && Integer.parseInt(control[0]) > 0) {
                buffer.append("请填写该项信息");
                return false;
            }
        } catch (ClassCastException e1) {
            try {
                List listValue = (List) value;
                if (CollectionUtils.isEmpty(listValue) && Integer.parseInt(control[0]) > 0) {
                    buffer.append("请填写该项信息");
                    return false;
                }
                if (listValue.size() < Integer.parseInt(control[0])) {
                    buffer.append("该项信息不能少于" + Integer.parseInt(control[0]) + "条");
                    return false;
                }
                if (!control[1].equals("*")) {
                    int integer = Integer.parseInt(control[1]);
                    if (listValue.size() > integer) {
                        buffer.append("该项信息不能超过" + integer + "条");
                        return false;
                    }
                }
            } catch (ClassCastException e2) {
                Map mapValue = (Map) value;
                if (CollectionUtils.isEmpty(mapValue) && Integer.parseInt(control[0]) > 0) {
                    buffer.append("请填写该项信息");
                    return false;
                }
            }
        }
        return true;
    }

    private static void tableCheck(Map<String, String> modelMap, String iri, Object value, MongoTemplate mongoTemplate) {
        if (modelMap.containsKey(iri)) {
            String[] split = modelMap.get(iri).split("~");
            String check = split[1];
            String title = split[0];
            // Value type conversion
            String stringValue = (String) value;
            String[] control = check.split(":");
            if (StringUtils.isNotEmpty(stringValue)) {
                long type = mongoTemplate.count(new Query().addCriteria(Criteria.where("type").is(1).and("sampleId").is(stringValue)), SampleDo.class);
                if (type > 0) {
                    throw new CommonException(-1, title + " 请补全待完善数据");
                }
            }
            if (Integer.parseInt(control[0]) > 0) {
                // Required data
                if (StringUtils.isEmpty(stringValue)) {
                    throw new CommonException(-1, title + " 请填写该项信息");
                }
                long count = mongoTemplate.count(new Query().addCriteria(Criteria.where("sampleId").is(stringValue)), SampleDo.class);
                if (count <= 0) {
                    throw new CommonException(-1, title + " 请填写该项信息");
                }
                if (count < Integer.parseInt(control[0])) {
                    throw new CommonException(-1, title + " 该项信息不能少于" + Integer.parseInt(control[0]) + "条");
                }
                if (!control[1].equals("*")) {
                    int integer = Integer.parseInt(control[1]);
                    if (count > integer) {
                        throw new CommonException(-1, title + " 该项信息不能超过" + integer + "条");
                    }
                }
            }
            modelMap.remove(iri);
        }
    }
}
