package com.shresth.FrankenCloud.Repositories;

import com.shresth.FrankenCloud.Entity.Files;
import com.shresth.FrankenCloud.Entity.User;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface FileRepository extends MongoRepository<Files, ObjectId> {
    Files getFilesById(ObjectId id);

    //($O(1)$ lookup speed due to heiarchial design
    List<Files> findByUserIdAndParentFolderId(ObjectId userId, ObjectId parentFolderId);
}
