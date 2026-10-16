package com.japaneselearning.user.common.response;

import java.util.List;

public record APIError(
        List<ErrorDetail> details
) {

    public record ErrorDetail(
            String field,
            String code,
            String message
    ) {
    }
}