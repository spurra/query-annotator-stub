package annotatorstub.classification;

import annotatorstub.annotator.CandidateGenerator;
import annotatorstub.annotator.SVMAnnotator;
import it.unipi.di.acube.batframework.cache.BenchmarkResults;
import libsvm.*;

import java.io.*;
import java.util.*;

public class Classifier {
    private svm_parameter param;		// set by read_parameters
    private svm_problem prob;		// set by read_problem
    public svm_model model;
    private String model_string;
    private String error_msg;
    private int cross_validation;
    private int nr_fold;
    public double weight;
    private static HashMap<String, List<Integer>> mentionIdMap;





    public Classifier() {
        model_string = "";

        File f = new File(SVMAnnotator.model_path);
        if (f.exists()) {
            System.out.println("Read svm model from "  + SVMAnnotator.model_path);
            try {
                model = svm.svm_load_model(SVMAnnotator.model_path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }


    public void addPositiveExample(String ex) {
        model_string += "+1 " + ex + "\n";
        System.out.println("+1 " + ex);
    }

    public void addNegativeExample(String ex) {
        model_string += "-1 " + ex + "\n";
        System.out.println("-1 " + ex);
    }


    public static void main(String argv[]) throws Exception
    {
        Map<String,List<Double>> entity_features = CandidateGenerator.get_entity_candidates("Funny cats wikipedia");
        ModelConverter serializer = new ModelConverter(entity_features);
        //String svm_model = serializer.serializeToString(entity_features);
        Classifier t = new Classifier();
        //t.model_string = svm_model;
        t.run();

    }


    private void do_cross_validation()
    {
        int i;
        int total_correct = 0;
        double total_error = 0;
        double sumv = 0, sumy = 0, sumvv = 0, sumyy = 0, sumvy = 0;
        double[] target = new double[prob.l];

        svm.svm_cross_validation(prob,param,nr_fold,target);
        if(param.svm_type == svm_parameter.EPSILON_SVR ||
                param.svm_type == svm_parameter.NU_SVR)
        {
            for(i=0;i<prob.l;i++)
            {
                double y = prob.y[i];
                double v = target[i];
                total_error += (v-y)*(v-y);
                sumv += v;
                sumy += y;
                sumvv += v*v;
                sumyy += y*y;
                sumvy += v*y;
            }
            System.out.print("Cross Validation Mean squared error = "+total_error/prob.l+"\n");
            System.out.print("Cross Validation Squared correlation coefficient = "+
                    ((prob.l*sumvy-sumv*sumy)*(prob.l*sumvy-sumv*sumy))/
                            ((prob.l*sumvv-sumv*sumv)*(prob.l*sumyy-sumy*sumy))+"\n"
            );
        }
        else
        {
            for(i=0;i<prob.l;i++)
                if(target[i] == prob.y[i])
                    ++total_correct;
            System.out.print("Cross Validation Accuracy = "+100.0*total_correct/prob.l+"%\n");
        }
    }

    public void saveDataset() {
        try {
            PrintWriter out = new PrintWriter(SVMAnnotator.train_dataset_path);
            out.println(this.model_string);
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void run() throws IOException
    {
        read_parameters();
        read_problem();
        error_msg = svm.svm_check_parameter(prob,param);

        if(error_msg != null)
        {
            System.err.print("ERROR: "+error_msg+"\n");
            System.exit(1);
        }

        if(cross_validation != 0)
        {
            do_cross_validation();
        }
        model = svm.svm_train(prob,param);
        svm.svm_save_model(SVMAnnotator.model_path,model);
    }


    public double predict(BufferedReader input, /*DataOutputStream output, */int predict_probability) throws IOException
    {
        String res = "";

        int svm_type=svm.svm_get_svm_type(model);
        int nr_class=svm.svm_get_nr_class(model);
        double[] prob_estimates=null;

        if(predict_probability == 1)
        {
            if(svm_type == svm_parameter.EPSILON_SVR ||
                    svm_type == svm_parameter.NU_SVR)
            {
                System.out.println("Prob. model for test data: target value = predicted value + z,\nz: Laplace distribution e^(-|z|/sigma)/(2sigma),sigma="+svm.svm_get_svr_probability(model)+"\n");
            }
            else
            {
                int[] labels=new int[nr_class];
                svm.svm_get_labels(model,labels);
                prob_estimates = new double[nr_class];
                //output.writeBytes("labels");
                res += "labels";
                for(int j=0;j<nr_class;j++) {
                    //output.writeBytes(" " + labels[j]);
                    res += " " + labels[j];
                }
                //output.writeBytes("\n");
                res += "\n";
            }
        }
        double v = 0;
        while(true)
        {
            String line = input.readLine();
            if(line == null) break;

            StringTokenizer st = new StringTokenizer(line," \t\n\r\f:");

            double target = atof(st.nextToken());
            int m = st.countTokens()/2;
            svm_node[] x = new svm_node[m];
            for(int j=0;j<m;j++)
            {
                x[j] = new svm_node();
                x[j].index = atoi(st.nextToken());
                x[j].value = atof(st.nextToken());
            }


            if (predict_probability==1 && (svm_type==svm_parameter.C_SVC || svm_type==svm_parameter.NU_SVC))
            {
                v = svm.svm_predict_probability(model,x,prob_estimates);
                //output.writeBytes(v+" ");
                res += v+" ";
                List<Double> scores = new ArrayList<Double>();
                for(int j=0;j<nr_class;j++) {
                    //output.writeBytes(prob_estimates[j] + " ");
                    res += prob_estimates[j] + " ";
                    scores.add(prob_estimates[j]);
                }
                if (prob_estimates[0] > prob_estimates[1])
                    v = v*(prob_estimates[0] - prob_estimates[1]);
                else
                    v = v*(prob_estimates[1] - prob_estimates[0]);
                //output.writeBytes("\n");
                res += "\n";
            }
            else
            {
                v = svm.svm_predict(model,x);
                //output.writeBytes(v+"\n");
                res += v+"\n";
            }

        }

        return v;
    }



    private static double atof(String s)
    {
        double d = Double.valueOf(s).doubleValue();
        if (Double.isNaN(d) || Double.isInfinite(d))
        {
            System.err.print("NaN or Infinity in input\n");
            System.exit(1);
        }
        return(d);
    }

    private static int atoi(String s)
    {
        int i = -1;
        try {
            i = Integer.parseInt(s);
        } catch(Exception e) {
            e.printStackTrace();
        }
        return i;
    }

    private void read_parameters()
    {
        int i;
        svm_print_interface print_func = null;	// default printing to stdout

        param = new svm_parameter();
        // default values
        param.svm_type = svm_parameter.C_SVC;
        param.kernel_type = svm_parameter.RBF;
        param.degree = 3;
        param.gamma = SVMAnnotator.GAMMA; //0.0625;
        param.coef0 = 0;
        param.nu = 0.5;
        param.cache_size = 100;
        param.C = SVMAnnotator.C; //32768;
        param.eps = 1e-3;
        param.p = 0.1;
        param.shrinking = 1;
        param.probability = SVMAnnotator.PREDICTION_PROBABILITY;
        if (SVMAnnotator.WEIGHTED) {
            param.nr_weight = 2;
            param.weight_label = new int[]{-1, 1};
            param.weight = new double[]{1, weight};
        }

        cross_validation = 0;
        nr_fold = 10;


        //svm.svm_set_print_string_function(print_func);
    }

    // read in a problem (in svmlight format)

    private void read_problem() throws IOException
    {
        BufferedReader fp;
        File f = new File(SVMAnnotator.train_dataset_scaled_path);
        if (f.exists())
            fp = new BufferedReader(new FileReader(SVMAnnotator.train_dataset_scaled_path));
        else
            fp = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(this.model_string.getBytes())));
        Vector<Double> vy = new Vector<Double>();
        Vector<svm_node[]> vx = new Vector<svm_node[]>();
        int max_index = 0;

        while(true)
        {
            String line = fp.readLine();
            if(line == null) break;

            StringTokenizer st = new StringTokenizer(line," \t\n\r\f:");

            vy.addElement(atof(st.nextToken()));
            int m = st.countTokens()/2;
            svm_node[] x = new svm_node[m];
            for(int j=0;j<m;j++)
            {
                x[j] = new svm_node();
                String temp = st.nextToken();
                x[j].index = atoi(temp);
                x[j].value = atof(st.nextToken());
            }
            if(m>0) max_index = Math.max(max_index, x[m-1].index);
            vx.addElement(x);
        }

        prob = new svm_problem();
        prob.l = vy.size();
        prob.x = new svm_node[prob.l][];
        for(int i=0;i<prob.l;i++)
            prob.x[i] = vx.elementAt(i);
        prob.y = new double[prob.l];
        for(int i=0;i<prob.l;i++)
            prob.y[i] = vy.elementAt(i);

        if(param.gamma == 0 && max_index > 0)
            param.gamma = 1.0/max_index;

        if(param.kernel_type == svm_parameter.PRECOMPUTED)
            for(int i=0;i<prob.l;i++)
            {
                if (prob.x[i][0].index != 0)
                {
                    System.err.print("Wrong kernel matrix: first column must be 0:sample_serial_number\n");
                    System.exit(1);
                }
                if ((int)prob.x[i][0].value <= 0 || (int)prob.x[i][0].value > max_index)
                {
                    System.err.print("Wrong input format: sample_serial_number out of range\n");
                    System.exit(1);
                }
            }

        fp.close();
    }

}
