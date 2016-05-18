package annotatorstub.annotator;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by lennart on 5/17/16.
 */
public class TagMeAnnotatorHack {
    // URL of service
    public static final String TAGME_URL = "http://tagme.di.unipi.it";

    // Singleton
    private static TagMeAnnotatorHack instance;

    protected TagMeAnnotatorHack() {}

    public static TagMeAnnotatorHack getInstance() {
        if (instance == null) {
            instance = new TagMeAnnotatorHack();
        }

        return instance;
    }

    public List<Entity> getEntities(String snippet, double rho) {
        List<Entity> result = new ArrayList<>();

        // Prepare data
        Map<String, String> data = new HashMap<>();
        data.put("lang", "en");
        data.put("text", snippet);
        data.put("rho", "50");

        try {
             // Assemble and submit HTTP Post request
            Document doc = Jsoup.connect(TAGME_URL + "/gui")
                .header("Accept-Encoding", "gzip, deflate, sdch")
                .header("Accept-Language", "en-US,en;q=0.8,de;q=0.6")
                .header("Upgrade-Insecure-Requests", "1")
                .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .header("Cookie", "JSESSIONID=5E4D335F715B75136CB454DD48CB17FA")
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .header("Connection", "keep-alive")
                .ignoreContentType(true)
                .data(data).post();

            System.out.println(doc);
        } catch (IOException e) {
            e.printStackTrace();
        }


        return result;
    }

    public class Entity {
        // Anchor text
        private String anchor_text;

        // Link
        private String link;

        public Entity (String anchor_text, String link) {
            this.anchor_text = anchor_text;
            this.link = link;
        }

        public String getAnchorText() {
            return this.anchor_text;
        }

        public String getLink() {
            return this.link;
        }
    }
}
