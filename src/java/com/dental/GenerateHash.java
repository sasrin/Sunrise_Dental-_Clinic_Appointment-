package com.dental;

import com.dental.util.PasswordUtil;

public class GenerateHash {
    public static void main(String[] args) {
        String password = "sasrin123";
        String hash = PasswordUtil.hashPassword(password);
        System.out.println("Password: " + password);
        System.out.println("Hash: " + hash);
    }
}
