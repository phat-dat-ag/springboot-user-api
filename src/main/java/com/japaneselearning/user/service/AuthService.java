package com.japaneselearning.user.service;

import com.japaneselearning.user.dto.request.RegisterAccountRequest;
import com.japaneselearning.user.dto.response.RegisterAccountResponse;

public interface AuthService {
    RegisterAccountResponse registerAccount(RegisterAccountRequest registerAccountRequest);
}
