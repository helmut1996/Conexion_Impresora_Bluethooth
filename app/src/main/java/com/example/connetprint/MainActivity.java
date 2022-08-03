package com.example.connetprint;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

//Import librery
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ThemedSpinnerAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.Set;
import java.util.UUID;
import android.os.Handler;

public class MainActivity extends AppCompatActivity {

    Button btnConnect, btnDisconnect, btnPrint;
    TextView lbnPrinterName;
    EditText teztBox;


    //variables para coneccion
    BluetoothAdapter bluoothAdapter;
    BluetoothSocket bluoothSocket;
    BluetoothDevice bluoothDevice;
    InputStream inputStream;
    OutputStream outputStream;
    Thread thread;

    byte[] readBuffer;
    int readBufferPosition;
    volatile boolean stopMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnConnect = findViewById(R.id.btnConnect);
        btnDisconnect = findViewById(R.id.btnDisconnwct);
        btnPrint = findViewById(R.id.btnPrintDocument);

        teztBox = findViewById(R.id.txtText);
        lbnPrinterName = findViewById(R.id.lblPrinterName);


        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {

                    funBluoothDevicwe();
                    openBluethoothPrinter();
                }catch (Exception ex){
                    ex.printStackTrace();
                }
            }
        });


        btnDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    DisconnectPrinter();
                }catch (Exception ex){
                    ex.printStackTrace();
                }
            }
        });


        btnPrint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    printData();
                }catch (Exception ex){
                    ex.printStackTrace();
                }
            }
        });

    }

    public void funBluoothDevicwe() {
        try {
            bluoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluoothAdapter == null) {
                lbnPrinterName.setText("No Bluetooth Adapter found");
            }

            if (bluoothAdapter.isEnabled()) {
                Intent enableBI = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBI, 0);
            }

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            Set<BluetoothDevice> pairedDevice = bluoothAdapter.getBondedDevices();

            if (pairedDevice.size() > 0) {
                for (BluetoothDevice pairedDev : pairedDevice) {

                    // Aqui se agrega el nombre de la impresora por blouthooth
                    if (pairedDev.getName().equals("RP4")) {
                        bluoothDevice = pairedDev;
                        lbnPrinterName.setText("Bluetooth Printer : " + pairedDev.getName());
                        break;
                    }
                }
            }

            lbnPrinterName.setText("bLUETHOOTH pRINTER aTTACHED");

        } catch (Exception ex) {
            ex.printStackTrace();
        }


    }

    // Open Bluethooth

    public void openBluethoothPrinter() {
        try {
            // 00001101-0000-1000-8000-00805F9B34FB
            //fa87c0d0-afac-11de-8a39-0800200c9a66
            UUID uuiString = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            bluoothSocket = bluoothDevice.createRfcommSocketToServiceRecord(uuiString);
            bluoothSocket.connect();

            outputStream = bluoothSocket.getOutputStream();
            inputStream = bluoothSocket.getInputStream();

            beginListData();

        }catch (Exception ex){

        }
    }

    private void beginListData() {

        try {
            final Handler handler = new Handler();
            byte delimiter =10;
            stopMarker = false;
            readBufferPosition = 0;
            readBuffer = new byte[1024];

            thread = new Thread(new Runnable() {
                @Override
                public void run() {

                    while (!Thread.currentThread().isInterrupted() && !stopMarker){

                        try {

                            int byteAvailable = inputStream.available();
                            if (byteAvailable>0){
                                byte[] packetByte = new byte[readBufferPosition];
                                inputStream.read(packetByte);

                                for (int i = 0 ; i<byteAvailable; i++){

                                    byte b = packetByte[i];
                                    if (b==delimiter){
                                        byte [] encodedByte = new byte [readBufferPosition];
                                        System.arraycopy(
                                                readBuffer,0,
                                                encodedByte,0,
                                                encodedByte.length
                                        );
                                        final String data = new String(encodedByte, "US-ASCII");
                                        readBufferPosition = 0;
                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                lbnPrinterName.setText(data);
                                            }
                                        });

                                     }else {
                                        readBuffer[readBufferPosition++]=b;
                                    }


                                }

                            }

                        }catch (Exception ex){
                            stopMarker = true;
                        }
                    }


                }
            });

                     thread.start();

        }catch (Exception ex){

        }
    }


    // Printing Text toBluethooth Printer //
    private void printData() throws IOException{

        try {
            String msg = teztBox.getText().toString();
            msg = "\n";
            outputStream.write(msg.getBytes());
            lbnPrinterName.setText("Printing Text");
        }catch (Exception ex){
                ex.printStackTrace();
        }
    }


    // Disconwct Printer //
    private void DisconnectPrinter() throws IOException{
        try {
            stopMarker = true;
            outputStream.close();
            inputStream.close();
            bluoothSocket.close();
            lbnPrinterName.setText("Printer Disconnected.");

        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

}