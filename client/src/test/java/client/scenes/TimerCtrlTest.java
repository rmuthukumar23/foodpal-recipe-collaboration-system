package client.scenes;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.isDisabled;
import static org.testfx.matcher.base.NodeMatchers.isEnabled;
import static org.testfx.matcher.control.LabeledMatchers.hasText;

@ExtendWith(ApplicationExtension.class)
class TimerCtrlTest {

    private Stage stage;
    private TimerCtrl controller;

    @Start
    private void start(Stage stage) throws Exception {
        this.stage = stage;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/scenes/Timer.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

        stage.setScene(new Scene(root));
        stage.show();

        Platform.runLater(() -> controller.setTimerStage(stage));
        WaitForAsyncUtils.waitForFxEvents();
    }

    private void setSpinner(String fxId, int value) {
        Platform.runLater(() -> {
            @SuppressWarnings("unchecked")
            Spinner<Integer> sp = (Spinner<Integer>) stage.getScene().lookup(fxId);
            sp.getValueFactory().setValue(value);
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    private String getLabelText(String fxId) {
        AtomicReference<String> ref = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            ref.set(((Label) stage.getScene().lookup(fxId)).getText());
            latch.countDown();
        });
        try {
            assertTrue(latch.await(2, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }
        return ref.get();
    }

    private int getSpinnerValue(String fxId) {
        AtomicReference<Integer> ref = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            @SuppressWarnings("unchecked")
            Spinner<Integer> sp = (Spinner<Integer>) stage.getScene().lookup(fxId);
            ref.set(sp.getValue());
            latch.countDown();
        });
        try {
            assertTrue(latch.await(2, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }
        return ref.get();
    }

    private static Object getPrivateField(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    @Test
    void setTimerStageStoresStageReference() throws Exception {
        Platform.runLater(() -> controller.setTimerStage(stage));
        WaitForAsyncUtils.waitForFxEvents();
        assertSame(stage, getPrivateField(controller, "timerStage"));
    }

    @Test
    void setPrimaryCtrlStoresReference() throws Exception {
        PrimaryCtrl pc = mock(PrimaryCtrl.class);
        Platform.runLater(() -> controller.setPrimaryCtrl(pc));
        WaitForAsyncUtils.waitForFxEvents();
        assertSame(pc, getPrivateField(controller, "primaryCtrl"));
    }

    @Test
    void initializeConfiguresSpinnersAndDisablesPause() {
        verifyThat("#pauseButton", isDisabled());
        final boolean[] hasFactories = new boolean[3];
        final boolean[] editable = new boolean[3];
        final int[] values = new int[3];
        Platform.runLater(() -> {
            @SuppressWarnings("unchecked") Spinner<Integer> h = (Spinner<Integer>) stage.getScene().lookup("#hoursSpinner");
            @SuppressWarnings("unchecked") Spinner<Integer> m = (Spinner<Integer>) stage.getScene().lookup("#minutesSpinner");
            @SuppressWarnings("unchecked") Spinner<Integer> s = (Spinner<Integer>) stage.getScene().lookup("#secondsSpinner");
            hasFactories[0] = h.getValueFactory() != null;
            hasFactories[1] = m.getValueFactory() != null;
            hasFactories[2] = s.getValueFactory() != null;
            editable[0] = h.isEditable(); editable[1] = m.isEditable(); editable[2] = s.isEditable();
            values[0] = h.getValueFactory().getValue(); values[1] = m.getValueFactory().getValue(); values[2] = s.getValueFactory().getValue();
        });
        WaitForAsyncUtils.waitForFxEvents();
        assertTrue(hasFactories[0]); assertTrue(hasFactories[1]); assertTrue(hasFactories[2]);
        assertTrue(editable[0]); assertTrue(editable[1]); assertTrue(editable[2]);
        assertEquals(0, values[0]); assertEquals(0, values[1]); assertEquals(0, values[2]);
    }

    @Test
    void onStartWithZeroTimeDoesNotStart() {
        setSpinner("#hoursSpinner", 0); setSpinner("#minutesSpinner", 0); setSpinner("#secondsSpinner", 0);
        Platform.runLater(controller::onStart); WaitForAsyncUtils.waitForFxEvents();
        verifyThat("#startButton", isEnabled()); verifyThat("#pauseButton", isDisabled());
        assertEquals(0, getSpinnerValue("#hoursSpinner")); assertEquals(0, getSpinnerValue("#minutesSpinner")); assertEquals(0, getSpinnerValue("#secondsSpinner"));
    }

    @Test
    void onStartStartsTimerAndDisablesControls() throws Exception {
        setSpinner("#hoursSpinner", 0); setSpinner("#minutesSpinner", 0); setSpinner("#secondsSpinner", 5);
        Platform.runLater(controller::onStart); WaitForAsyncUtils.waitForFxEvents();
        verifyThat("#startButton", isDisabled()); verifyThat("#pauseButton", isEnabled());
        Field remaining = TimerCtrl.class.getDeclaredField("remainingSeconds"); remaining.setAccessible(true);
        assertEquals(5, remaining.getInt(controller));
    }

    @Test
    void onPauseWhenRunningEnablesStartAndDisablesPause() throws Exception {
        setSpinner("#secondsSpinner", 5);
        Platform.runLater(controller::onStart); WaitForAsyncUtils.waitForFxEvents();
        Platform.runLater(controller::onPause); WaitForAsyncUtils.waitForFxEvents();
        verifyThat("#startButton", isEnabled()); verifyThat("#pauseButton", isDisabled());
        Field running = TimerCtrl.class.getDeclaredField("isRunning"); running.setAccessible(true);
        assertFalse(running.getBoolean(controller));
    }

    @Test
    void onResetStopsTimerAndRestoresDefaults() throws Exception {
        setSpinner("#secondsSpinner", 5);
        Platform.runLater(controller::onStart); WaitForAsyncUtils.waitForFxEvents();
        Platform.runLater(controller::onReset); WaitForAsyncUtils.waitForFxEvents();
        verifyThat("#timerDisplay", hasText("00:00:00")); verifyThat("#startButton", isEnabled()); verifyThat("#pauseButton", isDisabled());
        Field remaining = TimerCtrl.class.getDeclaredField("remainingSeconds"); remaining.setAccessible(true);
        assertEquals(0, remaining.getInt(controller));
    }

    @Test
    void pauseStopsCountdownAndAllowsResume() {
        setSpinner("#secondsSpinner", 5);
        Platform.runLater(controller::onStart); WaitForAsyncUtils.waitForFxEvents();
        WaitForAsyncUtils.sleep(1200, TimeUnit.MILLISECONDS); WaitForAsyncUtils.waitForFxEvents();
        Platform.runLater(controller::onPause); WaitForAsyncUtils.waitForFxEvents();
        String pausedValue = getLabelText("#timerDisplay");
        WaitForAsyncUtils.sleep(1200, TimeUnit.MILLISECONDS); WaitForAsyncUtils.waitForFxEvents();
        assertEquals(pausedValue, getLabelText("#timerDisplay"));
        Platform.runLater(controller::onStart); WaitForAsyncUtils.waitForFxEvents();
        WaitForAsyncUtils.sleep(1200, TimeUnit.MILLISECONDS); WaitForAsyncUtils.waitForFxEvents();
        assertNotEquals(pausedValue, getLabelText("#timerDisplay"));
    }

    @Test
    void timerCompletesAndShowsDone() {
        setSpinner("#secondsSpinner", 1);
        Platform.runLater(controller::onStart); WaitForAsyncUtils.waitForFxEvents();
        WaitForAsyncUtils.sleep(3200, TimeUnit.MILLISECONDS); WaitForAsyncUtils.waitForFxEvents();
        verifyThat("#timerDisplay", hasText("DONE!")); verifyThat("#startButton", isEnabled()); verifyThat("#pauseButton", isDisabled());
    }

    @Test
    void updateDisplayUpdatesMinuteAndSecondLabels() throws Exception {
        Field remaining = TimerCtrl.class.getDeclaredField("remainingSeconds"); remaining.setAccessible(true);
        Method update = TimerCtrl.class.getDeclaredMethod("updateDisplay"); update.setAccessible(true);
        Platform.runLater(() -> { try { remaining.setInt(controller, 65); update.invoke(controller); } catch (Exception e) { throw new RuntimeException(e); } });
        WaitForAsyncUtils.waitForFxEvents();
        verifyThat("#timerDisplay", hasText("00:01:05"));
    }

    @Test
    void onTimerCompleteDisablesPauseAndEnablesControls() throws Exception {
        Method complete = TimerCtrl.class.getDeclaredMethod("onTimerComplete"); complete.setAccessible(true);
        Platform.runLater(() -> { try { complete.invoke(controller); } catch (Exception e) { throw new RuntimeException(e); } });
        WaitForAsyncUtils.waitForFxEvents();
        verifyThat("#pauseButton", isDisabled()); verifyThat("#startButton", isEnabled()); verifyThat("#timerDisplay", hasText("DONE!"));
    }

    @Test
    void onCloseHidesStageWhenInvoked() {
        assertTrue(stage.isShowing());
        Platform.runLater(controller::onClose); WaitForAsyncUtils.waitForFxEvents();
        assertFalse(stage.isShowing());
    }
}
