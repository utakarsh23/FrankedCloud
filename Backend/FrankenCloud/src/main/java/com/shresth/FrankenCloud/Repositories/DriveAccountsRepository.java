package com.shresth.FrankenCloud.Repositories;

import com.shresth.FrankenCloud.Entity.DriveAccounts;
import com.shresth.FrankenCloud.Entity.Files;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DriveAccountsRepository extends MongoRepository<DriveAccounts, ObjectId> {
    List<DriveAccounts> findDriveAccountsByUserId(ObjectId id);
    List<DriveAccounts> findDriveAccountsByUserIdAndIsActive(ObjectId id, Boolean isActive);
    List<DriveAccounts> findDriveAccountsByUserIdAndIsActiveAndRemainingSpaceGreaterThanEqual(ObjectId id, Boolean isActive, Long minRemainingSpace);

}
