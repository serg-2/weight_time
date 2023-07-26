package com.example.weight_time;

import static com.example.weight_time.consts.NUMBER_OF_MONTHS_TO_USE;
import static com.example.weight_time.consts.tableName;
import static com.example.weight_time.consts.timestampColumnName;
import static com.example.weight_time.consts.weightColumnName;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class DbHelper {
    private FeedReaderDbHelper dbHelper;

    private SQLiteDatabase dbWrite;
    private SQLiteDatabase dbRead;

    private long minX = 0L;

    public DbHelper(Context context) {
        dbHelper = new FeedReaderDbHelper(context);
        dbWrite = dbHelper.getWritableDatabase();
        dbRead = dbHelper.getReadableDatabase();
    }

    public void WriteNewWeight(double weight, long curTime) {
        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(timestampColumnName, curTime);
        values.put(weightColumnName, weight);

        // Insert the new row, returning the primary key value of the new row
        // long newRowId = dbMain.insert(tableName, null, values);
        dbWrite.insertOrThrow(tableName, null, values);
        Log.d("DB", "Values inserted: time: " + curTime + " weight: " + weight);
    }

    public Pair<Double, Double> GetKoeffs() {
        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] nameOfColumns = {
                timestampColumnName,
                weightColumnName
        };

        // How you want the results sorted in the resulting Cursor
        String sortOrder =
                timestampColumnName + " ASC";

        Cursor cursor = dbRead.query(
                tableName,                      // The table to query
                nameOfColumns,                  // The array of columns to return (pass null to get all)
                timestampColumnName + " > ?",   // The columns for the WHERE clause (pass null - without selection)
                GetSelectionArgs(),             // The values for the WHERE clause (null if no WHERE)
                null,                           // don't group the rows
                null,                           // don't filter by row groups
                sortOrder                       // The sort order
        );

        Log.d("DB", "Size of weightings: " + cursor.getCount());

        List<Double> rawX = new ArrayList<>();
        List<Double> rawY = new ArrayList<>();
        while (cursor.moveToNext()) {
            rawX.add(Long.valueOf(cursor.getLong(0)).doubleValue());
            rawY.add(cursor.getDouble(1));
        }
        cursor.close();

        // Number of points
        int la = rawY.size();
        if (la < 2) {
            return new Pair<>(-1d, -1d);
        }

        // Calculating K
        double sumX = CalcSum(rawX);
        double sumY = CalcSum(rawY);
        double sumXY = CalcSumMultiplication(rawX, rawY);
        double sumXX = CalcSumMultiplication(rawX, rawX);

        // Calculating k and b for line in SECONDS
        double k = (la * sumXY - sumX * sumY) / (la * sumXX - sumX * sumX);
        double b = (sumY - k * sumX) / la;

        return new Pair<>(k, b);
    }

    public Pair<Long, Double> GetLastResult() {
        String[] nameOfColumns = {
                timestampColumnName,
                weightColumnName
        };

        String order = timestampColumnName + " DESC";
        Cursor cursor = dbRead.query(
                tableName,                      // The table to query
                nameOfColumns,                  // The array of columns to return (pass null to get all)
                timestampColumnName + " > ?",   // The columns for the WHERE clause
                GetSelectionArgs(),             // The values for the WHERE clause
                null,                           // don't group the rows
                null,                           // don't filter by row groups
                order                           // The sort order
        );

        if (cursor.getCount() > 0) {
            cursor.moveToNext();
            long lastTime = cursor.getLong(0);
            double lastWeight = cursor.getLong(1);
            cursor.close();
            return new Pair<>(lastTime - Double.valueOf(minX).longValue(), lastWeight);
        } else {
            cursor.close();
            return new Pair<>(-1L, -1d);
        }
    }

    private String[] GetSelectionArgs() {
        // calculate minus 3 months. minus number of months * 30 * 24 * 3600
        long startTime = System.currentTimeMillis() / 1000L - NUMBER_OF_MONTHS_TO_USE * 30 * 24 * 3600;
        return new String[]{Long.toString(startTime)};
    }

    public void Destroy() {
        dbHelper.close();
    }

    private double CalcSum(List<Double> a) {
        double sum = 0;
        for (Double value : a) {
            sum += value;
        }
        return sum;
    }

    private double CalcSumMultiplication(List<Double> a, List<Double> b) {
        if (a.size() != b.size()) {
            return 0;
        }
        double sum = 0;
        for (int i = 0; i < a.size(); i++) {
            sum += a.get(i) * b.get(i);
        }
        return sum;
    }

    /*
    public long GetMinTime() {
        if (minX == 0L) {
            String[] nameOfColumns = {
                    timestampColumnName,
                    weightColumnName
            };

            // How you want the results sorted in the resulting Cursor
            String sortOrder =
                    timestampColumnName + " ASC";

            Cursor cursor = dbRead.query(
                    tableName,                      // The table to query
                    nameOfColumns,                  // The array of columns to return (pass null to get all)
                    timestampColumnName + " > ?",   // The columns for the WHERE clause (pass null - without selection)
                    GetSelectionArgs(),             // The values for the WHERE clause (null if no WHERE)
                    null,                           // don't group the rows
                    null,                           // don't filter by row groups
                    sortOrder                       // The sort order
            );
            if (cursor.getCount() > 0) {
                cursor.moveToNext();
                long miniTime = cursor.getLong(0);
                cursor.close();
                minX = miniTime;
            }

        }
        return minX;
    }

     */
}
