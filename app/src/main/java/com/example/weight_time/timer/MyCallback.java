package com.example.weight_time.timer;

public class MyCallback {

    private Callback callback;

    public void registerCallBack(Callback callback) {
        this.callback = callback;
    }

    public void doSomething() {
        callback.callingBack();
    }
}
