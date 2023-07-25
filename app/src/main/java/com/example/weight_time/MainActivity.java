package com.example.weight_time;

import static com.example.weight_time.consts.MAX_WEIGHT_VALUE;
import static com.example.weight_time.consts.MIN_WEIGHT_VALUE;
import static com.example.weight_time.consts.defaultFont;
import static com.example.weight_time.consts.n;
import static com.example.weight_time.consts.updateClockTimeMillis;
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

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    // database
    private DbHelper db;
    // timer
    private Timer timer;
    // timer started?
    private boolean timerState = false;
    // Main text view
    private TextView weightTV;
    // Arrow image
    private ImageView arrowV;
    // Enter of wieght
    private EditText mainET;
    // Calculated koeff
    private Double k;
    private Double b;

    // Real line koeff
    private MutableLiveData<Double> kReal = new MutableLiveData<>(0d);
    private MutableLiveData<Double> bReal = new MutableLiveData<>(0d);

    // Time of first weight
    private long startTimeMS;

    // Has enough data to show
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
        kReal.observe(this, arrowObserver);

        // DB Part
        db = new DbHelper(this);

        Pair<Long, Double> res2 = db.GetLastResult();
        // Check Last Result exists
        if (res2.first != -1L) {
            // Get Min Time to simplify calculations
            Log.e("MAIN", "MIN TIME: " + db.GetMinTime());

            startTimeMS = res2.first - db.GetMinTime();
            initAtStart(res2.second);
        }
    }

    private void initAtStart(Double weight) {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                recalculateKoefMNK();
                if (appHasEnoughData) {
                    calculateRealLineKoeff(weight, startTimeMS / 1000d);
                    startTimer();
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
            }, 0, updateClockTimeMillis); //put here time 1000 milliseconds=1 second
            timerState = true;
        }
    }

    private void onTimeChanged() {
        weightTV.setText(String.format(Locale.ENGLISH, weightFormatterString, getWeightCalculated()));
    }

    private double getWeightCalculated() {
        double timeS = ((double) (System.currentTimeMillis() - db.GetMinTime() + startTimeMS)) / 1000d;
        return kReal.getValue() * timeS + bReal.getValue();
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

        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                // Write new value to DB
                long currentTime = System.currentTimeMillis();
                db.WriteNewWeight(newWeightValue, currentTime);

                startTimeMS = currentTime - db.GetMinTime();

                // Recalculate koeff calculated
                recalculateKoefMNK();

                // Calculate Real line==================================
                if (appHasEnoughData) {
                    calculateRealLineKoeff(newWeightValue, startTimeMS / 1000d);
                }
            }
        });
    }

    private void calculateRealLineKoeff(double weight, double timeS) {
        double realKoeff;
        double cWeight = timeS * k + b;

        // real higher than calculated and trend to low
        if ((k < 1) && (weight > cWeight)) {
            realKoeff = 1 / n;
        // Real lower than calculated and trend to low
        } else if ((k < 1) && (weight <= cWeight)) {
            realKoeff = n;
        // Real higher than calculated and trend to high
        } else if ((k > 1) && (weight > cWeight)) {
            realKoeff = 1/ n;
        // Real lower than calculated and trend to high
        } else {
            realKoeff = n;
        }

        double timeOfConvergenceS = (weight - timeS * k * realKoeff - b) / (k * (1 - realKoeff));
        double weightAtConvergence = timeOfConvergenceS * k + b;

        double kRealz = (weight - weightAtConvergence) / (timeS - timeOfConvergenceS);
        double bRealz = weightAtConvergence - kRealz * timeOfConvergenceS;
        Log.e("New Converged", "K= " + kRealz + " B= " + bRealz);

        // Check time between last real weight and calculated time of convergence.
        long ttMS = System.currentTimeMillis() - db.GetMinTime();

        // Logging
        // 2021-03-24 16:48:05
        SimpleDateFormat sdf3 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Log.e("TIMING", "START TIME=" + sdf3.format(new Timestamp(startTimeMS)) + " CURRENT TIME=" + sdf3.format(new Timestamp(ttMS)) + " TIME OF CONVERGENCE=" + sdf3.format(new Timestamp(Double.valueOf(timeOfConvergenceS * 1000d).longValue())));

        if (ttMS > startTimeMS && ttMS < timeOfConvergenceS * 1000d) {
            // Use additional line of convergence
            Log.e("WEIGHT LINE", "Using CONVERGENCE");
            kReal.postValue(kRealz);
            bReal.postValue(bRealz);
        } else {
            Log.e("WEIGHT LINE", "Using CALCULATED");
            // Use real calculation
            kReal.postValue(k);
            bReal.postValue(b);
        }
    }

    private void recalculateKoefMNK() {
        Pair<Double, Double> res = db.GetKoeffs();
        // check something returned
        if (!((res.first == -1d) && (res.second == -1d))) {
            appHasEnoughData = true;
            startTimer();
            k = res.first;
            b = res.second;
            Log.e("New Calculated", "K= " + res.first + " B= " + res.second);
        }
    }
}
