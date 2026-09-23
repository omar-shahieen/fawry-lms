package com.fawry.lms.user.utils;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ProfilePictureUrlGenerator {

    private static final String DICEBEAR_URL = "https://api.dicebear.com/10.x/lorelei/svg?seed=";

    public String generate() {
        return DICEBEAR_URL + UUID.randomUUID();
    }
}
