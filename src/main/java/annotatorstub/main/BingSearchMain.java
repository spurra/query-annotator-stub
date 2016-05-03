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
import annotatorstub.utils.WebResult;
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
		if (false) {
			BingInterface bing = new BingInterface("jRstdZaO2NyTyCDBnXkl2PAXeXSGksYjM1T20XXuxa8");
			JSONObject a = bing.queryBing("funny kittens wikipedia");
			
			// see: http://datamarket.azure.com/dataset/bing/search#schema for
			// query/response format
			System.out.println(a.getJSONObject("d").getJSONArray("results").getJSONObject(0).toString(4));
		} else {
			BingSearchMain search = new BingSearchMain("funy kittens wikipedia");
			System.out.println(search.corrected_query);
			for (WebResult res : search.results) {
				System.out.println(res.toString());
			}
		}
	}
}
