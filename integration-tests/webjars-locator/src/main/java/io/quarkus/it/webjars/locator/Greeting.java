package io.quarkus.it.webjars.locator;

import java.time.LocalDate;

public class Greeting {

    private final String message;
    private final LocalDate date;

    public Greeting(String message, LocalDate date) {
        this.message = message;
        this.date = date;
    }

    public String getMessage() {
        return message;
    }

    public LocalDate getDate() {
        return date;
    }
}
