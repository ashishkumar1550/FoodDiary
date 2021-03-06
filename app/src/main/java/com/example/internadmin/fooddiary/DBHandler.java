package com.example.internadmin.fooddiary;

import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.util.LongSparseArray;

import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class DBHandler extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "historyfooddiary.db";

    private static final String HISTORY_TABLE_NAME = "mealshistory";
    private static final String HISTORY_COLUMN_ID = "id";
    private static final String HISTORY_COLUMN_FOODNAME = "foodname";
    private static final String HISTORY_COLUMN_TIME = "time";
    private static final String HISTORY_COLUMN_SERVINGS = "servingamt";
    private static final String HISTORY_COLUMN_IMGPATH = "imgpath";
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static final String DISHID_TABLE_NAME = "DishID";
    private static final String DISHID_COLUMN_FOODNAME = "foodname";
    private static final String DISHID_COLUMN_IMGPATH = "imgpath";
    private static final String DISHID_COLUMN_VERSION = "version";
    private static final String DISHID_COLUMN_NUTRITIONJSON = "nutritionjson";
    private static final String DISHID_COLUMN_INGREDIENTLIST = "ingredientlist";

    private Context ctx;

    public DBHandler(Context context) {

        super(context, DATABASE_NAME , null, 1);

        ctx = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        String CREATE_TABLE_MEALSHISTORY = "create table " + HISTORY_TABLE_NAME +
                " (" + HISTORY_COLUMN_ID + " integer primary key autoincrement, " +
                HISTORY_COLUMN_FOODNAME + " text, " +
                HISTORY_COLUMN_TIME + " datetime default current_timestamp, " +
                HISTORY_COLUMN_IMGPATH + " text default null, " +
                HISTORY_COLUMN_SERVINGS + " real);";

        String CREATE_TABLE_DISHID = "create table " + DISHID_TABLE_NAME +
                " (" + DISHID_COLUMN_FOODNAME + " text primary key, " +
                DISHID_COLUMN_VERSION + " integer, " +
                DISHID_COLUMN_IMGPATH + " text default null, " +
                DISHID_COLUMN_NUTRITIONJSON + "text, " +
                DISHID_COLUMN_INGREDIENTLIST + "text); ";

        db.execSQL(CREATE_TABLE_MEALSHISTORY);
        db.execSQL(CREATE_TABLE_DISHID);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + HISTORY_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + DISHID_TABLE_NAME);
        onCreate(db);
    }

    //Methods for manipulating Meal Table

    public boolean insertMealEntry (String FoodName, Date TimeConsumed, float ServingAmt, Bitmap FoodImg) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(HISTORY_COLUMN_FOODNAME, FoodName);
        contentValues.put(HISTORY_COLUMN_TIME, dateFormat.format(TimeConsumed));
        contentValues.put(HISTORY_COLUMN_SERVINGS, ServingAmt);

        long rowID = db.insert(HISTORY_TABLE_NAME, null, contentValues);
        if(rowID == -1){
            return false;
        }else{
            ContentValues cv = new ContentValues();
            cv.put(HISTORY_COLUMN_IMGPATH, SaveMealImg(ctx, rowID, FoodName, FoodImg));
            db.update(HISTORY_TABLE_NAME, cv, HISTORY_COLUMN_IMGPATH + " = ? ", new String[] { Long.toString(rowID)});
            return true;
        }

    }

    private String SaveMealImg(Context context, long rowID, String FoodName, Bitmap FoodImg){

        ContextWrapper cw = new ContextWrapper(context);
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        File mypath = new File(directory, FoodName + "_" + Long.toString(rowID) +".jpg");

        FileOutputStream fos;
        try {
            fos = new FileOutputStream(mypath);
            FoodImg.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        } catch(FileNotFoundException e){
            Log.e("Meal Save Image", e.getMessage());
            return null;
        }

        return mypath.getAbsolutePath();
    }

    public Long numberOfMealRecords(){
        SQLiteDatabase db = this.getReadableDatabase();
        return DatabaseUtils.queryNumEntries(db, HISTORY_TABLE_NAME);
    }

    public boolean updateHistoryEntry (String FoodName, Date TimeConsumed, float ServingAmt, Bitmap FoodImg, Long RowID)  {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(HISTORY_COLUMN_FOODNAME, FoodName);
        cv.put(HISTORY_COLUMN_TIME, dateFormat.format(TimeConsumed));
        cv.put(HISTORY_COLUMN_SERVINGS, ServingAmt);
        cv.put(HISTORY_COLUMN_IMGPATH, SaveMealImg(ctx, RowID, FoodName, FoodImg));
        db.update(HISTORY_TABLE_NAME, cv, HISTORY_COLUMN_ID + "=" + RowID, null);
        return true;

    }

    public Integer deleteHistoryEntry (long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(HISTORY_TABLE_NAME,
                "id = ? ",
                new String[] { Long.toString(id) });
    }

    public List<Long> getHistoryEntries(Date startdate, Date enddate) {
        List<Long> list = new ArrayList<>();

        String sqlquery = "select " + HISTORY_COLUMN_ID +  " from " + HISTORY_TABLE_NAME;

        if(startdate == null){
            if(enddate != null){
                sqlquery += " where " + HISTORY_COLUMN_TIME + " <= '" + dateFormat.format(enddate) + "'";
            }
        }else{
            if (enddate == null){
                sqlquery += " where " + HISTORY_COLUMN_TIME + " >= '" + dateFormat.format(startdate) + "'";
            }else{
                sqlquery += " where " + HISTORY_COLUMN_TIME + " >= '" + dateFormat.format(startdate) + "' and " +
                HISTORY_COLUMN_TIME + " <= '" + dateFormat.format(enddate) + "'";
            }
        }

        sqlquery += " order by " + HISTORY_COLUMN_TIME + " desc";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery(sqlquery, null );
        res.moveToFirst();

        while(!res.isAfterLast()){
            list.add(Long.valueOf(res.getInt(res.getColumnIndex(HISTORY_COLUMN_ID))));
            res.moveToNext();
        }
        res.close();
        return list;
    }

    public HashMap<String, Float> getAllServingsTimePeriod(Date startdate, Date enddate) {
        HashMap<String, Float> hmap = new HashMap<>();

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select " + HISTORY_COLUMN_FOODNAME + " , sum("+ HISTORY_COLUMN_SERVINGS +") from " +
                HISTORY_TABLE_NAME + " where " + HISTORY_COLUMN_TIME + " between '" +
                dateFormat.format(startdate) + "" + "' and '" + dateFormat.format(enddate) +
                "' group by " + HISTORY_COLUMN_FOODNAME, null);
        res.moveToFirst();
        while(!res.isAfterLast()){
            hmap.put(res.getString(0), res.getFloat(1));
            res.moveToNext();
        }
        res.close();

        return hmap;
    }

    public Bundle getMeal(long mealID){
        SQLiteDatabase db = this.getReadableDatabase();
        Bundle b = new Bundle();

        String selection = HISTORY_COLUMN_ID + " = ? ";
        String[] columns = {HISTORY_COLUMN_FOODNAME, HISTORY_COLUMN_TIME, HISTORY_COLUMN_SERVINGS,
        HISTORY_COLUMN_IMGPATH};
        String[] selectionArgs = {  Long.toString(mealID)  };

        Cursor cursor = db.query(HISTORY_TABLE_NAME, columns, selection, selectionArgs,
                null, null, null);

        if(cursor != null){
            b.putString("FoodName", cursor.getString(0));
            b.putFloat("ServingAmt", cursor.getFloat(2));
            try{
                b.putSerializable("TimeConsumed", dateFormat.parse(cursor.getString(1)));
                b.putSerializable("FoodImg", new File(cursor.getString(3)));
            }catch (ParseException e){
                Log.e("getMeal", "Parse Error: " + e.getMessage());
            }

        }

        return b;
    }

    //Methods for manipulating DishID table

    public void insertNewDishID(String FoodName, int ver, String NutritionJSONstr, String IngListstr, String ImgPath){

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DISHID_COLUMN_FOODNAME, FoodName);
        cv.put(DISHID_COLUMN_VERSION, ver);
        cv.put(DISHID_COLUMN_NUTRITIONJSON, NutritionJSONstr);
        cv.put(DISHID_COLUMN_INGREDIENTLIST, IngListstr);
        cv.put(DISHID_COLUMN_IMGPATH, ImgPath);

        db.insertWithOnConflict(HISTORY_TABLE_NAME, null, cv, SQLiteDatabase.CONFLICT_REPLACE);

    }

    public Bundle getDishID(String FoodName){

        SQLiteDatabase db = this.getReadableDatabase();
        Bundle b = new Bundle();

        String selection = DISHID_COLUMN_FOODNAME + " = ? ";

        String[] selectionArgs = {"'" + FoodName + "'"};

        Cursor cursor = db.query(DISHID_TABLE_NAME, null, selection, selectionArgs,
                null, null, null);

        if (cursor.moveToFirst()){

            b.putBoolean("Exists", true);
            b.putString("FoodName", cursor.getString(0));
            b.putInt("Version", cursor.getInt(1));
            b.putSerializable("FoodImg", new File(cursor.getString(2)));
            b.putString("Nutrition", cursor.getString(3));
            b.putString("Ingredients", cursor.getString(4));

        }else{
            b.putBoolean("Exists", false);
        }

        cursor.close();

        return b;

    }

    public Integer deleteDishIDEntry (String FoodName) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(HISTORY_TABLE_NAME,
                DISHID_COLUMN_FOODNAME + " = ? ",
                new String[] { "'" + FoodName + "'" });
    }

}