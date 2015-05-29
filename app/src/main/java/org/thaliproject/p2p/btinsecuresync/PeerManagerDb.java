// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.

package org.thaliproject.p2p.btinsecuresync;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.thaliproject.p2p.btpollingsynclib.PeerDataItem;
import org.thaliproject.p2p.btpollingsynclib.PeerSynchStatus;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by juksilve on 20.5.2015.
 */
public class PeerManagerDb {

    //todo add broadcast for any change on database

    SQLiteDatabase dataBase = null;
    Context context = null;

    final List<PeerDataItem> myPeerDataItemList = new ArrayList<>();

    public PeerManagerDb(Context Context){
        this.context = Context;
        this.dataBase= Context.openOrCreateDatabase("PeersDataBase", Context.MODE_PRIVATE, null);
        this.dataBase.execSQL("CREATE TABLE IF NOT EXISTS peeritems(address VARCHAR PRIMARY KEY,name VARCHAR);");
        reFreshItemDataList();

        print_line("DB","tables are cleared, items: " + myPeerDataItemList.size());
    }

    public void closeDataBase(){
        if(this.dataBase != null) {
            this.dataBase.close();
            this.dataBase = null;
        }
    }

    public List<PeerDataItem> GetPeerList(){
        return myPeerDataItemList;
    }

    public void removeItemData(String Address) {
        if(this.dataBase != null) {
            this.dataBase.execSQL("DELETE FROM peeritems WHERE address='" + Address + "'");
            reFreshItemDataList();
        }
    }
    public void addOrUpdateItemData(String Address,String Name){
        if(this.dataBase != null) {
            this.dataBase.execSQL("INSERT OR REPLACE INTO peeritems VALUES('" + Address + "','" + Name +  "');");
            reFreshItemDataList();
        }
    }

    private void reFreshItemDataList(){
        myPeerDataItemList.clear();
        if(this.dataBase != null) {
            Cursor cursor = this.dataBase.rawQuery("SELECT  * FROM peeritems", null);
            if (cursor.moveToFirst()) {
                do {
                    myPeerDataItemList.add(new PeerDataItem(cursor.getString(1), cursor.getString(0), ""));
                } while (cursor.moveToNext());
            }
        }
    }


    public void print_line(String who, String line) {
        //latsDbgString = who + " : " + line;
        Log.i("PeerManagerDb" + who, line);
    }
}
