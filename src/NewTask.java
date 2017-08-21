import FCM.FcmServer;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NewTask {
    private static final Logger logger = Logger.getLogger(FcmServer.class.getName());

    /**
     * Reads task data from file, add these tasks to database
     * If no file given, will print out example format of task to use
     * @param args name of file to read task data from
     */
    public static void main(String[] args) {
        //If no file given as argument to read, print out task format
        if (args.length == 0) {
            Task dummyTask = new Task(50.1341, -1.4082, 50.0832, -1.4028, LocalDateTime.now().toString(), null,
                    "Title Dummy", "Description of dummy task. It's tree fiddy on completion.", "NEW", null, 3.50);
            String taskJson = new Gson().toJson(dummyTask);
            logger.log(Level.INFO, "Task Format: ");
            logger.log(Level.INFO, taskJson);
            return;
        } else if (args[0].startsWith("create")) {
            //TODO create dummy tasks
            return;
        }

        logger.log(Level.INFO, "Reading tasks from file: " + args[0]);
        File inputFile = null;
        BufferedReader reader = null;
        Gson gson = new Gson();
        try {
            inputFile = new File(args[0]);

            reader = Files.newBufferedReader(inputFile.toPath());
            String line = null;
            while ((line = reader.readLine()) != null) {
                logger.log(Level.INFO, "reading line: " + line);
                Task newTask = gson.fromJson(line, Task.class);
                Main.addTaskToDatabase(newTask);
                Main.addTaskToDatabase(line);
            }

            //TODO if incorrect format, print out example format





        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            }
            catch (IOException ioe)
            {
                System.out.println("Error in closing the BufferedReader");
            }
        }
    }

    /**
     * Location and incentive are randomly generated using guassian distribution.
     * @return generated task
     */
    public static Task generateRandomTask() {
        logger.log(Level.INFO, "Generating Random Task");

        Task dummyTask = new Task(
                gaussianRandom(50, 1),
                gaussianRandom(-1.4, 1),
                gaussianRandom(50, 1),
                gaussianRandom(-1.4, 1),
                LocalDateTime.now().toString(),
                null,
                "Generated",
                "Randomly generated task values.",
                "NEW",
                null,
                gaussianRandom(10, 2.3));
        String taskJson = new Gson().toJson(dummyTask);
        logger.log(Level.INFO, "Task Format: ");
        logger.log(Level.INFO, taskJson);
        return dummyTask;
    }

    private static double gaussianRandom(double mean, double variance){
        double result = mean + new Random().nextGaussian() * variance;
        BigDecimal bd = new BigDecimal(result);
        bd = bd.setScale(5, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
