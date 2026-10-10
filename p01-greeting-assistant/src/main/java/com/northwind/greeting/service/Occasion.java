package com.northwind.greeting.service;

/** Why we are greeting the customer. Each occasion carries its own instruction for the model. */
public enum Occasion {

    WELCOME("Welcome them as a new member and invite them to explore the catalog."),
    ORDER_SHIPPED("Tell them their latest order is on its way and wish them a great trip."),
    BIRTHDAY("Wish them a happy birthday."),
    WIN_BACK("Tell them we miss them and would love to help with their next adventure.");

    private final String instruction;

    Occasion(String instruction) {
        this.instruction = instruction;
    }

    public String instruction() {
        return instruction;
    }
}
