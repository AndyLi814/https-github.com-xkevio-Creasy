package ovgu.creasy.ui;

import javafx.application.HostServices;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import ovgu.creasy.Main;
import ovgu.creasy.origami.*;
import ovgu.creasy.origami.oripa.OripaFoldedModelWindow;
import ovgu.creasy.util.TextLogger;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static ovgu.creasy.ui.ResizableCanvas.CANVAS_HEIGHT;
import static ovgu.creasy.ui.ResizableCanvas.CANVAS_WIDTH;

public class MainWindow {

    private ResizableCanvas activeHistory;

    @FXML
    private ScrollPane canvasHolder;

    private HostServices hostServices;
    private OrigamiModel model;
    private CreasePattern cp;

    private List<ResizableCanvas> historyCanvasList;
    private List<ResizableCanvas> stepsCanvasList;

    public ResizableCanvas mainCanvas;
    public ResizableCanvas gridCanvas;

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


    /* This makes sure that the window scales correctly
       by binding the canvas to the window size and redrawing if
       necessary.
     */
    @FXML
    public void initialize() {
        mainCanvas = new ResizableCanvas(1000, 1000);
        mainCanvas.setId("main");
        mainCanvas.setManaged(false);

        mainCanvas.getGraphicsContext2D().setFill(Color.TRANSPARENT);
        mainCanvas.getGraphicsContext2D().clearRect(0, 0, 1000,1000);

        gridCanvas = new ResizableCanvas(1000, 1000);
        gridCanvas.setManaged(false);

        gridCanvas.getGraphicsContext2D().setFill(Color.WHITE);
        gridCanvas.getGraphicsContext2D().fillRect(0, 0, 1000,1000);
        gridCanvas.drawGrid();

        canvasHolder.setContent(new Group(gridCanvas, mainCanvas));

        canvasHolder.addEventFilter(ScrollEvent.ANY, scrollEvent -> {
            if (mainCanvas.getCp() != null) {
                if (scrollEvent.getDeltaY() < 0) {
                    mainCanvas.zoomOut();
                }  else {
                    mainCanvas.zoomIn();
                }
                scrollEvent.consume();
            }
        });

        historyCanvasList = new ArrayList<>();
        stepsCanvasList = new ArrayList<>();

        TextLogger.logText("Starting up... Welcome to " + Main.APPLICATION_TITLE + " " + Main.VERSION + "!", log);
    }

    /**
     * Opens a file explorer dialogue which lets the user select
     * .cp files and upon loading them, calls setupGUI() -- drawing the
     * pattern to the screen.
     */
    @FXML
    public void onMenuImportAction() {
        FileChooser openFileChooser = new FileChooser();
        openFileChooser.setTitle("Open .cp File");
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
            TextLogger.logText("Crease Pattern successfully loaded!", log);
        } else {
            System.err.println("No file selected or path is invalid!");
            TextLogger.logText("No file selected or path is invalid!", log);
        }
    }

    /**
     * Opens a file explorer dialogue which lets the user export
     * the .cp file
     *
     */
    @FXML
    public void onMenuExportPDFAction() {
        FileChooser exportPdf = new FileChooser();
        exportPdf.setTitle("Save as .pdf");
        exportPdf.getExtensionFilters().add(new FileChooser.ExtensionFilter("Adobe Acrobat Document", ".pdf"));
        File filePdf = exportPdf.showSaveDialog(mainCanvas.getScene().getWindow());

        if (filePdf != null) {

        } else {
            TextLogger.logText("Error saving file!", log);
        }
    }

    @FXML
    public void onMenuExportSVGAction() {
        FileChooser exportSvg = new FileChooser();
        exportSvg.setTitle("Save as .svg");
        exportSvg.getExtensionFilters().add(new FileChooser.ExtensionFilter("Scalable Vector Graphics", ".svg"));
        File fileSvg = exportSvg.showSaveDialog(mainCanvas.getScene().getWindow());

        if (fileSvg != null) {

        }
        else {
            TextLogger.logText("Error saving file!", log);
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
    }

    @FXML
    public void onZoomOutMenuItem() {
        mainCanvas.zoomOut();
    }

    @FXML
    public void onGridIncreaseAction() {
        gridCanvas.drawGrid(gridCanvas.getCurrentCellSize() * 2);
        TextLogger.logText("Increased Grid (x2), new grid cell size: " + gridCanvas.getCurrentCellSize(), log);
    }

    @FXML
    public void onGridDecreaseAction() {
        gridCanvas.drawGrid(gridCanvas.getCurrentCellSize() / 2);
        TextLogger.logText("Decreased Grid (x0.5), new grid cell size: " + gridCanvas.getCurrentCellSize(), log);
    }

    @FXML
    public void onGridCustomAction() {
        CustomGridSizeWindow.open(gridCanvas);
        TextLogger.logText("New grid cell size: " + gridCanvas.getCurrentCellSize(), log);
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
        InputStream is = Main.class.getResourceAsStream("example/bird.cp");
        setupUI(is, "example/bird.cp");
        TextLogger.logText("Import: example/bird.cp", log);
        TextLogger.logText("Crease Pattern successfully loaded!", log);
    }

    @FXML
    public void onLoadExamplePenguin() {
        resetGUI();
        InputStream is = Main.class.getResourceAsStream("example/penguin_hideo_komatsu.cp");
        setupUI(is, "example/penguin_hideo_komatsu.cp");
        TextLogger.logText("Import: example/penguin_hideo_komatsu.cp", log);
        TextLogger.logText("Crease Pattern successfully loaded!", log);
    }

    @FXML
    public void onLoadExampleCrane() {
        resetGUI();
        InputStream is = Main.class.getResourceAsStream("example/crane.cp");
        setupUI(is, "example/crane.cp");
        TextLogger.logText("Import: example/crane.cp", log);
        TextLogger.logText("Crease Pattern successfully loaded!", log);
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
        ((Stage) mainCanvas.getScene().getWindow()).setTitle(filePath + " - " + Main.APPLICATION_TITLE);

        cp = CreasePattern.createFromFile(is);
        if (cp != null) cp.drawOnCanvas(mainCanvas, 1, 1);

        model = new OrigamiModel(cp);

        ExtendedCreasePattern ecp = new ExtendedCreasePatternFactory().createExtendedCreasePattern(cp);
        TextLogger.logText(ecp.possibleSteps().size() + " possible step(s) were calculated", log);
        System.out.println("size = " + ecp.possibleSteps().size());

        // should be called when the algorithm is executed, aka once the amount of steps is known
        createCanvases(steps, stepsCanvasList, ecp.possibleSteps().size());

        drawSteps(ecp);
        drawHistory(cp, history);

        setupMouseEvents(stepsCanvasList, historyCanvasList);

        // after reading the file, if the file is valid:
        foldedModelMenuItem.setDisable(false);
        zoomInMenuItem.setDisable(false);
        zoomOutMenuItem.setDisable(false);
        resetMenuItem.setDisable(false);
        exportMenu.setDisable(false);
    }

    private void drawSteps(ExtendedCreasePattern ecp) {
        for (int i = 0; i < ecp.possibleSteps().size(); i++) {
            DiagramStep step = ecp.possibleSteps().get(i);
            step.to.toCreasePattern().drawOnCanvas(stepsCanvasList.get(i),
                    0.45, 0.45);
        }
    }

    private void drawHistory(CreasePattern cp, Pane history) {
        createCanvases(history, historyCanvasList, 1);
        historyCanvasList.forEach(c -> {
            if (c.getCp() == null) {
                cp.drawOnCanvas(c, 0.45, 0.45);
            }
        });
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
                        c.getCp().drawOnCanvas(c, 0.45, 0.45);
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
                                history.getChildren().remove(c);
                                TextLogger.logText("1 step in history successfully deleted", log);
                            });
                            contextMenu.getItems().add(delete);

                            c.setOnContextMenuRequested(contextMenuEvent -> {
                                contextMenu.show(c, contextMenuEvent.getScreenX(), contextMenuEvent.getScreenY());
                            });
                        }
                    } else {
                        CreasePattern currentStep = c.getCp();

                        currentStep.drawOnCanvas(mainCanvas, 1, 1);
                        ExtendedCreasePattern ecp = new ExtendedCreasePatternFactory().createExtendedCreasePattern(currentStep);

                        if (c.getParent().equals(steps)) {
                            drawHistory(currentStep, history);
                            historyCanvasList.stream().filter(node -> node.getCp().equals(currentStep))
                                    .forEach(node -> {
                                        if (activeHistory != null) {
                                            activeHistory.getGraphicsContext2D().clearRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
                                            activeHistory.getCp().drawOnCanvas(activeHistory, 0.45, 0.45);
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
                                activeHistory.getCp().drawOnCanvas(activeHistory, 0.45, 0.45);
                            }

                            activeHistory = c;
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
        ((Stage) mainCanvas.getScene().getWindow()).setTitle(Main.APPLICATION_TITLE);

        steps.getChildren().clear();
        history.getChildren().clear();

        foldedModelMenuItem.setDisable(true);
        zoomInMenuItem.setDisable(true);
        zoomOutMenuItem.setDisable(true);
        resetMenuItem.setDisable(true);
        exportMenu.setDisable(true);

        stepsCanvasList.clear();
        historyCanvasList.clear();

        cp = null;
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
            pane.getChildren().add(canvas);
            pane.getChildren().add(new Separator());
            list.add(canvas);
        }
    }

    @FXML
    private void reverseHistory() {
        List<ResizableCanvas> reverseList = new ArrayList<>();
        for (int i = historyCanvasList.size() - 1; i >= 0; i--) {
            reverseList.add(new ResizableCanvas(historyCanvasList.get(i)));
        }

        for (int i = 0, reverseListSize = reverseList.size(); i < reverseListSize; i++) {
            ResizableCanvas canvas = reverseList.get(i);
            canvas.getCp().drawOnCanvas(historyCanvasList.get(i), 0.45, 0.45);
        }
    }

    public void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }
}
