package com.example.hakaton;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.Date;

@Entity(tableName = "events")
public class Event {
    @PrimaryKey(autoGenerate = true)
    private long id;

    private String regionCode;
    private String smcCode;
    private String brigadeNumber;
    private String deviceId;
    private String appVersion;
    private String eventText;
    private long eventTime;

    public Event() {}

    public Event(String regionCode, String smcCode, String brigadeNumber,
                 String deviceId, String appVersion, String eventText) {
        this.regionCode = regionCode;
        this.smcCode = smcCode;
        this.brigadeNumber = brigadeNumber;
        this.deviceId = deviceId;
        this.appVersion = appVersion;
        this.eventText = eventText;
        this.eventTime = System.currentTimeMillis();
    }


    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getRegionCode() { return regionCode; }
    public void setRegionCode(String regionCode) { this.regionCode = regionCode; }

    public String getSmcCode() { return smcCode; }
    public void setSmcCode(String smcCode) { this.smcCode = smcCode; }

    public String getBrigadeNumber() { return brigadeNumber; }
    public void setBrigadeNumber(String brigadeNumber) { this.brigadeNumber = brigadeNumber; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }

    public String getEventText() { return eventText; }
    public void setEventText(String eventText) { this.eventText = eventText; }

    public long getEventTime() { return eventTime; }
    public void setEventTime(long eventTime) { this.eventTime = eventTime; }
}