package com.nova.emergency.service;

import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * GridFsStorageService — stores citizen-uploaded images and voice notes
 * directly in MongoDB Atlas using GridFS (fs.files + fs.chunks collections).
 *
 * No files are written to the local filesystem.
 */
@Service
public class GridFsStorageService {

    private static final Logger log = LoggerFactory.getLogger(GridFsStorageService.class);

    private final GridFsTemplate gridFsTemplate;
    private final GridFsOperations gridFsOperations;

    public GridFsStorageService(GridFsTemplate gridFsTemplate, GridFsOperations gridFsOperations) {
        this.gridFsTemplate = gridFsTemplate;
        this.gridFsOperations = gridFsOperations;
    }

    /**
     * Stores a MultipartFile in MongoDB GridFS.
     *
     * @param file   the uploaded file (photo or audio)
     * @param prefix "photo" or "audio" — used as metadata tag
     * @return the GridFS ObjectId as a hex string, or null on failure
     */
    public String store(MultipartFile file, String prefix) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        try {
            String originalName = file.getOriginalFilename();
            String ext = resolveExtension(originalName, prefix);
            String storedFilename = prefix + "-" + java.util.UUID.randomUUID() + "." + ext;
            String contentType = resolveContentType(file.getContentType(), ext);

            try (InputStream inputStream = file.getInputStream()) {
                ObjectId objectId = gridFsTemplate.store(
                    inputStream,
                    storedFilename,
                    contentType
                );
                String id = objectId.toHexString();
                log.info("Stored {} in MongoDB GridFS: objectId={}, filename={}, size={}B",
                    prefix, id, storedFilename, file.getSize());
                return id;
            }
        } catch (IOException ex) {
            log.error("Failed to store {} in MongoDB GridFS: {}", prefix, ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * Loads a GridFS file by its ObjectId string for streaming to the client.
     *
     * @param objectId hex string of the GridFS ObjectId
     * @return GridFsResource to stream, or null if not found
     */
    public GridFsResource load(String objectId) {
        try {
            GridFSFile file = gridFsTemplate.findOne(
                new Query(Criteria.where("_id").is(new ObjectId(objectId)))
            );
            if (file == null) {
                log.warn("GridFS file not found for objectId: {}", objectId);
                return null;
            }
            return gridFsOperations.getResource(file);
        } catch (Exception ex) {
            log.error("Failed to load GridFS file {}: {}", objectId, ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * Returns the content type stored in GridFS metadata for the given objectId.
     *
     * @param objectId hex string
     * @return MIME type string, or "application/octet-stream" as fallback
     */
    public String getContentType(String objectId) {
        try {
            GridFSFile file = gridFsTemplate.findOne(
                new Query(Criteria.where("_id").is(new ObjectId(objectId)))
            );
            if (file != null && file.getMetadata() != null) {
                Object ct = file.getMetadata().get("_contentType");
                if (ct instanceof String s && !s.isBlank()) return s;
            }
        } catch (Exception ex) {
            log.warn("Could not determine content type for GridFS objectId {}: {}", objectId, ex.getMessage());
        }
        return "application/octet-stream";
    }

    /**
     * Deletes a GridFS file by its ObjectId string.
     *
     * @param objectId hex string
     */
    public void delete(String objectId) {
        try {
            gridFsTemplate.delete(new Query(Criteria.where("_id").is(new ObjectId(objectId))));
            log.info("Deleted GridFS file: {}", objectId);
        } catch (Exception ex) {
            log.warn("Could not delete GridFS file {}: {}", objectId, ex.getMessage());
        }
    }

    // ─── Helpers ────────────────────────────────────────────────────

    private String resolveExtension(String originalName, String prefix) {
        if (originalName != null && originalName.contains(".")) {
            return originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
        }
        return "audio".equals(prefix) ? "webm" : "jpg";
    }

    private String resolveContentType(String declaredContentType, String ext) {
        if (declaredContentType != null && !declaredContentType.isBlank()
                && !declaredContentType.equals("application/octet-stream")) {
            return declaredContentType;
        }
        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png"         -> "image/png";
            case "webp"        -> "image/webp";
            case "webm"        -> "audio/webm";
            case "mp4"         -> "audio/mp4";
            case "ogg"         -> "audio/ogg";
            case "wav"         -> "audio/wav";
            default            -> "application/octet-stream";
        };
    }
}
