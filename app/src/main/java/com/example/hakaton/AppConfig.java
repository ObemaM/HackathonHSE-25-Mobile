package com.example.hakaton;

import java.util.Arrays;
import java.util.List;

public class AppConfig {
    // Версия приложения
    public static final String APP_VERSION = "1";

    // Действия
    public static final List<ActionItem> ACTIONS = Arrays.asList(
            new ActionItem("1", "Кнопка A нажата"),
            new ActionItem("2", "Кнопка B нажата"),
            new ActionItem("3", "Осмотр пациента"),
            new ActionItem("4", "Введение лекарства"),
            new ActionItem("5", "Измерение давления"),
            new ActionItem("6", "Забор крови"),
            new ActionItem("7", "Перевязка раны"),
            new ActionItem("8", "Консультация"),
            new ActionItem("9", "Выписка пациента"),
            new ActionItem("10", "Экстренная помощь")
    );
}