package edu.lab.mit.gui;

import edu.lab.mit.utils.Utilities;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Control;
import javafx.scene.control.Labeled;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * <p>Project: KEWILL FORWARD ENTERPRISE</p>
 * <p>File: edu.lab.mit.gui.Launcher</p>
 * <p>Copyright: Copyright � 2015 Kewill Co., Ltd. All Rights Reserved.</p>
 * <p>Company: Kewill Co., Ltd</p>
 *
 * @author <chao.deng@kewill.com>
 * @version 1.0
 * @since 7/30/2015
 */
public class Launcher extends Application {

    private static Logger logger = LogManager.getLogger(Launcher.class);
    @Override
    public void start(Stage primaryStage) throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        FXMLLoader loader = new FXMLLoader(classLoader.getResource("config/layout.fxml"));
        final Parent root = Parent.class.cast(loader.load());
        final Controller controller = Controller.class.cast(loader.getController());

        Scene scene = new Scene(root, 1200, 820);
        scene.getStylesheets().add("/config/styles.css");
        primaryStage.setOnCloseRequest(
            windowEvent -> {
                logger.info("System exit!");
                primaryStage.hide();
                controller.exit();
            });
        scene.widthProperty().addListener((observable, oldValue, newValue) -> {
            double delta = newValue.doubleValue() - oldValue.doubleValue();
            ObservableList<Node> components = Pane.class.cast(root).getChildren();
            if (components != null && components.size() > 0) {
                components.parallelStream()
                    .filter(
                        node -> node.isResizable() && !Labeled.class.isAssignableFrom(node.getClass()) && Control.class
                            .isAssignableFrom(node.getClass()))
                    .forEach(node -> Utilities.adjustSize(node, "Width", delta));
            }
        });
        scene.heightProperty().addListener((observable, oldValue, newValue) -> {
            double delta = newValue.doubleValue() - oldValue.doubleValue();
            ObservableList<Node> components = Pane.class.cast(root).getChildren();
            if (components != null && components.size() > 0) {
                components.parallelStream()
                    .filter(node -> node.isResizable() && TableView.class.isAssignableFrom(node.getClass()))
                    .forEach(node -> Utilities.adjustSize(node, "Height", delta));
            }
        });
        primaryStage.setTitle("Error Log Analyze Tool");
        Image icon = new Image(classLoader.getResourceAsStream("images/gear_tools.png"));
        primaryStage.getIcons().add(icon);
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.show();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}
