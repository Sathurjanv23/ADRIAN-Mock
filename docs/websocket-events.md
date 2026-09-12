# AI Emergency Response & Relief Network (ADRN) — WebSocket Events & Protocol

## 1. Protocol & Handshake

ADRN uses **STOMP (Simple Text Oriented Messaging Protocol)** over **SockJS** to deliver bi-directional, sub-second event broadcasts between the Spring Boot backend and connected client portals (Command Center, Relief Logistics, Field Units).

- **WebSocket URL**: `http://localhost:8080/ws` (SockJS transport fallback enabled)
- **Direct WS URL**: `ws://localhost:8080/ws/websocket`

### 1.1. Client Connection & Authentication

When opening a STOMP connection, clients pass their JWT in the `connectHeaders`:

```javascript
import { Client } from '@stomp/stompjs';

const client = new Client({
  brokerURL: 'ws://localhost:8080/ws/websocket',
  connectHeaders: {
    Authorization: `Bearer ${jwtToken}`
  },
  heartbeatIncoming: 10000,
  heartbeatOutgoing: 10000,
  reconnectDelay: 5000,
});
client.activate();
```

---

## 2. Topic Catalog

| Destination Channel | Purpose | Published By | Subscribed By |
| :--- | :--- | :--- | :--- |
| `/topic/incidents` | New incidents, severity triage, status changes | Backend / AI | Command, Rescue, Relief |
| `/topic/relief-missions` | Mission dispatches, transit GPS updates, delivery | Relief Logistics | Command, Relief, Rescue |
| `/topic/teams` | First responder availability & tactical telemetry | Rescue Teams | Command, Rescue |
| `/topic/hospitals` | Bed availability, trauma room status | Hospitals | Command, Rescue |
| `/topic/alerts` | Critical civic emergency broadcasts & sirens | Command Center | All Users & Citizens |
| `/user/queue/notifications`| User-specific alerts, assignments, mentions | System | Targeted User |

---

## 3. Event Schemas & Payloads

### 3.1. Incident Created / Triaged (`/topic/incidents`)

```json
{
  "eventType": "INCIDENT_TRIAGED",
  "timestamp": "2026-09-11T20:30:15Z",
  "payload": {
    "id": "inc-982",
    "type": "FLOOD",
    "severity": "CRITICAL_P1",
    "title": "Flash flood submerging residential ward 4",
    "location": { "lat": 6.9147, "lng": 79.9729, "district": "Gampaha" },
    "peopleAffected": 140,
    "urgencyScore": 95,
    "needsRelief": true,
    "reliefRequirements": {
      "meals": 400,
      "waterLiters": 1000
    }
  }
}
```

### 3.2. Relief Mission Dispatched (`/topic/relief-missions`)

```json
{
  "eventType": "MISSION_DISPATCHED",
  "timestamp": "2026-09-11T20:31:00Z",
  "payload": {
    "id": "rm-771",
    "reliefRequestId": "rr-101",
    "foodSourceName": "Kelaniya Central Relief Kitchen",
    "vehicleNumber": "WP-GA-9012",
    "driverName": "Sunil Shantha",
    "mealsLoaded": 400,
    "waterLitersLoaded": 1000,
    "status": "DISPATCHED",
    "estimatedArrival": "2026-09-11T21:15:00Z",
    "origin": { "lat": 6.9534, "lng": 79.9168 },
    "destination": { "lat": 6.9147, "lng": 79.9729 }
  }
}
```

### 3.3. Relief Mission Location Ping (`/topic/relief-missions`)

Sent periodically by the transport vehicle's GPS transponder or mobile field app:

```json
{
  "eventType": "MISSION_LOCATION_UPDATE",
  "timestamp": "2026-09-11T20:38:22Z",
  "payload": {
    "missionId": "rm-771",
    "currentLocation": { "lat": 6.9320, "lng": 79.9450 },
    "speedKmh": 42.5,
    "headingDegrees": 118,
    "status": "IN_TRANSIT"
  }
}
```

### 3.4. Relief Mission Completed (`/topic/relief-missions`)

```json
{
  "eventType": "MISSION_DELIVERED",
  "timestamp": "2026-09-11T21:12:45Z",
  "payload": {
    "missionId": "rm-771",
    "deliveredAt": "2026-09-11T21:12:45Z",
    "receivedBy": "Camp Director Jayawardena",
    "status": "DELIVERED",
    "proofPhotoUrl": "https://s3.amazonaws.com/adrn-media/proof-771.jpg"
  }
}
```

---

## 4. Resilience & Reconnection Strategy

1. **Automatic Heartbeats**: Client and server exchange heartbeat frames every 10 seconds. If missing for 2 consecutive cycles, the socket is dropped and re-established.
2. **Backoff Exponential Reconnection**: Clients back off with intervals (1s, 2s, 4s, up to 15s) until connection succeeds.
3. **State Hydration on Reconnect**: When the WebSocket reconnects, client UI queries `GET /api/v1/relief-missions/active` and `GET /api/v1/incidents` to reconcile any missed broadcast frames.
