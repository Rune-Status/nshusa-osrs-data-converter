package io.freyr.app.osdc.util;
import java.awt.Desktop;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Optional;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

public final class Dialogue {
	
	private Dialogue() {
		
	}

	public static void openDirectory(String headerText, File dir) {
		Optional<ButtonType> result = Dialogue.showOption(headerText, ButtonType.YES, ButtonType.NO).showAndWait();
		if (result.isPresent()) {
			if (result.get() == ButtonType.YES) {
				try {
					Desktop.getDesktop().open(dir);
				} catch (Exception ex) {
					Dialogue.showException("Error while trying to view image on desktop.", ex);
				}
			}
		}
	}
	
	public static Alert showWarning(String message) {
		Alert alert = new Alert(AlertType.WARNING);
		alert.setTitle("Warning");
		alert.setHeaderText(null);
		alert.setContentText(message);
		return alert;
	}

	public static Alert showInfo(String title, String message) {	
		Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(message);        
        return alert;
	}

	public static Alert showException(String message, Exception ex) {
		Alert alert = new Alert(AlertType.ERROR);
		
		alert.setTitle("Exception");
        alert.setHeaderText("Encountered an Exception");
        alert.setContentText(message);

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        String exceptionText = sw.toString();

        Label label = new Label("The exception stacktrace was:");

        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(label, 0, 0);
        expContent.add(textArea, 0, 1);

        alert.getDialogPane().setExpandableContent(expContent);        
        return alert;
	}
	
	public static Alert showOption(String title, String header, String context, ButtonType ...types) {
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle(title);
		alert.setHeaderText(header);
		alert.setContentText(context);
		
		alert.getButtonTypes().clear();
		alert.getButtonTypes().addAll(Arrays.asList(types)); 
        return alert;
	}
	
	public static Alert showOption(String title, String header, ButtonType... types) {
		return showOption(title, header, "", types);
	}
	
	public static Alert showOption(String header, ButtonType... types) {
		return showOption("Confirmation", header, "", types);
	}
	
	public static TextInputDialog showInput(String title, String context, String text) {
		TextInputDialog input = new TextInputDialog();
		input.setTitle(title);
		input.setHeaderText(null);
		input.setContentText(context);        
        return input;
	}
	
	public static TextInputDialog showInput(String context, String text) {
		return showInput("Input", context, text);
	}

}