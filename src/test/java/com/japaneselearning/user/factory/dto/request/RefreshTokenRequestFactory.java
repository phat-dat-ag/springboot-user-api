package com.japaneselearning.user.factory.dto.request;

import com.japaneselearning.user.dto.request.RefreshTokenRequest;

public class RefreshTokenRequestFactory {

    public static RefreshTokenRequest createValid() {
        return new RefreshTokenRequest("refresh_token_request");
    }
}
