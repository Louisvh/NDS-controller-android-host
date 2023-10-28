package com.ldvhrtn.ndscontroller;

import android.inputmethodservice.InputMethodService;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.TextView;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NDSControllerService extends InputMethodService {
    int[] last_held_buttons = {0,0,0,0};
    int[] player_ips = {0,0,0,0};

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

    class Rec_UDP_packets {
        private final ExecutorService executor;
        boolean cancelled = false;
        private boolean isCancelled(){
            return this.cancelled;
        }
        public void cancel(){
            this.cancelled = true;
        }

        int[] known_player_ips;
        public Rec_UDP_packets(int[] saved_player_ips) {
            this.known_player_ips = saved_player_ips;
            this.executor = Executors.newSingleThreadExecutor();
        }

        protected Void doInBackground() {
            if(android.os.Debug.isDebuggerConnected()) android.os.Debug.waitForDebugger();
            while(!isCancelled()) {
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
                        String source_ip = recv_packet.getAddress().toString();
                        int ip_int = Integer.parseInt(source_ip.replace(".","").replace("/",""));
                        button_data = byteArrayToInt(receivedata, 0);
                        meta_data = byteArrayToInt(receivedata, 4);
                        Log.d(" Received int", Integer.toString(button_data));
                        publishProgress(button_data, meta_data, ip_int);
                    }
                } catch (SocketException se) {
                    Log.d("UDP", "Socket error", se);
                } catch (Exception e) {
                    Log.e("UDP", "Ignored error", e);
                }
                return null;
            }
            return null;
        }

        public void execute() {
            Log.d("NDSControllerService", "executing service loop");
            this.executor.execute(() -> this.doInBackground());
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

        private void publishProgress(Integer... msg){
            int current_held_buttons = msg[0];
            int current_player = msg[1]%4;
            int current_ip = msg[2];
            if (current_player == 0) {
                for (int i=0; i<4; i++){
                    if (this.known_player_ips[i] == 0){
                        this.known_player_ips[i] = current_ip;
                        current_player = i;
                        break;
                    }
                }
            }
            int new_buttons = current_held_buttons & ~last_held_buttons[current_player];
            int released_buttons = last_held_buttons[current_player] & ~current_held_buttons;
            last_held_buttons[current_player] = current_held_buttons;

            send_new_events(new_buttons, released_buttons, current_player);
        }
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        Log.d("NDSControllerService", "onStartInput hit");
        ic = getCurrentInputConnection();
        rec_task = new Rec_UDP_packets(player_ips);
        rec_task.execute();
    }

    public NDSControllerService() {
    }

    @Override
    public void onFinishInput() {
        super.onFinishInput();
        Log.d("NDSControllerService", "onFinishInput hit");
        rec_task.cancel();
        if (m_sock != null) m_sock.close();
    }
}
