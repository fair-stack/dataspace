package cn.cnic.dataspace.api.repository;

import cn.cnic.dataspace.api.model.manage.ApproveSetting;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * ApproveSettingRepository
 *
 * @author wangCc
 * @date 2021-11-16 18:18
 */
public interface ApproveSettingRepository extends MongoRepository<ApproveSetting, String> {
}
