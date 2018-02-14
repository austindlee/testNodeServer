package com.example.austindlee.communicatewithnodejsserver;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.os.AsyncTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.io.InputStream;
import java.net.Socket;
import java.io.OutputStream;


public class MainActivity extends Activity {
    Button btnStart, btnSend;
    TextView textStatus;
    NetworkTask networkTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnStart = (Button)findViewById(R.id.connectButton);
        btnSend = (Button) findViewById(R.id.sendButton);
        textStatus = (TextView) findViewById(R.id.responseTextView);
        btnStart.setOnClickListener(btnStartListener);
        btnSend.setOnClickListener(btnSendListener);
        networkTask = new NetworkTask();
    }

    private OnClickListener btnStartListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            networkTask = new NetworkTask();
            networkTask.execute();
        }
    };

    private OnClickListener btnSendListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            textStatus.setText("Sending Message to AsyncTask");
            networkTask.SendDataToNetwork("test");
        }
    };
    @Override
    protected void onDestroy() {
        super.onDestroy();
        networkTask.cancel(true); //In case the task is currently running
    }

    public class NetworkTask extends AsyncTask<Void, byte[], Boolean> {
        Socket nsocket;
        InputStream nis;
        OutputStream nos;

        @Override
        protected void onPreExecute() {
            Log.d("AsyncTask", "OnPreExecute");
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            boolean result = false;
            try {
                Log.d("AsyncTask", "doInBackground");
                SocketAddress sockaddr = new InetSocketAddress("164.67.40.197", 8080);
                nsocket = new Socket();
                nsocket.connect(sockaddr, 5000);
                if (nsocket.isConnected()) {
                    nis = nsocket.getInputStream();
                    nos = nsocket.getOutputStream();
                    Log.i("AsyncTask", "doInBackground: Socket created, streams assigned");
                    Log.i("AsyncTask", "doInBackground: Waiting for inital data...");
                    ByteArrayOutputStream rst = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    int length;
                    int read = nis.read(buffer, 0, 4096); //This is blocking
                    while (read != -1) {
                        byte[] tempdata = new byte[read];
                        System.arraycopy(buffer, 0, tempdata, 0, read);
                        publishProgress(tempdata);
                        Log.i("AsyncTask", "doInBackground: Got some data");
                        read = nis.read(buffer, 0, 4096); //This is blocking
                        length = nis.read(buffer);
                        rst.write(buffer, 0, length);
                    }
                    Log.i("RESULT", "RESuLT " + rst.toString("UTF-8"));
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.i("AsyncTask", "doInBackground: IOException");
                result = true;
            } catch (Exception e) {
                e.printStackTrace();
                Log.i("AsyncTask", "doInBackground: Exception");
                result = true;
            } finally {
                try {
                    nis.close();
                    nos.close();
                    nsocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Log.i("AsyncTask", "doInBackground: Finished");
            }
            return result;
        }

        public void SendDataToNetwork(final String cmd) {
            if (nsocket.isConnected()) {
                Log.i("AsyncTask", "SendDataToNetwork: Writing received message to socket");
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            nos.write(cmd.getBytes());
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.i("AsyncTask", "SendDataToNetwork: Message send failed. Caught an exception" + e.getClass().getName());
                        }
                    }
                }).start();
                return;
            }
            Log.i("TAg", "SendDataToNetwork: Cannot send message. Socket is closed");
            return;
        }

        @Override
        protected void onProgressUpdate(byte[]... values) {
            if (values.length > 0) {
                Log.i("AsyncTask", "onProgressUpdate: " + values[0].length + " bytes received.");
                textStatus.setText(new String(values[0]));
            }
        }

        @Override
        protected void onCancelled() {
            Log.i("AsyncTask", "Cancelled.");
            btnStart.setVisibility(View.VISIBLE);
        }

        protected void onPostExecute(String result) {
            if (result == "") {
                Log.i("AsyncTask", "onPostExecute: Completed with an Error.");
                textStatus.setText("There was a connection error.");
            } else {
                textStatus.setText(result);
                Log.i("AsyncTask", "onPostExecute: Completed.");
            }
            btnStart.setVisibility(View.VISIBLE);
        }
    }
}


