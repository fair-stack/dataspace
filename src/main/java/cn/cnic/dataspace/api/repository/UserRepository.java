package cn.cnic.dataspace.api.repository;

import cn.cnic.dataspace.api.model.user.ConsumerDO;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * UserRepository
 *
 * @author wangCc
 * @date 2021-03-19 15:45
 */
public interface UserRepository extends MongoRepository<ConsumerDO, String> {

    ConsumerDO findByEmailAccounts(String emailAccounts);
}
