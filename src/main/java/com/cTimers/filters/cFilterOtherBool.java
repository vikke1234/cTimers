package com.cTimers.filters;

import com.cTimers.cRoomData;

public class cFilterOtherBool extends cFilterCondition
{
    boolean value;
    int param;

    public cFilterOtherBool(int param, boolean value)
    {
        this.param = param;
        this.value = value;
    }

    @Override
    public boolean evaluate(cRoomData data)
    {
        switch(param)
        {
            case 0:
                return data.maidenSkip == value;
            case 1:
                return data.maidenReset == value;
            case 2:
                return data.maidenWipe == value;
            case 3:
                return data.bloatReset == value;
            case 4:
                return data.bloatWipe == value;
            case 5:
                return data.nyloReset == value;
            case 6:
                return data.nyloWipe == value;
            case 7:
                return data.soteReset == value;
            case 8:
                return data.soteWipe == value;
            case 9:
                return data.xarpReset == value;
            case 10:
                return data.xarpWipe == value;
            case 11:
                return data.verzikWipe == value;
        }
        return false;
    }
}
