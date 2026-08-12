package com.shresth.FrankenCloud.Services;


import com.shresth.FrankenCloud.Entity.DriveAccounts;
import com.shresth.FrankenCloud.Repositories.DriveAccountsRepository;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DriveService {

    @Autowired
    private DriveAccountsRepository driveAccountsRepository;

    public List<DriveAccounts> getDriveAccounts(ObjectId userId) {
        return driveAccountsRepository.findDriveAccountsByUserId(userId);
    }

    public List<DriveAccounts> getDriveAccounts(ObjectId userId, Boolean activeAccounts) {
        return driveAccountsRepository.findDriveAccountsByUserIdAndIsActive(userId, activeAccounts);
    }
}
