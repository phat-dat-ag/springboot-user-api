package com.hospitalmanagement.user.factory.dto.request;

import com.hospitalmanagement.user.dto.request.RegisterAccountRequest;

public class RegisterAccountRequestFactory {

    public static RegisterAccountRequest createValid() {
        return new RegisterAccountRequest(
                "datngu133",
                "datngu133@gmail.com",
                "11111111"
        );
    }
}
