package com.ecampus;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenerateHash {

    public static void main(String[] args) {
        if (args.length != 1 || args[0].isBlank()) {
            System.err.println("Usage: GenerateHash <password>");
            return;
        }

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println(encoder.encode(args[0]));
    }
}
