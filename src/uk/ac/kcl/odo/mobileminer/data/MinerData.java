// Licensed under the Apache License Version 2.0: http://www.apache.org/licenses/LICENSE-2.0.txt
package uk.ac.kcl.odo.mobileminer.data;

import java.math.BigInteger;
import java.net.InetAddress;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import uk.ac.kcl.odo.mobileminer.cells.CountedCell;
import uk.ac.kcl.odo.mobileminer.cells.MinerLocation;
import uk.ac.kcl.odo.mobileminer.data.MinerTables.BookKeepingTable;
import uk.ac.kcl.odo.mobileminer.data.MinerTables.GSMCellPolygonTable;
import uk.ac.kcl.odo.mobileminer.data.MinerTables.GSMCellTable;
import uk.ac.kcl.odo.mobileminer.data.MinerTables.GSMLocationTable;
import uk.ac.kcl.odo.mobileminer.data.MinerTables.MinerLogTable;
import uk.ac.kcl.odo.mobileminer.data.MinerTables.MobileNetworkTable;
import uk.ac.kcl.odo.mobileminer.data.MinerTables.NetworkTrafficTable;
import uk.ac.kcl.odo.mobileminer.data.MinerTables.NotificationTable;
import uk.ac.kcl.odo.mobileminer.data.MinerTables.SocketTable;
import uk.ac.kcl.odo.mobileminer.data.MinerTables.WifiNetworkTable;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
//import android.util.Log;

public class MinerData extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "MobileMiner.db";
    public static final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    public static final SimpleDateFormat dayGetter = new SimpleDateFormat("EEE");
    
    public static class WifiData {
    	private String ssid,bssid,ip;
    	public WifiData() {
    		ssid = "null"; bssid = "null"; ip = "null";
    	}
    	public WifiData(WifiInfo info) {
    		String[] chunks;
			ssid = info.getSSID();
			if (ssid == null) ssid = "null";
			bssid = info.getBSSID();
			if (bssid == null) bssid = "null";
			try {
				// http://stackoverflow.com/questions/17055946/android-formatter-formatipaddress-deprecation-with-api-12
				ip = InetAddress.getByAddress(BigInteger.valueOf(info.getIpAddress()).toByteArray()).getHostAddress();
				chunks = ip.split("\\.");
				Collections.reverse(Arrays.asList(chunks));
				ip = TextUtils.join(".",chunks);
			}
			catch (Exception e) {
				ip = "null";
			}			
    	}
    	public String getSSID() {return ssid;}
    	public String getBSSID() {return bssid;}
    	public String getIP() {return ip;}
    	
    }
    
	public MinerData(Context context, String name, CursorFactory factory, int version) {
		super(context, name, factory, version);
	}

    public MinerData(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
	
	public void onCreate(SQLiteDatabase db) {
		for (String sql: MinerTables.CreateTables)
			 {
			try {
				db.execSQL(sql);
			}
			catch (Exception e) {
			}
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//		if (oldVersion == 1) {
//			for (String table: MinerTables.ExpirableTables) {
//				try {
//					db.execSQL("DROP TABLE "+table+";");
//				}
//				catch (Exception e) {
//					
//				}
//			}
//			onCreate(db);
//		}
		
	}

	public void putSocket(SQLiteDatabase db, String proc, String prot, String addr, Date opened, Date closed) {
		String[] chunks;
		ContentValues values = new ContentValues();
		values.put(SocketTable.COLUMN_NAME_PROCESS,proc);
		values.put(SocketTable.COLUMN_NAME_PROTOCOL,prot);
		chunks = addr.split(":");
		values.put(SocketTable.COLUMN_NAME_IP,chunks[0]);
		values.put(SocketTable.COLUMN_NAME_PORT,chunks[1]);
		values.put(SocketTable.COLUMN_NAME_OPENED,df.format(opened));
		values.put(SocketTable.COLUMN_NAME_CLOSED,df.format(closed));
		values.put(SocketTable.COLUMN_NAME_DAY,dayGetter.format(opened));
		putRow(db,SocketTable.TABLE_NAME,values);
	}

	public void putGSMCell(SQLiteDatabase db, MinerLocation location, Date time) {
		ContentValues values = new ContentValues();
		values.put(GSMCellTable.COLUMN_NAME_MCC,location.getMcc());
		values.put(GSMCellTable.COLUMN_NAME_MNC,location.getMnc());
		values.put(GSMCellTable.COLUMN_NAME_LAC,location.getLac());
		values.put(GSMCellTable.COLUMN_NAME_CELLID,location.getId());
		values.put(GSMCellTable.COLUMN_NAME_STRENGTH,location.getStrength());
		values.put(GSMCellTable.COLUMN_NAME_TIME,df.format(time));
		values.put(SocketTable.COLUMN_NAME_DAY,dayGetter.format(time));
		putRow(db,GSMCellTable.TABLE_NAME,values);
	}

	public void putGSMLocation(SQLiteDatabase db, String Mcc, String Mnc, String Lac, String Id, String Lat, String Long,
		String Source,Date time) {
			ContentValues values = new ContentValues();
			values.put(GSMLocationTable.COLUMN_NAME_MCC,Mcc);
			values.put(GSMLocationTable.COLUMN_NAME_MNC,Mnc);
			values.put(GSMLocationTable.COLUMN_NAME_LAC,Lac);
			values.put(GSMLocationTable.COLUMN_NAME_CELLID,Id);
			values.put(GSMLocationTable.COLUMN_NAME_LAT,Lat);
			values.put(GSMLocationTable.COLUMN_NAME_LONG,Long);
			values.put(GSMLocationTable.COLUMN_NAME_SOURCE,Source);
			values.put(GSMLocationTable.COLUMN_NAME_TIME,df.format(time));
			putRow(db,GSMLocationTable.TABLE_NAME,values);
		}
	
	public void putGSMCellPolygon(SQLiteDatabase db, String Mcc, String Mnc, String Lac, String Id, String Json, 
		String Source,Date time) {
			ContentValues values = new ContentValues();
			values.put(GSMCellPolygonTable.COLUMN_NAME_MCC,Mcc);
			values.put(GSMCellPolygonTable.COLUMN_NAME_MNC,Mnc);
			values.put(GSMCellPolygonTable.COLUMN_NAME_LAC,Lac);
			values.put(GSMCellPolygonTable.COLUMN_NAME_CELLID,Id);
			values.put(GSMCellPolygonTable.COLUMN_NAME_JSON,Json);
			values.put(GSMCellPolygonTable.COLUMN_NAME_SOURCE,Source);
			values.put(GSMCellPolygonTable.COLUMN_NAME_TIME,df.format(time));
			putRow(db,GSMCellPolygonTable.TABLE_NAME,values);
		}
	
	public void putMobileNetwork(SQLiteDatabase db, TelephonyManager manager, Date time) {
		ContentValues values = new ContentValues();
		values.put(MobileNetworkTable.COLUMN_NAME_NETWORKNAME, manager.getNetworkOperatorName());
		values.put(MobileNetworkTable.COLUMN_NAME_NETWORK, manager.getNetworkOperator());
		values.put(MobileNetworkTable.COLUMN_NAME_TIME,df.format(time));
		putRow(db,MobileNetworkTable.TABLE_NAME,values);	
	}
	
	public void putWifiNetwork(SQLiteDatabase db, WifiData data, Date time ) {
		ContentValues values = new ContentValues();
		values.put(WifiNetworkTable.COLUMN_NAME_SSID,data.getSSID());
		values.put(WifiNetworkTable.COLUMN_NAME_BSSID,data.getBSSID());
		values.put(WifiNetworkTable.COLUMN_NAME_IP,data.getIP());
		values.put(WifiNetworkTable.COLUMN_NAME_TIME,df.format(time));
		values.put(SocketTable.COLUMN_NAME_DAY,dayGetter.format(time));
		putRow(db,WifiNetworkTable.TABLE_NAME,values);
	}
	
	public void putMinerLog(SQLiteDatabase db, Date start, Date stop) {
		ContentValues values = new ContentValues();
		values.put(MinerLogTable.COLUMN_NAME_START,df.format(start));
		values.put(MinerLogTable.COLUMN_NAME_STOP,df.format(stop));
		putRow(db,MinerLogTable.TABLE_NAME,values);
	}	

	public void putNotification(SQLiteDatabase db, String packageName, Date time) {
		ContentValues values = new ContentValues();
		values.put(NotificationTable.COLUMN_NAME_PACKAGE,packageName);
		values.put(NotificationTable.COLUMN_NAME_TIME,df.format(time));
		values.put(SocketTable.COLUMN_NAME_DAY,dayGetter.format(time));
		putRow(db,NotificationTable.TABLE_NAME,values);
	}	

// None of our business...
//	public void putNotification(SQLiteDatabase db, String packageName, String text, Date time) {
//		ContentValues values = new ContentValues();
//		values.put(NotificationTable.COLUMN_NAME_PACKAGE,packageName);
//		values.put(NotificationTable.COLUMN_NAME_TEXT,text);
//		values.put(NotificationTable.COLUMN_NAME_TIME,df.format(time));
//		putRow(db,NotificationTable.TABLE_NAME,values);
//	}	
	
	public void putNetworkTraffic(SQLiteDatabase db, boolean tx, String packageName, Date start, Date stop, long bytes) {
		ContentValues values = new ContentValues();
		if (tx) {
			values.put(NetworkTrafficTable.COLUMN_NAME_TX,"1");
		}
		else {
			values.put(NetworkTrafficTable.COLUMN_NAME_TX,"0");
		}
		values.put(NetworkTrafficTable.COLUMN_NAME_PROCESS,packageName);
		values.put(NetworkTrafficTable.COLUMN_NAME_START,df.format(start));
		values.put(NetworkTrafficTable.COLUMN_NAME_STOP,df.format(stop));
		values.put(NetworkTrafficTable.COLUMN_NAME_DAY,dayGetter.format(start));
		values.put(NetworkTrafficTable.COLUMN_NAME_BYTES,Long.toString(bytes));
		putRow(db,NetworkTrafficTable.TABLE_NAME,values);
		
	}
		
	private void putRow(SQLiteDatabase db, String table, ContentValues values ) {
		// http://developer.android.com/training/basics/data-storage/databases.html#WriteDbRow
		try {
			db.insert(table,null,values);
		}
		catch(Exception e) {

		}
	}
	
	private String howLongAgo(Date date) {
		long divider,count;
		long howLong = new Date().getTime() - date.getTime();
		String unit;

		divider = 1000; unit = "Second";
		if (howLong > 60000 && howLong < 3600000) {divider = 60000; unit = "Minute";}
		if (howLong > 3600000 && howLong < 86400000) {divider = 3600000; unit = "Hour";}
		if (howLong > 86400000) {divider = 86400000; unit = "Day";}
		
		count = howLong / divider;
		
		if (count < 2) {
			return "one "+unit+" ago.";
		}
		else {
			return Long.toString(count)+" "+unit+"s ago";
		}
		

	}
	
	public void deleteBookKeepingKey(SQLiteDatabase db, String key) {
		db.beginTransaction();
		db.delete(BookKeepingTable.TABLE_NAME, BookKeepingTable.COLUMN_NAME_KEY + " = ? ", new String[]{key});
		db.setTransactionSuccessful();
		db.endTransaction();	
	}
	
	public void setBookKeepingKey(SQLiteDatabase db, String key, String value) {
		ContentValues values = new ContentValues();
		values.put(BookKeepingTable.COLUMN_NAME_KEY, key);
		values.put(BookKeepingTable.COLUMN_NAME_VALUE, value);
		db.beginTransaction();
		putRow(db,BookKeepingTable.TABLE_NAME,values);
		db.setTransactionSuccessful();
		db.endTransaction();	
	}
	
	public String getBookKeepingKey(SQLiteDatabase db, String key) {
		String[] retColumns = {BookKeepingTable.COLUMN_NAME_VALUE};
		String[] whereValues = {key};
		Cursor c = db.query(BookKeepingTable.TABLE_NAME,retColumns,BookKeepingTable.COLUMN_NAME_KEY+" = ?",whereValues,null,null,null);
		c.moveToFirst();
		try {
			return c.getString(c.getColumnIndex(BookKeepingTable.COLUMN_NAME_VALUE));
		}
		catch (Exception e) {
			return null;
		}
	}
	
	private void initBookKeepingDate(SQLiteDatabase db,String key) {
		ContentValues values = new ContentValues();
		values.put(BookKeepingTable.COLUMN_NAME_KEY, key);
		values.put(BookKeepingTable.COLUMN_NAME_VALUE, BookKeepingTable.NULL_DATE);
		putRow(db,BookKeepingTable.TABLE_NAME,values);
	}
	
	public Date getBookKeepingDate(SQLiteDatabase db,String key) {
		String[] retColumns = {BookKeepingTable.COLUMN_NAME_VALUE};
		String[] whereValues = {key};
		Cursor c = db.query(BookKeepingTable.TABLE_NAME,retColumns,BookKeepingTable.COLUMN_NAME_KEY+" = ?",
			whereValues,null,null,null);
		c.moveToFirst();
		String dateString;	
		try {
			dateString = c.getString(c.getColumnIndex(BookKeepingTable.COLUMN_NAME_VALUE));
		}
		catch (Exception e) {
			initBookKeepingDate(db,key);
			dateString = BookKeepingTable.NULL_DATE;
		}
		
		if (dateString.equals(BookKeepingTable.NULL_DATE)) {
			return null;
		}
		else {
			try {
				return df.parse(dateString);
			} catch (ParseException e) {
				return null;
			}
		}	
	}
	
	public void setBookKeepingDate(SQLiteDatabase db,String key,Date date) {
		// http://developer.android.com/training/basics/data-storage/databases.html#UpdateDbRow
		ContentValues values = new ContentValues();
		String[] whereArgs = {key};
		values.put(BookKeepingTable.COLUMN_NAME_VALUE, df.format(date));
		db.update(BookKeepingTable.TABLE_NAME, values, BookKeepingTable.COLUMN_NAME_KEY+" = ?", whereArgs);
	}
	
	public static String[] getCellLocation(SQLiteDatabase db, String Mcc, String Mnc, String Lac, String Id) {
		String[] retColumns = {GSMLocationTable.COLUMN_NAME_LAT,GSMLocationTable.COLUMN_NAME_LONG};
		String q = TextUtils.join(" = ? AND ",new String[]{GSMLocationTable.COLUMN_NAME_MCC,GSMLocationTable.COLUMN_NAME_MNC,
		GSMLocationTable.COLUMN_NAME_LAC,GSMLocationTable.COLUMN_NAME_CELLID})+ " = ?";
		String[] whereValues = {Mcc,Mnc,Lac,Id};
		Cursor c = db.query(GSMLocationTable.TABLE_NAME,retColumns,q,whereValues,null,null,null);
		c.moveToFirst();
		try {
			return new String[]{c.getString(c.getColumnIndex(GSMLocationTable.COLUMN_NAME_LAT)),
				c.getString(c.getColumnIndex(GSMLocationTable.COLUMN_NAME_LONG))};
		}
		catch (Exception e) {
			return null;
		}
	}
	
	public String getCellPolygon(SQLiteDatabase db, String Mcc, String Mnc, String Lac, String Id) {
		String[] retColumns = {GSMCellPolygonTable.COLUMN_NAME_JSON};
		String[] whereValues = {Mcc,Mnc,Lac,Id};
		String q = TextUtils.join(" = ? AND ",new String[]{GSMCellPolygonTable.COLUMN_NAME_MCC,GSMCellPolygonTable.COLUMN_NAME_MNC,
			GSMCellPolygonTable.COLUMN_NAME_LAC,GSMCellPolygonTable.COLUMN_NAME_CELLID})+ " = ?";
		Cursor c = db.query(GSMCellPolygonTable.TABLE_NAME,retColumns,q,whereValues,null,null,null);
		try {
			return c.getString(c.getColumnIndex(GSMCellPolygonTable.COLUMN_NAME_JSON));
		}
		catch (Exception e) {
			return null;
		}
	}
	
	public static ArrayList<CountedCell> getMyCells(SQLiteDatabase db) {
		ArrayList<CountedCell> cells = new ArrayList<CountedCell>();
		final String myCellLocationsQuery = "SELECT count(*) AS count,mcc, mnc, lac, cid FROM gsmcell GROUP BY lac,cid ORDER BY count DESC";
		Cursor c = db.rawQuery(myCellLocationsQuery, null);
		c.moveToFirst();
		boolean searching = true;
		while (searching) {
			searching = !c.isLast();
			try {
				cells.add(new CountedCell(
					c.getInt(c.getColumnIndex("count")),
					c.getString(c.getColumnIndex("mcc")),
					c.getString(c.getColumnIndex("mnc")),
					c.getString(c.getColumnIndex("lac")),
					c.getString(c.getColumnIndex("cid"))
				));
			}
			catch (Exception e) {
				
			}
			c.moveToNext();
		}
		
		return cells;
	}
	
	public Cursor socketsByProc(String[] projection, String selection, String[] selectionArgs) {
		SQLiteDatabase db = getReadableDatabase();
		
		String[] retCols = new String[projection.length+1];
		
		System.arraycopy(projection, 0, retCols, 0, projection.length);
		System.arraycopy(new String[]{"_id"}, 0, retCols, projection.length, 1);
				
		return db.query(MinerTables.SocketTable.TABLE_NAME,retCols,selection,selectionArgs,
				MinerTables.SocketTable.COLUMN_NAME_PROCESS,null,"COUNT(*) DESC",null);
		
	}
	
	public String getLastExported(SQLiteDatabase db) {
		Date exported = getBookKeepingDate(db,BookKeepingTable.DATA_LAST_EXPORTED);
		if (exported != null) {
			return howLongAgo(exported);
		}
		return null;
	}
	
	public void setLastExported(SQLiteDatabase db, Date date) {
		setBookKeepingDate(db, BookKeepingTable.DATA_LAST_EXPORTED,date);		
	}
	
	public String getLastExpired(SQLiteDatabase db) {
		Date expired = getBookKeepingDate(db,BookKeepingTable.DATA_LAST_EXPIRED);
		if (expired != null) {
			return howLongAgo(expired);
		}
		return null;
	}
	
	public void expireData(SQLiteDatabase db) {
		Date expiryDate = getBookKeepingDate(db,BookKeepingTable.DATA_LAST_EXPORTED);
		if (expiryDate == null) return;
		String[] expiryValues = {df.format(expiryDate)};
		int i;
		for (i=0;i<MinerTables.ExpirableTables.length;i++) {
			db.delete(MinerTables.ExpirableTables[i], MinerTables.ExpirableTimeStamps[i]+ " < ?", expiryValues);
		}
		getBookKeepingDate(db,BookKeepingTable.DATA_LAST_EXPIRED);
		setBookKeepingDate(db,BookKeepingTable.DATA_LAST_EXPIRED,expiryDate);
	}
	
	public ArrayList<String> topApps(SQLiteDatabase db) {
		ArrayList<String> procs = new ArrayList<String>(); 
		String mySocketsQuery = "SELECT process FROM socket GROUP BY process ORDER BY count(*) DESC";
		Cursor c = db.rawQuery(mySocketsQuery, null);
		c.moveToFirst();
		Boolean searching = true;
		while (searching) {
			searching = !c.isLast();
			try {
				procs.add(c.getString(c.getColumnIndex("process")));
			} catch (Exception E) {
				return procs;
			}
		}	
		
		return procs;
	}
	
}
