package com.japaneselearning.user.specification;

import com.japaneselearning.user.entity.User;
import lombok.NonNull;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

public final class UserSpecification {

    private UserSpecification() {
    }

    public static Specification<@NonNull User> hasActivated(boolean isActivated) {
        return (root, query, cb) ->
                cb.equal(root.get("isActivated"), isActivated);
    }

    public static Specification<@NonNull User> usernameContains(String usernamePattern) {
        return (root, query, cb) -> {
            if (usernamePattern == null || usernamePattern.isBlank()) {
                return cb.conjunction();
            }

            String pattern = "%" + usernamePattern.trim().toLowerCase(Locale.ROOT) + "%";

            return cb.like(
                    cb.lower(root.get("username")),
                    pattern
            );
        };
    }

    public static Specification<@NonNull User> emailContains(String emailPattern) {
        return (root, query, cb) -> {
            if (emailPattern == null || emailPattern.isBlank()) {
                return cb.conjunction();
            }

            String pattern = "%" + emailPattern.trim().toLowerCase(Locale.ROOT) + "%";

            return cb.like(
                    cb.lower(root.get("email")),
                    pattern
            );
        };
    }
}