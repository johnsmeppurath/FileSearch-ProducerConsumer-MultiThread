package sample;
//I Johns Varughese Meppurath, 000759854 certify that this material is my original work. No other person's work has been used without due acknowledgement. I have not made my work available to anyone else."


import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.File;
import java.io.FileFilter;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class Controller {

    public static volatile Object lock = new Object(); //lock object to wait and notify threads when the producer consumer sequence is over
    public static  volatile int producerNumber=0;      //track the number of producer created


    @FXML
    public    TextArea outputArea; //UI output area

    @FXML
    public TextField SearchDirectory; //UI input area for directory name

    @FXML
    public TextField searchName;  //UI input area for file name

    @FXML
    private Label fileCount;     //UI label to print the number of files

    @FXML
    private Button searchButton; //UI search button to start the process

    private static volatile String output="";  //variable to store the output
    private static final int BOUND = 10;       // Size of blocking queue
    private static final int N_CONSUMERS = Runtime.getRuntime().availableProcessors()/2;  // number of consumer threads for the consumer thread pool( half of the available processors)
    private static final int N_PRODUCERS = Runtime.getRuntime().availableProcessors()/2;  // number of producer threads for the producer thread pool( half of the available processors)
    private static ExecutorService executorConsumer = null;                               //  ExecutorService for Consumer
    private static ExecutorService executorProducer = null;                               //  ExecutorService for Producer
    private static  int numberOfProducerCreated = 10;                                     //  Number of times ExecutorService for Producer is created in the Main Method
    private static String rootName = "";                                                  //  File root name entered by the user
    private static   String pattern ;                                                     //The search file name patter which uses the file name entered by the user

    private static volatile HashSet<File> fileSet = new HashSet<>();                      //Hash set to identify the already indexed files


    private static volatile int fileCountOutput = 0;                                        //variable used to count number of files

          /**
             * FileCrawler class that gets the file folder and it will look for all the files in the given folder .
             * In other words this is the producer function that will find the files in the give folder.The class is Multi threaded , which will help to create
             * multiple producers. The way multi threading was implemented to cover all the files including the file in the root folder was to create produce
             * as much as threads as the subdirectories of the root folder plus a separate thread for the root folder. But the crawl function will
             * identity the root folder and will not invoke the Recursion part but will only look for files , but for all the subdirectories will undergo
             * Recursion until it finds all the files
          */
        static class FileCrawler implements Runnable {
            private final BlockingQueue<File> fileQueue;
            private final FileFilter fileFilter;
            private final File root;

            /**
             * The constructor that initialize the values
             *
             * @param fileQueue  This BlockingQueue will ensure that the producer consumer protocol is properly followed
             *                   that is, BlockingQueue will wait() when the queue is full and notify when the queue has space.The type is declared as  file.
             * @param fileFilter the FileFilter passed form startIndexing function
             * @param root       The root folder  form  File array in the main method . In this case juts we are considering multiple consumers so all main directories and the Home directory
             */
            public FileCrawler(BlockingQueue<File> fileQueue,
                               final FileFilter fileFilter,
                               File root) {
                this.fileQueue = fileQueue;
                this.root = root;
                this.fileFilter = new FileFilter() {
                    public boolean accept(File f) {
                        return f.isDirectory() || fileFilter.accept(f);
                    }
                };
            }

            /**
             * Check weather the file is already indexed using hash set called fileSet. If the file already exits the function will return ture else
             * it file will be saved to hash set and return false
             *
             * @param f passed file
             * @return ture if the file already exits in the set and false if it does not
             */
            private boolean alreadyIndexed(File f) {
                if(fileSet.contains(f)){
                    return true;
                }else {
                    fileSet.add(f);
                    return false;
                }

            }


            /**
             * run method , which will be called when this thread starts. The run method is calling crawl function and passing the root as the parameter
             * After all the files are produced the run method will enter into the sync block with object lock and will notify the main method that
             * all the production threads are done producing.
             *
             * This will be achieved by tracking the number of producer created matches the number of
             * threads which is suppose to be produced. When the last producer thread is invoked thread crawl function is called and after that
             * the thread will sleep for 200 mill seconds in order for the consumer to consume the last produced data.When the sleep period is over
             * the notify method will tell the main thread , that the production and consuming is over
             */
            public void run() {
                try {
                    //Crawl method is called and root folder is passed as parameter

                    crawl(root);
                    producerNumber++;
                   if(producerNumber== numberOfProducerCreated){
                       Thread.sleep(200);
                       System.out.println("notify block");
                       synchronized (lock){
                           lock.notify();
                       }
                   }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            /**
             * Crawl function which will first look if the passed file is the Main root directory, and if true the crawl function will only
             * look for files, if it is a subdirectory the recursion part of crawl function will be invoked, this way multiple producer threads can be
             * invoked which also looks for files in root directory. So number of producers are the sum of all the directories plus the root directory
             *
             * @param root the folder passed to check for files
             * @throws InterruptedException
             */
            private synchronized void crawl(File root) throws InterruptedException {
                if (!root.toString().equals(rootName)) {

                    File[] entries = root.listFiles(fileFilter);
                    if (entries != null) {
                        for (File entry : entries)
                            if (entry.isDirectory()) {


                                crawl(entry);
                                // }

                            } else if (!alreadyIndexed(entry)) {

                                fileQueue.put(entry);
                                System.out.println("producer produced data= " + entry);
                            }
                    }
                }
                else {
                    File[] entries = root.listFiles(fileFilter);
                    if (entries != null) {
                        for (File entry : entries)
                            if (entry.isDirectory()) {

                            } else if (!alreadyIndexed(entry)) {

                                fileQueue.put(entry);
                                System.out.println("producer produced data= " + entry);
                            }
                    }
                }
            }
        }

        /**
         * Indexer class which consumes the FileCrawler class. The class is Multi threaded , Multiple threads are active to consume the produced data
         */
        static class Indexer implements Runnable {
            private final BlockingQueue<File> queue;


            /**
             * constructor assigning the the queue's value
             *
             * @param queue
             */
            public Indexer(BlockingQueue<File> queue) {
                this.queue = queue;
            }

            /**
             * run method that class the indexfile function which uses the consumed result to print the text files .
             * */
            public void run() {

                try {
    //Loop that consumes that data
                    while (true) {

                      File currentFile = queue.take();
                        indexFile(currentFile);

                    }


                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            /**
             * indexFile function that will keep the count of consumed items and printing the consumed results, if the given pattern matches the output.
             * The patter is the user input so user has the ability to search the files they need.
             *
             * @param file
             */
            public synchronized void indexFile(File file) {

                if (file != null) {

                        if(file.getName().matches(pattern)) {
                            fileCountOutput++;
                            System.out.println("consumed files. " + file + " and count is = " + fileCountOutput);
                            output = output + file + "\n";

                        }

                }else {
                    System.out.println("Null");
                }

            }

        }

            /**
             * UI onClick method will invoke the entire process. The functions gets the all the user inputs and passes the information to create mutiple
             * producers and consumers to read the files using thread pools. Also the last production thread will notify this class after given some time for
             * the consumer to consume the last data that the process ends and the function will shutdown all the executors and printout the right output
             * to the UI. Also the function will do nothing if the given root folder name is empty or wrong, the file name which need to be searched left empty
             * all the file names will be displayed without any filters
             * @param event
             * @throws InterruptedException
             */

            @FXML
            void onClick(ActionEvent event) throws InterruptedException {
                //Setting global variables to default vales when the search button is clicked
                fileSet.clear();
                fileCountOutput =0;
                output="";
                String filename = "";
                producerNumber=0;
                boolean isRegex = false;
                pattern="";

                searchButton.setDisable(true); // make sure that button is disabled until all the process is over

                executorConsumer = Executors.newFixedThreadPool(N_CONSUMERS); //Consumer executor for consumer threads
                executorProducer = Executors.newFixedThreadPool(N_PRODUCERS); //Producer executor for consumer threads
                if(SearchDirectory.getText().equals("")){ //if the file name is empty button is enabled to repeat the work
                    searchButton.setDisable(false);

                    return;

                }else {                                    // if the file name is not empty the process starts
                    try {
                        //gets the root folder name and search file name from UI
                        File[] roots = new File[1];

                        roots[0] = new File(SearchDirectory.getText());

                     //  pattern = (".*\\.*("+searchName.getText()+")");
                        pattern = searchName.getText();
                        if(pattern.toString().equals("")){
                            pattern=".*";
                        }

                        BlockingQueue<File> queue = new LinkedBlockingQueue<File>(BOUND);//blocking que with size 10

                        //file filter to identify the files and directories
                        FileFilter filter = new FileFilter() {
                            public boolean accept(File file) {
                                return true;
                            }
                        };


                        //logging the consumer and producer thread pool size
                        System.out.println(N_CONSUMERS);
                        System.out.println(N_PRODUCERS);

                        String[] pathNames;
                        pathNames = roots[0].list();//finding all the subdirectories for the root file
                        numberOfProducerCreated =pathNames.length+1;  //number of Producer that will be Created is total primary subdirectories plus one(including the root directory itself)
                         rootName=roots[0].toString();                //saving the root name into a global variable to check in the crawl function weather to do the  Recursion function or not

                        // For each pathname in the pathname array creates a new producer thread using thread pool
                        for (String pathname : pathNames) {
                            System.out.println(pathname);
                            // Print the names of files and directories
                            Runnable Producer = new FileCrawler(queue, filter, new File(rootName+"\\"+pathname));
                            executorProducer.execute(Producer);
                            // System.out.println(pathname);
                        }
                        // producer thread using thread pool for the root directory
                        Runnable Producer = new FileCrawler(queue, filter, new File(rootName));
                        executorProducer.execute(Producer);


                        //Multiple consumer threads are invoked
                        for (int i = 0; i < N_CONSUMERS; i++) {
                            Runnable worker = new Indexer(queue);
                            executorConsumer.execute(worker);

                        }

                        //sync block that will wait until all the producing and consuming ends.
                        // When this block is notified threads are closed and output is  printed in UI using Platform.runLater method

                        synchronized (lock){
                            lock.wait();
                            executorConsumer.shutdownNow();
                            executorProducer.shutdownNow();
                            System.out.println("Run complete");
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        outputArea.setText(output);
                                        fileCount.setText("" + fileCountOutput);
                                    } catch (Exception e) {

                                    }
                                }

                            });
                            searchButton.setDisable(false);
                        }


                    } catch (Exception e) {
                        searchButton.setDisable(false);
                    }
                }

            }


    }
