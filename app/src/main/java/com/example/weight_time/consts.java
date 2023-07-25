package com.example.weight_time;

public class consts {
    public static final int updateClockTimeMillis = 200;
    public static final int MIN_WEIGHT_VALUE = 3;
    public static final int MAX_WEIGHT_VALUE = 300;
    public static final long NUMBER_OF_MONTHS_TO_USE = 3L;

    public static final String defaultFont = "S15.otf";
    public static final String weightFormatterString = "%011.8f";

    // ---------- DB CONSTS
    public static final String tableName = "weight_table";
    public static final String timestampColumnName = "timestamp";
    public static final String weightColumnName = "weight";

    public static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + tableName + " (" +
                    timestampColumnName + " INTEGER PRIMARY KEY NOT NULL," +
                    weightColumnName + " REAL NOT NULL)";

    // Speed of convergence real weight and line of calculated weight
    public static final double n = 1.2;

    public static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + tableName;
}
