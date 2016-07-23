package se.yarin.cbhlib.annotations;

import lombok.Getter;
import se.yarin.cbhlib.ByteBufferUtil;
import se.yarin.chess.annotations.Annotation;

import java.nio.ByteBuffer;

public class WhiteClockAnnotation extends Annotation {
    @Getter
    private int clockTime;

    public WhiteClockAnnotation(int clockTime) {
        this.clockTime = clockTime;
    }

    @Override
    public String toString() {
        int hours = clockTime / 100 / 3600;
        int minutes = (clockTime / 100 / 60) % 60;
        int seconds = (clockTime / 100) % 60;
        return String.format("WhiteClock = %02d:%02d:%02d", hours, minutes, seconds);
    }

    public static WhiteClockAnnotation deserialize(ByteBuffer buf) {
        return new WhiteClockAnnotation(ByteBufferUtil.getIntB(buf));
    }
}