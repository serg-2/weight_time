package com.example.weight_time;

import static com.example.weight_time.Constants.NUMBER_OF_MONTHS_TO_USE;
import static com.example.weight_time.Constants.tableName;
import static com.example.weight_time.Constants.timestampColumnName;
import static com.example.weight_time.Constants.weightColumnName;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class DbHelper {
    private final FeedReaderDbHelper dbHelper;

    private final SQLiteDatabase dbWrite;
    private final SQLiteDatabase dbRead;

    private final long minimumX = 0L;

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

    public Pair<Double, Double> GetCoefficients() {
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
        double sumX = Sum(rawX);
        double sumY = Sum(rawY);
        double sumXY = SumOfMultiplication(rawX, rawY);
        double sumXX = SumOfMultiplication(rawX, rawX);

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
            return new Pair<>(lastTime - Double.valueOf(minimumX).longValue(), lastWeight);
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

    private double Sum(List<Double> a) {
        return a.stream()
                .mapToDouble(i-> i)
                .sum();
    }

    private double SumOfMultiplication(List<Double> list1, List<Double> list2) {
        if (list1.size() != list2.size()) {
            return 0;
        }
        double sum = 0;
        for (int i = 0; i < list1.size(); i++) {
            sum += list1.get(i) * list2.get(i);
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
