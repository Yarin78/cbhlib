package se.yarin.morphy.tools;

import se.yarin.cbhlib.entities.PlayerBase;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class BenchmarkEntityIndexBuilder {
    public static void main(String[] args) throws IOException {
        URL url = BenchmarkEntityIndexBuilder.class.getResource("megadb2016.cbp");
        PlayerBase playerBase = PlayerBase.open(new File(url.getFile()));
        System.out.println("Counting all: " + playerBase.streamOrderedAscending().count());
    }
}
