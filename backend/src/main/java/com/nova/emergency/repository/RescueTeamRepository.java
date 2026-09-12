package com.nova.emergency.repository;

import com.nova.emergency.model.RescueTeam;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RescueTeamRepository extends MongoRepository<RescueTeam, String> {
    List<RescueTeam> findByStatus(String status);
    List<RescueTeam> findByDistrict(String district);
    List<RescueTeam> findByStatusAndDistrict(String status, String district);
}
