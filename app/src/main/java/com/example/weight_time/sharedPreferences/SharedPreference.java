package com.example.weight_time.sharedPreferences;

import static com.example.weight_time.Constants.NUMBER_OF_TILES_SHARED;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Objects;

public class SharedPreference {
    Context context;
    SharedPreferences sharedPref;
    private final String PREFS_NAME = "weight_time";

    public SharedPreference(Context context) {
        this.context = context;
        this.sharedPref = Objects.requireNonNull(context).getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
        );
    }

    public void save(String KEY_NAME, Object text) {
        SharedPreferences.Editor editor = sharedPref.edit();

        if (text.getClass().equals(String.class)) {
            editor.putString(KEY_NAME, (String) text);
        }
        if (text.getClass().equals(Boolean.class)) {
            editor.putBoolean(KEY_NAME, (Boolean)text);
        }
        if (text.getClass().equals(Integer.class)) {
            editor.putInt(KEY_NAME, (Integer) text);
        }
        if (text.getClass().equals(Long.class)) {
            editor.putLong(KEY_NAME, (Long) text);
        }
        editor.apply();
    }

    public void clearSharedPreference() {
        sharedPref
                .edit()
                .remove(NUMBER_OF_TILES_SHARED)
                .apply();
    }

    public String getValueString(String KEY_NAME) {
        return sharedPref.getString(KEY_NAME, null);
    }

    public Long getValueLong(String KEY_NAME) {
        return sharedPref.getLong(KEY_NAME, -1L);
    }

    public Boolean getValueBoolean(String KEY_NAME) {
        return sharedPref.getBoolean(KEY_NAME, false);
    }

    public Integer getValueInteger(String KEY_NAME) {
        return sharedPref.getInt(KEY_NAME, -1);
    }

    public void removeValue(String KEY_NAME) {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.remove(KEY_NAME);
        editor.apply();
    }

}
