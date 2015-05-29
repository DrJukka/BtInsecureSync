// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.

package org.thaliproject.p2p.btinsecuresync;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import org.thaliproject.p2p.btpollingsynclib.BtPollingSyncService;
import org.thaliproject.p2p.btpollingsynclib.PeerDataItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by juksilve on 21.5.2015.
 */
public class PeersActivity extends Activity{

    PeersActivity that = this;
    ListView listView = null;
    ArrayList<String> listItems = new ArrayList<String>();
    ArrayAdapter<String> adapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.showpeers);

        Button bcButton = (Button) findViewById(R.id.backtomain);
        bcButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), MainActivity.class);
                startActivity(intent);
            }
        });

        Button cuntButton = (Button) findViewById(R.id.addnewPeer);
        cuntButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), PeerManagerActivity.class);
                startActivity(intent);
            }
        });

        Button rfButton = (Button) findViewById(R.id.SendMsg);
        rfButton .setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                boolean isOk = false;
                BtPollingSyncService  service = ((MyDataHandlerApp)getApplicationContext()).GetService();
                if(service != null) {
                    if (service.isRunnuing()) {
                        isOk = true;
                    }
                }
                if(isOk) {
                    SparseBooleanArray checked = listView.getCheckedItemPositions();
                    ArrayList<String> selectedItems = new ArrayList<String>();
                    for (int i = 0; i < checked.size(); i++) {
                        // Item position in adapter
                        int position = checked.keyAt(i);
                        // Add sport if it is checked i.e.) == TRUE!
                        if (checked.valueAt(i))
                            selectedItems.add(adapter.getItem(position));
                    }

                    SendMessage(selectedItems);
                }else{
                    showAlert("Error","Make sure the service is running before sending messages");
                }
           }
        });

        listView = (ListView) findViewById(R.id.list);
        reSetTheListArray();
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            public boolean onItemLongClick(AdapterView<?> arg0, View arg1,int pos, long id) {
                deleteFromDatabase((String) listView.getItemAtPosition(pos));
                return true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        reSetTheListArray();
    }

    private void reSetTheListArray(){
        if(listItems != null && listView!= null ) {
            listItems.clear();
            PeerManagerDb db = ((MyDataHandlerApp) getApplicationContext()).GetDb();
            if (db != null) {
                List<PeerDataItem> pers = db.GetPeerList();
                if (pers != null) {
                    for (int i = 0; i < pers.size(); i++) {
                        listItems.add(pers.get(i).Name + ";" + pers.get(i).Address);
                    }
                }
            }

            adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice, listItems);
            listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
            listView.setAdapter(adapter);
        }
    }

    private void SendMessage(ArrayList<String> selectedItems) {

        final BtPollingSyncService service = ((MyDataHandlerApp)getApplicationContext()).GetService();
        if(service == null) {
            showAlert("Error","Service is NULL !!");
        }
        else if(selectedItems != null && selectedItems.size() > 0){

            final ArrayList<String> selectedItemsTmp = selectedItems;

            String tittle = "Schedule message to " + selectedItems.size() + " Peers ?";

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(tittle);

            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            builder.setView(input);

            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    String message = input.getText().toString();
                    if(message.length() > 0) {
                        int cunt = 0;
                        for (int i = 0; i < selectedItemsTmp.size(); i++) {

                            String[] separated = selectedItemsTmp.get(i).split(";");

                            if (separated.length > 1) {
                                final String name = separated[0];
                                final String address = separated[1];
                                cunt = cunt + 1;
                                service.SyncDataChanged(address, name, message, false);
                            }
                        }

                        showAlert("Error",cunt + " messages scheduled for sending.");
                    }else{
                        showAlert("Error","Please define the message for sending.");
                    }
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            builder.show();
        }
    }

    private void deleteFromDatabase(String selectedItem){

        String[] separated = selectedItem.split(";");
        final PeerManagerDb db = ((MyDataHandlerApp)getApplicationContext()).GetDb();
        if(db != null && separated.length > 1) {
            final String name = separated[0];
            final String address = separated[1];

            new AlertDialog.Builder(this)
                    .setTitle("Delete peer from database")
                    .setMessage(name + " with Bluetooth address " + address)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            db.removeItemData(address);
                            reSetTheListArray();
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothing
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
    }

    private void showAlert(String title, String message){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setCancelable(true);
        builder.setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialog, int which) {}});

        final AlertDialog dlg = builder.create();
        dlg.show();

        final Timer t = new Timer();
        t.schedule(new TimerTask() {
            public void run() {
                dlg.dismiss(); // when the task active then close the dialog
                t.cancel(); // also just top the timer thread, otherwise, you may receive a crash report
            }
        }, 5000);

    }

}
