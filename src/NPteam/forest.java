package NPteam;

import com.google.common.base.Stopwatch;
import javassist.NotFoundException;
import org.apache.log4j.Level;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.mllib.linalg.DenseVector;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.regression.LabeledPoint;
import org.apache.spark.mllib.tree.RandomForest;
import org.apache.spark.mllib.tree.model.RandomForestModel;
import org.apache.spark.mllib.util.MLUtils;
import scala.Tuple2;

//import org.slf4j.impl.StaticLoggerBinder;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * RFSpark
 * Created by kote on 02.06.17.
 */
public class forest {
    private static Integer numClasses, numTrees, maxDepth, maxBins, seed;
    private static SparkConf sparkConf;
    private static JavaSparkContext jsc;
    private static Logger log;

    forest(Integer numClasses, Integer numTrees, Integer maxDepth, Integer maxBins) {
        org.apache.log4j.Logger.getLogger("org").setLevel(Level.ERROR);
        org.apache.log4j.Logger.getLogger("akka").setLevel(Level.ERROR);

        forest.log = Logger.getLogger(forest.class.getName());

        forest.maxBins = maxBins;
        forest.maxDepth = maxDepth;
        forest.numClasses = numClasses;
        forest.numTrees = numTrees;
        seed = new Random().nextInt();

        sparkConf = new SparkConf().setAppName("JavaRandomForestClassificationFire").setMaster("local[4]").set("spark.executor.memory","2g");
        jsc = new JavaSparkContext(sparkConf);
        jsc.setLogLevel("ERROR");
    }

    @Override
    protected void finalize() {
        jsc.stop();
    }

    static public void load(String fileName, String data, String fileAnswer) {
        RandomForestModel loadModel = RandomForestModel.load(jsc.sc(), fileName);
        String[] sData = data.split(",");
        double[] dData = new double[sData.length];
        for (int i = 0; i < sData.length; i++) {
            dData[i] = Double.valueOf(sData[i]);
        }
        Vector inputVectorPredict = new DenseVector(dData);
        try {
            Double predict = loadModel.predict(inputVectorPredict);
            System.out.println("Predict: " + predict);
            try (FileWriter fw = new FileWriter(fileAnswer, true);
                 BufferedWriter bw = new BufferedWriter(fw);
                 PrintWriter out = new PrintWriter(bw))
            {
                out.println(predict.toString());
            } catch (Exception e) {
                log.severe("Can not write answer to file!");
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            log.severe("Number of parameters in the learning dataset does not converge with received!");
        }
    }

    static public void save(String fileName, String datasetPath) {
        JavaRDD<LabeledPoint> data = MLUtils.loadLibSVMFile(jsc.sc(), datasetPath).toJavaRDD();
        JavaRDD<LabeledPoint>[] splits = data.randomSplit(new double[]{1.0, 0.25});
        JavaRDD<LabeledPoint> trainingData = splits[0];
        JavaRDD<LabeledPoint> testData = splits[1];

        Map<Integer, Integer> categoricalFeaturesInfo = new HashMap<>();
        String featureSubsetStrategy = "auto";
        String impurity = "gini";

        RandomForestModel model = RandomForest.trainClassifier(trainingData, numClasses,
                categoricalFeaturesInfo, numTrees, featureSubsetStrategy, impurity, maxDepth, maxBins,
                seed);

        JavaPairRDD<Double, Double> predictionAndLabel =
                testData.mapToPair(p -> new Tuple2<>(model.predict(p.features()), p.label()));
        double testErr =
                predictionAndLabel.filter(pl -> !pl._1().equals(pl._2())).count() / (double) testData.count();
        System.out.println("totalError: " + testErr);
        model.save(jsc.sc(), fileName);
    }

    static private String toVectors(String[] foo) {
        String last = foo[foo.length-1];
        String ans = last;
        for(Integer i = 0; i < foo.length-1; i++) {
            ans += (" " + (i+1) + ":" + foo[i]);
        }
        return ans;
    }

    /*static private String toVectors(String[] foo, Integer roundTo, Integer maxEl) {


        String last = foo[foo.length-1];
        String ans = last;
        for(Integer i = 0; i < foo.length-1; i++) {
            ans += (" " + (i+1) + ":" + foo[i]);
        }
        return ans;
    }*/

    static public void convert(String inputFile) {
        JavaRDD<String> csv = jsc.textFile(inputFile);
        JavaRDD<String[]> csvCol =  csv.map(s -> s.split(";"));
        JavaRDD<String> out = csvCol.map(sArr -> toVectors(sArr));

        String nameCorrected = inputFile.replaceAll("\\.","") + "_corrected";
        File fileCorrected = new File(nameCorrected);
        try {
            if (!fileCorrected.exists()) {
                fileCorrected.createNewFile();
            } else {
                fileCorrected.delete();
                fileCorrected.createNewFile();
            }
            PrintWriter w = new PrintWriter(fileCorrected.getAbsoluteFile());
            for (String str : out.collect()) {
                w.println(str);
            }
            w.close();
        } catch (IOException e) {
            log.severe("Can't save corrected dataset!");
            e.printStackTrace();
        }

        //JavaRDD<String> out = csvColl.
    }
}
