package annotatorstub.utils;

import org.json.JSONObject;

public class TagMeEntity {

    // Id of Wikipedia article
    private int wiki_id;
    // Mention that has been spotted (e.g. Obama for entity Barack Obama)
    private String mention;
    // Title of wikipedia article (eg. en.wikipedia.org/Barack_Obama)
    private String wiki_title;
    // Abstract of wikipedia article
    private String wiki_abstract;
    // Rho ("Quality measure")
    private double rho;

    public TagMeEntity(int wiki_id, String mention, String wiki_title, String wiki_abstract) {
        this.wiki_id = wiki_id;
        this.mention = mention;
        this.wiki_title = wiki_title;
        this.wiki_abstract = wiki_abstract;
    }

    // Routine to convert JSON (as fetched from TagMe) to Entity object
    public TagMeEntity(JSONObject json_obj) {
        this.wiki_id = json_obj.getInt("id");
        this.mention = json_obj.getString("spot");
        this.wiki_title = json_obj.getString("title");
        this.wiki_abstract = json_obj.getString("abstract");

        this.rho = json_obj.getDouble("rho");
    }

    public int getWikiId() {
        return this.wiki_id;
    }

    public String getMention() {
        return this.mention;
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

}
