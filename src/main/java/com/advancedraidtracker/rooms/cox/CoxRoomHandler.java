package com.advancedraidtracker.rooms.cox;

import com.advancedraidtracker.AdvancedRaidTrackerConfig;
import com.advancedraidtracker.rooms.tob.RoomHandler;
import com.advancedraidtracker.utility.datautility.DataWriter;
import net.runelite.api.Client;

public class CoxRoomHandler extends RoomHandler
{
    public CoxRoomHandler(Client client, DataWriter clog, AdvancedRaidTrackerConfig config)
    {
        super(client, clog, config);
    }
}