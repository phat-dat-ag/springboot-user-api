package com.japaneselearning.user.common.controller;

import com.japaneselearning.user.common.response.APIResponse;
import lombok.NonNull;
import org.springframework.http.ResponseEntity;

public abstract class BaseController {

    protected <T> ResponseEntity<@NonNull APIResponse<T>> success(T data) {
        return ResponseEntity.ok(
                APIResponse.success(null, null, data)
        );
    }

    protected ResponseEntity<@NonNull APIResponse<Void>> successWithoutContent() {
        return ResponseEntity.ok(
                APIResponse.success(null, null, null)
        );
    }
}