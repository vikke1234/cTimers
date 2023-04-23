package com.cTimers.utility;

import com.google.inject.Inject;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import com.cTimers.LogID;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class cLogger
{
    private Client client;
    private int versionID = 1;

    public cLogger(Client client)
    {
        this.client = client;
    }

    public void write(LogID id, String... params) {
        if (params.length > 5)
            throw new IllegalArgumentException("Too many values passed to cLogger");
        String[] values = {"", "", "", "", ""};
        for (int i = 0; i < params.length; i++) {
            values[i] = params[i];
        }
        write(id.getId(), values[0], values[1], values[2], values[3], values[4]);
    }

    public void write(int key)
    {
        write(key, "", "", "", "", "");
    }

    public void write(int key, String v1)
    {
        write(key, v1, "", "", "", "");
    }

    public void write(int key, String v1, String v2)
    {
        write(key, v1, v2, "", "", "");
    }

    public void write(int key, String v1, String v2, String v3)
    {
        write(key, v1, v2, v3, "", "");
    }

    public void write(int key, String v1, String v2, String v3, String v4)
    {
        write(key, v1, v2, v3, v4, "");
    }

    public void write(int key, String v1, String v2, String v3, String v4, String v5)
    {
        writeFile(getUID() + "," + System.currentTimeMillis() + "," + versionID + "," + key + "," + v1 + "," + v2 + "," + v3 + "," + v4 + "," + v5);
    }

    public void writeFile(String msg)
    {
        if(cDebugHelper.writeKeyMessagesToChatbox)
        {
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", msg, "");
        }
        try
        {
            File logFile = new File(System.getProperty("user.home").replace("\\", "/") + "/.runelite/logs/tobdata.log");
            if (!logFile.exists())
            {
                logFile.createNewFile();
            }
            BufferedWriter logger = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(System.getProperty("user.home").replace("\\", "/") + "/.runelite/logs/tobdata.log", true),StandardCharsets.UTF_8));
            logger.write(msg);
            logger.newLine();
            logger.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private String getUID()
    {
        String UID = "";
        for(int i = 0; i < Objects.requireNonNull(Objects.requireNonNull(client.getLocalPlayer()).getName()).length(); i++)
        {
            int value = Objects.requireNonNull(client.getLocalPlayer().getName()).toCharArray()[i];
            UID += value;
        }
        return UID;
    }
}