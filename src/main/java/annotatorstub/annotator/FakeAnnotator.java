package annotatorstub.annotator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

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

public class FakeAnnotator implements Sa2WSystem {
	private static long lastTime = -1;
	private static float threshold = -1f;

	private static A2WDataset trainingData;
	private static HashMap<String, List<Integer>> mentionIdMap;
	
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

	public HashSet<ScoredAnnotation> solveSa2W(String text) throws AnnotationException {
		lastTime = System.currentTimeMillis();

		String[] words = text.split(" ");
		int n = words.length;

		// Iterate through all possible mentions and check if it exists in the training set.
		// Start with the longest.
		HashSet<ScoredAnnotation> result = new HashSet<>();

		for (int i = 0; i < n; i++) {
			for (int j = 0; j <= i; j++) {
				// TODO adjust index for substring
				int left_index = j;
				int right_index = n - i + j - 1;

				String extract = FakeAnnotator.concatenateStrings(words, left_index, right_index);
				int id = checkMention(extract);

				if (id != -1)
					result.add(new ScoredAnnotation(text.indexOf(extract), extract.length(), id, 0.1f));


			}
		}

		lastTime = System.currentTimeMillis() - lastTime;
		return result;
    }

	public void setTrainingData(A2WDataset data) {
		trainingData = data;
		mentionIdMap = convertDatasetToMap();
	}

	public static HashMap<String, List<Integer>> convertDatasetToMap () {
		A2WDataset dataSet = FakeAnnotator.trainingData;

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
		if (FakeAnnotator.mentionIdMap.containsKey(mention)) {
			return FakeAnnotator.mentionIdMap.get(mention).get(0);
		}

		return -1;
	}
	
	public String getName() {
		return "Simple yet uneffective query annotator";
	}
}
