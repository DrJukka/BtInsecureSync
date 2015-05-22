package org.thaliproject.p2p.btinsecuresync;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by juksilve on 21.5.2015.
 */
public class PeerManagerActivity extends Activity implements WifiBase.WifiStatusCallBack {

    ListView listView = null;
    ArrayList<String> listItems = new ArrayList<String>();
    ArrayAdapter<String> adapter = null;

    WifiBase mWifiBase = null;
    WifiServiceAdvertiser mWifiServiceAdvertiser = null;
    WifiServiceSearcher mWifiServiceSearcher = null;

    Timer timmer = new Timer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.peermanager);

        Button bcButton = (Button) findViewById(R.id.backtomain);
        bcButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), PeersActivity.class);
                startActivity(intent);
            }
        });

        Button cuntButton = (Button) findViewById(R.id.saveAllPeers);
        cuntButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(listItems != null){
                    for(int i =0; i < listItems.size(); i++){
                        SavetoDatabase(listItems.get(i));
                    }
                }
            }
        });

        Button rfButton = (Button) findViewById(R.id.refresh);
        rfButton .setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mWifiServiceSearcher != null && listItems != null && adapter != null){
                    listItems.clear();
                    adapter.notifyDataSetChanged();
                    mWifiServiceSearcher.startPeerDiscovery();
                }
            }
        });

        listView = (ListView) findViewById(R.id.list);
        adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,listItems);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,int position, long id) {
                SavetoDatabase((String) listView.getItemAtPosition(position));
            }

        });

        mWifiBase = new WifiBase(this,this);
        mWifiBase.Start();

        StartWifiDirect();
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        timmer.cancel();


        StopWifiDirect();

        if(mWifiBase != null){
            mWifiBase.Stop();
            mWifiBase = null;
        }
    }

    private void StartWifiDirect(){
        StopWifiDirect();

        if(mWifiBase != null) {

            mWifiServiceAdvertiser = new WifiServiceAdvertiser(mWifiBase.GetWifiP2pManager(),mWifiBase.GetWifiChannel(),this);
            mWifiServiceAdvertiser.Start();

            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showAlert("Hint", "In order other devices to be visible for the search, they also need to be in this same view and have the search active.");
                        }
                    });
                }
            };

            timmer.schedule(task, 10000);

            mWifiServiceSearcher = new WifiServiceSearcher(this,mWifiBase.GetWifiP2pManager(),mWifiBase.GetWifiChannel(),this);
            mWifiServiceSearcher.Start();
            mWifiServiceSearcher.startPeerDiscovery();
        }
    }

    private void StopWifiDirect(){

        if(mWifiServiceAdvertiser != null){
            mWifiServiceAdvertiser.Stop();
            mWifiServiceAdvertiser = null;
        }

        if(mWifiServiceSearcher != null){
            mWifiServiceSearcher.Stop();
            mWifiServiceSearcher = null;
        }
    }

    private void SavetoDatabase(String selectedItem){

        String[] separated = selectedItem.split(";");
        final PeerManagerDb db = ((MyDataHandlerApp)getApplicationContext()).GetDb();
        if(db != null && separated.length > 1) {
            final String name = separated[0];
            final String address = separated[1];

            new AlertDialog.Builder(this)
                    .setTitle("Save peer to database")
                    .setMessage(name + " with Bluetooth address " + address)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            db.addOrUpdateItemData(address,name);
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

    @Override
    public void WifiStateChanged(int state) {
        if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
            StartWifiDirect();
        } else {
            //no wifi availavble, thus we need to stop doing anything;
            StopWifiDirect();
            showAlert("Wifi is Disabled","Please enable Wifi, otherwise no peers will be found.");
        }
    }

    @Override
    public void gotService(ServiceItem item) {

        if(listItems != null && adapter != null) {
            timmer.cancel();

            listItems.add(item.instanceName + ";" + item.instanceAddress);
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void LocalServiceStartError(String error) {
        showAlert("Local service error", error);
    }

    @Override
    public void PeerStartError(String error) {
        showAlert("Peer discovery error", error);
    }

    @Override
    public void ServiceStartError(String error) {
        showAlert("Service discovery error", error);
    }

    @Override
    public void AddReqStartError(String error) {
        showAlert("Add reguest error", error);
    }

}
