package com.example.hakaton;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ColumnInfo;

@Entity(tableName = "actions", primaryKeys = {"action_code", "app_version"})
public class Action {
    @ColumnInfo(name = "action_text")
    private String actionText;

    @NonNull
    @ColumnInfo(name = "action_code")
    private String actionCode = "";

    @NonNull
    @ColumnInfo(name = "app_version")
    private String appVersion = "";

    public Action() {}

    public Action(String actionText, @NonNull String actionCode, @NonNull String appVersion) {
        this.actionText = actionText;
        this.actionCode = actionCode;
        this.appVersion = appVersion;
    }

    // Геттеры и сеттеры
    public String getActionText() { return actionText; }
    public void setActionText(String actionText) { this.actionText = actionText; }

    @NonNull
    public String getActionCode() { return actionCode; }
    public void setActionCode(@NonNull String actionCode) { this.actionCode = actionCode; }

    @NonNull
    public String getAppVersion() { return appVersion; }
    public void setAppVersion(@NonNull String appVersion) { this.appVersion = appVersion; }
}