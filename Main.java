package sample;
//I Johns Varughese Meppurath, 000759854 certify that this material is my original work. No other person's work has been used without due acknowledgement. I have not made my work available to anyone else."


import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileFilter;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Main extends Application {



    @Override
    public void start(Stage primaryStage) throws Exception{

        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        primaryStage.setTitle("Hello World");
        primaryStage.setScene(new Scene(root, 700, 475));
        primaryStage.show();

System.out.println("ssssss");

    }


    public static void main(String[] args) {
        launch(args);
    }
}
