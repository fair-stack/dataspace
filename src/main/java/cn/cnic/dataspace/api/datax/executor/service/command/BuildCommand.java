package cn.cnic.dataspace.api.datax.executor.service.command;

import cn.cnic.dataspace.api.datax.core.biz.model.TriggerParam;
import cn.cnic.dataspace.api.datax.core.enums.IncrementTypeEnum;
import cn.cnic.dataspace.api.datax.core.util.Constants;
import cn.cnic.dataspace.api.datax.core.util.DateUtil;
import cn.cnic.dataspace.api.datax.executor.util.SystemUtils;
import org.apache.commons.lang3.StringUtils;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import static cn.cnic.dataspace.api.datax.core.util.Constants.SPLIT_COMMA;
import static cn.cnic.dataspace.api.datax.executor.service.jobhandler.DataXConstant.*;

/**
 * DataX command build
 *
 * @author  2020-06-07
 */
public class BuildCommand {

    /**
     * DataX command build
     *
     * @param tgParam
     * @param tmpFilePath
     * @param dataXPyPath
     * @return
     */
    public static String[] buildDataXExecutorCmd(TriggerParam tgParam, String tmpFilePath, String dataXPyPath) {
        // command process
        // "--loglevel=debug"
        List<String> cmdArr = new ArrayList<>();
        cmdArr.add("python");
        String dataXHomePath = SystemUtils.getDataXHomePath();
        if (StringUtils.isNotEmpty(dataXHomePath)) {
            dataXPyPath = dataXHomePath.contains("bin") ? dataXHomePath + DEFAULT_DATAX_PY : dataXHomePath + "bin" + File.separator + DEFAULT_DATAX_PY;
        }
        cmdArr.add(dataXPyPath);
        String doc = buildDataXParam(tgParam);
        if (StringUtils.isNotBlank(doc)) {
            cmdArr.add(doc.replaceAll(SPLIT_SPACE, TRANSFORM_SPLIT_SPACE));
        }
        cmdArr.add(tmpFilePath);
        return cmdArr.toArray(new String[cmdArr.size()]);
    }

    /**
     * Build datax to run virtual machine parameters
     */
    private static String buildDataXParam(TriggerParam tgParam) {
        StringBuilder doc = new StringBuilder();
        String jvmParam = StringUtils.isNotBlank(tgParam.getJvmParam()) ? tgParam.getJvmParam().trim() : tgParam.getJvmParam();
        if (StringUtils.isNotBlank(jvmParam)) {
            doc.append(JVM_CM).append(TRANSFORM_QUOTES).append(jvmParam).append(TRANSFORM_QUOTES);
        }
        return doc.toString();
    }

    /**
     * Build datax incremental parameters
     */
    public static HashMap<String, String> buildDataXParamToMap(TriggerParam tgParam) {
        String partitionStr = tgParam.getPartitionInfo();
        Integer incrementType = tgParam.getIncrementType();
        String replaceParam = StringUtils.isNotBlank(tgParam.getReplaceParam()) ? tgParam.getReplaceParam().trim() : null;
        if (incrementType != null && replaceParam != null) {
            if (IncrementTypeEnum.ID.getCode() == incrementType) {
                long startId = tgParam.getStartId();
                long endId = tgParam.getEndId();
                String formatParam = String.format(replaceParam, startId, endId);
                return getKeyValue(formatParam);
            } else if (IncrementTypeEnum.TIME.getCode() == incrementType) {
                String replaceParamType = tgParam.getReplaceParamType();
                if (StringUtils.isBlank(replaceParamType) || "Timestamp".equals(replaceParamType)) {
                    long startTime = tgParam.getStartTime().getTime() / 1000;
                    long endTime = tgParam.getTriggerTime().getTime() / 1000;
                    String formatParam = String.format(replaceParam, startTime, endTime);
                    return getKeyValue(formatParam);
                } else {
                    SimpleDateFormat sdf = new SimpleDateFormat(replaceParamType);
                    String endTime = sdf.format(tgParam.getTriggerTime());
                    String startTime = sdf.format(tgParam.getStartTime());
                    String formatParam = String.format(replaceParam, startTime, endTime);
                    return getKeyValue(formatParam);
                }
            }
        }
        if (incrementType != null && IncrementTypeEnum.PARTITION.getCode() == incrementType) {
            if (StringUtils.isNotBlank(partitionStr)) {
                List<String> partitionInfo = Arrays.asList(partitionStr.split(SPLIT_COMMA));
                String formatParam = String.format(PARAMS_CM_V_PT, buildPartition(partitionInfo));
                return getKeyValue(formatParam);
            }
        }
        return null;
    }

    /**
     * Encapsulate task parameters as maps
     */
    private static HashMap<String, String> getKeyValue(String formatParam) {
        String[] paramArr = formatParam.split(PARAMS_SYSTEM);
        HashMap<String, String> map = new HashMap<String, String>();
        for (String param : paramArr) {
            if (StringUtils.isNotBlank(param)) {
                param = param.trim();
                String[] keyValue = param.split(PARAMS_EQUALS);
                map.put(keyValue[0], keyValue[1]);
            }
        }
        return map;
    }

    /**
     * Datax task built-in variables: mimic Alibaba Cloud's commercial DataWorks/ODPS to provide built-in variables<br/>
     */
    public static Map<String, String> builtInVar() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("datax_biz_date", DateUtil.format(new Date(), "yyyy-MM-dd"));
        map.put("datax_biz_time", DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        map.put("datax_biz_unixtimestamp", System.currentTimeMillis() + "");
        return map;
    }

    private void buildPartitionCM(StringBuilder doc, String partitionStr) {
        if (StringUtils.isNotBlank(partitionStr)) {
            doc.append(SPLIT_SPACE);
            List<String> partitionInfo = Arrays.asList(partitionStr.split(SPLIT_COMMA));
            doc.append(String.format(PARAMS_CM_V_PT, buildPartition(partitionInfo)));
        }
    }

    private static String buildPartition(List<String> partitionInfo) {
        String field = partitionInfo.get(0);
        int timeOffset = Integer.parseInt(partitionInfo.get(1));
        String timeFormat = partitionInfo.get(2);
        String partitionTime = DateUtil.format(DateUtil.addDays(new Date(), timeOffset), timeFormat);
        return field + Constants.EQUAL + partitionTime;
    }
}
