// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.

package org.thaliproject.p2p.btpollingsynclib;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by juksilve on 20.5.2015.
 */
public class PeerDatabaseHandler {

    //todo add broadcast for any change on database

    SQLiteDatabase dataBase = null;
    Context context = null;

    public interface PeerDataChangedCallback{
        public void SyncDataChanged(String Address,String  Name, String  data, boolean isSynched);
        public void GotSyncData(String Address,String  Name, String  data);
    }


    final List<PeerDataItem> myPeerDataItemList = new ArrayList<>();
    final List<PeerSynchStatus> myPeerSynchStatusList = new ArrayList<>();

    public PeerDatabaseHandler(Context Context){
        this.context = Context;
        this.dataBase= Context.openOrCreateDatabase("PeerItemDataBase", Context.MODE_PRIVATE, null);
        this.dataBase.execSQL("CREATE TABLE IF NOT EXISTS peerdataitems(address VARCHAR PRIMARY KEY,name VARCHAR,data VARCHAR);");
        this.dataBase.execSQL("CREATE TABLE IF NOT EXISTS peersychstatus(address VARCHAR PRIMARY KEY,name VARCHAR,data VARCHAR,status INTEGER);");
        reFreshItemDataList();
        reFreshSynchStatusList();

        print_line("DB","tables are cleared, items: " + myPeerDataItemList.size() + ", syncData: " + myPeerSynchStatusList.size());
    }

    public void closeDataBase(){
        if(this.dataBase != null) {
            this.dataBase.close();
            this.dataBase = null;
        }
    }

    public List<PeerDataItem> GetPeerDataItemList(){
        return myPeerDataItemList;
    }

    public void removeItemData(String Address) {
        if(this.dataBase != null) {
            this.dataBase.execSQL("DELETE FROM peerdataitems WHERE address='" + Address.toUpperCase().trim() + "'");
            reFreshItemDataList();
        }
    }
    public void addOrUpdateItemData(String Address,String Name, String data){
        if(this.dataBase != null) {
            this.dataBase.execSQL("INSERT OR REPLACE INTO peerdataitems VALUES('" + Address.toUpperCase().trim() + "','" + Name + "','" + data + "');");
            reFreshItemDataList();
        }
    }

    private void reFreshItemDataList(){
        myPeerDataItemList.clear();
        if(this.dataBase != null) {
            Cursor cursor = this.dataBase.rawQuery("SELECT  * FROM peerdataitems", null);
            if (cursor.moveToFirst()) {
                do {
                    myPeerDataItemList.add(new PeerDataItem(cursor.getString(0), cursor.getString(1), cursor.getString(2)));
                } while (cursor.moveToNext());
            }
        }
    }

    public List<PeerSynchStatus>  GetPeerSynchStatusList(){
        return myPeerSynchStatusList;
    }

    public void removeSyncItem(String Address) {
        if(this.dataBase != null) {
            this.dataBase.execSQL("DELETE FROM peersychstatus WHERE address='" + Address.toUpperCase().trim() + "'");
            reFreshSynchStatusList();
        }
    }
    public void addOrUpdateSyncItem(String Address,String Name, String data, boolean status){
        if(this.dataBase != null) {

            Integer statusval = 0;
            if(status){
                statusval = 1;
            }

            this.dataBase.execSQL("INSERT OR REPLACE INTO peersychstatus VALUES('" + Address.toUpperCase().trim() + "','" + Name + "','" + data + "','" + statusval + "');");
            reFreshSynchStatusList();
        }
        print_line("DB","addOrUpdateSyncItem, syncData: " + myPeerSynchStatusList.size());
    }

    private void reFreshSynchStatusList(){
        myPeerSynchStatusList.clear();
        if(this.dataBase != null) {
            Cursor cursor = this.dataBase.rawQuery("SELECT  * FROM peersychstatus", null);
            if (cursor.moveToFirst()) {
                do {
                    boolean synched = false;
                    if(cursor.getInt(3) > 0){
                        synched = true;
                    }

                    print_line("DB","read item: " + cursor.getString(1) + ", address : " + cursor.getString(0) + ", synched: " + synched);

                    myPeerSynchStatusList.add(new PeerSynchStatus(cursor.getString(1), cursor.getString(0), cursor.getString(2),synched));
                } while (cursor.moveToNext());
            }
        }
    }

    public void print_line(String who, String line) {
        //latsDbgString = who + " : " + line;
        Log.i("PeerDatabaseHandler" + who, line);
    }
}
