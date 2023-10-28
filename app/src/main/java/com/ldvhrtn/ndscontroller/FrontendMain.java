package com.ldvhrtn.ndscontroller;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Html;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class FrontendMain extends Activity {

    static final String con_ok_string = "<font color='#006600'><b>IP Address:</b> </font>";
    static final String con_not_ok_string = "<b>IP Address:</b> ";
    CheckForNetworkUpdates netcheck_task;

    class CheckForNetworkUpdates{
        private final ExecutorService executor;
        private Handler handler;
        boolean cancelled = false;

        private boolean isCancelled(){
            return this.cancelled;
        }
        public void cancel(){
            this.cancelled = true;
        }

        public CheckForNetworkUpdates() {
            this.executor = Executors.newSingleThreadExecutor();
            this.handler = new Handler(Looper.getMainLooper());
        }
        private void publishProgress(String msg){
            this.handler.post(() -> {
                TextView m_textview = (TextView) findViewById(R.id.netinfo_tv);
                m_textview.setText(Html.fromHtml(msg));
                update_button_state();
            });
        }
        public void execute() {
            Log.d("frontmain", "executing checkfornetworkupdates");
            this.executor.execute(() -> this.doInBackground());
        }
        private Void doInBackground() {
            while(!isCancelled()) {
                try {
                    ConnectionStateManager tetherMan = new ConnectionStateManager(getBaseContext());
                    String ip = tetherMan.get_ip_address();
                    if (tetherMan.get_tether_state() || tetherMan.wifi_is_connected()) {
                        if (tetherMan.connection_nds_compatible()) {
                            publishProgress(con_ok_string + ip);
                        } else {
                            publishProgress(con_not_ok_string + ip);
                        }
                    } else {
                        publishProgress("0.0.0.0" + ", <font color='#EE0000'>no connection.</font>");
                    }
                    Thread.sleep(1000);
                    // Log.d("frontmain", "networkupdate hit");
                } catch (InterruptedException ei) {
                    Log.d("frontmain", "networkupdates check interrupted");
                } catch (Throwable e){
                    Log.e("connectionmanager", "throwable in tetherMan creation", e);
                }
            }
            return null;
        }
    }

    // Prompt the user to allow the keyboard to be selected
    public void input_enabler(View v) {
        new AlertDialog.Builder(v.getContext())
                .setTitle("Opening Language & Input Dialog")
                .setMessage("Android will give a warning that enabling an additional input method" +
                        " could be used to steal your data. This app does not log *any* data; " +
                        "it's open source, so you can check for yourself if you want!")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        startActivityForResult(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK), 2000);
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    // Prompt the user to select the keyboard
    public void input_selector(View v) {
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).showInputMethodPicker();
        update_button_state();
    }

    public void update_button_state() {
        Button inputenable_button = (Button) findViewById(R.id.inputenablebutton);
        Button inputselect_button = (Button) findViewById(R.id.inputselectbutton);
        Context ctx = inputenable_button.getContext();

        String id = Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
        ComponentName default_input_method = ComponentName.unflattenFromString(id);
        ComponentName m_service_name = new ComponentName(ctx, NDSControllerService.class);

        if(m_service_name.equals(default_input_method)) {
            String selected = "NDS Input Method is <font color='#00ee00'>Selected</font>";
            inputselect_button.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.checkbox_on_background, 0);
            inputenable_button.setEnabled(false);
            inputselect_button.setText(Html.fromHtml(selected));
        } else {
            String selected = "NDS Input Method is <font color='#ee0000'>Not Selected</font>";
            inputselect_button.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.checkbox_off_background, 0);
            inputenable_button.setEnabled(true);
            inputselect_button.setText(Html.fromHtml(selected));
        }
    }

    @Override
    public boolean onKeyDown ( int keyCode, KeyEvent event ) {
        display_keyevent(keyCode, event);
        return super.onKeyDown( keyCode, event );
    }
    @Override
    public boolean onKeyUp ( int keyCode, KeyEvent event ) {
        display_keyevent(keyCode, event);
        return super.onKeyUp( keyCode, event );
    }

    private void display_keyevent(int keyCode, KeyEvent event) {
        TextView show_input = (TextView) findViewById(R.id.inputcheck_tv);
        String action = event.toString().split("action=")[1].split(", keyCo")[0];
        if (Build.VERSION.SDK_INT >= 12) {
            show_input.setText(KeyEvent.keyCodeToString(keyCode)+", "+ action);
        } else {
            show_input.setText("keycode: "+Integer.toString(keyCode)+", "+ action);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("frontmain", "oncreate hit");
        setContentView(R.layout.activity_frontend_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("frontmain", "onResume hit");
        netcheck_task = new CheckForNetworkUpdates();
        netcheck_task.execute();
        update_button_state();
    }

    @Override
    protected void onPause() {
        super.onPause();
        netcheck_task.cancel();
        Log.d("frontmain", "onPause hit");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_frontend_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
