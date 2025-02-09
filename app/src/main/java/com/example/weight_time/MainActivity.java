package com.example.weight_time;

import static com.example.weight_time.Constants.MAX_WEIGHT_VALUE;
import static com.example.weight_time.Constants.MIN_WEIGHT_VALUE;
import static com.example.weight_time.Constants.NUMBER_OF_TILES_SHARED;
import static com.example.weight_time.Constants.defaultFont;
import static com.example.weight_time.Constants.logDateFormat;
import static com.example.weight_time.Constants.updateClockTimeMillis;
import static com.example.weight_time.Constants.weightFormatterString;
import static com.example.weight_time.Constants.ws;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.weight_time.models.MainViewModel;
import com.example.weight_time.sharedPreferences.SharedPreference;
import com.example.weight_time.timer.MyTimer;

import java.sql.Timestamp;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;

import lombok.Getter;

public class MainActivity extends AppCompatActivity {
    // database
    private DbHelper db;
    // timer
    private Timer timer;
    // timer started?
    private boolean isTimerStarted = false;
    // Main text view
    private TextView weightTV;
    // Arrow image
    private ImageView arrowV;
    // Enter of weight
    private EditText mainET;
    // Calculated coefficient
    private Double k;
    private Double b;

    // Real line coefficient
    private final MutableLiveData<Double> kReal = new MutableLiveData<>(0d);
    private final MutableLiveData<Double> bReal = new MutableLiveData<>(0d);

    // Time of converge
    private final MutableLiveData<Long> timeOfConvergeMS = new MutableLiveData<>(0L);

    // Time of last weighting
    private long lastWeightTime;

    // Has enough data to show
    private boolean appHasEnoughData = false;

    // Recyclerview
    private RecyclerView recyclerView;

    // ViewModel
    public MainViewModel mainViewModel;
    @Getter
    private SharedPreference sharedPreference;

    // Alarm
    private MyTimer myTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainViewModel = getVieModel(MainViewModel.class);

        // Shared preferences
        sharedPreference = new SharedPreference(getApplicationContext());
        mainViewModel.initViewModel(sharedPreference.getValueInteger(NUMBER_OF_TILES_SHARED));

        weightTV = findViewById(R.id.weight);
        arrowV = findViewById(R.id.arrow);
        mainET = findViewById(R.id.textInput);

        Typeface typeface = Typeface.createFromAsset(this.getAssets(), defaultFont);
        weightTV.setTypeface(typeface);
        weightTV.setTextSize(48f);
        weightTV.setTextColor(Color.RED);

        arrowV.setImageResource(R.drawable.ic_arrow_up);

        final Observer<Double> arrowObserver = newValue -> {
            Log.d("kReal", "Changed real coefficient.");
            Double valueToCheck;
            if (timeOfConvergeMS.getValue() != 0) {
                if (System.currentTimeMillis() < timeOfConvergeMS.getValue()) {
                    valueToCheck = kReal.getValue();
                } else {
                    valueToCheck = k;
                }
                // Log.e("MAIN", "System current time: " + System.currentTimeMillis() + " converged time: " + timeOfConvergeMS.getValue());
                // Log.e("MAIN", "Check arrow value: " + valueToCheck);
                if (valueToCheck < 0) {
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
            Log.e("Last Result", "Weight: " + res2.second + " At: " + logDateFormat.format(new Timestamp(res2.first * 1000L)));
            lastWeightTime = res2.first;
            initAtStart(res2.second);
        }

        // Recycler view
        recyclerView = findViewById(R.id.recyclerView);
        // Setting the layout as Staggered Grid for vertical orientation
        StaggeredGridLayoutManager staggeredGridLayoutManager = new StaggeredGridLayoutManager(3, LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(staggeredGridLayoutManager);

        // Sending reference and data to Adapter
        Adapter adapter = new Adapter(MainActivity.this, mainViewModel, sharedPreference);

        // Setting Adapter to RecyclerView
        recyclerView.setAdapter(adapter);

        // Alarm
        myTimer = new MyTimer(adapter::someOutput);
    }

    private void initAtStart(Double weight) {
        Executors.newSingleThreadExecutor().execute(() -> {
            recalculateCoefficientMNK();
            if (appHasEnoughData) {
                calculateRealLineCoefficient(weight, lastWeightTime);
                startTimer();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isTimerStarted) {
            timer.cancel();
            isTimerStarted = false;
        }
        myTimer.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (appHasEnoughData) {
            startTimer();
        }
        myTimer.resume();
    }

    private void startTimer() {
        // Timer
        if (!isTimerStarted) {
            timer = new Timer();
            timer.schedule(new TimerTask() {
                               @Override
                               public void run() {
                                   runOnUiThread(() -> onTimeChanged());
                               }
                           },
                    0,
                    updateClockTimeMillis //put here time 1000 milliseconds=1 second
            );

            isTimerStarted = true;
        }
    }

    private void onTimeChanged() {
        weightTV.setText(String.format(Locale.ENGLISH, weightFormatterString, getWeightCalculated()));
    }

    private double getWeightCalculated() {
        long curTime = System.currentTimeMillis();
        double timeS = ((double) curTime / 1000d);
        if (curTime < timeOfConvergeMS.getValue()) {
            return kReal.getValue() * timeS + bReal.getValue();
        } else {
            return k * timeS + b;
        }
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

        Executors.newSingleThreadExecutor().execute(() -> {
            // Write new value to DB
            long currentTimeS = System.currentTimeMillis() / 1000L;
            db.WriteNewWeight(newWeightValue, currentTimeS);

            lastWeightTime = currentTimeS;

            // Recalculate coefficient calculated
            recalculateCoefficientMNK();

            // Calculate Real line==================================
            if (appHasEnoughData) {
                calculateRealLineCoefficient(newWeightValue, currentTimeS);
            }
        });
    }

    private void calculateRealLineCoefficient(double weight, double timeS) {

        // double timeOfConvergenceS = (weight - timeS * k * n - b) / (k * (1 - n));
        // double weightAtConvergence = timeOfConvergenceS * k + b;

        double timeNeededToConvergeS = (Math.abs((k * timeS + b) - weight) / ws) * 3600 * 24;

        double timeOfConvergenceS = timeS + timeNeededToConvergeS;
        double weightAtConvergence = timeOfConvergenceS * k + b;

        // Storing convergence time
        timeOfConvergeMS.postValue(Double.valueOf(timeOfConvergenceS).longValue() * 1000L);

        Pair<Double, Double> p2 = getLineCoefficientByTwoPoints(timeS, weight, timeOfConvergenceS, weightAtConvergence);
        double kRealz = p2.first;
        double bRealz = p2.second;
        Log.e("New Converged", "K= " + kRealz + " B= " + bRealz);

        // Check time between last real weight and calculated time of convergence.
        long timeOfCheckS = System.currentTimeMillis() / 1000L;

        // Logging
        Log.e("TIMING", "CURRENT TIME=" + logDateFormat.format(new Timestamp(timeOfCheckS * 1000L)) + " TIME OF CONVERGENCE=" + logDateFormat.format(new Timestamp(Double.valueOf(timeOfConvergenceS * 1000d).longValue())));

        if (timeOfCheckS < timeOfConvergenceS) {
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

    private Pair<Double, Double> getLineCoefficientByTwoPoints(double x1, double y1, double x2, double y2) {
        double coefficient1 = (y2 - y1) / (x2 - x1);
        double coefficient2 = (y1 - coefficient1 * x1);
        return new Pair<>(coefficient1, coefficient2);
    }

    private void recalculateCoefficientMNK() {
        Pair<Double, Double> res = db.GetCoefficients();
        // check something returned
        if (!((res.first == -1d) && (res.second == -1d))) {
            appHasEnoughData = true;
            startTimer();
            k = res.first;
            b = res.second;
            Log.e("New Calculated", "K= " + res.first + " B= " + res.second);
        }
    }

    // TECH PART ===================================================================
    protected <T extends ViewModel> T getVieModel(Class<T> clazz) {
        return getVieModel(this, clazz);
    }

    protected <T extends ViewModel> T getVieModel(ViewModelStoreOwner owner, Class<T> clazz) {
        return new ViewModelProvider(owner).get(clazz);
    }
}
