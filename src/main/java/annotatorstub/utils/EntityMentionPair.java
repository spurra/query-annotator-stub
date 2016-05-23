package annotatorstub.utils;

import org.json.JSONObject;

public class EntityMentionPair implements Comparable<EntityMentionPair> {

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
	private int start_idx;
	private int end_idx;

    public EntityMentionPair(int wiki_id, String mention, String wiki_title, String wiki_abstract) {
    	
        this.wiki_id = wiki_id;
        this.mention = mention;
        this.wiki_title = wiki_title;
        this.wiki_abstract = wiki_abstract;
    }
    
    public EntityMentionPair(int wiki_id, String mention, String wiki_title, String wiki_abstract, double rho) {
    	this.wiki_id = wiki_id;
        this.mention = mention;
        this.wiki_title = wiki_title;
        this.wiki_abstract = wiki_abstract;
        this.rho=rho;
    }

    // Routine to convert JSON (as fetched from TagMe) to Entity object
    public EntityMentionPair(JSONObject json_obj) {
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
    
    public void setMentionPosition(int start_idx, int end_idx) {
    	this.start_idx=start_idx;
    	this.end_idx=end_idx;
    }
    
    public int getStartIdx() {
    	return this.start_idx;
    }
    
    public int getEndIdx() {
    	return this.end_idx;
    }
    
	public int compareTo(EntityMentionPair other) {
		
		double compare_rho = ((EntityMentionPair) other).getRho(); 

		
		//descending order
		if (compare_rho==this.getRho()) {
			return 0;
		} else if (compare_rho > this.getRho()) {
			return 1;
		} else {
			return -1;
		}
		
	}	

}
