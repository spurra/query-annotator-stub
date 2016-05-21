package annotatorstub.annotator;

import annotatorstub.classification.Classifier;
import annotatorstub.classification.ModelConverter;
import annotatorstub.utils.WATRelatednessComputer;
import it.unipi.di.acube.batframework.data.*;
import it.unipi.di.acube.batframework.problems.A2WDataset;
import it.unipi.di.acube.batframework.problems.Sa2WSystem;
import it.unipi.di.acube.batframework.utils.AnnotationException;
import it.unipi.di.acube.batframework.utils.ProblemReduction;
import it.unipi.di.acube.batframework.utils.WikipediaApiInterface;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by dejan on 5/17/16.
 */
public class SVMAnnotator implements Sa2WSystem {
	private static long lastTime = -1;
	private static float threshold = 0f;
	private static final int MAX_LINKS = 5;
	private static final String feature_path = "data/svm/features/";
	private int nofp, nofn;
	private static HashMap<String, List<Integer>> queryIdMap;

	private WikipediaApiInterface wikiApi;

	private Classifier classifier;

	public SVMAnnotator(WikipediaApiInterface wikiApi) {
		this.classifier = new Classifier();
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
	
	public static void main() {
		WikipediaApiInterface wikiApi = WikipediaApiInterface.api();
		SVMAnnotator ann = new SVMAnnotator(wikiApi);
		ann.solveSa2W("error in mathematics calculas");
		
	}

	public void trainClassifier() {

		if (classifier.model != null)
			return;

		nofp = 0; nofn = 0;
		for (String query: queryIdMap.keySet()){
			Map<String,List<Double>> entity_features = null;
			try {
				entity_features = CandidateGenerator.get_entity_candidates(query);
				for (String cand : entity_features.keySet()) {
					if (cand.isEmpty())
						continue;
					if (addCachedFeatures(entity_features, cand))
						continue;
					String feature = ModelConverter.serializeToString(entity_features.get(cand));
					if (queryIdMap.get(query).contains(wikiApi.getIdByTitle(cand))) {
						classifier.addPositiveExample(feature);
						safeFeature("+1", cand, feature);
						nofp++;
					}
					else {
						classifier.addNegativeExample(feature);
						safeFeature("-1", cand, feature);
						nofn++;
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
			//nr++;
			//if (nr > MAX_LINKS) break;
		}
		classifier.weight = nofn/(double)nofp;

		try {
			classifier.run(new String[]{});
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private boolean addCachedFeatures(Map<String,List<Double>> entity_features, String cand) {
		boolean found = false;
		String cand_file_name = feature_path + cand.replace("/", "_") + ".txt";
		File f = new File(cand_file_name);
		if (f.exists()) {
			found = true;
			System.out.println("Read features from cache: " + cand_file_name);
			try {
				String feature = Files.readAllLines(f.toPath()).get(0);
				if (feature.substring(0,2).equals("+1")) {
					classifier.addPositiveExample(feature.substring(3));
					nofp++;
				} else if (feature.substring(0,2).equals("-1")) {
					classifier.addNegativeExample(feature.substring(3));
					nofn++;
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return found;
	}

	private void safeFeature(String label, String cand, String feature) {
		String cand_file_name = feature_path + cand.replace("/", "_") + ".txt";
		File f = new File(cand_file_name);
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(f));
			writer.write(label + " " + feature);
			//Close writer
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public HashSet<ScoredAnnotation> solveSa2W(String text) throws AnnotationException {

		lastTime = System.currentTimeMillis();
		HashSet<ScoredAnnotation> res = new HashSet<>();
		try {
			Map<String,List<Double>> entity_features = CandidateGenerator.get_entity_candidates(text);
			List<String> lines = new ArrayList<>();
			for (String cand : entity_features.keySet()) {
				String features = "0 " + ModelConverter.serializeToString(entity_features.get(cand));
				BufferedReader input = new BufferedReader(new StringReader(features));
				double pred = classifier.predict(input, 1);
				if (pred > threshold)
					res.add(new ScoredAnnotation(0, 0, wikiApi.getIdByTitle(cand), (float) pred));
				System.out.println("Candidate " + cand + "\t score: " + pred);
				lines.add("Candidate " + cand + "\t score: " + pred);
			}

			Path file = Paths.get("data/svm/prediction.txt");
			Files.write(file, lines, Charset.forName("UTF-8"));

		} catch (Exception e) {
			e.printStackTrace();
		}
		lastTime = System.currentTimeMillis() - lastTime;
		return res;
    }

	public void setTrainingData(A2WDataset... data) {
		queryIdMap = new HashMap<>();
		for (A2WDataset dataset : data) {
			queryIdMap.putAll(convertDatasetToMap(dataset));
		}

		trainClassifier();
	}

	public HashMap<String, List<Integer>> convertDatasetToMap (A2WDataset dataSet) {
		List<HashSet<Tag>> tags =  dataSet.getC2WGoldStandardList();
		List<String> queries = dataSet.getTextInstanceList();

		int n = tags.size();
		HashMap<String, List<Integer>> map = new HashMap<>();

		for (int i = 0; i < n; i++) {
			String query = queries.get(i);
			HashSet<Tag> query_tags = tags.get(i);

			for (Tag t : query_tags) {
				int wid = t.getConcept();

				// Check if query is already in the map
				if (!map.containsKey(query)) {
					List<Integer> tagList = new ArrayList<>();
					tagList.add(wid);

					map.put(query, tagList);
				} else {
					map.get(query).add(wid);
				}
			}

		}

		return map;
	}

	private int checkMention(String mention) {
//		if (FakeAnnotator.queryIdMap.containsKey(mention)) {
//			return FakeAnnotator.queryIdMap.get(mention).get(0);
//		}
		String sanitizedMention = mention.replaceAll("[^a-zA-Z0-9' ]", "");

		int articleId = -1;
		try {
			int max_commonness_id = Integer.MAX_VALUE;
			double max_commonness = 0.0d;

			int[] links = WATRelatednessComputer.getLinks(sanitizedMention);
			int link_count = 0;
			for (int id : links) {
				if (link_count >= SVMAnnotator.MAX_LINKS || max_commonness == 1.0d)
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
		return "Annotator that scores entity candidates with SVM";
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
