package ovgu.creasy.ui;

import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import ovgu.creasy.Main;
import ovgu.creasy.geom.Line;
import ovgu.creasy.geom.Point;
import ovgu.creasy.origami.*;
import ovgu.creasy.origami.oripa.OripaFoldedModelWindow;
import ovgu.creasy.util.CreasePatternEditor;
import ovgu.creasy.util.TextLogger;
import ovgu.creasy.util.exporter.cp.PDFCreasePatternExporter;
import ovgu.creasy.util.exporter.cp.PNGCreasePatternExporter;
import ovgu.creasy.util.exporter.cp.SVGCreasePatternExporter;
import ovgu.creasy.util.exporter.history.PDFHistoryExporter;
import ovgu.creasy.util.exporter.history.SVGHistoryExporter;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static ovgu.creasy.ui.ResizableCanvas.CANVAS_HEIGHT;
import static ovgu.creasy.ui.ResizableCanvas.CANVAS_WIDTH;

public class MainWindow {

    @FXML
    private ToggleGroup edit;
    private ResizableCanvas activeHistory;

    @FXML
    private ScrollPane canvasHolder;
    @FXML
    private VBox window;
    @FXML
    private Label historyLabel;

    private HostServices hostServices;
    private OrigamiModel model;
    private CreasePattern cp;

    private List<ResizableCanvas> historyCanvasList;
    private List<ResizableCanvas> stepsCanvasList;
    private HashMap<ResizableCanvas, Separator> pairs;

    private boolean wasSaved = false;

    public ResizableCanvas mainCanvas;
    public ResizableCanvas gridCanvas;
    public ResizableCanvas.Grid grid;

    @FXML
    private CheckBox showPointsCheck;
    @FXML
    private MenuItem foldedModelMenuItem;
    @FXML
    private MenuItem zoomInMenuItem;
    @FXML
    private MenuItem zoomOutMenuItem;
    @FXML
    private MenuItem saveMenuItem;
    @FXML
    private Menu exportMenu;

    @FXML
    private TextArea log;
    @FXML
    private VBox history;
    @FXML
    private VBox steps;

    @FXML
    private HBox creaseEditor;
    @FXML
    private GridPane boxes;

    @FXML
    private ColumnConstraints left;
    @FXML
    private ColumnConstraints right;

    private CreasePatternEditor.EditSetting editSetting = CreasePatternEditor.EditSetting.NONE;

    @FXML
    public void initialize() {
        mainCanvas = new ResizableCanvas(2000, 2000);
        mainCanvas.setId("main");
        mainCanvas.setManaged(false);

        mainCanvas.getGraphicsContext2D().setFill(Color.TRANSPARENT);
        mainCanvas.getGraphicsContext2D().clearRect(0, 0, 2000,2000);

        gridCanvas = new ResizableCanvas(2000, 2000);
        gridCanvas.setManaged(false);

        gridCanvas.getGraphicsContext2D().setFill(Color.WHITE);
        gridCanvas.getGraphicsContext2D().fillRect(0, 0, 2000,2000);
        gridCanvas.getGraphicsContext2D().setImageSmoothing(false);

        grid = new ResizableCanvas.Grid(gridCanvas,50);
        grid.drawGrid();

        canvasHolder.setContent(new Group(gridCanvas, mainCanvas));

        canvasHolder.addEventFilter(ScrollEvent.ANY, scrollEvent -> {
            if (mainCanvas.getCp() != null) {
                if (scrollEvent.getDeltaY() < 0) {
                    this.onZoomOutMenuItem();
                }  else {
                    this.onZoomInMenuItem();
                }
                scrollEvent.consume();
            }
        });

        /*
        resizes the left and right sidebars when the window is maximized
        by adding the appropriate listeners when the Scene and Stage are initialized
         */
        window.sceneProperty().addListener((observableScene, oldScene, newScene) -> {
            if (oldScene == null && newScene != null) {
                newScene.windowProperty().addListener((observableWindow, oldWindow, newWindow) -> {
                    if (oldWindow == null && newWindow != null) {
                        ((Stage) newWindow).maximizedProperty().addListener((o, oldBoolean, maximized) -> {
                            if (maximized) {
                                left.setPrefWidth(300);
                                right.setPrefWidth(300);
                            } else {
                                left.setPrefWidth(200);
                                right.setPrefWidth(200);
                            }
                        });
                    }
                });
            }
        });

        /*
        resizes the left and right sidebars when the window width is manually being changed
        by dragging it out or in
         */
        window.widthProperty().addListener((o, oldValue, newValue) -> {
            if (newValue.intValue() > oldValue.intValue()) {
                if (left.getPrefWidth() < 300) {
                    left.setPrefWidth(left.getPrefWidth() + 1);
                }
                if (right.getPrefWidth() < 300) {
                    right.setPrefWidth(right.getPrefWidth() + 1);
                }
            } else {
                if (left.getPrefWidth() > 200) {
                    left.setPrefWidth(left.getPrefWidth() - 2);
                }
                if (right.getPrefWidth() > 200) {
                    right.setPrefWidth(right.getPrefWidth() - 2);
                }
            }
        });

        mainCanvas.setOnMouseMoved(mouseEvent -> {
            if (mainCanvas.getCp() != null) {
                CreasePattern main = mainCanvas.getCp();
                Point scale = new Point(mainCanvas.getCpScaleX(), mainCanvas.getCpScaleY());

                Point mousePos = Point.fromPoint2D(mainCanvas.sceneToLocal(
                        mouseEvent.getSceneX() - mainCanvas.getWidth() / 2,
                        mouseEvent.getSceneY() - mainCanvas.getHeight() / 2)
                );

                if (editSetting == CreasePatternEditor.EditSetting.REMOVE || editSetting == CreasePatternEditor.EditSetting.CHANGE) {
                    for (Crease crease : main.getCreases()) {
                        Point start = crease.getLine().getStart().multiply(scale);
                        Point end = crease.getLine().getEnd().multiply(scale);

                        Line scaledLine = new Line(start, end);

                        if (scaledLine.contains(mousePos, 0.1)) {
                            CreasePatternEditor.highlightCrease(mainCanvas, crease);
                            break;
                        } else {
                            if (crease.isHighlighted()) {
                                main.drawOnCanvas(mainCanvas, mainCanvas.getCpScaleX(), mainCanvas.getCpScaleY());
                                crease.setHighlighted(false);
                                break;
                            }
                        }
                    }
                }
            }
        });

        edit.selectedToggleProperty().addListener((observableValue, oldToggle, newToggle) -> {
            if (newToggle != null) {
                switch (((ToggleButton) newToggle).getId()) {
                    case "add" -> editSetting = CreasePatternEditor.EditSetting.ADD;
                    case "remove" -> editSetting = CreasePatternEditor.EditSetting.REMOVE;
                    case "change" -> editSetting = CreasePatternEditor.EditSetting.CHANGE;
                }
            } else {
                editSetting = CreasePatternEditor.EditSetting.NONE;
            }
        });

        historyCanvasList = new ArrayList<>();
        stepsCanvasList = new ArrayList<>();

        TextLogger.logText("Starting up... Welcome to " + Main.APPLICATION_TITLE + " " + Main.VERSION + "!", log);
    }

    /**
     * Opens a file explorer dialogue which lets the user select
     * .cp files and upon loading them, calls setupUI() -- drawing the
     * pattern to the screen.
     */
    @FXML
    public void onMenuImportAction() {
        FileChooser openFileChooser = new FileChooser();
        openFileChooser.setTitle("Open .cp file");
        openFileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Crease Patterns", "*.cp"));

        File file = openFileChooser.showOpenDialog(mainCanvas.getScene().getWindow());
        var filePath = file == null ? "" : file.getPath();

        if (file != null && file.exists()) {
            resetGUI();
            TextLogger.logText("Import: " + filePath, log);
            try {
                setupUI(new FileInputStream(file), filePath);
            } catch (FileNotFoundException e) {
                TextLogger.logText("File not found or invalid!", log);
            }
        } else {
            System.err.println("No file selected or path is invalid!");
            TextLogger.logText("No file selected or path is invalid!", log);
        }
    }

    /**
     * Opens a file explorer dialogue which lets the user export
     * the history to either pdf or svg
     */
    @FXML
    public void onMenuExportPDFAction() {
        PDFHistoryExporter pdfHistoryExporter = new PDFHistoryExporter(historyCanvasList);
        var file = pdfHistoryExporter.open(mainCanvas.getScene().getRoot());

        if (file.isPresent()) {
            if (pdfHistoryExporter.export(file.get())) {
                TextLogger.logText("Saved " + file.get().getName() + " successfully", log);
                wasSaved = true;

                String title = ((Stage) mainCanvas.getScene().getWindow()).getTitle();
                ((Stage) mainCanvas.getScene().getWindow()).setTitle(title.replace("*", ""));
            } else {
                TextLogger.logText("Error while exporting to PDF", log);
            }
        }
    }

    @FXML
    public void onMenuExportSVGAction() {
        SVGHistoryExporter svgHistoryExporter = new SVGHistoryExporter(historyCanvasList);
        var file = svgHistoryExporter.open(mainCanvas.getScene().getRoot());

        if (file.isPresent()) {
            if (svgHistoryExporter.export(file.get())) {
                TextLogger.logText("Saved " + file.get().getName() + " successfully", log);
                wasSaved = true;

                String title = ((Stage) mainCanvas.getScene().getWindow()).getTitle();
                ((Stage) mainCanvas.getScene().getWindow()).setTitle(title.replace("*", ""));
            } else {
                TextLogger.logText("Error while exporting to SVG", log);
            }
        }
    }

    /**
     * Opens Oripa with the folded 3d model,
     * calls foldModel() method.
     *
     * Opens an Alert in case of an error while folding the model.
     */
    @FXML
    public void onShowFoldedModelAction() throws IOException {
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
                TextLogger.logText("Crease Pattern is invalid", log);
            }
        }
    }

    // -------------------------
    // Handling different kinds of zoom
    @FXML
    public void onZoomInMenuItem() {
        mainCanvas.zoomIn();
        grid.zoomIn();
    }

    @FXML
    public void onZoomOutMenuItem() {
        mainCanvas.zoomOut();
        grid.zoomOut();
    }

    @FXML
    public void onGridIncreaseAction() {
        grid.drawGrid(grid.getCurrentCellSize() * 2);
        TextLogger.logText("Increased Grid (x2), new grid cell size: " + grid.getCurrentCellSize(), log);
    }

    @FXML
    public void onGridDecreaseAction() {
        grid.drawGrid(grid.getCurrentCellSize() / 2);
        TextLogger.logText("Decreased Grid (x0.5), new grid cell size: " + grid.getCurrentCellSize(), log);
    }

    @FXML
    public void onGridCustomAction() {
        CustomGridSizeWindow.open(grid);
        TextLogger.logText("New grid cell size: " + grid.getCurrentCellSize(), log);
    }
    // -------------------------

    @FXML
    public void onMenuResetAction() {
        resetGUI();
        TextLogger.logText("Reset: UI cleared!", log);
    }

    // -------------------------
    // Loading example files
    @FXML
    public void onLoadExampleBird() {
        resetGUI();
        TextLogger.logText("Import: example/bird.cp", log);
        InputStream is = Main.class.getResourceAsStream("example/bird.cp");
        setupUI(is, "example/bird.cp");
    }

    @FXML
    public void onLoadExamplePenguin() {
        resetGUI();
        TextLogger.logText("Import: example/penguin_hideo_komatsu.cp", log);
        InputStream is = Main.class.getResourceAsStream("example/penguin_hideo_komatsu.cp");
        setupUI(is, "example/penguin_hideo_komatsu.cp");
    }

    @FXML
    public void onLoadExampleCrane() {
        resetGUI();
        TextLogger.logText("Import: example/crane.cp", log);
        InputStream is = Main.class.getResourceAsStream("example/crane.cp");
        setupUI(is, "example/crane.cp");
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
    private void setupUI(InputStream is, String filePath) {
        ((Stage) mainCanvas.getScene().getWindow()).setTitle(filePath + "* - " + Main.APPLICATION_TITLE);

        cp = CreasePattern.createFromFile(is);
        if (cp != null) {
            cp.drawOnCanvas(mainCanvas, 1, 1);
            TextLogger.logText("Crease Pattern successfully loaded!", log);
        }

        model = new OrigamiModel(cp);

        ExtendedCreasePattern ecp = new ExtendedCreasePatternFactory().createExtendedCreasePattern(cp);
        TextLogger.logText(ecp.possibleSteps().size() + " possible step(s) were calculated", log);
        System.out.println("size = " + ecp.possibleSteps().size());

        // should be called when the algorithm is executed, aka once the amount of steps is known
        pairs = new HashMap<>();
        createCanvases(steps, stepsCanvasList, ecp.possibleSteps().size());

        drawSteps(ecp);
        drawHistory(cp, history);

        setupMouseEvents(stepsCanvasList, historyCanvasList);
        mainCanvas.getScene().getWindow().setOnCloseRequest(windowEvent -> {
            if (!historyCanvasList.isEmpty() && !wasSaved) {
                ClosingWindow.open(historyCanvasList, filePath);
                windowEvent.consume();
            }
        });

        // after reading the file, if the file is valid:
        foldedModelMenuItem.setDisable(false);
        zoomInMenuItem.setDisable(false);
        zoomOutMenuItem.setDisable(false);
        exportMenu.setDisable(false);
        saveMenuItem.setDisable(false);

        creaseEditor.setDisable(false);
        boxes.setDisable(false);
    }

    private void drawSteps(ExtendedCreasePattern ecp) {
        for (int i = 0; i < ecp.possibleSteps().size(); i++) {
            DiagramStep step = ecp.possibleSteps().get(i);
            step.to.toCreasePattern().drawOnCanvas(stepsCanvasList.get(i),
                    0.4, 0.4);
        }
    }

    private void drawHistory(CreasePattern cp, Pane history) {
        createCanvases(history, historyCanvasList, 1);
        historyCanvasList.forEach(c -> {
            if (c.getCp() == null) {
                cp.drawOnCanvas(c, 0.4, 0.4);
            }
        });
        historyLabel.setText("History (" + historyCanvasList.size() + " steps)");
    }

    @SafeVarargs
    private void setupMouseEvents(List<ResizableCanvas>... lists) {
        for (var list : lists) {
            list.forEach(c -> {
                GraphicsContext graphicsContext = c.getGraphicsContext2D();
                c.setOnMouseEntered(mouseEvent -> {
                    if (!c.equals(activeHistory)) {
                        graphicsContext.setFill(Color.color(0.2, 0.2, 0.2, 0.2));
                        graphicsContext.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
                    }
                    c.setCursor(Cursor.HAND);

                    CreasePattern diff = mainCanvas.getCp().getDifference(c.getCp());
                    diff.drawOverCanvas(mainCanvas, mainCanvas.getCpScaleX(), mainCanvas.getCpScaleY());
                });

                c.setOnMouseExited(mouseEvent -> {
                    if (!c.equals(activeHistory)) {
                        graphicsContext.clearRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
                        c.getCp().drawOnCanvas(c, 0.4, 0.4);
                    }

                    c.setCursor(Cursor.DEFAULT);
                    mainCanvas.getCp().drawOnCanvas(mainCanvas, mainCanvas.getCpScaleX(), mainCanvas.getCpScaleY());
                });

                c.setOnMouseClicked(mouseEvent -> {
                    if (mouseEvent.getButton() == MouseButton.SECONDARY) {
                        if (c.getParent().equals(history)) {
                            ContextMenu contextMenu = new ContextMenu();
                            MenuItem delete = new MenuItem("Delete");

                            delete.setOnAction(actionEvent -> {
                                history.getChildren().remove(pairs.get(c));
                                history.getChildren().remove(c);

                                historyCanvasList.remove(c);
                                historyLabel.setText("History (" + historyCanvasList.size() + " steps)");
                                pairs.remove(c);

                                TextLogger.logText("1 step in history successfully deleted", log);
                            });
                            contextMenu.getItems().add(delete);

                            c.setOnContextMenuRequested(contextMenuEvent -> {
                                contextMenu.show(c, contextMenuEvent.getScreenX(), contextMenuEvent.getScreenY());
                            });
                        }
                    } else {
                        CreasePattern currentStep = c.getCp();

                        currentStep.drawOnCanvas(mainCanvas, mainCanvas.getCpScaleX(), mainCanvas.getCpScaleY());
                        ExtendedCreasePattern ecp = new ExtendedCreasePatternFactory().createExtendedCreasePattern(currentStep);

                        if (c.getParent().equals(steps)) {
                            drawHistory(currentStep, history);
                            historyCanvasList.stream().filter(node -> node.getCp().equals(currentStep))
                                    .forEach(node -> {
                                        if (activeHistory != null) {
                                            activeHistory.getGraphicsContext2D().clearRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
                                            activeHistory.getCp().drawOnCanvas(activeHistory, 0.4, 0.4);
                                            activeHistory.setSelected(false);
                                        }

                                        activeHistory = node;
                                        activeHistory.markAsCurrentlySelected();
                                    }
                            );
                            TextLogger.logText("Pick this step and add to history... " + ecp.possibleSteps().size() + " new option(s) were calculated", log);
                        }
                        if (c.getParent().equals(history)) {
                            if (activeHistory != null) {
                                activeHistory.getGraphicsContext2D().clearRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
                                activeHistory.getCp().drawOnCanvas(activeHistory, 0.4, 0.4);
                                activeHistory.setSelected(false);
                            }

                            activeHistory = c;
                            activeHistory.setSelected(true);
                            // activeHistory.markAsCurrentlySelected();
                        }

                        steps.getChildren().clear();
                        stepsCanvasList.clear();

                        createCanvases(steps, stepsCanvasList, ecp.possibleSteps().size());
                        setupMouseEvents(stepsCanvasList, historyCanvasList);
                        drawSteps(ecp);
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
        mainCanvas.setCp(null);
        mainCanvas.getGraphicsContext2D().clearRect(0, 0, mainCanvas.getWidth(), mainCanvas.getHeight());
        mainCanvas.setShowPoints(false);
        ((Stage) mainCanvas.getScene().getWindow()).setTitle(Main.APPLICATION_TITLE);

        historyLabel.setText("History (0 steps)");

        steps.getChildren().clear();
        history.getChildren().clear();

        foldedModelMenuItem.setDisable(true);
        zoomInMenuItem.setDisable(true);
        zoomOutMenuItem.setDisable(true);
        exportMenu.setDisable(true);
        saveMenuItem.setDisable(true);

        creaseEditor.setDisable(true);
        boxes.setDisable(true);
        showPointsCheck.setSelected(false);

        stepsCanvasList.clear();
        historyCanvasList.clear();

        cp = null;
        wasSaved = false;

        grid.reset();

        TextLogger.logText("-----------------", log);
    }

    /**
     * Creates new canvases in the sidebar when necessary to show
     * the steps needed
     *
     * @param pane the VBox or HBox in which to add the Canvases to
     * @param list the list to add the Canvases to
     * @param amount the amount of Canvases needed
     */
    private void createCanvases(Pane pane, List<ResizableCanvas> list, int amount) {
        for (int i = 0; i < amount; ++i) {
            ResizableCanvas canvas = new ResizableCanvas(CANVAS_WIDTH, CANVAS_HEIGHT);
            Separator separator = new Separator();

            pane.getChildren().add(canvas);
            pane.getChildren().add(separator);
            pairs.put(canvas, separator);

            list.add(canvas);
        }
    }

    @FXML
    private void reverseHistory() {
        List<ResizableCanvas> reverseList = new ArrayList<>();
        for (int i = historyCanvasList.size() - 1; i >= 0; i--) {
            reverseList.add(new ResizableCanvas(historyCanvasList.get(i)));
        }

        if (activeHistory != null) activeHistory.setSelected(false);

        for (int i = 0, reverseListSize = reverseList.size(); i < reverseListSize; i++) {
            ResizableCanvas canvas = reverseList.get(i);
            canvas.getCp().drawOnCanvas(historyCanvasList.get(i), 0.4, 0.4);

            if (canvas.isSelected()) {
                System.out.println("is selected");
                activeHistory = historyCanvasList.get(i);
                activeHistory.markAsCurrentlySelected();
            }
        }
    }

    public void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }

    @FXML
    private void onExit() {
        Platform.exit();
    }

    @FXML
    public void onSaveMainCP() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save crease pattern as...");

        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("PDF Document", "*.pdf"),
                                                 new FileChooser.ExtensionFilter("Scalable Vector Graphics", "*.svg"),
                                                 new FileChooser.ExtensionFilter("PNG Image", "*.png"));

        File file = fileChooser.showSaveDialog(mainCanvas.getScene().getWindow());
        if (file != null) {
            switch (fileChooser.getSelectedExtensionFilter().getDescription()) {
                case "PDF Document" -> new PDFCreasePatternExporter(cp).export(file);
                case "Scalable Vector Graphics" -> new SVGCreasePatternExporter(cp).export(file);
                case "PNG Image" -> new PNGCreasePatternExporter(mainCanvas).export(file);
            }
        }
    }

    @FXML
    public void onShowPoints() {
        if (showPointsCheck.isSelected()) {
            TextLogger.logText("Rendering all points in the crease pattern", log);
            mainCanvas.setShowPoints(true);
        } else {
            TextLogger.logText("Disabling render of points in crease pattern", log);
            mainCanvas.setShowPoints(false);
        }
        mainCanvas.getCp().drawOnCanvas(mainCanvas, mainCanvas.getCpScaleX(), mainCanvas.getCpScaleY());
    }
}
