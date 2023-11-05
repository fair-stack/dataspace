package cn.cnic.dataspace.api.repository;

import cn.cnic.dataspace.api.model.manage.ReleaseAccount;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Publishing account management
 */
@Repository
public interface ReleaseAccountRepository extends MongoRepository<ReleaseAccount, String> {
}
