package com.nova.emergency.repository;

import com.nova.emergency.model.FoodSource;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FoodSourceRepository extends MongoRepository<FoodSource, String> {
    List<FoodSource> findByStatus(String status);
    List<FoodSource> findByTypeAndStatus(String type, String status);
    List<FoodSource> findByLocationDistrict(String district);
    List<FoodSource> findByStatusAndAvailableMealsGreaterThan(String status, int minMeals);
    List<FoodSource> findByStatusAndWaterBottlesGreaterThan(String status, int minWater);
}
