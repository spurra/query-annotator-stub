package annotatorstub.main;
import annotatorstub.annotator.TagMeAnnotator;

import java.util.List;

/**
 * Created by lennart on 5/17/16.
 */
public class TestMain {
    public static void main (String[] args) {
        TagMeAnnotator tag_me = TagMeAnnotator.getInstance();

        String example = "The U.S. government has ruled the October bombing of a hospital in Kunduz, Afghanistan, an accident. But mounting evidence suggests that Afghans’ mistrust for Doctors Without Borders might have set the tragedy in motion.";
        double rho = 0.2d;
        System.out.println("Retrieving entitites contained in:");
        System.out.println("\t\t" + example);
        System.out.println();
        System.out.println("Found entities: ");
        List<TagMeAnnotator.Entity> entities = tag_me.getFilteredEntities(example, rho);
        for (TagMeAnnotator.Entity entity : entities) {
            System.out.println("\t\t" + entity.getWikiTitle() + " [rho = " + Double.toString(entity.getRho()) + "]");
        }
    }
}
