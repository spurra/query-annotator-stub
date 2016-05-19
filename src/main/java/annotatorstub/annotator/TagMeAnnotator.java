package annotatorstub.annotator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.json.*;

import annotatorstub.utils.EntityMentionPair;

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
        if (snippet.isEmpty())
            return new ArrayList<>();

        // Initialization
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(TagMeAnnotator.TAGME_URL);

        // Hand over parameters
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("text", snippet));
        params.add(new BasicNameValuePair("key", TagMeAnnotator.TAGME_KEY));
        params.add(new BasicNameValuePair("include_abstract", "true"));

        // TODO: Demand abstract

        try {
            // Encode
            httpPost.setEntity(new UrlEncodedFormEntity(params));

            // Retrieve server response
            CloseableHttpResponse response = client.execute(httpPost);
            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

            StringBuffer result = new StringBuffer();
            String line = "";
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }

            // Convert to JSON
            JSONObject o = new JSONObject(result.toString());

            // Close client
            client.close();

            // Extract entities
            List<EntityMentionPair> entities = TagMeAnnotator.extractEntitiesFromJSON(o);

            // Filter entities (drop out entities that are too weak)
            List<EntityMentionPair> filtered_entities = new ArrayList<>();
            for (EntityMentionPair entity : entities) {
                if (entity.getRho() >= rho)
                    filtered_entities.add(entity);
            }

            return filtered_entities;

        } catch (UnsupportedEncodingException e) {
            System.err.println("Error while encoding POST request");
        } catch (IOException e) {
            System.err.println("Error while transmitting POST request");
        }

        return null;
    }

    // Extract the entities from the TagMe's server response
    static public List<EntityMentionPair> extractEntitiesFromJSON(JSONObject json_root) {
        List<EntityMentionPair> result = new ArrayList<>();

        JSONArray json_array = json_root.getJSONArray("annotations");

        for (int i = 0; i < json_array.length(); i++) {
            JSONObject json_annotation = json_array.getJSONObject(i);

            result.add(new EntityMentionPair(json_annotation));
        }

        return result;
    }

}
