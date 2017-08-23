import FCM.FcmServer;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;
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
        logger.log(Level.INFO, "RUNNING NEW TASK ");


        if (args.length == 0) {
            Task dummyTask = new Task(50.1341, -1.4082, 50.0832, -1.4028, LocalDateTime.now().toString(), null,
                    "Title Dummy", "Description of dummy task. It's tree fiddy on completion.", "new", null, 3.50);
            String taskJson = new Gson().toJson(dummyTask);
            logger.log(Level.INFO, "Task Format: ");
            logger.log(Level.INFO, taskJson);
            return;
        } else if (args[0].startsWith("create")) {
            Main.addTaskToDatabase(generateRandomTask());
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

    public static void CLI() {
        //If no file given as argument to read, print out task format
        logger.log(Level.INFO, "RUNNING NEW TASK.");
        Scanner scanner = new Scanner(System.in);
        String userInput = scanner.nextLine();
        String[] userInputTokens = userInput.split(" ");

        logger.log(Level.INFO, "[" + userInput + "]");
        switch (userInputTokens[0]) {
            case "":
                Task dummyTask = new Task(50.1341, -1.4082, 50.0832, -1.4028,
                        LocalDateTime.now().toString(), null, "Title Dummy",
                        "Description of dummy task. It's tree fiddy on completion.", "new", null, 3.50);
                String taskJson = new Gson().toJson(dummyTask);
                logger.log(Level.INFO, "Task Format: ");
                logger.log(Level.INFO, taskJson);
                break;
            case "create":
                switch (userInputTokens[1]) {
                    case "available":
                        Main.addTaskToDatabase(generateRandomTask("available"));
                        break;
                    default:
                        Main.addTaskToDatabase(generateRandomTask());
                }
                break;
            default:
                break;
        }
//
//        logger.log(Level.INFO, "Reading tasks from file: " + userInput);
//        File inputFile = null;
//        BufferedReader reader = null;
//        Gson gson = new Gson();
//        try {
//            inputFile = new File(userInput);
//            reader = Files.newBufferedReader(inputFile.toPath());
//            String line = null;
//            while ((line = reader.readLine()) != null) {
//                logger.log(Level.INFO, "reading line: " + line);
//                Task newTask = gson.fromJson(line, Task.class);
//                Main.addTaskToDatabase(newTask);
//                Main.addTaskToDatabase(line);
//            }//
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            try {
//                if (reader != null) {
//                    reader.close();
//                }
//            }
//            catch (IOException ioe)
//            {
//                System.out.println("Error in closing the BufferedReader");
//            }
//        }
    }

    public static Task generateRandomTask() {
        return generateRandomTask("new");
    }

    /**
     * Location and incentive are randomly generated using guassian distribution.
     * @return generated task
     */
    public static Task generateRandomTask(String state) {
        logger.log(Level.INFO, "Generating Random Task");

        Task newTask = new Task(
                gaussianRandom(50, 1),
                gaussianRandom(-1.4, 1),
                gaussianRandom(50, 1),
                gaussianRandom(-1.4, 1),
                LocalDateTime.now().toString(),
                null,
                "Generated",
                "Randomly generated task values.",
                state,
                null,
                gaussianRandom(10, 2.3));
        return newTask;
    }

    /**
     * Returns an Arraylist of a specified amount of randomly generated tasks.
     * Location and incentive are randomly generated using guassian distribution.
     * @param amount of tasks to generate and return
     * @return list of tasks generated
     */
    public static ArrayList<Task> generateRandomTasks(int amount) {
        logger.log(Level.INFO, "Generating " + amount + " Random Tasks");

        ArrayList<Task> tasks = new ArrayList<>();
        for (int i=0; i<amount; i++) {
            tasks.add(generateRandomTask("new"));
        }
        return tasks;
    }

    /**
     * Returns an Arraylist of a specified amount of randomly generated tasks.
     * State specified the state of the task when created.
     * Valid states: 'new', 'available'. Also: 'accepted', 'pending', 'completed'.
     * todo validation. add states as consts (to VALUES)
     * todo make task Builder
     * Location and incentive are randomly generated using guassian distribution.
     * @param amount of tasks to generate and return
     * @param state of generated tasks
     * @return list of tasks generated
     */
    public static ArrayList<Task> generateRandomTasks(int amount, String state) {
        logger.log(Level.INFO, "Generating " + amount + " Random Tasks");

        ArrayList<Task> tasks = new ArrayList<>();
        for (int i=0; i<amount; i++) {
            tasks.add(generateRandomTask(state));
        }
        return tasks;
    }

    private static double gaussianRandom(double mean, double variance){
        double result = mean + new Random().nextGaussian() * variance;
        BigDecimal bd = new BigDecimal(result);
        bd = bd.setScale(5, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
