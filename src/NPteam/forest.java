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
import java.nio.file.FileAlreadyExistsException;
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
    private static remoteLog rLog;

    forest(Integer numClasses, Integer numTrees, Integer maxDepth, Integer maxBins) {
        org.apache.log4j.Logger.getLogger("org").setLevel(Level.ERROR);
        org.apache.log4j.Logger.getLogger("akka").setLevel(Level.ERROR);

        forest.log = Logger.getLogger(forest.class.getName());
        forest.rLog = new remoteLog();

        forest.maxBins = maxBins;
        forest.maxDepth = maxDepth;
        forest.numClasses = numClasses;
        forest.numTrees = numTrees;
        seed = new Random().nextInt();

        sparkConf = new SparkConf().setAppName("JavaRandomForestClassificationFire").setMaster("local[4]").set("spark.executor.memory","2g");
        //sparkConf = new SparkConf().setAppName("JavaRandomForestClassificationFire");
        jsc = new JavaSparkContext(sparkConf);
        rLog.send(jsc.getConf().toDebugString() + "\nPARMS: \nnumClasses:" +
                numClasses + "\nnumTrees:" + numTrees + "\nmaxDepth:" + maxDepth +
                "\nmaxBins:" + maxBins + "\nseed:" + seed);
        jsc.setLogLevel("ERROR");

    }

    @Override
    protected void finalize() {
        jsc.stop();
    }

    static public void load(String fileName, String data, String fileAnswer) {
        String to_rLog = "Try load forest model and predict data..." +
                "\nfileName=" + fileName +
                "\ndata=" + data +
                "\nfileAnswer=" + fileAnswer +
                "\n";
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
            to_rLog += "Predict=" + predict.toString();
            try (FileWriter fw = new FileWriter(fileAnswer, true);
                 BufferedWriter bw = new BufferedWriter(fw);
                 PrintWriter out = new PrintWriter(bw))
            {
                out.println(predict.toString());
            } catch (Exception e) {
                to_rLog += ("\nCan not write answer to file!\n" + e.toString());
                log.severe("Can not write answer to file!");
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            to_rLog += "\nNumber of parameters in the learning dataset does not converge with received!\n";
            log.severe("Number of parameters in the learning dataset does not converge with received!");
        }
        rLog.send(to_rLog);
    }

    static public boolean approximeEquals(Tuple2 V) {
        if ((V._1.equals(V._2)) || (V._1.equals((Double)V._2-1.0)) || (V._1.equals((Double)V._2+1.0))) {
            return true;
        }
        return false;
    }

    static public void save(String fileName, String datasetPath) {
        String to_rLog = "Try generate forest..." +
                "\nforestName=" + fileName +
                "\ndatasetPath=" + datasetPath +
                "\n";
        JavaRDD<LabeledPoint> data = MLUtils.loadLibSVMFile(jsc.sc(), datasetPath).toJavaRDD();
        JavaRDD<LabeledPoint>[] splits = data.randomSplit(new double[]{1.0, 0.25});
        JavaRDD<LabeledPoint> trainingData = splits[0];
        JavaRDD<LabeledPoint> testData = splits[1];

        Map<Integer, Integer> categoricalFeaturesInfo = new HashMap<>();
        //String featureSubsetStrategy = "auto";
        String featureSubsetStrategy = "all";
        String impurity = "gini";

        RandomForestModel model = RandomForest.trainClassifier(trainingData, numClasses,
                categoricalFeaturesInfo, numTrees, featureSubsetStrategy, impurity, maxDepth, maxBins,
                seed);

        JavaPairRDD<Double, Double> predictionAndLabel =
                testData.mapToPair(p -> new Tuple2<>(model.predict(p.features()), p.label()));
        double testErr =
                predictionAndLabel.filter(pl -> !pl._1().equals(pl._2())).count() / (double) testData.count();
        double approxErr =
                predictionAndLabel.filter(pl -> !approximeEquals(pl)).count() / (double) testData.count();

        System.out.println("totalError: " + testErr);
        System.out.println("approxError: " + approxErr);
        to_rLog += ("totalError=" + String.valueOf(testErr) + "\n" + "approxError=" + approxErr + "\n");
        try {
            model.save(jsc.sc(), fileName);
        } catch (Exception ex) {
            log.severe("Can't save forest! " + ex.getMessage());
            to_rLog += ("Can't save forest! " + ex.getMessage());
        }
        rLog.send(to_rLog);
    }

    static private String toVectors(String[] foo) {
        String last = foo[foo.length-1];
        Integer iLast = Integer.valueOf(last);
        if (iLast <= 20) {
            iLast = 1;
        } else if (iLast <= 40) {
            iLast = 2;
        } else if (iLast <= 60) {
            iLast = 3;
        } else if (iLast <= 80) {
            iLast = 4;
        } else iLast = 5;

        String ans = iLast.toString();
        for(Integer i = 0; i < foo.length-1; i++) {
            ans += (" " + (i+1) + ":" + foo[i]);
        }
        return ans;
    }

    static private String riskToVectors(String[] foo) {
        String last = foo[foo.length-1];
        Integer iLast = Integer.valueOf(last);
        String ans = iLast.toString();
        for(Integer i = 0; i < foo.length-1; i++) {
            ans += (" " + (i+1) + ":" + foo[i]);
        }
        return ans;
    }

    static public void convertFire(String inputFile) {
        String to_rLog = "Try converting csv dataset..." +
                "\ninputFile=" + inputFile +
                "\n";
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
            to_rLog += "Successfuly!\n";
        } catch (IOException e) {
            to_rLog += ("Can't save corrected dataset!\n" + e.toString() + "\n");
            log.severe("Can't save corrected dataset!");
            e.printStackTrace();
        }

        rLog.send(to_rLog);

        //JavaRDD<String> out = csvColl.
    }

    static public void convertRisk(String inputFile) {
        String to_rLog = "Try converting csv dataset..." +
                "\ninputFile=" + inputFile +
                "\n";
        JavaRDD<String> csv = jsc.textFile(inputFile);
        JavaRDD<String[]> csvCol =  csv.map(s -> s.split(";"));
        JavaRDD<String> out = csvCol.map(sArr -> riskToVectors(sArr));

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
            to_rLog += "Successfuly!\n";
        } catch (IOException e) {
            to_rLog += ("Can't save corrected dataset!\n" + e.toString() + "\n");
            log.severe("Can't save corrected dataset!");
            e.printStackTrace();
        }

        rLog.send(to_rLog);

        //JavaRDD<String> out = csvColl.
    }
}
