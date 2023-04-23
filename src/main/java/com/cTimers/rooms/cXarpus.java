package com.cTimers.rooms;

import com.cTimers.cTimersConfig;
import com.cTimers.constants.LogID;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.events.*;
import com.cTimers.Point;
import com.cTimers.utility.RoomUtil;
import com.cTimers.utility.cLogger;
import com.cTimers.utility.cRoomState;

import static com.cTimers.constants.LogID.*;
import static com.cTimers.constants.LogID.ACCURATE_XARP_END;
import static com.cTimers.constants.LogID.XARPUS_STARTED;

@Slf4j
public class cXarpus extends cRoom
{
    public cRoomState.XarpusRoomState roomState = cRoomState.XarpusRoomState.NOT_STARTED;

    public cXarpus(Client client, cLogger clog, cTimersConfig config)
    {
        super(client, clog, config);
    }
    private int xarpusEntryTick = -1;
    private int xarpusExhumedsEnd = -1;
    private int xarpusScreechTick = -1;
    private int xarpusEndTick = -1;

    public void reset()
    {
        log.info("Resetting xarpus");
        xarpusEntryTick = -1;
        xarpusExhumedsEnd = -1;
        xarpusScreechTick = -1;
        xarpusEndTick = -1;
        roomState = cRoomState.XarpusRoomState.NOT_STARTED;
    }

    public void updateAnimationChanged(AnimationChanged event)
    {
        if(event.getActor().getAnimation() == 8061)
        {
            endExhumeds();
        }
        if(event.getActor().getAnimation() == 8063)
        {
            endXarpus();
        }
    }

    public void updateProjectileMoved(ProjectileMoved event)
    {
        if(event.getProjectile().getEndCycle()-event.getProjectile().getStartCycle() == event.getProjectile().getRemainingCycles())
        {
            if(event.getProjectile().getId() == 1550)
            {
                clog.write(XARPUS_HEAL);
            }
        }
    }

    public void updateOverheadText(OverheadTextChanged event)
    {
        if(event.getActor() instanceof NPC)
        {
            NPC npc = (NPC) event.getActor();
            if(npc.getId() == 8340)
            {
                startScreech();
            }
        }
    }

    public void updateNpcDespawned(NpcDespawned event)
    {
        switch(event.getNpc().getId())
        {
            case 8338:
            case 8339:
            case 8340:
            case 8341:
                clog.write(XARPUS_DESPAWNED, ""+(xarpusEndTick-xarpusEntryTick));
        }
    }

    public void updateGameTick(GameTick event)
    {
        if(roomState == cRoomState.XarpusRoomState.NOT_STARTED)
        {
            if(RoomUtil.crossedLine(RoomUtil.XARPUS_REGION, new Point(25, 12), new Point(27, 12), false, client))
            {
                startXarpus();
            }
        }
    }

    private void startScreech()
    {
        clog.write(XARPUS_SCREECH, ""+(client.getTickCount()-xarpusEntryTick));
        roomState = cRoomState.XarpusRoomState.POSTSCREECH;
        xarpusScreechTick = client.getTickCount();
        String splitMessage = "Wave 'Xarpus phase 2' complete. Duration: " + timeColor() + RoomUtil.time(xarpusScreechTick-xarpusEntryTick) + " (" + RoomUtil.time(xarpusScreechTick-xarpusExhumedsEnd) + ")";
        this.client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", splitMessage, null,false);
    }

    private void startXarpus()
    {
        roomState = cRoomState.XarpusRoomState.EXHUMEDS;
        xarpusEntryTick = client.getTickCount();
        clog.write(XARPUS_STARTED);
        clog.write(ACCURATE_XARP_START);
    }

    private void endExhumeds()
    {
        roomState = cRoomState.XarpusRoomState.PRESCREECH;
        xarpusExhumedsEnd = client.getTickCount();
        String splitMessage = "Wave 'Xarpus phase 1' complete. Duration: " + timeColor() + RoomUtil.time(xarpusExhumedsEnd-xarpusEntryTick);
        this.client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", splitMessage, null,false);
    }

    private void endXarpus()
    {

        roomState = cRoomState.XarpusRoomState.FINISHED;
        xarpusEndTick = client.getTickCount()+3;
        clog.write(ACCURATE_XARP_END);
        String splitMessage = "Wave 'Xarpus phase 3' complete. Duration: " + timeColor() + RoomUtil.time(xarpusEndTick-xarpusEntryTick) + " (" + RoomUtil.time(xarpusEndTick-xarpusScreechTick) + ")";
        this.client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", splitMessage, null,false);
    }
}
