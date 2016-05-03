package annotatorstub.utils;
/**
 * Created by Adrian on 29/04/16.
 */

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

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

    public static int rank(JSONObject q, String e) {

        int rank = getWikiRank(q, e);

        return rank;
    }

    // Returns minED for query q and the wikipedia title of entity e.
    public static double EDTitle(JSONObject q, String e) {
        String wikiTitle = "";
        double edTitle = -1;

        try {
            int rank = getWikiRank(q, e);
            wikiTitle = q.getJSONArray("Web").getJSONObject(rank).getString("Title");
            edTitle = minED(wikiTitle, getQuery(q, true));
        } catch (JSONException e1) {
            e1.printStackTrace();
        }


        return edTitle;
    }

    // Returns minED for query q and the wikipedia title of entity e with possible paranthetis removed.
    public static double EDTitNP(JSONObject q, String e) {
        String wikiTitle = "";
        double edTitNP = -1;

        try {
            int rank = getWikiRank(q, e);
            wikiTitle = q.getJSONArray("Web").getJSONObject(rank).getString("Title");
            // Remove the final parenthetical-string at the end of the wiki title (if present).
            wikiTitle.replaceFirst("(.*)$", "");
            edTitNP = minED(wikiTitle, getQuery(q, true));
        } catch (JSONException e1) {
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
        Helper functions
    */

    private static void notImplemented() {
        throw new RuntimeException("Not implemented");
    }

    //TODO remove bold markings
    // Returns a list of words which are bold in the query.
    private static List<String> getBoldWords(JSONObject q) {
        JSONArray searchResults;
        int rank = -1;
        ArrayList<String> boldWords = new ArrayList<String>();

        try {
            searchResults = q.getJSONArray("Web");
            // Go through all search results to get the rank of the entity e
            for (int i = 0; i < searchResults.length(); i++) {
                String currTitle = searchResults.getJSONObject(i).getString("Description");
                /* TODO
                * possibly greedy search may cause issues: \"cat\" \"cat\"
                * Will match the entire string and not each seperate word.
                * Bold letters seem to be enclosed with  symbols.
                */
                Matcher m = Pattern.compile("\uE000.*\uE001").matcher(currTitle);
                while (m.find())
                    boldWords.add(m.group(1));
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

        //iterate though, and check last char
        for (int i = 0; i < len1; i++) {
            char c1 = s1.charAt(i);
            for (int j = 0; j < len2; j++) {
                char c2 = s2.charAt(j);

                //if last two chars equal
                if (c1 == c2) {
                    //update dp value for +1 length
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

    private static int getWikiRank(JSONObject q, String e) {
        int rank = -1;
        try {
            JSONArray searchResults = q.getJSONArray("Web");
            // Go through all search results to get the rank of the entity e
            for (int i = 0; i < searchResults.length(); i++) {
                String currTitle = searchResults.getJSONObject(i).getString("Title");
                // Check if e's wikipedia page is the current search result
                if (currTitle.matches(".*\\b" + e + "\\b.*") && currTitle.matches(".*\\bWikipedia\\b.*")) {
                    rank = i;
                    break;
                }
            }
        } catch (JSONException e1) {
            e1.printStackTrace();
        }

        return rank;
    }

    private static String getQuery(JSONObject q, boolean corrected) {
        String query = "";

        try {
            if (corrected)
                query = q.getString("AlteredQuery");
            else
                query = q.getString("AlterationOverrideQuery");

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return query;
    }
}


