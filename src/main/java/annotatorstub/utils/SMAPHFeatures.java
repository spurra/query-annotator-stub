package annotatorstub.utils;
/**
 * Created by Adrian on 29/04/16.
 */

import it.unipi.di.acube.batframework.utils.WikipediaApiInterface;
import it.unipi.di.acube.BingInterface;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



/* Each static function in this class expects the first element of the results array of a bing query as input. E.g:

*      JSONObject a = bing.queryBing("query");
*      JSONObject q = a.getJSONObject("d").getJSONArray("results").getJSONObject(0);
*
*  Some functions require an entity to be given.
*/
public class SMAPHFeatures {


    /*
    ************************
    *                      *
    *  E1 and E2 features  *
    *                      *
    ************************
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
        //TODO
        notImplemented();
        return false;
    }

    // Return the position of the wikipedia page of e in the search results. Returns Integer.MAX_VALUE if not found.
    public static int rank(JSONObject q, String e) {

        int rank = getWikiRank(q, e);

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


    public static double minEDBolds(JSONObject q) {
        List<String> boldWords = getBoldWords(q);
        double minEDBolds = Integer.MAX_VALUE;
        String query = getQuery(q, true);
        for (String bw : boldWords) {
            double curr = minED(bw, query);
            if (minEDBolds > curr)
                minEDBolds = curr;
        }

        return minEDBolds;
    }

    // Returns the number of capitalised strings in B(q)
    public static int captBolds(JSONObject q) {
        List<String> boldWords = getBoldWords(q);
        int nrCapitalised = 0;

        for (String bw : boldWords) {
            if (Character.isUpperCase(bw.charAt(0)))
                nrCapitalised++;
        }

        return nrCapitalised;
    }

    // Returns the average length of the bold terms
    public static double boldTerms(JSONObject q) {
        List<String> boldWords = getBoldWords(q);

        double avgLength = 0.0;
        for (String bw : boldWords) {
            avgLength += bw.length();
        }
        avgLength /= boldWords.size();

        return avgLength;
    }

    /*
    ***************************
    *                         *
    *  Annotiation features   *
    *                         *
    ***************************
    */

    public static double anchorsAvgED(String e, String m) {
        double avgED = 0.0;
        double sum1 = 0.0;
        double sum2 = 0.0;


        List<String> G = anchorSetG(e);
        for (String g : G) {
            double f = Math.sqrt(freq(e,g));
            sum1 += f;
            sum2 += f * editDistance(e, m);
        }

        avgED = sum2 / sum1;

        return avgED;
    }

    public static double minEdTitle(WikipediaApiInterface wikiApi, String e, String m) {
        double minEDTit = 0.0;

        int ID;
        try {
            ID = wikiApi.getIdByTitle(e);
            if (ID != -1) {
                String wikiTitle = wikiApi.getTitlebyId(ID);
                minEDTit = minED(wikiTitle, e);
            }
        }

        catch (IOException e1) {
            e1.printStackTrace();
        }

        return minEDTit;
    }

    public static double EdTitle(WikipediaApiInterface wikiApi, String e, String m) {
        double edTit = 0.0;

        int ID;
        try {
            ID = wikiApi.getIdByTitle(e);
            if (ID != -1) {
                String wikiTitle = wikiApi.getTitlebyId(ID);
                edTit = editDistance(wikiTitle, e);
            }
        }

        catch (IOException e1) {
            e1.printStackTrace();
        }

        return edTit;
    }

    public static double commonness(WikipediaApiInterface wikiApi, String e, String m) {
        double comm = 0.0;

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
        double lp = 0.0;

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
    private static List<String> anchorSetG(String e) {
        ArrayList<String> setG = new ArrayList<>();

        return setG;
    }

    private static int freq(String e, String a) {
        int freq = 0;


        return freq;
    }

    // Returns a list of words which are bold in the query.
    private static List<String> getBoldWords(JSONObject q) {
        JSONArray searchResults;
        ArrayList<String> boldWords = new ArrayList<String>();

        try {
            searchResults = q.getJSONArray("Web");
            // Go through all search results to get the rank of the entity e
            for (int i = 0; i < searchResults.length(); i++) {
                String currTitle = searchResults.getJSONObject(i).getString("Description");

                Matcher m = Pattern.compile("\uE000\\w+\uE001").matcher(currTitle);
                while (m.find()) {
                    String currBold = m.group();
                    // Remove the bold indicators
                    currBold = currBold.substring(1, currBold.length() - 1);
                    boldWords.add(currBold);
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
        int rank = Integer.MAX_VALUE;
        try {
            JSONArray searchResults = q.getJSONArray("Web");
            // Go through all search results to get the rank of the entity e
            for (int i = 0; i < searchResults.length(); i++) {
                String currTitle = searchResults.getJSONObject(i).getString("Title");
                // Check if e's wikipedia page is the current search result
                if (currTitle.matches("^\uE000?" + e + "\uE001? - \uE000?Wikipedia\uE001?.*")) {
                    rank = i;
                    break;
                }
            }
        } catch (JSONException e1) {
            e1.printStackTrace();
        }

        return rank;
    }

    // Returns the query used in q.
    private static String getQuery(JSONObject q, boolean corrected) {
        String query = "";

        try {
            String uri = q.getJSONObject("__metadata").getString("uri");
            Matcher m = Pattern.compile("Query='[\\w\\s]*'").matcher(uri);
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

    // testPrivateFunctions tests all the private functions of this class. Useful to check for bugs
    public static void testPrivateFunctions() {
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
    public static void main(String[] args) {
    	testPrivateFunctions();
    }

}


