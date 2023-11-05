package cn.cnic.dataspace.api.datax.admin.service;

public interface DataMappingLockService {

    boolean tryLockDataMapping(Long dataMappingId, String spaceId, boolean isSendMes);

    void releaseLock(Long dataMappingId, String spaceId, boolean isSendMes);
}
