package annotatorstub.annotator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.*;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.json.*;

import annotatorstub.utils.EntityMentionPair;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;

/**
 * Created by lennart on 5/15/16.
 */
public class TagMeAnnotator {

    // URL of the service
    private static final String TAGME_URL = "http://tagme.di.unipi.it/tag";

    // The key provided by the TagMe team
    private static final String TAGME_KEY = "eth2016a334GqA";

    // Singleton
    private static TagMeAnnotator instance = null;

    protected TagMeAnnotator() {
    }

    public static TagMeAnnotator getInstance() {
        if (instance == null) {
            instance = new TagMeAnnotator();
        }

        return instance;
    }

    // Retrieve all entities (unfiltered)
    static public List<EntityMentionPair> getEntities(String snippet) {
        return TagMeAnnotator.getFilteredEntities(snippet, 0.0d);
    }

    // Return a list of all entities that a snippet contains (according to TagMe)
    static public List<EntityMentionPair> getFilteredEntities(String snippet, double rho) {
        List<EntityMentionPair> filtered_entities = new ArrayList<>();

        if (snippet.isEmpty())
            return new ArrayList<>();

        try {


            Connection.Response doc;
            while (true) {
                doc = Jsoup.connect(TagMeAnnotator.TAGME_URL)
                        .method(Connection.Method.POST)
                        .data("text", snippet)
                        .data("key", TagMeAnnotator.TAGME_KEY)
                        .data("include_abstract", "true")
                        .ignoreContentType(true)
                        .execute();

                if (doc.statusCode() == 200)
                    break;

                System.out.println("HTTP Status Code: " + Integer.toString(doc.statusCode()));
                Thread.sleep(300);
            }

            String docText = doc.body();
            JSONObject o = new JSONObject(docText);


            // Extract entities
            List<EntityMentionPair> entities = TagMeAnnotator.extractEntitiesFromJSON(o);

            // Filter entities (drop out entities that are too weak)
            filtered_entities = new ArrayList<>();
            for (EntityMentionPair entity : entities) {
                if (entity.getRho() >= rho)
                    filtered_entities.add(entity);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return filtered_entities;


    }
    // Extract the entities from the TagMe's server response
    static public List<EntityMentionPair> extractEntitiesFromJSON(JSONObject json_root) throws Exception {
        List<EntityMentionPair> result = new ArrayList<>();



        JSONArray json_array = json_root.getJSONArray("annotations");

        for (int i = 0; i < json_array.length(); i++) {
            JSONObject json_annotation = json_array.getJSONObject(i);
            result.add(new EntityMentionPair(json_annotation));
        }



        return result;
    }

}
