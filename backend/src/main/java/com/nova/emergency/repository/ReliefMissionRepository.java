package com.nova.emergency.repository;

import com.nova.emergency.model.ReliefMission;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReliefMissionRepository extends MongoRepository<ReliefMission, String> {
    List<ReliefMission> findByStatus(String status);
    Optional<ReliefMission> findByReliefRequestId(String reliefRequestId);
    List<ReliefMission> findByFoodSourceId(String foodSourceId);
    List<ReliefMission> findByVehicleId(String vehicleId);
    List<ReliefMission> findByStatusIn(List<String> statuses);
    long countByStatus(String status);
}
