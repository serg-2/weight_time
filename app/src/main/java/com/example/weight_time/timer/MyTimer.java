package com.example.weight_time.timer;

import android.util.Log;

import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class MyTimer {

    private Timer timer;
    private boolean isTimerStarted = false;
    private final Function timerTask;
    private Date startDate;

    @FunctionalInterface
    public interface Function {
        void apply();
    }

    public MyTimer(Function timerTask) {
        this.timerTask = timerTask;
        Calendar calendar = Calendar.getInstance();
        Calendar timeOfStart = (Calendar) calendar.clone();
        // DEBUG
//        timeOfStart.set(Calendar.HOUR_OF_DAY, 3);
//        timeOfStart.set(Calendar.MINUTE, 30);
        timeOfStart.set(Calendar.HOUR_OF_DAY, 0);
        timeOfStart.set(Calendar.MINUTE, 1);
        this.startDate = timeOfStart.getTime();
    }

    public void pause() {
        if (isTimerStarted) {
            timer.cancel();
            isTimerStarted = false;
        }
    }

    public void resume() {
        if (!isTimerStarted) {
            isTimerStarted = true;
            startTimer(startDate);
        }
    }

    private void startTimer(Date passedDate) {
        Log.e("TIMER", "Rescheduling");
        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer();
        timer.schedule(getNewTimerTask(passedDate), passedDate);
        Log.e("TIMER", "Scheduled");
    }

    private static Date updateStartDate(Date passedDate) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(passedDate);
        // DEBUG
        // cal.add(Calendar.DATE, 1);
        cal.add(Calendar.SECOND, 5);
        return cal.getTime();
    }

    private TimerTask getNewTimerTask(Date passedDate2) {
        return new TimerTask() {
            @Override
            public void run() {
                Log.e("TIMER", "Executing task");
                timerTask.apply();
                Date tmpDate = updateStartDate(passedDate2);
                startTimer(tmpDate);
            }
        };
    }
}
