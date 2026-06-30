package com.dhub;

public class ProfileModel {
    public String packageName;
    public String cookie;
    public String link;
    public String status; // idle | starting | running | error

    public ProfileModel(String packageName) {
        this.packageName = packageName;
        this.cookie = "";
        this.link = "";
        this.status = "idle";
    }
}
