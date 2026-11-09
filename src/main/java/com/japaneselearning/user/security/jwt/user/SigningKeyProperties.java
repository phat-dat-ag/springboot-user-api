package com.japaneselearning.user.security.jwt.user;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app.security.signing")
@Getter
@Setter
public class SigningKeyProperties {

    private List<Key> keys = new ArrayList<>();

    @Getter
    @Setter
    public static class Key {

        private String kid;
        private String privateKeyPath;
        private boolean active;
    }
}
