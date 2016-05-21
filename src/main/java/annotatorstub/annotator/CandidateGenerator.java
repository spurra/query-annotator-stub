package annotatorstub.annotator;
import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.*;

import annotatorstub.classification.ModelConverter;
import annotatorstub.main.BingSearchMain;
import org.codehaus.jettison.json.JSONObject;
import annotatorstub.utils.SMAPHFeatures;
import annotatorstub.utils.EntityMentionPair;
import it.unipi.di.acube.batframework.utils.WikipediaApiInterface;
public class CandidateGenerator {

	/**
	 * This Class provides the capabilities to get entity, feature pairs given a query
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		get_entity_candidates("los angeles oversized towing");
	}
	
	public static boolean use_invididual_words = true;
	public static boolean use_tagme = true;	
	public static int num_invididual_words = 5;
	private static final String feature_path = "data/svm/features/";
	public static WikipediaApiInterface wiki =  WikipediaApiInterface.api();
	
	
	public static Map<String,List<Double>> get_entity_candidates(String query) throws Exception {
		/**
		 *  Compute (entity,features) pairs given a string
		 */

		Map<String,List<Double>> entity_features = get_entites_and_features(query);
		Map<String,List<Double>> entity_features_wiki = get_entites_and_features(query+ " wikipedia");
		for (String key : entity_features_wiki.keySet()) {
			entity_features.put(key, entity_features_wiki.get(key));
		}
		if (use_invididual_words) {
			String[] words = query.split(" ");
			for (String word : words) {
				Map<String,List<Double>> entity_features_words_wiki = get_entites_and_features(word + " wikipedia",num_invididual_words);
				for (String key : entity_features_words_wiki.keySet()) {
					entity_features.put(key, entity_features_words_wiki.get(key));
				}
			}
		}
		if (use_tagme) {
			List<EntityMentionPair> tagme_entities=TagMeAnnotator.getEntities(query);
			for (EntityMentionPair ent : tagme_entities) {
				Map<String,List<Double>> entity_features_words_tagme = get_entites_and_features(ent.getWikiTitle() + " wikipedia",1);
				for (String key : entity_features_words_tagme.keySet()) {
					entity_features.put(key, entity_features_words_tagme.get(key));
				}	
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
	public static Map<String,List<Double>> get_entites_and_features(String query) throws Exception {
		return get_entites_and_features(query,999);
	}
	public static Map<String,List<Double>> get_entites_and_features(String query,int max_results) throws Exception {
		Map<String,List<Double>> entity_features = new HashMap<String,List<Double>>(); 
		JSONObject queryData = BingSearchMain.getQueryResults(query);

		String url;
		
		for (Integer idx =0;idx<queryData.getJSONArray("Web").length() && idx<max_results;idx++) {
			String entity = null;
			JSONObject res = queryData.getJSONArray("Web").getJSONObject(idx);
			url=res.getString("Url");
			entity = get_entity(url);
			
			if (entity != null) { // We have found an entity

				// Skip if cached features
				String cand_file_name = feature_path + entity.replace("/", "_") + ".txt";
				File f = new File(cand_file_name);
				List<Double> features = new ArrayList<Double>();
				if (!f.exists()) {

					// Compute global features
					features.add(new Double(SMAPHFeatures.webTotal(queryData)));
					//features.add(new Double(SMAPHFeatures.isNE(res)));

					// Compute entity specific features
					features.add(new Double(SMAPHFeatures.rank(queryData, entity)));
					// Use the rank we have instead
					//features.add(new Double(idx));

					features.add(new Double(SMAPHFeatures.EDTitle(wiki, queryData, entity)));
					features.add(new Double(SMAPHFeatures.EDTitNP(wiki, queryData, entity)));
					features.add(new Double(SMAPHFeatures.captBolds(queryData)));
					features.add(new Double(SMAPHFeatures.boldTerms(queryData)));

					// New features
					features.add(new Double(SMAPHFeatures.freq(queryData, entity)));
					features.add(new Double(SMAPHFeatures.avgRank(queryData, entity)));
					features.add(new Double(SMAPHFeatures.rhoMin(queryData, entity)));
					features.add(new Double(SMAPHFeatures.rhoMax(queryData, entity)));
					features.add(new Double(SMAPHFeatures.rhoAvg(queryData, entity)));
					features.add(new Double(SMAPHFeatures.ambigMin(wiki, queryData, entity)));
					features.add(new Double(SMAPHFeatures.ambigMax(wiki, queryData, entity)));
					features.add(new Double(SMAPHFeatures.ambigAvg(wiki, queryData, entity)));
					features.add(new Double(SMAPHFeatures.commMin(wiki, queryData, entity)));
					features.add(new Double(SMAPHFeatures.commMax(wiki, queryData, entity)));
					features.add(new Double(SMAPHFeatures.commAvg(wiki, queryData, entity)));
					features.add(new Double(SMAPHFeatures.lpMin(queryData)));
					features.add(new Double(SMAPHFeatures.lpMax(queryData)));
					features.add(new Double(SMAPHFeatures.mentMEDMin(queryData)));
					features.add(new Double(SMAPHFeatures.mentMEDMax(queryData)));
				}
				// Add features
				entity_features.put(entity, features);
				
			}
		}
		return entity_features;	
	}

	static WikipediaApiInterface wikiApi = WikipediaApiInterface.api();
	private static String get_entity(String url) throws IOException {
		/**
		 * Find entity in any language
		 * and verify that it is there, using the wikipedia API
		 * if none was found or the verification failed, return null.
		 * Otherwise return entity string
		 */
		String entity=null;
		int found_id = -1;
		url=URLDecoder.decode(url, "UTF-8");
		url=url.replace("https://", "").replace("http://", "");
		if (url.contains("wikipedia.org/wiki/")) {
			entity = url.replace("wikipedia.org/wiki/", "");
			entity=entity.split("\\.")[1];
			if (url.startsWith("www") && entity.split(":").length>1) {
				entity=entity.split(":")[1];
			}
			found_id = wikiApi.getIdByTitle(entity);
			if (found_id<0) {
				System.err.println("Found entity " + entity + " " + found_id + "; " + url);
			}else {
				//System.out.println("Found entity " + entity + " " + found_id + "; " + url);
			}
		}
		if (found_id == -1){
			return null;
		} else {
			entity = entity.replaceAll("_", " ");
			return entity;
		}
		
	}

}