package com.example.weight_time.models;

import static com.example.weight_time.Constants.MAX_CALORIES;
import static com.example.weight_time.Constants.SHARED_NUMBER_OF_TILES;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.weight_time.R;
import com.example.weight_time.sharedPreferences.SharedPreference;

import java.util.ArrayList;
import java.util.Collections;

public class MainViewModel extends AndroidViewModel {

    private SharedPreference sharedPreference;
    private final MutableLiveData<ArrayList<Integer>> calories = new MutableLiveData<>(new ArrayList<>());

    public MainViewModel(@NonNull Application application) {
        super(application);
    }

    public int get(int position) {
        return calories.getValue().get(position);
    }

    public int size() {
        return calories.getValue().size();
    }

    public void remove(int position) {
        calories.getValue().remove(position);
    }

    public void initViewModel(SharedPreference sharedPreference) {
        this.sharedPreference = sharedPreference;
        Integer tiles = sharedPreference.getValueInteger(SHARED_NUMBER_OF_TILES);
        if (tiles == -1) tiles = MAX_CALORIES;
        calories.postValue(new ArrayList<>((Collections.nCopies(tiles, R.mipmap.hundred))));
    }

    public MutableLiveData<ArrayList<Integer>> getCalories() {
        return calories;
    }

    // Reinit adapter by time
    public void reinit() {
        Log.i("TIMER", "Reloading calories.");
        // Will call observer here. Don't need to notify
        getCalories().postValue(new ArrayList<>(Collections.nCopies(MAX_CALORIES, R.mipmap.hundred)));
        sharedPreference.save(SHARED_NUMBER_OF_TILES, size());
    }
}
