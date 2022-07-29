package com.example.weight_time;

import static com.example.weight_time.consts.tableName;
import static com.example.weight_time.consts.timestampColumnName;
import static com.example.weight_time.consts.weightColumnName;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class DbHelper {
    private FeedReaderDbHelper dbHelper;

    private SQLiteDatabase dbWrite;
    private SQLiteDatabase dbRead;

    public DbHelper(Context context) {
        dbHelper = new FeedReaderDbHelper(context);
        dbWrite = dbHelper.getWritableDatabase();
        dbRead = dbHelper.getReadableDatabase();
    }

    public void WriteNewWeight(double weight) {
        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(timestampColumnName, System.currentTimeMillis());
        values.put(weightColumnName, weight);

        // Insert the new row, returning the primary key value of the new row
        // long newRowId = dbMain.insert(tableName, null, values);
        dbWrite.insert(tableName, null, values);
    }

    public Pair<Double, Double> GetKoeffs() {
        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                timestampColumnName,
                weightColumnName
        };

        // How you want the results sorted in the resulting Cursor
        String sortOrder =
                timestampColumnName + " ASC";

        Cursor cursor = dbRead.query(
                tableName,   // The table to query
                projection,                   // The array of columns to return (pass null to get all)
                null,              // The columns for the WHERE clause
                null,          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                sortOrder               // The sort order
        );

        List<Double> rawX = new ArrayList<>();
        List<Double> modX = new ArrayList<>();
        List<Double> rawY = new ArrayList<>();
        while (cursor.moveToNext()) {
            rawX.add(cursor.getDouble(0));
            rawY.add(cursor.getDouble(1));
        }
        cursor.close();

        int la = rawY.size();
        if (la < 2) {
            return new Pair<>(-1d,-1d);
        }
        Double minX = rawX.get(0);

        for (int i = 0; i < la; i++) {
            // Seconds from first calculation
            modX.add((rawX.get(i) - minX) / 1000);
        }

        // Calculating K
        double sumX = CalcSum(modX);
        double sumY = CalcSum(rawY);
        double sumXY = CalcSumMultiplication(modX, rawY);
        double sumXX = CalcSumMultiplication(modX, modX);

        double k = (la * sumXY - sumX * sumY) / (la * sumXX - sumX * sumX);
        double b = (sumY - k * sumX) / la;

        return new Pair<>(k, b);
    }

    public long GetStartTime() {
        String[] projection = {
                "MIN("+timestampColumnName+")"
        };
        Cursor cursor = dbRead.query(
                tableName,                   // The table to query
                projection,                 // The array of columns to return (pass null to get all)
                null,               // The columns for the WHERE clause
                null,           // The values for the WHERE clause
                null,               // don't group the rows
                null,                // don't filter by row groups
                null                // The sort order
        );
        cursor.moveToNext();
        long startTime = cursor.getLong(0);
        cursor.close();
        return startTime;
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
        for (int i = 0; i < a.size(); i++)
        {
            sum += a.get(i) * b.get(i);
        }
        return sum;
    }

    /* Example
    public void GetWeights(double medTime) {
        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                timestampColumnName,
                weightColumnName
        };

        // Filter results
        String selection = timestampColumnName + " > ?";
        String[] selectionArgs = { String.valueOf(medTime) };

        // How you want the results sorted in the resulting Cursor
        String sortOrder =
                timestampColumnName + " ASC";

        Cursor cursor = dbRead.query(
                viewNameFirstPart,   // The table to query
                projection,                   // The array of columns to return (pass null to get all)
                selection,              // The columns for the WHERE clause
                selectionArgs,          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                null               // The sort order
        );

        cursor.moveToNext();

        Log.e("MAIN", "Timestamp: " + cursor.getDouble(0) + " weight: " + cursor.getDouble(1));

        cursor.close();
    }
    */
}
