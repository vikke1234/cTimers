package com.TheatreTracker;

import com.TheatreTracker.constants.TobIDs;
import com.TheatreTracker.constants.TOBRoom;
import com.TheatreTracker.ui.charts.LiveChart;
import com.TheatreTracker.ui.RaidTrackerSidePanel;
import com.TheatreTracker.utility.*;
import com.TheatreTracker.utility.Point;
import com.TheatreTracker.utility.datautility.DataWriter;
import com.TheatreTracker.utility.thrallvengtracking.*;
import com.TheatreTracker.utility.wrappers.PlayerCopy;
import com.TheatreTracker.utility.wrappers.PlayerDidAttack;
import com.TheatreTracker.utility.wrappers.QueuedPlayerAttackLessProjectiles;
import com.TheatreTracker.utility.wrappers.ThrallOutlineBox;
import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.kit.KitType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PartyChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.party.PartyMember;
import net.runelite.client.party.PartyService;
import net.runelite.client.party.WSClient;
import net.runelite.client.party.events.UserJoin;
import net.runelite.client.party.events.UserPart;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import com.TheatreTracker.rooms.*;
import net.runelite.client.plugins.devtools.DevToolsPlugin;
import net.runelite.client.plugins.specialcounter.SpecialCounterUpdate;
import net.runelite.client.plugins.specialcounter.SpecialWeapon;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.List;
import static com.TheatreTracker.constants.LogID.*;
import static com.TheatreTracker.constants.TobIDs.*;
import static com.TheatreTracker.constants.TOBRoom.*;
import static com.TheatreTracker.utility.RoomUtil.inRegion;


@Slf4j
@PluginDescriptor(
        name = "Theatre of Blood Tracker",
        description = "Tracking and statistics for Theatre of Blood",
        tags = {"timers", "tob", "tracker", "time", "theatre", "analytics"}
)
public class TheatreTrackerPlugin extends Plugin
{
    private NavigationButton navButtonPrimary;
    public DataWriter clog;

    private boolean partyIntact = false;

    @Inject
    private TheatreTrackerConfig config;

    @Inject
    private ItemManager itemManager;

    public TheatreTrackerPlugin()
    {
    }

    @Provides
    TheatreTrackerConfig getConfig(ConfigManager configManager)
    {
        return configManager.getConfig(TheatreTrackerConfig.class);
    }

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    public ClientThread clientThread;

    @Inject
    private PartyService party;

    @Inject
    private Client client;

    private LobbyHandler lobby;
    private MaidenHandler maiden;
    private BloatHandler bloat;
    private NyloHandler nylo;
    private SotetsegHandler sote;
    private XarpusHandler xarpus;
    private VerzikHandler verzik;

    private ArrayList<DamageQueueShell> queuedThrallDamage;

    private ArrayList<QueuedPlayerAttackLessProjectiles> playersAttacked;


    private boolean inTheatre;
    private boolean wasInTheatre;
    private RoomHandler currentRoom;
    int deferredTick;
    public ArrayList<String> currentPlayers;
    public static int scale = -1;
    public boolean verzShieldActive = false;

    private ThrallTracker thrallTracker;
    private VengTracker vengTracker;
    private List<PlayerShell> localPlayers;
    private List<ProjectileQueue> activeProjectiles;
    private List<VengDamageQueue> activeVenges;
    private RaidTrackerSidePanel timersPanelPrimary;

    @Inject
    private PluginManager pluginManager;

    @Inject
    private EventBus eventBus;

    @Inject
    private WSClient wsClient;

    Map<Player, Integer> activelyPiping;
    public LiveChart liveFrame;

    @Override
    protected void shutDown()
    {
        partyIntact = false;
        clog.write(LEFT_TOB, String.valueOf(client.getTickCount() - currentRoom.roomStartTick), currentRoom.getName());
        clientToolbar.removeNavigation(navButtonPrimary);
    }

    public void openLiveFrame()
    {
        liveFrame.open();
    }

    public int getTick()
    {
        return client.getTickCount();
    }

    public boolean isVerzP2()
    {
        if (currentRoom instanceof VerzikHandler)
        {
            VerzikHandler room = (VerzikHandler) currentRoom;
            return room.roomState == RoomState.VerzikRoomState.PHASE_2 || room.roomState == RoomState.VerzikRoomState.PHASE_2_REDS;
        }
        return false;
    }

    @Override
    protected void startUp() throws Exception
    {
        super.startUp();
        localPlayers = new ArrayList<>();
        thrallTracker = new ThrallTracker(this);
        vengTracker = new VengTracker(this);
        activeProjectiles = new ArrayList<>();
        activeVenges = new ArrayList<>();
        queuedThrallDamage = new ArrayList<>();
        timersPanelPrimary = injector.getInstance(RaidTrackerSidePanel.class);
        partyIntact = false;
        activelyPiping = new LinkedHashMap<>();
        liveFrame = new LiveChart(config, itemManager, clientThread);
        playersTextChanged = new ArrayList<>();
        File dirMain = new File(System.getProperty("user.home").replace("\\", "/") + "/.runelite/theatretracker/primary/");
        File dirFilters = new File(System.getProperty("user.home").replace("\\", "/") + "/.runelite/theatretracker/filters/");

        File dirRaids = new File(System.getProperty("user.home").replace("\\", "/") + "/.runelite/theatretracker/raids/");
        if (!dirRaids.exists()) dirRaids.mkdirs();

        if (!dirMain.exists()) dirMain.mkdirs();
        if (!dirFilters.exists()) dirFilters.mkdirs();

        File logFile = new File(System.getProperty("user.home").replace("\\", "/") + "/.runelite/theatretracker/primary/tobdata.log");
        if (!logFile.exists())
        {
            logFile.createNewFile();
        }

        final BufferedImage icon = ImageUtil.loadImageResource(DevToolsPlugin.class, "devtools_icon.png");
        navButtonPrimary = NavigationButton.builder().tooltip("RaidTrackerPanelPrimary").icon(icon).priority(10).panel(timersPanelPrimary).build();

        clientToolbar.addNavigation(navButtonPrimary);

        clog = new DataWriter(config);
        lobby = new LobbyHandler(client, clog, config);
        maiden = new MaidenHandler(client, clog, config, this, itemManager);
        bloat = new BloatHandler(client, clog, config, this);
        nylo = new NyloHandler(client, clog, config, this);
        sote = new SotetsegHandler(client, clog, config, this);
        xarpus = new XarpusHandler(client, clog, config, this);
        verzik = new VerzikHandler(client, clog, config, this, itemManager);
        inTheatre = false;
        wasInTheatre = false;
        deferredTick = 0;
        currentPlayers = new ArrayList<>();
        playersAttacked = new ArrayList<>();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> clog.write(LEFT_TOB, String.valueOf(client.getTickCount() - currentRoom.roomStartTick), currentRoom.getName())));

    }

    /**
     * @return Room as int if inside TOB (0 indexed), -1 otherwise
     */
    private int getRoom()
    {
        if (inRegion(client, LOBBY_REGION))
            return -1;
        else if (inRegion(client, MAIDEN_REGION))
            return 0;
        else if (inRegion(client, BLOAT_REGION))
            return 1;
        else if (inRegion(client, NYLO_REGION))
            return 2;
        else if (inRegion(client, SOTETSEG_REGION) || inRegion(client, SOTETSEG_UNDER_REGION))
            return 3;
        else if (inRegion(client, XARPUS_REGION))
            return 4;
        else if (inRegion(client, VERZIK_REGION))
            return 5;
        return -1;
    }

    private void updateRoom()
    {
        RoomHandler previous = currentRoom;
        int currentRegion = getRoom();
        boolean activeState = false;
        if (inRegion(client, LOBBY_REGION))
        {
            currentRoom = lobby;
        } else if (previous == lobby && inRegion(client, BLOAT_REGION, NYLO_REGION, SOTETSEG_REGION, XARPUS_REGION, VERZIK_REGION))
        {
            deferredTick = client.getTickCount() + 2; //Check two ticks from now for player names in orbs
            clog.write(ENTERED_TOB);
            clog.write(SPECTATE);
            clog.write(LATE_START, String.valueOf(currentRegion));
            liveFrame.resetAll();
        }
        if (inRegion(client, MAIDEN_REGION))
        {
            if (previous != maiden)
            {
                currentRoom = maiden;
                enteredMaiden();
                liveFrame.resetAll();
            }
            activeState = true;
        } else if (inRegion(client, BLOAT_REGION))
        {
            if (previous != bloat)
            {
                currentRoom = bloat;
                enteredBloat();
            }
            activeState = true;
        } else if (inRegion(client, NYLO_REGION))
        {
            if (previous != nylo)
            {
                currentRoom = nylo;
                enteredNylo();
            }
            activeState = true;
        } else if (inRegion(client, SOTETSEG_REGION, SOTETSEG_UNDER_REGION))
        {
            if (previous != sote)
            {
                currentRoom = sote;
                enteredSote();
            }
            activeState = true;
        } else if (inRegion(client, XARPUS_REGION))
        {
            if (previous != xarpus)
            {
                currentRoom = xarpus;
                enteredXarpus();
            }
            activeState = true;
        } else if (inRegion(client, VERZIK_REGION))
        {
            if (previous != verzik)
            {
                currentRoom = verzik;
                enteredVerzik();
            }
            activeState = true;
        }
        inTheatre = activeState;
    }

    private void enteredMaiden()
    {
        clog.write(ENTERED_TOB);
        deferredTick = client.getTickCount() + 2;
        maiden.reset();
        liveFrame.tabbedPane.setSelectedIndex(0);
    }

    private void enteredBloat()
    {
        clog.write(ENTERED_NEW_TOB_REGION, TOBRoom.BLOAT.ordinal());
        maiden.reset();
        bloat.reset();
        liveFrame.tabbedPane.setSelectedIndex(1);
    }

    private void enteredNylo()
    {
        clog.write(ENTERED_NEW_TOB_REGION, NYLOCAS.ordinal());
        bloat.reset();
        nylo.reset();
        liveFrame.tabbedPane.setSelectedIndex(2);
    }

    private void enteredSote()
    {
        clog.write(ENTERED_NEW_TOB_REGION, SOTETSEG.ordinal());
        nylo.reset();
        sote.reset();
        liveFrame.tabbedPane.setSelectedIndex(3);
    }

    private void enteredXarpus()
    {
        clog.write(ENTERED_NEW_TOB_REGION, XARPUS.ordinal());
        sote.reset();
        xarpus.reset();
        liveFrame.tabbedPane.setSelectedIndex(4);
    }

    private void enteredVerzik()
    {
        clog.write(ENTERED_NEW_TOB_REGION, VERZIK.ordinal());
        xarpus.reset();
        verzik.reset();
        liveFrame.tabbedPane.setSelectedIndex(5);
    }

    @Subscribe
    public void onSpecialCounterUpdate(SpecialCounterUpdate event)
    {
        if (inTheatre)
        {
            String name = party.getMemberById(event.getMemberId()).getDisplayName();
            if (name == null)
            {
                return;
            }
            boolean playerInRaid = false;
            // Ensures correct names across encodings
            for (String player : currentPlayers)
            {
                if (name.equals(player.replaceAll(String.valueOf((char) 160), String.valueOf((char) 32))))
                {
                    playerInRaid = true;
                    break;
                }
            }
            if (playerInRaid)
            {
                if (event.getWeapon().equals(SpecialWeapon.BANDOS_GODSWORD))
                {
                    clog.write(BGS, name, String.valueOf(event.getHit()), String.valueOf(client.getTickCount() - currentRoom.roomStartTick));
                }
                if (event.getWeapon().equals(SpecialWeapon.DRAGON_WARHAMMER))
                {
                    clog.write(DWH, name, String.valueOf(client.getTickCount() - currentRoom.roomStartTick));
                }
            }
        }
    }

    private String cleanString(String s1)
    {
        return s1.replaceAll(String.valueOf((char) 160), String.valueOf((char) 32));
    }

    private boolean isPartyComplete()
    {
        if (currentPlayers.size() > party.getMembers().size())
        {
            return false;
        }
        for (String raidPlayer : currentPlayers)
        {
            boolean currentPlayerMatched = false;
            for (PartyMember partyPlayer : party.getMembers())
            {
                if (cleanString(raidPlayer).equals(partyPlayer.getDisplayName()))
                {
                    currentPlayerMatched = true;
                }
            }
            if (!currentPlayerMatched)
            {
                return false;
            }
        }
        return true;
    }

    private void checkPartyUpdate()
    {
        if (inTheatre)
        {
            if (partyIntact)
            {
                if (!isPartyComplete())
                {
                    partyIntact = false;
                    clog.write(PARTY_INCOMPLETE);
                }
            } else
            {
                if (isPartyComplete())
                {
                    partyIntact = true;
                    clog.write(PARTY_COMPLETE);
                }
            }
        }

    }

    @Subscribe
    public void onPartyChanged(final PartyChanged party)
    {

        checkPartyUpdate();
    }

    @Subscribe
    public void onUserPart(final UserPart event)
    {
        checkPartyUpdate();
    }

    @Subscribe
    public void onUserJoin(final UserJoin event)
    {
        checkPartyUpdate();
    }

    public void addQueuedThrallDamage(int targetIndex, int sourceIndex, int offset, String source)
    {
        queuedThrallDamage.add(new DamageQueueShell(targetIndex, sourceIndex, offset, source, client.getTickCount()));
    }

    public void removeDeadProjectiles()
    {
        activeProjectiles.removeIf(projectileQueue -> projectileQueue.finalTick <= client.getTickCount());
    }

    public void removeDeadVenges()
    {
        activeVenges.removeIf(vengDamageQueue -> vengDamageQueue.appliedTick <= client.getTickCount());
    }

    public void addDelayedLine(TOBRoom room, int value, String description)
    {
        switch (TOBRoom.valueOf(room.value))
        {
            case MAIDEN:
                liveFrame.addMaidenLine(value, description);
                break;
            case BLOAT:
                liveFrame.addBloatLine(value, description);
                break;
            case NYLOCAS:
                liveFrame.addNyloLine(value, description);
                break;
            case SOTETSEG:
                liveFrame.addSoteLine(value, description);
                break;
            case XARPUS:
                liveFrame.addXarpLine(value, description);
                break;
            case VERZIK:
                liveFrame.addVerzikLine(value, description);
                break;
        }
    }

    public void thrallAttackedP2VerzikShield(int tickOffset)
    {
        if (currentRoom instanceof VerzikHandler)
        {
            VerzikHandler room = (VerzikHandler) currentRoom;
            room.thrallAttackedShield(client.getTickCount() + tickOffset);
        }
    }

    public void removeThrallBox(Thrall thrall)
    {
        clog.write(THRALL_DESPAWN, thrall.getOwner(), String.valueOf(client.getTickCount() - currentRoom.roomStartTick));
        liveFrame.getPanel(currentRoom.getName()).removeThrall(thrall.getOwner());
    }

    public void addThrallOutlineBox(ThrallOutlineBox outlineBox)
    {
        clog.write(THRALL_SPAWN, outlineBox.owner, String.valueOf(outlineBox.spawnTick), String.valueOf(outlineBox.id));
        liveFrame.getPanel(currentRoom.getName()).addThrallBox(outlineBox);
    }

    public Map<String, PlayerCopy> lastTickPlayer = new HashMap<>();
    public int getRoomTick()
    {
        return client.getTickCount() - currentRoom.roomStartTick;
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        log.info("Tick " + client.getTickCount());
        checkAnimationsThatChanged();
        checkOverheadTextsThatChanged();

        for (Player p : activelyPiping.keySet())
        {
            if ((client.getTickCount() > (activelyPiping.get(p) + 1)) && ((client.getTickCount() - activelyPiping.get(p)-1) % 2 == 0))
            {
                if (p.getAnimation() == BLOWPIPE_ANIMATION || p.getAnimation() == BLOWPIPE_ANIMATION_OR)
                {
                    PlayerCopy previous = lastTickPlayer.get(p.getName());
                    if(previous != null)
                    {
                        clog.write(PLAYER_ATTACK,
                                previous.name + ":" + (client.getTickCount() - currentRoom.roomStartTick - 1),
                                previous.animation + ":" + previous.wornItems,
                                "",
                                previous.weapon + ":" + previous.interactingIndex + ":" + previous.interactingID,
                                "-1:" + previous.interactingName);
                        liveFrame.addAttack(new PlayerDidAttack(itemManager,
                                previous.name,
                                String.valueOf(previous.animation),
                                -1,
                                previous.weapon,
                                "-1",
                                "",
                                previous.interactingIndex,
                                previous.interactingID,
                                previous.interactingName,
                                previous.wornItems
                        ), currentRoom.getName());
                    }
                }
            }
            int interactedIndex = -1;
            int interactedID = -1;
            String targetName = "";
            Actor interacted = p.getInteracting();
            if (interacted instanceof NPC)
            {
                NPC npc = (NPC) interacted;
                interactedID = npc.getId();
                interactedIndex = npc.getIndex();
                targetName = npc.getName();
            }
            if (interacted instanceof Player)
            {
                Player player = (Player) interacted;
                targetName = player.getName();
            }
            lastTickPlayer.put(p.getName(), new PlayerCopy(
                    p.getName(), interactedIndex, interactedID, targetName, p.getAnimation(), PlayerWornItems.getStringFromComposition(p.getPlayerComposition()
            ), String.valueOf(p.getPlayerComposition().getEquipmentId(KitType.WEAPON))));
        }

        for (QueuedPlayerAttackLessProjectiles playerAttackQueuedItem : playersAttacked)
        {
            playerAttackQueuedItem.tick--;
            if (playerAttackQueuedItem.tick == 0)
            {
                for (Projectile projectile : client.getProjectiles())
                {
                    int offset = 41; //zcb
                    if (projectile.getId() == DAWNBRINGER_AUTO_PROJECTILE || projectile.getId() == DAWNBRINGER_SPEC_PROJECTILE)
                    {
                        offset = 51; //dawnbringer
                    }
                    if (projectile.getStartCycle() == client.getGameCycle() + offset)
                    {
                        WorldPoint position = WorldPoint.fromLocal(client, new LocalPoint(projectile.getX1(), projectile.getY1()));
                        if (position.distanceTo(playerAttackQueuedItem.player.getWorldLocation()) == 0)
                        {
                            if (projectile.getId() == DAWNBRINGER_AUTO_PROJECTILE || projectile.getId() == DAWNBRINGER_SPEC_PROJECTILE)
                            {
                                int projectileHitTick = projectile.getRemainingCycles();
                                projectileHitTick = (projectileHitTick / 30);
                                clog.write(DAWN_SPEC, playerAttackQueuedItem.player.getName(), String.valueOf(client.getTickCount() - currentRoom.roomStartTick + projectileHitTick + 1));

                            }
                            if (projectile.getId() == DAWNBRINGER_AUTO_PROJECTILE || projectile.getId() == ZCB_PROJECTILE || projectile.getId() == ZCB_SPEC_PROJECTILE || projectile.getId() == DAWNBRINGER_SPEC_PROJECTILE)
                            {
                                int interactedIndex = -1;
                                int interactedID = -1;
                                Actor interacted = playerAttackQueuedItem.player.getInteracting();
                                String targetName = "";
                                if (interacted instanceof NPC)
                                {
                                    NPC npc = (NPC) interacted;
                                    interactedID = npc.getId();
                                    interactedIndex = npc.getIndex();
                                    targetName = npc.getName();
                                }
                                if (interacted instanceof Player)
                                {
                                    Player player = (Player) interacted;
                                    targetName = player.getName();
                                }
                                clog.write(PLAYER_ATTACK,
                                        playerAttackQueuedItem.player.getName() + ":" + (client.getTickCount() - currentRoom.roomStartTick),
                                        playerAttackQueuedItem.animation+":"+PlayerWornItems.getStringFromComposition(playerAttackQueuedItem.player.getPlayerComposition()),
                                        playerAttackQueuedItem.spotAnims,
                                        playerAttackQueuedItem.weapon + ":" + interactedIndex + ":" + interactedID,
                                        projectile.getId() + ":" + targetName);
                                liveFrame.addAttack(new PlayerDidAttack(itemManager,
                                                playerAttackQueuedItem.player.getName(),
                                                playerAttackQueuedItem.animation,
                                                0,
                                                playerAttackQueuedItem.weapon,
                                                String.valueOf(projectile.getId()),
                                                playerAttackQueuedItem.spotAnims,
                                                interactedIndex,
                                                interactedID,
                                                targetName,
                                                PlayerWornItems.getStringFromComposition(playerAttackQueuedItem.player.getPlayerComposition()))
                                        , currentRoom.getName());
                            }
                        }
                    }
                }
            }
        }
        playersAttacked.removeIf(p -> p.tick == 0);
        removeDeadProjectiles();
        removeDeadVenges();
        playersTextChanged.clear();
        localPlayers.clear();
        for (Player p : client.getPlayers())
        {
            localPlayers.add(new PlayerShell(p.getWorldLocation(), p.getName()));
            thrallTracker.updatePlayerInteracting(p.getName(), p.getInteracting());
        }
        for (DamageQueueShell damage : queuedThrallDamage)
        {
            damage.offset--;
        }
        thrallTracker.updateTick();
        vengTracker.updateTick();
        updateRoom();
        if (inTheatre)
        {
            wasInTheatre = true;
            currentRoom.updateGameTick(event);

            if (currentRoom.isActive())
            {
                liveFrame.incrementTick(currentRoom.getName());
                int HP_VARBIT = 6448;
                liveFrame.getPanel(currentRoom.getName()).addRoomHP(client.getTickCount() - currentRoom.roomStartTick, client.getVarbitValue(HP_VARBIT));
                clog.write(UPDATE_HP, String.valueOf(client.getVarbitValue(HP_VARBIT)), String.valueOf(client.getTickCount() - currentRoom.roomStartTick), currentRoom.getName());
            }

            if (client.getTickCount() == deferredTick)
            {
                String[] players = {"", "", "", "", ""};
                int varcStrID = 330; // Widget for player names
                for (int i = varcStrID; i < varcStrID + 5; i++)
                {
                    if (client.getVarcStrValue(i) != null && !client.getVarcStrValue(i).isEmpty())
                    {
                        players[i - varcStrID] = Text.escapeJagex(client.getVarcStrValue(i));
                    }
                }
                for (String s : players)
                {
                    if (!s.isEmpty())
                    {
                        currentPlayers.add(s.replaceAll(String.valueOf((char) 160), String.valueOf((char) 32)));
                    }
                }
                liveFrame.setPlayers(currentPlayers);
                checkPartyUpdate();
                boolean flag = false;
                for (String p : players)
                {
                    if (p.replaceAll(String.valueOf((char) 160), String.valueOf((char) 32)).equals(Objects.requireNonNull(client.getLocalPlayer().getName()).replaceAll(String.valueOf((char) 160), String.valueOf((char) 32))))
                    {
                        flag = true;
                    }
                }
                deferredTick = 0;
                if (!flag)
                {
                    clog.write(SPECTATE);
                }
                clog.write(PARTY_MEMBERS, players[0], players[1], players[2], players[3], players[4]);
                maiden.setScale((int) Arrays.stream(players).filter(x -> !x.isEmpty()).count());
                scale = currentPlayers.size();
            }
        } else
        {
            if (wasInTheatre)
            {
                leftRaid();
                wasInTheatre = false;
            }
        }
    }

    private void checkOverheadTextsThatChanged()
    {
        for (String player : playersWhoHaveOverheadText)
        {
            for (VengPair vp : playersTextChanged)
            {
                if (vp.player.equals(player))
                {
                    vengTracker.vengProcced(vp);
                    activeVenges.add(new VengDamageQueue(vp.player, vp.hitsplat, client.getTickCount() + 1));
                }
            }
        }
        playersWhoHaveOverheadText.clear();
    }

    private void checkAnimationsThatChanged()
    {
        for(Player p : deferredAnimations)
        {
            checkAnimation(p);
        }
        deferredAnimations.clear();
    }

    private void checkAnimation(Player p)
    {
        if (inTheatre)
        {
            if (p.getPlayerComposition() != null)
            {
                int id = p.getPlayerComposition().getEquipmentId(KitType.WEAPON);
                if (p.getAnimation() == SCYTHE_ANIMATION)
                {
                    if (id == UNCHARGED_SCYTHE || id == UNCHARGED_BLOOD_SCYTHE || id == UNCHARGED_HOLY_SCYTHE)
                    {
                        if (config.showMistakesInChat())
                        {
                            sendChatMessage(p.getName() + " is using an uncharged scythe");
                        }
                    }
                } else if (p.getAnimation() == BOP_ANIMATION)
                {
                    if (id == DRAGON_WARHAMMER || id == DRAGON_WARHAMMER_ALTERNATE)
                    {
                        if (config.showMistakesInChat())
                        {
                            sendChatMessage(p.getName() + " hammer bopped (bad rng)");
                        }
                        clog.write(DWH_BOP, p.getName());
                    }
                } else if (p.getAnimation() == WHACK_ANIMATION)
                {
                    if (id == KODAI_WAND || id == KODAI_WAND_ALTERNATE)
                    {
                        if (config.showMistakesInChat())
                        {
                            sendChatMessage(p.getName() + " kodai bopped (nothing they could've done to prevent it)");
                        }
                        clog.write(KODAI_BOP, p.getName());
                    }
                } else if (p.getAnimation() == STAB_ANIMATION)
                {
                    if (id == CHALLY)
                    {
                        if (config.showMistakesInChat())
                        {
                            sendChatMessage(p.getName() + " chally poked");
                        }
                        clog.write(CHALLY_POKE, p.getName());
                    }
                } else if (p.getAnimation() == TWO_HAND_SWORD_SWING)
                {
                    if(id == BANDOS_GODSWORD || id == BANDOS_GODSWORD_OR)
                    {
                        if (config.showMistakesInChat())
                        {
                            sendChatMessage(p.getName() + " swung BGS without speccing");
                        }
                        clog.write(BGS_WHACK, p.getName());
                    }
                }
                StringBuilder animations = new StringBuilder();
                for (ActorSpotAnim anim : p.getSpotAnims())
                {
                    animations.append(anim.getId());
                    animations.append(":");
                }
                if (p.getAnimation() == POWERED_STAFF_ANIMATION || p.getAnimation() == CROSSBOW_ANIMATION)
                {
                    if (p.getAnimation() != POWERED_STAFF_ANIMATION || p.getPlayerComposition().getEquipmentId(KitType.WEAPON) == DAWNBRINGER_ITEM)
                    { //Can be ZCB, Sang, or Dawnbringer. We only care about projectile for dawnbringer or ZCB. Sang & dawnbringer share animation
                        //so this filters powered staves unless it's dawnbringer
                        WorldPoint worldPoint = p.getWorldLocation();
                        playersAttacked.add(new QueuedPlayerAttackLessProjectiles(p, worldPoint, 1, animations.toString(), String.valueOf(p.getPlayerComposition().getEquipmentId(KitType.WEAPON)), String.valueOf(p.getAnimation())));
                    }
                    else
                    {
                        int interactedIndex = -1;
                        int interactedID = -1;
                        Actor interacted = p.getInteracting();
                        String targetName = "";
                        if (interacted instanceof NPC)
                        {
                            NPC npc = (NPC) interacted;
                            interactedID = npc.getId();
                            interactedIndex = npc.getIndex();
                            targetName = npc.getName();
                        }
                        generatePlayerAttackInfo(p, animations.toString(), interactedIndex, interactedID, interacted, targetName);
                    }
                }
                else if (p.getAnimation() != -1)
                {
                    int interactedIndex = -1;
                    int interactedID = -1;
                    Actor interacted = p.getInteracting();
                    String targetName = "";
                    if (interacted instanceof NPC)
                    {
                        NPC npc = (NPC) interacted;
                        interactedID = npc.getId();
                        interactedIndex = npc.getIndex();
                    }
                    generatePlayerAttackInfo(p, animations.toString(), interactedIndex, interactedID, interacted, targetName);
                    if (p.getAnimation() == BLOWPIPE_ANIMATION || p.getAnimation() == BLOWPIPE_ANIMATION_OR)
                    {
                        activelyPiping.put(p, client.getTickCount());
                        interactedIndex = -1;
                        interactedID = -1;
                        targetName = "";
                        interacted = p.getInteracting();
                        if (interacted instanceof NPC)
                        {
                            NPC npc = (NPC) interacted;
                            interactedID = npc.getId();
                            interactedIndex = npc.getIndex();
                            targetName = npc.getName();
                        }
                        if (interacted instanceof Player)
                        {
                            Player player = (Player) interacted;
                            targetName = player.getName();
                        }
                        lastTickPlayer.put(p.getName(), new PlayerCopy(
                                p.getName(), interactedIndex, interactedID, targetName, p.getAnimation(), PlayerWornItems.getStringFromComposition(p.getPlayerComposition()
                        ), String.valueOf(p.getPlayerComposition().getEquipmentId(KitType.WEAPON))));
                        log.info("Adding " + p.getName() + " on tick " + client.getTickCount());
                    }
                    else
                    {
                        activelyPiping.remove(p);
                        lastTickPlayer.remove(p.getName());
                        log.info("removing " + p.getName());
                    }
                }
                else
                {
                    activelyPiping.remove(p);
                    lastTickPlayer.remove(p.getName());
                    log.info("removing " + p.getName());
                }

            }
        }
    }

    public void leftRaid()
    {
        lastTickPlayer.clear();
        partyIntact = false;
        currentPlayers.clear();
        clog.write(LEFT_TOB, String.valueOf(client.getTickCount() - currentRoom.roomStartTick), currentRoom.getName()); //todo add region
        currentRoom = null;
        activelyPiping.clear();
        deferredAnimations.clear();
    }

    @Subscribe
    public void onActorDeath(ActorDeath event)
    {
        if (inTheatre)
        {
            Actor a = event.getActor();
            if (a instanceof Player)
            {
                clog.write(PLAYER_DIED, event.getActor().getName(), String.valueOf(client.getTickCount() - currentRoom.roomStartTick));
            }
        }
    }

    @Subscribe
    public void onGroundObjectSpawned(GroundObjectSpawned event)
    {
        if(inTheatre)
        {
            currentRoom.updateGroundObjectSpawned(event);
        }
    }

    @Subscribe
    public void onGraphicChanged(GraphicChanged event)
    {
        if (event.getActor() instanceof Player)
        {
            int id = -1;
            if (event.getActor().hasSpotAnim(THRALL_CAST_GRAPHIC_MAGE))
            {
                id = THRALL_CAST_GRAPHIC_MAGE;
            } else if (event.getActor().hasSpotAnim(THRALL_CAST_GRAPHIC_MELEE))
            {
                id = THRALL_CAST_GRAPHIC_MELEE;
            } else if (event.getActor().hasSpotAnim(THRALL_CAST_GRAPHIC_RANGE))
            {
                id = THRALL_CAST_GRAPHIC_RANGE;
            } else if (event.getActor().hasSpotAnim(VENG_GRAPHIC))
            {
                vengTracker.vengSelfGraphicApplied((Player) event.getActor());
            } else if (event.getActor().hasSpotAnim(VENG_OTHER_GRAPHIC))
            {
                vengTracker.vengOtherGraphicApplied((Player) event.getActor());
            }
            if (id != -1)
            {
                thrallTracker.playerHasThrallCastSpotAnim((Player) event.getActor(), id);
            }

        }
        if (inTheatre)
        {
            currentRoom.updateGraphicChanged(event);
        }
    }

    @Subscribe
    public void onGraphicsObjectCreated(GraphicsObjectCreated event)
    {
        if (inTheatre)
        {
            currentRoom.updateGraphicsObjectCreated(event);
        }
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event)
    {
        if (inTheatre)
        {
            currentRoom.updateGameObjectSpawned(event);
        }
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned event)
    {
        if (inTheatre)
        {
            currentRoom.updateGameObjectDespawned(event);
        }
    }

    @Subscribe
    public void onItemSpawned(ItemSpawned event)
    {
        if (inTheatre)
        {
            currentRoom.updateItemSpawned(event);
        }
    }

    @Subscribe
    public void onProjectileMoved(ProjectileMoved event)
    {
        if (inTheatre)
        {
            int id = event.getProjectile().getId();
            if (id == THRALL_PROJECTILE_RANGE || id == THRALL_PROJECTILE_MAGE)
            {
                if (event.getProjectile().getStartCycle() == client.getGameCycle())
                {
                    thrallTracker.projectileCreated(event.getProjectile(), WorldPoint.fromLocal(client, new LocalPoint(event.getProjectile().getX1(), event.getProjectile().getY1())));
                }
            }
            //Thrall hitsplats come before damage hitsplits unless the source is a projectile that was spawned on a tick before the thrall projectile spawned
            else if (event.getProjectile().getStartCycle() == client.getGameCycle())
            { //Thrall projectiles move slower and the only time this situation occurs in TOB is max distance TBOW/ZCB during maiden
                if (id == TBOW_PROJECTILE || id == ZCB_PROJECTILE || id == ZCB_SPEC_PROJECTILE)
                { //Not sure why 10 is correct instead of 19 (60 - 41 tick delay) but extensive trial and error shows this to be accurate
                    int projectileHitTick = 10 + event.getProjectile().getRemainingCycles();
                    projectileHitTick = (projectileHitTick / 30);
                    if (event.getProjectile().getInteracting() instanceof NPC)
                    {
                        int index = ((NPC) event.getProjectile().getInteracting()).getIndex();
                        activeProjectiles.add(new ProjectileQueue(client.getTickCount(), projectileHitTick + client.getTickCount(), index));
                    }
                }
            }
            if (inTheatre)
            {
                currentRoom.updateProjectileMoved(event);
            }
        }
    }

    public void sendChatMessage(String msg)
    {
        clientThread.invoke(() -> client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", msg, null, false));
    }

    private final ArrayList<Player> deferredAnimations = new ArrayList<>();

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if(event.getKey().equals("reduceMemoryLoad") && event.getGroup().equals("Theatre Statistic Tracker"))
        {
            timersPanelPrimary.refreshRaids();
        }
    }

    @Subscribe
    public void onAnimationChanged(AnimationChanged event)
    {
        if(event.getActor() instanceof Player)
        {
            Player p = (Player) event.getActor();
            if(event.getActor().getAnimation() == 6294 || event.getActor().getAnimation() == 722 || event.getActor().getAnimation() == 6299 || event.getActor().getAnimation() == -1)
            {
                log.info(event.getActor().getName() + " has animation " + event.getActor().getAnimation() + " on tick " + client.getTickCount());
                checkAnimation(p);
            }
            else
            {
                deferredAnimations.add(p);
            }
        }
        if (inTheatre)
        {
            int id = event.getActor().getAnimation();
            if (event.getActor().getAnimation() == THRALL_CAST_ANIMATION)
            {
                thrallTracker.castThrallAnimation((Player) event.getActor());
            } else if (event.getActor().getAnimation() == MELEE_THRALL_ATTACK_ANIMATION && event.getActor() instanceof NPC)
            {
                thrallTracker.meleeThrallAttacked((NPC) event.getActor());
            } else if (event.getActor().getAnimation() == VENG_CAST)
            {
                vengTracker.vengSelfCast((Player) event.getActor());
            } else if (event.getActor().getAnimation() == VENG_OTHER_CAST)
            {
                vengTracker.vengOtherCast((Player) event.getActor());
            } else if (id == DWH_SPEC)
            {
                clog.write(HAMMER_ATTEMPTED, event.getActor().getName());
            } else if (event.getActor().getName() != null && event.getActor().getName().contains("Maiden") && id == MAIDEN_BLOOD_THROW_ANIM)
            {
                clog.write(BLOOD_THROWN);
            }
            if (inTheatre)
            {
                currentRoom.updateAnimationChanged(event);
            }
        }
    }

    private void generatePlayerAttackInfo(Player p, String animations, int interactedIndex, int interactedID, Actor interacted, String targetName)
    {
        if (interacted instanceof Player)
        {
            Player player = (Player) interacted;
            targetName = player.getName();
        }
        clog.write(PLAYER_ATTACK,
                p.getName() + ":" + (client.getTickCount() - currentRoom.roomStartTick),
                p.getAnimation()+":"+PlayerWornItems.getStringFromComposition(p.getPlayerComposition()),
                animations,
                p.getPlayerComposition().getEquipmentId(KitType.WEAPON) + ":" + interactedIndex + ":" + interactedID,
                "-1:" + targetName);
        liveFrame.addAttack(new PlayerDidAttack(itemManager,
                String.valueOf(p.getName()),
                String.valueOf(p.getAnimation()),
                0,
                String.valueOf(p.getPlayerComposition().getEquipmentId(KitType.WEAPON)),
                "-1",
                animations,
                interactedIndex,
                interactedID,
                targetName,
                PlayerWornItems.getStringFromComposition(p.getPlayerComposition())
        ), currentRoom.getName());
    }

    @Subscribe
    public void onInteractingChanged(InteractingChanged event)
    {
        if (inTheatre)
        {
            currentRoom.updateInteractingChanged(event);
        }
    }

    @Subscribe
    public void onNpcChanged(NpcChanged event)
    {
        if (inTheatre)
        {
            currentRoom.handleNPCChanged(event.getNpc().getId());
        }
    }

    private void handleThrallSpawn(NPC npc)
    {
        ArrayList<PlayerShell> potentialPlayers = new ArrayList<>();
        for (PlayerShell p : localPlayers)
        {
            if (p.worldLocation.distanceTo(npc.getWorldLocation()) == 1)
            {
                potentialPlayers.add(p);
            }
        }
        thrallTracker.thrallSpawned(npc, potentialPlayers);
    }

    @Subscribe
    public void onNpcSpawned(NpcSpawned event)
    {
        int id = event.getNpc().getId();
        if (id == MELEE_THRALL || id == RANGE_THRALL || id == MAGE_THRALL)
        {
            handleThrallSpawn(event.getNpc());
        }
        switch (event.getNpc().getId())
        {
            case TobIDs.MAIDEN_P0:
            case TobIDs.MAIDEN_P1:
            case TobIDs.MAIDEN_P2:
            case TobIDs.MAIDEN_P3:
            case TobIDs.MAIDEN_PRE_DEAD:
            case TobIDs.MAIDEN_DEAD:
            case TobIDs.MAIDEN_MATOMENOS:
            case TobIDs.MAIDEN_P0_HM:
            case TobIDs.MAIDEN_P1_HM:
            case TobIDs.MAIDEN_P2_HM:
            case TobIDs.MAIDEN_P3_HM:
            case TobIDs.MAIDEN_PRE_DEAD_HM:
            case TobIDs.MAIDEN_DEAD_HM:
            case TobIDs.MAIDEN_MATOMENOS_HM:
            case TobIDs.MAIDEN_P0_SM:
            case TobIDs.MAIDEN_P1_SM:
            case TobIDs.MAIDEN_P2_SM:
            case TobIDs.MAIDEN_P3_SM:
            case TobIDs.MAIDEN_PRE_DEAD_SM:
            case TobIDs.MAIDEN_DEAD_SM:
            case TobIDs.MAIDEN_MATOMENOS_SM:
            case TobIDs.MAIDEN_BLOOD:
            case TobIDs.MAIDEN_BLOOD_HM:
            case TobIDs.MAIDEN_BLOOD_SM:
            {
                maiden.updateNpcSpawned(event);
            }
            break;
            case TobIDs.BLOAT:
            case TobIDs.BLOAT_HM:
            case TobIDs.BLOAT_SM:
                bloat.updateNpcSpawned(event);
                break;
            case TobIDs.NYLO_MELEE_SMALL:
            case TobIDs.NYLO_MELEE_SMALL_AGRO:
            case TobIDs.NYLO_RANGE_SMALL:
            case TobIDs.NYLO_RANGE_SMALL_AGRO:
            case TobIDs.NYLO_MAGE_SMALL:
            case TobIDs.NYLO_MAGE_SMALL_AGRO:
            case TobIDs.NYLO_MELEE_BIG:
            case TobIDs.NYLO_MELEE_BIG_AGRO:
            case TobIDs.NYLO_RANGE_BIG:
            case TobIDs.NYLO_RANGE_BIG_AGRO:
            case TobIDs.NYLO_MAGE_BIG:
            case TobIDs.NYLO_MAGE_BIG_AGRO:
            case TobIDs.NYLO_MELEE_SMALL_HM:
            case TobIDs.NYLO_MELEE_SMALL_AGRO_HM:
            case TobIDs.NYLO_RANGE_SMALL_HM:
            case TobIDs.NYLO_RANGE_SMALL_AGRO_HM:
            case TobIDs.NYLO_MAGE_SMALL_HM:
            case TobIDs.NYLO_MAGE_SMALL_AGRO_HM:
            case TobIDs.NYLO_MELEE_BIG_HM:
            case TobIDs.NYLO_MELEE_BIG_AGRO_HM:
            case TobIDs.NYLO_RANGE_BIG_HM:
            case TobIDs.NYLO_RANGE_BIG_AGRO_HM:
            case TobIDs.NYLO_MAGE_BIG_HM:
            case TobIDs.NYLO_MAGE_BIG_AGRO_HM:
            case TobIDs.NYLO_MELEE_SMALL_SM:
            case TobIDs.NYLO_MELEE_SMALL_AGRO_SM:
            case TobIDs.NYLO_RANGE_SMALL_SM:
            case TobIDs.NYLO_RANGE_SMALL_AGRO_SM:
            case TobIDs.NYLO_MAGE_SMALL_SM:
            case TobIDs.NYLO_MAGE_SMALL_AGRO_SM:
            case TobIDs.NYLO_MELEE_BIG_SM:
            case TobIDs.NYLO_MELEE_BIG_AGRO_SM:
            case TobIDs.NYLO_RANGE_BIG_SM:
            case TobIDs.NYLO_RANGE_BIG_AGRO_SM:
            case TobIDs.NYLO_MAGE_BIG_SM:
            case TobIDs.NYLO_MAGE_BIG_AGRO_SM:
            case TobIDs.NYLO_BOSS_DROPPING:
            case TobIDs.NYLO_BOSS_DROPPING_HM:
            case TobIDs.NYLO_BOSS_DROPING_SM:
            case TobIDs.NYLO_BOSS_MELEE:
            case TobIDs.NYLO_BOSS_MELEE_HM:
            case TobIDs.NYLO_BOSS_MELEE_SM:
            case TobIDs.NYLO_BOSS_MAGE:
            case TobIDs.NYLO_BOSS_MAGE_HM:
            case TobIDs.NYLO_BOSS_MAGE_SM:
            case TobIDs.NYLO_BOSS_RANGE:
            case TobIDs.NYLO_BOSS_RANGE_HM:
            case TobIDs.NYLO_BOSS_RANGE_SM:
            case TobIDs.NYLO_PRINKIPAS_DROPPING:
            case TobIDs.NYLO_PRINKIPAS_MELEE:
            case TobIDs.NYLO_PRINKIPAS_MAGIC:
            case TobIDs.NYLO_PRINKIPAS_RANGE:
                nylo.updateNpcSpawned(event);
                break;
            case TobIDs.SOTETSEG_ACTIVE:
            case TobIDs.SOTETSEG_ACTIVE_HM:
            case TobIDs.SOTETSEG_ACTIVE_SM:
            case TobIDs.SOTETSEG_INACTIVE:
            case TobIDs.SOTETSEG_INACTIVE_HM:
            case TobIDs.SOTETSEG_INACTIVE_SM:
                sote.updateNpcSpawned(event);
                break;
            case TobIDs.XARPUS_INACTIVE:
            case TobIDs.XARPUS_P1:
            case TobIDs.XARPUS_P23:
            case TobIDs.XARPUS_DEAD:
            case TobIDs.XARPUS_INACTIVE_HM:
            case TobIDs.XARPUS_P1_HM:
            case TobIDs.XARPUS_P23_HM:
            case TobIDs.XARPUS_DEAD_HM:
            case TobIDs.XARPUS_INACTIVE_SM:
            case TobIDs.XARPUS_P1_SM:
            case TobIDs.XARPUS_P23_SM:
            case TobIDs.XARPUS_DEAD_SM:
                xarpus.updateNpcSpawned(event);
                break;
            case TobIDs.VERZIK_P1_INACTIVE:
            case TobIDs.VERZIK_P1:
            case TobIDs.VERZIK_P2_INACTIVE:
            case TobIDs.VERZIK_P2:
            case TobIDs.VERZIK_P3_INACTIVE:
            case TobIDs.VERZIK_P3:
            case TobIDs.VERZIK_DEAD:
            case TobIDs.VERZIK_P1_INACTIVE_HM:
            case TobIDs.VERZIK_P1_HM:
            case TobIDs.VERZIK_P2_INACTIVE_HM:
            case TobIDs.VERZIK_P2_HM:
            case TobIDs.VERZIK_P3_INACTIVE_HM:
            case TobIDs.VERZIK_P3_HM:
            case TobIDs.VERZIK_DEAD_HM:
            case TobIDs.VERZIK_P1_INACTIVE_SM:
            case TobIDs.VERZIK_P1_SM:
            case TobIDs.VERZIK_P2_INACTIVE_SM:
            case TobIDs.VERZIK_P2_SM:
            case TobIDs.VERZIK_P3_INACTIVE_SM:
            case TobIDs.VERZIK_P3_SM:
            case TobIDs.VERZIK_DEAD_SM:
                verzik.updateNpcSpawned(event);
                break;
            default:
                if (currentRoom != null)
                {
                    currentRoom.updateNpcSpawned(event);
                }
                break;
        }
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned event)
    {
        int id = event.getNpc().getId();
        if (id == MELEE_THRALL || id == RANGE_THRALL || id == MAGE_THRALL)
        {
            thrallTracker.removeThrall(event.getNpc());
        }
        if (inTheatre)
        {
            currentRoom.updateNpcDespawned(event);
        }
    }

    @Subscribe
    public void onHitsplatApplied(HitsplatApplied event)
    {
        if (inTheatre)
        {
            if (event.getActor() instanceof Player && inTheatre)
            {
                playersTextChanged.add(new VengPair(event.getActor().getName(), event.getHitsplat().getAmount()));
            }
            queuedThrallDamage.sort(Comparator.comparing(DamageQueueShell::getSourceIndex));
            int index = -1;
            if (event.getActor() instanceof NPC && event.getHitsplat().getHitsplatType() != HitsplatID.HEAL)
            {
                for (int i = 0; i < queuedThrallDamage.size(); i++)
                {
                    int altIndex = 0;
                    int matchedIndex = -1;
                    boolean postponeThrallHit = false;
                    for (ProjectileQueue projectile : activeProjectiles)
                    {
                        if (projectile.targetIndex == ((NPC) event.getActor()).getIndex())
                        {
                            if (client.getTickCount() == projectile.finalTick)
                            {
                                if (projectile.originTick < queuedThrallDamage.get(i).originTick)
                                {
                                    postponeThrallHit = true;
                                    matchedIndex = altIndex;
                                }
                            }
                        }
                        altIndex++;
                    }
                    if (queuedThrallDamage.get(i).offset == 0 && queuedThrallDamage.get(i).targetIndex == ((NPC) event.getActor()).getIndex())
                    {
                        if (postponeThrallHit)
                        {
                            activeProjectiles.remove(matchedIndex);
                        } else
                        {
                            if (event.getHitsplat().getAmount() <= 3)
                            {
                                index = i;
                                clog.write(THRALL_DAMAGED, queuedThrallDamage.get(i).source, String.valueOf(event.getHitsplat().getAmount()));

                            }
                        }
                        if (index != -1)
                        {
                            queuedThrallDamage.remove(index);
                        }
                        if (inTheatre)
                        {
                            currentRoom.updateHitsplatApplied(event);
                        }
                        return;
                    }
                }
                for (VengDamageQueue veng : activeVenges)
                {
                    int expectedDamage = (int) (0.75 * veng.damage);
                    if (event.getHitsplat().getAmount() == expectedDamage)
                    {
                        //todo can be wrong if splat would overkill
                        clog.write(VENG_WAS_PROCCED, veng.target, String.valueOf(expectedDamage));
                        if (inTheatre)
                        {
                            currentRoom.updateHitsplatApplied(event);
                        }
                        return;
                    }
                }
            }

            if (inTheatre)
            {
                currentRoom.updateHitsplatApplied(event);
            }
        }
    }


    private ArrayList<VengPair> playersTextChanged;

    private final ArrayList<String> playersWhoHaveOverheadText = new ArrayList<>();

    @Subscribe
    public void onOverheadTextChanged(OverheadTextChanged event)
    {
        if (inTheatre)
        {
            if (event.getOverheadText().equals("Taste vengeance!"))
            {
                playersWhoHaveOverheadText.add(event.getActor().getName());
            }
            if (currentRoom instanceof XarpusHandler)
            {
                xarpus.updateOverheadText(event);
            }
        }
    }
}
