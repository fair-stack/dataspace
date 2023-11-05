package cn.cnic.dataspace.api.datax.admin.service.impl;

import cn.cnic.dataspace.api.datax.admin.entity.DataMappingLock;
import cn.cnic.dataspace.api.datax.admin.mapper.DataMappingLockMapper;
import cn.cnic.dataspace.api.datax.admin.service.DataMappingLockService;
import cn.cnic.dataspace.api.websocket.WebSocketProcess;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class DataMappingLockServiceImpl implements DataMappingLockService {

    @Resource
    private DataMappingLockMapper dataMappingLockMapper;

    @Resource
    private WebSocketProcess webSocketProcess;

    /**
     * Send notification to the front-end to refresh the structured data list
     */
    private void socketMessage2AllClient(String spaceId) {
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("mark", "table_refresh");
        messageMap.put("spaceId", spaceId);
        try {
            webSocketProcess.sendMessage2AllSpaceClient(spaceId, JSONObject.toJSONString(messageMap));
            log.error("---- socket send ----");
        } catch (Exception e) {
            log.error("空间结构化数据列表刷新消息通知发送失败: {} " + e.getMessage());
        }
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public boolean tryLockDataMapping(Long dataMappingId, String spaceId, boolean isSendMes) {
        DataMappingLock dataMappingLock = new DataMappingLock();
        dataMappingLock.setDataMappingId(dataMappingId);
        try {
            dataMappingLockMapper.insert(dataMappingLock);
            if (isSendMes) {
                socketMessage2AllClient(spaceId);
            }
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void releaseLock(Long dataMappingId, String spaceId, boolean isSendMes) {
        dataMappingLockMapper.deleteById(dataMappingId);
        if (isSendMes) {
            socketMessage2AllClient(spaceId);
        }
    }
}
