package annotatorstub.annotator;
import java.util.ArrayList;
import java.util.List;
import org.json.*;
/**
 * Created by lennart on 5/15/16.
 */
public class TagMeAnnotator {
    // Singleton
    private static TagMeAnnotator instance = null;

    // URL of the service
    private static final String TAGME_URL = "http://tagme.di.unipi.it/tag";

    // The key provided by the TagMe team
    private static final String KEY = "";

    protected TagMeAnnotator () {}

    public static TagMeAnnotator getInstance() {
        if (instance == null) {
            instance = new TagMeAnnotator();
        }

        return instance;
    }

    // Return a list of all entities that
    public List<Integer> getEntities (String snippet) {
        List<Integer> result = new ArrayList<>();

        return result;
    }

    // Extract the entities from the TagMe's server response
    public List<Entity> extractEntitiesFromJSON (JSONObject json_root) {
        List<Entity> result = new ArrayList<>();

        JSONArray json_array = json_root.getJSONArray("annotations");

        for (int i = 0; i < json_array.length(); i++) {
            JSONObject json_annotation = json_array.getJSONObject(i);

            result.add(new Entity(json_annotation));
        }

        return result;
    }

    public class Entity {

        public String getWikiId() {
            return this.wiki_id;
        }

        public String getWikiTitle() {
            return this.wiki_title;
        }

        public String getWikiAbstract() {
            return this.wiki_abstract;
        }

        public double getRho() {
            return this.rho;
        }

        // Id of Wikipedia article
        private String wiki_id;

        // Title of wikipedia article
        private String wiki_title;

        // Abstract of wikipedia article
        private String wiki_abstract;

        // Rho ("Quality measure")
        private double rho;

        public Entity (String wiki_id, String wiki_title, String wiki_abstract) {
            this.wiki_id = wiki_id;
            this.wiki_title = wiki_title;
            this.wiki_abstract = wiki_abstract;
        }

        // Routine to convert JSON (as fetched from TagMe) to Entity object
        public Entity (JSONObject json_obj) {
            this.wiki_id = json_obj.getString("id");
            this.wiki_title = json_obj.getString("title");
            this.wiki_abstract = json_obj.getString("abstract");

            this.rho = json_obj.getDouble("rho");
        }

    }
}
