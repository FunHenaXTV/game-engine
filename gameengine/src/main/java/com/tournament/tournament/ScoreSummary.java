package com.tournament.tournament;

public sealed interface ScoreSummary permits PointsScoreSummary, TableScoreSummary {
}
