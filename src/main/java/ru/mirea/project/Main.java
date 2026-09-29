package ru.mirea.project;

import ru.mirea.project.ui.ConsoleApp;

public class Main {
    public static void main(String[] args) {
        System.setProperty("log4j2.statusLoggerLevel", "OFF");
        new ConsoleApp().run();
    }
}
