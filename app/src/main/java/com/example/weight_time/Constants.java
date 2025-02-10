package com.example.weight_time;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class Constants {
    public static final int updateClockTimeMillis = 200;
    public static final int MIN_WEIGHT_VALUE = 3;
    public static final int MAX_WEIGHT_VALUE = 300;
    public static final long NUMBER_OF_MONTHS_TO_USE = 3L;

    public static final String defaultFont = "S15.otf";
    public static final String weightFormatterString = "%011.8f";

    // ---------- DataBase constants
    public static final String tableName = "weight_table";
    public static final String timestampColumnName = "timestamp";
    public static final String weightColumnName = "weight";

    public static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + tableName + " (" +
                    timestampColumnName + " INTEGER PRIMARY KEY NOT NULL," +
                    weightColumnName + " REAL NOT NULL)";

    // Speed of convergence real weight and line of calculated weight
    // public static final double n = 1.2;

    // Speed of additional weight obtain or loose. Kg per Day
    public static final double ws = 0.1;

    public static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + tableName;

    // 2021-03-24 16:48:05
    public static final SimpleDateFormat logDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", new Locale("ru","RU"));

    public static final String SHARED_NUMBER_OF_TILES = "tiles";
    public static final String SHARED_LAST_RUN = "last_run";
    public static final int MAX_CALORIES = 15;

    public static final long TIME_BETWEEN_RESET_SECS = 24*3600;
    //public static final long TIME_BETWEEN_RESET_SECS = 15;
}
