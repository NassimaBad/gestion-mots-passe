package com.passwordmanager.common;

import java.io.Serializable;

public class PasswordEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    private String website;
    private String username;
    private String password;

    // Default constructor for JSON serialization/deserialization
    public PasswordEntry() {
    }

    public PasswordEntry(String website, String username, String password) {
        this.website = website;
        this.username = username;
        this.password = password;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "PasswordEntry{" +
               "website='" + website + '\'' +
               ", username='" + username + '\'' +
               "}";
    }
} 