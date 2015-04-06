package edu.gatech.cc.cs7470.facecard;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.glass.app.Card;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created by miseonpark on 3/31/15.
 */
public class FaceCardMainActivity extends Activity {

    String tag = "Glass Main Activity";

    private List<Card> mCards;
    private CardScrollView mCardScrollView;
    GestureDetector mGestureDetector;

    public static String msgToSend;
    public static final int STATE_CONNECTION_STARTED = 0;
    public static final int STATE_CONNECTION_LOST = 1;
    public static final int READY_TO_CONN = 2;

    ConnectedThread mConnectedThread;
    BluetoothAdapter mBluetoothAdapter;
    public String TAG = "log";
    public String NAME =" BLE";
    Handler handle;

    ArrayList<BluetoothSocket> mSockets = new ArrayList<BluetoothSocket>();
    // list of addresses for devices we've connected to
    ArrayList<String> mDeviceAddresses = new ArrayList<String>();

    // We can handle up to 7 connections... or something...
    UUID[] uuids = new UUID[2];
    // some uuid's we like to use..
    String uuid1 = "00001101-0000-1000-8000-00805F9B34FB";
    String uuid2 = "c2911cd0-5c3c-11e3-949a-0800200c9a66";

    int REQUEST_ENABLE_BT = 1;
    AcceptThread accThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.face_card_main);

        //initialize an array list of card object, which works the same as the list view..
        mCards = new ArrayList<Card>();
        mGestureDetector = this.createGestureDetector(this);

        uuids[0] = UUID.fromString(uuid1);
        uuids[1] = UUID.fromString(uuid2);

        handle = new Handler(Looper.getMainLooper()) {

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    // if connection is built.....
                    case STATE_CONNECTION_STARTED:
                        setContentView(R.layout.face_card_main);
                        Toast.makeText(getApplicationContext(), "bluetooth connection started", Toast.LENGTH_LONG).show();
                        break;
                    case STATE_CONNECTION_LOST:
                        Toast.makeText(getApplicationContext(), "bluetooth connection lost", Toast.LENGTH_LONG).show();
                        // if the connection is broken, listening the device again
                        startListening();
                        break;
                    case READY_TO_CONN:
                        // if the connection is ready to go, start listening the
                        // device
                        Toast.makeText(getApplicationContext(), "bluetooth ready to connect", Toast.LENGTH_LONG).show();
                        startListening();
                        break;
                    default:
                        break;
                }
            }

        };
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        Card card = new Card(getApplicationContext());
        card.setText("waiting for bluetooth connection");
        //add the card to the array list
        mCards.add(card);
        setupScrollView();

        // run the "go get em" thread..
        accThread = new AcceptThread();
        accThread.start();

//        initBluetooth();
//        //check the bluetooth
//        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//        if (mBluetoothAdapter == null) {
//            // Device does not support Bluetooth
//            Toast toast = Toast.makeText(context, "Bluetooth disabled", Toast.LENGTH_LONG);
//            toast.show();
//            finish();
//        }
//        else
//        {
//            if (!mBluetoothAdapter.isEnabled()) {
//
//                Toast toast = Toast.makeText(context, "please turn on your Bluetooth ", Toast.LENGTH_LONG);
//                toast.show();
//                //push to the setting view to enable the bluetooth
//                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//                startActivityForResult(enableBtIntent, 1);
//            }
//            Set<BluetoothDevice> devicesArray = mBluetoothAdapter.getBondedDevices();
//            // If there are devices
//            if (devicesArray != null && devicesArray.size() > 0) {
//                // Loop through paired devices
//                Toast toast = Toast.makeText(context, "Bluetooth founded!", Toast.LENGTH_LONG);
//                toast.show();
//                for (BluetoothDevice device : devicesArray) {
//                    //add the name of each device to each card
//                    Card card = new Card(this);
//                    card.setText(device.getName());
//                    //add the card to the array list
//                    mCards.add(card);
//                    devices.add(device);
//                }
//                setupScrollView();
//            }
//        }
    }

    public void startListening() {
        if (accThread != null) {
            accThread.cancel();
        } else if (mConnectedThread != null) {
            mConnectedThread.cancel();
        } else {
            accThread = new AcceptThread();
            accThread.start();
        }
    }

    public static class HostBroadRec extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle b = intent.getExtras();
            String vals = "";
            for (String key : b.keySet()) {
                vals += key + "&" + b.getString(key) + "Z";
            }
            FaceCardMainActivity.setMsg(vals);
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {

            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[4];
            int bytes;
        }

        public void connectionLost() {
            Message msg = handle.obtainMessage(STATE_CONNECTION_LOST);
            handle.sendMessage(msg);
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer
         *            The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                // Toast.makeText(getApplicationContext(), "write buffer!",
                // 1).show();
                mmOutStream.write(buffer);
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
                connectionLost();
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
                Message msg = handle.obtainMessage(READY_TO_CONN);
                handle.sendMessage(msg);
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    public static synchronized void setMsg(String newMsg) {
        msgToSend = newMsg;
    }

    private class AcceptThread extends Thread {
        private BluetoothServerSocket mmServerSocket;
        BluetoothServerSocket tmp;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, uuids[0]);
            } catch (IOException e) {
            }
            mmServerSocket = tmp;
        }

        public void run() {
            Log.e(TAG, "Running");
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned
            while (true) {

                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
                // If a connection was accepted

                if (socket != null) {
                    // if the connection has been built, then close the server
                    // socket..
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    // Do work to manage the connection (in a separate thread)
                    manageConnectedSocket(socket);
                    break;
                }
            }
        }

        /** Will cancel the listening socket, and cause the thread to finish */
        public void cancel() {
            try {
                mmServerSocket.close();
                Message msg = handle.obtainMessage(READY_TO_CONN);
                handle.sendMessage(msg);

            } catch (IOException e) {
            }
        }
    }

    private void manageConnectedSocket(BluetoothSocket socket) {
        // start our connection thread
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();
        // Send the name of the connected device back to the UI Activity
        // so the HH can show you it's working and stuff...
        String devs = "";
        for (BluetoothSocket sock : mSockets) {
            devs += sock.getRemoteDevice().getName() + "\n";
        }
        // pass it to the pool....
        Message msg = handle.obtainMessage(STATE_CONNECTION_STARTED);
        Bundle bundle = new Bundle();
        bundle.putString("NAMES", devs);
        msg.setData(bundle);
        handle.sendMessage(msg);
    }

//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        unregisterReceiver(mReceiver);
//    }

    private void setupScrollView(){
        mCardScrollView = new CardScrollView(this){
            @Override
            public final boolean dispatchGenericFocusedEvent(MotionEvent event) {
                if (mGestureDetector.onMotionEvent(event)) {
                    return true;
                }
                return super.dispatchGenericFocusedEvent(event);
            }
        };

        ExampleCardScrollAdapter adapter = new ExampleCardScrollAdapter();
        mCardScrollView.setAdapter(adapter);
        mCardScrollView.activate();
        setContentView(mCardScrollView);

//        mBluetoothAdapter.cancelDiscovery();
//        mBluetoothAdapter.startDiscovery();
    }

    private GestureDetector createGestureDetector(final Context context) {

        GestureDetector mGestureDetector = new GestureDetector(context);
        mGestureDetector.setBaseListener(new GestureDetector.BaseListener() {

            @Override
            public boolean onGesture(Gesture gesture) {
                if (gesture == Gesture.TAP) {
                    // do something on tap
                    int index = mCardScrollView.getSelectedItemPosition();
                    Log.d(tag, Integer.toString(index));

//                    if(mBluetoothAdapter.isDiscovering()){
//                        mBluetoothAdapter.cancelDiscovery();
//                    }
//                    BluetoothDevice selectedDevice = devices.get(index);
//                    ConnectThread connect = new ConnectThread(selectedDevice);
//                    connect.start();
                    Log.i(tag, "clicked");
                    return true;
                }
                return false;
            }
        });
        return mGestureDetector;
    }

    private class ExampleCardScrollAdapter extends CardScrollAdapter {
        //the same as implementing a list view

        public int findIdPosition(Object id) {
            return -1;
        }

        public int findItemPosition(Object item) {
            return mCards.indexOf(item);
        }

        @Override
        public int getCount() {
            return mCards.size();
        }

        @Override
        public Object getItem(int position) {
            return mCards.get(position);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return mCards.get(position).getView();
        }

        @Override
        public int getPosition(Object arg0) {
            // TODO Auto-generated method stub
            return 0;
        }
    }
}