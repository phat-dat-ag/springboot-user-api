package com.japaneselearning.user.factory.dto.entity;

import com.japaneselearning.user.entity.User;

public class UserFactory {

    public static User createValid() {
        return new User(
                "datngu133",
                "datngu133@gmail.com",
                "11111111"
        );
    }
}
