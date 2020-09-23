package fructuoso.polygate;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import junit.framework.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    ImageButton btnWalker, btnCar, btnLight, btnBell, btnClose;
    Button btnDis;
    String address = null;
    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    InputStream mmInputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    boolean stopWorker;
    //SPP UUID. Look for it
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Intent newint = getIntent();
        address = newint.getStringExtra(DeviceList.EXTRA_ADDRESS); //receive the address of the bluetooth device

        //view of the ledControl
        setContentView(R.layout.activity_main);

        //call the widgtes
        btnWalker = (ImageButton)findViewById(R.id.ButtonWalker);
        btnCar = (ImageButton)findViewById(R.id.ButtonCar);
        btnLight = (ImageButton)findViewById(R.id.ButtonGyro);
        btnBell = (ImageButton)findViewById(R.id.ButtonAlarm);
        btnClose = (ImageButton)findViewById(R.id.ButtonClose);
        btnDis = (Button)findViewById(R.id.btn_disconnect);

        new ConnectBT().execute(); //Call the class to connect

        //commands to be sent to bluetooth
        btnWalker.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                OpenSmall();
            }
        });

        btnCar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                OpenFull();
            }
        });

        btnLight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                LightGyro();
            }
        });

        btnBell.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                RingBell();
            }
        });

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                CloseFull();
            }
        });

        btnDis.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Disconnect(); //close connection
            }
        });
    }

    private void Disconnect()
    {
        if (btSocket!=null) //If the btSocket is busy
        {
            try
            {
                btSocket.close(); //close connection
            }
            catch (IOException e)
            { msg("Error");}
        }
        finish(); //return to the first layout

    }

    private void OpenFull()
    {
        if (btSocket!=null)
        {
            try
            {
                btSocket.getOutputStream().write("C".toString().getBytes());
                mmInputStream = btSocket.getInputStream();
                beginListenForData();
                msg("Opening Gate full");
            }
            catch (IOException e)
            {
                msg("Error");
            }
        }
    }

    private void OpenSmall()
    {
        if (btSocket!=null)
        {
            try
            {
                btSocket.getOutputStream().write("W".toString().getBytes());
                mmInputStream = btSocket.getInputStream();
                beginListenForData();
                msg("Open Gate half");
            }
            catch (IOException e)
            {
                msg("Error");
            }
        }
    }

    private void LightGyro()
    {
        if (btSocket!=null)
        {
            try
            {
                btSocket.getOutputStream().write("L".getBytes());
                mmInputStream = btSocket.getInputStream();
                beginListenForData();
                msg("Light LED");
            }
            catch (IOException e)
            {
                msg("Error");
            }
        }
    }

    private void RingBell()
    {
        if (btSocket!=null)
        {
            try
            {
                btSocket.getOutputStream().write("B".toString().getBytes());
                mmInputStream = btSocket.getInputStream();
                beginListenForData();
                msg("Ring Buzzer");
            }
            catch (IOException e)
            {
                msg("Error");
            }
        }
    }

    private void CloseFull()
    {
        if (btSocket!=null)
        {
            try
            {
                btSocket.getOutputStream().write("X".toString().getBytes());
                mmInputStream = btSocket.getInputStream();
                beginListenForData();
                msg("Close");
            }
            catch (IOException e)
            {
                msg("Error");
            }
        }
    }

    // fast way to call Toast
    private void msg(String s)
    {
        Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_led_control, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute()
        {
            progress = ProgressDialog.show(MainActivity.this, "Connecting...", "Please wait!!!");  //show a progress dialog
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {
            try
            {
                if (btSocket == null || !isBtConnected)
                {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);//connects to the device's address and checks if it's available
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();//start connection
                }
            }
            catch (IOException e)
            {
                ConnectSuccess = false;//if the try failed, you can check the exception here
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            if (!ConnectSuccess)
            {
                msg("Connection Failed. Is it a SPP Bluetooth? Try again.");
                finish();
            }
            else
            {
                msg("Connected.");
                isBtConnected = true;
            }
            progress.dismiss();
        }
    }

    void beginListenForData()
    {
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try
                    {
                        int bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }
}

