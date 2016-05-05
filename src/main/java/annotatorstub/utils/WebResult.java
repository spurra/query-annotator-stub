package annotatorstub.utils;
import java.util.regex.Pattern;

public class WebResult {
	public String title;
	public String description;
	public String url;
	public String entity;
	
	public WebResult(String title, String description, String url) {
		this.title=title;
		this.url=url;
		this.description=description;
		if (url.startsWith("https://en.wikipedia.org/wiki/")) {
			this.entity = url.replace("https://en.wikipedia.org/wiki/", "");
		}
		
	}
	
	public String toString() { 
	    return "title: '" + this.title + "', entity: '" + this.entity + "', url: '" + this.url + "', description: '" + this.description + "'";
	} 
}