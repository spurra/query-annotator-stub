/*
 * Bing account
 * mail: gygli@vision.ee.ethz.ch
 * pwd: Placeholder
 * key: jRstdZaO2NyTyCDBnXkl2PAXeXSGksYjM1T20XXuxa8
 */
package annotatorstub.main;

import org.codehaus.jettison.json.JSONObject;

import it.unipi.di.acube.BingInterface;

import annotatorstub.utils.SMAPHFeatures;

public class BingSearchMain {
	public static void main(String[] args) throws Exception {
		BingInterface bing = new BingInterface("jRstdZaO2NyTyCDBnXkl2PAXeXSGksYjM1T20XXuxa8");
		JSONObject a = bing.queryBing("funy kittens wikipedia");

		// see: http://datamarket.azure.com/dataset/bing/search#schema for
		// query/response format
		//System.out.println(a.toString(4))
		JSONObject q = a.getJSONObject("d").getJSONArray("results").getJSONObject(0);
		System.out.println(q.toString(4));

		String entity = "kittens";

		int webTotal = SMAPHFeatures.webTotal(q);



	}
}
