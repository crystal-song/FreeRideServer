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

        System.out.println("New task command line interface. Type 'exit' to quit");
        System.out.println("or 'create' to generate tasks and add them to the database.");
        Scanner scanner = new Scanner(System.in);
//        System.out.print(">");
        String userInput = scanner.nextLine();

        while (!userInput.equalsIgnoreCase("exit")) {
            switch (userInput) {
                case "":
                    System.out.println("Type 'exit' to quit.");
                    break;
                case "create":
                    System.out.println("How many tasks to create? Enter '0' to cancel.");
//                    System.out.print(">");
                    String amountString = scanner.nextLine();
                    Integer amount = Integer.parseInt(amountString);
                    if (amount > 0) {
                        System.out.println("Choose tasks state? ('new' or 'available') Enter \"\" to cancel.");
//                        System.out.print(">");
                        String state = scanner.nextLine();
                        if (state.equals("new") || state.equals("available")) {
                            Main.addTasksArrayToDatabase(generateRandomTasks(amount, state));
                            System.out.println("Added " + amount + " " + state + " tasks to database.");
                        } else {
                            System.out.println(state + " not a valid task state. \uD83D\uDC80 ");
                        }
                    }
                    break;
                case "info":
                    System.out.println("Explaining things. Look unicode: ☠");
                default:
                    break;
            }
            System.out.println("New task command line interface. Type 'exit' to quit");
            System.out.println("or 'create' to generate tasks and add them to the database.");
//            System.out.print(">");
            userInput = scanner.nextLine();
        }



//        Task dummyTask = new Task(50.1341, -1.4082, 50.0832, -1.4028,
//                LocalDateTime.now().toString(), null, "Title Dummy",
//                "Description of dummy task. It's tree fiddy on completion.", "new", null, 3.50);
//        String taskJson = new Gson().toJson(dummyTask);
//        System.out.println("Task Format: ");
//        System.out.println(taskJson);

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
                gaussianRandom(51, 1, true),
                gaussianRandom(-1, 1, false),
                gaussianRandom(51, 1, true),
                gaussianRandom(-1, 1, false),
                LocalDateTime.now().toString(),
                null,
                "Generated",
                "Randomly generated task values.",
                state,
                null,
                gaussianRandom(10, 2.3, false));
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

    private static double gaussianRandom(double mean, double variance, boolean oneDirection){ //well that variable name will have to be changed
        double diffFromMean = new Random().nextGaussian() * variance;
        if (oneDirection) {
            diffFromMean = Math.abs(diffFromMean);
        }
        double result = mean + diffFromMean;
        BigDecimal bd = new BigDecimal(result);
        bd = bd.setScale(5, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
