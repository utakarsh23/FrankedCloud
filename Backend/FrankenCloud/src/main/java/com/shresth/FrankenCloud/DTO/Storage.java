package com.shresth.FrankenCloud.DTO;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Storage {

    private long storageSize; // total storage size
    private long remainingStorage; // remaining space in storage
    private long usedStorage; // used storage
    private long totalAccounts;
    private long activeAccounts;

}
