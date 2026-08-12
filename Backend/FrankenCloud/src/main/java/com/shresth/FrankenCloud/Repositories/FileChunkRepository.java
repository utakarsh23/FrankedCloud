package com.shresth.FrankenCloud.Repositories;

import com.shresth.FrankenCloud.Entity.FileChunk;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileChunkRepository extends MongoRepository<FileChunk, ObjectId> {

    List<FileChunk> findByFileIdOrderByChunkIndexAsc(ObjectId fileId);

    // Find chunks belonging to a specific drive account (useful for health checks)
    List<FileChunk> findByAccountId(ObjectId accountId);
}
