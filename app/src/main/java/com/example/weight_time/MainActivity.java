package com.example.weight_time;

import static com.example.weight_time.consts.MAX_WEIGHT_VALUE;
import static com.example.weight_time.consts.MIN_WEIGHT_VALUE;
import static com.example.weight_time.consts.defaultFont;
import static com.example.weight_time.consts.updateClockTimeMillis;
import static com.example.weight_time.consts.viewNameFirstPart;
import static com.example.weight_time.consts.viewNameLastPart;
import static com.example.weight_time.consts.weightFormatterString;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.time.LocalDateTime;
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
                arrowV.setVisibility(View.VISIBLE);
            }
        };

        k.observe(this, arrowObserver);

        // DB Part
        db = new DbHelper(this);

        recalculateKoef();
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
        double weight = getCurrentWeight(System.currentTimeMillis());
        weightTV.setText(String.format(Locale.ENGLISH, weightFormatterString, weight));
    }

    private double getCurrentWeight(long time) {
        return k.getValue() * ((double) time) + b.getValue();
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

        recalculateKoef();
    }

    private void recalculateKoef() {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                // get Med
                double medTime = db.GetMedTime();

                // Calculate new koeff
                Pair<Double, Double> fp = db.GetWeights(medTime, true);
                Pair<Double, Double> lp = db.GetWeights(medTime, false);

                // Log.e("MAIN", "First Part: " + fp.first + " : " + fp.second);
                // Log.e("MAIN", "Second Part: " + lp.first + " : " + lp.second);

                double tmp_k = (lp.second - fp.second) / (lp.first - fp.first);
                k.postValue(tmp_k);
                double tmp_b = fp.second - tmp_k * fp.first;
                b.postValue(tmp_b);
            }
        });
    }
}