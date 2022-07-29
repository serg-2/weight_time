package com.example.weight_time;

import static com.example.weight_time.consts.MAX_WEIGHT_VALUE;
import static com.example.weight_time.consts.MIN_WEIGHT_VALUE;
import static com.example.weight_time.consts.defaultFont;
import static com.example.weight_time.consts.updateClockTimeMillis;
import static com.example.weight_time.consts.weightFormatterString;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private Timer timer;
    private boolean timerState = false;
    private TextView weightTV;
    private ImageView arrowV;
    private EditText mainET;
    private MutableLiveData<Double> k = new MutableLiveData<>(0d);
    private MutableLiveData<Double> b = new MutableLiveData<>(0d);
    private DbHelper db;
    private long startTime;
    private boolean appHasEnoughData = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        weightTV = findViewById(R.id.weight);
        arrowV = findViewById(R.id.arrow);
        mainET = findViewById(R.id.textInput);

        Typeface myTypeface = Typeface.createFromAsset(this.getAssets(), defaultFont);
        weightTV.setTypeface(myTypeface);
        weightTV.setTextSize(48f);
        weightTV.setTextColor(Color.RED);

        arrowV.setImageResource(R.drawable.ic_arrow_up);

        final Observer<Double> arrowObserver = new Observer<Double>() {
            @Override
            public void onChanged(@Nullable final Double newValue) {
                if (newValue < 0) {
                    arrowV.setImageResource(R.drawable.ic_arrow_down);
                } else {
                    arrowV.setImageResource(R.drawable.ic_arrow_up);
                }
                if (appHasEnoughData) {
                    arrowV.setVisibility(View.VISIBLE);
                }
            }
        };
        k.observe(this, arrowObserver);

        // DB Part
        db = new DbHelper(this);

        initStartTime();
    }

    private void initStartTime() {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                long stTime = db.GetStartTime();
                if (stTime != 0) {
                    startTime = stTime;
                    recalculateKoefMNK();
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (timerState) {
            timer.cancel();
            timerState = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (appHasEnoughData) {
            startTimer();
        }
    }

    private void startTimer() {
        // Timer
        if (!timerState) {
            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onTimeChanged();
                        }
                    });
                }
            }, 0, updateClockTimeMillis);//put here time 1000 milliseconds=1 second
            timerState = true;
        }
    }

    private void onTimeChanged() {
        double weight = getCurrentWeight(((double) (System.currentTimeMillis() - startTime)) / 1000d);
        weightTV.setText(String.format(Locale.ENGLISH, weightFormatterString, weight));
    }

    private double getCurrentWeight(double time) {
        return k.getValue() * time + b.getValue();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        db.Destroy();
    }

    public void onAddPressed(View view) {
        // Read New value
        String value = mainET.getText().toString();
        mainET.setText("");
        if (value.length() > 8) {
            return;
        }
        double newWeightValue;
        try {
            newWeightValue = Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return;
        }

        if (newWeightValue < MIN_WEIGHT_VALUE) {
            return;
        }

        if (newWeightValue > MAX_WEIGHT_VALUE) {
            return;
        }

        // Write new value to DB
        db.WriteNewWeight(newWeightValue);

        if (appHasEnoughData) {
            recalculateKoefMNK();
        } else {
            initStartTime();
        }
    }

    private void recalculateKoefMNK() {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                Pair<Double, Double> res = db.GetKoeffs();
                if (!((res.first == -1d) && (res.second == -1d))) {
                    appHasEnoughData = true;
                    startTimer();
                }
                k.postValue(res.first);
                b.postValue(res.second);
            }
        });
    }
}