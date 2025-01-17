package com.TheatreTracker.ui.comparisonview.graph;

import com.TheatreTracker.RoomData;
import com.TheatreTracker.TheatreTrackerConfig;
import com.TheatreTracker.ui.Raids;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;

import javax.swing.*;
import java.util.ArrayList;

@Slf4j
public class GraphRightClickContextMenu extends JPopupMenu
{
    JMenuItem item;

    public GraphRightClickContextMenu(ArrayList<RoomData> raids, TheatreTrackerConfig config, ItemManager itemManager, ClientThread clientThread)
    {
        item = new JMenuItem("Show Represented Raids In New Window");
        item.addActionListener(al ->
        {
            Raids raidFrame = new Raids(config, itemManager, clientThread);
            raidFrame.createFrame(raids);
            raidFrame.repaint();
            raidFrame.open();
        });
        add(item);
    }
}
