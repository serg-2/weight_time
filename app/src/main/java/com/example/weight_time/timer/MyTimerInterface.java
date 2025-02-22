package com.example.weight_time.timer;

public interface MyTimerInterface {

    default void onPause() {}

    void onDestroy();

    void onResume();


}
