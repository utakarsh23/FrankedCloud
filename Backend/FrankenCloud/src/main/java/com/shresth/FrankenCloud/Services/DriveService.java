package com.shresth.FrankenCloud.Services;


import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.About;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.UserCredentials;
import com.shresth.FrankenCloud.Entity.DriveAccount;
import com.shresth.FrankenCloud.DTO.Storage;
import com.shresth.FrankenCloud.Entity.User;
import com.shresth.FrankenCloud.Repositories.DriveRepository;
import com.shresth.FrankenCloud.Repositories.UserRepository;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class DriveService {



    @Value("${google.drive.client-id}")
    private String clientId;

    @Value("${google.drive.client-secret}")
    private String clientSecret;

    @Value("${google.drive.redirect-uri}")
    private String redirectUri;

    private Long minRemainingSpace = 1024 * 1024 * 1024L;


    @Autowired
    private EncryptionService encryptionService;

    @Autowired
    private DriveRepository driveRepository;
    @Autowired
    private UserRepository userRepository;

    public List<DriveAccount> getDriveAccounts(ObjectId userId) {
        return driveRepository.findDriveAccountsByUserId(userId);
    }

    public List<DriveAccount> getDriveAccounts(ObjectId userId, Boolean activeAccounts) {
        return driveRepository.findDriveAccountsByUserIdAndIsActive(userId, activeAccounts);
    }

    public List<DriveAccount> getDriveAccounts(ObjectId userId, Boolean activeStatus, Boolean isUsable) {
        // rem space by def should be greater than 1GB
        // this fetched the drives that are for the user and fetches only to be used drives.

        return driveRepository.findDriveAccountsByUserIdAndIsActiveAndRemainingSpaceGreaterThanEqual(userId, activeStatus, minRemainingSpace);
    }

    public DriveAccount getDriveAccount(String driveAccountId) {
        DriveAccount driveAccount = driveRepository.getDriveAccountByAccountId(driveAccountId);

        if (driveAccount == null) {
            throw new NoSuchElementException("Drive account not found.");
        }
        return driveAccount;
    }

    public DriveAccount linkDrive(ObjectId userId, String authCode) throws IOException {
        GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                "https://oauth2.googleapis.com/token",
                clientId,
                clientSecret,
                authCode,
                redirectUri
        ).execute();

        String accessTokenStr = tokenResponse.getAccessToken();
        String refreshTokenStr = tokenResponse.getRefreshToken();

        if (refreshTokenStr == null) {
            throw new IllegalArgumentException("Failed to obtain refresh token. Re-consent is required.");
        }

        UserCredentials credentials = UserCredentials.newBuilder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setRefreshToken(refreshTokenStr)
                .setAccessToken(new AccessToken(accessTokenStr, null))
                .build();

        Drive driveClient = new Drive.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials)
        ).setApplicationName("FrankenCloud").build();

        About about = driveClient.about().get().setFields("user(permissionId, emailAddress), storageQuota").execute();

        String googleAccountId = about.getUser().getPermissionId();
        String googleEmail = about.getUser().getEmailAddress();
        Long totalLimit = about.getStorageQuota().getLimit();
        Long usedSpace = about.getStorageQuota().getUsage();

        long limitBytes = (totalLimit != null) ? totalLimit : 16106127360L;
        long remainingSpace = Math.max(0L, limitBytes - usedSpace);

        DriveAccount driveAccount = driveRepository.getDriveAccountByAccountId(googleAccountId);
        if(driveAccount != null && driveAccount.getIsActive()) {
            throw new FileAlreadyExistsException("Google Drive account is already linked and active.");
        }

        driveAccount = driveAccount == null ? new DriveAccount() : driveAccount;
        driveAccount.setGoogleEmail(googleEmail);
        driveAccount.setUserId(userId);
        driveAccount.setRefreshToken(encryptionService.encryptInput(refreshTokenStr));
        driveAccount.setIsActive(true);
        driveAccount.setUsedSpace(usedSpace);
        driveAccount.setRemainingSpace(remainingSpace);
        driveAccount.setAccountId(googleAccountId);


        User user = userRepository.findUserById(userId);
        if(user == null) {
            throw new NoSuchElementException("User not found.");
        }


        Storage storage = user.getStorage() == null ? new Storage() : user.getStorage();

        long currentStorageSize = storage.getStorageSize();
        long currentRemaining = storage.getRemainingStorage();
        long currentUsed = storage.getUsedStorage();

        storage.setStorageSize(currentStorageSize + limitBytes);
        storage.setRemainingStorage(currentRemaining + remainingSpace);
        storage.setActiveAccounts(storage.getActiveAccounts() + (remainingSpace >= minRemainingSpace ? 1 : 0));
        storage.setTotalAccounts(storage.getTotalAccounts() + 1);
        storage.setUsedStorage(currentUsed + usedSpace);

        user.setStorage(storage);
        userRepository.save(user);

        return driveRepository.save(driveAccount);
    }

    public Drive getDriveClient(DriveAccount driveAccount) {
        String rawRefreshToken = encryptionService.decryptInput(driveAccount.getRefreshToken());

        UserCredentials credentials = UserCredentials.newBuilder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setRefreshToken(rawRefreshToken)
                .build();

        return new Drive.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials)
        ).setApplicationName("FrankenCloud").build();
    }

    public Storage getAccountSpecs(DriveAccount driveAccount) throws IOException {
        Drive frankenCloud = getDriveClient(driveAccount);

        About about = frankenCloud.about().get().setFields("user(permissionId, emailAddress), storageQuota").execute();
        Storage storage = new Storage();
        Long totalLimit = about.getStorageQuota().getLimit();
        Long usedSpace = about.getStorageQuota().getUsage();

        long limitBytes = (totalLimit != null) ? totalLimit : 16106127360L;
        long remainingSpace = Math.max(0L, limitBytes - usedSpace);
        storage.setStorageSize(limitBytes);
        storage.setRemainingStorage(remainingSpace);
        storage.setUsedStorage(usedSpace);
        return storage;
    }

    public DriveAccount updateDriveAccount(DriveAccount driveAccount) throws IOException {
            Storage newStorage = getAccountSpecs(driveAccount);

            long oldRemaining = driveAccount.getRemainingSpace();
            long oldUsed = driveAccount.getUsedSpace();
            long oldTotalCapacity = oldRemaining + oldUsed;

            long newRemaining = newStorage.getRemainingStorage();
            long newUsed = newStorage.getUsedStorage();
            long newTotalCapacity = newStorage.getStorageSize();

            long deltaRemaining = newRemaining - oldRemaining;
            long deltaTotalCapacity = newTotalCapacity - oldTotalCapacity;
            long deltaUsed = newUsed - oldUsed;

            // Skip saving if nothing changed on Google Drive
            if (deltaRemaining == 0 && deltaTotalCapacity == 0 && deltaUsed == 0) {
                return driveAccount;
            }

            driveAccount.setRemainingSpace(newRemaining);
            driveAccount.setUsedSpace(newUsed);
            driveAccount.setIsActive(true);

            User user = userRepository.findUserById(driveAccount.getUserId());
            if (user != null && user.getStorage() != null) {
                Storage userStorage = user.getStorage();
                userStorage.setRemainingStorage(userStorage.getRemainingStorage() + deltaRemaining);
                userStorage.setStorageSize(userStorage.getStorageSize() + deltaTotalCapacity);
                userStorage.setUsedStorage(userStorage.getUsedStorage() + deltaUsed);
                userRepository.save(user);
            }

            return driveRepository.save(driveAccount);
    }

    public DriveAccount toggleDriveAccount(String account_id) {
        DriveAccount driveAccount = driveRepository.getDriveAccountByAccountId(account_id);
        if (driveAccount == null) {
            throw new NoSuchElementException("Drive account not found.");
        }
        driveAccount.setIsActive(!driveAccount.getIsActive());
        return driveRepository.save(driveAccount);
    }
}
