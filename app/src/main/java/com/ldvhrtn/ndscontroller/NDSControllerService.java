package com.ldvhrtn.ndscontroller;

import android.inputmethodservice.InputMethodService;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

public class NDSControllerService extends InputMethodService {
    int[] last_held_buttons = {0,0,0,0};

    // values in [0] correspond to libnds codes in order
    // TODO read these from saved settings
    //                  A,   B,sel,str,  r,  l,  u,  d,  R,  L,  X,  Y, ot.  -, ZL, ZR,  -,  -,  -,  -, 3t,  -,  -,  -, cr, cl, cu, cd,kcr,kcl,kcu,kcd
    int[][] events = {{ 96, 97,109,108, 22, 21, 19, 20,103,102, 99,100, 66,  0,104,105,  0,  0,  0,  0, 66,  0,  0,  0,188,189,190,191, 22, 21, 19, 20},
                      {192,193,194,195,196,197,198,199,200,201,202,203,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0},
                      { 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0},
                      { 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0}};

    InputConnection ic;

    DatagramSocket m_sock;
    Rec_UDP_packets rec_task;

    class Rec_UDP_packets extends AsyncTask<Void, Integer, Void> {
        protected Void doInBackground(Void... v) {
            if(android.os.Debug.isDebuggerConnected()) android.os.Debug.waitForDebugger();
            try {
                m_sock = new DatagramSocket(null);
                m_sock.setReuseAddress(true);
                m_sock.bind(new InetSocketAddress(3210));

                while (!isCancelled()) {
                    int button_data, meta_data, port;
                    byte[] receivedata = new byte[8];
                    Log.d("UDP", "Socket receiving");
                    DatagramPacket recv_packet = new DatagramPacket(receivedata, receivedata.length);
                    m_sock.receive(recv_packet);

                    button_data = byteArrayToInt(receivedata, 0);
                    meta_data = byteArrayToInt(receivedata, 4);
                    Log.d(" Received int", Integer.toString(button_data));
                    publishProgress(button_data, meta_data);
                }
            } catch (SocketException se) {
                Log.d("UDP", "Socket error", se);
            } catch (Exception e) {
                Log.e("UDP", "Ignored error", e);
            }
            return null;
        }

        int byteArrayToInt(byte[] b, int offset) {
            return  b[3+offset] & 0xFF |
                    (b[2+offset] & 0xFF) << 8 |
                    (b[1+offset] & 0xFF) << 16 |
                    (b[0+offset] & 0xFF) << 24;
        }

        void send_new_events(int new_buttons, int released_buttons, int player) {
            for(int i=0; i<32; i++) {
                if((new_buttons & (1<<i)) != 0) {
                    ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, events[player][i]));
                }
                if((released_buttons & (1<<i)) != 0) {
                    ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, events[player][i]));
                }
            }
        }

        protected void onProgressUpdate(Integer... msg) {
            int current_held_buttons = msg[0];
            int current_player = msg[1]%4;
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
        rec_task.cancel(true);
        if (m_sock != null) m_sock.close();
    }
}
