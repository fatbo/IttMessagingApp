package com.oneport.manager;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.oneport.model.Msg;

public class SqliteController extends SQLiteOpenHelper {
	// table ----> msgid | content | time
	public static SqliteController singleton;
	public static String db_name = "OnePortDB";
	public String table_name = "msgTable";
	SQLiteDatabase db;
	public final static int show_amount = 40;

	public static void init(Context context) {
			singleton = new SqliteController(context, db_name);
	}

	public static SqliteController getInstance() {
		return singleton;
	}
	
	public SQLiteDatabase getDB(){
		return db;
	}
	
	public void setDB(SQLiteDatabase db){
		this.db = db;
	}

	private static final int VERSION = 3;// 資料庫版本

	public SqliteController(Context context, String name,
			CursorFactory factory, int version) {
		super(context, name, factory, version);
	}
	
	public void deleteAllData(){
		String DELETE = "DELETE FROM " + table_name;
		db.execSQL(DELETE);
	}

	public SqliteController(Context context, String name) {
		this(context, name, null, VERSION);
	}

	public SqliteController(Context context, String name, int version) {
		this(context, name, null, version);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		StringBuffer sb = new StringBuffer(10);
		sb.append("create table msgTable( ");
		sb.append("_ID INTEGER PRIMARY KEY  AUTOINCREMENT  NOT NULL, ");
		sb.append("msgId VARCHAR, ");
		sb.append("content VARCHAR, ");
		sb.append("time VARCHAR ");
		// msgDateTime in yyyy/MM/dd HH:mm:ss:SSS format
		sb.append(",msgDateTime VARCHAR ");
		
		sb.append(",msgType VARCHAR ");
		sb.append(",contentJson VARCHAR ");
		sb.append(") ");
		
		db.execSQL(sb.toString());
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
Log.d(this.getClass().getCanonicalName(), "onUpgarde, "+oldVersion+" -> "+newVersion);
		switch(oldVersion) {
			case 1:
				String addMsgDateTimeColumn = "alter table msgTable add msgDateTime varchar";
				db.execSQL(addMsgDateTimeColumn);
			case 2:
				String addMsgTypeColumn = "alter table msgTable add msgType varchar";
				db.execSQL(addMsgTypeColumn);
				String addContentJsonColumn = "alter table msgTable add contentJson varchar";
				db.execSQL(addContentJsonColumn);
		}
	}

	@Override
	public void onOpen(SQLiteDatabase db) {
		super.onOpen(db);
		// TODO 每次成功打開數據庫後首先被執行
	}

	public void getMsgFromDB() {
		//String queryStr = "select msgId,content,time from msgTable ORDER BY _ID DESC";
		String queryStr = "select msgId,content,time,msgDateTime,msgType,contentJson from msgTable ORDER BY msgDateTime DESC, msgId desc, _id desc";
		Cursor cursor = db.rawQuery(queryStr, null);
		int rows_num = cursor.getCount();
		if(rows_num > 40){
			rows_num = show_amount;
		}
		if (rows_num != 0) {
			cursor.moveToFirst();
			MsgManager.getInstance().msgList = new ArrayList<Msg>();
			for (int i = 0; i < rows_num; i++) {
				Msg msg = new Msg();
				msg.msgId = cursor.getString(0);
				msg.content = cursor.getString(1);
				msg.time = cursor.getString(2);
				msg.msgDateTime = cursor.getString(3);
				msg.msgType = cursor.getString(4);
				msg.contentJson= cursor.getString(5);
				MsgManager.getInstance().msgList.add(msg);
				cursor.moveToNext();
			}
		}
		
		cursor.close();
	}

	public boolean checkMsgIfExist(String msgId) {
		Cursor cursor = db.query(table_name, new String[] { "_ID", "msgId",
				"content", "time" }, "msgId" + "=?", new String[] { msgId },
				null, null, null, null);

		if (cursor.moveToFirst()) {
			return true;
		} else {
			return false;
		}
	}

	public boolean putMsgToDB(Msg msg) {
		try {
			ContentValues cv = new ContentValues();
			cv.put("msgId", msg.msgId);
			cv.put("content", msg.content);
			cv.put("time", msg.time);			
			cv.put("msgDateTime", msg.msgDateTime);
			
			cv.put("msgType", msg.msgType);
			cv.put("contentJson", msg.contentJson);
			long long1 = db.insert(table_name, "", cv);
			if (long1 == -1)
				return false;
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public synchronized void close() {
		super.close();
	}

}