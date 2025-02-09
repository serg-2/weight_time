package com.example.weight_time.models;

import static com.example.weight_time.Constants.MAX_CALORIES;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.example.weight_time.R;

import java.util.ArrayList;
import java.util.Collections;

public class MainViewModel extends AndroidViewModel {

    private ArrayList<Integer> calories;

    public MainViewModel(@NonNull Application application) {
        super(application);
    }

    public int get(int position) {
        return calories.get(position);
    }

    public int size() {
        return calories.size();
    }

    public void remove(int position) {
        calories.remove(position);
    }

    public void initViewModel(Integer tiles) {
        if (tiles == -1) tiles = MAX_CALORIES;
        calories = new ArrayList<>(Collections.nCopies(tiles, R.mipmap.hundred));
    }

    public void resetCalories() {
        calories = new ArrayList<>(Collections.nCopies(MAX_CALORIES, R.mipmap.hundred));
    }


}
