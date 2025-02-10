package com.example.weight_time.timer;

import static com.example.weight_time.Constants.SHARED_LAST_RUN;
import static com.example.weight_time.Constants.TIME_BETWEEN_RESET_SECS;
import static com.example.weight_time.Constants.logDateFormat;

import android.util.Log;

import com.example.weight_time.sharedPreferences.SharedPreference;

import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MyTimer {

    public static final String TAG = "TIMER";

    private final Function timerTask;
    private final SharedPreference sharedPreference;
    private final Calendar timeOfNextEvent;
    private boolean needToImmediateStart;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    ScheduledFuture<?> future;

    @FunctionalInterface
    public interface Function {
        void apply();
    }

    public MyTimer(SharedPreference sharedPreference, Function timerTask, int hour, int minute) {
        this.timerTask = timerTask;
        this.sharedPreference = sharedPreference;

        Calendar now = Calendar.getInstance();
        timeOfNextEvent = (Calendar) now.clone();
        timeOfNextEvent.set(Calendar.HOUR_OF_DAY, hour);
        timeOfNextEvent.set(Calendar.MINUTE, minute);
        timeOfNextEvent.set(Calendar.SECOND, 0);

        // Check need to immediate start
        needToImmediateStart = now.getTimeInMillis() - sharedPreference.getValueLong(SHARED_LAST_RUN) > TIME_BETWEEN_RESET_SECS * 1000;
        Log.i(TAG, "Need to immediate start: " + needToImmediateStart);

        // Adjust if in Past
        while (timeOfNextEvent.getTimeInMillis() - now.getTimeInMillis() < 0) {
            timeOfNextEvent.setTimeInMillis(timeOfNextEvent.getTimeInMillis() + TIME_BETWEEN_RESET_SECS * 1000);
        }
        Log.i(TAG, "Planned time of next start: " + logDateFormat.format(timeOfNextEvent.getTime()));
    }

    public void onPause() {
    }

    public void onResume() {
        // need to immediate start
        if (needToImmediateStart) {
            Log.d(TAG, "Immediate start!");
            needToImmediateStart = false;
            Executors.newSingleThreadExecutor().execute(this::startTask);
        }
        long firstDelay = timeOfNextEvent.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
        if (firstDelay < 0) firstDelay = 0;
        if (future == null || future.isDone()) {
            Log.i(TAG, "Scheduling with first delay(ms): " + firstDelay);
            future = scheduler.schedule((Runnable) () -> {
                        while (true) {
                            try {
                                startTask();
                                long delay = TIME_BETWEEN_RESET_SECS * 1000;
                                Log.i(TAG, "Rescheduling. New delay: " + delay);
                                Thread.sleep(delay);
                            } catch (InterruptedException ignored) {
                                Log.e(TAG, "Timer interrupted");
                            }
                        }
                    },
                    firstDelay,
                    TimeUnit.MILLISECONDS);
        } else {
            Log.d(TAG, "Timer still active.");
        }
    }

    private void startTask() {
        sharedPreference.save(SHARED_LAST_RUN, Calendar.getInstance().getTimeInMillis());
        timerTask.apply();
    }
}
