package com.ease.home.homeease;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.ParcelUuid;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private BluetoothDevice connected_device;
    private OutputStream bluetoothOutStream;
    private Button connect;
    private TextView status;
    private BluetoothAdapter bluetoothAdapter;


    //all on and all off buttons
    Button all_on, all_off;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        List<ListItem> list = new ArrayList<>();

        //populate list items
        list.add(new ListItem("LED Blue", 0, 1));
        list.add(new ListItem("LED Yellow", 2, 3));
        list.add(new ListItem("LED White", 4, 5));
        list.add(new ListItem("LED Green", 6, 7));
        //till here

        all_on = (Button) findViewById(R.id.all_on);
        all_on. setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    sendData("9");
                } catch (IOException e){
                    Toast.makeText(MainActivity.this, "Can't Send Data. Connect Again", Toast.LENGTH_SHORT).show();
                    clear();
                } catch (NullPointerException e){
                    Toast.makeText(MainActivity.this, "Not Connected", Toast.LENGTH_SHORT).show();
                    clear();
                }
            }
        });

        all_off = (Button) findViewById(R.id.all_off);
        all_off.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    sendData("8");
                } catch (IOException e){
                    Toast.makeText(MainActivity.this, "Can't Send Data. Connect Again", Toast.LENGTH_SHORT).show();
                    clear();
                } catch (NullPointerException e){
                    Toast.makeText(MainActivity.this, "Not Connected", Toast.LENGTH_SHORT).show();
                    clear();
                }
            }
        });

        recyclerView.setAdapter(new ListAdapter(list));

        clear();

        connect = (Button)findViewById(R.id.connect);
        connect.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                clear();
                init();
                SelectDevice();
            }
        });

        status = (TextView)findViewById(R.id.status);
        if (connected_device == null) status.setText("Not Connected");
        else status.setText(connected_device.getName());

//        StatusCheckThread statusCheckThread = new StatusCheckThread();
//        statusCheckThread.run();

    }

    private class StatusCheckThread extends Thread{

        StatusCheckThread(){

        }

        @Override
        public void run() {
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        sendData("");
                    } catch (Exception e){
                        Toast.makeText(MainActivity.this, "Connection Lost", Toast.LENGTH_SHORT).show();
                        clear();
                        init();
                    }
                }
            }, 0, 500);
        }
    }

    public void clear(){
        connected_device = null;
        bluetoothAdapter = null;
        bluetoothOutStream = null;
        ((TextView) findViewById(R.id.status)).setText("Not Connected");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode){
            case 1:
                if (resultCode != RESULT_OK) {
                    Toast.makeText(this, "Unable to turn bluetooth on", Toast.LENGTH_SHORT).show();
                    init();
                }
                break;
        }
    }

    void init(){
        if (bluetoothAdapter == null)
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }
    }

    List<BluetoothDevice> getDevices(){
        List<BluetoothDevice> devices = new ArrayList<>();
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();

        Iterator<BluetoothDevice> iterator;
        Log.e("jatin", "Started");
        for (iterator = bondedDevices.iterator(); iterator.hasNext();){
            BluetoothDevice device = iterator.next();
            devices.add(device);
        }

        if (bondedDevices.size() <= 0){
            Toast.makeText(this, "No Appropriate paired devices", Toast.LENGTH_SHORT).show();
        }
        return devices;

    }

    void SelectDevice(){
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()){

            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Select the device");
            final List<BluetoothDevice> devices = getDevices();
            CharSequence[] devicesChars = new CharSequence[devices.size()];
            for (int i=0; i<devices.size(); i++){
                devicesChars[i] = devices.get(i).getName();
            }
            builder.setItems(devicesChars, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface,final int i) {
                    Timer timer = new Timer();
                    final int[] flag = {0};
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            try {
                                if (flag[0] != 1){
                                    flag[0] = 1;
                                    connected_device = devices.get(i);
                                    ParcelUuid[] uuids = connected_device.getUuids();
                                    BluetoothSocket socket = connected_device.createInsecureRfcommSocketToServiceRecord(uuids[0].getUuid());
                                    socket.connect();
                                    bluetoothOutStream = socket.getOutputStream();
                                    ((TextView) findViewById(R.id.status)).setText(connected_device.getName());
                                } else {
                                    this.cancel();
                                }
                            } catch (IOException e){
                                Toast.makeText(MainActivity.this, "Error Occured: " + e.toString(), Toast.LENGTH_SHORT).show();
                                this.cancel();
                            }
                        }
                    }, 0, 5000);

                }
            });
            AlertDialog alert = builder.create();
            alert.show();

        } else {
            Toast.makeText(this, "Unable to Connect !", Toast.LENGTH_SHORT).show();
        }
    }

    public void sendData(String s) throws IOException{
        bluetoothOutStream.write(s.getBytes());
    }

    public class ListItem {
        public String action;
        public int code1;
        public int code2;

        ListItem(String action, int code1, int code2){
            this.action = action;
            this.code1 = code1;
            this.code2 = code2;
        }

        ListItem(){}
    }

    public class ListAdapter extends RecyclerView.Adapter<ListAdapter.ViewHolder> {

        List<ListItem> list;

        public class ViewHolder extends RecyclerView.ViewHolder{

            public TextView title;
            public Button on, off;

            public ViewHolder(View itemView) {
                super(itemView);
                title = (TextView) itemView.findViewById(R.id.title);
                on = (Button) itemView.findViewById(R.id.on);
                off = (Button) itemView.findViewById(R.id.off);
            }
        }

        ListAdapter(List<ListItem> list){
            this.list = list;
        }

        @Override
        public ListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.list_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ListAdapter.ViewHolder holder, final int position) {
            holder.title.setText(list.get(position).action);
            holder.on.setOnClickListener(new View.OnClickListener(){

                @Override
                public void onClick(View view) {
                    try {
                        sendData("" + list.get(position).code1);
                    } catch (IOException e){
                        Toast.makeText(MainActivity.this, "Can't Send Data. Connect Again", Toast.LENGTH_SHORT).show();
                        clear();
                    } catch (NullPointerException e){
                        Toast.makeText(MainActivity.this, "Not Connected", Toast.LENGTH_SHORT).show();
                        clear();
                    }

                }
            });
            holder.off.setOnClickListener(new View.OnClickListener(){

                @Override
                public void onClick(View view) {
                    try {
                        sendData("" + list.get(position).code2);
                    } catch (IOException e){
                        Toast.makeText(MainActivity.this, "Can't Send Data. Connect Again", Toast.LENGTH_SHORT).show();
                        clear();
                    } catch (NullPointerException e){
                        Toast.makeText(MainActivity.this, "Not Connected", Toast.LENGTH_SHORT).show();
                        clear();
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }
    }
}
