package ru.avilov.hookrillistner;

public enum RadioState {
    RADIO_OFF,
    RADIO_UNAVAILABLE,
    SIM_NOT_READY,
    SIM_LOCKED_OR_ABSENT,
    SIM_READY,
    RUIM_NOT_READY,
    RUIM_READY,
    RUIM_LOCKED_OR_ABSENT,
    NV_NOT_READY,
    NV_READY,
    RADIO_ON;

    public static RadioState getRadioState(int state) {
        switch(state) {
            case 0:
                return RADIO_OFF;
            case 1:
                return RADIO_UNAVAILABLE;
            case 2:
                return SIM_NOT_READY;
            case 3:
                return SIM_LOCKED_OR_ABSENT;
            case 4:
                return SIM_READY;
            case 5:
                return RUIM_NOT_READY;
            case 6:
                return RUIM_READY;
            case 7:
                return RUIM_LOCKED_OR_ABSENT;
            case 8:
                return NV_NOT_READY;
            case 9:
                return NV_READY;
            case 10:
                return RADIO_ON;
            case 15:
                return RADIO_OFF;
        }
        return RADIO_OFF;
    }
}
