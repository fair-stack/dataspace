package cn.cnic.dataspace.api.repository;

import cn.cnic.dataspace.api.model.user.Message;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * message repository
 *
 * @author wangCc
 * @date 2021-04-06 17:11
 */
@Repository
public interface MessageRepository extends MongoRepository<Message, String> {
}
