package com.TheatreTracker.utility.datautility;

import com.TheatreTracker.utility.datautility.DataPoint;
import lombok.Getter;
import lombok.Setter;

public class DataPointWrapper
{
    @Setter
    @Getter
    public int value = 0;
    DataPoint dataPoint;

    public DataPointWrapper(DataPoint point)
    {
        dataPoint = point;
        if (point.equals(DataPoint.MAIDEN_DEFENSE))
        {
            value = 200;
        } else if (point.equals(DataPoint.BLOAT_DEFENSE))
        {
            value = 100;
        } else if (point.equals(DataPoint.NYLO_DEFENSE))
        {
            value = 50;
        } else if (point.equals(DataPoint.XARP_DEFENSE))
        {
            value = 250;
        } else if (point.equals(DataPoint.VERZIK_HP_AT_WEBS))
        {
            value = -1;
        }
    }

    public void increment(int valueAdded)
    {
        value += valueAdded;
    }

    public void increment()
    {
        value++;
    }
}
