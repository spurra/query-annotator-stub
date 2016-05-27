package annotatorstub.annotator;

import annotatorstub.classification.ModelConverter;
import org.bytedeco.javacpp.opencv_ml;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * Created by dejan on 5/25/16.
 */
public class ConvertFeatures {

    public static void main(String[] args) {
        File dir = new File("/home/dejan/Git/query-annotator-stub/data/svm/features_old/");
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            for (File child : directoryListing) {
                // Do something with child
                String feature = null;
                System.out.println("Converting " + child.toPath());
                try {
                    feature = Files.readAllLines(child.toPath()).get(0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                String label = feature.substring(0, 2);
                List<Double> flist = ModelConverter.deserializeFromString(feature.substring(3));
                flist.remove(1);
                feature = ModelConverter.serializeToString(flist);
                File newfile = new File(child.getPath().replace("features_old", "features_new"));
                BufferedWriter writer = null;
                try {
                    writer = new BufferedWriter(new FileWriter(newfile));
                    writer.write(label + " " + feature);
                    //Close writer
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            // Handle the case where dir is not really a directory.
            // Checking dir.isDirectory() above would not be sufficient
            // to avoid race conditions with another process that deletes
            // directories.
        }
    }

}
