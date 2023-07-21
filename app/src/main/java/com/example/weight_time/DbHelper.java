package com.example.weight_time;

import static com.example.weight_time.consts.NUMBER_OF_MONTHS_TO_USE;
import static com.example.weight_time.consts.diffWeightColumnName;
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

    public DbHelper(Context context) {
        dbHelper = new FeedReaderDbHelper(context);
        dbWrite = dbHelper.getWritableDatabase();
        dbRead = dbHelper.getReadableDatabase();
    }

    public void WriteNewWeight(double weight, long curTime, double difference) {
        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(timestampColumnName, curTime);
        values.put(weightColumnName, weight);
        values.put(diffWeightColumnName, difference);

        // Insert the new row, returning the primary key value of the new row
        // long newRowId = dbMain.insert(tableName, null, values);
        dbWrite.insert(tableName, null, values);
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

        List<Double> rawX = new ArrayList<>();
        List<Double> modX = new ArrayList<>();
        List<Double> rawY = new ArrayList<>();
        while (cursor.moveToNext()) {
            rawX.add(cursor.getDouble(0));
            rawY.add(cursor.getDouble(1));
        }
        cursor.close();

        // Number of points
        int la = rawY.size();
        if (la < 2) {
            return new Pair<>(-1d, -1d);
        }

        // Time of first point
        Double minX = rawX.get(0);

        for (int i = 0; i < la; i++) {
            // Number of seconds from first calculation
            modX.add((rawX.get(i) - minX) / 1000);
        }

        // Calculating K
        double sumX = CalcSum(modX);
        double sumY = CalcSum(rawY);
        double sumXY = CalcSumMultiplication(modX, rawY);
        double sumXX = CalcSumMultiplication(modX, modX);

        // Calculating k and b for line
        double k = (la * sumXY - sumX * sumY) / (la * sumXX - sumX * sumX);
        double b = (sumY - k * sumX) / la;

        return new Pair<>(k, b);
    }

    /* DEPRECATED
    public long GetStartTime() {
        String[] nameOfColumns = {
                "MIN(" + timestampColumnName + ")"
        };
        Cursor cursor = dbRead.query(
                tableName,                      // The table to query
                nameOfColumns,                  // The array of columns to return (pass null to get all)
                timestampColumnName + " > ?",   // The columns for the WHERE clause
                GetSelectionArgs(),             // The values for the WHERE clause
                null,                           // don't group the rows
                null,                           // don't filter by row groups
                null                            // The sort order
        );
        cursor.moveToNext();
        long startTime = cursor.getLong(0);
        cursor.close();
        return startTime;
    }
     */

    public double GetLastDiff() {
        String[] nameOfColumns = {
                timestampColumnName,
                diffWeightColumnName
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
        cursor.moveToNext();
        double lastDiff = cursor.getLong(1);
        cursor.close();
        return lastDiff;
    }

    private String[] GetSelectionArgs() {
        // calculate minus 3 months. minus number of months * 30 * 24 * 3600 * 1000
        long startTime = System.currentTimeMillis() - NUMBER_OF_MONTHS_TO_USE * 30 * 24 * 3600 * 1000;
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
