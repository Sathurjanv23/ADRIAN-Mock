package com.nova.emergency.config;

import com.nova.emergency.model.Incident;
import com.nova.emergency.model.RescueTeam;
import com.nova.emergency.model.Hospital;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "com.nova.emergency.repository")
@EnableMongoAuditing
public class MongoConfig {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MongoConfig.class);

    @Bean
    public ApplicationRunner initMongoIndices(MongoTemplate mongoTemplate) {
        return args -> {
            try {
                // Incident operational query indices
                mongoTemplate.indexOps(Incident.class)
                    .ensureIndex(new Index().on("trackingCode", Sort.Direction.ASC));
                mongoTemplate.indexOps(Incident.class)
                    .ensureIndex(new Index().on("status", Sort.Direction.ASC).on("priority", Sort.Direction.ASC));
                mongoTemplate.indexOps(Incident.class)
                    .ensureIndex(new Index().on("severity", Sort.Direction.ASC).on("priority", Sort.Direction.ASC));
                mongoTemplate.indexOps(Incident.class)
                    .ensureIndex(new Index().on("type", Sort.Direction.ASC));
                mongoTemplate.indexOps(Incident.class)
                    .ensureIndex(new Index().on("reportedAt", Sort.Direction.DESC));

                // Rescue Team indices
                mongoTemplate.indexOps(RescueTeam.class)
                    .ensureIndex(new Index().on("status", Sort.Direction.ASC).on("district", Sort.Direction.ASC));

                // Hospital indices
                mongoTemplate.indexOps(Hospital.class)
                    .ensureIndex(new Index().on("district", Sort.Direction.ASC).on("isActive", Sort.Direction.ASC));

                log.info("Verified all MongoDB operational indexes for Project NOVA.");
            } catch (Exception e) {
                log.warn("Could not verify/create MongoDB indexes: {}", e.getMessage());
            }
        };
    }
}

