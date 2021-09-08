package ovgu.creasy.ui;

import javafx.application.HostServices;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import ovgu.creasy.Main;
import ovgu.creasy.origami.*;
import ovgu.creasy.origami.oripa.OripaFoldedModelWindow;
import ovgu.creasy.util.TextLogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class MainWindow {

    private static final int CANVAS_WIDTH = 200;
    private static final int CANVAS_HEIGHT = 200;

    private final FileChooser openFileChooser;

    @FXML
    private MenuItem onGridIncrease;
    @FXML
    private MenuItem onGridDecrease;
    @FXML
    private MenuItem onCustomGridSize;

    @FXML
    private ScrollPane canvasHolder;

    private HostServices hostServices;
    private OrigamiModel model;
    private CreasePattern cp;

    public ResizableCanvas mainCanvas;

    @FXML
    private MenuItem foldedModelMenuItem;
    @FXML
    private MenuItem zoomInMenuItem;
    @FXML
    private MenuItem zoomOutMenuItem;
    @FXML
    private MenuItem resetMenuItem;
    @FXML
    private Menu exportMenu;

    @FXML
    private TextArea log;
    @FXML
    private VBox history;
    @FXML
    private VBox steps;

    public MainWindow() {
        openFileChooser = new FileChooser();
        openFileChooser.setTitle("Open .cp File");
        openFileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Crease Patterns", "*.cp"));
    }

    /* This makes sure that the window scales correctly
       by binding the canvas to the window size and redrawing if
       necessary.
     */
    @FXML
    public void initialize() {
        mainCanvas = new ResizableCanvas(1000, 1000);
        mainCanvas.setId("main");
        mainCanvas.setManaged(false);

        mainCanvas.getGraphicsContext2D().setFill(Color.WHITE);
        mainCanvas.getGraphicsContext2D().fillRect(0, 0, 1000,1000);
        mainCanvas.drawGrid();

        canvasHolder.setContent(mainCanvas);

        //mainCanvas.widthProperty().bind(canvasHolder.widthProperty());
        //mainCanvas.heightProperty().bind(canvasHolder.heightProperty());

        mainCanvas.widthProperty().addListener((observableValue, number, t1) -> {
            if (cp != null) {
                cp.drawOnCanvas(mainCanvas, 1, 1, 50);
            }
        });

        mainCanvas.heightProperty().addListener((observableValue, number, t1) -> {
            if (cp != null) {
                cp.drawOnCanvas(mainCanvas, 1, 1, 50);
            }
        });

        TextLogger.logText("Starting up... Welcome to " + Main.APPLICATION_TITLE + " v0.1.0!", log);
    }

    /**
     * Opens a file explorer dialogue which lets the user select
     * .cp files and upon loading them, calls setupGUI() -- drawing the
     * Pattern to the screen.
     */
    @FXML
    public void onMenuImportAction() {
        File file = openFileChooser.showOpenDialog(mainCanvas.getScene().getWindow());
        var filePath = file == null ? "" : file.getPath();

        if (file != null && file.exists()) {
            try {
                resetGUI();
                setupCreasePattern(new FileInputStream(file), filePath);
                TextLogger.logText("Import " + filePath, log);
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Error loading file " + filePath + "!");
            }
        } else {
            System.err.println("No file selected or path is invalid!");
        }
    }

    /**
     * Opens a file explorer dialogue which lets the user export
     * the .cp file
     *
     */
    @FXML
    public void onMenuExportAction() {

    }

    /**
     * Opens Oripa with the folded 3d model,
     * calls foldModel() method.
     *
     * Opens an Alert in case of an error while folding the model.
     */
    @FXML
    public void onShowFoldedModelAction() {
        if (model == null) {
            Alert error = new Alert(Alert.AlertType.ERROR,
                    "There is no model to fold, perhaps it wasn't loaded correctly", ButtonType.OK);
            error.setTitle("Model is not loaded");
            error.setHeaderText("Error while folding model");
            error.showAndWait();
        } else {
            OripaFoldedModelWindow foldedModelWindow = new OripaFoldedModelWindow(model.getFinishedCp());
            if (foldedModelWindow.foldModel()) {
                foldedModelWindow.show();
            } else {
                foldedModelWindow.showError();
                System.err.println("Crease Pattern is invalid");
            }
        }
    }

    @FXML
    public void onZoomInMenuItem() {
        CreasePattern mainCP = mainCanvas.getCp();
        mainCP.drawOnCanvas(mainCanvas, 1.1 * mainCanvas.getCpScaleX(), 1.1 * mainCanvas.getCpScaleY());
    }

    @FXML
    public void onZoomOutMenuItem() {
        CreasePattern mainCP = mainCanvas.getCp();
        mainCP.drawOnCanvas(mainCanvas, 0.9 * mainCanvas.getCpScaleX(), 0.9 * mainCanvas.getCpScaleY());
    }

    @FXML
    public void onGridIncreaseAction() {
        // TODO
    }

    @FXML
    public void onMenuResetAction() {
        resetGUI();
    }

    // -------------------------
    // Loading example files
    @FXML
    public void onLoadExampleBird() {
        resetGUI();
        InputStream is = Main.class.getResourceAsStream("example/bird.cp");
        setupCreasePattern(is, "example/bird.cp");
        TextLogger.logText("Import example/bird.cp", log);
    }

    @FXML
    public void onLoadExamplePenguin() {
        resetGUI();
        InputStream is = Main.class.getResourceAsStream("example/penguin_hideo_komatsu.cp");
        setupCreasePattern(is, "example/penguin_hideo_komatsu.cp");
        TextLogger.logText("Import example/penguin_hideo_komatsu.cp", log);
    }

    @FXML
    public void onLoadExampleCrane() {
        resetGUI();
        InputStream is = Main.class.getResourceAsStream("example/crane.cp");
        setupCreasePattern(is, "example/crane.cp");
        TextLogger.logText("Import example/crane.cp", log);
    }
    // -------------------------

    /**
     * Opens an "about" dialogue which displays information about Creasy
     * and its developers
     */
    @FXML
    public void onHelpAbout() {
        AboutWindow.open(this.hostServices);
    }

    /**
     * Opens a help dialogue explaining what crease patterns are, what they
     * are used for and explaining the different types of folds
     */
    @FXML
    public void onHelpCP() {
        CreasePatternHelpWindow.open();
    }

    /**
     * Loads a Crease Pattern, displays it on the canvases and
     * initializes variables
     * @param is the InputStream that is the crease pattern file
     * @param filePath what is displayed in the title bar of the window
     */
    private void setupCreasePattern(InputStream is, String filePath) {
        cp = CreasePattern.createFromFile(is);
        ((Stage) mainCanvas.getScene().getWindow()).setTitle(filePath + " - " + Main.APPLICATION_TITLE);

        model = new OrigamiModel(cp);

        cp.drawOnCanvas(mainCanvas, 1, 1);

        ExtendedCreasePattern ecp = new ExtendedCreasePatternFactory().createExtendedCreasePattern(cp);
        System.out.println("size = " + ecp.possibleSteps().size());

        // should be called when the algorithm is executed, aka once the amount of steps is known
        createCanvases(steps, ecp.possibleSteps().size(), CANVAS_WIDTH, CANVAS_HEIGHT);

        drawSteps(ecp, steps);
        drawHistory(cp, history);

        setupMouseEvents(steps, history);

        // after reading the file, if the file is valid:
        foldedModelMenuItem.setDisable(false);
        zoomInMenuItem.setDisable(false);
        zoomOutMenuItem.setDisable(false);
        resetMenuItem.setDisable(false);
        exportMenu.setDisable(false);
    }

    private void drawSteps(ExtendedCreasePattern ecp, Parent steps) {
        for (int i = 0; i < ecp.possibleSteps().size(); i++) {
            DiagramStep step = ecp.possibleSteps().get(i);
            step.to.toCreasePattern().drawOnCanvas((ResizableCanvas) ((Pane) steps).getChildren().get(i),
                    0.45, 0.45, 0);
        }
    }

    private void drawHistory(CreasePattern cp, Parent history) {
        createCanvases(history, 1, CANVAS_WIDTH, CANVAS_HEIGHT);
        ((Pane) history).getChildren().forEach(c -> {
            if (((ResizableCanvas) c).getCp() == null) {
                cp.drawOnCanvas((ResizableCanvas) c, 0.45, 0.45, 0);
            }
        });
    }

    private void setupMouseEvents(Parent... parents) {
        for (Parent parent : parents) {
            ((Pane) parent).getChildren().forEach(c -> {
                GraphicsContext graphicsContext = ((Canvas) c).getGraphicsContext2D();
                c.setOnMouseEntered(mouseEvent -> {
                    graphicsContext.setFill(Color.color(0.2, 0.2, 0.2, 0.2));
                    graphicsContext.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
                    c.setCursor(Cursor.HAND);

                    CreasePattern diff;
                    if (c.getParent().equals(history)) {
                        diff = ((ResizableCanvas) c).getCp().getDifference(mainCanvas.getCp());
                    } else {
                        diff = mainCanvas.getCp().getDifference(((ResizableCanvas) c).getCp());
                    }
                    diff.drawOverCanvas(mainCanvas, mainCanvas.getCpScaleX(), mainCanvas.getCpScaleY());
                });

                c.setOnMouseExited(mouseEvent -> {
                    graphicsContext.clearRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
                    ((ResizableCanvas) c).getCp().drawOnCanvas((ResizableCanvas) c, 0.45, 0.45, 0);
                    c.setCursor(Cursor.DEFAULT);

                    mainCanvas.getCp().drawOnCanvas(mainCanvas, mainCanvas.getCpScaleX(), mainCanvas.getCpScaleY());
                });

                c.setOnMouseClicked(mouseEvent -> {
                    if (mouseEvent.getButton() == MouseButton.SECONDARY) {
                        if (c.getParent().equals(history)) {
                            ContextMenu contextMenu = new ContextMenu();
                            MenuItem delete = new MenuItem("Delete");

                            delete.setOnAction(actionEvent -> history.getChildren().remove(c));
                            contextMenu.getItems().add(delete);

                            c.setOnContextMenuRequested(contextMenuEvent -> {
                                contextMenu.show(c, contextMenuEvent.getScreenX(), contextMenuEvent.getScreenY());
                            });
                        }
                    } else {
                        var startCP = cp.copy();
                        var currentStep = ((ResizableCanvas) c).getCp();

                        System.out.println(history.getChildren().size());
                        System.out.println(currentStep.equals(cp) ? "equals" : "not equals");

                        currentStep.drawOnCanvas(mainCanvas, 1, 1);
                        ExtendedCreasePattern ecp = new ExtendedCreasePatternFactory().createExtendedCreasePattern(currentStep);

                        if (c.getParent().equals(steps)) {
                            drawHistory(currentStep, history);
                            TextLogger.logText("Pick this step and add to history... " + ecp.possibleSteps().size() + " new option(s) were calculated", log);
                        }

                        steps.getChildren().clear();

                        createCanvases(steps, ecp.possibleSteps().size(), CANVAS_WIDTH, CANVAS_HEIGHT);
                        setupMouseEvents(steps, history);
                        drawSteps(ecp, steps);
                    }
                });
            });
        }
    }

    /**
     * Clears all the canvases and disables menu options that would only work
     * with a loaded file.
     * Useful for resetting state.
     */
    private void resetGUI() {
        mainCanvas.getGraphicsContext2D().setFill(Color.WHITE);
        mainCanvas.getGraphicsContext2D().fillRect(0, 0, mainCanvas.getWidth(), mainCanvas.getHeight());
        ((Stage) mainCanvas.getScene().getWindow()).setTitle(Main.APPLICATION_TITLE);

        steps.getChildren().clear();
        history.getChildren().clear();

        foldedModelMenuItem.setDisable(true);
        zoomInMenuItem.setDisable(true);
        zoomOutMenuItem.setDisable(true);
        resetMenuItem.setDisable(true);
        exportMenu.setDisable(true);

        cp = null;
    }

    /**
     * Creates new canvases in the sidebar when necessary to show
     * the steps needed
     *
     * @param parent the VBox or HBox in which to add the Canvases to
     * @param amount the amount of Canvases needed
     * @param width  width of the Canvas
     * @param height height of the Canvas
     */
    private void createCanvases(Parent parent, int amount, int width, int height) {
        System.out.println("amount = " + amount);
        for (int i = 0; i < amount; ++i) {
            System.out.println("i = " + i);
            ((Pane) parent).getChildren().add(new ResizableCanvas(width, height));
        }
    }

    public void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }
}
