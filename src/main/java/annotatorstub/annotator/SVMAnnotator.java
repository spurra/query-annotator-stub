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
import org.deeplearning4j.nn.api.Model;

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
	public static final int PREDICTION_PROBABILITY = 0;
	public static final int C = 1024;
	public static final double GAMMA = 0.0625;
	public static final String feature_path = "data/svm/features/";
	public static final String train_dataset_path = "data/svm/train_dataset.txt";
	public static final String train_dataset_scaled_path = "data/svm/train_dataset_scaled.txt";
	public static final String model_path = "data/svm/model.txt";
	private int nofp, nofn;
	private static HashMap<String, List<Integer>> testingQueryIdMap;
	private static HashMap<String, List<Integer>> queryIdMap;

	private WikipediaApiInterface wikiApi;

	private Classifier classifier;

	public SVMAnnotator(WikipediaApiInterface wikiApi) {
		this.classifier = new Classifier(train_dataset_scaled_path);
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

	
	public static void main() {
		WikipediaApiInterface wikiApi = WikipediaApiInterface.api();
		SVMAnnotator ann = new SVMAnnotator(wikiApi);
		ann.solveSa2W("error in mathematics calculas");
		
	}

	public void trainClassifier() {

		if (classifier.model != null)
			return;
		File f = new File(classifier.model_file_name);
		if (f.exists()) {
			try {
				classifier.run();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}

		nofp = 0; nofn = 0;
		for (String query: queryIdMap.keySet()){
			Map<String,List<Double>> entity_features;
			try {
				entity_features = CandidateGenerator.get_entity_candidates(query);
				for (String cand : entity_features.keySet()) {
					if (cand.isEmpty())
						continue;
					if (addCachedFeatures(query, cand))
						continue;
					String feature = ModelConverter.serializeToString(entity_features.get(cand));
					if (queryIdMap.get(query).contains(wikiApi.getIdByTitle(cand))) {
						classifier.addPositiveExample(feature);
						safeFeature("+1", query, cand, feature);
						nofp++;
					}
					else {
						classifier.addNegativeExample(feature);
						safeFeature("-1", query, cand, feature);
						nofn++;
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		classifier.weight = nofn/(double)nofp;
		classifier.saveDataset();
		// Normalize training data
		Process p;
		try {
			p = Runtime.getRuntime().exec("python data/svm/python/normalizeFeatures.py");
			p.waitFor();
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			classifier.run();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	public static String readCachedFeatures(String query, String cand) {
		String features = "";
		String cand_file_name = feature_path + query.replace("/", "_") + ":" + cand.replace("/", "_") + ".txt";
		File f = new File(cand_file_name);
		if (f.exists()) {
			System.out.println("Read features from cache: " + cand_file_name);
			try {
				features = Files.readAllLines(f.toPath()).get(0);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return features;
	}
	private boolean addCachedFeatures(String query, String cand) {
		boolean found = false;
		String cand_file_name = feature_path + query.replace("/", "_") + ":" + cand.replace("/", "_") + ".txt";
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
		else {
			String old_cand_file_name = feature_path + cand.replace("/", "_") + ".txt";
			File f_old = new File(old_cand_file_name);
			if (f_old.exists()) {
				found = true;
				System.out.println("Converting old feature file " + old_cand_file_name);
				String feature = null;
				try {
					feature = Files.readAllLines(f_old.toPath()).get(0);
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (feature.substring(0,2).equals("+1")) {
					safeFeature("+1", query, cand, feature.substring(3));
				} else if (feature.substring(0,2).equals("-1")) {
					safeFeature("-1", query, cand, feature.substring(3));
				}

				if(f_old.delete()){
					System.out.println("Old feature file " + f_old.getName() + " is deleted!");
				}else{
					System.out.println("Delete operation is failed.");
				}

			}
		}
		return found;
	}

	public static void safeFeature(String label, String query, String cand, String feature) {
		String cand_file_name = feature_path + query.replace("/", "_") + ":" + cand.replace("/", "_") + ".txt";
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
		if (testingQueryIdMap.get(text) == null)
			return res;
		try {
			Map<String,List<Double>> entity_features = CandidateGenerator.get_entity_candidates(text);
			List<String> lines = new ArrayList<>();
			for (String cand : entity_features.keySet()) {
				if (cand.isEmpty())
					continue;
				String features = readCachedFeatures(text, cand);
				features = normalizeFeatures(features);
				BufferedReader input;
				if (features.isEmpty()) {
					String label;
					if (testingQueryIdMap.get(text).contains(wikiApi.getIdByTitle(cand)))
						label = "+1";
					else
						label = "-1";

					features = ModelConverter.serializeToString(entity_features.get(cand));
					safeFeature(label, text, cand, features);
					input = new BufferedReader(new StringReader(label + " " + features));
				} else
					input = new BufferedReader(new StringReader(features));

				double pred = classifier.predict(input, PREDICTION_PROBABILITY);
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

	public void setTestingData(A2WDataset... data) {
		testingQueryIdMap = new HashMap<>();
		for (A2WDataset dataset : data) {
			testingQueryIdMap.putAll(convertDatasetToMap(dataset));
		}
	}

	public static HashMap<String, List<Integer>> convertDatasetToMap (A2WDataset dataSet) {
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

	public Classifier setDataAndTrainClassifier(HashMap<String, List<Integer>> dataSet) {
		queryIdMap = dataSet;
		trainClassifier();

		return classifier;
	}

	public static String normalizeFeatures(String feature) {
		String label = "";
		if (feature.startsWith("-1") || feature.startsWith("+1"))
			label = feature.substring(0, 2);
		else if (feature.startsWith("0 "))
			label = feature.substring(0, 1);
		List<Double> features = ModelConverter.deserializeFromString(feature);
		File f_mean = new File("data/svm/mean.txt");
		File f_std = new File("data/svm/std.txt");
		try {
			String[] means = Files.readAllLines(f_mean.toPath()).get(0).split(" ");
			String[] stds = Files.readAllLines(f_std.toPath()).get(0).split(" ");
			for (int i = 0; i < means.length; i++) {
				Double mean = new Double(means[i]);
				Double std = new Double(stds[i]);
				features.set(i, (features.get(i)-mean)/std);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return label + " " + ModelConverter.serializeToString(features);
	}
	
	public String getName() {
		return "Annotator that scores entity candidates with SVM";
	}

}
