package cn.cnic.dataspace.api.repository;

import cn.cnic.dataspace.api.model.space.SpaceSvnLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * SvnSpaceLog
 *
 * @author wangCc
 * @date 2021-04-12 14:13
 */
@Repository
public interface SvnSpaceLogRepository extends MongoRepository<SpaceSvnLog, String> {

    Iterable<SpaceSvnLog> findBySpaceId(String spaceId);
}
