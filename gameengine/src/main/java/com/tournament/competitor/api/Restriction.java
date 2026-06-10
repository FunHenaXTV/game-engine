package com.tournament.competitor.api;

public interface Restriction {
    boolean isActive();

    String getReason();
}
