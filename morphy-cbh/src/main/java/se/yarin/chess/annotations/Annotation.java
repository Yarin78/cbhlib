package se.yarin.chess.annotations;

import lombok.NonNull;
import se.yarin.chess.GameMovesModel;

/**
 * An annotation of a chess move or position in the {@link GameMovesModel}.
 * An annotation is always attached to a {@link se.yarin.chess.GameMovesModel.Node},
 * and then refers to either the position at that node, or the move leading up to
 * that node (the {@link GameMovesModel.Node#lastMove()}.
 *
 * An Annotation is immutable.
 */
public abstract class Annotation {

    /**
     * A value indicating which order the priority should be formatted
     * (when converting a game to text) compared to other annotations
     * for the same position.
     * @return a priority, higher value means it should apply early
     */
    public int priority() {
        return 0;
    }

    /**
     * Formats this annotation
     * @param text the existing text for the move that this annotation is attached to
     * @param ascii if true, then only ASCII characters are allowed to be added to text
     * @return an updated
     */
    public String format(@NonNull String text, boolean ascii) {
        return text;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}
