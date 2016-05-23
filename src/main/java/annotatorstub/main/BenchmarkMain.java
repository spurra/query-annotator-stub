package annotatorstub.main;

import annotatorstub.annotator.SVMAnnotator;
import annotatorstub.annotator.TagMeAnnotator;
import annotatorstub.utils.Utils;
import it.unipi.di.acube.batframework.cache.BenchmarkCache;
import it.unipi.di.acube.batframework.data.Annotation;
import it.unipi.di.acube.batframework.data.Tag;
import it.unipi.di.acube.batframework.datasetPlugins.DatasetBuilder;
import it.unipi.di.acube.batframework.metrics.Metrics;
import it.unipi.di.acube.batframework.metrics.MetricsResultSet;
import it.unipi.di.acube.batframework.metrics.StrongAnnotationMatch;
import it.unipi.di.acube.batframework.metrics.StrongTagMatch;
import it.unipi.di.acube.batframework.problems.A2WDataset;
import it.unipi.di.acube.batframework.utils.DumpData;
import it.unipi.di.acube.batframework.utils.WikipediaApiInterface;

import java.io.File;
import java.util.HashSet;
import java.util.List;

public class BenchmarkMain {

	public static void main(String[] args) throws Exception {

		WikipediaApiInterface wikiApi = WikipediaApiInterface.api();

		// The test set
		A2WDataset ds = DatasetBuilder.getGerdaqTest();

		// The development set
		//A2WDataset ds = DatasetBuilder.getGerdaqDevel();
		//FakeAnnotator ann = new FakeAnnotator(wikiApi);

		// My fancy fake annotator
		//FancyFakeAnnotator ann = new FancyFakeAnnotator(wikiApi);

		//String wrong = "{\"timestamp\":\"2016-05-21T15:19:36\",\"time\":2,\"api\":\"tag\",\"annotations\":[{\"abstract\":\"Christian missionary activities often involve sending individuals and groups (called \\\"missionaries\\\"), to foreign countries and to places in their own homeland. This has frequently involved not only evangelization (in order to expand Christianity through the conversion of new members), but also humanitarian work, especially among the poor and disadvantaged. Missionaries have the authority to preach the Christian faith (and sometimes to administer sacraments), and provide humanitarian work to improve economic development, literacy, education, health care, and orphanages. Christian doctrines (such as the \\\"Doctrine of Love\\\" professed by many missions) permit the provision of aid without requiring religious conversion.\",\"id\":666270,\"title\":\"Mission (Christianity)\",\"start\":0,\"rho\":\"0.21675\",\"end\":12,\"spot\":\"MISSIONARIES\"},{\"abstract\":\"Central Africa is a core region of the African continent which includes Burundi, the Central African Republic, Chad, the Democratic Republic of the Congo, and Rwanda.\",\"id\":539606,\"title\":\"Central Africa\",\"start\":14,\"rho\":\"0.05927\",\"end\":21,\"spot\":\"Central\"},{\"abstract\":\"Christian missionary activities often involve sending individuals and groups (called \\\"missionaries\\\"), to foreign countries and to places in their own homeland. This has frequently involved not only evangelization (in order to expand Christianity through the conversion of new members), but also humanitarian work, especially among the poor and disadvantaged. Missionaries have the authority to preach the Christian faith (and sometimes to administer sacraments), and provide humanitarian work to improve economic development, literacy, education, health care, and orphanages. Christian doctrines (such as the \\\"Doctrine of Love\\\" professed by many missions) permit the provision of aid without requiring religious conversion.\",\"id\":666270,\"title\":\"Mission (Christianity)\",\"start\":22,\"rho\":\"0.23588\",\"end\":32,\"spot\":\"Missionary\"},{\"abstract\":\"Baptists are Christians who comprise a group of denominations and churches that subscribe to a doctrine that baptism should be performed only for professing believers (believer's baptism, as opposed to infant baptism), and that it must be done by immersion (as opposed to affusion or sprinkling). Other tenets of Baptist churches include soul competency (liberty), salvation through faith alone, scripture alone as the rule of faith and practice, and the autonomy of the local congregation. Baptists recognize two ministerial offices, pastors and deacons. Baptist churches are widely considered to be Protestant churches, though some Baptists disavow this identity.\",\"id\":3979,\"title\":\"Baptists\",\"start\":54,\"rho\":\"0.22905\",\"end\":61,\"spot\":\"Baptist\"},{\"abstract\":\"Mission Australia is a provider of family and community services throughout Australia. The organisation has at least 3200 staff, 1,000 volunteers and 300 services in every state and territory of Australia, and is one of the largest community organisations in the nation. It is currently headed by Toby Hall. The previous CEO, Patrick McClure, headed a major study looking at welfare reform for the Australian Government released in 2000.\",\"id\":2006314,\"title\":\"Mission Australia\",\"start\":64,\"rho\":\"0.56627\",\"end\":83,\"spot\":\"Mission\uE001: Australia\"},{\"abstract\":\"EarthLink is an IT services, network and communications provider headquartered in Atlanta, Georgia, USA. The company serves more than 150,000 businesses and 1 million U.S. consumers.\",\"id\":23554587,\"title\":\"EarthLink\",\"start\":95,\"rho\":\"0.51377\",\"end\":104,\"spot\":\"earthlink\"},{\"abstract\":\"EarthLink is an IT services, network and communications provider headquartered in Atlanta, Georgia, USA. The company serves more than 150,000 businesses and 1 million U.S. consumers.\",\"id\":23554587,\"title\":\"EarthLink\",\"start\":95,\"rho\":\"0.30000\",\"end\":108,\"spot\":\"earthlink.net\"},{\"abstract\":\"Laurice Dean Napper, known as L.D. \\\"Buddy\\\" Napper (born ca. 1918), is an attorney and civic figure in Ruston, Louisiana, who served as a Democratic member of the Louisiana House of Representatives from 1952-1964.\",\"id\":27196181,\"title\":\"L.D. \"Buddy\" Napper\",\"start\":111,\"rho\":\"0.28165\",\"end\":117,\"spot\":\"Napper\"},{\"abstract\":\"Michael Gordon Oldfield (born 15 May 1953) is an English multi-instrumentalist musician and composer, working a style that blends progressive rock, folk, ethnic or world music, classical music, electronic music, New Age, and more recently, dance. His music is often elaborate and complex in nature. He is best known for his 1973 hit album Tubular Bells, which launched Virgin Records, and for his 1983 hit single \\\"Moonlight Shadow\\\". He is also well known for his hit rendition of the Christmas piece, \\\"In Dulci Jubilo\\\".\",\"id\":20032,\"title\":\"Mike Oldfield\",\"start\":119,\"rho\":\"0.03034\",\"end\":123,\"spot\":\"Mike\"},{\"abstract\":\"Christopher Andrew \\\"Christy\\\" Moore (born 7 May 1945) is an Irish folk singer, songwriter, and guitarist. He is well known as one of the founding members of Planxty and Moving Hearts. His first album, Paddy on the Road (a minor release of 500, although made available again on CD through his website and at gigs in 2010) was recorded with Dominic Behan (brother of Brendan) in 1969. In 2007, he was named as Ireland's greatest living musician in RTÉ's People of the Year Awards.\",\"id\":1022191,\"title\":\"Christy Moore\",\"start\":126,\"rho\":\"0.04235\",\"end\":133,\"spot\":\"Christy\"},{\"abstract\":\"In Christian churches, a minister is someone who is authorized by a church or religious organization to perform functions such as teaching of beliefs; leading services such as weddings, baptisms or funerals; or otherwise providing spiritual guidance to the community. The term is taken from Latin minister \\u201cservant, attendant\\u201d, which itself was derived from minus \\u201cless.\\u201d.\",\"id\":739238,\"title\":\"Minister (Christianity)\",\"start\":142,\"rho\":\"0.12074\",\"end\":150,\"spot\":\"Ministry\"}],\"lang\":\"en\"}\n";
		//String result = TagMeAnnotator.sanitizeJson(wrong);

		// SVM annotator
		SVMAnnotator ann = new SVMAnnotator(wikiApi);
		ann.setTrainingData(DatasetBuilder.getGerdaqTrainA(), DatasetBuilder.getGerdaqTrainB(), DatasetBuilder.getGerdaqDevel());


		List<HashSet<Tag>> resTag = BenchmarkCache.doC2WTags(ann, ds);
		List<HashSet<Annotation>> resAnn = BenchmarkCache.doA2WAnnotations(ann, ds);
		DumpData.dumpCompareList(ds.getTextInstanceList(), ds.getA2WGoldStandardList(), resAnn, wikiApi);

		Metrics<Tag> metricsTag = new Metrics<>();
		MetricsResultSet C2WRes = metricsTag.getResult(resTag, ds.getC2WGoldStandardList(), new StrongTagMatch(wikiApi));
		Utils.printMetricsResultSet("C2W", C2WRes, ann.getName());

		Metrics<Annotation> metricsAnn = new Metrics<>();
		MetricsResultSet rsA2W = metricsAnn.getResult(resAnn, ds.getA2WGoldStandardList(), new StrongAnnotationMatch(wikiApi));
		Utils.printMetricsResultSet("A2W-SAM", rsA2W, ann.getName());

		Utils.serializeResult(ann, ds, new File("annotations.bin"));
		wikiApi.flush();
	}

}
