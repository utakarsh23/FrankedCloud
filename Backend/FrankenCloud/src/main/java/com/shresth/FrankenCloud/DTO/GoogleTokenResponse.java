package com.shresth.FrankenCloud.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GoogleTokenResponse {
    private String accessToken;
    private Long expiresIn;
    private String scope;
    private String tokenType;
}