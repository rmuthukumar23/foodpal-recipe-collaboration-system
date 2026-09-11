package client.scenes;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.stage.Stage;
import javafx.util.Duration;

public class TimerCtrl {
    @FXML private Spinner<Integer> hoursSpinner;
    @FXML private Spinner<Integer> minutesSpinner;
    @FXML private Spinner<Integer> secondsSpinner;
    @FXML private Label timerDisplay;
    @FXML private Button startButton;
    @FXML private Button pauseButton;
    @FXML private Button resetButton;
    @FXML private Button closeButton;
    private Timeline timeline;
    private int remainingSeconds;
    private boolean isRunning;
    private Stage timerStage;
    private PrimaryCtrl primaryCtrl;

    public void setTimerStage(Stage stage) { this.timerStage = stage; }
    public void setPrimaryCtrl(PrimaryCtrl primaryCtrl) { this.primaryCtrl = primaryCtrl; }

    @FXML
    public void initialize() {
        hoursSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 0));
        minutesSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
        secondsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
        hoursSpinner.setEditable(true);
        minutesSpinner.setEditable(true);
        secondsSpinner.setEditable(true);
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            if (remainingSeconds > 0) {
                remainingSeconds--;
                updateDisplay();
            } else onTimerComplete();
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        updateDisplay();
        pauseButton.setDisable(true);
    }

    @FXML
    public void onStart() {
        if (!isRunning) {
            if (remainingSeconds == 0) {
                int hours = hoursSpinner.getValue();
                int minutes = minutesSpinner.getValue();
                int seconds = secondsSpinner.getValue();
                remainingSeconds = (hours * 3600) + (minutes * 60) + seconds;
                if (remainingSeconds == 0) return;
            }
            isRunning = true;
            timeline.play();
            startButton.setDisable(true);
            pauseButton.setDisable(false);
            hoursSpinner.setDisable(true);
            minutesSpinner.setDisable(true);
            secondsSpinner.setDisable(true);
        }
    }

    @FXML
    public void onPause() {
        if (isRunning) {
            isRunning = false;
            timeline.pause();
            startButton.setDisable(false);
            pauseButton.setDisable(true);
        }
    }

    @FXML
    public void onReset() {
        isRunning = false;
        timeline.stop();
        remainingSeconds = 0;
        hoursSpinner.getValueFactory().setValue(0);
        minutesSpinner.getValueFactory().setValue(0);
        secondsSpinner.getValueFactory().setValue(0);
        updateDisplay();
        startButton.setDisable(false);
        pauseButton.setDisable(true);
        hoursSpinner.setDisable(false);
        minutesSpinner.setDisable(false);
        secondsSpinner.setDisable(false);
    }

    @FXML
    public void onClose() {
        if (isRunning) timeline.stop();
        if (timerStage != null) timerStage.close();
    }

    private void onTimerComplete() {
        isRunning = false;
        timeline.stop();
        startButton.setDisable(false);
        pauseButton.setDisable(true);
        hoursSpinner.setDisable(false);
        minutesSpinner.setDisable(false);
        secondsSpinner.setDisable(false);
        timerDisplay.setText("DONE!");
    }

    private void updateDisplay() {
        int hours = remainingSeconds / 3600;
        int minutes = (remainingSeconds % 3600) / 60;
        int seconds = remainingSeconds % 60;
        timerDisplay.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
    }
}