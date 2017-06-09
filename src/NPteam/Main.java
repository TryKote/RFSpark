package NPteam;

import javassist.NotFoundException;
import org.apache.spark.sql.catalyst.plans.logical.Except;
//import org.apache.log4j.Level;
import java.util.*;
import java.util.logging.Logger;

import static java.lang.System.exit;

public class Main {
    public static void main(String[] args) {
        Logger log = Logger.getLogger(Main.class.getName());
        remoteLog rLog = new remoteLog("http://TryKote.suroot.com/");

        config cfg = new config("config");

        System.out.println("" +
                "------------------------------------------------------------\n" +
                "|                Predict module by TryKote                 |\n" +
                "|                                                          |\n" +
                "|                      Instructions:                       |\n" +
                "| -> loadForest=<forest name> data=<double>,<double>, ...  |\n" +
                "| -> saveForest=<forest name> dataset=<dataset(vectors)>   |\n" +
                "| -> convert=<dataset.csv>                                 |\n" +
                "------------------------------------------------------------\n");

        //-----------------GET ALL PARAMETERS------------------------
        Map<String, String> inputArgs = new HashMap<>();
        if (args.length > 0) {
            for (String arg : args) {
                if (arg.contains("=")) {
                    inputArgs.put(arg.split("=")[0], arg.split("=")[1]);
                }
            }
        }
        //-----------------GET ALL PARAMETERS END------------------------

        //-----------------SET GENERAL PARAMETERS------------------
        Integer numClasses;   //4
        Integer numTrees;  //16
        Integer maxDepth;
        Integer maxBins;    //16

        try {
            numClasses = Integer.valueOf(inputArgs.get("numClasses"));
        } catch (Exception e) {
            numClasses = 4;
        }

        try {
            numTrees = Integer.valueOf(inputArgs.get("numTrees"));
        } catch (Exception e) {
            numTrees = 16;
        }

        try {
            maxDepth = Integer.valueOf(inputArgs.get("maxDepth"));
        } catch (Exception e) {
            maxDepth = 5;
        }

        try {
            maxBins = Integer.valueOf(inputArgs.get("maxBins"));
        } catch (Exception e) {
            maxBins = 16;
        }

        try {
            numClasses = Integer.valueOf(cfg.load("numClasses"));
        } catch (Exception ignored) {}
        try {
            numTrees = Integer.valueOf(cfg.load("numTrees"));
        } catch (Exception ignored) {}
        try {
            maxDepth = Integer.valueOf(cfg.load("maxDepth"));
        } catch (Exception ignored) {}
        try {
            maxBins = Integer.valueOf(cfg.load("maxBins"));
        } catch (Exception ignored) {}

        cfg.recreate();
        cfg.save("numClasses", numClasses.toString(), "How much classifications answer");
        cfg.save("numTrees", numTrees.toString(), "How much trees");
        cfg.save("maxDepth", maxDepth.toString(), "Accuracy");
        cfg.save("maxBins", maxBins.toString(), "How much bins");

        String fileAnswer = "fileAnswer.txt";
        if (inputArgs.containsKey("fileAnswer")) {
            if (!inputArgs.get("fileAnswer").isEmpty()) {
                fileAnswer = inputArgs.get("fileAnswer");
            }
        }
        //-----------------SET GENERAL PARAMETERS END------------------


        forest generalForest = new forest(numClasses, numTrees, maxDepth, maxBins);

        String datasetPath = new String();
        if (inputArgs.containsKey("saveForest")) {
            if (inputArgs.containsKey("dataset")) {
                if (!inputArgs.get("dataset").isEmpty()) {
                    datasetPath = inputArgs.get("dataset");
                } else {
                    try {
                        throw new NotFoundException("DATASET NOT FOUND!");
                    } catch (Exception e) {
                        log.severe("Can not calculate forest: DATASET NOT FOUND!");
                        exit(2);
                    }
                }
            }
        }

        if (inputArgs.containsKey("convert")) {
            if (!inputArgs.get("convert").isEmpty()) {
                generalForest.convert(inputArgs.get("convert"));
            }
        }

        //data=1.0,2.0,3.0,4.0
        if (inputArgs.containsKey("loadForest")) { //Load pregenerated forest from file
            if (!inputArgs.get("loadForest").isEmpty()) {
                generalForest.load(inputArgs.get("loadForest"), inputArgs.get("data"), fileAnswer);
            }
        }

        // Save and load model
        if (inputArgs.containsKey("saveForest")) {
            if (!inputArgs.get("saveForest").isEmpty()) {
                generalForest.save(inputArgs.get("saveForest"), datasetPath);
            }
        }
        generalForest.finalize();
    }
}
