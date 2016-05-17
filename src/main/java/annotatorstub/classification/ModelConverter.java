package annotatorstub.classification;

import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ModelConverter implements Serializable {

    private Map<String,List<Double>> entity_features;
    public static String path = "model/svm_model.txt";

    public ModelConverter(Map<String,List<Double>> entity_features) {
        this.entity_features = entity_features;
    }

    public static String serializeToString(/*Map<String,*/List<Double>/*> entity_*/features) {
        StringBuilder model = new StringBuilder();
        /*
        Iterator it = entity_features.keySet().iterator();
        while(it.hasNext()) {
            List<Double> features = entity_features.get(it.next());
            */
            for(int i = 0; i < features.size(); i++) {
                model.append((i+1) + ":" + features.get(i).toString() + " ");
            }
            model.append("\n");
        //}
        return model.toString();

    }

    void serializeToFile() {

        return;
    }

}
