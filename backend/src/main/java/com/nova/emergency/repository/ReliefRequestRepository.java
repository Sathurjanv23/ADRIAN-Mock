package com.nova.emergency.repository;

import com.nova.emergency.model.ReliefRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReliefRequestRepository extends MongoRepository<ReliefRequest, String> {
    List<ReliefRequest> findByStatus(String status);
    List<ReliefRequest> findByPriority(String priority);
    Optional<ReliefRequest> findByIncidentId(String incidentId);
    List<ReliefRequest> findByStatusOrderByCreatedAtAsc(String status);
    List<ReliefRequest> findByPriorityAndStatusOrderByCreatedAtAsc(String priority, String status);
}
