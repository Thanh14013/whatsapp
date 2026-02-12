package com.whatsapp.user;

import org.springframework.boot.SpringApplication;

public class TestUserApplication {

    public static void main(String[] args) {
        SpringApplication.from(UserApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
