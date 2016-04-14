package com.ldvhrtn.ndscontroller;

import android.app.Service;
import android.content.Intent;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.TextView;
import android.widget.Toast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class NDSControllerService extends InputMethodService {
    int prev_packet_num = 0;
    int last_held_buttons = 0;

    //correspond to libnds codes in order
    int[] events = {96,97,109,108,22,21,19,20,103,102,99,100,188,189};

    InputConnection ic;

    DatagramSocket m_sock;
    Rec_UDP_packets rec_task;

    class Rec_UDP_packets extends AsyncTask<Void, String, Void> {
        private Exception exception;
        protected Void doInBackground(Void... v) {
            try {
                m_sock = new DatagramSocket(3210);
                while (!isCancelled()) {
                    byte[] receivedata = new byte[8];
                    DatagramPacket recv_packet = new DatagramPacket(receivedata, receivedata.length);
                    Log.d("UDP", "S: Receiving...");
                    m_sock.receive(recv_packet);
                    String rec_msg = new String(recv_packet.getData());
                    Log.d(" Received String ", rec_msg);
                    publishProgress(rec_msg);
                    InetAddress ipaddress = recv_packet.getAddress();
                    int port = recv_packet.getPort();
                    Log.d("IPAddress : ", ipaddress.toString());
                    Log.d(" Port : ", Integer.toString(port));
                }
            } catch (SocketException se) {
                Log.d("UDP", "Socket closed");
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
                    ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, events[i]));
                }
            }

            for(int i=0; i<14; i++) {
                if((released_buttons & (1<<i)) > 0) {
                    ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, events[i]));
                }
            }
        }

        protected void onProgressUpdate(String... msg) {
            int current_held_buttons = down_events(msg[0]);
            int new_buttons = current_held_buttons & ~last_held_buttons;
            int released_buttons = last_held_buttons & ~current_held_buttons;
            last_held_buttons = current_held_buttons;

            send_new_events(new_buttons, released_buttons);

            ic.commitText(msg[0], msg[0].length());
            return;
        }

        protected void onPostExecute(String msg) {
            Log.d("post_execute", "hit");
        }
    }

    public void onNDSPacket(int primaryCode, int[] keyCodes) {
        ic = getCurrentInputConnection();
        switch(primaryCode){
            case Keyboard.KEYCODE_DONE:
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                break;
            default:
                char code = (char)primaryCode;
                if(Character.isLetter(code)){
                    code = Character.toLowerCase(code);
                }
                ic.commitText(String.valueOf(code),1);
        }
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        Toast.makeText(getApplicationContext(), "Starting UDP input", Toast.LENGTH_SHORT).show();
        ic = getCurrentInputConnection();
        rec_task = new Rec_UDP_packets();
        rec_task.execute();
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
