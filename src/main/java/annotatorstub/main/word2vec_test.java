package annotatorstub.main;

import org.canova.api.util.ClassPathResource;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
import org.deeplearning4j.text.sentenceiterator.LineSentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentencePreProcessor;
import org.deeplearning4j.text.tokenization.tokenizer.TokenPreProcess;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.EndingPreProcessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.File;
import java.util.Collection;

/**
 * Created by adrian on 17/05/16.
 */

// Requires following file: GoogleNews-vectors-negative300.bin.gz.
// Download here: https://s3.amazonaws.com/dl4j-distribution/GoogleNews-vectors-negative300.bin.gz
public class word2vec_test {



    private static Logger log = LoggerFactory.getLogger(word2vec_test.class);

    public static void main(String[] args) throws Exception {

        long startTime;
        WordVectors vec;
        startTime = System.currentTimeMillis();

        // Most promising model
        System.out.println("Loading google model");
        vec = WordVectorSerializer.loadGoogleModel(new File("models/GoogleNews-vectors-negative300.bin.gz"), true);

/*        // Memory problems
        System.out.println("Loading freebase model");
        vec = WordVectorSerializer.loadGoogleModel(new File("models/freebase-vectors-skipgram1000.bin.gz"), true);*/

/*        // Memory problems
        System.out.println("Loading glove model");
        vec = WordVectorSerializer.loadTxtVectors(new File("models/glove.840B.300d.txt"));*/

        // Limited words available
/*        System.out.println("Loading glove model");
        vec = WordVectorSerializer.loadTxtVectors(new File("models/glove.6B.300d.txt"));*/

        System.out.println("Loading time: " + (System.currentTimeMillis() - startTime)/1000.0 + " sec");

        System.out.println("Calculating similarity");
        startTime = System.currentTimeMillis();
        double sim = vec.similarity("company", "government");
        System.out.println("Similarity: " + sim + " Calc time: " + (System.currentTimeMillis() - startTime)/1000 + " sec");

        System.out.println("Calculating nearest words");
        startTime = System.currentTimeMillis();
        Collection<String> lst = vec.wordsNearest("government", 10);
        System.out.println(lst + "\nCalc time: " + (System.currentTimeMillis() - startTime)/1000.0 + " sec");




    }
}
