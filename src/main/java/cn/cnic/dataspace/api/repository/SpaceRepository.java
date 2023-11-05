package cn.cnic.dataspace.api.repository;

import cn.cnic.dataspace.api.model.space.Space;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * SpaceRepository
 *
 * @author wangCc
 * @date 2021-03-22 13:32
 */
@Repository
public interface SpaceRepository extends MongoRepository<Space, String> {

    Space findByUserIdAndSpaceName(String userId, String spaceName);

    Optional<Space> findByHomeUrl(String index);
}
