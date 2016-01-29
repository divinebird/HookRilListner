package ru.avilov.hookrillistner;

import android.app.Service;
import android.content.Intent;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.IBinder;
import android.os.Parcel;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Listner extends Service {

    static final private String HOOK_RIL_SERVICE_LOG_TAG = Listner.class.getName();

    public Listner() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
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
        static final private int RESPONSE_SOLICITED = 0;
        static final private int RESPONSE_UNSOLICITED = 1;

        static final private byte RIL_REQUEST_FUNC = 1;
        static final private byte RIL_ON_REQUEST_COMPLETE = 2;
        static final private byte RIL_ON_UNSOLICITED_RESPONSE = 3;
        static final private byte RIL_RADIO_STATER_EQUEST = 4;


        byte[] buffer;
        boolean mStoped = false;
        static final int SOCKET_OPEN_RETRY_MILLIS = 1000;
        ByteBuffer byteBuffer;

        @Override
        public void run() {
            buffer = new byte[RIL_MAX_COMMAND_BYTES];
            int retryCount = 0;
            byteBuffer = ByteBuffer.allocate(32);
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
                        InputStream is = s.getInputStream();

                        for (;;) {
                            Parcel p;

                            CommandHeader header = readHeader(is);
                            if (header.length < 0) {
                                break;
                            }
                            if(header.length != is.read(buffer, 0, header.length)) {
                                break;
                            }

                            p = Parcel.obtain();
                            p.unmarshall(buffer, 0, header.length);
                            p.setDataPosition(0);

                            Log.v(HOOK_RIL_SERVICE_LOG_TAG, "Read packet: " + header.length + " bytes");

                            parseCommand(p);
                            p.recycle();
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
        }

        private void parseCommand(CommandHeader header, Parcel p) {
            int type = p.readInt();
        }

        private CommandHeader readHeader(InputStream is) throws IOException {
            int readlength = is.read(buffer, 0, 17);
            if(readlength != 13)
                throw new IOException();

            CommandHeader commandHeader = new CommandHeader();
            byteBuffer.put(buffer);
            commandHeader.functionId = byteBuffer.get();
            commandHeader.command = byteBuffer.getInt();
            commandHeader.tocken = byteBuffer.getInt();
            commandHeader.length = byteBuffer.getInt();
            return commandHeader;
        }

        private int readRilMessage(InputStream is, byte[] buffer) {
            return 0;
        }
    }

    
}
