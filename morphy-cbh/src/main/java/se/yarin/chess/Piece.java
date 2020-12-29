package se.yarin.chess;

/**
 * A Piece is an uncolored type of chessman
 */
public enum Piece {
    NO_PIECE(0, ' '),
    PAWN(1, 'P'),
    KNIGHT(2, 'N'),
    BISHOP(3, 'B'),
    ROOK(4, 'R'),
    QUEEN(5, 'Q'),
    KING(6, 'K');

    private final int value;
    private final char ch;
    private static Piece[] charToPiece;

    static {
        charToPiece = new Piece[128];
        for (Piece piece : Piece.values()) {
            charToPiece[piece.ch] = piece;
        }
    }

    public Stone toStone(Player player) {
        if (player == Player.NOBODY) return Stone.NO_STONE;
        switch (this) {
            case NO_PIECE: return Stone.NO_STONE;
            case PAWN:     return player == Player.WHITE ? Stone.WHITE_PAWN : Stone.BLACK_PAWN;
            case KNIGHT:   return player == Player.WHITE ? Stone.WHITE_KNIGHT: Stone.BLACK_KNIGHT;
            case BISHOP:   return player == Player.WHITE ? Stone.WHITE_BISHOP: Stone.BLACK_BISHOP;
            case ROOK:     return player == Player.WHITE ? Stone.WHITE_ROOK: Stone.BLACK_ROOK;
            case QUEEN:    return player == Player.WHITE ? Stone.WHITE_QUEEN: Stone.BLACK_QUEEN;
            case KING:     return player == Player.WHITE ? Stone.WHITE_KING: Stone.BLACK_KING;
        }
        throw new RuntimeException("Invalid piece: " + this);
    }

    public char toChar() {
        return ch;
    }

    public static Piece fromChar(char ch) {
        return charToPiece[ch];
    }

    Piece(int value, char ch) {
        this.value = value;
        this.ch = ch;
    }
}
