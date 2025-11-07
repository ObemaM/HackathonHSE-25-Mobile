package com.example.hakaton;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ColumnInfo;
import androidx.room.ForeignKey;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@Entity(
        tableName = "logs",
        primaryKeys = {"device_code", "datetime"},
        foreignKeys = @ForeignKey(
                entity = Action.class,
                parentColumns = {"action_code", "app_version"},
                childColumns = {"action_code", "app_version"},
                onDelete = ForeignKey.CASCADE
        )
)
public class Log {
    @ColumnInfo(name = "team_number")
    private String teamNumber;

    @NonNull
    @ColumnInfo(name = "action_code")
    private String actionCode = "";

    @NonNull
    @ColumnInfo(name = "app_version")
    private String appVersion = "";

    @ColumnInfo(name = "is_sent")
    private boolean isSent;

    @NonNull
    @ColumnInfo(name = "device_code")
    private String deviceCode = "";

    @NonNull
    @ColumnInfo(name = "datetime")
    private String datetime = "";

    public Log() {}

    public Log(@NonNull String teamNumber, @NonNull String actionCode, @NonNull String appVersion, @NonNull String deviceCode) {
        this.teamNumber = teamNumber;
        this.actionCode = actionCode;
        this.appVersion = appVersion;
        this.deviceCode = deviceCode;
        this.isSent = false;
        this.datetime = getCurrentDateTimeString();
    }

    // Метод для получения текущей даты-времени
    private String getCurrentDateTimeString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS", Locale.getDefault());
        return sdf.format(new Date());
    }

    // Геттеры и сеттеры
    public String getTeamNumber() { return teamNumber; }
    public void setTeamNumber(String teamNumber) { this.teamNumber = teamNumber; }
    @NonNull
    public String getActionCode() { return actionCode; }
    public void setActionCode(@NonNull String actionCode) { this.actionCode = actionCode; }

    @NonNull
    public String getAppVersion() { return appVersion; }
    public void setAppVersion(@NonNull String appVersion) { this.appVersion = appVersion; }

    public boolean isSent() { return isSent; }
    public void setSent(boolean sent) { isSent = sent; }

    @NonNull
    public String getDeviceCode() { return deviceCode; }
    public void setDeviceCode(@NonNull String deviceCode) { this.deviceCode = deviceCode; }

    @NonNull
    public String getDatetime() { return datetime; }
    public void setDatetime(@NonNull String datetime) { this.datetime = datetime; }
}