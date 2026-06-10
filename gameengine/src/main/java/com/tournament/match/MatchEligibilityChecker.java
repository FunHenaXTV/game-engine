package com.tournament.match;

import com.tournament.competitor.api.Role;
import com.tournament.discipline.Discipline;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class MatchEligibilityChecker {

    public boolean isRosterEligible(MatchRoster roster, Discipline discipline) {
        if (roster == null || discipline == null) {
            return false;
        }
        int size = roster.size();
        if (size < discipline.getMinPlayersRequired() || size > discipline.getMaxRosterSize()) {
            return false;
        }
        return hasAllRequiredRoles(roster.getRoles(), discipline.getRequiredRoles());
    }

    public void verifyRosterEligible(MatchRoster roster, Discipline discipline) {
        if (roster == null) {
            throw new IllegalArgumentException("roster must not be null");
        }
        if (discipline == null) {
            throw new IllegalArgumentException("discipline must not be null");
        }
        int size = roster.size();
        if (size < discipline.getMinPlayersRequired() || size > discipline.getMaxRosterSize()) {
            throw new IllegalArgumentException(
                    "roster size " + size + " is outside ["
                            + discipline.getMinPlayersRequired() + ", "
                            + discipline.getMaxRosterSize() + "] for discipline "
                            + discipline.getName());
        }
        List<String> assignedRoles = roster.getRoles();
        for (Role required : discipline.getRequiredRoles()) {
            if (!assignedRoles.contains(required.name())) {
                throw new IllegalArgumentException(
                        "roster is missing required role " + required.name()
                                + " for discipline " + discipline.getName());
            }
        }
    }

    private static boolean hasAllRequiredRoles(List<String> assignedRoles, List<Role> required) {
        Set<String> assigned = new HashSet<>(assignedRoles);
        for (Role role : required) {
            if (!assigned.contains(role.name())) {
                return false;
            }
        }
        return true;
    }
}
