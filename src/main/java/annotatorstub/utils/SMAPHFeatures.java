package annotatorstub.utils;
/**
 * Created by Adrian on 29/04/16.
 */

import annotatorstub.annotator.TagMeAnnotator;
import annotatorstub.main.BingSearchMain;
import it.unipi.di.acube.batframework.utils.Pair;
import it.unipi.di.acube.batframework.utils.WikipediaApiInterface;
import it.unipi.di.acube.BingInterface;
import org.bytedeco.javacpp.presets.opencv_core;
import org.bytedeco.javacv.CanvasFrame;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;

import org.apache.commons.lang.StringUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.*;

import org.jsoup.nodes.*;
import org.jsoup.select.Elements;


/* Each static function in this class expects the first element of the results array of a bing query as input. E.g:

*      JSONObject a = bing.queryBing("query");
*      JSONObject q = a.getJSONObject("d").getJSONArray("results").getJSONObject(0);
*
*  Some functions require an entity to be given.
*/
public class SMAPHFeatures {

    // Hyperparameters
    private static int searchCount = 100;
    private static double rho = 0.2f;

    // Cached set values
    private static List<EntityMentionPair> A;
    private static List<Pair<String, String>> X;
    private static List<Double> P;
    private static List<Integer> LatinA;
    private static List<Double> C;
    private static List<Double> L;

    private static String oldSnippet_A = "";

    private static String oldQuery_X = "";

    private static String oldQuery_P = "";
    private static String oldEntity_P = "";

    private static String oldQuery_LA = "";
    private static String oldEntity_LA = "";

    private static String oldQuery_C = "";
    private static String oldEntity_C = "";

    private static String oldQuery_L = "";


    /*
    *******************************
    *                             *
    *  Candidate entity features  *
    *                             *
    *******************************
    */

    public static int webTotal(JSONObject q) {
        int total = 0;
        try {
            total = q.getInt("WebTotal");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return total;
    }

    public static boolean isNE(JSONObject q) {
        notImplemented();
        return false;
    }

    // Return the position of the wikipedia page of e in the search results. Returns Integer.MAX_VALUE if not found.
    public static int rank(JSONObject q, String e) {
        int rank = 0;
        try {
            rank = getWikiRank(q, e);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return rank;
    }


    // Returns minED for query q and the wikipedia title of entity e.
    public static double EDTitle(WikipediaApiInterface wikiApi, JSONObject q, String e) {
        String wikiTitle = "";
        double edTitle = Integer.MAX_VALUE;
        try {
            int ID = wikiApi.getIdByTitle(e);
            if (ID != -1) {
                wikiTitle = wikiApi.getTitlebyId(ID);
                edTitle = minED(wikiTitle, getQuery(q, true));
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }


        return edTitle;
    }

    // Returns minED for query q and the wikipedia title of entity e with possible parenthesis removed.
    public static double EDTitNP(WikipediaApiInterface wikiApi, JSONObject q, String e) {
        String wikiTitle;
        double edTitNP = Integer.MAX_VALUE;

        try {
            int ID = wikiApi.getIdByTitle(e);
            if (ID != -1) {
                wikiTitle = wikiApi.getTitlebyId(ID);
                // Remove the final parenthetical-string at the end of the wiki title (if present).
                wikiTitle.replaceFirst("(.*)$", "");
                edTitNP = minED(wikiTitle, getQuery(q, true));
            }

        } catch (Exception e1) {
            e1.printStackTrace();
        }


        return edTitNP;
    }

    public static double word2vec_sim(WordVectors vec, String mention, String entity) {
    	try {
    		return vec.similarity(mention.replace(" ", "_"), entity);
    	} catch (Exception e1) {
    		e1.printStackTrace();
    		return 0;
    	}
    }

    public static double minEDBolds(JSONObject q) {
        List<ArrayList<String>> boldWords = getBoldWords(q);
        double minEDBolds = Integer.MAX_VALUE;
        String query = getQuery(q, true);

        List<String> boldSentences = new ArrayList<>();
        for (ArrayList<String> list : boldWords) {
            String curr = "";
            for (String w : list) {
                curr += w + " ";
            }
            boldSentences.add(curr);
        }

        for (String bw : boldSentences) {
            double curr = minED(bw, query);
            if (minEDBolds > curr)
                minEDBolds = curr;
        }

        return minEDBolds;
    }

    // Returns the number of capitalised strings in B(q)
    public static int captBolds(JSONObject q) {
        List<ArrayList<String>> boldWords = getBoldWords(q);
        int nrCapitalised = 0;

        for (ArrayList<String> list : boldWords) {
            for (String bw : list) {
                if (Character.isUpperCase(bw.charAt(0)))
                    nrCapitalised++;
            }
        }

        return nrCapitalised;
    }

    // Returns the average number of bold terms
    public static double boldTerms(JSONObject q) {
        List<ArrayList<String>> boldWords = getBoldWords(q);

        double avgLength = 0.0;
        for (ArrayList<String> bw : boldWords) {
            avgLength += bw.size();
        }
        avgLength /= boldWords.size();

        return avgLength;
    }

    // Returns the frequency of snippets have mention linked to entity e
    public static double freq(JSONObject q, String e) {
        List<String> C = getDescriptions(q);

        double freq1 = 0.0;
        for (String s : C) {
            List<EntityMentionPair> A = getSetA(s);
            for (EntityMentionPair emp : A)
                if (emp.getWikiTitle().equals(e))
                    freq1++;

        }

        return freq1 / C.size();
    }

    public static double avgRank(JSONObject q, String e) {
        List<String> C = getDescriptions(q);

        double p = 0.0;
        for (int i = 0; i < 25 && i < C.size(); i++) {
            boolean found = false;
            String s = C.get(i);
            List<EntityMentionPair> A = getSetA(s);
            for (EntityMentionPair emp : A) {
                if (emp.getWikiTitle().equals(e)) {
                    p += i;
                    found = true;
                    break;
                }

            }

            if (!found)
                p += 25;
        }

        return p / 25.0;
    }

    public static double rhoMin(JSONObject q, String e) {
        double min = 0.0;
        List<Double> P = getSetP(q, e);

        if (P.isEmpty())
            return 0;

        try {
            min = Collections.min(P);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return min;
    }

    public static double rhoMax(JSONObject q, String e) {
        List<Double> P = getSetP(q, e);

        if (P.isEmpty())
            return 0;

        return Collections.max(P);
    }

    public static double rhoAvg(JSONObject q, String e) {
        List<Double> P = getSetP(q, e);

        if (P.isEmpty())
            return 0;

        double avg = 0.0;
        for (Double d : P)
            avg += d;

        avg /= P.size();

        return avg;
    }

    // TODO: Verify
    public static double ambigMin(WikipediaApiInterface wikiApi, JSONObject q, String e) {
        List<Integer> latinA = getSetLatinA(q, e);

        if (latinA.isEmpty())
            return 0;

        return (double) Collections.min(latinA);
    }

    // TODO: Verify
    public static double ambigMax(WikipediaApiInterface wikiApi, JSONObject q, String e) {
        List<Integer> latinA = getSetLatinA(q, e);

        if (latinA.isEmpty())
            return 0;

        return (double) Collections.max(latinA);
    }

    // TODO: Verify
    public static double ambigAvg(WikipediaApiInterface wikiApi, JSONObject q, String e) {
        List<Integer> latinA = getSetLatinA(q, e);

        if (latinA.isEmpty())
            return 0;

        double avg = 0.0;
        for (Integer d : latinA)
            avg += (double) d;

        avg /= latinA.size();

        return avg;
    }

    // TODO: Verify
    public static double commMin(WikipediaApiInterface wikiApi, JSONObject q, String e) {
        List<Double> setC = getSetC(wikiApi, q, e);

        if (setC.isEmpty())
            return 0;

        return Collections.min(setC);
    }

    // TODO: Verify
    public static double commMax(WikipediaApiInterface wikiApi, JSONObject q, String e) {
        List<Double> setC = getSetC(wikiApi, q, e);

        if (setC.isEmpty())
            return 0;

        return Collections.max(setC);
    }

    // TODO: Verify
    public static double commAvg(WikipediaApiInterface wikiApi, JSONObject q, String e) {
        List<Double> setC = getSetC(wikiApi, q, e);

        if (setC.isEmpty())
            return 0;

        double avg = 0.0;
        for (Double d : setC)
            avg += d;

        avg /= setC.size();

        return avg;
    }

    // TODO: Verify
    public static double lpMin(JSONObject q) {
        List<Double> setL = getSetL(q);

        if (setL.isEmpty())
            return 0;

        return Collections.min(setL);
    }

    // TODO: Verify
    public static double lpMax(JSONObject q) {
        List<Double> setL = getSetL(q);

        if (setL.isEmpty())
            return 0;

        return Collections.max(setL);
    }

    // TODO: Verify
    public static double mentMEDMin(JSONObject q) {
        List<Pair<String, String>> setX = getSetX(q);
        double min_val = Integer.MAX_VALUE;

        // Corrected?
        String query = getQuery(q, true);

        for (Pair<String, String> pair : setX) {
            String mention = pair.first;

            double min_ed_val = minED(mention, query);

            min_val = (min_ed_val < min_val) ? min_ed_val : min_val;
        }


        return min_val;
    }

    // TODO: Verify
    public static double mentMEDMax(JSONObject q) {
        List<Pair<String, String>> setX = getSetX(q);
        double max_val = 0.0d;

        // Corrected?
        String query = getQuery(q, true);

        for (Pair<String, String> pair : setX) {
            String mention = pair.first;

            double min_ed_val = minED(mention, query);

            max_val = (min_ed_val > max_val) ? min_ed_val : max_val;
        }


        return max_val;
    }
    /*
    *****************************************
    *                                       *
    *    Candidate annotation features      *
    *                                       *
    *****************************************
    */

    // Returns some important feature
    public static double anchorsAvgED(WikipediaApiInterface wikiApi, String e, String m) {

        double avgED;
        double sum1 = 1;
        double sum2 = -1;

        try {
            List<String> G = anchorSetG(wikiApi, e);
            for (String g : G) {
                double f = Math.sqrt(freqAnchors(wikiApi, e, g));
                sum1 += f;
                sum2 += f * editDistance(e, m);
            }
        } catch (Exception e1) {
            e1.printStackTrace();
            return Double.MIN_VALUE;
        }

        avgED = sum2 / sum1;

        return avgED;
    }

    public static double minEdTitle(WikipediaApiInterface wikiApi, String e, String m) {
        double minEDTit = -1;

        int ID;
        try {
            ID = wikiApi.getIdByTitle(e);
            if (ID != -1) {
                String wikiTitle = wikiApi.getTitlebyId(ID);
                minEDTit = minED(wikiTitle, m);
            }
        }

        catch (IOException e1) {
            e1.printStackTrace();
        }

        return minEDTit;
    }

    public static double EdTitle(WikipediaApiInterface wikiApi, String e, String m) {
        double edTit = -1;

        int ID;
        try {
            ID = wikiApi.getIdByTitle(e);
            if (ID != -1) {
                String wikiTitle = wikiApi.getTitlebyId(ID);
                edTit = editDistance(wikiTitle, m);
            }
        }

        catch (IOException e1) {
            e1.printStackTrace();
        }

        return edTit;
    }

    public static double commonness(WikipediaApiInterface wikiApi, String e, String m) {
        double comm = -1;

        int ID = 0;
        try {
            ID = wikiApi.getIdByTitle(e);
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        WATRelatednessComputer.getCommonness(m, ID);

        return comm;
    }

    public static double lp(String m) {
        double lp = -1;

        WATRelatednessComputer.getLp(m);

        return lp;
    }

    /*
    ************************
    *                      *
    *  Helper functions    *
    *                      *
    ************************
    */


    // Returns a list of anchors used in Wikipedia to link e
    // Assuming e is a valid wikipedia title.
    private static List<String> anchorSetG(WikipediaApiInterface wikiApi, String e) throws IOException {
        List<String> setG = new ArrayList<>();

        EntityToAnchors e2a = EntityToAnchors.e2a();

        int ID = wikiApi.getIdByTitle(e);

        for (Pair<String, Integer> anchorAndFreq: e2a.getAnchors(ID))
            setG.add(anchorAndFreq.first);

        return setG;
    }

    // Returns the number of times that entity e has been linked in Wiki by anchor a
    // Assuming e is a valid wikipedia title.
    private static int freqAnchors(WikipediaApiInterface wikiApi, String e, String a) throws IOException {
        int freq = 0;

        EntityToAnchors e2a = EntityToAnchors.e2a();

        int ID = wikiApi.getIdByTitle(e);

        for (Pair<String, Integer> anchorAndFreq: e2a.getAnchors(ID))
            if (anchorAndFreq.first.equals(a))
                freq = anchorAndFreq.second;

        return freq;
    }

    // Returns a list of words which are bold in the query.
    private static List<ArrayList<String>> getBoldWords(JSONObject q) {
        JSONArray searchResults;
        ArrayList<ArrayList<String>> boldWords = new ArrayList<ArrayList<String>>();

        try {
            searchResults = q.getJSONArray("Web");
            // Go through all search results to get the rank of the entity e
            for (int i = 0; i < searchResults.length(); i++) {
                boldWords.add(new ArrayList<String>());
                String currTitle = searchResults.getJSONObject(i).getString("Description");

                Matcher m = Pattern.compile("\uE000\\w+\uE001").matcher(currTitle);
                while (m.find()) {
                    String currBold = m.group();
                    // Remove the bold indicators
                    currBold = currBold.substring(1, currBold.length() - 1);
                    boldWords.get(i).add(currBold);
                }
            }
        } catch (JSONException e1) {
            e1.printStackTrace();
        }

        return boldWords;
    }

    // Calculates the asymmetric measure of the average distance of all terms in a to its closest term in b
    private static double minED(String a, String b) {
        double minED = 0.0;
        String[] wordsA = a.split(" ");
        String[] wordsB = b.split(" ");

        for (String wA : wordsA) {
            int minDist = Integer.MAX_VALUE;
            // Find the term in string b which has the closest edit distance to wA.
            for (String wB : wordsB) {
                int currDist = editDistance(wA, wB);
                if (minDist > currDist)
                    minDist = currDist;
            }

            minED += minDist;
        }

        minED /= wordsA.length;

        return minED;
    }

    // Calculates the edit distance of the strings s1 and s2 with dynamic programming.
    private static int editDistance(String s1, String s2) {
        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();
        int len1 = s1.length();
        int len2 = s2.length();

        // len1+1, len2+1, because finally return dp[len1][len2]
        int[][] dp = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }

        // Iterate though, and check last char
        for (int i = 0; i < len1; i++) {
            char c1 = s1.charAt(i);
            for (int j = 0; j < len2; j++) {
                char c2 = s2.charAt(j);

                // If last two chars equal
                if (c1 == c2) {
                    // Update dp value for +1 length
                    dp[i + 1][j + 1] = dp[i][j];
                } else {
                    int replace = dp[i][j] + 1;
                    int insert = dp[i][j + 1] + 1;
                    int delete = dp[i + 1][j] + 1;

                    int min = replace > insert ? insert : replace;
                    min = delete > min ? min : delete;
                    dp[i + 1][j + 1] = min;
                }
            }
        }

        return dp[len1][len2];
    }

    // Returns the rank of the wikipedia url in the search result
    private static int getWikiRank(JSONObject q, String e) {
        String qs = getQuery(q, true);
        String url = "http://www.bing.com/search?q=" + qs.replace(" ", "+") + "&count=" + searchCount;
        int rank = Integer.MAX_VALUE;


        try {

            Connection.Response response = null;
            while (true) {
                try {
                    response = Jsoup.connect(url).method(Connection.Method.GET).execute();

                    if (response.statusCode() == 200)
                        break;

                } catch (IOException ex) {
                    System.err.println("Wiki HTTP Retry..");
                    Thread.sleep(300);
                }
            }


            Document doc = response.parse();
            Elements searchResults = doc.getElementById("b_results").getElementsByClass("b_algo");
            int i = 0;
            for (Element res : searchResults) {
                String currTitle = res.getElementsByTag("a").first().text();
                if (currTitle.matches("^\uE000?" + e + "\uE001? - \uE000?Wikipedia\uE001?.*")) {
                    rank = i;
                    break;
                }
                i++;
            }

        } catch (Exception exept) {
            exept.printStackTrace();
        }



        return rank;
    }


//    // Returns the rank of the wikipedia url in the search result
//    private static int getWikiRank(JSONObject q, String e) {
//        String qs = getQuery(q, true);
//        String url = "http://www.bing.com/search?q=" + qs.replace(" ", "+") + "&count=" + searchCount;
//        int rank = Integer.MAX_VALUE;
//
//        try {
//            Document doc = Jsoup.connect(url).get();
//            Elements searchResults = doc.getElementById("b_results").getElementsByClass("b_algo");
//            int i = 0;
//            for (Element res : searchResults) {
//                String currTitle = res.getElementsByTag("a").first().text();
//                if (currTitle.matches("^\uE000?" + e + "\uE001? - \uE000?Wikipedia\uE001?.*")) {
//                    rank = i;
//                    break;
//                }
//                i++;
//            }
//
//
//
//        } catch (IOException e1) {
//            e1.printStackTrace();
//        }
//
//
//
//        return rank;
//    }

    // Returns the query used in q.
    private static String getQuery(JSONObject q, boolean corrected) {
        String query = "";

        try {
            String uri = q.getJSONObject("__metadata").getString("uri");
            Matcher m = Pattern.compile("Query='[^&]*'").matcher(uri);
            if (m.find()) {
                query = m.group();
                query = query.substring(7, query.length() - 1);

                if (corrected) {
                    String currQ = q.getString("AlteredQuery");
                    // Remove the special characters.
                    currQ = currQ.replaceAll("(\uE000)|(\uE001)", "");
                    // If there is a correction available, use it.
                    if (!currQ.isEmpty())
                        query = currQ;
                }

            }


        } catch (JSONException e) {
            e.printStackTrace();
        }

        return query;
    }

    private static List<String> getDescriptions(JSONObject q) {
        List<String> descs = new ArrayList<String>();

        try {

            JSONArray webRes = q.getJSONArray("Web");
            for (int i = 0;i < webRes.length();i++) {
                String currDesc = webRes.getJSONObject(i).getString("Description");
                currDesc = currDesc.replaceAll("<a href=\".*\">", "");
                currDesc = currDesc.replaceAll("</a>", "");
                descs.add(currDesc);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }


        return descs;
    }

    // Verified
    private static List<EntityMentionPair> getSetA(String s) {
        // Check if we have a cached set available
        if (s.equals(oldSnippet_A))
            return A;


        List<EntityMentionPair> setA = new ArrayList<EntityMentionPair>();
        TagMeAnnotator tag_me = TagMeAnnotator.getInstance();
        List<String> boldWords = new ArrayList<>();


        // Find all the bold words in the description
        Matcher m = Pattern.compile("\uE000\\w+\uE001").matcher(s);
        while (m.find()) {
            String currBold = m.group();
            // Remove the bold indicators
            currBold = currBold.substring(1, currBold.length() - 1);
            boldWords.add(currBold);
        }

        /*List<EntityMentionPair> entities = new ArrayList<>();
        try {
            // Get all mention-entity pairs from the TAGME annotator
            entities = tag_me.getFilteredEntities(s, rho);
        } catch (Exception e) {
            System.err.println(e);
        }*/

        List<EntityMentionPair> entities = tag_me.getFilteredEntities(s, rho);



        // Check if any mentions overlap with the boldwords
        for (String bw : boldWords)
            for (EntityMentionPair emp : entities) {
                String currM = emp.getMention();
                if (isOverlap(bw, currM))
                    setA.add(emp);
            }

        // Cache the set.
        A = setA;
        oldSnippet_A = s;

        return setA;
    }

    // Returns the set X. First element of the pair is the mention, the second the snippet.
    // Verified. May contain pairs which are identitical
    private static List<Pair<String, String>> getSetX(JSONObject q) {
        // Check if we have a cached set available
        String query = getQuery(q, false);
        if (query.equals(oldQuery_X))
            return X;


        List<Pair<String, String>> setX = new ArrayList<Pair<String, String>>();
        List<String> descs = getDescriptions(q);

        for (String s : descs) {
            List<EntityMentionPair> A = getSetA(s);
            List<String> mentions = new ArrayList<>();
            for (EntityMentionPair e : A) {
                String currM = e.getMention();
                if (mentions.contains(currM))
                    continue;
                mentions.add(currM);
                setX.add(new Pair<String, String>(e.getMention(), s));
            }

        }

        // Cache the set and remember the old value
        X = setX;
        oldQuery_X = query;

        return setX;
    }

    // Verified
    private static List<Double> getSetP(JSONObject q, String e) {
        // Check if we have a cached set available
        String query = getQuery(q, false);
        if (query.equals(oldQuery_P) && e.equals(oldEntity_P))
            return P;


        List<Double> setP = new ArrayList<>();
        List<String> C = getDescriptions(q);

        List<Pair<Pair<String,String>, String>> seenEMPs = new ArrayList<>();
        for (String s : C) {
            List<EntityMentionPair> A = getSetA(s);
            for (EntityMentionPair emp : A) {
                if (!Seen(seenEMPs, emp, s)) {
                    seenEMPs.add(new Pair<>(new Pair<>(emp.getMention(), emp.getWikiTitle()), s));
                    if (emp.getWikiTitle().equals(e))
                        setP.add(emp.getRho());
                    else
                        setP.add(0.0);
                }
            }
        }

        // Cache the set and remember the old values
        P = setP;
        oldQuery_P = query;
        oldEntity_P = e;


        return setP;
    }

    // Returns true if the EntityMentionPair curr_emp is contained in list
    private static boolean Seen(List<Pair<Pair<String,String>, String>> list, EntityMentionPair curr_emp, String s) {

        for (Pair<Pair<String,String>, String> emp : list) {
            if(emp.first.first.equals(curr_emp.getMention()) && emp.first.second.equals(curr_emp.getWikiTitle()) && emp.second.equals(s))
                return true;
        }

        return false;
    }

    // Verified
    private static List<Integer> getSetLatinA(JSONObject q, String e) {
        // Check if we have a cached set available
        String query = getQuery(q, false);
        if (query.equals(oldQuery_LA) && e.equals(oldEntity_LA))
            return LatinA;


        List<Integer> setLatinA = new ArrayList<>();

        List<Pair<String, String>> X = getSetX(q);

        for (Pair<String, String> pair : X) {
            String mention = pair.first;

            int[] links_id = WATRelatednessComputer.getLinks(mention);

            setLatinA.add(links_id.length);
        }

        // Cache the set and remember the parameters
        LatinA = setLatinA;
        oldQuery_LA = query;
        oldEntity_LA = e;

        return setLatinA;
    }

    // Verified
    private static List<Double> getSetC(WikipediaApiInterface wikiApi, JSONObject q, String e) {
        // Check if we have a cached set available
        String query = getQuery(q, false);
        if (query.equals(oldQuery_C) && e.equals(oldEntity_C))
            return C;


        List<Double> setC = new ArrayList<>();

        List<Pair<String, String>> X = getSetX(q);

        for (Pair<String, String> pair : X) {
            String mention = pair.first;

            double commonness = 0.0d;
            try {
                commonness = WATRelatednessComputer.getCommonness(mention, wikiApi.getIdByTitle(e));
                setC.add(commonness);
            } catch (IOException e1) {
                System.err.println("Unable to fetch id from wikiApi");
            }
        }

        // Cache the set and remember the parameters
        C = setC;
        oldQuery_C = query;
        oldEntity_C = e;

        return setC;
    }
    


    // Verified
    private static List<Double> getSetL(JSONObject q) {
        // Check if we have a cached set available
        String query = getQuery(q, false);
        if (query.equals(oldQuery_L))
            return L;

        List<Double> setL = new ArrayList<>();

        List<Pair<String, String>> X = getSetX(q);

        for (Pair<String, String> pair : X) {
            String mention = pair.first;

            double link_prob = WATRelatednessComputer.getLp(mention);
            setL.add(link_prob);
        }

        // Cache the set and remember the parameters
        L = setL;
        oldQuery_L = query;

        return setL;
    }

    private static void notImplemented() {
        throw new RuntimeException("Not implemented");
    }

    // TESTER FUNCTION. DO NOT USE.
    // Returns a list of words which are bold in the query.
    private static List<String> getBoldWords(String currTitle) {
        JSONArray searchResults;
        ArrayList<String> boldWords = new ArrayList<String>();


        Matcher m = Pattern.compile("\uE000\\w+\uE001").matcher(currTitle);
        while (m.find()) {
            String currBold = m.group();
            // Remove the bold indicators
            currBold = currBold.substring(1, currBold.length() - 1);
            boldWords.add(currBold);
        }

        return boldWords;
    }

    // Verified
    private static boolean isOverlap(String s1, String s2) {
        String s3 = s1;

        // Put the longer string into s1.
        s1 = s1.length() > s2.length() ? s1 : s2;
        s2 = s1.equals(s2) ? s3 : s2;


        List<String> s2Arr = Arrays.asList(s2.toLowerCase().split(" "));
        s1 = s1.toLowerCase();

        for (int i = 0; i < s2Arr.size(); i++) {

            String subWord = StringUtils.join(s2Arr.subList(0,i+1)," ");

            if (i+1 == 1) {
                if (s1.equals(subWord))
                    return true;
            }
            else {
                if (s1.contains(subWord))
                    return true;
            }
        }

        for (int i = 0; i < s2Arr.size(); i++) {
            String subWord = StringUtils.join(s2Arr.subList(i, s2Arr.size())," ");

            if (s2Arr.size() - i == 1) {
                if (s1.equals(subWord))
                    return true;
            }
            else {
                if (s1.contains(subWord))
                    return true;
            }
        }

        return false;
    }

    // testPrivateFunctions tests all the private functions of this class. Useful to check for bugs
    private static void testPrivateFunctions() {
        String word1 = "hellp";
        String word2 = "hello";
        String word3 = "how";
        String word4 = "are";
        String word5 = "you";

        String s1 = "hello how are you";
        String s2 = "hellp";
        String b1 = "Cat - Wikipedia, the free encyclopedia";
        String b2 = "Funny Cat Videos - Battle Cats Wiki - Wikia";
        String b3 = "Cat Compilation - Top 2013 - Funny Moments with Cats - YouTube";

        // Edit Distance test.
        System.out.println(editDistance(word1, word2));
        System.out.println(editDistance(word1, word3));
        System.out.println(editDistance(word1, word4));
        System.out.println(editDistance(word1, word5));
        System.out.println(editDistance(word1, word1));

        // MinED test
        System.out.println(minED(s1, s2));
        System.out.println(minED(s2, s1));
        System.out.println(minED(s1, s1));

        // Bold words test
        List<String> boldWords = getBoldWords(b1);
        for (String b : boldWords)
            System.out.print(b + ", ");
        System.out.println();

        boldWords = getBoldWords(b2);
        for (String b : boldWords)
            System.out.print(b + ", ");
        System.out.println();

        boldWords = getBoldWords(b3);
        for (String b : boldWords)
            System.out.print(b + ", ");
        System.out.println();

    }


    private static void testPrivateFunctionsE3() throws Exception {
 /*       String s1 = "Look at that motherfucker in his red car";
        String s2 = "his red car for pussies";
        String s3 = "that motherfucker";
        String s4 = "Hey look at that";
        String s5 = "Completely irrelevant string";
        System.out.println(isOverlap(s1, s2));
        System.out.println(isOverlap(s1, s3));
        System.out.println(isOverlap(s1, s4));
        System.out.println(isOverlap(s1, s5));

        System.out.println(isOverlap(s2, s1));
        System.out.println(isOverlap(s3, s1));
        System.out.println(isOverlap(s4, s1));
        System.out.println(isOverlap(s5, s1));

        System.out.println(isOverlap(s1, s1));*/

        /*BingInterface bing = new BingInterface("XsdC/uY+ssHhsatEvIC2xQiUD1gs4GGazQZI0wWO2bY");*/
        WikipediaApiInterface wikiApi = WikipediaApiInterface.api();
        JSONObject q = BingSearchMain.getQueryResults("funy kittens wikipedia");
        JSONObject q2 = BingSearchMain.getQueryResults("Kitten wikipedia");
        System.out.println(q.toString(4));
        String e = "Cat";
        String e2 = "Kitten";

        long startTime;
        List<String> descs = getDescriptions(q);
        String currD = descs.get(0);

        startTime = System.currentTimeMillis();
        List<EntityMentionPair> A = getSetA(currD);
        List<Pair<String, String>> X = getSetX(q);
        List<Double> P = getSetP(q, e);
        List<Integer> AL = getSetLatinA(q, e);
        List<Double> C = getSetC(wikiApi, q, e);
        List<Double> L = getSetL(q);
        double noCache = (System.currentTimeMillis() - startTime) / 1000.0;


        startTime = System.currentTimeMillis();
        List<EntityMentionPair> A2 = getSetA(currD);
        List<Pair<String, String>> X2 = getSetX(q);
        List<Double> P2 = getSetP(q, e);
        List<Integer> AL2 = getSetLatinA(q, e);
        List<Double> C2 = getSetC(wikiApi, q, e);
        List<Double> L2 = getSetL(q);
        double withCache = (System.currentTimeMillis() - startTime) / 1000.0;

        String currD2 = getDescriptions(q2).get(0);
        startTime = System.currentTimeMillis();
        List<EntityMentionPair> A3 = getSetA(currD2);
        List<Pair<String, String>> X3 = getSetX(q2);
        List<Double> P3 = getSetP(q, e2);
        List<Integer> AL3 = getSetLatinA(q, e2);
        List<Double> C3 = getSetC(wikiApi, q, e2);
        List<Double> L3 = getSetL(q);
        double noCache2 = (System.currentTimeMillis() - startTime) / 1000.0;

        System.out.println("No caching: " + noCache + " s");
        System.out.println("With caching: " + withCache + " s");
        System.out.println("No cache, new query: " + noCache2 + " s");
        System.out.println("Break here");

    }

    public static void main(String[] args) {
        try {
            testPrivateFunctionsE3();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}


