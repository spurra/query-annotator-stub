package annotatorstub.annotator;

import annotatorstub.classification.Classifier;
import annotatorstub.classification.ModelConverter;
import annotatorstub.utils.EntityMentionPair;
import annotatorstub.utils.SMAPHFeatures;
import annotatorstub.utils.WATRelatednessComputer;
import it.unipi.di.acube.batframework.data.*;
import it.unipi.di.acube.batframework.problems.A2WDataset;
import it.unipi.di.acube.batframework.problems.Sa2WSystem;
import it.unipi.di.acube.batframework.utils.AnnotationException;
import it.unipi.di.acube.batframework.utils.ProblemReduction;
import it.unipi.di.acube.batframework.utils.WikipediaApiInterface;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by adrian on 27/05/16.
 */
public class SVMAnnotatorFull implements Sa2WSystem {

    private static long lastTime = -1;
    private static float threshold = -1f;
    private static final int MAX_LINKS = 500;
    public static WikipediaApiInterface wiki =  WikipediaApiInterface.api();
    private static WordVectors vec;
    private static HashMap<String, List<Integer>> mentionIdMap;
    private static HashMap<String, List<Integer>> testingQueryIdMap;

    private WikipediaApiInterface wikiApi;

    private Classifier classifier;

    public SVMAnnotatorFull (WikipediaApiInterface wikiApi) {
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
        List<SVMAnnotatorFull.FakeMention> mention_candidates = new SVMAnnotatorFull(wiki).getMentionCandidates(query);
        //ann.solveSa2W(query);

    }

    public List<SVMAnnotatorFull.FakeMention> getMentionCandidates(String query) {
        String[] words = query.split(" ");
        List<SVMAnnotatorFull.FakeMention> mention_candidates = new ArrayList<SVMAnnotatorFull.FakeMention>();
        // Iterate over every start word and every possible end word (connected) to generate mention candidates
        for (int start_idx=0;start_idx<words.length;start_idx++) {
            String mention=words[start_idx];
            SVMAnnotatorFull.FakeMention m = new SVMAnnotatorFull.FakeMention(mention,start_idx,start_idx);
            mention_candidates.add(m);
            System.out.println(mention_candidates.get(mention_candidates.size()-1));
            for (int end_idx=start_idx+1;end_idx<words.length;end_idx++) {
                mention=new String(mention + " " + words[end_idx]);
                m = new SVMAnnotatorFull.FakeMention(mention,start_idx,end_idx);
                mention_candidates.add(m);
                System.out.println(mention_candidates.get(mention_candidates.size()-1));
            }
        }
        return mention_candidates;
    }
    public HashSet<ScoredAnnotation> solveSa2W(String text) throws AnnotationException {
        HashSet<ScoredAnnotation> result = new HashSet<>();
        if (true && vec == null) {
            System.out.println("Load word2vec model");
            try {
                vec = WordVectorSerializer.loadGoogleModel(new File("models/GoogleNews-vectors-negative300.bin.gz"), true);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        lastTime = System.currentTimeMillis();
        Set<String> entity_candidates = new HashSet<>();
        if (testingQueryIdMap.get(text) == null)
            return result;
        try {
            Map<String,List<Double>> entity_features = CandidateGenerator.get_entity_candidates(text);
            List<String> lines = new ArrayList<>();
            for (String cand : entity_features.keySet()) {
                if (cand.isEmpty())
                    continue;
                String features = SVMAnnotator.readCachedFeatures(text, cand);
                features = SVMAnnotator.normalizeFeatures(features);
                BufferedReader input;
                if (features.isEmpty()) {
                    String label;
                    if (testingQueryIdMap.get(text).contains(wikiApi.getIdByTitle(cand)))
                        label = "+1";
                    else
                        label = "-1";

                    features = ModelConverter.serializeToString(entity_features.get(cand));
                    SVMAnnotator.safeFeature(label, text, cand, features);
                    input = new BufferedReader(new StringReader(label + " " + features));
                } else
                    input = new BufferedReader(new StringReader(features));

                double pred = classifier.predict(input, SVMAnnotator.PREDICTION_PROBABILITY);
                if (pred > threshold)
                    entity_candidates.add(cand);
                System.out.println("Candidate " + cand + "\t score: " + pred);
                lines.add("Candidate " + cand + "\t score: " + pred);
            }

            Path file = Paths.get("data/svm/prediction.txt");
            Files.write(file, lines, Charset.forName("UTF-8"));

        } catch (Exception e) {
            e.printStackTrace();
        }


        String[] words = text.split(" ");
        int n = words.length;
        System.err.println("Find entities for query " + text);

        // Get all mention candidates
        List<SVMAnnotatorFull.FakeMention> mention_candidates = new SVMAnnotatorFull(wiki).getMentionCandidates(text);

        // Iterate through all possible mentions entity pairs and score them according to their anchorsAvgED score
        List<EntityMentionPair> em_candidates = new ArrayList<EntityMentionPair>();
        EntityMentionPair pair;
        for (String entity : entity_candidates) {
            int wiki_id = -1;
            try {
                wiki_id = wiki.getIdByTitle(entity);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            for (SVMAnnotatorFull.FakeMention mention : mention_candidates) {
                double score=0;
                try {
                    score = SMAPHFeatures.word2vec_sim(vec, entity, mention.name);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                pair = new EntityMentionPair(wiki_id, mention.name, entity, null, score);
                pair.setMentionPosition(mention.start, mention.end);
                em_candidates.add(pair);
            }

        }
        List<SVMAnnotatorFull.Interval> used_intervals = new ArrayList<>();

        // greedily select mention entity pairs
        Collections.sort(em_candidates);
        for (EntityMentionPair cand_pair : em_candidates) {
            if (SVMAnnotatorFull.isForbiddenInterval(used_intervals, cand_pair.getStartIdx(), cand_pair.getEndIdx()))
                continue;


            String extract = FakeAnnotator.concatenateStrings(words, cand_pair.getStartIdx(), cand_pair.getEndIdx());
            int id = checkMention(extract);

            if (id != -1) {
                System.out.println("Select " + cand_pair.getMention() + "->" + cand_pair.getWikiTitle() + "; score: " + Double.toString(cand_pair.getRho()));
                result.add(new ScoredAnnotation(text.indexOf(extract), extract.length(), id, (float)cand_pair.getRho()));
                used_intervals.add(new SVMAnnotatorFull.Interval(cand_pair.getStartIdx(), cand_pair.getEndIdx()));
            }
        }


        lastTime = System.currentTimeMillis() - lastTime;
        return result;
    }

    public void setTrainingData(A2WDataset... data) {
        mentionIdMap = new HashMap<>();
        for (A2WDataset dataset : data) {
            mentionIdMap.putAll(SVMAnnotator.convertDatasetToMap(dataset));
        }

        SVMAnnotator tmp = new SVMAnnotator(wikiApi);
        classifier = tmp.setDataAndTrainClassifier(mentionIdMap);

    }

    public void setTestingData(A2WDataset... data) {
        testingQueryIdMap = new HashMap<>();
        for (A2WDataset dataset : data) {
            testingQueryIdMap.putAll(SVMAnnotator.convertDatasetToMap(dataset));
        }
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
                if (link_count >= SVMAnnotatorFull.MAX_LINKS || max_commonness == 1.0d)
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
        return "SVMAnnotatorFull";
    }

    public static boolean isForbiddenInterval(List<SVMAnnotatorFull.Interval> intervals, int left_index, int right_index) {
        for (SVMAnnotatorFull.Interval interval : intervals) {
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

    public class FakeMention {
        public int start;
        public int end;
        public String name;

        public FakeMention(String name, int start, int end) {
            this.start=start;
            this.end=end;
            this.name=name;

        }
    }
}
