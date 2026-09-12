package com.nova.emergency.service;

import com.nova.emergency.model.*;
import com.nova.emergency.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// ADRN Relief Module repositories
import com.nova.emergency.repository.FoodSourceRepository;
import com.nova.emergency.repository.ReliefRequestRepository;
import com.nova.emergency.repository.ReliefMissionRepository;
import com.nova.emergency.repository.InventoryRepository;

/**
 * DataSeederService — Seeds all mock data from the original TypeScript mock into MongoDB on startup.
 * Set nova.seed.enabled=false in application.properties after first successful run.
 */
@Service
public class DataSeederService implements CommandLineRunner {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DataSeederService.class);

    private final UserRepository userRepository;
    private final IncidentRepository incidentRepository;
    private final RescueTeamRepository rescueTeamRepository;
    private final HospitalRepository hospitalRepository;
    private final ResourceRepository resourceRepository;
    private final RiskPredictionRepository riskPredictionRepository;
    private final ShelterRepository shelterRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;
    // ADRN Relief Module
    private final FoodSourceRepository foodSourceRepository;
    private final ReliefRequestRepository reliefRequestRepository;
    private final ReliefMissionRepository reliefMissionRepository;
    private final InventoryRepository inventoryRepository;

    public DataSeederService(UserRepository userRepository, IncidentRepository incidentRepository,
                             RescueTeamRepository rescueTeamRepository, HospitalRepository hospitalRepository,
                             ResourceRepository resourceRepository, RiskPredictionRepository riskPredictionRepository,
                             ShelterRepository shelterRepository, NotificationRepository notificationRepository,
                             AuditLogRepository auditLogRepository, PasswordEncoder passwordEncoder,
                             FoodSourceRepository foodSourceRepository, ReliefRequestRepository reliefRequestRepository,
                             ReliefMissionRepository reliefMissionRepository, InventoryRepository inventoryRepository) {
        this.userRepository = userRepository;
        this.incidentRepository = incidentRepository;
        this.rescueTeamRepository = rescueTeamRepository;
        this.hospitalRepository = hospitalRepository;
        this.resourceRepository = resourceRepository;
        this.riskPredictionRepository = riskPredictionRepository;
        this.shelterRepository = shelterRepository;
        this.notificationRepository = notificationRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.foodSourceRepository = foodSourceRepository;
        this.reliefRequestRepository = reliefRequestRepository;
        this.reliefMissionRepository = reliefMissionRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Value("${nova.seed.enabled:true}")
    private boolean seedEnabled;

    @Override
    public void run(String... args) {
        if (!seedEnabled) {
            log.info("Data seeding is disabled. Skipping.");
            return;
        }

        try {
            // ── One-time migration: ensure all users have approvalStatus=APPROVED ──
            // Fixes users created before the auth role fix was applied.
            userRepository.findAll().forEach(u -> {
                boolean changed = false;
                if (!"APPROVED".equalsIgnoreCase(u.getApprovalStatus())) {
                    u.setApprovalStatus("APPROVED");
                    changed = true;
                }
                if ("rescue_team".equals(u.getRole()) && u.getRescueTeamId() == null) {
                    u.setRescueTeamId("RT-ALPHA-01");
                    changed = true;
                }
                if (!u.isVerified()) {
                    u.setVerified(true);
                    changed = true;
                }
                if (!u.isActive()) {
                    u.setActive(true);
                    u.setStatus("ACTIVE");
                    changed = true;
                }
                if (changed) {
                    userRepository.save(u);
                }
            });

            if (userRepository.findByEmail("rescue@nova.lk").isEmpty()) {
                log.info("Seeding essential users...");
                seedUsers();
            }
            seedRescueTeams();
            List.of("citizen@nova.lk", "officer@nova.lk", "rescue@nova.lk", "hospital@nova.lk", "admin@nova.lk")
                .forEach(email -> userRepository.findByEmail(email).ifPresent(user -> {
                    user.setApprovalStatus("APPROVED");
                    if ("rescue@nova.lk".equals(email)) user.setRescueTeamId("RT-ALPHA-01");
                    user.setStatus("ACTIVE");
                    userRepository.save(user);
                }));
            seedHospitals();
            if (incidentRepository.count() == 0) {
                seedIncidents();
            }
            seedResources();
            if (riskPredictionRepository.count() == 0) {
                seedRiskPredictions();
            }
            if (shelterRepository.count() == 0) {
                seedShelters();
            }
            seedNotifications();
            seedIncidentAlerts();
            if (auditLogRepository.count() == 0) {
                seedAuditLogs();
            }
            // ─── ADRN Relief Module Seeding ───────────────────────────
            if (foodSourceRepository.count() == 0) {
                seedFoodSources();
            }
            if (reliefRequestRepository.count() == 0) {
                seedReliefData();
            }

            log.info("=== Seeding check complete. ===");

        } catch (Exception e) {
            log.error("=== NOVA: Could not connect to MongoDB Atlas. App will still run. ===");
            log.error(">>> Cause: {}", e.getMessage());
        }
    }

    // ─── Users ──────────────────────────────────────────────────

    private void seedUsers() {
        String demoPassword = passwordEncoder.encode("Demo1234");
        String now = Instant.now().toString();

        List<User> users = List.of(
            makeUser("u001", "Amal Perera",          "citizen@nova.lk",  demoPassword, "citizen",     "+94771234567", "Colombo",  null,                             "en", now),
            makeUser("u002", "Dilrukshi Silva",       "officer@nova.lk",  demoPassword, "officer",     "+94779876543", "Colombo",  "Disaster Management Centre",     "en", now),
            makeUser("u003", "Sgt. Kapila Fernando",  "rescue@nova.lk",   demoPassword, "rescue_team", "+94772345678", "Gampaha",  "SL Rescue Corps",                "si", now),
            makeUser("u004", "Dr. Priya Rajapaksa",  "hospital@nova.lk", demoPassword, "hospital",    "+94773456789", "Colombo",  "National Hospital Sri Lanka",    "en", now),
            makeUser("u005", "Nova Admin",            "admin@nova.lk",    demoPassword, "admin",       null,           null,       "PROJECT NOVA",                   "en", now),
            makeUser("u006", "Muthu Selvam",          "muthu@nova.lk",    demoPassword, "citizen",     "+94774567890", "Jaffna",   null,                             "ta", now)
        );

        int count = 0;
        for (User u : users) {
            if (userRepository.findByEmail(u.getEmail().toLowerCase().trim()).isEmpty()) {
                userRepository.save(u);
                count++;
            }
        }
        log.info("Seeded {} new demo users", count);
    }

    private User makeUser(String id, String name, String email, String hash, String role,
                          String phone, String district, String org, String lang, String now) {
        User u = new User();
        u.setId(id);
        u.setName(name);
        u.setEmail(email);
        u.setPasswordHash(hash);
        u.setRole(role);
        u.setApprovalStatus("citizen".equals(role) ? "APPROVED" : "PENDING_APPROVAL");
        if ("rescue_team".equals(role)) {
            u.setRescueTeamId("RT-ALPHA-01");
            u.setApprovalStatus("APPROVED");
        }
        u.setPhone(phone);
        u.setDistrict(district);
        u.setOrganization(org);
        u.setLanguage(lang);
        u.setCreatedAt("2024-01-01T00:00:00Z");
        u.setLastActive(now);
        u.setActive(true);
        u.setVerified(true);
        u.setProvider("email");
        return u;
    }

    // ─── Incidents ──────────────────────────────────────────────

    private void seedIncidents() {
        String t = Instant.now().toString();
        String t3m = Instant.now().minus(3, ChronoUnit.MINUTES).toString();
        String t8m = Instant.now().minus(8, ChronoUnit.MINUTES).toString();
        String t10m = Instant.now().minus(10, ChronoUnit.MINUTES).toString();
        String t12m = Instant.now().minus(12, ChronoUnit.MINUTES).toString();
        String t20m = Instant.now().minus(20, ChronoUnit.MINUTES).toString();
        String t35m = Instant.now().minus(35, ChronoUnit.MINUTES).toString();
        String t45m = Instant.now().minus(45, ChronoUnit.MINUTES).toString();
        String t3h  = Instant.now().minus(3, ChronoUnit.HOURS).toString();
        String t1h  = Instant.now().minus(1, ChronoUnit.HOURS).toString();

        List<Incident> incidents = List.of(
            makeIncident("NOV-1042", "flood", "critical", "en_route",
                "House surrounded by floodwater — elderly trapped",
                "Entire ground floor submerged. Elderly man unable to move. Water still rising. Located near Kelani River bank.",
                6.9271, 79.9817, "42 River View Road, Kaduwela", "Colombo", "Zone 04",
                "u001", "Amal Perera", t12m, t3m, 5, "RT-ALPHA-02", "Team Bravo", 8, 1),

            makeIncident("NOV-1039", "flood", "critical", "responding",
                "Residential collapse risk — family of 6 stranded on rooftop",
                "Family of 6 including a pregnant woman stranded on rooftop. Structural damage visible. Water level at 1.5m.",
                6.9559, 79.9156, "15 Kelani Mawatha, Kelaniya", "Gampaha", "Zone 03",
                "u006", "Muthu Selvam", t35m, Instant.now().minus(5, ChronoUnit.MINUTES).toString(), 6, "RT-GAMMA-01", "Team Delta", 2, 2),

            makeIncident("NOV-1040", "landslide", "high", "assigned",
                "Landslide blocking main road — 3 vehicles trapped",
                "Major landslide has blocked the A9 highway near Kandy. 3 vehicles and approximately 12 persons trapped.",
                7.2906, 80.6337, "A9 Highway km 78, Kandy", "Kandy", "Zone 07",
                "u001", "Road User", t20m, t10m, 12, "RT-BETA-03", "Team Echo", 15, 3),

            makeIncident("NOV-1041", "medical", "high", "dispatched",
                "Medical emergency — suspected cardiac arrest",
                "Elderly male, 68 years old, suspected cardiac arrest. Bystanders performing CPR.",
                6.9271, 79.8612, "Pettah Market, Colombo 11", "Colombo", "Zone 01",
                "u001", "Bystander", t8m, Instant.now().minus(2, ChronoUnit.MINUTES).toString(), 1, null, null, 4, 4),

            makeIncident("NOV-1038", "flood", "medium", "prioritized",
                "Flooded agricultural land — cattle stranded",
                "Paddy fields completely inundated. Farmer reports 40 cattle stranded on elevated ground. No human casualties.",
                7.0873, 80.0171, "Biyagama Agri Zone, Gampaha", "Gampaha", "Zone 05",
                "u001", "Farmer", t45m, Instant.now().minus(40, ChronoUnit.MINUTES).toString(), 2, null, null, null, 8),

            makeIncident("NOV-1035", "fire", "high", "resolved",
                "Warehouse fire — Colombo Port area",
                "Large warehouse fire at port container yard. Fire brigade responded. All persons evacuated.",
                6.9271, 79.8474, "Port Container Yard, Colombo 01", "Colombo", "Zone 01",
                "u001", "Port Security", t3h, t1h, 0, null, null, null, 10)
        );

        incidentRepository.saveAll(incidents);
        log.info("Seeded {} incidents", incidents.size());
    }

    private Incident makeIncident(String id, String type, String severity, String status,
                                  String title, String description,
                                  double lat, double lng, String address, String district, String zone,
                                  String reportedBy, String reporterName,
                                  String reportedAt, String updatedAt, int peopleAffected,
                                  String assignedTeam, String assignedTeamName,
                                  Integer eta, Integer priority) {
        Incident inc = new Incident();
        inc.setId(id);
        inc.setTrackingCode(id);
        inc.setType(type);
        inc.setSeverity(severity);
        inc.setStatus(status);
        inc.setTitle(title);
        inc.setDescription(description);

        Incident.GeoLocation loc = new Incident.GeoLocation();
        loc.setLat(lat); loc.setLng(lng);
        loc.setAddress(address); loc.setDistrict(district); loc.setZone(zone);
        inc.setLocation(loc);

        inc.setReportedBy(reportedBy);
        inc.setReporterName(reporterName);
        inc.setReportedAt(reportedAt);
        inc.setUpdatedAt(updatedAt);
        inc.setPeopleAffected(peopleAffected);
        inc.setAssignedTeam(assignedTeam);
        inc.setAssignedTeamId(assignedTeam);
        inc.setAssignedTeamName(assignedTeamName);
        inc.setEta(eta);
        inc.setPriority(priority);
        inc.setNotes(new ArrayList<>());
        inc.setAttachments(new ArrayList<>());
        List<Incident.IncidentUpdate> updates = new ArrayList<>();
        updates.add(new Incident.IncidentUpdate("upd-" + id, status, "Status: " + status, "System", reportedAt, false));
        inc.setUpdates(updates);
        inc.setResourcesAllocated(new ArrayList<>());
        return inc;
    }

    // ─── Rescue Teams ────────────────────────────────────────────

    private void seedRescueTeams() {
        String now = Instant.now().toString();

        List<RescueTeam> teams = List.of(
            makeTeam("RT-ALPHA-01", "Team Alpha", "ALPHA-01", "available",
                6.9271, 79.9012, "DMC Base Station, Colombo", "Colombo", "+94771234001",
                "Rescue Truck + Boat", null, null, now,
                List.of("Water Rescue", "First Aid", "Search & Rescue")),

            makeTeam("RT-ALPHA-02", "Team Bravo", "BRAVO-02", "en_route",
                6.9380, 79.9650, "Kelaniya Depot", "Gampaha", "+94771234002",
                "Light Rescue Vehicle + Inflatable Boat", "NOV-1042", 8, now,
                List.of("Flood Rescue", "Medical Response", "Evacuation")),

            makeTeam("RT-GAMMA-01", "Team Delta", "DELTA-01", "on_scene",
                6.9559, 79.9156, "Kelaniya Bridge Area", "Gampaha", "+94771234003",
                "Heavy Rescue + Motor Boat", "NOV-1039", 0, now,
                List.of("Dive Rescue", "Swift Water", "Medical", "Vertical Rescue")),

            makeTeam("RT-BETA-03", "Team Echo", "ECHO-03", "assigned",
                7.2534, 80.5936, "Kandy Base", "Kandy", "+94771234004",
                "Heavy Rescue Truck + Excavator", "NOV-1040", 15, now,
                List.of("Heavy Machinery", "Landslide Response", "Road Clearing", "Medical")),

            makeTeam("RT-DELTA-02", "Team Fox", "FOX-02", "unavailable",
                6.0174, 80.2170, "Galle Base", "Galle", "+94771234005",
                "Coastal Rescue Vessel", null, null, now,
                List.of("Coastal Rescue", "Medical"))
        );

        List<RescueTeam> missing = teams.stream()
            .filter(team -> rescueTeamRepository.findById(team.getId()).isEmpty())
            .toList();
        rescueTeamRepository.saveAll(missing);
        log.info("Seeded {} missing rescue teams", missing.size());
    }

    private RescueTeam makeTeam(String id, String name, String code, String status,
                                double lat, double lng, String address, String district,
                                String contact, String vehicleType, String currentIncident,
                                Integer eta, String lastUpdated, List<String> capabilities) {
        RescueTeam t = new RescueTeam();
        t.setId(id); t.setName(name); t.setCode(code); t.setStatus(status);
        RescueTeam.GeoLocation loc = new RescueTeam.GeoLocation();
        loc.setLat(lat); loc.setLng(lng); loc.setAddress(address); loc.setDistrict(district);
        t.setLocation(loc);
        t.setDistrict(district); t.setContact(contact); t.setVehicleType(vehicleType);
        t.setCurrentIncident(currentIncident); t.setEta(eta); t.setLastUpdated(lastUpdated);
        t.setCapabilities(capabilities);
        t.setMembers(List.of());
        t.setEquipment(List.of());
        return t;
    }

    // ─── Hospitals ───────────────────────────────────────────────

    private void seedHospitals() {
        String now = Instant.now().toString();

        List<Hospital> hospitals = List.of(
            makeHospital("h001", "National Hospital Sri Lanka", "teaching",
                6.9271, 79.8660, "Regent Street, Colombo 10", "Colombo", "+94112691111",
                3200, 47, 80, 8, 6,
                List.of("Trauma", "Cardiac", "Neurology", "Burns"), now,
                List.of(
                    new Hospital.AmbulanceUnit("amb-h001-1", "NHSL-AMB-01", "available", null, null),
                    new Hospital.AmbulanceUnit("amb-h001-2", "NHSL-AMB-02", "dispatched", 8, "NOV-1041"),
                    new Hospital.AmbulanceUnit("amb-h001-3", "NHSL-AMB-03", "available", null, null),
                    new Hospital.AmbulanceUnit("amb-h001-4", "NHSL-AMB-04", "dispatched", 12, "NOV-1039"),
                    new Hospital.AmbulanceUnit("amb-h001-5", "NHSL-AMB-05", "available", null, null),
                    new Hospital.AmbulanceUnit("amb-h001-6", "NHSL-AMB-06", "maintenance", null, null)
                ),
                List.of(
                    new Hospital.IncomingCase("ic001", "NOV-1041", "critical", 4, 1,
                        "Suspected cardiac arrest — elderly male", List.of("Cardiac", "ICU"), "amb-h001-2"),
                    new Hospital.IncomingCase("ic002", "NOV-1039", "high", 12, 2,
                        "Flood trauma — multiple injuries", List.of("Trauma", "Surgery"), "amb-h001-4")
                )),

            makeHospital("h002", "Colombo South Teaching Hospital", "teaching",
                6.8765, 79.8601, "Hospital Road, Kalubowila", "Colombo", "+94112510555",
                1800, 123, 40, 12, 4,
                List.of("Emergency Medicine", "Surgery", "Orthopedics"), now,
                List.of(
                    new Hospital.AmbulanceUnit("amb-h002-1", "CSTH-AMB-01", "available", null, null),
                    new Hospital.AmbulanceUnit("amb-h002-2", "CSTH-AMB-02", "available", null, null),
                    new Hospital.AmbulanceUnit("amb-h002-3", "CSTH-AMB-03", "dispatched", 15, "NOV-1042")
                ),
                List.of(
                    new Hospital.IncomingCase("ic003", "NOV-1042", "critical", 8, 1,
                        "Flood rescue — elderly trapped", List.of("Emergency", "Trauma"), "amb-h002-3")
                )),

            makeHospital("h003", "Kelaniya Teaching Hospital", "teaching",
                6.9559, 79.9209, "Kelaniya", "Gampaha", "+94112910000",
                900, 34, 20, 2, 3,
                List.of("Emergency", "Obstetrics", "Pediatrics"), now,
                List.of(
                    new Hospital.AmbulanceUnit("amb-h003-1", "KTH-AMB-01", "available", null, null),
                    new Hospital.AmbulanceUnit("amb-h003-2", "KTH-AMB-02", "dispatched", 6, "NOV-1039")
                ),
                List.of()),

            makeHospital("h004", "Kandy General Hospital", "general",
                7.2906, 80.6337, "William Gopallawa Mawatha, Kandy", "Kandy", "+94812222261",
                1200, 89, 30, 7, 3,
                List.of("General Surgery", "Emergency", "Orthopedics"), now,
                List.of(
                    new Hospital.AmbulanceUnit("amb-h004-1", "KGH-AMB-01", "available", null, null),
                    new Hospital.AmbulanceUnit("amb-h004-2", "KGH-AMB-02", "available", null, null)
                ),
                List.of())
        );

        // Always upsert hospitals so ambulance/case data is refreshed on restart
        hospitalRepository.saveAll(hospitals);
        log.info("Upserted {} hospitals with ambulance fleet data", hospitals.size());
    }

    private Hospital makeHospital(String id, String name, String type,
                                  double lat, double lng, String address, String district, String contact,
                                  int totalBeds, int availableBeds, int icuTotal, int icuAvailable,
                                  int emergencyTeams, List<String> specializations, String lastUpdated,
                                  List<Hospital.AmbulanceUnit> ambulances,
                                  List<Hospital.IncomingCase> incomingCases) {
        Hospital h = new Hospital();
        h.setId(id); h.setName(name); h.setType(type); h.setDistrict(district); h.setContact(contact);
        Hospital.GeoLocation loc = new Hospital.GeoLocation();
        loc.setLat(lat); loc.setLng(lng); loc.setAddress(address); loc.setDistrict(district);
        h.setLocation(loc);
        h.setTotalBeds(totalBeds); h.setAvailableBeds(availableBeds);
        h.setIcuTotal(icuTotal); h.setIcuAvailable(icuAvailable);
        h.setEmergencyTeams(emergencyTeams);
        h.setSpecializations(specializations);
        h.setAmbulances(ambulances != null ? ambulances : List.of());
        h.setIncomingCases(incomingCases != null ? incomingCases : List.of());
        h.setActive(true); h.setLastUpdated(lastUpdated);
        return h;
    }

    // ─── Resources ───────────────────────────────────────────────

    private void seedResources() {
        Resource.GeoLocation depot = new Resource.GeoLocation(6.9271, 79.8660, "Central Depot, Colombo", "Colombo");

        List<Resource> resources = List.of(
            makeResource("r001", "rescue_boat",       "Inflatable Rescue Boats",      24, 18, 2,  4,  "available", depot, "2024-01-10", "units", 5),
            makeResource("r002", "ambulance",         "ALS Ambulances",               32, 24, 0,  8,  "available", depot, "2024-01-15", "units", 5),
            makeResource("r003", "medical_kit",       "Emergency Medical Kits",       450,312,48, 90, "available", depot, "2024-01-12", "kits",  50),
            makeResource("r004", "food_pack",         "Emergency Food Packs",         5000,3200,800,1000,"available",depot,"2024-01-08","packs",500),
            makeResource("r005", "water_unit",        "Clean Water Units (20L)",      8000,4100,1200,2700,"low_stock",depot,"2024-01-05","units",1000),
            makeResource("r006", "medicine",          "Emergency Medicine Kits",      200,87,40,  73, "low_stock", depot, "2024-01-01", "kits",  30),
            makeResource("r007", "shelter",           "Emergency Shelter Capacity",   500,340,60, 100,"available",
                new Resource.GeoLocation(6.8900, 79.8660, "Dehiwala Grounds", "Colombo"), "2024-01-14", "spaces", 50),
            makeResource("r008", "rescue_personnel",  "Trained Rescue Personnel",     280,165,0,  115,"available", depot, "2024-01-01", "personnel", 30)
        );

        List<Resource> missing = resources.stream()
            .filter(resource -> resourceRepository.findById(resource.getId()).isEmpty())
            .toList();
        resourceRepository.saveAll(missing);
        log.info("Seeded {} missing resources", missing.size());
    }

    private Resource makeResource(String id, String type, String name, int qty, int avail, int reserved,
                                  int deployed, String status, Resource.GeoLocation loc, String lastRestocked,
                                  String unit, int threshold) {
        Resource r = new Resource();
        r.setId(id); r.setType(type); r.setName(name); r.setQuantity(qty);
        r.setCategory(type);
        r.setAvailable(avail); r.setReserved(reserved); r.setDeployed(deployed);
        r.setStatus(status); r.setLocation(loc); r.setLastRestocked(lastRestocked);
        r.setUnit(unit); r.setLowStockThreshold(threshold);
        return r;
    }

    // ─── Risk Predictions ────────────────────────────────────────

    private void seedRiskPredictions() {
        String now = Instant.now().toString();

        List<RiskPrediction> predictions = List.of(
            makeRisk("rp001","Zone 04","Colombo", 6.9271,79.9817, 82,15,5,78,"critical",187,4.8,26,45,8500,3,87,42000,now,
                List.of("Prepare Shelter B for Zone 4 residents","Move emergency resources closer to Zone 4","Monitor River Station 02 continuously","Issue early warning to Zone 4 residents"),
                "43% higher than 2023 flood event"),
            makeRisk("rp002","Zone 03","Gampaha", 6.9559,79.9156, 74,22,8,70,"high",154,3.9,25,38,6200,3,84,28000,now,
                List.of("Activate Shelter C as precautionary measure","Position rescue boats at Kelani River bridges"),
                "28% higher than 2022 seasonal average"),
            makeRisk("rp003","Zone 07","Kandy", 7.2906,80.6337, 32,68,12,55,"high",142,1.8,22,52,4100,6,79,15000,now,
                List.of("Issue landslide warning for hill settlements","Restrict heavy vehicle movement on A9"),
                "Consistent with historical monsoon patterns"),
            makeRisk("rp004","Zone 01","Colombo", 6.9271,79.8474, 28,5,20,25,"medium",89,1.2,29,22,15000,6,75,8000,now,
                List.of("Monitor densely populated areas","Fire risk elevated due to dry pockets"),
                "Normal seasonal baseline"),
            makeRisk("rp005","Zone 02","Gampaha", 7.0873,80.0171, 45,18,6,40,"medium",122,2.4,27,30,3800,3,81,12000,now,
                List.of("Alert agricultural communities","Position one rescue team in standby"),
                "12% above seasonal average")
        );

        riskPredictionRepository.saveAll(predictions);
        log.info("Seeded {} risk predictions", predictions.size());
    }

    private RiskPrediction makeRisk(String id, String zone, String district, double lat, double lng,
                                    int flood, int landslide, int fire, int overall, String riskLevel,
                                    double rainfall, double riverLevel, double temp, double wind,
                                    int popDensity, int window, int confidence, int affectedPop, String updatedAt,
                                    List<String> recommendations, String historical) {
        RiskPrediction rp = new RiskPrediction();
        rp.setId(id); rp.setZone(zone); rp.setDistrict(district);
        rp.setLocation(new RiskPrediction.GeoLocation(lat, lng));
        rp.setFloodRisk(flood); rp.setLandslideRisk(landslide); rp.setFireRisk(fire);
        rp.setOverallRisk(overall); rp.setRiskLevel(riskLevel);
        rp.setRainfall(rainfall); rp.setRiverLevel(riverLevel);
        rp.setTemperature(temp); rp.setWindSpeed(wind);
        rp.setPopulationDensity(popDensity); rp.setPredictionWindow(window);
        rp.setConfidence(confidence); rp.setAffectedPopulation(affectedPop);
        rp.setUpdatedAt(updatedAt); rp.setAiRecommendations(recommendations);
        rp.setHistoricalComparison(historical);
        return rp;
    }

    // ─── Shelters ────────────────────────────────────────────────

    private void seedShelters() {
        List<Shelter> shelters = List.of(
            makeShelter("sh001", "Shelter A — Dehiwala Sports Complex", 6.8790, 79.8650, "Dehiwala", "Colombo", "Colombo", 500, 160, "active", "+94112345678", List.of("Meals","Medical","Toilets","Beds")),
            makeShelter("sh002", "Shelter B — Zone 4 Community Center",  6.9271, 79.9500, "Kaduwela", "Colombo", "Colombo", 300, 0,   "standby", "+94117654321", List.of("Meals","Toilets","Basic Medical")),
            makeShelter("sh003", "Shelter C — Kelaniya Municipal Hall",  6.9559, 79.9209, "Kelaniya Town", "Gampaha", "Gampaha", 400, 95, "active", "+94119876543", List.of("Meals","Medical","Toilets","Beds","Counseling"))
        );
        shelterRepository.saveAll(shelters);
        log.info("Seeded {} shelters", shelters.size());
    }

    private Shelter makeShelter(String id, String name, double lat, double lng, String address,
                                String district, String shelterDistrict, int capacity, int occupancy,
                                String status, String contact, List<String> amenities) {
        Shelter s = new Shelter();
        s.setId(id); s.setName(name); s.setCapacity(capacity); s.setCurrentOccupancy(occupancy);
        s.setStatus(status); s.setContact(contact); s.setDistrict(shelterDistrict);
        s.setAmenities(amenities);
        Shelter.GeoLocation loc = new Shelter.GeoLocation();
        loc.setLat(lat); loc.setLng(lng); loc.setAddress(address); loc.setDistrict(district);
        s.setLocation(loc);
        return s;
    }

    // ─── Notifications ───────────────────────────────────────────

    private void seedNotifications() {
        String t = Instant.now().toString();

        List<Notification> notifs = List.of(
            makeNotif("notif001", "critical_incident", "CRITICAL: NOV-1042 Activated",
                "New critical flood incident in Zone 4. Team Bravo dispatched.", "critical", false, t, "NOV-1042", "incident"),
            makeNotif("notif002", "prediction_alert", "Risk Escalation: Zone 04",
                "Flood risk in Zone 04 has exceeded 80%. River level at 4.8m.", "critical", false, t, "rp001", "prediction"),
            makeNotif("notif003", "resource_shortage", "Low Stock: Water Units",
                "Clean water units below threshold. 4,100 remaining.", "high", false, t, null, null),
            makeNotif("notif004", "hospital_capacity", "Hospital Alert: Kelaniya",
                "Kelaniya Teaching Hospital ICU at 90% capacity. 2 beds remaining.", "high", true, t, "h003", "hospital"),
            makeNotif("notif005", "team_status", "Team Delta On Scene",
                "Team Delta has arrived at NOV-1039. Extraction in progress.", "medium", true, t, "NOV-1039", "incident"),
            makeNotif("notif006", "weather_alert", "Severe Weather Warning",
                "Meteorological Department issues red alert for Western Province.", "high", false, t, null, null)
        );

        List<Notification> missing = notifs.stream()
            .filter(notification -> notificationRepository.findById(notification.getId()).isEmpty())
            .toList();
        notificationRepository.saveAll(missing);
        log.info("Seeded {} missing notifications", missing.size());
    }

        private void seedIncidentAlerts() {
        List<Notification> existing = notificationRepository.findAll();
        List<Notification> missing = incidentRepository.findAll().stream()
            .filter(incident -> "critical".equalsIgnoreCase(incident.getSeverity()))
            .filter(incident -> existing.stream().noneMatch(notification -> incident.getId().equals(notification.getRelatedId())))
            .map(incident -> makeNotif(
                "alert-" + incident.getId(),
                "critical_incident",
                "Critical incident: " + incident.getId(),
                incident.getTitle() != null ? incident.getTitle() : "Critical incident requires response",
                "critical",
                false,
                incident.getUpdatedAt() != null ? incident.getUpdatedAt() : incident.getReportedAt(),
                incident.getId(),
                "incident"))
            .toList();
        notificationRepository.saveAll(missing);
        log.info("Seeded {} missing incident alerts", missing.size());
        }

    private Notification makeNotif(String id, String type, String title, String message,
                                   String severity, boolean read, String createdAt,
                                   String relatedId, String relatedType) {
        Notification n = new Notification();
        n.setId(id); n.setType(type); n.setTitle(title); n.setMessage(message);
        n.setSeverity(severity); n.setRead(read); n.setCreatedAt(createdAt);
        n.setRelatedId(relatedId); n.setRelatedType(relatedType);
        return n;
    }

    // ─── Audit Logs ──────────────────────────────────────────────

    private void seedAuditLogs() {
        String t = Instant.now().toString();

        List<AuditLog> logs = List.of(
            makeAuditLog("al001", "u002", "Dilrukshi Silva", "ASSIGN_TEAM", "incident", "NOV-1042",
                "Assigned Team Bravo to critical flood incident", "192.168.1.45", t, "info"),
            makeAuditLog("al002", "u002", "Dilrukshi Silva", "ESCALATE_INCIDENT", "incident", "NOV-1039",
                "Escalated incident priority from High to Critical", "192.168.1.45", t, "warning"),
            makeAuditLog("al003", "u005", "Admin Nova", "AI_CONFIG_UPDATE", "ai_model", "model-001",
                "Updated flood severity threshold from 75% to 70%", "192.168.1.10", t, "warning"),
            makeAuditLog("al004", "u003", "Sgt. Kapila Fernando", "STATUS_UPDATE", "team", "RT-GAMMA-01",
                "Team Delta status changed to On Scene", "10.0.0.23", t, "info"),
            makeAuditLog("al005", "u005", "Admin Nova", "USER_CREATED", "user", "u006",
                "New citizen account created for Jaffna district", "192.168.1.10", t, "info")
        );

        auditLogRepository.saveAll(logs);
        log.info("Seeded {} audit logs", logs.size());
    }

    private AuditLog makeAuditLog(String id, String userId, String userName, String action,
                                  String resource, String resourceId, String details,
                                  String ip, String timestamp, String severity) {
        AuditLog a = new AuditLog();
        a.setId(id); a.setUserId(userId); a.setUserName(userName); a.setAction(action);
        a.setResource(resource); a.setResourceId(resourceId); a.setDetails(details);
        a.setIpAddress(ip); a.setTimestamp(timestamp); a.setSeverity(severity);
        return a;
    }

    // ─── ADRN Relief Module Seed Data ──────────────────────────────

    private void seedFoodSources() {
        String now = Instant.now().toString();
        String expiry = Instant.now().plus(8, ChronoUnit.HOURS).toString();

        List<FoodSource> sources = List.of(
            makeFoodSource("FS-001", "Grand Colombo Hotel", "HOTEL",
                6.9271, 79.8612, "Galle Road, Colombo 03", "Colombo",
                3500, 5000, expiry, "+94112345678", "ACTIVE", now),
            makeFoodSource("FS-002", "Keells Super Nugegoda", "SUPERMARKET",
                6.8726, 79.8926, "Stanley Thilakaratne Mawatha, Nugegoda", "Colombo",
                2000, 8000, expiry, "+94112456789", "ACTIVE", now),
            makeFoodSource("FS-003", "Thambapanni Restaurant", "RESTAURANT",
                6.9548, 79.9164, "Rajagiriya Junction, Rajagiriya", "Colombo",
                1200, 2000, expiry, "+94112567890", "ACTIVE", now),
            makeFoodSource("FS-004", "NFSL Emergency Warehouse Colombo", "WAREHOUSE",
                6.9422, 79.8543, "Pettah, Colombo 11", "Colombo",
                15000, 25000, expiry, "+94112678901", "ACTIVE", now),
            makeFoodSource("FS-005", "Cinnamon Lake Hotel", "HOTEL",
                6.8951, 79.8673, "Sir Chittampalam A Gardiner Mawatha, Colombo", "Colombo",
                2800, 4500, expiry, "+94112789012", "ACTIVE", now),
            makeFoodSource("FS-006", "Gampaha Relief Store", "WAREHOUSE",
                7.0917, 80.0043, "Gampaha Town, Gampaha", "Gampaha",
                8000, 12000, expiry, "+94332345678", "ACTIVE", now),
            makeFoodSource("FS-007", "Arpico Supercentre Kandy", "SUPERMARKET",
                7.2906, 80.6337, "Kandy City Centre, Kandy", "Kandy",
                3000, 6000, expiry, "+94812345678", "ACTIVE", now),
            makeFoodSource("FS-008", "Lanka Sathosa Ratnapura", "SUPERMARKET",
                6.6828, 80.4027, "Main Street, Ratnapura", "Ratnapura",
                1800, 3500, expiry, "+94452345678", "ACTIVE", now)
        );

        foodSourceRepository.saveAll(sources);
        log.info("Seeded {} food sources", sources.size());

        // Create inventory records for each source
        for (FoodSource source : sources) {
            inventoryRepository.save(makeInventory(source.getId(), source.getName(), "MEALS",
                source.getAvailableMeals(), source.getExpiryTime(), now));
            inventoryRepository.save(makeInventory(source.getId(), source.getName(), "WATER",
                source.getWaterBottles(), source.getExpiryTime(), now));
        }
        log.info("Seeded inventory records for all food sources");
    }

    private void seedReliefData() {
        String now = Instant.now().toString();

        // Relief requests linked to existing incidents
        ReliefRequest rr1 = new ReliefRequest();
        rr1.setId("RR-001");
        rr1.setIncidentId("INC-001");
        rr1.setIncidentTrackingCode("NOVA-001");
        rr1.setDisasterLocation(new ReliefRequest.GeoLocation(6.9319, 79.8478, "Wellawatte, Colombo", "Colombo"));
        rr1.setPeopleAffected(350);
        rr1.setRequiredMeals(1050);
        rr1.setRequiredWater(700);
        rr1.setPriority("CRITICAL");
        rr1.setRequestType("FOOD_AND_WATER");
        rr1.setStatus("ASSIGNED");
        rr1.setCreatedBy("SYSTEM");
        rr1.setCreatedAt(Instant.now().minus(45, ChronoUnit.MINUTES).toString());
        rr1.setUpdatedAt(now);

        ReliefRequest rr2 = new ReliefRequest();
        rr2.setId("RR-002");
        rr2.setIncidentId("INC-002");
        rr2.setIncidentTrackingCode("NOVA-002");
        rr2.setDisasterLocation(new ReliefRequest.GeoLocation(7.0917, 80.0043, "Gampaha Town, Gampaha", "Gampaha"));
        rr2.setPeopleAffected(120);
        rr2.setRequiredMeals(360);
        rr2.setRequiredWater(240);
        rr2.setPriority("HIGH");
        rr2.setRequestType("FOOD_AND_WATER");
        rr2.setStatus("PENDING");
        rr2.setCreatedBy("officer@nova.lk");
        rr2.setCreatedAt(Instant.now().minus(20, ChronoUnit.MINUTES).toString());
        rr2.setUpdatedAt(now);

        ReliefRequest rr3 = new ReliefRequest();
        rr3.setId("RR-003");
        rr3.setIncidentId("INC-003");
        rr3.setIncidentTrackingCode("NOVA-003");
        rr3.setDisasterLocation(new ReliefRequest.GeoLocation(6.8726, 79.8926, "Nugegoda, Colombo", "Colombo"));
        rr3.setPeopleAffected(55);
        rr3.setRequiredMeals(165);
        rr3.setRequiredWater(110);
        rr3.setPriority("MEDIUM");
        rr3.setRequestType("WATER_ONLY");
        rr3.setStatus("COMPLETED");
        rr3.setCreatedBy("SYSTEM");
        rr3.setCreatedAt(Instant.now().minus(120, ChronoUnit.MINUTES).toString());
        rr3.setUpdatedAt(Instant.now().minus(30, ChronoUnit.MINUTES).toString());

        reliefRequestRepository.saveAll(List.of(rr1, rr2, rr3));
        log.info("Seeded {} relief requests", 3);

        // Relief mission for RR-001
        ReliefMission rm1 = new ReliefMission();
        rm1.setId("RM-001");
        rm1.setReliefRequestId("RR-001");
        rm1.setFoodSourceId("FS-004");
        rm1.setFoodSourceName("NFSL Emergency Warehouse Colombo");
        rm1.setVehicleId("VEH-COL-01");
        rm1.setVehicleName("Relief Truck Alpha");
        rm1.setDriverName("Suresh Bandara");
        rm1.setDriverContact("+94771234567");
        rm1.setPickupLocation(new ReliefMission.GeoPoint(6.9422, 79.8543, "Pettah, Colombo 11", "Colombo"));
        rm1.setDestination(new ReliefMission.GeoPoint(6.9319, 79.8478, "Wellawatte, Colombo", "Colombo"));
        rm1.setMeals(1050);
        rm1.setWaterBottles(700);
        rm1.setEta(18);
        rm1.setDistanceKm(2.1);
        rm1.setStatus("ON_THE_WAY");
        rm1.setCurrentLat(6.9380);
        rm1.setCurrentLng(6.9380);
        rm1.setCurrentLat(6.9380);
        rm1.setCurrentLng(79.8510);
        rm1.setCreatedAt(Instant.now().minus(30, ChronoUnit.MINUTES).toString());
        rm1.setUpdatedAt(now);

        // Completed relief mission for RR-003
        ReliefMission rm2 = new ReliefMission();
        rm2.setId("RM-002");
        rm2.setReliefRequestId("RR-003");
        rm2.setFoodSourceId("FS-002");
        rm2.setFoodSourceName("Keells Super Nugegoda");
        rm2.setVehicleId("VEH-COL-02");
        rm2.setVehicleName("Relief Van Beta");
        rm2.setDriverName("Chamara Wickrama");
        rm2.setDriverContact("+94772345678");
        rm2.setPickupLocation(new ReliefMission.GeoPoint(6.8726, 79.8926, "Nugegoda", "Colombo"));
        rm2.setDestination(new ReliefMission.GeoPoint(6.8726, 79.8926, "Nugegoda", "Colombo"));
        rm2.setMeals(0);
        rm2.setWaterBottles(110);
        rm2.setEta(0);
        rm2.setDistanceKm(0.5);
        rm2.setStatus("COMPLETED");
        rm2.setCurrentLat(6.8726);
        rm2.setCurrentLng(79.8926);
        rm2.setCreatedAt(Instant.now().minus(90, ChronoUnit.MINUTES).toString());
        rm2.setUpdatedAt(Instant.now().minus(30, ChronoUnit.MINUTES).toString());
        rm2.setCompletedAt(Instant.now().minus(30, ChronoUnit.MINUTES).toString());

        reliefMissionRepository.saveAll(List.of(rm1, rm2));
        log.info("Seeded {} relief missions", 2);
    }

    private FoodSource makeFoodSource(String id, String name, String type, double lat, double lng,
                                       String address, String district, int meals, int water,
                                       String expiry, String contact, String status, String now) {
        FoodSource s = new FoodSource();
        s.setId(id);
        s.setName(name);
        s.setType(type);
        s.setLocation(new FoodSource.GeoLocation(lat, lng, address, district));
        s.setAvailableMeals(meals);
        s.setWaterBottles(water);
        s.setExpiryTime(expiry);
        s.setContact(contact);
        s.setStatus(status);
        s.setCreatedAt(now);
        s.setUpdatedAt(now);
        return s;
    }

    private Inventory makeInventory(String sourceId, String sourceName, String itemType,
                                     int quantity, String expiry, String now) {
        Inventory inv = new Inventory();
        inv.setSourceId(sourceId);
        inv.setSourceName(sourceName);
        inv.setItemType(itemType);
        inv.setQuantity(quantity);
        inv.setReservedQuantity(0);
        inv.setAvailableQuantity(quantity);
        inv.setExpiryTime(expiry);
        inv.setUpdatedAt(now);
        return inv;
    }
}
