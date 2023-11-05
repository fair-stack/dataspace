package cn.cnic.dataspace.api.repository;

import cn.cnic.dataspace.api.model.space.SpaceShare;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * file share
 *
 * @author wangCc
 * @date 2021-3-22 16:41:24
 */
@Repository
public interface SpaceShareRepository extends MongoRepository<SpaceShare, String> {

    SpaceShare findBySpaceId(String fileId);

    Page<SpaceShare> findByUserId(String userId, Pageable pageable);
}
