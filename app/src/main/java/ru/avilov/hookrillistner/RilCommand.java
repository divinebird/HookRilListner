package ru.avilov.hookrillistner;

import android.os.Parcel;
import android.util.Log;

/**
 * Created by vasily on 31.01.16.
 */
public class RilCommand {
    private static final String TAG = RilCommand.class.getName();

    static final private int RESPONSE_SOLICITED = 0;
    static final private int RESPONSE_UNSOLICITED = 1;

    static final private byte RIL_REQUEST_FUNC = 1;
    static final private byte RIL_ON_REQUEST_COMPLETE = 2;
    static final private byte RIL_ON_UNSOLICITED_RESPONSE = 3;
    static final private byte RIL_RADIO_STATE_REQUEST = 4;

    byte functionId;
    int command;
    int tocken;
    int type;
    Object commandObject;

    public static RilCommand parseCommandFromParcel(byte functionId, int command, int tocken, Parcel p) {
        RilCommand ret = new RilCommand();
        ret.functionId = functionId;
        ret.command = command;
        ret.tocken = tocken;

        Log.d(TAG, "functionId: " + functionId + ", command: " + command + ", tocken: " + tocken);
        return ret;
    }
}
