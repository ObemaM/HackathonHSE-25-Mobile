package com.example.hakaton;

import java.util.Arrays;
import java.util.List;

public class AppConfig {
    // Версия приложения
    public static final String APP_VERSION = "1.1";

    // Действия
    public static final List<ActionItem> ACTIONS = Arrays.asList(
            new ActionItem("1", "A"),
            new ActionItem("2", "B"),
            new ActionItem("3", "C")
    );
}