package yarin.cbhlib;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yarin.asflib.ASFScriptCommand;
import yarin.asflib.ASFScriptCommandReader;
import yarin.cbhlib.actions.ApplyActionException;
import yarin.cbhlib.actions.RecordedAction;
import yarin.cbhlib.annotations.GraphicalArrowsAnnotation;
import yarin.cbhlib.exceptions.CBMException;
import yarin.chess.GameMetaData;
import yarin.chess.GameModel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

public class ChessBaseMediaParserTest {

    private static final Logger log = LoggerFactory.getLogger(ChessBaseMediaParserTest.class);

    @Test
    public void testParseCommand() throws CBMException {
        String command =
                "0000000590020000000000000000000000000000000b47757267656e6964" +
                "7a65000842756b68757469000748656e6c65790006526f6e205700085462" +
                "696c69736900085462696c69736900007f0f000300950008030f00044d43" +
                "4c000a43686573734261736500000000000000000001010c4b6f7073612c" +
                "506574726900a609dd09006a000000007e0f002500000000000000000000" +
                "00000000000100004a00000021230000002625000000302a0000001e1c00" +
                "00000812000000372d00000023240000002d1e000000191b000000161400" +
                "0000280c0000000f1500000020300000002f260000002820000000273700" +
                "000012210000001f0d0000000c03000001090000000c150000000e150000" +
                "00111200000017050000002132000000141b000000121b00000015140000" +
                "00090a000000141b000000211b0000001e14000000030a0000002e2d0000" +
                "00102b0000002d240000002b2400000014230000002432000000373f0000" +
                "001112000000262d00000020210000002332000000393200000025240000" +
                "001b1100000024230000002a390000001c1b000000121b000000151b0000" +
                "00111b0000002d1b0000001820000000171e00000000100000002f2d0000" +
                "003928000000072f00000028220000002d2900000020290000002f290000" +
                "0030290000003e3c00000010110000001e3300000021200000001b090000" +
                "000a1c0000000d2d0000002930000000091b00000030390000002d240000" +
                "002233000000241c00000033220000001c3400000022280000003c3b0000" +
                "0011170000003f3e01000800000000030008008a0000000000";
        new ChessBaseMediaParser().parseTextCommand(command);
    }

    //@Test
    public void findCommandTypes() throws IOException {
        final AtomicInteger noErrors = new AtomicInteger();
        Files.walk(Paths.get("/Users/yarin/chessbasemedia/mediafiles/TEXT")).forEach(filePath -> {
            if (Files.isRegularFile(filePath) && filePath.toString().endsWith(".wmv")) {
                try {
//                    System.out.println("Reading " + filePath);
                    ASFScriptCommandReader reader = new ASFScriptCommandReader(filePath.toFile());
                    int cnt = reader.init();
                    if (cnt == 0) {
                        System.out.println("NO COMMANDS  " + filePath);
                    } else {
                        ASFScriptCommand cmd = reader.read();
                        System.out.println(String.format("%-20s %s", cmd.getType(), filePath));
                    }
                } catch (IOException e) {
                    System.err.println("Error reading file " + filePath);
                    noErrors.incrementAndGet();
                }
            }
        });
    }
}
