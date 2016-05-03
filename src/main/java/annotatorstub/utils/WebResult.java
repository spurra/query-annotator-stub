package annotatorstub.utils;

public class WebResult {
	public String title;
	public String description;
	public String url;
	
	public WebResult(String title, String description, String url) {
		this.title=title;
		this.url=url;
		this.description=description;
	}
	
	public String toString() { 
	    return "title: '" + this.title + "', url: '" + this.url + "', description: '" + this.description + "'";
	} 
}