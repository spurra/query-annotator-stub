package annotatorstub.classification;

import annotatorstub.annotator.CandidateGenerator;
import it.unipi.di.acube.batframework.cache.BenchmarkResults;
import libsvm.*;

import java.io.*;
import java.util.*;

public class Classifier {
    private svm_parameter param;		// set by parse_command_line
    private svm_problem prob;		// set by read_problem
    public svm_model model;
    private String input_file_name;		// set by parse_command_line
    private String model_file_name;		// set by parse_command_line
    private String model_string;
    private String error_msg;
    private int cross_validation;
    private int nr_fold;
    public double weight;
    private static HashMap<String, List<Integer>> mentionIdMap;
    private static BenchmarkResults resultsCache = new BenchmarkResults();

    private static svm_print_interface svm_print_null = new svm_print_interface()
    {
        public void print(String s) {}
    };

    private static svm_print_interface svm_print_stdout = new svm_print_interface()
    {
        public void print(String s)
        {
            System.out.print(s);
        }
    };

    private static svm_print_interface svm_print_string = svm_print_stdout;

    static void info(String s)
    {
        svm_print_string.print(s);
    }



    public Classifier() {
        model_string = "";
        model_file_name = "data/svm/model.txt";

        File f = new File(model_file_name);
        if (f.exists()) {
            System.out.println("Read svm model from "  + model_file_name);
            try {
                model = svm.svm_load_model(model_file_name);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }


    public void addPositiveExample(String ex) {
        model_string += "+1 " + ex;
        System.out.println("+1 " + ex);
    }

    public void addNegativeExample(String ex) {
        model_string += "-1 " + ex;
        System.out.println("-1 " + ex);
    }


    public static void main(String argv[]) throws Exception
    {
        Map<String,List<Double>> entity_features = CandidateGenerator.get_entity_candidates("Funny cats wikipedia");
        ModelConverter serializer = new ModelConverter(entity_features);
        //String svm_model = serializer.serializeToString(entity_features);
        Classifier t = new Classifier();
        //t.model_string = svm_model;
        t.run(argv);

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

    public void run(String argv[]) throws IOException
    {
        parse_command_line(argv);
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
        else
        {
            model = svm.svm_train(prob,param);
            svm.svm_save_model(model_file_name,model);
        }
    }

    public double predict(BufferedReader input, /*DataOutputStream output, */int predict_probability) throws IOException
    {
        String res = "";
        int correct = 0;
        int total = 0;
        double error = 0;
        double sumv = 0, sumy = 0, sumvv = 0, sumyy = 0, sumvy = 0;

        int svm_type=svm.svm_get_svm_type(model);
        int nr_class=svm.svm_get_nr_class(model);
        double[] prob_estimates=null;

        if(predict_probability == 1)
        {
            if(svm_type == svm_parameter.EPSILON_SVR ||
                    svm_type == svm_parameter.NU_SVR)
            {
                Classifier.info("Prob. model for test data: target value = predicted value + z,\nz: Laplace distribution e^(-|z|/sigma)/(2sigma),sigma="+svm.svm_get_svr_probability(model)+"\n");
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

            if(v == target)
                ++correct;
            error += (v-target)*(v-target);
            sumv += v;
            sumy += target;
            sumvv += v*v;
            sumyy += target*target;
            sumvy += v*target;
            ++total;
        }
        /*
        if(svm_type == svm_parameter.EPSILON_SVR ||
                svm_type == svm_parameter.NU_SVR)
        {
            Classifier.info("Mean squared error = "+error/total+" (regression)\n");
            Classifier.info("Squared correlation coefficient = "+
                    ((total*sumvy-sumv*sumy)*(total*sumvy-sumv*sumy))/
                            ((total*sumvv-sumv*sumv)*(total*sumyy-sumy*sumy))+
                    " (regression)\n");
        }
        else
            Classifier.info("Accuracy = "+(double)correct/total*100+
                    "% ("+correct+"/"+total+") (classification)\n");
        */

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
        return Integer.parseInt(s);
    }

    private void parse_command_line(String argv[])
    {
        int i;
        svm_print_interface print_func = null;	// default printing to stdout

        param = new svm_parameter();
        // default values
        param.svm_type = svm_parameter.C_SVC;
        param.kernel_type = svm_parameter.RBF;
        param.degree = 3;
        param.gamma = 0;	// 1/num_features
        param.coef0 = 0;
        param.nu = 0.5;
        param.cache_size = 100;
        param.C = 1;
        param.eps = 1e-3;
        param.p = 0.1;
        param.shrinking = 1;
        param.probability = 1;
        param.nr_weight = 2;
        param.weight_label = new int[]{-1, 1};
        param.weight = new double[]{1, weight};
        cross_validation = 1;
        nr_fold = 10;

/*
        // parse options
        for(i=0;i<argv.length;i++)
        {
            if(argv[i].charAt(0) != '-') break;
            //if(++i>=argv.length)
            //    exit_with_help();
            switch(argv[i-1].charAt(1))
            {
                case 's':
                    param.svm_type = atoi(argv[i]);
                    break;
                case 't':
                    param.kernel_type = atoi(argv[i]);
                    break;
                case 'd':
                    param.degree = atoi(argv[i]);
                    break;
                case 'g':
                    param.gamma = atof(argv[i]);
                    break;
                case 'r':
                    param.coef0 = atof(argv[i]);
                    break;
                case 'n':
                    param.nu = atof(argv[i]);
                    break;
                case 'm':
                    param.cache_size = atof(argv[i]);
                    break;
                case 'c':
                    param.C = atof(argv[i]);
                    break;
                case 'e':
                    param.eps = atof(argv[i]);
                    break;
                case 'p':
                    param.p = atof(argv[i]);
                    break;
                case 'h':
                    param.shrinking = atoi(argv[i]);
                    break;
                case 'b':
                    param.probability = atoi(argv[i]);
                    break;
                case 'q':
                    print_func = svm_print_null;
                    i--;
                    break;
                case 'v':
                    cross_validation = 1;
                    nr_fold = atoi(argv[i]);
                    if(nr_fold < 2)
                    {
                        System.err.print("n-fold cross validation: n must >= 2\n");
                        exit_with_help();
                    }
                    break;
                case 'w':
                    ++param.nr_weight;
                {
                    int[] old = param.weight_label;
                    param.weight_label = new int[param.nr_weight];
                    System.arraycopy(old,0,param.weight_label,0,param.nr_weight-1);
                }

                {
                    double[] old = param.weight;
                    param.weight = new double[param.nr_weight];
                    System.arraycopy(old,0,param.weight,0,param.nr_weight-1);
                }

                param.weight_label[param.nr_weight-1] = atoi(argv[i-1].substring(2));
                param.weight[param.nr_weight-1] = atof(argv[i]);
                break;
                default:
                    System.err.print("Unknown option: " + argv[i-1] + "\n");
                    //exit_with_help();
            }
        }*/

        svm.svm_set_print_string_function(print_func);

        // determine filenames
/*
        if(i>=argv.length)
            exit_with_help();

        input_file_name = argv[i];

        if(i<argv.length-1)
            model_file_name = argv[i+1];
        else
        {
            int p = argv[i].lastIndexOf('/');
            ++p;	// whew...
            model_file_name = argv[i].substring(p)+".model";
        }
        */
    }

    // read in a problem (in svmlight format)

    private void read_problem() throws IOException
    {
        BufferedReader fp;
        if (!this.model_string.isEmpty())
            fp = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(this.model_string.getBytes())));
        else
            fp = new BufferedReader(new FileReader(input_file_name));
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
