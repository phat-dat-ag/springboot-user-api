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

    public static User createNotActivated() {
        User user = new User(
                "datngu133",
                "datngu133@gmail.com",
                "11111111"
        );

        user.setActivated(false);

        return user;
    }
}
