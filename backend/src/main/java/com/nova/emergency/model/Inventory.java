package com.nova.emergency.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Inventory — Tracks the real-time stock levels at a FoodSource.
 * Prevents double-booking and negative inventory.
 * Updated atomically when ReliefMissions reserve or release supplies.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "inventory")
public class Inventory {

    @Id
    private String id;

    // Reference to FoodSource
    private String sourceId;
    private String sourceName;

    // MEALS | WATER
    private String itemType;

    // Total quantity at source
    private int quantity;

    // Quantity reserved by active missions (not yet collected)
    private int reservedQuantity;

    // quantity - reservedQuantity — always >= 0
    private int availableQuantity;

    private String expiryTime;
    private String updatedAt;

    /**
     * Reserve stock — reduces availableQuantity and increases reservedQuantity.
     * Throws IllegalStateException if insufficient stock.
     */
    public void reserve(int amount) {
        if (amount > availableQuantity) {
            throw new IllegalStateException(
                "Insufficient inventory: requested " + amount + " but only " + availableQuantity + " available."
            );
        }
        reservedQuantity += amount;
        availableQuantity -= amount;
    }

    /**
     * Release reserved stock back to available (e.g., mission cancelled).
     */
    public void release(int amount) {
        int toRelease = Math.min(amount, reservedQuantity);
        reservedQuantity -= toRelease;
        availableQuantity += toRelease;
    }

    /**
     * Consume stock when mission actually collects supplies.
     * Reduces quantity and clears reservation.
     */
    public void consume(int amount) {
        int toConsume = Math.min(amount, quantity);
        int toUncoverFromReserved = Math.min(amount, reservedQuantity);
        reservedQuantity -= toUncoverFromReserved;
        quantity = Math.max(0, quantity - toConsume);
        availableQuantity = Math.max(0, quantity - reservedQuantity);
    }

    // Explicit getters/setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public int getReservedQuantity() { return reservedQuantity; }
    public void setReservedQuantity(int reservedQuantity) { this.reservedQuantity = reservedQuantity; }
    public int getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(int availableQuantity) { this.availableQuantity = availableQuantity; }
    public String getExpiryTime() { return expiryTime; }
    public void setExpiryTime(String expiryTime) { this.expiryTime = expiryTime; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
