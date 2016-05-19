package annotatorstub.annotator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import annotatorstub.main.BingSearchMain;
import annotatorstub.utils.WATRelatednessComputer;
import it.unipi.di.acube.batframework.data.Annotation;
import it.unipi.di.acube.batframework.data.Mention;
import it.unipi.di.acube.batframework.data.ScoredAnnotation;
import it.unipi.di.acube.batframework.data.ScoredTag;
import it.unipi.di.acube.batframework.data.Tag;
import it.unipi.di.acube.batframework.problems.Sa2WSystem;
import it.unipi.di.acube.batframework.utils.AnnotationException;
import it.unipi.di.acube.batframework.utils.ProblemReduction;
import it.unipi.di.acube.batframework.utils.WikipediaApiInterface;
import it.unipi.di.acube.batframework.problems.A2WDataset;


public class FancyFakeAnnotator implements Sa2WSystem {
	/**
	 * Generates a large set of (entity,mention) candidates and then does a greedy selection
	 * using anchorsAvgED as the scoring method 
	 * so it is essentially a fancy (semi-) fake annotator :)
	 */	
	private static long lastTime = -1;
	private static float threshold = -1f;
	private static final int MAX_LINKS = 500;

	private static HashMap<String, List<Integer>> mentionIdMap;

	private WikipediaApiInterface wikiApi;

	public FancyFakeAnnotator (WikipediaApiInterface wikiApi) {
		this.wikiApi = wikiApi;
	}

	public long getLastAnnotationTime() {
		return lastTime;
	}

	public HashSet<Tag> solveC2W(String text) throws AnnotationException {
		return ProblemReduction.A2WToC2W(solveA2W(text));
	}

	public HashSet<Annotation> solveD2W(String text, HashSet<Mention> mentions) throws AnnotationException {
		return ProblemReduction.Sa2WToD2W(solveSa2W(text), mentions, threshold);
	}

	public HashSet<Annotation> solveA2W(String text) throws AnnotationException {
		return ProblemReduction.Sa2WToA2W(solveSa2W(text), threshold);
	}

	public HashSet<ScoredTag> solveSc2W(String text) throws AnnotationException {
	    return ProblemReduction.Sa2WToSc2W(solveSa2W(text));
    }

	public static String concatenateStrings(String[] words, int left_index, int right_index) {
		String result = new String();

		for (int i = left_index; i <= right_index; i++) {
			result += words[i] + " ";
		}

		try {
			result = result.substring(0, result.length() - 1);
		} catch (StringIndexOutOfBoundsException e) {
			System.err.println();
		}
		return result;
	}
	
	public static void main(String[] args) {
		WikipediaApiInterface wikiApi = WikipediaApiInterface.api();
		FancyFakeAnnotator ann = new FancyFakeAnnotator(wikiApi);
		String query = "error in mathematics calculas";
		List<String> mention_candidates = FancyFakeAnnotator.getMentionCandidates(query);
		//ann.solveSa2W(query);
		
	}
	
	public static List<String> getMentionCandidates(String query) {
		String[] words = query.split(" ");
		List<String> mention_candidates = new ArrayList<String>();
		// Iterate over every start word and every possible end word (connected) to generate mention candidates
		for (int start_idx=0;start_idx<words.length;start_idx++) {
			String mention=words[start_idx];
			mention_candidates.add(mention);
			System.out.println(mention_candidates.get(mention_candidates.size()-1));
			for (int end_idx=start_idx+1;end_idx<words.length;end_idx++) {
				mention=new String(mention + " " + words[end_idx]);
				mention_candidates.add(mention);
				System.out.println(mention_candidates.get(mention_candidates.size()-1));
			}
		}
		return mention_candidates;
	}
	public HashSet<ScoredAnnotation> solveSa2W(String text) throws AnnotationException {
		lastTime = System.currentTimeMillis();
		try {
			Set<String> entities = CandidateGenerator.get_entity_candidates(text).keySet();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new AnnotationException(e.toString());
		}

		String[] words = text.split(" ");
		int n = words.length;
		System.err.println("Find entities for query " + text);
		List<String> mention_candidates = FancyFakeAnnotator.getMentionCandidates(text);
		
		// Iterate through all possible mentions entity pairs and greedily select them according to their anchorsAvgED score
		HashSet<ScoredAnnotation> result = new HashSet<>();
		List<Interval> used_intervals = new ArrayList<>();

		for (int i = 0; i < n; i++) {
			for (int j = 0; j <= i; j++) {
				int left_index = j;
				int right_index = n - i + j - 1;

				if (FancyFakeAnnotator.isForbiddenInterval(used_intervals, left_index, right_index))
					continue;

				
				String extract = FakeAnnotator.concatenateStrings(words, left_index, right_index);			
				int id = checkMention(extract);
				
				if (id != -1) {
					result.add(new ScoredAnnotation(text.indexOf(extract), extract.length(), id, 0.1f));
					used_intervals.add(new Interval(left_index, right_index));
				}


			}
		}

		lastTime = System.currentTimeMillis() - lastTime;
		return result;
    }

	public void setTrainingData(A2WDataset... data) {
		mentionIdMap = new HashMap<>();
		for (A2WDataset dataset : data) {
			mentionIdMap.putAll(convertDatasetToMap(dataset));
		}
	}

	public static HashMap<String, List<Integer>> convertDatasetToMap (A2WDataset dataSet) {
		List<HashSet<Annotation>> annotations =  dataSet.getA2WGoldStandardList();
		List<String> queries = dataSet.getTextInstanceList();

		int n = annotations.size();
		HashMap<String, List<Integer>> map = new HashMap<>();

		for (int i = 0; i < n; i++) {
			String query = queries.get(i);
			HashSet<Annotation> query_annotations = annotations.get(i);

			for (Annotation a : query_annotations) {
				int pos = a.getPosition();
				int len = a.getLength();
				int wid = a.getConcept();

				// Extract word from query
				String mention = query.substring(pos, pos + len);

				// Check if mention is already in the map
				if (!map.containsKey(mention)) {
					List<Integer> tagList = new ArrayList<>();
					tagList.add(wid);

					map.put(mention, tagList);
				} else {
					map.get(mention).add(wid);
				}
			}
		}

		return map;
	}

	private int checkMention(String mention) {
//		if (FakeAnnotator.mentionIdMap.containsKey(mention)) {
//			return FakeAnnotator.mentionIdMap.get(mention).get(0);
//		}
		String sanitizedMention = mention.replaceAll("[^a-zA-Z0-9' ]", "");

		int articleId = -1;
		try {
			int max_commonness_id = Integer.MAX_VALUE;
			double max_commonness = 0.0d;

			int[] links = WATRelatednessComputer.getLinks(sanitizedMention);
			int link_count = 0;
			for (int id : links) {
				if (link_count >= FancyFakeAnnotator.MAX_LINKS || max_commonness == 1.0d)
					break;

				String articleTitle = this.wikiApi.getTitlebyId(id);
				double commonness = WATRelatednessComputer.getCommonness(sanitizedMention, id);
				System.err.println(articleTitle);

				if (commonness >= max_commonness && id < max_commonness_id) {
					max_commonness_id = id;
					max_commonness = commonness;
				}

				link_count++;
			}

			return (links.length != 0) ? max_commonness_id : -1;
		} catch (IOException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public String getName() {
		return "Simple yet uneffective query annotator";
	}

	public static boolean isForbiddenInterval(List<Interval> intervals, int left_index, int right_index) {
		for (Interval interval : intervals) {
			if (interval.isWithin(left_index) || interval.isWithin(right_index))
				return true;
		}

		return false;
	}

	public class Interval {
		public final int l;
		public final int r;

		public Interval (int l, int r) {
			this.l = l;
			this.r = r;
		}

		public boolean isWithin (int index) {
			return this.l <= index && index <= this.r;
		}
	}
}

