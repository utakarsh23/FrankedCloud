package com.shresth.FrankenCloud.Repositories;

import com.shresth.FrankenCloud.Entity.DriveAccount;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DriveRepository extends MongoRepository<DriveAccount, ObjectId> {
    List<DriveAccount> findDriveAccountsByUserId(ObjectId id);
    List<DriveAccount> findDriveAccountsByUserIdAndIsActive(ObjectId id, Boolean isActive);
    DriveAccount getDriveAccountByUserId(ObjectId userid);
    DriveAccount getDriveAccountByAccountId(String accountId);
    List<DriveAccount> findDriveAccountsByUserIdAndIsActiveAndRemainingSpaceGreaterThanEqual(ObjectId id, Boolean isActive, Long minRemainingSpace);

    boolean existsDriveAccountByAccountId(String accountId);
}
