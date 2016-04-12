package com.ldvhrtn.ndscontroller;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import android.util.Log;

public class FrontendMain extends Activity {

    DatagramSocket m_sock;
    Rec_UDP_packets rec_task;

    int last_held_buttons = 0;
    //correspond to libnds codes in order
    int[] events = {96,97,109,108,22,21,19,20,102,103,99,100,188,189};

    class Rec_UDP_packets extends AsyncTask<Void, String, Void> {
        private Exception exception;
        protected Void doInBackground(Void... v) {
            try {
                m_sock=new DatagramSocket(3210);
                while(!isCancelled())
                {
                    byte[] receivedata = new byte[1024];
                    DatagramPacket recv_packet = new DatagramPacket(receivedata, receivedata.length);
                    Log.d("UDP", "S: Receiving...");
                    m_sock.receive(recv_packet);
                    String rec_msg = new String(recv_packet.getData());
                    Log.d(" Received String ",rec_msg);
                    publishProgress(rec_msg);
                    InetAddress ipaddress = recv_packet.getAddress();
                    int port = recv_packet.getPort();
                    Log.d("IPAddress : ",ipaddress.toString());
                    Log.d(" Port : ",Integer.toString(port));
                }
            } catch (Exception e) {
                Log.e("UDP", "S: Error", e);
            }
            return null;
        }

        //TODO replace pc receive stuff with protocol sync'd with nds program
        int down_events(String msg) {
            int current_held_buttons = 0;
            String buttons = "lknmdawsoqijcv";
            for(int i=0; i<14; i++) {
                if(msg.indexOf(buttons.charAt(i)) != -1) {
                    current_held_buttons += (1 << i);
                }
            }
            return current_held_buttons;
        }

        void send_new_events(int new_buttons, int released_buttons) {
            for(int i=0; i<14; i++) {
                if((new_buttons & (1<<i)) > 0) {
                    //ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, events[i]));
                }
            }

            for(int i=0; i<14; i++) {
                if((released_buttons & (1<<i)) > 0) {
                    //ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, events[i]));
                }
            }
        }

        protected void onProgressUpdate(String... msg) {
            int current_held_buttons = down_events(msg[0]);
            int new_buttons = current_held_buttons & ~last_held_buttons;
            int released_buttons = last_held_buttons & ~current_held_buttons;

            send_new_events(new_buttons, released_buttons);

            TextView m_textview = (TextView) findViewById(R.id.hello_textview);
            m_textview.setText(msg[0]);
            return;
        }

        protected void onPostExecute(String msg) {
            Log.e("post_execute", "hit");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setContentView(R.layout.activity_frontend_main);
        TextView m_textview;
        m_textview = (TextView) findViewById(R.id.hello_textview);

        WifiManager wifiMgr = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        int m_ip = wifiInfo.getIpAddress();
        String m_ip_s = Formatter.formatIpAddress(m_ip);

        m_textview.setText(m_ip_s);

        rec_task = new Rec_UDP_packets();
        rec_task.execute();
    }

    @Override
    protected void onPause() {
        super.onPause();
        rec_task.cancel(true);
        m_sock.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_frontend_main, menu);
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
}
