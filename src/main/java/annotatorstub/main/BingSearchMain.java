/*
 * Bing account
 * mail: gygli@vision.ee.ethz.ch
 * pwd: Placeholder
 * key: jRstdZaO2NyTyCDBnXkl2PAXeXSGksYjM1T20XXuxa8
 */
package annotatorstub.main;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONObject;
import java.nio.file.Files;
import java.nio.file.Paths;
import it.unipi.di.acube.BingInterface;
import it.unipi.di.acube.batframework.utils.WikipediaApiInterface;
import annotatorstub.utils.WebResult;
import annotatorstub.utils.SMAPHFeatures;



public class BingSearchMain {
	public static BingInterface bing = new BingInterface("Zp18VyUZCbZVwR50JdlimwbkWsPHX+q+afXCfSW+ejU");
	//public static BingInterface bing = new BingInterface("jRstdZaO2NyTyCDBnXkl2PAXeXSGksYjM1T20XXuxa8");
	public static String corrected_query;
	// TODO add related search suggestions?
	public static JSONObject getQueryResults(String query) throws Exception {
		//String corrected_query;
		//List<WebResult> results = new ArrayList<WebResult>();
		String entity_file="data/" + query.replace("/", "_") + ".txt";
		File f = new File(entity_file);
		JSONObject a;
		if (f.exists()) {
			System.out.println("Read data from "  + entity_file);
			String content = new String(Files.readAllBytes(Paths.get(entity_file)));
			a = new JSONObject(content);
		} else {	
			
			a = bing.queryBing(query);
			try {
				FileWriter fr = new FileWriter(f);
				fr.write(a.toString());
				fr.close();
			} catch (Exception e) {
				
			}
		}
		JSONObject data = a.getJSONObject("d").getJSONArray("results").getJSONObject(0);
		
		
		corrected_query=data.getString("AlteredQuery");
		if (corrected_query==null || corrected_query.length()==0)  {
			corrected_query=query;
		}

		return data; 
	}
	public static void main(String[] args) throws Exception {

		/*BingInterface bing = new BingInterface("jRstdZaO2NyTyCDBnXkl2PAXeXSGksYjM1T20XXuxa8");
		JSONObject a = bing.queryBing();
		
		*/
		//System.out.println(BingSearchMain.getQueryResults("funny kittens"));
		// see: http://datamarket.azure.com/dataset/bing/search#schema for
		// query/response format
		WikipediaApiInterface wikiApi = WikipediaApiInterface.api();
		JSONObject q = BingSearchMain.getQueryResults("funy kittens wikipedia");
		System.out.println(q.toString(4));
		String e = "Cat";

		// Test the private functions.
		//SMAPHFeatures.testPrivateFunctions();

		// Test all the features

		int wT = SMAPHFeatures.webTotal(q);
		System.out.println("WebTotal: " + wT);
		int rank = SMAPHFeatures.rank(q, e);
		System.out.println("Rank: " + rank);
		double edTit = SMAPHFeatures.EDTitle(wikiApi, q, e);
		System.out.println("EDTitle: " + edTit);
		double edNP = SMAPHFeatures.EDTitNP(wikiApi, q, e);
		System.out.println("EDTitNP: " + edNP);
		double minEDB = SMAPHFeatures.minEDBolds(q);
		System.out.println("minEDBolds: " + minEDB);
		int captBold = SMAPHFeatures.captBolds(q);
		System.out.println("CaptBolds: " + captBold);
		double bTerms = SMAPHFeatures.boldTerms(q);
		System.out.println("boldTerms: " + bTerms);
	}
}
