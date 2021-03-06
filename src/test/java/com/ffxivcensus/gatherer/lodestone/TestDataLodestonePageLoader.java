package com.ffxivcensus.gatherer.lodestone;

import java.io.File;
import java.io.IOException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class TestDataLodestonePageLoader implements LodestonePageLoader {

    @Override
    public Document getCharacterPage(int characterId) throws IOException, InterruptedException, CharacterDeletedException {
        Document doc;
        try {
            doc = Jsoup.parse(
                              new File(
                                       this.getClass().getResource(
                                                                   String.format("/data/lodestone/Character-%d.html", characterId))
                                           .toURI()),
                              null);
        } catch(Exception e) {
            throw new RuntimeException();
        }
        return doc;
    }

}
