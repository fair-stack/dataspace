package cn.cnic.dataspace.api.repository;

import cn.cnic.dataspace.api.model.space.RecentView;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

/**
 * recent view repository
 *
 * @author wangCc
 * @date 2021-10-09 16:04
 */
public interface RecentViewRepository extends MongoRepository<RecentView, String> {

    List<RecentView> findByEmailAndSpaceId(String email, String spaceId);

    void deleteBySpaceId(String spaceId);
}
