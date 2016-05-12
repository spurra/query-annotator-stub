package annotatorstub.annotator;
import java.util.*;
import annotatorstub.main.BingSearchMain;
import org.codehaus.jettison.json.JSONObject;
import annotatorstub.utils.SMAPHFeatures;
import it.unipi.di.acube.batframework.utils.WikipediaApiInterface;
public class CandidateGenerator {

	/**
	 * This Class provides the capabilities to get entity, feature pairs given a query
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		get_entity_candidates("Funny cats wikipedia");
	}
	public static WikipediaApiInterface wiki =  WikipediaApiInterface.api();
	
	public static Map<String,List<Double>> get_entity_candidates(String query) throws Exception {
		/**
		 *  Compute (entity,features) pairs given a string
		 */
		Map<String,List<Double>> entity_features = new HashMap<String,List<Double>>(); 
		JSONObject data = BingSearchMain.getQueryResults(query);

		String url;
		
		for (Integer idx =0;idx<data.getJSONArray("Web").length();idx++) {
			String entity = null;
			JSONObject res = data.getJSONArray("Web").getJSONObject(idx);
			url=res.getString("Url");
			if (url.startsWith("https://en.wikipedia.org/wiki/")) {
				entity = url.replace("https://en.wikipedia.org/wiki/", "");
			}
			
			if (entity != null) { // We have found an entity
				List<Double> features = new ArrayList<Double>();
				
				// Compute global features
				features.add(new Double(SMAPHFeatures.webTotal(data)));
				//features.add(new Double(SMAPHFeatures.isNE(res)));
				
				// Compute entity specific features
				//features.add(new Double(SMAPHFeatures.rank(data,entity)));
				// Use the rank we have instead
				features.add(new Double(idx));
				
				features.add(new Double(SMAPHFeatures.EDTitle(wiki,data,entity)));
				features.add(new Double(SMAPHFeatures.EDTitNP(wiki,data,entity)));
				features.add(new Double(SMAPHFeatures.captBolds(data)));
				features.add(new Double(SMAPHFeatures.boldTerms(data)));
				
				entity_features.put(entity, features);
				
			}
		}
		
		if (true) {
			for (String key : entity_features.keySet()) {
				System.out.print(key+ ": ");
				for (Double score : entity_features.get(key)) {
					System.out.print(score + " ");
				}
				System.out.println();				
			}
		}


		return entity_features;	
	}

}