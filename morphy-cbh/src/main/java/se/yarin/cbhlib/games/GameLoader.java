package se.yarin.cbhlib.games;

import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.yarin.cbhlib.Database;
import se.yarin.cbhlib.Game;
import se.yarin.cbhlib.annotations.AnnotationStatistics;
import se.yarin.cbhlib.annotations.StatisticalAnnotation;
import se.yarin.cbhlib.entities.*;
import se.yarin.cbhlib.exceptions.ChessBaseException;
import se.yarin.cbhlib.exceptions.ChessBaseInvalidDataException;
import se.yarin.cbhlib.storage.EntityStorageException;
import se.yarin.chess.*;
import se.yarin.chess.annotations.Annotation;

import java.util.EnumSet;

/**
 * Contains methods for converting game data from the format as stored in {@link se.yarin.cbhlib.Database}
 * to the generic {@link se.yarin.chess.GameModel} and {@link TextModel}.
 */
public class GameLoader {
    private static final Logger log = LoggerFactory.getLogger(GameLoader.class);

    public static final String DATABASE_ID = "databaseId";
    public static final String WHITE_ID = "whiteId";
    public static final String BLACK_ID = "blackId";
    public static final String EVENT_ID = "eventId";
    public static final String ANNOTATOR_ID = "annotatorId";
    public static final String SOURCE_ID = "sourceId";
    public static final String WHITE_TEAM_ID = "whiteTeamId";
    public static final String BLACK_TEAM_ID = "blackTeamId";
    public static final String GAME_TAG_ID = "gameTagId";

    private final Database database;

    public GameLoader(Database database) {
        this.database = database;
    }

    /**
     * Creates a mutable model of the game header
     * @param game the game to get the header model for
     * @return a mutable game header model
     * @throws ChessBaseInvalidDataException if an entity couldn't be resolved
     */
    public GameHeaderModel getGameHeaderModel(@NonNull Game game) throws ChessBaseInvalidDataException {
        GameHeader header = game.getHeader();
        ExtendedGameHeader extendedHeader = game.getExtendedHeader();

        GameHeaderModel model = new GameHeaderModel();

        if (header.isGuidingText()) {
            throw new IllegalArgumentException("Can't get game header model for a guiding text (id " + header.getId() + ")");
        }

        PlayerEntity whitePlayer = resolveEntity(database.getPlayerBase(), "white player", header.getWhitePlayerId(), game, false);
        PlayerEntity blackPlayer = resolveEntity(database.getPlayerBase(), "black player", header.getBlackPlayerId(), game, false);
        AnnotatorEntity annotator = resolveEntity(database.getAnnotatorBase(), "annotator", header.getAnnotatorId(), game, false);
        SourceEntity source = resolveEntity(database.getSourceBase(), "source", header.getSourceId(), game, false);
        TournamentEntity tournament = resolveEntity(database.getTournamentBase(), "tournament", header.getTournamentId(), game, false);
        TeamEntity whiteTeam = resolveEntity(database.getTeamBase(), "white team", extendedHeader.getWhiteTeamId(), game, true);
        TeamEntity blackTeam = resolveEntity(database.getTeamBase(), "black team", extendedHeader.getBlackTeamId(), game, true);
        GameTagEntity gameTag = resolveEntity(database.getGameTagBase(), "game tag", extendedHeader.getGameTagId(), game, true);

        assert whitePlayer != null;
        assert blackPlayer != null;
        assert annotator != null;
        assert source != null;
        assert tournament != null;

        // TODO: Instead of duplicating each CBH specific field, it's probably better to keep a reference
        // to the raw header data. The GameHeaderModel should probably just contain the most generic fields,
        // while ID's and internal CBH stuff like "last updated", "version" etc should not be there,
        // but an opaque blob/reference to the CBH/CBJ data should be kept instead.
        // For data that can be deduced from the game moves (like eco, final material, endgame etc), perhaps
        // the version of the game can be used to deduce if it needs to be recalculated?

        model.setField(DATABASE_ID, database);

        model.setWhite(whitePlayer.getFullName());
        model.setField(WHITE_ID, whitePlayer.getId());
        if (header.getWhiteElo() > 0) {
            model.setWhiteElo(header.getWhiteElo());
        }
        if (whiteTeam != null) {
            model.setField(WHITE_TEAM_ID, whiteTeam.getId());
            model.setWhiteTeam(whiteTeam.getTitle());
        }
        model.setBlack(blackPlayer.getFullName());
        model.setField(BLACK_ID, blackPlayer.getId());
        if (header.getBlackElo() > 0) {
            model.setBlackElo(header.getBlackElo());
        }
        if (blackTeam != null) {
            model.setField(BLACK_TEAM_ID, blackTeam.getId());
            model.setBlackTeam(blackTeam.getTitle());
        }
        model.setResult(header.getResult());
        model.setDate(header.getPlayedDate());
        model.setEco(header.getEco());
        if (header.getRound() > 0) {
            model.setRound(header.getRound());
        }
        if (header.getSubRound() > 0) {
            model.setSubRound(header.getRound());
        }

        model.setEvent(tournament.getTitle());
        model.setField(EVENT_ID, tournament.getId());
        model.setEventDate(tournament.getDate());
        model.setEventSite(tournament.getPlace());
        if (tournament.getNation() != Nation.NONE) {
            model.setEventCountry(tournament.getNation().getIocCode());
        }
        if (tournament.getType() != TournamentType.NONE) {
            model.setEventType(tournament.getType().getName());
        }
        if (tournament.getTimeControl() != TournamentTimeControl.NORMAL) {
            model.setEventTimeControl(tournament.getTimeControl().getName());
        }
        if (tournament.getCategory() > 0) {
            model.setEventCategory(tournament.getCategory());
        }
        if (tournament.getRounds() > 0) {
            model.setEventRounds(tournament.getRounds());
        }

        model.setSourceTitle(source.getTitle());
        model.setSource(source.getPublisher());
        model.setSourceDate(source.getPublication());
        model.setField(SOURCE_ID, source.getId());
        model.setAnnotator(annotator.getName());
        model.setField(ANNOTATOR_ID, annotator.getId());
        if (gameTag != null) {
            model.setField(GAME_TAG_ID, gameTag.getId());
        }

        model.setLineEvaluation(header.getLineEvaluation());

        return model;
    }

    /**
     * Creates a mutable model of the game, header and moves
     * @param game the game to get the model for
     * @return a mutable game model
     * @throws ChessBaseInvalidDataException if the model couldn't be created due to broken references
     * If the move data is broken, a model is still returned with as many moves as could be decoded
     */
    public GameModel getGameModel(Game game) throws ChessBaseInvalidDataException {
        assert game.getDatabase() == this.database;
        GameHeaderModel headerModel = getGameHeaderModel(game);

        GameMovesModel moves = database.getMovesBase().getMoves(game.getMovesOffset(), game.getId());
        database.getAnnotationBase().getAnnotations(moves, game.getAnnotationOffset());

        return new GameModel(headerModel, moves);
    }

    /**
     * Creates a mutable model of the game, header and moves
     * @param gameId the id of the game to get the model for
     * @return a mutable game model
     * @throws ChessBaseInvalidDataException if the model couldn't be created due to broken references
     * If the move data is broken, a model is still returned with as many moves as could be decoded
     */
    public GameModel getGameModel(int gameId) throws ChessBaseInvalidDataException {
        return getGameModel(database.getGame(gameId));
    }

    public TextHeaderModel getTextHeaderModel(Game game) {
        GameHeader header = game.getHeader();
        if (!header.isGuidingText()) {
            throw new IllegalArgumentException("Can't get text header model for a game (id " + header.getId() + ")");
        }

        AnnotatorEntity annotator = database.getAnnotatorBase().get(header.getAnnotatorId());
        SourceEntity source = database.getSourceBase().get(header.getSourceId());
        TournamentEntity tournament = database.getTournamentBase().get(header.getTournamentId());

        TextHeaderModel model = new TextHeaderModel();

        model.setRound(header.getRound());
        model.setSubRound(header.getRound());
        model.setTournament(tournament == null ? "" : tournament.getTitle());
        model.setTournamentYear(tournament == null ? 0 : tournament.getDate().year());
        model.setSource(source == null ? "" : source.getPublisher());
        model.setAnnotator(annotator == null ? "" : annotator.getName());

        return model;
    }

    public TextModel getTextModel(Game game) throws ChessBaseException {
        assert game.getDatabase() == this.database;

        TextHeaderModel header = getTextHeaderModel(game);
        TextContentsModel contents = database.getMovesBase().getText(game.getMovesOffset(), game.getId());
        return new TextModel(header, contents);
    }

    public TextModel getTextModel(int gameId) throws ChessBaseException {
        return getTextModel(database.getGame(gameId));
    }

    public ExtendedGameHeader createExtendedGameHeader(Game game, int gameId, long movesOfs, long annotationOfs) throws ChessBaseInvalidDataException {
        boolean sameDb = database.getDatabaseId().equals(game.getDatabase().getDatabaseId());

        ExtendedGameHeader.ExtendedGameHeaderBuilder builder = game.getExtendedHeader().toBuilder();

        builder.id(gameId); // TODO: Is this the actual game id?
        builder.movesOffset(movesOfs);
        builder.annotationOffset(annotationOfs);

        if (!sameDb) {
            // If we're adding the game to a different database (usually the case),
            // the ids of all entities may be different so we can't just copy them
            // If no entity exists with the same name, create a new one.
            try {
                TeamEntity oldWhiteTeam = game.getWhiteTeam();
                TeamEntity oldBlackTeam = game.getBlackTeam();
                GameTagEntity oldGameTag = game.getGameTag();

                TeamEntity whiteTeam = oldWhiteTeam != null ? resolveOrCreateEntity(0,
                        oldWhiteTeam.withNewId(0), database.getTeamBase()) : null;
                TeamEntity blackTeam = oldBlackTeam != null ? resolveOrCreateEntity(0,
                        oldBlackTeam.withNewId(0), database.getTeamBase()) : null;
                GameTagEntity gameTag = oldGameTag != null ? resolveOrCreateEntity(0,
                        oldGameTag.withNewId(0), database.getGameTagBase()) : null;

                builder.whiteTeamId(whiteTeam == null ? -1 : whiteTeam.getId());
                builder.blackTeamId(blackTeam == null ? -1 : blackTeam.getId());
                builder.gameTagId(gameTag == null ? -1 : gameTag.getId());
            } catch (IllegalArgumentException e) {
                throw new ChessBaseInvalidDataException("Failed to create ExtendedGameHeader entry due to invalid entity data reference", e);
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("Some argument were not set in the extended game header model", e);
            }
        }

        builder.lastChangedTimestamp(0); // TODO

        return builder.build();
    }

    public ExtendedGameHeader createExtendedGameHeader(TextModel model, int gameId, long movesOfs) {
        return ExtendedGameHeader.builder()
                .id(gameId)
                .whiteTeamId(-1)
                .blackTeamId(-1)
                .whiteRatingType(RatingType.unspecified())
                .blackRatingType(RatingType.unspecified())
                .movesOffset(movesOfs)
                .annotationOffset(0)
                .creationTimestamp(0) // TODO
                .lastChangedTimestamp(0) // TODO
                .gameTagId(-1) // TODO
                .build();
    }

    public ExtendedGameHeader createExtendedGameHeader(GameModel model, int gameId, long movesOfs, long annotationOfs)
            throws ChessBaseInvalidDataException {
        GameHeaderModel header = model.header();

        TeamEntity whiteTeam, blackTeam;
        GameTagEntity gameTag;

        // If the GameModel contains ID's from a different database, we can't use them
        boolean sameDb = database.getDatabaseId().equals(header.getField(DATABASE_ID));

        // Lookup or create the entities that are referenced in the game header
        // If the id field is set, use that one. Otherwise use the string field.
        // If no entity exists with the same name, create a new one.
        try {
            whiteTeam = resolveOrCreateEntity(sameDb ? (Integer) header.getField(WHITE_TEAM_ID) : 0,
                    new TeamEntity(defaultName(header.getWhiteTeam())), database.getTeamBase());
            blackTeam = resolveOrCreateEntity(sameDb ? (Integer) header.getField(BLACK_TEAM_ID) : 0,
                    new TeamEntity(defaultName(header.getBlackTeam())), database.getTeamBase());
            gameTag = resolveOrCreateEntity(sameDb ? (Integer) header.getField(GAME_TAG_ID) : 0,
                    new GameTagEntity(defaultName(header.getGameTag())), database.getGameTagBase());
        } catch (IllegalArgumentException e) {
            throw new ChessBaseInvalidDataException("Failed to create ExtendedGameHeader entry due to invalid entity data reference", e);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Some argument were not set in the extended game header model", e);
        }

        return ExtendedGameHeader.builder()
                .id(gameId) // TODO: Is this the actual game id?
                .movesOffset(movesOfs)
                .annotationOffset(annotationOfs)
                .finalMaterial(false)
                .whiteTeamId(whiteTeam.getId())
                .blackTeamId(blackTeam.getId())
                .whiteRatingType(RatingType.international(TournamentTimeControl.NORMAL))
                .blackRatingType(RatingType.international(TournamentTimeControl.NORMAL))
                .creationTimestamp(0) // TODO
                .endgameInfo(null)
                .lastChangedTimestamp(0) // TODO
                .gameTagId(gameTag.getId())
                .build();
    }

    public GameHeader createGameHeader(Game game, long movesOfs, long annotationOfs) throws ChessBaseInvalidDataException {
        boolean sameDb = database.getDatabaseId().equals(game.getDatabase().getDatabaseId());

        GameHeader.GameHeaderBuilder builder = game.getHeader().toBuilder();

        if (!sameDb) {
            // If we're adding the game to a different database (usually the case),
            // the ids of all entities may be different so we can't just copy them
            // If no entity exists with the same name, create a new one.
            try {
                if (!game.isGuidingText()) {
                    PlayerEntity white = resolveOrCreateEntity(0,
                            PlayerEntity.fromFullName(game.getWhite().getFullName()), database.getPlayerBase());
                    PlayerEntity black = resolveOrCreateEntity(0,
                            PlayerEntity.fromFullName(game.getBlack().getFullName()), database.getPlayerBase());

                    builder.whitePlayerId(white.getId());
                    builder.blackPlayerId(black.getId());
                }

                TournamentEntity tournament = resolveOrCreateEntity(0,
                        new TournamentEntity(game.getTournament().getTitle(), game.getTournament().getDate()), database.getTournamentBase());
                AnnotatorEntity annotator = resolveOrCreateEntity(0,
                        new AnnotatorEntity(game.getAnnotator().getName()), database.getAnnotatorBase());
                SourceEntity source = resolveOrCreateEntity(0,
                        new SourceEntity(game.getSource().getTitle()), database.getSourceBase());

                builder.tournamentId(tournament.getId());
                builder.annotatorId(annotator.getId());
                builder.sourceId(source.getId());
            } catch (IllegalArgumentException e) {
                throw new ChessBaseInvalidDataException("Failed to create GameHeader entry due to invalid entity data reference", e);
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("Some argument were not set in the game header model", e);
            }
        }

        // The moves and annotation offset may be truncated if they are > 32 bits
        // This is fine as the full value is stored in the ExtendedGameHeader
        builder.annotationOffset((int) annotationOfs);
        builder.movesOffset((int) movesOfs);
        return builder.build();
    }

    public GameHeader createGameHeader(TextModel model, long movesOfs) throws ChessBaseInvalidDataException {
        TextHeaderModel header = model.getHeader();

        TournamentEntity tournament;
        AnnotatorEntity annotator;
        SourceEntity source;

        try {
            tournament = resolveOrCreateEntity(0,
                    new TournamentEntity(defaultName(header.getTournament()), new Date(header.getTournamentYear())), database.getTournamentBase());
            annotator = resolveOrCreateEntity(0,
                    new AnnotatorEntity(defaultName(header.getAnnotator())), database.getAnnotatorBase());
            source = resolveOrCreateEntity(0,
                    new SourceEntity(defaultName(header.getSource())), database.getSourceBase());
        } catch (IllegalArgumentException e) {
            throw new ChessBaseInvalidDataException("Failed to create GameHeader entry due to invalid entity data reference", e);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Some argument were not set in the text header model", e);
        }

        return GameHeader.builder()
                .game(true)
                .guidingText(true)
                .whitePlayerId(-1)
                .blackPlayerId(-1)
                .tournamentId(tournament.getId())
                .annotatorId(annotator.getId())
                .sourceId(source.getId())
                .playedDate(new Date(0))
                .result(GameResult.NOT_FINISHED)
                .round(header.getRound())
                .subRound(header.getSubRound())
                .eco(Eco.unset())
                .lineEvaluation(NAG.NONE)
                .medals(EnumSet.noneOf(Medal.class))
                .flags(EnumSet.noneOf(GameHeaderFlags.class)) // TODO: Maybe some flags can be set?
                .annotationOffset(0)
                .movesOffset((int) movesOfs)
                .build();
    }

    public GameHeader createGameHeader(GameModel model, long movesOfs, long annotationOfs) throws ChessBaseInvalidDataException {
        GameHeaderModel header = model.header();

        PlayerEntity white, black;
        TournamentEntity tournament;
        AnnotatorEntity annotator;
        SourceEntity source;

        // If the GameModel contains ID's from a different database, we can't use them
        boolean sameDb = database.getDatabaseId().equals(header.getField(DATABASE_ID));

        // Lookup or create the entities that are referenced in the game header
        // If the id field is set, use that one. Otherwise use the string field.
        // If no entity exists with the same name, create a new one.
        try {
            white = resolveOrCreateEntity(sameDb ? (Integer) header.getField(WHITE_ID) : 0,
                    PlayerEntity.fromFullName(defaultName(header.getWhite())), database.getPlayerBase());
            black = resolveOrCreateEntity(sameDb ? (Integer) header.getField(BLACK_ID) : 0,
                    PlayerEntity.fromFullName(defaultName(header.getBlack())), database.getPlayerBase());
            tournament = resolveOrCreateEntity(sameDb ? (Integer) header.getField(EVENT_ID) : 0,
                    new TournamentEntity(defaultName(header.getEvent()), Date.today()), database.getTournamentBase());
            annotator = resolveOrCreateEntity(sameDb ? (Integer) header.getField(ANNOTATOR_ID) : 0,
                    new AnnotatorEntity(defaultName(header.getAnnotator())), database.getAnnotatorBase());
            source = resolveOrCreateEntity(sameDb ? (Integer) header.getField(SOURCE_ID) : 0,
                    new SourceEntity(defaultName(header.getSourceTitle())), database.getSourceBase());
        } catch (IllegalArgumentException e) {
           throw new ChessBaseInvalidDataException("Failed to create GameHeader entry due to invalid entity data reference", e);
        }  catch (RuntimeException e) {
            throw new IllegalArgumentException("Some argument were not set in the game header model", e);
        }

        GameHeader.GameHeaderBuilder builder = GameHeader.builder();

        builder.game(true);
        builder.whitePlayerId(white.getId());
        builder.blackPlayerId(black.getId());
        builder.tournamentId(tournament.getId());
        builder.annotatorId(annotator.getId());
        builder.sourceId(source.getId());
        builder.playedDate(header.getDate() == null ? Date.today() : header.getDate());
        builder.result(header.getResult() == null ? GameResult.NOT_FINISHED : header.getResult());
        if (header.getRound() != null) {
            builder.round(header.getRound());
        }
        if (header.getSubRound() != null) {
            builder.subRound(header.getSubRound());
        }
        if (header.getWhiteElo() != null) {
            builder.whiteElo(header.getWhiteElo());
        }
        if (header.getBlackElo() != null) {
            builder.blackElo(header.getBlackElo());
        }
        builder.eco(header.getEco() == null ? Eco.unset() : header.getEco());

        int moves = (model.moves().countPly(false) + 1) / 2;
        builder.noMoves(moves > 255 ? -1 : moves);

        NAG eval = header.getLineEvaluation();
        builder.lineEvaluation(eval == null ? NAG.NONE : eval);

        setInferredData(builder, model.moves());

        // The moves and annotation offset may be truncated if they are > 32 bits
        // This is fine as the full value is stored in the ExtendedGameHeader
        builder.annotationOffset((int) annotationOfs);
        builder.movesOffset((int) movesOfs);
        return builder.build();
    }

    private void setInferredData(GameHeader.GameHeaderBuilder builder, GameMovesModel model) {
        // TODO: Add tests!!
        AnnotationStatistics stats = new AnnotationStatistics();

        collectStats(model.root(), stats);

        EnumSet<GameHeaderFlags> gameFlags = stats.getFlags();

        int v = model.countPly(true) - model.countPly(false);
        if (v > 0) {
            gameFlags.add(GameHeaderFlags.VARIATIONS);
            builder.variationsMagnitude(v > 1000 ? 4 : v > 300 ? 3 : v > 50 ? 2 : 1);
        }
        if (model.isSetupPosition()) {
            gameFlags.add(GameHeaderFlags.SETUP_POSITION);
        }
        if (!model.root().position().isRegularChess()) {
            gameFlags.add(GameHeaderFlags.UNORTHODOX);
        }

        // TODO: Stream flag (if it should be kept here!?)
        builder.medals(stats.getMedals());
        builder.flags(gameFlags);
        builder.commentariesMagnitude(stats.getCommentariesMagnitude());
        builder.symbolsMagnitude(stats.getSymbolsMagnitude());
        builder.graphicalSquaresMagnitude(stats.getGraphicalSquaresMagnitude());
        builder.graphicalArrowsMagnitude(stats.getGraphicalArrowsMagnitude());
        builder.trainingMagnitude(stats.getTrainingMagnitude());
        builder.timeSpentMagnitude(stats.getTimeSpentMagnitude());
    }

    private void collectStats(GameMovesModel.Node node, AnnotationStatistics stats) {
        for (Annotation annotation : node.getAnnotations()) {
            if (annotation instanceof StatisticalAnnotation) {
                ((StatisticalAnnotation) annotation).updateStatistics(stats);
            }
        }
        for (GameMovesModel.Node child : node.children()) {
            collectStats(child, stats);
        }
    }

    private @NonNull String defaultName(String name) {
        return name == null ? "" : name;
    }

    private <T extends Entity & Comparable<T>> T resolveEntity(EntityBase<T> base, String entityType, int id, Game game, boolean allowNone) throws ChessBaseInvalidDataException {
        if (id == -1 && allowNone) {
            return null;
        }
        T entity = base.get(id);
        if (entity == null) {
            throw new ChessBaseInvalidDataException(String.format("Invalid %s in game %d (id %d does not exist)", entityType, game.getId(), id));
        }
        return entity;
    }

    <T extends Entity & Comparable<T>> T resolveOrCreateEntity(Integer id, T key, EntityBase<T> base) {
        T entity;
        if (id != null && id > 0) {
            entity = base.get(id);
            if (entity == null) {
                throw new IllegalArgumentException("No entity with id " + id + " in " + base.getClass().getSimpleName());
            }
        } else {
            entity = base.getAny(key);
            if (entity == null) {
                try {
                    entity = base.add(key);
                } catch (EntityStorageException e) {
                    // Shouldn't happen since we tried to lookup the player first
                    throw new RuntimeException("Internal error");
                }
            }
        }
        return entity;
    }
}
