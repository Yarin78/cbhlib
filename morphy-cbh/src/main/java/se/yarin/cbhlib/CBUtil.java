package se.yarin.cbhlib;

import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.yarin.chess.Date;
import se.yarin.chess.Eco;
import se.yarin.chess.GameResult;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.EnumSet;

/**
 * Contains various utility functions for reading and parsing ChessBase data files.
 */
public final class CBUtil {
    private static final Logger log = LoggerFactory.getLogger(CBUtil.class);

    private CBUtil() { }

    // This is the character set that CB uses
    static Charset cbCharSet = Charset.forName("ISO-8859-1");

    /**
     * Decodes a 21 bit CBH encoded date to a {@link Date}.
     * @param dateValue an integer containing an encoded date value
     * @return the decoded date
     */
    public static Date decodeDate(int dateValue) {
        // Bit 0-4 is day, bit 5-8 is month, bit 9-20 is year
        dateValue %= (1<<21);
        int day = dateValue % 32;
        int month = (dateValue / 32) % 16;
        int year = dateValue / 512;
        return new Date(year, month, day);
    }

    /**
     * Converts a {@link Date} to a 21 bit CBH encoded date.
     * @param date the date to encode
     * @return the encoded date
     */
    public static int encodeDate(@NonNull Date date) {
        return (date.year() * 512 + date.month() * 32 + date.day()) % (1<<21);
    }

    /**
     * Decodes a CBH encoded Eco code to a {@link Eco}
     * @param ecoValue an integer containing an encoded Eco value
     * @return the decoded Eco
     */
    public static Eco decodeEco(int ecoValue) {
        int eco = ecoValue / 128 - 1;
        int subEco = ecoValue % 128;
        return eco < 0 ? Eco.unset() : Eco.fromInt(eco, subEco);
    }

    /**
     * Converts a {@link Eco} to a CBH encoded Eco.
     * @param eco the Eco to encode
     * @return the encoded Eco
     */
    public static int encodeEco(@NonNull Eco eco) {
        if (!eco.isSet()) {
            return 0;
        }
        return (eco.getInt() + 1) * 128 + eco.getSubEco();
    }

    public static GameResult decodeGameResult(int data) {
        return GameResult.values()[data];
    }

    public static int encodeGameResult(GameResult data) {
        return data.ordinal();
    }

    public static int encodeTournamentType(TournamentType type, TournamentTimeControl timeControl) {
        // bit 0-3: type
        // bit 5: blitz
        // bit 6: rapid
        // bit 7: correspondence
        // But only one of bit 5-7 is actually set
        int typeValue = 0;
        switch (timeControl) {
            case BLITZ: typeValue = 32; break;
            case RAPID: typeValue = 64; break;
            case CORRESPONDENCE: typeValue = 128; break;
        }
        typeValue += type.ordinal();
        return typeValue;
    }

    public static TournamentType decodeTournamentType(int data) {
        // TODO: Out of range
        return TournamentType.values()[data & 31];
    }

    public static TournamentTimeControl decodeTournamentTimeControl(int data) {
        if ((data & 32) > 0) return TournamentTimeControl.BLITZ;
        if ((data & 64) > 0) return TournamentTimeControl.RAPID;
        if ((data & 128) > 0) return TournamentTimeControl.CORRESPONDENCE;
        return TournamentTimeControl.NORMAL;
    }

    public static Nation decodeNation(int data) {
        // TODO: Should save this value raw instead to make it more future proof
        if (data < 0 || data >= Nation.values().length) {
            return Nation.NONE;
        }
        return Nation.values()[data];
    }

    public static int encodeNation(Nation nation) {
        return nation.ordinal();
    }

    public static EnumSet<Medal> decodeMedals(int data) {
        EnumSet<Medal> medals = EnumSet.noneOf(Medal.class);
        for (Medal medal : Medal.values()) {
            if (((1<<medal.ordinal()) & data) > 0) {
                medals.add(medal);
            }
        }
        return medals;
    }

    public static int encodeMedals(EnumSet<Medal> medals) {
        int value = 0;
        for (Medal medal : medals) {
            value += (1 << medal.ordinal());
        }
        return value;
    }

    public static FinalMaterial decodeFinalMaterial(int value) {
        int numPawns = (value >> 12) & 15;
        int numQueens = (value >> 9) & 7;
        int numKnights = (value >> 6) & 7;
        int numBishops = (value >> 3) & 7;
        int numRooks = (value >> 3) & 7;
        return new FinalMaterial(numPawns, numQueens, numKnights, numBishops, numRooks);
    }

    public static int encodeFinalMaterial(FinalMaterial material) {
        if (material == null) {
            return 0;
        }
        int value = (material.getNumPawns() & 15) << 12;
        value += Math.max(material.getNumQueens(), 7) << 9;
        value += Math.max(material.getNumKnights(), 7) << 6;
        value += Math.max(material.getNumBishops(), 7) << 3;
        value += Math.max(material.getNumRooks(), 7);
        return value;
    }

    public static EndgameType decodeEndgameType(int value) {
        if (value < 0 || value >= EndgameType.values().length) {
            return EndgameType.NONE;
        }
        return EndgameType.values()[value];
    }

    public static int encodeEndgameType(@NonNull EndgameType endgameType) {
        return endgameType.ordinal();
    }

    // Debug code

    public static String toHexString(ByteBuffer buf) {
        int oldPos = buf.position();
        byte[] bytes = new byte[buf.limit() - oldPos];
        buf.get(bytes);
        buf.position(oldPos);
        return toHexString(bytes);
    }

    public static String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i];
            if (v < 0) v += 256;
            sb.append(String.format("%02X ", v));
        }
        return sb.toString();

    }
}