package com.example.hakaton;

public class ActionItem {
    private final String actionCode;
    private final String actionText;

    public ActionItem(String actionCode, String actionText) {
        this.actionCode = actionCode;
        this.actionText = actionText;
    }

    public String getActionCode() { return actionCode; }
    public String getActionText() { return actionText; }
}