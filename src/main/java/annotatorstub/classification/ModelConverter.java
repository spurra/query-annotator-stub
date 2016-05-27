package annotatorstub.classification;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

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

    public static List<Double> deserializeFromString(String string) {
        if (string.startsWith("-1") || string.startsWith("+1"))
            string = string.substring(3);
        else if (string.startsWith("0 "))
            string = string.substring(2);
        List<Double> features = new ArrayList<>();

        String[] temp = string.split(" ");
        for (int i = 0; i < temp.length; i++) {
            String[] feature = temp[i].split(":");
            features.add(new Double(feature[1]));

        }
        return features;
    }

}
