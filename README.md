# Project NOVA — AI Emergency Response & Relief Network (ADRN)

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Next.js](https://img.shields.io/badge/Next.js-14.x-black.svg)](https://nextjs.org/)
[![AWS Bedrock](https://img.shields.io/badge/AWS%20Bedrock-Claude%203%20Sonnet-orange.svg)](https://aws.amazon.com/bedrock/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.x-blue.svg)](https://www.typescriptlang.org/)

> **A mission-critical, unified disaster management ecosystem connecting Citizens, AI, Command Centers, Rescue Responders, Hospitals, and Relief Logistics Supply Chains into one synchronized network.**

---

## 🌟 The Vision

Disasters do not end when search and rescue teams leave the scene. Communities require immediate shelter, food packs, safe drinking water, and continuous humanitarian supply chains.

**ADRN unifies emergency response and humanitarian relief into a single operational canvas.** Rather than operating disjointed emergency hotlines and disparate relief donation trackers, ADRN orchestrates the **complete disaster lifecycle**:

$$\textbf{Report} \longrightarrow \textbf{Analyze} \longrightarrow \textbf{Prioritize} \longrightarrow \textbf{Rescue} \longrightarrow \textbf{Hospitalize} \longrightarrow \textbf{Relief Supply} \longrightarrow \textbf{Recovery}$$

---

## ⚡ Core Operational Pillars

1. **Citizen Emergency & Relief Reporting (`/citizen`)**
   - 1-click SOS broadcast with real-time GPS telemetry.
   - Multilingual voice memo transcription and photo evidence upload.
   - Offline-capable PWA reporting for disconnected disaster zones.
2. **AI Triaging, NLP & Supply Matching Engine**
   - Multilingual triaging (Sinhala, Tamil, English) via **Amazon Bedrock (Claude 3 Sonnet)** with deterministic offline heuristic fallback.
   - Automated severity rating (P1 Critical to P4 Minor) and casualty estimation.
   - Proximity-based relief source recommendation algorithm matching camps with nearby food hubs.
3. **Command Center & Operational GIS Map (`/command`)**
   - Real-time geospatial map with dynamic heatmaps and danger radius contours.
   - Live fleet and responder tactical telemetry.
   - High-level Relief Supply Chain metrics and inventory widgets.
4. **Rescue Teams & Field Dispatch (`/rescue`)**
   - Active mission dispatch, turn-by-turn routing, and live status reporting.
   - Unified tactical feed for rescue personnel and volunteer boat operators.
5. **Hospital & Medical Triage (`/hospital`)**
   - Live ICU and general bed capacity tracking.
   - Blood bank supply indicators and automated ambulance diversion.
6. **Relief Logistics & Aid Supply Chain (`/relief`)**
   - Central registry for **Community Kitchens, Warehouses, Food Banks, and Water Distribution Points**.
   - Live tracking of meal portions, water liters, baby formula, and dry ration kits.
   - Delivery fleet dispatch with live transit tracking and spoilage risk monitoring.

---

## 🏗️ Architecture & Technology Stack

| Tier | Technologies |
| :--- | :--- |
| **Frontend** | Next.js 14 (App Router), TypeScript, TailwindCSS + NOVA Cyber Design System, Framer Motion, Lucide Icons |
| **Backend** | Java 17, Spring Boot 3.2, Spring Security (Stateless JWT), Spring WebSocket (STOMP / SockJS), Spring Data |
| **Database & Cache**| MongoDB Atlas (Geospatial 2dsphere indexes), Redis |
| **AI Intelligence** | Amazon Bedrock (Anthropic Claude 3 Sonnet), Built-in Deterministic Mock AI |
| **Infrastructure** | Docker, Docker Compose, AWS LocalStack, AWS ECS Fargate, AWS S3 |

---

## 🚀 Quick Start Guide

### Prerequisites
- **Node.js**: v18.x or higher
- **Java JDK**: v17 or higher
- **Maven**: v3.8+
- **MongoDB**: Local MongoDB instance on `localhost:27017` or MongoDB Atlas URI

### 1. Backend Setup

```bash
cd project-nova/backend

# Copy environment template
cp .env.example .env

# Compile and run the Spring Boot server
mvn clean spring-boot:run
```
The API server starts on **`http://localhost:8080`** with seeded demonstration data.

### 2. Frontend Setup

```bash
cd project-nova/frontend

# Install dependencies
npm install

# Start Next.js development server
npm run dev
```
The application opens at **`http://localhost:3000`**.

---

## 🐳 Running with Docker Compose

To spin up the entire ADRN ecosystem (MongoDB, Redis, LocalStack, Backend, Frontend) with a single command:

```bash
cd project-nova/infrastructure/docker
docker-compose up --build
```

---

## 🔐 Demonstration Credentials (Pre-seeded)

| Role | Email | Password | Access Portal |
| :--- | :--- | :--- | :--- |
| **Command Officer** | `officer@nova.org` | `Password123!` | `/command`, `/relief` |
| **Rescue Leader** | `rescue@nova.org` | `Password123!` | `/rescue`, `/relief/missions` |
| **Hospital Coordinator**| `hospital@nova.org` | `Password123!` | `/hospital` |
| **Citizen** | `citizen@nova.org` | `Password123!` | `/citizen` |
| **Administrator** | `admin@nova.org` | `Password123!` | All Portals |

---

## 📚 Technical Documentation

- **[System Architecture](docs/architecture.md)**: Deep dive into the data pipelines, ML triaging, and disaster lifecycle.
- **[API Reference](docs/api.md)**: Complete REST API schemas, request/response models, and error structures.
- **[WebSocket Events & Telemetry](docs/websocket-events.md)**: Real-time STOMP topic catalog and reconnection policies.
- **[AWS Cloud Deployment](docs/aws-deployment.md)**: Production deployment instructions for ECS Fargate, S3, and Bedrock.
