package com.TheatreTracker.ui;


import com.TheatreTracker.RoomData;
import com.TheatreTracker.TheatreTrackerConfig;
import com.TheatreTracker.filters.*;
import com.TheatreTracker.ui.buttons.*;
import com.TheatreTracker.ui.charts.ChartFrame;
import com.TheatreTracker.ui.comparisonview.ComparisonViewFrame;
import com.TheatreTracker.ui.comparisonview.ComparisonViewPanel;
import com.TheatreTracker.ui.comparisonview.NoDataPopUp;
import com.TheatreTracker.ui.crableaks.CrabLeakInfo;
import com.TheatreTracker.ui.exportraids.SaveRaids;
import com.TheatreTracker.ui.filters.LoadFilter;
import com.TheatreTracker.ui.filters.SaveFilter;
import com.TheatreTracker.ui.statistics.StatisticTab;
import com.TheatreTracker.ui.summary.SummarizeRaids;
import com.TheatreTracker.utility.*;
import com.TheatreTracker.utility.datautility.DataPoint;
import com.TheatreTracker.utility.datautility.DataWriter;
import com.TheatreTracker.utility.wrappers.PlayerCorrelatedPointData;
import com.TheatreTracker.utility.wrappers.StringInt;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

import static com.TheatreTracker.utility.UISwingUtility.*;

@Slf4j
public class Raids extends BaseFrame
{
    private final ArrayList<Integer> filteredIndices;
    private JTable comparisonTable;
    private final ArrayList<ArrayList<RoomData>> comparisons;

    private final JTabbedPane tabbedPane = new JTabbedPane();
    public ArrayList<ImplicitFilter> activeFilters;
    private final JLabel raidsFoundLabel = new JLabel("", SwingConstants.LEFT);
    private final JLabel completionsFound = new JLabel("", SwingConstants.LEFT);
    private JComboBox<String> viewByRaidComboBox;

    private JComboBox<String> sortOrderBox;
    private JComboBox<String> sortOptionsBox;

    Map<String, JLabel> averageLabels = new LinkedHashMap<>();
    Map<String, JLabel> medianLabels = new LinkedHashMap<>();
    Map<String, JLabel> minLabels = new LinkedHashMap<>();
    Map<String, JLabel> maxLabels = new LinkedHashMap<>();
    public JComboBox statisticsBox;
    public JLabel customAverageLabel = new JLabel("", SwingConstants.RIGHT);
    public JLabel customMedianLabel = new JLabel("", SwingConstants.RIGHT);
    public JLabel customModeLabel = new JLabel("", SwingConstants.RIGHT);
    public JLabel customMinLabel = new JLabel("", SwingConstants.RIGHT);
    public JLabel customMaxLabel = new JLabel("", SwingConstants.RIGHT);

    private ArrayList<Map<String, ArrayList<String>>> aliases;

    private final JTextArea aliasText;

    JTextField dateTextField;
    JCheckBox filterSpectateOnly;
    JCheckBox filterInRaidOnly;
    JCheckBox filterCompletionOnly;
    JCheckBox filterWipeResetOnly;
    JComboBox filterComboBoxScale;
    JCheckBox filterCheckBoxScale;
    JCheckBox filterTodayOnly;
    JCheckBox filterPartyOnly;
    JCheckBox filterPartialData;
    JCheckBox filterPartialOnly;
    JCheckBox filterNormalOnly;
    JTable table;
    JPanel container;
    private JPanel filterTableContainer;
    public ArrayList<RoomData> currentData;
    private JComboBox<String> timeFilterChoice;
    private JComboBox<String> timeFilterOperator;
    private JTextField timeFilterValue;
    private JTable filterTable;
    private JComboBox<String> playerFilterOperator;
    private JTextField playerFilterValue;
    private JCheckBox timeFollowsTab;
    private StatisticTab maidenTab;
    private StatisticTab bloatTab;
    private StatisticTab nyloTab;
    private StatisticTab soteTab;
    private StatisticTab xarpTab;
    private StatisticTab verzikTab;
    private boolean built = false;
    private JComboBox<String> dateFilterOperator;
    private JTextField dateFilterValue;
    private JComboBox<String> otherIntFilterChoice;
    private JComboBox<String> otherIntFilterOperator;
    private JTextField otherIntFilterValue;
    private JComboBox<String> otherBoolFilterChoice;
    private JComboBox<String> otherBoolFilterOperator;


    private final TheatreTrackerConfig config;
    private final ItemManager itemManager;
    private ClientThread clientThread;

    public String[] rooms = {"Maiden", "Bloat","Nylocas","Sotetseg","Xarpus","Verzik","Challenge"};

    public Raids(TheatreTrackerConfig config, ItemManager itemManager, ClientThread clientThread)
    {
        for(String s : rooms)
        {
            averageLabels.put(s, getDarkJLabel("", SwingConstants.RIGHT));
            medianLabels.put(s, getDarkJLabel("", SwingConstants.RIGHT));
            minLabels.put(s, getDarkJLabel("", SwingConstants.RIGHT));
            maxLabels.put(s, getDarkJLabel("", SwingConstants.RIGHT));
        }
        this.clientThread = clientThread;
        this.itemManager = itemManager;
        columnHeaders = new ArrayList<>();
        for (String s : columnHeaderNames)
        {
            columnHeaders.add(getCheckBoxMenuItem(s));
        }
        aliases = new ArrayList<>();
        filteredIndices = new ArrayList<>();
        comparisons = new ArrayList<>();
        activeFilters = new ArrayList<>();
        aliasText = new JTextArea();
        this.config = config;
        this.setPreferredSize(new Dimension(1200, 820));
    }

    public void updateCustomStats(ArrayList<RoomData> raids)
    {
        DataPoint dataPoint = DataPoint.ATTEMPTED_BGS_BLOAT;
        boolean time = dataPoint.type == DataPoint.types.TIME;

        double avg = StatisticGatherer.getGenericAverage(raids, dataPoint);
        double med = StatisticGatherer.getGenericMedian(raids, dataPoint);
        double mod = StatisticGatherer.getGenericMode(raids, dataPoint);
        double min = StatisticGatherer.getGenericMin(raids, dataPoint);
        double max = StatisticGatherer.getGenericMax(raids, dataPoint);

        String avgStr = (time) ? RoomUtil.time(avg) : String.valueOf(avg);
        String medStr = (time) ? RoomUtil.time(med) : String.valueOf(med);
        String modStr = (time) ? RoomUtil.time(mod) : String.valueOf(mod);
        String minStr = (time) ? RoomUtil.time(min) : String.valueOf(min);
        String maxStr = (time) ? RoomUtil.time(max) : String.valueOf(max);

        if (avg == -1) avgStr = "-";
        if (med == -1) medStr = "-";
        if (mod == -1) modStr = "-";
        if (min == -1) minStr = "-";
        if (max == -1) maxStr = "-";

        customAverageLabel.setText(avgStr);
        customMedianLabel.setText(medStr);
        customModeLabel.setText(modStr);
        customMinLabel.setText(minStr);
        customMaxLabel.setText(maxStr);
    }

    private boolean evaluateAllFilters(RoomData data)
    {
        for (ImplicitFilter filter : activeFilters)
        {
            if (!filter.evaluate(data))
            {
                return false;
            }
        }
        return true;
    }

    public void updateTable()
    {
        String timeToDisplay = "0";
        int completions = 0;
        ArrayList<RoomData> tableData = new ArrayList<>();
        for (RoomData data : currentData)
        {
            boolean shouldDataBeIncluded = true;
            if (filterSpectateOnly.isSelected())
            {
                if (!data.spectated)
                {
                    shouldDataBeIncluded = false;
                }
            }
            if (filterInRaidOnly.isSelected())
            {
                if (data.spectated)
                {
                    shouldDataBeIncluded = false;
                }
            }
            if (filterCompletionOnly.isSelected())
            {
                if (!data.raidCompleted || !data.getOverallTimeAccurate())
                {
                    shouldDataBeIncluded = false;
                }
            }
            if (filterWipeResetOnly.isSelected())
            {
                if (data.raidCompleted)
                {
                    shouldDataBeIncluded = false;
                }
            }
            if (filterPartialData.isSelected())
            {
                if (!(data.maidenStartAccurate == data.maidenEndAccurate &&
                        data.bloatStartAccurate == data.bloatEndAccurate &&
                        data.nyloStartAccurate == data.nyloEndAccurate &&
                        data.soteStartAccurate == data.soteEndAccurate &&
                        data.xarpStartAccurate == data.xarpEndAccurate &&
                        data.verzikStartAccurate == data.verzikEndAccurate))
                {
                    shouldDataBeIncluded = false;
                }
            }
            if (shouldDataBeIncluded && filterTodayOnly.isSelected())
            {
                shouldDataBeIncluded = false;
                Calendar cal1 = Calendar.getInstance();
                Calendar cal2 = Calendar.getInstance();
                cal1.setTime(data.raidStarted);
                cal2.setTime(new Date(System.currentTimeMillis()));
                if (cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) && cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH))
                {
                    shouldDataBeIncluded = true;
                }
            }
            if (filterPartyOnly.isSelected())
            {
                if (!data.maidenDefenseAccurate || !data.bloatDefenseAccurate || !data.nyloDefenseAccurate || !data.soteDefenseAccurate || !data.xarpDefenseAccurate)
                {
                    shouldDataBeIncluded = false;
                }
            }
            if (filterNormalOnly.isSelected())
            {
                if (data.storyMode || data.hardMode)
                {
                    shouldDataBeIncluded = false;
                }
            }
            if (filterPartialOnly.isSelected())
            {
                switch (viewByRaidComboBox.getSelectedItem().toString())
                {
                    case "Challenge Time":
                        if (!data.getOverallTimeAccurate())
                        {
                            shouldDataBeIncluded = false;
                        }
                        break;
                    case "Maiden Time":
                        if (!data.maidenStartAccurate || !data.maidenEndAccurate)
                        {
                            shouldDataBeIncluded = false;
                        }
                        break;
                    case "Bloat Time":
                        if (!data.bloatStartAccurate || !data.bloatEndAccurate)
                        {
                            shouldDataBeIncluded = false;
                        }
                        break;
                    case "Nylo Time":
                        if (!data.nyloStartAccurate || !data.nyloEndAccurate)
                        {
                            shouldDataBeIncluded = false;
                        }
                        break;
                    case "Sote Time":
                        if (!data.soteStartAccurate || !data.soteEndAccurate)
                        {
                            shouldDataBeIncluded = false;
                        }
                        break;
                    case "Xarp Time":
                        if (!data.xarpStartAccurate || !data.xarpEndAccurate)
                        {
                            shouldDataBeIncluded = false;
                        }
                        break;
                    case "Verzik Time":
                        if (!data.verzikStartAccurate || !data.verzikEndAccurate)
                        {
                            shouldDataBeIncluded = false;
                        }
                        break;
                }
            }
            if (shouldDataBeIncluded && filterCheckBoxScale.isSelected())
            {
                shouldDataBeIncluded = filterComboBoxScale.getSelectedIndex() + 1 == data.raidTeamSize;
            }
            timeToDisplay = String.valueOf(data.getSpecificTimeInactive(viewByRaidComboBox.getSelectedItem().toString()));

            for (Integer i : filteredIndices)
            {
                if (data.index == i)
                {
                    shouldDataBeIncluded = false;
                }
            }
            if (!evaluateAllFilters(data))
            {
                shouldDataBeIncluded = false;
            }
            if (shouldDataBeIncluded)
            {
                tableData.add(data);
                if (data.raidCompleted && data.getOverallTimeAccurate())
                {
                    completions++;
                }
            }
        }
        if (sortOptionsBox.getSelectedIndex() == 0)
        {
            if (sortOrderBox.getSelectedIndex() == 0)
            {
                tableData.sort(Comparator.comparing(RoomData::getDate));
            } else
            {
                tableData.sort(Comparator.comparing(RoomData::getDate).reversed());
            }
        } else if (sortOptionsBox.getSelectedIndex() == 1)
        {
            if (sortOrderBox.getSelectedIndex() == 0)
            {
                for (RoomData data : tableData)
                {
                    data.activeValue = viewByRaidComboBox.getSelectedItem().toString();
                }
                tableData.sort(Comparator.comparing(RoomData::getSpecificTime));
            } else
            {
                for (RoomData data : tableData)
                {
                    data.activeValue = viewByRaidComboBox.getSelectedItem().toString();
                }
                tableData.sort(Comparator.comparing(RoomData::getSpecificTime).reversed());
            }
        } else if (sortOptionsBox.getSelectedIndex() == 2)
        {
            if (sortOrderBox.getSelectedIndex() == 0)
            {
                tableData.sort(Comparator.comparing(RoomData::getScale));
            } else
            {
                tableData.sort(Comparator.comparing(RoomData::getScale).reversed());
            }
        }

        updateCustomStats(tableData);
        raidsFoundLabel.setText("Raids Found: " + tableData.size());
        completionsFound.setText("Completions Found: " + completions);
        updateTabNames(tableData);

        ArrayList<String> columnNamesDynamic = new ArrayList<>();
        columnNamesDynamic.add("");
        for (JCheckBoxMenuItem item : columnHeaders)
        {
            if (item.getState())
            {
                columnNamesDynamic.add(item.getText());
            }
            if (item.getText().equals("Status"))
            {
                columnNamesDynamic.add(viewByRaidComboBox.getSelectedItem().toString());
            }
        }
        ArrayList<Object[]> tableBuilder = new ArrayList<>();
        for (RoomData raid : tableData)
        {
            ArrayList<Object> rowBuilder = new ArrayList<>();
            for (String column : columnNamesDynamic)
            {
                rowBuilder.add(getRowData(column, raid));
            }
            tableBuilder.add(rowBuilder.toArray());
        }
        int columns = 0;
        if (tableBuilder.size() != 0)
        {
            columns = tableBuilder.get(0).length;
        }
        Object[][] tableObject = new Object[tableData.size()][columns];
        int count = 0;
        for (Object[] row : tableBuilder)
        {
            tableObject[count] = row;
            count++;
        }

        table.setModel(new DefaultTableModel(tableObject, columnNamesDynamic.toArray()));
        for (int i = 0; i < table.getColumnCount(); i++)
        {
            if (table.getColumnName(i).equals("View"))
            {
                table.getColumn(table.getColumnName(i)).setCellEditor(new ButtonEditorRoomData(new JCheckBox(), tableData));
                table.getColumn(table.getColumnName(i)).setCellRenderer(new ButtonRenderer());
            } else
            {
                table.getColumn(table.getColumnName(i)).setCellEditor(new NonEditableCell(new JTextField()));
                table.getColumn(table.getColumnName(i)).setCellRenderer(new StripedTableRowCellRenderer());
            }
        }

        resizeColumnWidth(table);
        table.setFillsViewportHeight(true);
        setLabels(tableData);
        container.validate();
        container.repaint();
    }

    public Object getRowData(String column, RoomData raid)
    {
        switch (column)
        {
            case "":
                return raid.index;
            case "Date":
                Calendar cal = Calendar.getInstance();
                cal.setTime(raid.raidStarted);
                String dateString = (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.DAY_OF_MONTH) + "-" + cal.get(Calendar.YEAR);
                return dateString;
            case "Scale":
                return raid.getScaleString();
            case "Status":
                return raid.getRoomStatus();
            case "Players":
                StringBuilder players = new StringBuilder();
                for (String s : raid.players.keySet())
                {
                    players.append(s).append(", ");
                }
                return (players.length() > 2) ? players.substring(0, players.length() - 2) : "";
            case "Spectate":
                return (raid.spectated) ? "Yes" : "No";
            case "View":
                return "View";
            case "Time":
                Calendar cal2 = Calendar.getInstance();
                cal2.setTime(raid.raidStarted);
                int hour = cal2.get(Calendar.HOUR_OF_DAY);
                int minute = cal2.get(Calendar.MINUTE);
                String minuteString = (minute < 10) ? "0" + minute : String.valueOf(minute);
                String period = (hour > 11) ? " PM" : " AM";
                if (hour == 0)
                {
                    hour = 12;
                } else if (hour > 12)
                {
                    hour -= 12;
                }
                return hour + ":" + minuteString + period;
        }
        String valueToDisplay = "(?)";
        try
        {
            PlayerCorrelatedPointData pointData = raid.getSpecificTimeInactiveCorrelated(column);
            if (pointData == null)
            {
                valueToDisplay = String.valueOf(raid.getSpecificTimeInactive(column));
            } else
            {
                if (pointData.value == 0)
                {
                    valueToDisplay = "0";
                } else
                {
                    valueToDisplay = pointData.value + " (" + pointData.player + ")";
                }
            }
        } catch (Exception e)
        {

        }
        return (isTime(column) ? RoomUtil.time(valueToDisplay) : valueToDisplay);
    }

    boolean isTime(String value)
    {
        try
        {
            if (!value.contains("Player:"))
            {
                return (Objects.requireNonNull(DataPoint.getValue(Objects.requireNonNull(value)).type == DataPoint.types.TIME));
            } else
            {
                return false;
            }
        } catch (Exception e)
        {
            return false;
        }
    }

    boolean isTime()
    {
        if (!viewByRaidComboBox.getSelectedItem().toString().contains("Player:"))
        {
            return (Objects.requireNonNull(DataPoint.getValue(Objects.requireNonNull(viewByRaidComboBox.getSelectedItem()).toString())).type == DataPoint.types.TIME);
        } else
        {
            return false;
        }
    }


    private void updateTabNames(ArrayList<RoomData> data)
    {
        int maidenCount = 0;
        int bloatCount = 0;
        int nyloCount = 0;
        int soteCount = 0;
        int xarpCount = 0;
        int verzikCount = 0;
        for (RoomData d : data)
        {
            if (d.maidenStartAccurate && d.maidenEndAccurate)
            {
                maidenCount++;
            }
            if (d.bloatStartAccurate && d.bloatEndAccurate)
            {
                bloatCount++;
            }
            if (d.nyloStartAccurate && d.nyloEndAccurate)
            {
                nyloCount++;
            }
            if (d.soteStartAccurate && d.soteEndAccurate)
            {
                soteCount++;
            }
            if (d.xarpStartAccurate && d.xarpEndAccurate)
            {
                xarpCount++;
            }
            if (d.verzikStartAccurate && d.verzikEndAccurate)
            {
                verzikCount++;
            }
        }
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabbedPane.setTitleAt(1, "Maiden (" + maidenCount + ")");
        tabbedPane.setTitleAt(2, "Bloat (" + bloatCount + ")");
        tabbedPane.setTitleAt(3, "Nylo (" + nyloCount + ")");
        tabbedPane.setTitleAt(4, "Sotetseg (" + soteCount + ")");
        tabbedPane.setTitleAt(5, "Xarpus (" + xarpCount + ")");
        tabbedPane.setTitleAt(6, "Verzik (" + verzikCount + ")");
    }

    public void resizeColumnWidthFilters(JTable table)
    {
        final TableColumnModel columnModel = table.getColumnModel();
        for (int column = 0; column < table.getColumnCount(); column++)
        {
            int width = 25; // Min width
            for (int row = 0; row < table.getRowCount(); row++)
            {
                TableCellRenderer renderer = table.getCellRenderer(row, column);
                Component comp = table.prepareRenderer(renderer, row, column);
                width = Math.max(comp.getPreferredSize().width + 1, width);
            }
            if (width > 300)
            {
                width = 300;
            }
            columnModel.getColumn(column).setPreferredWidth(width);
        }
    }

    public void resizeColumnWidth(JTable table)
    {
        final TableColumnModel columnModel = table.getColumnModel();
        for (int column = 0; column < table.getColumnCount(); column++)
        {
            int width = 50; // Min width
            for (int row = 0; row < table.getRowCount(); row++)
            {
                TableCellRenderer renderer = table.getCellRenderer(row, column);
                Component comp = table.prepareRenderer(renderer, row, column);
                width = Math.max(comp.getPreferredSize().width + 1, width);
            }
            if (width > 500)
            {
                width = 500;
            }
            columnModel.getColumn(column).setPreferredWidth(width);
        }
    }

    public void setLabels(ArrayList<RoomData> data)
    {
        setOverallLabels(data);
        maidenTab.updateTab(data);
        bloatTab.updateTab(data);
        nyloTab.updateTab(data);
        soteTab.updateTab(data);
        xarpTab.updateTab(data);
        verzikTab.updateTab(data);
    }

    public void setOverallLabels(ArrayList<RoomData> data)
    {
        setOverallAverageLabels(data);
        setOverallMedianLabels(data);
        setOverallMinLabels(data);
        setOverallMaxLabels(data);
    }

    public void setOverallAverageLabels(ArrayList<RoomData> data)
    {
        for(String s : averageLabels.keySet())
        {
            averageLabels.get(s).setText(RoomUtil.time(StatisticGatherer.getGenericAverage(data, DataPoint.getValue(s + " Time"))));
        }
    }

    public void setOverallMedianLabels(ArrayList<RoomData> data)
    {
        for(String s : medianLabels.keySet())
        {
            medianLabels.get(s).setText(RoomUtil.time(StatisticGatherer.getGenericMedian(data, DataPoint.getValue(s + " Time"))));
        }
    }

    public void setOverallMinLabels(ArrayList<RoomData> data)
    {
        for(String s : minLabels.keySet())
        {
            minLabels.get(s).setText(RoomUtil.time(StatisticGatherer.getGenericMin(data, DataPoint.getValue(s + " Time"))));
        }
    }

    private void setOverallMaxLabels(ArrayList<RoomData> data)
    {
        for(String s : maxLabels.keySet())
        {
            maxLabels.get(s).setText(RoomUtil.time(StatisticGatherer.getGenericMax(data, DataPoint.getValue(s + " Time"))));
        }
    }

    private JPopupMenu comboPopupMenu;
    private ArrayList<String> comboStrictData;
    private AbstractButton arrowButton;
    private boolean writing = false;

    private void updateAliases()
    {
        writing = true;
        aliases.clear();
        aliasText.setText("");
        for (String s : DataWriter.readAliasFile())
        {
            aliasText.append(s + "\n");
            String[] split = s.split(":");
            if (split.length != 2)
            {
                continue;
            }
            String name = split[0];
            ArrayList<String> names = new ArrayList<String>(Arrays.asList(split[1].split(",")));
            if (!names.isEmpty())
            {
                Map<String, ArrayList<String>> map = new LinkedHashMap<>();
                map.put(name, names);
                aliases.add(map);
            }
        }
        writing = false;
    }

    private void setPopupVisible(boolean visible)
    {
        if (visible)
        {
            comboPopupMenu.show(viewByRaidComboBox, 0, viewByRaidComboBox.getSize().height);
        } else
        {
            comboPopupMenu.setVisible(false);
        }
    }

    private void setComboSelection(String name)
    {
        Vector<String> items = new Vector<>();

        ComparisonViewPanel.addComboItems(name, items, comboStrictData, viewByRaidComboBox);
    }

    private JMenuItem createMenuItemTableHeader(final String name)
    {
        JMenuItem item = new JMenuItem(name);
        item.setBackground(Color.BLACK);
        item.setOpaque(true);
        item.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent event)
            {
                getUpdatedPopupMenu(name);
            }
        });
        return item;
    }

    private JMenuItem createMenuItem(final String name)
    {
        JMenuItem item = new JMenuItem(name);
        item.setBackground(Color.BLACK);
        item.setOpaque(true);
        item.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent event)
            {
                setComboSelection(name);
            }
        });
        return item;
    }


    private final Map<String, String[]> comboPopupData = new LinkedHashMap<String, String[]>();


    public  JPanel getOverallPanel(String title, Map<String, JLabel> labelMap)
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));

        JPanel subPanel = new JPanel();
        subPanel.setLayout(new GridLayout(7, 2));

        for(String s : rooms)
        {
            JLabel leftLabel = new JLabel(roomColor + s);
            subPanel.add(leftLabel);
            subPanel.add(labelMap.get(s));
        }
        panel.add(subPanel);
        return panel;
    }

    public void clearData()
    {
        currentData.clear();
        comparisons.clear();
        close();
    }

    public void createFrame(ArrayList<RoomData> data)
    {
        comboPopupData.put("Room Times", DataPoint.getRoomTimes());
        comboPopupData.put("Maiden", DataPoint.getMaidenNames());
        comboPopupData.put("Bloat", DataPoint.getBloatNames());
        comboPopupData.put("Nylocas", DataPoint.getNyloNames());
        comboPopupData.put("Sotetseg", DataPoint.getSoteNames());
        comboPopupData.put("Xarpus", DataPoint.getXarpNames());
        comboPopupData.put("Verzik", DataPoint.getVerzikNames());
        comboPopupData.put("Any", DataPoint.getAnyRoomNames());
        comboPopupMenu = new JPopupMenu();
        comboPopupMenu.setBorder(new MatteBorder(1, 1, 1, 1, Color.DARK_GRAY));

        List<String> allComboValues = new ArrayList<String>(comboPopupData.keySet());

        comboStrictData = new ArrayList<String>();

        for (String category : allComboValues)
        {
            JMenu menu = new JMenu(category);
            menu.setBackground(Color.BLACK);
            menu.setOpaque(true);
            if (!category.equals("Room Times") && !category.equals("Any"))
            {
                JMenu timeMenu = new JMenu("Time");
                timeMenu.setBackground(Color.BLACK);
                timeMenu.setOpaque(true);
                for (String itemName : DataPoint.filterTimes(comboPopupData.get(category)))
                {
                    timeMenu.add(createMenuItem(itemName));
                    comboStrictData.add(itemName);
                }
                JMenu countMenu = new JMenu("Misc");
                countMenu.setBackground(Color.BLACK);
                countMenu.setOpaque(true);
                for (String itemName : DataPoint.filterInt(comboPopupData.get(category)))
                {
                    countMenu.add(createMenuItem(itemName));
                    comboStrictData.add(itemName);
                }
                JMenu thrallMenu = new JMenu("Thrall");
                thrallMenu.setBackground(Color.BLACK);
                thrallMenu.setOpaque(true);
                for (String itemName : DataPoint.filterThrall(comboPopupData.get(category)))
                {
                    thrallMenu.add(createMenuItem(itemName));
                    comboStrictData.add(itemName);
                }
                JMenu vengMenu = new JMenu("Veng");
                vengMenu.setBackground(Color.BLACK);
                vengMenu.setOpaque(true);
                for (String itemName : DataPoint.filterVeng(comboPopupData.get(category)))
                {
                    vengMenu.add(createMenuItem(itemName));
                    comboStrictData.add(itemName);
                }

                JMenu specMenu = new JMenu("Spec");
                specMenu.setBackground(Color.BLACK);
                specMenu.setOpaque(true);
                for (String itemName : DataPoint.filterSpecs(comboPopupData.get(category)))
                {
                    specMenu.add(createMenuItem(itemName));
                    comboStrictData.add(itemName);
                }

                menu.add(timeMenu);
                menu.add(countMenu);
                menu.add(thrallMenu);
                menu.add(vengMenu);
                menu.add(specMenu);
            } else
            {
                for (String itemName : comboPopupData.get(category))
                {
                    menu.add(createMenuItem(itemName));
                    comboStrictData.add(itemName);
                }
            }
            comboPopupMenu.add(menu);
        }
        JMenu playerSpecificMenu = new JMenu("Player Specific");
        playerSpecificMenu.setBackground(Color.BLACK);
        playerSpecificMenu.setOpaque(true);
        String[] qualifiers = new String[]{"Maiden", "Bloat", "Nylo", "Sote", "Xarp", "Verz", "deaths"};

        for (String s : qualifiers)
        {
            JMenu room = new JMenu(s);
            room.setBackground(Color.BLACK);
            room.setOpaque(true);
            for (String qualified : DataPoint.getPlayerSpecific())
            {
                if (qualified.contains(s))
                {
                    room.add(createMenuItem("Player: " + qualified));
                    comboStrictData.add("Player: " + qualified);
                }
            }
            playerSpecificMenu.add(room);
        }
        JMenu room = new JMenu("Other");
        room.setBackground(Color.BLACK);
        room.setOpaque(true);
        for (String qualified : DataPoint.getPlayerSpecific())
        {
            boolean anyFlagged = false;
            for (String s : qualifiers)
            {
                if (qualified.contains(s))
                {
                    anyFlagged = true;
                }
            }
            if (!anyFlagged)
            {
                room.add(createMenuItem("Player: " + qualified));
                comboStrictData.add("Player: " + qualified);
            }
        }
        playerSpecificMenu.add(room);

        comboPopupMenu.add(playerSpecificMenu);

        viewByRaidComboBox = new JComboBox<>();
        viewByRaidComboBox.setEditable(true);
        viewByRaidComboBox.setPrototypeDisplayValue("Challenge Time");
        viewByRaidComboBox.setSelectedItem("Challenge Time");
        viewByRaidComboBox.setEditable(false);
        for (Component comp : viewByRaidComboBox.getComponents())
        {
            if (comp instanceof AbstractButton)
            {
                arrowButton = (AbstractButton) comp;
                arrowButton.setBackground(Color.BLACK);
            }
        }

        arrowButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                setPopupVisible(!comboPopupMenu.isVisible());
            }
        });

        viewByRaidComboBox.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                setPopupVisible(!comboPopupMenu.isVisible());
            }
        });

        timeFollowsTab = new JCheckBox("Time Follows Tab");
        timeFollowsTab.setSelected(true);

        for (int i = 0; i < data.size(); i++)
        {
            data.get(i).index = i;
        }

        int completions = 0;
        currentData = data;
        setTitle("Raids");

        JPopupMenu tstMenu = getjPopupMenu();


        table = new JTable();
        table.getTableHeader().setComponentPopupMenu(tstMenu);
        JScrollPane pane = new JScrollPane(table);

        JPanel tablePanel = getTitledPanel("Raids");
        tablePanel.setLayout(new BorderLayout());
        tablePanel.add(pane);


        container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        tabbedPane.addChangeListener(e ->
        {
            if (timeFollowsTab.isSelected())
            {
                if (built)
                {
                    viewByRaidComboBox.setEditable(true);
                    switch (tabbedPane.getSelectedIndex())
                    {
                        case 0:
                            viewByRaidComboBox.setSelectedItem("Challenge Time");
                            break;
                        case 1:
                            viewByRaidComboBox.setSelectedItem("Maiden Time");
                            break;
                        case 2:
                            viewByRaidComboBox.setSelectedItem("Bloat Time");
                            break;
                        case 3:
                            viewByRaidComboBox.setSelectedItem("Nylo Time");
                            break;
                        case 4:
                            viewByRaidComboBox.setSelectedItem("Sote Time");
                            break;
                        case 5:
                            viewByRaidComboBox.setSelectedItem("Xarp Time");
                            break;
                        case 6:
                            viewByRaidComboBox.setSelectedItem("Verzik Time");
                            break;

                    }
                    viewByRaidComboBox.setEditable(false);
                    updateTable();
                }
            }
        });

        JComponent overallPanel = new JPanel();
        tabbedPane.addTab("Overall", overallPanel);
        overallPanel.setLayout(new GridLayout(2, 3));

        JPanel overallCustomPanel = new JPanel();
        overallCustomPanel.setLayout(new BorderLayout());
        overallCustomPanel.setBorder(BorderFactory.createTitledBorder(""));

        JPanel customSubPanel = new JPanel();
        customSubPanel.setLayout(new GridLayout(1, 4));

        JPanel chooseStatisticPanel = getTitledPanel("Choose Statistic");
        chooseStatisticPanel.setLayout(new GridLayout(1, 1));

        JPanel resultsPanel = getTitledPanel("Results");
        resultsPanel.setLayout(new GridLayout(5, 2));

        JPanel tableOptionsPanel = getTitledPanel("Table Options");

        JPanel viewRaidByPanel = getTitledPanel("View Raid By");

        sortOptionsBox = new JComboBox(new String[]
                {
                        "Date",
                        "Value",
                        "Scale"
                }
        );

        sortOrderBox = new JComboBox(new String[]
                {
                        "Ascending",
                        "Descending"
                });

        statisticsBox = new JComboBox(DataPoint.getByNames());


        statisticsBox.addActionListener(
                al ->
                {
                    updateTable();
                });

        sortOptionsBox.addActionListener(
                al ->
                {
                    updateTable();
                }
        );

        sortOrderBox.addActionListener(
                al ->
                {
                    updateTable();
                }
        );

        JLabel textCustomAverageLabel = new JLabel("Average:", SwingConstants.LEFT);
        JLabel textCustomMedianLabel = new JLabel("Median:", SwingConstants.LEFT);
        JLabel textCustomModeLabel = new JLabel("Mode:", SwingConstants.LEFT);
        JLabel textCustomMinLabel = new JLabel("Minimum:", SwingConstants.LEFT);
        JLabel textCustomMaxLabel = new JLabel("Maximum:", SwingConstants.LEFT);

        resultsPanel.add(textCustomAverageLabel);
        resultsPanel.add(customAverageLabel);

        resultsPanel.add(textCustomMedianLabel);
        resultsPanel.add(customMedianLabel);

        resultsPanel.add(textCustomModeLabel);
        resultsPanel.add(customModeLabel);

        resultsPanel.add(textCustomMinLabel);
        resultsPanel.add(customMinLabel);

        resultsPanel.add(textCustomMaxLabel);
        resultsPanel.add(customMaxLabel);

        chooseStatisticPanel.add(statisticsBox);

        JButton undoFilter = new JButton("Clear manual filter");
        undoFilter.addActionListener(al ->
        {
            filteredIndices.clear();
            updateTable();
        });

        tableOptionsPanel.add(sortOptionsBox);
        tableOptionsPanel.add(sortOrderBox);
        tableOptionsPanel.add(undoFilter);
        JPanel buttonLine = new JPanel();
        buttonLine.setLayout(new GridLayout(1, 2));
        buttonLine.add(new JLabel("Config"));

        viewByRaidComboBox.addActionListener(
                al ->
                {
                    updateTable();
                });

        viewRaidByPanel.add(viewByRaidComboBox);

        viewRaidByPanel.add(timeFollowsTab);

        viewRaidByPanel.add(raidsFoundLabel);
        viewRaidByPanel.add(completionsFound);
        raidsFoundLabel.setText("Raids found: " + data.size());
        completionsFound.setText("Completions found: " + completions);

        customSubPanel.add(chooseStatisticPanel);
        customSubPanel.add(resultsPanel);
        customSubPanel.add(tableOptionsPanel);
        customSubPanel.add(viewRaidByPanel);

        overallCustomPanel.add(customSubPanel);

        JPanel overallAveragePanel = getOverallPanel("Average", averageLabels);
        JPanel overallMedianPanel = getOverallPanel("Median", medianLabels);
        JPanel overallMinPanel = getOverallPanel("Minimum", minLabels);
        JPanel overallMaxPanel = getOverallPanel("Maximum",maxLabels);

        JPanel topStatPanel = new JPanel();
        topStatPanel.setLayout(new GridLayout(1, 4));

        topStatPanel.add(overallAveragePanel);
        topStatPanel.add(overallMedianPanel);
        topStatPanel.add(overallMinPanel);
        topStatPanel.add(overallMaxPanel);

        overallPanel.add(topStatPanel);
        overallPanel.add(overallCustomPanel);

        maidenTab = new StatisticTab(data, DataPoint.rooms.MAIDEN);
        tabbedPane.addTab("Maiden", maidenTab);
        bloatTab = new StatisticTab(data, DataPoint.rooms.BLOAT);
        tabbedPane.addTab("Bloat", bloatTab);
        nyloTab = new StatisticTab(data, DataPoint.rooms.NYLOCAS);
        tabbedPane.addTab("Nylo", nyloTab);
        soteTab = new StatisticTab(data, DataPoint.rooms.SOTETSEG);
        tabbedPane.addTab("Sotetseg", soteTab);
        xarpTab = new StatisticTab(data, DataPoint.rooms.XARPUS);
        tabbedPane.addTab("Xarpus", xarpTab);
        verzikTab = new StatisticTab(data, DataPoint.rooms.VERZIK);
        tabbedPane.addTab("Verzik", verzikTab);

        tabbedPane.setMinimumSize(new Dimension(100, 300));

        JPanel additionalFiltersPanel = getTitledPanel("Quick Filters");
        additionalFiltersPanel.setLayout(new BorderLayout());
        additionalFiltersPanel.setMinimumSize(new Dimension(200, 300));
        additionalFiltersPanel.setPreferredSize(new Dimension(200, 300));

        filterSpectateOnly = getActionListenCheckBox("Spectate Only", al->{updateTable();});
        filterInRaidOnly = getActionListenCheckBox("In Raid Only", al->{updateTable();});
        filterCompletionOnly = getActionListenCheckBox("Completion Only", al->{updateTable();});
        filterWipeResetOnly = getActionListenCheckBox("Wipe/Reset Only", al->{updateTable();});
        filterComboBoxScale = UISwingUtility.getActionListenCheckBox(new String[]{"Solo", "Duo", "Trio", "4-Man", "5-Man"}, al->{updateTable();});
        filterCheckBoxScale = getActionListenCheckBox("Scale", al -> {updateTable();});
        filterTodayOnly = getActionListenCheckBox("Today Only", al -> {updateTable();});
        filterPartyOnly = getActionListenCheckBox("Party Only", al -> {updateTable();});
        filterPartialData = getActionListenCheckBox("Filter Partial Raids", al -> {updateTable();});
        filterPartialOnly = getActionListenCheckBox("Filter Partial Rooms", al -> {updateTable();});
        filterPartialData.setToolTipText("Removes data sets that have any rooms that were partially completed");
        filterNormalOnly = getActionListenCheckBox("Normal Mode Only", true, al->{updateTable();});

        JPanel scaleContainer = new JPanel();
        scaleContainer.setLayout(new BoxLayout(scaleContainer, BoxLayout.X_AXIS));

        JPanel filterHolder = new JPanel();
        filterHolder.setLayout(new GridLayout(10, 1));
        filterHolder.add(filterSpectateOnly);
        filterHolder.add(filterInRaidOnly);
        filterHolder.add(filterCompletionOnly);
        filterHolder.add(filterWipeResetOnly);
        filterHolder.add(filterTodayOnly);
        filterHolder.add(filterPartyOnly);
        filterHolder.add(filterPartialData);
        filterHolder.add(filterPartialOnly);
        filterHolder.add(filterNormalOnly);
        scaleContainer.add(filterCheckBoxScale);
        scaleContainer.add(filterComboBoxScale);
        filterHolder.add(scaleContainer);

        additionalFiltersPanel.add(filterHolder);

        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.X_AXIS));

        topContainer.setPreferredSize(new Dimension(800, 300));
        topContainer.add(tabbedPane);
        topContainer.add(additionalFiltersPanel);

        setLabels(data);
        updateTable();

        container.setPreferredSize(new Dimension(800, 700));

        container.add(topContainer);
        container.add(tablePanel);

        JPanel splitLeftRight = new JPanel();
        splitLeftRight.setLayout(new BoxLayout(splitLeftRight, BoxLayout.X_AXIS));
        splitLeftRight.add(container);

        JPanel rightContainer = new JPanel();
        rightContainer.setPreferredSize(new Dimension(400, 700));
        rightContainer.setLayout(new BoxLayout(rightContainer, BoxLayout.Y_AXIS));

        JPanel rightTopContainer = getTitledPanel("Advanced Filters");
        rightTopContainer.setLayout(new GridLayout(3, 2));

        JPanel filterTimePanel = getTitledPanel("Filter by room or split time");
        filterTimePanel.setLayout(new BoxLayout(filterTimePanel, BoxLayout.Y_AXIS));

        JPanel filterPlayerPanel = getTitledPanel("Filter by players in a raid");
        filterPlayerPanel.setLayout(new GridLayout(2, 2));

        JPanel filterDatePanel = getTitledPanel("Filter by date");
        filterDatePanel.setLayout(new GridLayout(2, 2));

        JPanel filterOtherIntPanel = getTitledPanel("Filter by other condition (int)");
        filterOtherIntPanel.setLayout(new GridLayout(2, 2));

        JPanel filterOtherBoolPanel = getTitledPanel("Filter by other condition (bool)");
        filterOtherBoolPanel.setLayout(new GridLayout(2, 2));


        timeFilterChoice = new JComboBox<String>(DataPoint.getTimeNames());

        String[] timeOperatorChoices =
                {
                        "=",
                        "<",
                        ">",
                        "<=",
                        ">="
                };

        timeFilterOperator = new JComboBox<String>(timeOperatorChoices);


        timeFilterValue = new JTextField();

        JButton timeFilterAdd = new JButton("Add");
        timeFilterAdd.addActionListener(
                al ->
                {
                    String time = timeFilterValue.getText();
                    if (time.equals(""))
                    {
                        return;
                    }
                    String timeStr = timeFilterChoice.getSelectedItem().toString() + " " + timeFilterOperator.getSelectedItem().toString() + " " + time;
                    activeFilters.add(new ImplicitFilter(new FilterTime(DataPoint.getValue(String.valueOf(timeFilterChoice.getSelectedItem())), timeFilterOperator.getSelectedIndex(), getTimeFromString(time), timeStr)));
                    updateFilterTable();
                });
        timeFilterAdd.setPreferredSize(new Dimension(55, timeFilterAdd.getPreferredSize().height));
        timeFilterOperator.setPreferredSize(new Dimension(50, timeFilterAdd.getPreferredSize().height));
        timeFilterOperator.setMaximumSize(new Dimension(Integer.MAX_VALUE, timeFilterAdd.getPreferredSize().height));
        timeFilterChoice.setMaximumSize(new Dimension(Integer.MAX_VALUE, timeFilterAdd.getPreferredSize().height));
        timeFilterValue.setMaximumSize(new Dimension(Integer.MAX_VALUE, timeFilterAdd.getPreferredSize().height));
        timeFilterValue.setPreferredSize(new Dimension(75, timeFilterAdd.getPreferredSize().height));


        JPanel filterTimePanelTop = new JPanel();
        filterTimePanelTop.setLayout(new BoxLayout(filterTimePanelTop, BoxLayout.X_AXIS));
        filterTimePanelTop.add(timeFilterChoice);

        JPanel filterTimePanelBottom = new JPanel();
        filterTimePanelBottom.setLayout(new BoxLayout(filterTimePanelBottom, BoxLayout.X_AXIS));
        filterTimePanelBottom.add(timeFilterOperator);
        filterTimePanelBottom.add(Box.createRigidArea(new Dimension(2, 2)));
        filterTimePanelBottom.add(timeFilterValue);
        filterTimePanelBottom.add(Box.createRigidArea(new Dimension(2, 2)));
        filterTimePanelBottom.add(timeFilterAdd);
        filterTimePanel.add(filterTimePanelTop);
        filterTimePanel.add(Box.createRigidArea(new Dimension(5, 5)));
        filterTimePanel.add(filterTimePanelBottom);

        String[] playersQualifier = {
                "contains exactly",
                "includes",
                "includes any of",
                "excludes",
                "excludes all of"
        };

        playerFilterOperator = new JComboBox<String>(playersQualifier);
        playerFilterValue = new JTextField();
        JButton playerFilterAdd = new JButton("Add");
        playerFilterValue.setMaximumSize(new Dimension(Integer.MAX_VALUE, playerFilterAdd.getPreferredSize().height));
        playerFilterValue.setPreferredSize(new Dimension(75, playerFilterAdd.getPreferredSize().height));
        playerFilterOperator.setMaximumSize(new Dimension(Integer.MAX_VALUE, playerFilterAdd.getPreferredSize().height));
        playerFilterAdd.setPreferredSize(new Dimension(55, playerFilterAdd.getPreferredSize().height));

        playerFilterAdd.addActionListener(
                al ->
                {
                    String filterStr = "Raid " + playerFilterOperator.getSelectedItem().toString() + " " + playerFilterValue.getText();
                    activeFilters.add(new ImplicitFilter(new FilterPlayers(playerFilterValue.getText(), playerFilterOperator.getSelectedIndex(), filterStr)));
                    updateFilterTable();
                });

        JPanel filterPlayerPanelTop = new JPanel();
        filterPlayerPanelTop.setLayout(new BoxLayout(filterPlayerPanelTop, BoxLayout.X_AXIS));
        JPanel filterPlayerPanelBottom = new JPanel();
        filterPlayerPanelBottom.setLayout(new BoxLayout(filterPlayerPanelBottom, BoxLayout.X_AXIS));

        filterPlayerPanel.setLayout(new BoxLayout(filterPlayerPanel, BoxLayout.Y_AXIS));

        filterPlayerPanelTop.add(playerFilterOperator);
        filterPlayerPanelBottom.add(playerFilterValue);
        filterPlayerPanelBottom.add(Box.createRigidArea(new Dimension(2, 2)));
        filterPlayerPanelBottom.add(playerFilterAdd);

        filterPlayerPanel.add(filterPlayerPanelTop);
        filterPlayerPanel.add(Box.createRigidArea(new Dimension(5, 5)));
        filterPlayerPanel.add(filterPlayerPanelBottom);

        String[] choicesDate =
                {
                        "on",
                        "before",
                        "after"
                };

        dateFilterOperator = new JComboBox<String>(choicesDate);
        dateFilterValue = new JTextField();
        dateFilterOperator.setMaximumSize(new Dimension(Integer.MAX_VALUE, dateFilterValue.getPreferredSize().height));


        JButton dateFilterAdd = new JButton("Add");
        dateFilterAdd.addActionListener(
                al ->
                {
                    try
                    {
                        String dateString = dateTextField.getText();
                        String[] datePartial = dateString.split("/");
                        int year = Integer.parseInt(datePartial[0]);
                        int month = Integer.parseInt(datePartial[1]);
                        int day = Integer.parseInt(datePartial[2]);
                        Date date = new GregorianCalendar(year, month - 1, day).getTime();
                        String filterStr = "Raid was " + dateFilterOperator.getSelectedItem().toString() + " " + date.toString();
                        activeFilters.add(new ImplicitFilter(new FilterDate(date, dateFilterOperator.getSelectedIndex(), filterStr)));
                    } catch (Exception e)
                    {

                    }
                    updateFilterTable();
                });

        dateFilterValue.setMaximumSize(new Dimension(Integer.MAX_VALUE, dateFilterAdd.getPreferredSize().height));
        dateFilterValue.setPreferredSize(new Dimension(90, dateFilterAdd.getPreferredSize().height));

        dateFilterAdd.setMaximumSize(new Dimension(Integer.MAX_VALUE, dateFilterAdd.getPreferredSize().height));
        dateFilterAdd.setPreferredSize(new Dimension(55, dateFilterAdd.getPreferredSize().height));

        JPanel dateTopRow = new JPanel();
        dateTopRow.setLayout(new BoxLayout(dateTopRow, BoxLayout.X_AXIS));

        JPanel dateBottomRow = new JPanel();
        dateBottomRow.setLayout(new BoxLayout(dateBottomRow, BoxLayout.X_AXIS));

        dateTopRow.add(dateFilterOperator);
        dateTopRow.add(Box.createRigidArea(new Dimension(2, 2)));
        dateTopRow.add(dateFilterAdd);
        dateTextField = new JTextField();
        dateBottomRow.add(dateTextField);
        dateBottomRow.add(Box.createRigidArea(new Dimension(5, 5)));
        dateBottomRow.add(new JLabel("YYYY/MM/DD"));
        filterDatePanel.setLayout(new BoxLayout(filterDatePanel, BoxLayout.Y_AXIS));
        filterDatePanel.add(dateTopRow);
        filterDatePanel.add(Box.createRigidArea(new Dimension(5, 5)));
        filterDatePanel.add(dateBottomRow);


        String[] otherIntOperatorChoices = {
                "=",
                "<",
                ">",
                "<=",
                ">="
        };


        otherIntFilterChoice = new JComboBox<String>(DataPoint.getOtherIntNames());
        otherIntFilterOperator = new JComboBox<String>(otherIntOperatorChoices);
        otherIntFilterValue = new JTextField();

        JButton otherIntAdd = new JButton("Add");
        otherIntAdd.addActionListener(
                al ->
                {
                    String filterStr = otherIntFilterChoice.getSelectedItem().toString() + " " + otherIntFilterOperator.getSelectedItem().toString() + " " + otherIntFilterValue.getText() + " ";
                    activeFilters.add(new ImplicitFilter(new FilterOtherInt(DataPoint.getValue(String.valueOf(otherIntFilterChoice.getSelectedItem())), otherIntFilterOperator.getSelectedIndex(), Integer.parseInt(otherIntFilterValue.getText()), filterStr)));
                    updateFilterTable();
                }
        );

        otherIntFilterChoice.setMaximumSize(new Dimension(Integer.MAX_VALUE, otherIntAdd.getPreferredSize().height));

        otherIntFilterValue.setMaximumSize(new Dimension(Integer.MAX_VALUE, otherIntAdd.getPreferredSize().height));
        otherIntFilterValue.setPreferredSize(new Dimension(70, otherIntAdd.getPreferredSize().height));

        otherIntAdd.setMaximumSize(new Dimension(Integer.MAX_VALUE, otherIntAdd.getPreferredSize().height));
        otherIntAdd.setPreferredSize(new Dimension(55, otherIntAdd.getPreferredSize().height));

        otherIntFilterOperator.setMaximumSize(new Dimension(Integer.MAX_VALUE, otherIntAdd.getPreferredSize().height));
        otherIntFilterOperator.setPreferredSize(new Dimension(50, otherIntAdd.getPreferredSize().height));


        JPanel otherIntTop = new JPanel();
        otherIntTop.setLayout(new BoxLayout(otherIntTop, BoxLayout.X_AXIS));
        JPanel otherIntBottom = new JPanel();
        otherIntBottom.setLayout(new BoxLayout(otherIntBottom, BoxLayout.X_AXIS));

        otherIntTop.add(otherIntFilterChoice);
        otherIntBottom.add(otherIntFilterOperator);
        otherIntBottom.add(Box.createRigidArea(new Dimension(2, 2)));
        otherIntBottom.add(otherIntFilterValue);
        otherIntBottom.add(Box.createRigidArea(new Dimension(2, 2)));
        otherIntBottom.add(otherIntAdd);

        filterOtherIntPanel.setLayout(new BoxLayout(filterOtherIntPanel, BoxLayout.Y_AXIS));
        filterOtherIntPanel.add(otherIntTop);
        filterOtherIntPanel.add(Box.createRigidArea(new Dimension(5, 5)));
        filterOtherIntPanel.add(otherIntBottom);

        String[] choicesOtherBool = {
                "Maiden skip successful",
                "Reset after maiden",
                "Maiden wipe",
                "Reset after bloat",
                "Bloat wipe",
                "Reset after nylo",
                "Nylo wipe",
                "Reset after sote",
                "Sote wipe",
                "Reset after xarp",
                "Xarp wipe",
                "Verzik  wipe"
        };

        String[] qualifierOtherBool = {
                "True",
                "False"
        };

        otherBoolFilterChoice = new JComboBox<String>(choicesOtherBool);
        otherBoolFilterOperator = new JComboBox<String>(qualifierOtherBool);


        JButton otherBoolAdd = new JButton("Add Filter");
        otherBoolAdd.addActionListener(
                al ->
                {
                    String filterStr = otherBoolFilterChoice.getSelectedItem().toString() + " " + otherBoolFilterOperator.getSelectedItem().toString();
                    activeFilters.add(new ImplicitFilter(new FilterOtherBool(otherBoolFilterChoice.getSelectedIndex(), otherBoolFilterOperator.getSelectedIndex() == 0, filterStr)));
                    updateFilterTable();
                }
        );

        otherBoolFilterOperator.setMaximumSize(new Dimension(Integer.MAX_VALUE, otherBoolAdd.getPreferredSize().height));
        otherBoolFilterChoice.setMaximumSize(new Dimension(Integer.MAX_VALUE, otherBoolAdd.getPreferredSize().height));

        JPanel filterBoolTop = new JPanel();
        filterBoolTop.setLayout(new BoxLayout(filterBoolTop, BoxLayout.X_AXIS));
        JPanel filterBoolBottom = new JPanel();
        filterBoolBottom.setLayout(new BoxLayout(filterBoolBottom, BoxLayout.X_AXIS));

        filterBoolTop.add(otherBoolFilterChoice);
        filterBoolBottom.add(otherBoolFilterOperator);
        filterBoolBottom.add(Box.createRigidArea(new Dimension(2, 2)));
        filterBoolBottom.add(otherBoolAdd);

        filterOtherBoolPanel.setLayout(new BoxLayout(filterOtherBoolPanel, BoxLayout.Y_AXIS));

        filterOtherBoolPanel.add(filterBoolTop);
        filterOtherBoolPanel.add(Box.createRigidArea(new Dimension(5, 5)));
        filterOtherBoolPanel.add(filterBoolBottom);

        JPanel filterOptions = getTitledPanel("Filter Options");
        rightTopContainer.setPreferredSize(new Dimension(400, 250));
        rightTopContainer.add(filterTimePanel);
        rightTopContainer.add(filterOtherIntPanel);
        rightTopContainer.add(filterPlayerPanel);
        rightTopContainer.add(filterOtherBoolPanel);
        rightTopContainer.add(filterDatePanel);


        JPanel rightBottomContainer = getTitledPanel("Active Filters");
        rightBottomContainer.setPreferredSize(new Dimension(400, 200));

        filterTableContainer = new JPanel();

        JPopupMenu raidPopup = new JPopupMenu();

        JMenuItem analyzeSessions = new JMenuItem("Analyze Sessions");
        analyzeSessions.setBackground(Color.BLACK);
        analyzeSessions.setOpaque(true);
        analyzeSessions.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                updateAliases();
                ArrayList<RoomData> rows = new ArrayList<>();
                int[] toRemove = table.getSelectedRows();
                for (int i = 0; i < toRemove.length; i++)
                {
                    rows.add(currentData.get(Integer.parseInt(table.getModel().getValueAt(toRemove[i], 0).toString())));
                }
                Map<Integer, Map<String, ArrayList<RoomData>>> sessions = new LinkedHashMap<>();
                for (RoomData data : rows)
                {
                    if (!sessions.containsKey(data.players.size()))
                    {
                        Map<String, ArrayList<RoomData>> scale = new LinkedHashMap<>();
                        ArrayList<RoomData> list = new ArrayList<>();
                        list.add(data);
                        scale.put(data.getPlayerList(aliases), list);
                        sessions.put(data.players.size(), scale);
                    } else
                    {
                        if (!sessions.get(data.players.size()).containsKey(data.getPlayerList(aliases)))
                        {
                            ArrayList<RoomData> list = new ArrayList<>();
                            list.add(data);
                            sessions.get(data.players.size()).put(data.getPlayerList(aliases), list);
                        } else
                        {
                            sessions.get(data.players.size()).get(data.getPlayerList(aliases)).add(data);
                        }
                    }
                }
                ArrayList<ArrayList<String>> labelSets = new ArrayList<>();
                Map<Integer, ArrayList<ArrayList<RoomData>>> dataSets = new LinkedHashMap<>();
                for (Integer scale : sessions.keySet())
                {
                    ArrayList<ArrayList<RoomData>> scaleData = new ArrayList<>();
                    ArrayList<String> labels = new ArrayList<>();
                    for (String playerList : sessions.get(scale).keySet())
                    {
                        scaleData.add(sessions.get(scale).get(playerList));
                        labels.add(playerList);
                    }
                    dataSets.put(scale, scaleData);
                    labelSets.add(labels);
                }
                ComparisonViewFrame graphView = new ComparisonViewFrame(dataSets, labelSets, config, itemManager, clientThread);
                graphView.open();
            }
        });

        JMenuItem summarizeSession = new JMenuItem("Summarize Session");
        summarizeSession.setBackground(Color.BLACK);
        summarizeSession.setOpaque(true);

        summarizeSession.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                ArrayList<RoomData> rows = new ArrayList<>();
                int[] toRemove = table.getSelectedRows();
                for (int i = 0; i < toRemove.length; i++)
                {
                    rows.add(currentData.get(Integer.parseInt(table.getModel().getValueAt(toRemove[i], 0).toString())));
                }
                new SummarizeRaids(rows);
            }
        });

        JMenuItem addToComparison = new JMenuItem("Add set to comparison");
        addToComparison.setBackground(Color.BLACK);
        addToComparison.setOpaque(true);

        JMenuItem viewGraphs = new JMenuItem("View Graphs");
        viewGraphs.setBackground(Color.BLACK);
        viewGraphs.setOpaque(true);


        JMenuItem viewCharts = new JMenuItem("View Charts");
        viewCharts.setBackground(Color.BLACK);
        viewCharts.setOpaque(true);

        viewCharts.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                ArrayList<RoomData> rows = new ArrayList<>();
                int[] toRemove = table.getSelectedRows();
                for (int i = 0; i < toRemove.length; i++)
                {
                    rows.add(currentData.get(Integer.parseInt(table.getModel().getValueAt(toRemove[i], 0).toString())));
                }
                ChartFrame roomCharts = new ChartFrame(rows, config, itemManager, clientThread);
                roomCharts.open();
            }
        });

        viewGraphs.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                ArrayList<String> labels = new ArrayList<>();
                ArrayList<RoomData> rows = new ArrayList<>();
                int[] toRemove = table.getSelectedRows();
                for (int i = 0; i < toRemove.length; i++)
                {
                    rows.add(currentData.get(Integer.parseInt(table.getModel().getValueAt(toRemove[i], 0).toString())));
                }
                if (rows.isEmpty())
                {
                    new NoDataPopUp().open();
                } else
                {
                    labels.add("");
                    ArrayList<ArrayList<RoomData>> data = new ArrayList<>();
                    data.add(rows);
                    ComparisonViewFrame graphView = new ComparisonViewFrame(data, labels);
                    graphView.open();
                }
            }
        });

        addToComparison.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                ArrayList<RoomData> rows = new ArrayList<>();
                int[] toRemove = table.getSelectedRows();
                for (int i = 0; i < toRemove.length; i++)
                {
                    rows.add(currentData.get(Integer.parseInt(table.getModel().getValueAt(toRemove[i], 0).toString())));
                }
                comparisons.add(rows);
                updateComparisonTable();
            }
        });
        JMenuItem exportRaids = new JMenuItem("Export Selected Raids to CSV");
        exportRaids.setBackground(Color.BLACK);
        exportRaids.setOpaque(true);
        exportRaids.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                ArrayList<RoomData> rows = new ArrayList<>();
                int[] toRemove = table.getSelectedRows();
                for (int i = 0; i < toRemove.length; i++)
                {
                    rows.add(currentData.get(Integer.parseInt(table.getModel().getValueAt(toRemove[i], 0).toString())));
                }
                new SaveRaids(rows).open();
            }
        });

        JMenuItem filterRaids = new JMenuItem("Filter Selected Raids");
        filterRaids.setBackground(Color.BLACK);
        filterRaids.setOpaque(true);
        filterRaids.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                int[] toRemove = table.getSelectedRows();
                for (int i = 0; i < toRemove.length; i++)
                {
                    filteredIndices.add(Integer.parseInt(table.getModel().getValueAt(toRemove[i], 0).toString()));
                }

                updateTable();
            }
        });

        JMenuItem filterExclusiveRaids = new JMenuItem("Filter All Except Selected Raids");
        filterExclusiveRaids.setBackground(Color.BLACK);
        filterExclusiveRaids.setOpaque(true);
        filterExclusiveRaids.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                int[] toKeep = table.getSelectedRows();
                for (int i = 0; i < table.getRowCount(); i++)
                {
                    boolean found = false;
                    for (int j = 0; j < toKeep.length; j++)
                    {
                        if (i == toKeep[j])
                        {
                            found = true;
                        }
                    }
                    if (!found)
                    {
                        filteredIndices.add(Integer.parseInt(table.getModel().getValueAt(i, 0).toString()));
                    }
                }

                updateTable();
            }
        });

        JMenuItem analyzeCrabs = new JMenuItem("Analyze selection crab leaks");
        analyzeCrabs.setOpaque(true);
        analyzeCrabs.setBackground(Color.BLACK);
        analyzeCrabs.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                ArrayList<ArrayList<StringInt>> crabData = new ArrayList<>();
                int[] toRemove = table.getSelectedRows();
                for (int i = 0; i < toRemove.length; i++)
                {
                    crabData.add(currentData.get(Integer.parseInt(table.getModel().getValueAt(toRemove[i], 0).toString())).maidenCrabs);
                }
                new CrabLeakInfo(crabData);
            }
        });

        raidPopup.add(analyzeCrabs);
        raidPopup.add(exportRaids);
        raidPopup.add(addToComparison);
        raidPopup.add(filterRaids);
        raidPopup.add(filterExclusiveRaids);
        raidPopup.add(analyzeSessions);
        raidPopup.add(viewCharts);
        raidPopup.add(viewGraphs);
        raidPopup.add(summarizeSession);
        table.setComponentPopupMenu(raidPopup);

        filterTable = new JTable();
        filterTable.setPreferredSize(new Dimension(380, 135));
        JScrollPane tableScrollView = new JScrollPane(filterTable);
        tableScrollView.setPreferredSize(new Dimension(380, 140));
        updateFilterTable();
        filterTableContainer.add(tableScrollView);

        rightBottomContainer.add(filterTableContainer);

        JButton saveFiltersButton = new JButton("Save");
        saveFiltersButton.addActionListener(
                al ->
                {
                    ArrayList<String> quickFiltersState = new ArrayList<>();
                    quickFiltersState.add("QF-Spectate Only:"+filterSpectateOnly.isSelected());
                    quickFiltersState.add("QF-In Raid Only:"+filterInRaidOnly.isSelected());
                    quickFiltersState.add("QF-Completion Only:"+filterCompletionOnly.isSelected());
                    quickFiltersState.add("QF-Wipe/Reset Only:"+filterWipeResetOnly.isSelected());
                    quickFiltersState.add("QF-Today Only:"+filterTodayOnly.isSelected());
                    quickFiltersState.add("QF-Party Only:"+filterPartyOnly.isSelected());
                    quickFiltersState.add("QF-Partial Raids:"+filterPartialData.isSelected());
                    quickFiltersState.add("QF-Partial Rooms:"+filterPartialOnly.isSelected());
                    quickFiltersState.add("QF-Normal Mode Only:"+filterNormalOnly.isSelected());
                    quickFiltersState.add("QF-Scale:"+filterCheckBoxScale.isSelected()+":"+filterComboBoxScale.getSelectedIndex());
                    quickFiltersState.add("QF-View Raid By:"+viewByRaidComboBox.getItemAt(viewByRaidComboBox.getSelectedIndex()));
                    quickFiltersState.add("QF-Table Sort By:"+sortOptionsBox.getItemAt(sortOptionsBox.getSelectedIndex()));
                    quickFiltersState.add("QF-Table Sort:"+sortOrderBox.getItemAt(sortOrderBox.getSelectedIndex()));
                    SaveFilter saveFilter = new SaveFilter(activeFilters, quickFiltersState);
                    saveFilter.open();
                });
        JButton loadFiltersButton = new JButton("Load");
        loadFiltersButton.addActionListener(
                al ->
                {
                    new LoadFilter(this).open();
                });
        JButton clearFiltersButton = new JButton("Clear");
        clearFiltersButton.addActionListener(
                al ->
                {
                    activeFilters.clear();
                    updateFilterTable();
                });
        GridLayout layout = new GridLayout(2, 2);
        layout.setHgap(2);
        layout.setVgap(2);
        filterOptions.setLayout(layout);

        filterOptions.add(saveFiltersButton);
        filterOptions.add(loadFiltersButton);
        filterOptions.add(clearFiltersButton);
        rightTopContainer.add(filterOptions);

        JPanel rightBottomBottomContainer = getTitledPanel("Comparison Options");
        rightBottomBottomContainer.setPreferredSize(new Dimension(400, 250));

        comparisonTable = new JTable();
        JScrollPane comparisonTableScroll = new JScrollPane(comparisonTable);
        comparisonTable.setPreferredSize(new Dimension(380, 170));
        comparisonTableScroll.setPreferredSize(new Dimension(380, 155));
        updateComparisonTable();

        JPanel rightBottomMostContainer = getTitledPanel("Alias Options");

        aliasText.setToolTipText("This applies to the tab names when you use the analyze sessions features. Syntax- Name to be displayed:oldname1,oldname2,oldname3");


        aliasText.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override
            public void insertUpdate(DocumentEvent e)
            {
                try
                {
                    if (!writing)
                        DataWriter.writeAliasFile(e.getDocument().getText(0, e.getDocument().getLength()).replaceAll("\n", System.getProperty("line.separator")));
                } catch (BadLocationException ex)
                {
                    ex.printStackTrace();
                }
            }

            @Override
            public void removeUpdate(DocumentEvent e)
            {
                try
                {
                    if (!writing)
                        DataWriter.writeAliasFile(e.getDocument().getText(0, e.getDocument().getLength()).replaceAll("\n", System.getProperty("line.separator")));
                } catch (BadLocationException ex)
                {
                    ex.printStackTrace();
                }
            }

            @Override
            public void changedUpdate(DocumentEvent e)
            {
                try
                {
                    if (!writing)
                        DataWriter.writeAliasFile(e.getDocument().getText(0, e.getDocument().getLength()).replaceAll("\n", System.getProperty("line.separator")));
                } catch (BadLocationException ex)
                {
                    ex.printStackTrace();
                }
            }
        });

        updateAliases();

        JScrollPane aliasScrollPane = new JScrollPane(aliasText);
        aliasScrollPane.setPreferredSize(new Dimension(380, 70));
        rightBottomMostContainer.add(aliasScrollPane);

        rightBottomBottomContainer.add(comparisonTableScroll);
        JButton viewComparisonsButton = new JButton("View Comparisons");
        viewComparisonsButton.addActionListener(al ->
        {
            if (comparisonTable.getModel().getRowCount() == 0)
            {
                new NoDataPopUp().open();
            } else
            {
                ArrayList<String> labels = new ArrayList<>();
                for (int i = 0; i < comparisonTable.getModel().getRowCount(); i++)
                {
                    labels.add(comparisonTable.getModel().getValueAt(i, 1).toString());
                }
                ComparisonViewFrame graphView = new ComparisonViewFrame(comparisons, labels);
                graphView.open();
            }
        });
        rightBottomBottomContainer.add(viewComparisonsButton);

        rightContainer.add(rightTopContainer);
        rightContainer.add(rightBottomContainer);
        rightContainer.add(rightBottomBottomContainer);
        rightContainer.add(rightBottomMostContainer);
        splitLeftRight.add(rightContainer);
        sortOrderBox.setSelectedIndex(1);

        add(splitLeftRight);
        pack();
        built = true;
    }

    public String[] columnHeaderNames = new String[]{"Date", "Time", "Scale", "Status", "Players", "Spectate", "View"};
    public ArrayList<JCheckBoxMenuItem> columnHeaders;

    private void getUpdatedPopupMenu(String newItem)
    {
        JCheckBoxMenuItem item = new JCheckBoxMenuItem(newItem);
        item.setOpaque(true);
        item.setBackground(Color.BLACK);
        item.setState(true);
        item.addActionListener(al ->
        {
            updateTable();
        });
        columnHeaders.add(item);
        table.getTableHeader().setComponentPopupMenu(getjPopupMenu());
        updateTable();
    }

    private JCheckBoxMenuItem getCheckBoxMenuItem(String name)
    {
        JCheckBoxMenuItem item = new JCheckBoxMenuItem(name);
        if (!name.equals("Time"))
        {
            item.setState(true);
        }
        item.setOpaque(true);
        item.setBackground(Color.BLACK);
        item.addActionListener(al ->
        {
            if (built)
            {
                updateTable();
            }
        });
        return item;
    }

    private JPopupMenu getjPopupMenu()
    {
        JPopupMenu baseMenu = new JPopupMenu();

        for (JCheckBoxMenuItem item : columnHeaders)
        {
            baseMenu.add(item);
        }

        List<String> allComboValues = new ArrayList<String>(comboPopupData.keySet());

        comboStrictData = new ArrayList<>();

        JMenu addCustom = new JMenu("Add Custom");

        JMenuItem resetCustom = new JMenuItem("Reset Custom Columns");
        resetCustom.setOpaque(true);
        resetCustom.setBackground(Color.BLACK);

        resetCustom.addActionListener(al ->
        {
            columnHeaders.clear();
            for (String column : columnHeaderNames)
            {
                columnHeaders.add(getCheckBoxMenuItem(column));
            }
            table.getTableHeader().setComponentPopupMenu(getjPopupMenu());
            updateTable();
        });

        for (String category : allComboValues)
        {
            JMenu menu = new JMenu(category);
            menu.setBackground(Color.BLACK);
            menu.setOpaque(true);
            if (!category.equals("Room Times") && !category.equals("Any"))
            {
                JMenu timeMenu = new JMenu("Time");
                timeMenu.setBackground(Color.BLACK);
                timeMenu.setOpaque(true);
                for (String itemName : DataPoint.filterTimes(comboPopupData.get(category)))
                {
                    timeMenu.add(createMenuItemTableHeader(itemName));
                    comboStrictData.add(itemName);
                }
                JMenu countMenu = new JMenu("Misc");
                countMenu.setBackground(Color.BLACK);
                countMenu.setOpaque(true);
                for (String itemName : DataPoint.filterInt(comboPopupData.get(category)))
                {
                    countMenu.add(createMenuItemTableHeader(itemName));
                    comboStrictData.add(itemName);
                }
                JMenu thrallMenu = new JMenu("Thrall");
                thrallMenu.setBackground(Color.BLACK);
                thrallMenu.setOpaque(true);
                for (String itemName : DataPoint.filterThrall(comboPopupData.get(category)))
                {
                    thrallMenu.add(createMenuItemTableHeader(itemName));
                    comboStrictData.add(itemName);
                }
                JMenu vengMenu = new JMenu("Veng");
                vengMenu.setBackground(Color.BLACK);
                vengMenu.setOpaque(true);
                for (String itemName : DataPoint.filterVeng(comboPopupData.get(category)))
                {
                    vengMenu.add(createMenuItemTableHeader(itemName));
                    comboStrictData.add(itemName);
                }

                JMenu specMenu = new JMenu("Spec");
                specMenu.setBackground(Color.BLACK);
                specMenu.setOpaque(true);
                for (String itemName : DataPoint.filterSpecs(comboPopupData.get(category)))
                {
                    specMenu.add(createMenuItemTableHeader(itemName));
                    comboStrictData.add(itemName);
                }

                menu.add(timeMenu);
                menu.add(countMenu);
                menu.add(thrallMenu);
                menu.add(vengMenu);
                menu.add(specMenu);
            } else
            {
                for (String itemName : comboPopupData.get(category))
                {
                    menu.add(createMenuItemTableHeader(itemName));
                    comboStrictData.add(itemName);
                }
            }
            addCustom.add(menu);
        }
        JMenu playerSpecificMenu = new JMenu("Player Specific");
        playerSpecificMenu.setBackground(Color.BLACK);
        playerSpecificMenu.setOpaque(true);
        String[] qualifiers = new String[]{"Maiden", "Bloat", "Nylo", "Sote", "Xarp", "Verz", "deaths"};

        for (String s : qualifiers)
        {
            JMenu room = new JMenu(s);
            room.setBackground(Color.BLACK);
            room.setOpaque(true);
            for (String qualified : DataPoint.getPlayerSpecific())
            {
                if (qualified.contains(s))
                {
                    room.add(createMenuItemTableHeader("Player: " + qualified));
                    comboStrictData.add("Player: " + qualified);
                }
            }
            playerSpecificMenu.add(room);
        }
        JMenu room = new JMenu("Other");
        room.setBackground(Color.BLACK);
        room.setOpaque(true);
        for (String qualified : DataPoint.getPlayerSpecific())
        {
            boolean anyFlagged = false;
            for (String s : qualifiers)
            {
                if (qualified.contains(s))
                {
                    anyFlagged = true;
                }
            }
            if (!anyFlagged)
            {
                room.add(createMenuItemTableHeader("Player: " + qualified));
                comboStrictData.add("Player: " + qualified);
            }
        }
        playerSpecificMenu.add(room);

        addCustom.setOpaque(true);
        addCustom.setBackground(Color.BLACK);
        addCustom.add(playerSpecificMenu);
        baseMenu.add(addCustom);
        baseMenu.add(resetCustom);

        return baseMenu;
    }

    private int getTimeFromString(String text)
    {
        int ticks = 0;
        String sub = text;
        if (sub.contains(":"))
        {
            ticks += 100 * Integer.parseInt(sub.substring(0, sub.indexOf(":")));
            sub = text.substring(sub.indexOf(":") + 1);
        }
        ticks += (Double.parseDouble(sub) / 0.6);
        return ticks;
    }

    public void removeFilterRow(int row)
    {
        activeFilters.remove(row);
        updateFilterTable();
    }

    public void removeComparisonRow(int row)
    {
        comparisons.remove(row);
        updateComparisonTable();
    }

    private void updateComparisonTable()
    {
        String[] columnNames = {"Sets", "Label", ""};
        ArrayList<Object[]> tableData = new ArrayList<>();

        int index = 0;
        for (ArrayList<RoomData> comparison : comparisons)
        {
            Object[] row = {comparison.size() + " raids averaging: " + RoomUtil.time(StatisticGatherer.getOverallTimeAverage(comparison)), "Set " + index, "Remove"};
            tableData.add(row);
            index++;
        }

        Object[][] tableObject = new Object[tableData.size()][2];
        int count = 0;
        for (Object[] row : tableData)
        {
            tableObject[count] = row;
            count++;
        }
        comparisonTable.setModel(new DefaultTableModel(tableObject, columnNames));
        comparisonTable.getColumn("Sets").setCellEditor(new NonEditableCell(new JTextField()));
        comparisonTable.getColumn("").setCellRenderer(new ButtonRenderer());
        comparisonTable.getColumn("").setCellEditor(new ButtonEditorComparisonData(new JCheckBox(), this));
        resizeColumnWidthFilters(comparisonTable);
        comparisonTable.getColumn("").setMaxWidth(100);
        comparisonTable.setFillsViewportHeight(true);
        comparisonTable.validate();
        comparisonTable.repaint();
    }

    public void setFilterState(String state)
    {
        try
        {
            if (state.contains(":"))
            {
                String[] data = state.split(":");
                if (data.length > 1)
                {
                    switch (data[0])
                    {
                        case "Spectate Only":
                            filterSpectateOnly.setSelected(Boolean.parseBoolean(data[1]));
                            break;
                        case "In Raid Only":
                            filterInRaidOnly.setSelected(Boolean.parseBoolean(data[1]));
                            break;
                        case "Completion Only":
                            filterCompletionOnly.setSelected(Boolean.parseBoolean(data[1]));
                            break;
                        case "Wipe/Reset Only":
                            filterWipeResetOnly.setSelected(Boolean.parseBoolean(data[1]));
                            break;
                        case "Today Only":
                            filterTodayOnly.setSelected(Boolean.parseBoolean(data[1]));
                            break;
                        case "Party Only":
                            filterPartyOnly.setSelected(Boolean.parseBoolean(data[1]));
                            break;
                        case "Partial Raids":
                            filterPartialOnly.setSelected(Boolean.parseBoolean(data[1]));
                            break;
                        case "Partial Rooms":
                            filterPartialData.setSelected(Boolean.parseBoolean(data[1]));
                            break;
                        case "Normal Mode Only":
                            filterNormalOnly.setSelected(Boolean.parseBoolean(data[1]));
                            break;
                        case "Scale":
                            if(data.length > 2)
                            {
                                filterCheckBoxScale.setSelected(Boolean.parseBoolean(data[1]));
                                filterComboBoxScale.setSelectedIndex(Integer.parseInt(data[2]));
                            }
                            break;
                        case "View Raid By":
                            viewByRaidComboBox.setEditable(true);
                            if(!Objects.equals(data[1], "null"))
                            {
                                viewByRaidComboBox.setSelectedItem(data[1]);
                            }
                            else
                            {
                                viewByRaidComboBox.setSelectedItem("Challenge Time");
                            }
                            viewByRaidComboBox.setEditable(false);
                            break;
                        case "Table Sort By":
                            sortOptionsBox.setSelectedItem(data[1]);
                            break;
                        case "Table Sort":
                            sortOrderBox.setSelectedItem(data[1]);
                            break;
                    }
                }
            }
        }
        catch (Exception e)
        {
            log.info("Failed to set filter state: " + state);
        }
    }

    public void updateFilterTable()
    {
        String[] columnNames = {"Filter Descriptions", ""};
        ArrayList<Object[]> tableData = new ArrayList<>();

        for (ImplicitFilter filter : activeFilters)
        {
            Object[] row = {filter.getFilterDescription(), "Remove"};
            tableData.add(row);
        }

        Object[][] tableObject = new Object[tableData.size()][2];
        int count = 0;
        for (Object[] row : tableData)
        {
            tableObject[count] = row;
            count++;
        }
        filterTable.setModel(new DefaultTableModel(tableObject, columnNames));
        filterTable.setDefaultRenderer(Object.class, new StripedTableRowCellRenderer());
        filterTable.getColumn("Filter Descriptions").setCellEditor(new NonEditableCell(new JTextField()));
        filterTable.getColumn("").setCellRenderer(new ButtonRenderer());
        filterTable.getColumn("").setCellEditor(new ButtonEditorFilterData(new JCheckBox(), this));
        resizeColumnWidthFilters(filterTable);
        filterTable.getColumn("").setMaxWidth(100);
        filterTable.setFillsViewportHeight(true);
        filterTableContainer.validate();
        filterTableContainer.repaint();
        updateTable();
    }
}
