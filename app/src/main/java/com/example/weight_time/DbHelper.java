package com.example.weight_time;

import static com.example.weight_time.consts.tableName;
import static com.example.weight_time.consts.timestampColumnName;
import static com.example.weight_time.consts.viewNameMedTime;
import static com.example.weight_time.consts.weightColumnName;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Pair;

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

    public Pair<Double, Double> GetWeights(double medTime, boolean isFirstPart) {
        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                "MIN(" + timestampColumnName + ")",
                "MAX(" + timestampColumnName + ")",
                "AVG(" + weightColumnName + ")"
        };

        // Filter results
        String selection;
        if (isFirstPart) {
            selection = timestampColumnName + " < ?";
        } else {
            selection = timestampColumnName + " > ?";
        }

        String[] selectionArgs = {String.valueOf(medTime)};

        Cursor cursor = dbRead.query(
                tableName,   // The table to query
                projection,                   // The array of columns to return (pass null to get all)
                selection,              // The columns for the WHERE clause
                selectionArgs,          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                null               // The sort order
        );

        cursor.moveToNext();

        double avgTimestamp = (cursor.getDouble(0) + cursor.getDouble(1)) / 2d;

        //Log.e("MAIN", "Timestamp: " + cursor.getDouble(0) + " weight: " + cursor.getDouble(1));
        Pair<Double, Double> result = new Pair<>(avgTimestamp, cursor.getDouble(2));
        cursor.close();
        return result;
    }

    public double GetMedTime() {
        String[] projection = {
                "med"
        };
        Cursor cursor = dbRead.query(
                viewNameMedTime,                   // The table to query
                projection,                 // The array of columns to return (pass null to get all)
                null,               // The columns for the WHERE clause
                null,           // The values for the WHERE clause
                null,               // don't group the rows
                null,                // don't filter by row groups
                null                // The sort order
        );
        cursor.moveToNext();
        double medTime = cursor.getDouble(0);
        cursor.close();
        return medTime;
    }

    public void Destroy() {
        dbHelper.close();
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
