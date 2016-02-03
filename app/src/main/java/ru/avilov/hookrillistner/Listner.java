package ru.avilov.hookrillistner;

import android.app.Service;
import android.content.Intent;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcel;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;

public class Listner extends Service {

    static final private String HOOK_RIL_SERVICE_LOG_TAG = Listner.class.getName();

    private ArrayList<RilCommand> commands = new ArrayList<>();
    private static final int COMMAND_ARRAY_SIZE = 40;
    Thread worker;
    boolean mStoped = false;

    private final IBinder mBinder = new LocalBinder();

    public Listner() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        worker = new Thread(new CommandsReceiver());
        worker.start();
    }

    private Handler mHandler;
    public void setHandler(Handler hnd) {
        mHandler = hnd;
        if(mHandler != null)
            sendCommands();
    }

    private void sendCommands() {
        if (mHandler == null) //TODO: May be sync errors
            return;

        for(RilCommand command : commands) {
            Message msg = mHandler.obtainMessage();
            msg.obj = command;
            mHandler.sendMessage(msg);
        }
        commands.clear();
    }

    @Override
    public void onDestroy() {
        mStoped = true;
        worker.interrupt();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    class CommandHeader {
        byte functionId;
        int command;
        int tocken;
        int length;
    }

    class CommandsReceiver implements Runnable {
        static final private String SOCK_PATH = "hookril_tranlator";
        static final private int RIL_MAX_COMMAND_BYTES = 8 * 1024;

        static final int SOCKET_OPEN_RETRY_MILLIS = 1000;
        ByteBuffer byteBuffer;

        @Override
        public void run() {
            int retryCount = 0;
            byteBuffer = ByteBuffer.allocate(RIL_MAX_COMMAND_BYTES * 2);
            byteBuffer.order(ByteOrder.nativeOrder());

            try {
                while(!mStoped) {

                    LocalSocket s = null;
                    LocalSocketAddress l;

                    try {
                        s = new LocalSocket();
                        l = new LocalSocketAddress(SOCK_PATH, LocalSocketAddress.Namespace.ABSTRACT);
                        s.connect(l);
                    } catch (IOException ex){
                        try {
                            if (s != null) {
                                s.close();
                            }
                        } catch (IOException ex2) {
                        }

                        if (retryCount == 6) {
                            Log.e (HOOK_RIL_SERVICE_LOG_TAG, "Couldn't find '" + SOCK_PATH + "' socket after " + retryCount + " times, breaking");
                            mStoped = true;
                        } else if (retryCount > 0 && retryCount < 6) {
                            Log.i (HOOK_RIL_SERVICE_LOG_TAG, "Couldn't find '" + SOCK_PATH + "' socket; retrying after timeout");
                        }

                        try {
                            Thread.sleep(SOCKET_OPEN_RETRY_MILLIS);
                        } catch (InterruptedException er) {
                        }

                        retryCount++;
                        continue;
                    }

                    retryCount = 0;

                    try {
                        ReadableByteChannel readableChannel = Channels.newChannel(s.getInputStream());

                        while(!mStoped) {
                            CommandHeader header = readHeader(readableChannel);
                            if (header == null || header.length < 0) {
                                Log.e(HOOK_RIL_SERVICE_LOG_TAG, "The header or header.length is wrong");
                                continue;
                            }
                            Log.v(HOOK_RIL_SERVICE_LOG_TAG, "Read packet: " + header.length + " bytes");

                            RilCommand command = null;
                            if(header.length == 0) {
                                command = RilCommand.parseCommandFromBuffer(header.functionId, header.command, header.tocken, null);
//                                command = RilCommand.createCommand(header.functionId, header.command, header.tocken);
                            }
                            else {
                                readWithSize(readableChannel, header.length);
                                byteBuffer.position(0);
                                command = RilCommand.parseCommandFromBuffer(header.functionId, header.command, header.tocken, byteBuffer);
                                byteBuffer.position(header.length);
                                byteBuffer.compact();
                            }

                            if(command != null) {
                                commands.add(command);
                                sendCommands();
                                while(commands.size() > COMMAND_ARRAY_SIZE)
                                    commands.remove(0);
                            }
                        }
                    } catch (java.io.IOException ex) {
                        Log.i(HOOK_RIL_SERVICE_LOG_TAG, "Socket closed", ex);
                    }

                    Log.i(HOOK_RIL_SERVICE_LOG_TAG, "Disconnected from socket");

                    try {
                        s.close();
                    } catch (IOException ex) {
                    }

                }
            }
            catch (Throwable tr) {
                Log.e(HOOK_RIL_SERVICE_LOG_TAG, "Uncaught exception", tr);
            }
            worker = null;
        }

        private CommandHeader readHeader(ReadableByteChannel readableByteChannel) throws IOException {
            readWithSize(readableByteChannel, 13);

            CommandHeader commandHeader = new CommandHeader();
            byteBuffer.position(0);
            commandHeader.functionId = byteBuffer.get();
            commandHeader.command = byteBuffer.getInt();
            commandHeader.tocken = byteBuffer.getInt();
            commandHeader.length = byteBuffer.getInt();
            byteBuffer.compact();
            return commandHeader;
        }

        private void readWithSize(ReadableByteChannel readableByteChannel, int length) throws IOException {
            while(byteBuffer.position() < length)
                readableByteChannel.read(byteBuffer);
            byteBuffer.limit(byteBuffer.position());
        }
    }

    public class LocalBinder extends Binder {
        Listner getService() {
            return Listner.this;
        }
    }
}
