package com.hospitalmanagement.user.factory.dto.request;

import com.hospitalmanagement.user.dto.request.RefreshTokenRequest;

public class RefreshTokenRequestFactory {

    public static RefreshTokenRequest createValid() {
        return new RefreshTokenRequest("refresh_token_request");
    }
}
