package com.nova.emergency.repository;

import com.nova.emergency.model.Hospital;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface HospitalRepository extends MongoRepository<Hospital, String> {
    List<Hospital> findByDistrict(String district);
    List<Hospital> findByIsActive(boolean isActive);
}
