package com.TheatreTracker.utility.wrappers;

import com.TheatreTracker.utility.ItemReference;

import java.awt.*;

public class OutlineBox
{
    public String player;
    public int tick;
    public String letter;
    public Color color;
    public boolean primaryTarget;

    public String additionalText = "";
    public PlayerDidAttack attack;

    public OutlineBox(PlayerDidAttack attack, String letter, Color color, boolean primaryTarget, String additionalText)
    {
        this.attack = attack;
        this.player = attack.player;
        this.tick = attack.tick;
        this.letter = letter;
        this.color = color;
        this.primaryTarget = primaryTarget;
        this.additionalText = additionalText;
    }

    public final int NONE = 0;
    public final int MELEE = 1;
    public final int RANGE = 2;
    public final int MAGE = 3;
    public int style = NONE;
    public Color outlineColor = new Color(0, 0, 0, 0);

    private boolean anyMatch(String item, String[] items)
    {
        for(String s : items)
        {
            if(item.toLowerCase().contains(s))
            {
                return true;
            }
        }
        return false;
    }

    private void setStyle(String weapon)
    {
        if(anyMatch(weapon, ItemReference.ITEMS[MELEE]))
        {
            style = MELEE;
        }
        else if(anyMatch(weapon, ItemReference.ITEMS[RANGE]))
        {
            style = RANGE;
        }
        else if(anyMatch(weapon, ItemReference.ITEMS[MAGE]))
        {
            style = MAGE;
        }
    }
    public void createOutline()
    {
        if(letter.equals("VS") || letter.equals("AO") || letter.equals("HU") || letter.equals("MI") || letter.equals("DB"))
        {
            return;
        }
        int correctItems = 0;
        boolean voidHelmWorn = false;
        if(attack.wornItemNames.length == 9)
        {
            setStyle(attack.wornItemNames[3]);
            if(attack.wornItemNames[0].toLowerCase().contains("void"))
            {
                voidHelmWorn = true;
            }
            for(String s : attack.wornItemNames)
            {
                if(anyMatch(s, ItemReference.ITEMS[style]) || (voidHelmWorn && s.toLowerCase().contains("void")))
                {
                    correctItems++;
                }
            }
        }
        switch(style)
        {
            case MELEE:
                if(correctItems < 8)
                {
                    int opacity = (int)((255)*((8-correctItems)/8.0));
                    opacity = Math.max(opacity, 150);
                    outlineColor = new Color(255, 255, 0, opacity);
                }
                break;
            case RANGE:
                if(correctItems < 6)
                {
                    outlineColor = new Color(255, 255, 0, (int)((255)*((6-correctItems)/6.0)));
                }
                break;
            case MAGE:
                if(correctItems < 5)
                {
                    outlineColor = new Color(255, 255, 0, (int)((255)*((5-correctItems)/5.0)));
                }
                break;
        }
    }
}
