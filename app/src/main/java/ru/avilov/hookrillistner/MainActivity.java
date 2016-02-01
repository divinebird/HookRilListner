package ru.avilov.hookrillistner;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.AssetManager;
import android.os.Environment;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;

public class MainActivity extends AppCompatActivity {
    private Listner listner = null;
    private boolean isInstalled = false;

    private final static String TAG = MainActivity.class.getName();

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder binder) {
            listner = ((Listner.LocalBinder)binder).getService();
            Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_SHORT)
                    .show();
        }

        public void onServiceDisconnected(ComponentName className) {
            listner = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        File file = new File("/system/lib/libhookril.so");
        if(file.exists())
            isInstalled = true;

        if (Shell.SU.available()) {
            if(!isInstalled) {
                try {
                    installLib(false);
                } catch (IOException e) {
                    Log.e(TAG, "Copying error", e);
                }
            }
            List<String> res = Shell.SU.run("getprop rild.libpath");
            if(!res.get(0).contains("libhookril.so")) {
                Shell.SU.run(new String[] {"setprop rild.libpath /system/lib/libhookril.so",
                        "setprop rild.libpath_orig " + res.get(0),
                        "killall rild"
                });
            }
        }
        startService(new Intent(this, Listner.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
//        String libpath = System.getProperty("rild.libpath", null);
//        if(libpath != null && libpath.contains("libhookril")) {
            bindService(new Intent(this, Listner.class), mConnection, Context.BIND_AUTO_CREATE);
//        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(listner != null)
            unbindService(mConnection);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.my_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;

            case R.id.action_connect_button:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void installLib(boolean isMTK) throws IOException {
        String type = isMTK ? "mtk" : "clear";
        String libname = "libhookril.so";

        String outLibPath = getApplicationInfo().dataDir + "/" + libname;
        OutputStream myOutput = new FileOutputStream(outLibPath);
        byte[] buffer = new byte[1024];
        int length;
        InputStream myInput = getAssets().open(type + "/" + libname);
        while ((length = myInput.read(buffer)) > 0) {
            myOutput.write(buffer, 0, length);
        }
        myInput.close();
        myOutput.flush();
        myOutput.close();

        if (Shell.SU.available()) {
            Shell.SU.run("mount -o rw,remount /system");
            Shell.SU.run("cp " + outLibPath + " /system/lib/");
        }
    }
}
