package com.ldvhrtn.ndscontroller;

import android.inputmethodservice.InputMethodService;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Toast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;

public class NDSControllerService extends InputMethodService {
    int[] last_held_buttons = {0,0,0,0};

    // values in [0] correspond to libnds codes in order
    // TODO read these from saved settings
    int[][] events = {{96,97,109,108,22,21,19,20,103,102,99,100,188,189},
                        {190,191,192,193,194,195,196,197,198,199,200,201,202,203},
                        {29,30,31,32,33,34,35,36,37,38,39,40,41,42,43},
                        {44,45,46,47,48,49,50,51,52,53,54,55,56,57,58}};

    InputConnection ic;

    DatagramSocket m_sock;
    Rec_UDP_packets rec_task;

    class Rec_UDP_packets extends AsyncTask<Void, Integer, Void> {
        protected Void doInBackground(Void... v) {
            if(android.os.Debug.isDebuggerConnected()) android.os.Debug.waitForDebugger();
            try {
                m_sock = new DatagramSocket(3210);
                while (!isCancelled()) {
                    byte[] receivedata = new byte[4];
                    DatagramPacket recv_packet = new DatagramPacket(receivedata, receivedata.length);
                    Log.d("UDP", "Socket receiving");
                    m_sock.receive(recv_packet);
                    ByteBuffer wrapped_packet = ByteBuffer.wrap(recv_packet.getData());
                    int rec_msg = wrapped_packet.getInt();
                    Log.d(" Received int", Integer.toString(rec_msg));
                    publishProgress(rec_msg);
                    InetAddress ipaddress = recv_packet.getAddress();
                    int port = recv_packet.getPort();
                    Log.d("IPAddress : ", ipaddress.toString());
                    Log.d(" Port : ", Integer.toString(port));
                }
            } catch (SocketException se) {
                Log.d("UDP", "Socket closed");
            } catch (Exception e) {
                Log.e("UDP", "Ignored error", e);
            }
            return null;
        }

        int get_held_buttons(int msg) {
            return msg & 0x3FFF;
        }

        int get_current_player(int msg) {
            return (msg & 0xC000) >> 14;
        }

        //TODO perhaps simulate touch events using these
        int get_x_coord(int msg) {
            return (msg & 0xFF00000) >> 24;
        }
        int get_y_coord(int msg) {
            return (msg & 0xFF000) >> 16;
        }

        void send_new_events(int new_buttons, int released_buttons, int player) {
            for(int i=0; i<14; i++) {
                if((new_buttons & (1<<i)) != 0) {
                    ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, events[player][i]));
                }
                if((released_buttons & (1<<i)) != 0) {
                    ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, events[player][i]));
                }
            }
        }

        protected void onProgressUpdate(Integer... msg) {
            Toast.makeText(getApplicationContext(), msg[0].toString(), Toast.LENGTH_SHORT).show();
            int current_held_buttons = get_held_buttons(msg[0]);
            int current_player = get_current_player(msg[0]);
            int new_buttons = current_held_buttons & ~last_held_buttons[current_player];
            int released_buttons = last_held_buttons[current_player] & ~current_held_buttons;
            last_held_buttons[current_player] = current_held_buttons;

            send_new_events(new_buttons, released_buttons, current_player);
            return;
        }

        protected void onPostExecute(Void msg) {
            Log.d("post_execute", "hit");
        }
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        Toast.makeText(getApplicationContext(), "Starting UDP input", Toast.LENGTH_SHORT).show();
        ic = getCurrentInputConnection();
        rec_task = new Rec_UDP_packets();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            rec_task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            rec_task.execute();
        }
    }

    public NDSControllerService() {
    }

    @Override
    public void onFinishInput() {
        super.onFinishInput();
        Toast.makeText(getApplicationContext(), "Stopping UDP input", Toast.LENGTH_SHORT).show();
        rec_task.cancel(true);
        if (m_sock != null) m_sock.close();
    }
}
