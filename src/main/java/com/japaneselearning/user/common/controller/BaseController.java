package com.japaneselearning.user.common.controller;

import com.japaneselearning.user.common.response.APIResponse;
import org.springframework.http.ResponseEntity;

public abstract class BaseController {

    protected <T> ResponseEntity<APIResponse<T>> success(T data) {
        return ResponseEntity.ok(
                APIResponse.success(null, null, data)
        );
    }
}