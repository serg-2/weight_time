package com.example.weight_time;

public class consts {
    public static final int updateClockTimeMillis = 200;
    public static final int MIN_WEIGHT_VALUE = 40;
    public static final int MAX_WEIGHT_VALUE = 160;

    public static final String defaultFont = "S15.otf";
    public static final String weightFormatterString = "%011.8f";

    // ---------- DB CONSTS
    public static final String tableName = "weight_table";
    public static final String timestampColumnName = "timestamp";
    public static final String weightColumnName = "weight";
    public static final String viewNameMedTime = "weight_view";
    public static final String viewNameFirstPart = "fp_view";
    public static final String viewNameLastPart = "lp_view";

    public static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + tableName + " (" +
                    timestampColumnName + " INTEGER PRIMARY KEY NOT NULL," +
                    weightColumnName + " REAL NOT NULL)";

    public static final String SQL_CREATE_VIEW_MED_TIME =
            "CREATE VIEW " + viewNameMedTime + " (min, max, med) AS " +
                    "SELECT MIN(" + timestampColumnName + ")," +
                    " MAX(" + timestampColumnName + ")," +
                    " CAST((MIN(" + timestampColumnName + ") + MAX(" + timestampColumnName + ")) AS REAL) / CAST(2 AS REAL)" +
                    " FROM " + tableName;

    public static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + tableName;

    public static final String SQL_DELETE_VIEW_MED_TIME =
            "DROP VIEW IF EXISTS " + viewNameMedTime;

}
