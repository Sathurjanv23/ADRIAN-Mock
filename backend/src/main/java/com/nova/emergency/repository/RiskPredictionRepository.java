package com.nova.emergency.repository;

import com.nova.emergency.model.RiskPrediction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RiskPredictionRepository extends MongoRepository<RiskPrediction, String> {
    List<RiskPrediction> findByDistrict(String district);
    Optional<RiskPrediction> findByZone(String zone);
    List<RiskPrediction> findByRiskLevel(String riskLevel);
}
