package annotatorstub.annotator;
import java.util.List;
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

    public List<Integer> extractEntitiesAsIdentifiers(String snippet) {
        List<Integer> result =
    }

    public class Entity {
        // Id of Wikipedia article
        private String wiki_id;

        // Title of wikipedia article
        private String wiki_title;

        // Abstract of wikipedia article
        private String wiki_abstract;

        public Entity (String wiki_id, String wiki_title, String wiki_abstract) {
            this.wiki_id = wiki_id;
            this.wiki_title = wiki_title;
            this.wiki_abstract = wiki_abstract;
        }

        // Routine to convert JSON (as fetched from TagMe) to Entity object
        // TODO
    }
}
