package se.yarin.morphy.cli.columns;

import se.yarin.cbhlib.Database;
import se.yarin.cbhlib.Game;
import se.yarin.cbhlib.games.GameHeader;
import se.yarin.chess.GameModel;

import java.util.Arrays;
import java.util.stream.Collectors;

public interface GameColumn {

    String getHeader();

    default int width() {
        return getHeader().length();
    }

    default int marginLeft() {
        return 1;
    }

    default int marginRight() {
        return 1;
    }

    String getValue(Game game);

    String getId();

    // This also determines in what order the columns will be shown
    GameColumn[] ALL = {
            new GameIdColumn(),
            new NameColumn(true),
            new TeamColumn(true),
            new RatingColumn(true),
            new RatingTypeColumn(true),
            new NameColumn(false),
            new TeamColumn(false),
            new RatingColumn(false),
            new RatingTypeColumn(true),
            new ResultsColumn(),
            new NumMovesColumn(),
            new VCSColumn(),
            new EcoColumn(),
            new TournamentTitleColumn(),
            new TournamentPlaceColumn(),
            new TournamentNationColumn(),
            new TournamentCategoryColumn(),
            new TournamentRoundsColumn(),
            new TournamentTimeControlColumn(),
            new TournamentTypeColumn(),
            new TournamentDateColumn(),
            new RoundColumn(),
            new DateColumn(),
            new AnnotatorColumn(),
            new SourceColumn(),
            new GameVersionColumn(),
            new CreationTimestampColumn(),
            new LastChangedTimestampColumn(),
            new DatabaseColumn(),
    };

    static String allColumnsString() {
        return Arrays.stream(ALL).map(GameColumn::getId).collect(Collectors.joining(", "));
    }
}
