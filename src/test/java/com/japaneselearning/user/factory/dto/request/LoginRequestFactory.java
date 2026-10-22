package com.japaneselearning.user.factory.dto.request;

import com.japaneselearning.user.dto.request.LoginRequest;

public class LoginRequestFactory {

    public static LoginRequest createValidWithEmail() {
        return new LoginRequest(
                "datngu133@gmail.com",
                "11111111"
        );
    }

    public static LoginRequest createValidWithUsername() {
        return new LoginRequest(
                "datngu133",
                "11111111"
        );
    }
}
