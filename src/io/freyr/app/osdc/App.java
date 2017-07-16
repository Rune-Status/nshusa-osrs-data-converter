package io.freyr.app.osdc;
	
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;


public final class App extends Application {
	
	private static Stage stage;
	
	@Override
	public void start(Stage stage) {
		App.stage = stage;
		try {
			Parent root = FXMLLoader.load(App.class.getResource("/ui/Main.fxml"));
			Scene scene = new Scene(root);
			scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
			stage.setTitle("OSRS Data To 317 Converter");		
			stage.centerOnScreen();
			stage.setResizable(false);
			stage.sizeToScene();
			stage.initStyle(StageStyle.UNDECORATED);
			stage.setScene(scene);	
			stage.getIcons().add(new Image(App.class.getResourceAsStream("/icons/icon.png")));
			stage.show();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		launch(args);
	}
	
	public static Stage getStage() {
		return stage;
	}
	
}
