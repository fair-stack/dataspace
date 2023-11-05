package cn.cnic.dataspace.api.repository;

import cn.cnic.dataspace.api.model.space.child.SimpleSpace;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * SimpleSpaceRepository
 *
 * @author wangCc
 * @date 2021-03-22 13:32
 */
@Repository
public interface SimpleSpaceRepository extends MongoRepository<SimpleSpace, String> {
}
