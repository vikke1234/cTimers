package com.advancedraidtracker.rooms.cox;

import com.advancedraidtracker.AdvancedRaidTrackerConfig;
import com.advancedraidtracker.utility.datautility.DataWriter;
import net.runelite.api.Client;

public class ShamansHandler extends CoxRoomHandler
{
    public ShamansHandler(Client client, DataWriter clog, AdvancedRaidTrackerConfig config)
    {
        super(client, clog, config);
    }
}