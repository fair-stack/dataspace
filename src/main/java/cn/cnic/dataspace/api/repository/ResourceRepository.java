package cn.cnic.dataspace.api.repository;

import cn.cnic.dataspace.api.model.release.ResourceV2;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ResourceRepository extends MongoRepository<ResourceV2, String> {

    List<ResourceV2> findAllBySpaceIdAndType(String spaceId, Integer type);
}
