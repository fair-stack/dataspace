package cn.cnic.dataspace.api.repository;

import cn.cnic.dataspace.api.model.apply.Apply;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * apply repository
 *
 * @author wangCc
 * @date 2021-4-7 10:27:48
 */
@Repository
public interface ApplyRepository extends MongoRepository<Apply, String> {
}
