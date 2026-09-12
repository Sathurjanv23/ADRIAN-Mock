package com.nova.emergency.repository;

import com.nova.emergency.model.Inventory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends MongoRepository<Inventory, String> {
    List<Inventory> findBySourceId(String sourceId);
    Optional<Inventory> findBySourceIdAndItemType(String sourceId, String itemType);
    List<Inventory> findByItemTypeAndAvailableQuantityGreaterThan(String itemType, int minQuantity);
}
