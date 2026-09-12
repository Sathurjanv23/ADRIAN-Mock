package com.nova.emergency.repository;

import com.nova.emergency.model.Shelter;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ShelterRepository extends MongoRepository<Shelter, String> {
    List<Shelter> findByDistrict(String district);
    List<Shelter> findByStatus(String status);
}
