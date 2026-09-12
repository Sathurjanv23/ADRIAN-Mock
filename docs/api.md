# AI Emergency Response & Relief Network (ADRN) — API Reference

## Base URLs
- **Local Backend (Spring Boot)**: `http://localhost:8080/api/v1`
- **Frontend Proxy**: `http://localhost:3000/api`
- **WebSocket Gateway**: `ws://localhost:8080/ws`

## Authentication
Protected routes require the `Authorization` header with a valid JWT Bearer token:
```http
Authorization: Bearer <jwt-token>
```

---

## 1. Authentication Endpoints (`/auth`)

### POST `/auth/login`
Authenticate user and receive JWT.
- **Request Body**:
  ```json
  {
    "email": "officer@nova.org",
    "password": "Password123!"
  }
  ```
- **Response `200 OK`**:
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "type": "Bearer",
    "userId": "usr-101",
    "name": "Sarah Chen",
    "email": "officer@nova.org",
    "role": "officer"
  }
  ```

### POST `/auth/register`
Register new citizen or rescue responder.

### GET `/auth/me`
Retrieve currently logged-in user profile.

---

## 2. Relief Logistics & Food Sources (`/food-sources`)

### GET `/food-sources`
List all registered food sources, relief hubs, community kitchens, and warehouses.
- **Query Parameters**:
  - `type` (optional): `COMMUNITY_KITCHEN | RELIEF_CAMP | FOOD_BANK | WAREHOUSE | RESTAURANT_DONOR | WATER_DISTRIBUTION`
  - `status` (optional): `ACTIVE | INACTIVE | DEPLETED | CRITICAL_LOW`
  - `district` (optional): Filter by geographical district name
- **Response `200 OK`**:
  ```json
  [
    {
      "id": "fs-001",
      "name": "Central Colombo Relief Hub",
      "type": "COMMUNITY_KITCHEN",
      "contactPerson": "Priyantha Perera",
      "phone": "+94 77 123 4567",
      "address": "12 Station Road",
      "district": "Colombo",
      "location": { "lat": 6.9271, "lng": 79.8612 },
      "totalMealCapacity": 5000,
      "availableMeals": 3200,
      "waterLiters": 12000,
      "dryRationKits": 850,
      "dietaryTypes": ["VEGETARIAN", "HALAL"],
      "status": "ACTIVE",
      "isVerified": true,
      "operatingHours": "24/7",
      "spoilageRisk": false
    }
  ]
  ```

### POST `/food-sources`
Register a new food source or community kitchen. *(Requires `OFFICER` or `ADMIN` role)*

### PUT `/food-sources/{id}`
Update inventory levels or operating details for a food source.

### GET `/food-sources/stats`
Summary metrics across all supply distribution points.
- **Response `200 OK`**:
  ```json
  {
    "totalSources": 24,
    "activeSources": 21,
    "totalMealsAvailable": 45200,
    "totalWaterLiters": 120500,
    "criticalLowCount": 2
  }
  ```

---

## 3. Relief Requests (`/relief-requests`)

### GET `/relief-requests`
List humanitarian aid requests from shelters, camps, and isolated zones.
- **Query Parameters**:
  - `status`: `PENDING | SOURCING | ALLOCATED | IN_TRANSIT | FULFILLED | CANCELLED`
  - `urgency`: `CRITICAL | HIGH | MEDIUM | LOW`
- **Response `200 OK`**:
  ```json
  [
    {
      "id": "rr-101",
      "incidentId": "inc-552",
      "requestedBy": "Field Officer Jayasinghe",
      "beneficiaryCount": 350,
      "infantCount": 42,
      "elderlyCount": 68,
      "mealsNeeded": 700,
      "waterLitersNeeded": 1500,
      "specialNeeds": "Baby formula, hygiene packs, insulin refrigeration",
      "deliveryAddress": "St. Mary's Relief Camp, Kalutara",
      "location": { "lat": 6.5854, "lng": 79.9607 },
      "urgency": "CRITICAL",
      "status": "PENDING",
      "deadline": "2026-09-12T06:00:00Z"
    }
  ]
  ```

### POST `/relief-requests`
Create a relief request (can be triggered automatically from incident triaging).

### PUT `/relief-requests/{id}/status`
Update status of relief request (`PENDING` $\rightarrow$ `ALLOCATED` $\rightarrow$ `FULFILLED`).

---

## 4. Relief Missions & Fleet Dispatch (`/relief-missions`)

### GET `/relief-missions`
List all active and past supply delivery missions.

### POST `/relief-missions/dispatch`
Dispatch a transport vehicle/volunteer team from a food source to a disaster destination.
- **Request Body**:
  ```json
  {
    "reliefRequestId": "rr-101",
    "foodSourceId": "fs-001",
    "teamId": "rt-04",
    "driverName": "Kamal Bandara",
    "driverPhone": "+94 71 999 8888",
    "vehicleType": "TRUCK_5_TON",
    "vehicleNumber": "WP-CAB-4421",
    "mealsLoaded": 700,
    "waterLitersLoaded": 1500,
    "medicalKitsLoaded": 10,
    "notes": "Escorted through route A4 due to waterlogging"
  }
  ```
- **Response `201 Created`**:
  ```json
  {
    "id": "rm-901",
    "status": "DISPATCHED",
    "dispatchTime": "2026-09-11T20:30:00Z",
    "estimatedArrival": "2026-09-11T22:00:00Z",
    "distanceKm": 38.4,
    "currentLocation": { "lat": 6.9271, "lng": 79.8612 }
  }
  ```

### PUT `/relief-missions/{id}/status`
Progress delivery state: `DISPATCHED` $\rightarrow$ `IN_TRANSIT` $\rightarrow$ `DELIVERED` | `FAILED`.

### PUT `/relief-missions/{id}/location`
Update live GPS coordinates for moving delivery trucks. Broadcasts to `/topic/relief-missions`.

---

## 5. AI Triaging & Copilot (`/ai`)

### POST `/ai/analyze-incident`
Submits raw incident text, audio transcripts, or image context to the AI Engine for NLP triaging.
- **Request Body**:
  ```json
  {
    "type": "FLOOD",
    "description": "Rising water levels trapping 20 families in Kelaniya temple. Elderly need food and clean water urgently.",
    "peopleAffected": 80,
    "lat": 6.9534,
    "lng": 79.9168,
    "detectedLanguage": "en"
  }
  ```
- **Response `200 OK`**:
  ```json
  {
    "severity": "CRITICAL_P1",
    "urgencyScore": 92,
    "recommendedActions": [
      "Deploy Rescue Boat Unit to Kelaniya Temple",
      "Create Relief Request: 250 hot meals and 500L clean drinking water",
      "Alert General Hospital Ragama for potential hypothermia cases"
    ],
    "needsRelief": true,
    "requiredMeals": 250,
    "requiredWaterLiters": 500,
    "hazardsIdentified": ["DEEP_FLOODING", "ELECTRICAL_HAZARD", "TRAPPED_CIVILIANS"]
  }
  ```

### POST `/ai/relief-recommendation`
AI calculates the optimal hub to dispatch supplies for a given relief request based on inventory, distance, and road risks.
- **Request Body**:
  ```json
  { "reliefRequestId": "rr-101" }
  ```
- **Response `200 OK`**:
  ```json
  {
    "recommendedSourceId": "fs-001",
    "recommendedSourceName": "Central Colombo Relief Hub",
    "distanceKm": 12.4,
    "estimatedTravelMinutes": 28,
    "reasoning": "Central Colombo Relief Hub holds 3,200 available meals (matches 700 needed) and has 2 active 5-ton trucks ready for deployment.",
    "alternativeSourceId": "fs-003",
    "confidenceScore": 0.94
  }
  ```

### POST `/ai/copilot`
Interactive AI conversational assistant for Command Center officers.
- **Request Body**:
  ```json
  {
    "query": "What is our current water deficit in the Gampaha district?",
    "context": "{\"district\": \"Gampaha\"}"
  }
  ```

---

## 6. Error Responses (RFC 7807)

Standardized JSON error format across all endpoints:
```json
{
  "timestamp": "2026-09-11T20:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "code": "INSUFFICIENT_INVENTORY",
  "message": "Requested 1,500 meals exceeds available supply (800) at source fs-002",
  "path": "/api/v1/relief-missions/dispatch"
}
```
