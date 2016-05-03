/*
 * Bing account
 * mail: gygli@vision.ee.ethz.ch
 * pwd: Placeholder
 * key: jRstdZaO2NyTyCDBnXkl2PAXeXSGksYjM1T20XXuxa8
 */
package annotatorstub.main;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONObject;

import it.unipi.di.acube.BingInterface;
import it.unipi.di.acube.batframework.utils.WikipediaApiInterface;
import annotatorstub.utils.WebResult;
import annotatorstub.utils.SMAPHFeatures;


public class BingSearchMain {
	public BingInterface bing = new BingInterface("jRstdZaO2NyTyCDBnXkl2PAXeXSGksYjM1T20XXuxa8");
	public String corrected_query;
	public List<WebResult> results = new ArrayList<WebResult>();
	// TODO add related search suggestions?
	
	public BingSearchMain(String query) throws Exception {		
		BingInterface.setCache("bing_cache.txt");
		JSONObject a = bing.queryBing(query);
		JSONObject data = a.getJSONObject("d").getJSONArray("results").getJSONObject(0);
		corrected_query=data.getString("AlteredQuery");
		if (corrected_query==null || corrected_query.length()==0)  {
			corrected_query=query;
		}
		for (Integer idx =0;idx<data.getJSONArray("Web").length();idx++) {
			JSONObject res = data.getJSONArray("Web").getJSONObject(idx);
			results.add(new WebResult(res.getString("Title"),res.getString("Description"),res.getString("Url")));
		
		}
	}
	public static void main(String[] args) throws Exception {

		if (true) {
			BingInterface bing = new BingInterface("jRstdZaO2NyTyCDBnXkl2PAXeXSGksYjM1T20XXuxa8");
			JSONObject a = bing.queryBing("funy kittens wikipedia");
			WikipediaApiInterface wikiApi = WikipediaApiInterface.api();

			// see: http://datamarket.azure.com/dataset/bing/search#schema for
			// query/response format
			JSONObject q = a.getJSONObject("d").getJSONArray("results").getJSONObject(0);
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

		} else {
			BingSearchMain search = new BingSearchMain("funy kittens wikipedia");
			System.out.println(search.corrected_query);
			for (WebResult res : search.results) {
				System.out.println(res.toString());
			}
		}
	}
}
