package ru.avilov.hookrillistner;

import android.os.Parcel;
import android.util.Log;

import java.nio.ByteBuffer;

public class RilCommand {

    private final static int RIL_UNSOL_RESPONSE_RADIO_STATE_CHANGED = 1000;
    private final static int RIL_UNSOL_RESPONSE_CALL_STATE_CHANGED = 1001;

    private static final String TAG = RilCommand.class.getName();

    private static final int RESPONSE_SOLICITED = 0;
    private static final int RESPONSE_UNSOLICITED = 1;

    static final private byte RIL_REQUEST_FUNC = 1;
    static final private byte RIL_ON_REQUEST_COMPLETE = 2;
    static final private byte RIL_ON_UNSOLICITED_RESPONSE = 3;
    static final private byte RIL_RADIO_STATE_REQUEST = 4;

    byte functionId;
    int command;
    int tocken;
    int type;
    String commandName;
    Object commandObject;

    public static RilCommand createCommand(byte functionId, int command, int tocken) {
        RilCommand ret = new RilCommand();
        ret.functionId = functionId;
        ret.command = command;
        ret.tocken = tocken;
        ret.commandObject = null;
        ret.commandName = String.valueOf(command);
        Log.d(TAG, "functionId: " + functionId + ", command: " + command + ", tocken: " + tocken);

        return ret;
    }

    public static RilCommand parseCommandFromBuffer(byte functionId, int command, int tocken, ByteBuffer byteBuffer) {
        RilCommand ret = null;

        if(functionId > 1) {
            int respType = functionId - 2;

            if(respType == RESPONSE_UNSOLICITED) {
                Log.d(TAG, "Unsolicated responce");
                switch (command) {
                    case RIL_UNSOL_RESPONSE_RADIO_STATE_CHANGED:
//                        RadioState newState = RadioState.getRadioState(byteBuffer.getInt());
//                        ret.commandObject = newState;
                        Log.d(TAG, "Radio state changed");
                        ret = createCommand(functionId, command, tocken);
                        ret.commandName = "Radio state changed";
                        break;
                    case RIL_UNSOL_RESPONSE_CALL_STATE_CHANGED:
                        Log.d(TAG, "Call state changed");
                        ret = createCommand(functionId, command, tocken);
                        ret.commandName = "Call state changed";
                        break;
                }
            }
            else if(respType == RESPONSE_SOLICITED) {
                Log.d(TAG, "Solicated responce");
            }
        }
        else {
            Log.d(TAG, "This is request");
        }

        return ret;
    }
}
