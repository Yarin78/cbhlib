package se.yarin.cbhlib;

import se.yarin.cbhlib.entities.EntityStorageException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExtractNations {
    public static void main2(String[] args) throws IOException {
        FileInputStream fis = new FileInputStream("/Users/yarin/src/cbhlib/CBase13.exe");
        BufferedInputStream bis = new BufferedInputStream(fis);
        StringBuilder sb = new StringBuilder(19*1024*1024);
        int ch;
        while ((ch = bis.read()) != -1) {
            if (ch < 32 || ch > 126) ch = 32;
            sb.append((char) ch);
        }
        bis.close();
        String input = sb.toString();

        Pattern pattern = Pattern.compile("M_NAT_([A-Z]*) ");
        Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            System.out.println(matcher.group(1));
        }
    }

    public static void main(String[] args) throws IOException, EntityStorageException {
        TournamentBase tb = TournamentBase.open(new File("/Users/yarin/chessbasemedia/mediafiles/cbh/tournament_test.cbt"));

        for (TournamentEntity te : tb.getAll()) {
            System.out.println(te.getId() + ": " + te);
        }
        for (int i = 1; i <= 100; i++) {
            TournamentEntity te = new TournamentEntity("country #" + i);
            te.setNation(Nation.values()[i]);
            te.setCount(1);
            te.setFirstGameId(1);
            tb.add(te);
        }
        tb.close();
    }
}
