package com.fedir.smsdatalogger;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.os.Bundle;
import android.os.IBinder;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import android.telephony.SmsMessage;
import android.util.Log;


public class MyService extends Service {
    private static final String ACTION = "android.provider.Telephony.SMS_RECEIVED";
    BroadcastReceiver mReceiver;
    DBHelper dbHelper;

    final String LOG_TAG = getClass().getSimpleName();

    public void onCreate() {
        super.onCreate();
        Log.d(LOG_TAG, "onCreate");
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "onStartCommand");
        startListener();
        dbHelper = new DBHelper(this);
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }


    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        Log.d(LOG_TAG, "onDestroy");
    }

    public IBinder onBind(Intent intent) {
        Log.d(LOG_TAG, "onBind");
        return null;
    }

    private void saveInDB(SmsMessage message) {

        String senderPhoneNumber = message.getDisplayOriginatingAddress();
        String messageText = message.getDisplayMessageBody();
        long receiveDate = message.getTimestampMillis();

        ContentValues cv = new ContentValues();
        cv.put("messageText", messageText);
        cv.put("senderPhoneNumber", senderPhoneNumber);
        cv.put("receiveDate", receiveDate);
        SQLiteDatabase  db = dbHelper.getWritableDatabase();
        try {
            db.insert("message", "blank", cv);
        } finally {
            db.close();//fucking 15 API is not supporting try with resources:-)
        }
    }

    void startListener() {
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final Bundle bundle = intent.getExtras();

                try {

                    if (bundle != null) {
                        final Object[] pdusObj = (Object[]) bundle.get("pdus");
                        for (Object pdus : pdusObj) {
                            SmsMessage currentMessage = SmsMessage.createFromPdu((byte[]) pdus);
                            saveInDB(currentMessage);
                        }
                    }

                } catch (Exception e) {
                    Log.e("SmsReceiver", "Exception smsReceiver" + e);

                }
            }
        };
        try {
            unregisterReceiver(mReceiver);
        } catch (IllegalArgumentException e) {
            Log.e("SmsReceiver", "Exception smsReceiver" + e);
        }
        registerReceiver(
                mReceiver,
                new IntentFilter(ACTION));
    }

    class DBHelper extends SQLiteOpenHelper {

        public DBHelper(Context context) {

            super(context, "messageDB1", null, 2);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.d(LOG_TAG, "--- onCreate database ---");

            db.execSQL("create table message ("
                    + "id integer primary key autoincrement,"
                    + "messageText text,"
                    + "senderPhoneNumber text,"
                    + "receiveDate long"
                    + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }

}


}