package com.tournament.discipline;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import com.tournament.discipline.api.ActionType;
import com.tournament.discipline.api.DisciplinaryActionType;
import com.tournament.discipline.api.ScoreActionType;
import com.tournament.discipline.api.StatisticalActionType;
import com.tournament.discipline.impl.FootballDisciplinaryType;
import com.tournament.discipline.impl.FootballScoreType;
import com.tournament.discipline.impl.FootballStatisticalType;
import com.tournament.discipline.impl.RugbyDisciplinaryType;
import com.tournament.discipline.impl.RugbyScoreType;
import com.tournament.discipline.impl.RugbyStatisticalType;

class ActionTypeTest {

    private static Stream<Arguments> allActionTypes() {
        return Stream.of(
                Arguments.of(FootballScoreType.GOAL, ScoreActionType.class),
                Arguments.of(RugbyScoreType.TRY, ScoreActionType.class),
                Arguments.of(RugbyScoreType.CONVERSION, ScoreActionType.class),
                Arguments.of(RugbyScoreType.PENALTY_KICK, ScoreActionType.class),
                Arguments.of(RugbyScoreType.DROP_GOAL, ScoreActionType.class),
                Arguments.of(FootballDisciplinaryType.YELLOW_CARD, DisciplinaryActionType.class),
                Arguments.of(FootballDisciplinaryType.RED_CARD, DisciplinaryActionType.class),
                Arguments.of(RugbyDisciplinaryType.YELLOW_CARD, DisciplinaryActionType.class),
                Arguments.of(RugbyDisciplinaryType.RED_CARD, DisciplinaryActionType.class),
                Arguments.of(FootballStatisticalType.CORNER_KICK, StatisticalActionType.class),
                Arguments.of(FootballStatisticalType.OFFSIDE, StatisticalActionType.class),
                Arguments.of(FootballStatisticalType.SHOT_ON_TARGET, StatisticalActionType.class),
                Arguments.of(FootballStatisticalType.FOUL, StatisticalActionType.class),
                Arguments.of(RugbyStatisticalType.SCRUM_WON, StatisticalActionType.class),
                Arguments.of(RugbyStatisticalType.LINEOUT_WON, StatisticalActionType.class),
                Arguments.of(RugbyStatisticalType.KNOCK_ON, StatisticalActionType.class)
        );
    }

    @ParameterizedTest
    @MethodSource("allActionTypes")
    void getNameMatchesEnumName(Enum<?> action, Class<? extends ActionType> marker) {
        ActionType actionType = (ActionType) action;

        assertThat(actionType.getName()).isEqualTo(action.name());
        assertThat(actionType).isInstanceOf(marker);
        assertThat(actionType).isInstanceOf(ActionType.class);
    }

    @ParameterizedTest
    @MethodSource("allActionTypes")
    void enumImplementsMarkerInterfaceOnly(Enum<?> action, Class<? extends ActionType> marker) {
        // sanity: the marker is one of the three sub-interfaces, not just ActionType
        assertThat(marker).isIn(
                ScoreActionType.class, DisciplinaryActionType.class, StatisticalActionType.class);
        assertThat(marker.isInstance(action)).isTrue();
    }
}
