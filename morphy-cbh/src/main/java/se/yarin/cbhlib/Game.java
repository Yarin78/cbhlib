package se.yarin.cbhlib;

import lombok.Getter;
import lombok.NonNull;
import se.yarin.cbhlib.entities.*;
import se.yarin.cbhlib.games.ExtendedGameHeader;
import se.yarin.cbhlib.games.GameHeader;
import se.yarin.cbhlib.games.RatingType;
import se.yarin.chess.Date;
import se.yarin.chess.Eco;
import se.yarin.chess.GameResult;
import se.yarin.chess.NAG;

import java.util.Calendar;

/**
 * Class that represents a game stored in a {@link Database}.
 */
public class Game {
    @Getter
    private final Database database;

    @Getter
    private final GameHeader header;

    @Getter
    private final ExtendedGameHeader extendedHeader;

    public int getId() {
        return header.getId();
    }

    public @NonNull PlayerEntity getWhite() {
        return database.getPlayerBase().get(header.getWhitePlayerId());
    }

    public @NonNull PlayerEntity getBlack() {
        return database.getPlayerBase().get(header.getBlackPlayerId());
    }

    public @NonNull TournamentEntity getTournament() {
        return database.getTournamentBase().get(header.getTournamentId());
    }

    public @NonNull AnnotatorEntity getAnnotator() {
        return database.getAnnotatorBase().get(header.getAnnotatorId());
    }

    public @NonNull SourceEntity getSource() {
        return database.getSourceBase().get(header.getSourceId());
    }

    public TeamEntity getWhiteTeam() {
        int teamId = extendedHeader.getWhiteTeamId();
        return teamId == -1 ? null : database.getTeamBase().get(teamId);
    }

    public TeamEntity getBlackTeam() {
        int teamId = extendedHeader.getBlackTeamId();
        return teamId == -1 ? null : database.getTeamBase().get(teamId);
    }

    public Game(@NonNull Database database, int gameId) {
        this(database, database.getHeaderBase().getGameHeader(gameId));
    }

    public Game(@NonNull Database database, @NonNull GameHeader header) {
        this(database, header, database.getExtendedHeaderBase().getExtendedGameHeader(header.getId()));
    }

    public Game(@NonNull Database database, @NonNull GameHeader header, @NonNull ExtendedGameHeader extendedHeader) {
        this.database = database;
        this.header = header;
        this.extendedHeader = extendedHeader;
    }

    public Date getPlayedDate() {
        return header.getPlayedDate();
    }

    public GameResult getResult() {
        return header.getResult();
    }

    public boolean isGuidingText() {
        return header.isGuidingText();
    }

    public Calendar getCreationTime() {
        return extendedHeader.getCreationTime();
    }

    public long getCreationTimestamp() {
        return extendedHeader.getCreationTimestamp();
    }

    public Eco getEco() {
        return header.getEco();
    }

    public int getGameVersion() {
        return extendedHeader.getGameVersion();
    }

    public long getLastChangedTimestamp() {
        return extendedHeader.getLastChangedTimestamp();
    }

    public Calendar getLastChangedTime() {
        return extendedHeader.getLastChangedTime();
    }

    public int getNoMoves() {
        return header.getNoMoves();
    }

    public int getWhiteElo() {
        return header.getWhiteElo();
    }

    public int getBlackElo() {
        return header.getBlackElo();
    }

    public RatingType getWhiteRatingType() {
        return extendedHeader.getWhiteRatingType();
    }

    public RatingType getBlackRatingType() {
        return extendedHeader.getBlackRatingType();
    }

    public NAG getLineEvaluation() {
        return header.getLineEvaluation();
    }

    public int getRound() {
        return header.getRound();
    }

    public int getSubRound() {
        return header.getSubRound();
    }

    public int getVariationsMagnitude() {
        return header.getVariationsMagnitude();
    }

    public int getCommentariesMagnitude() {
        return header.getCommentariesMagnitude();
    }

    public int getSymbolsMagnitude() {
        return header.getSymbolsMagnitude();
    }
}
