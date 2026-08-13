package com.shresth.FrankenCloud.Repositories;

import com.shresth.FrankenCloud.DTO.Storage;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface StorageRepository extends MongoRepository<Storage, ObjectId> {
    Storage findByStorageId(ObjectId storageId);
    Storage findStorageByUserId(ObjectId userId);

}
