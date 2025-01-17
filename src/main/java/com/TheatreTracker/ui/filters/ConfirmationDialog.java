package com.TheatreTracker.ui.filters;

import com.TheatreTracker.RoomData;
import com.TheatreTracker.filters.ImplicitFilter;
import com.TheatreTracker.filters.FilterManager;
import com.TheatreTracker.ui.BaseFrame;
import com.TheatreTracker.utility.datautility.RaidsManager;


import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class ConfirmationDialog extends BaseFrame
{
    public ConfirmationDialog(String filterName, ArrayList<ImplicitFilter> filters, JFrame root)
    {
        setTitle("Confirm");
        JPanel panelButtons = new JPanel(new GridLayout(1, 2));
        setLayout(new GridLayout(2, 1));
        add(new JLabel("Are you sure you want to overwrite filter " + filterName + "?"));
        JButton yesButton = new JButton("Yes");
        yesButton.addActionListener(e ->
        {
            FilterManager.saveOverwriteFilter(filterName, filters);
            close();
            root.setVisible(false);
        });
        JButton noButton = new JButton("No");
        noButton.addActionListener(e -> close());
        panelButtons.add(yesButton);
        panelButtons.add(noButton);
        add(panelButtons);
        pack();
        setLocationRelativeTo(null);
    }

    public ConfirmationDialog(String raidsName, ArrayList<RoomData> raids, JFrame root, int mark)
    {
        setTitle("Confirm");
        JPanel panelButtons = new JPanel(new GridLayout(1, 2));
        setLayout(new GridLayout(2, 1));
        add(new JLabel("Are you sure you want to overwrite raids " + raidsName + "?"));
        JButton yesButton = new JButton("Yes");
        yesButton.addActionListener(e ->
        {
            RaidsManager.saveOverwriteRaids(raidsName, raids);
            close();
            root.setVisible(false);
        });
        JButton noButton = new JButton("No");
        noButton.addActionListener(e -> close());
        panelButtons.add(yesButton);
        panelButtons.add(noButton);
        add(panelButtons);
        pack();
        setLocationRelativeTo(null);
    }
}
