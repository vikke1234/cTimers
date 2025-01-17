package com.TheatreTracker.constants;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

import static com.TheatreTracker.constants.TOBRoom.*;

/**
 * Convenience class for the possible keys used to log events. The parameters each of these events should include can be found in
 * RoomData.
 */
@Getter
public enum LogID
{
    ENTERED_TOB(0, ANY, "Entered TOB"),
    PARTY_MEMBERS(1, ANY, "Party Members"),
    DWH(2, ANY,"DWH Hit"),
    BGS(3, ANY,"BGS Hit"),
    LEFT_TOB(4, ANY,"Left TOB"),
    PLAYER_DIED(5, ANY,"Played Died"),
    ENTERED_NEW_TOB_REGION(6, ANY,"Entered New TOB Region"),
    HAMMER_ATTEMPTED(7, ANY,"DWH Attempted"),
    DAWN_DROPPED(800, VERZIK,"Dawnbringer appeared"),
    WEBS_STARTED(901, VERZIK,"Webs Thrown"),
    PLAYER_ATTACK(801, ANY,"Player Animation"),
    BLOOD_THROWN(9,MAIDEN,"Maiden blood thrown"),
    BLOOD_SPAWNED(10),
    CRAB_LEAK(11),
    MAIDEN_SPAWNED(12),
    MAIDEN_70S(13),
    MAIDEN_50S(14),
    MAIDEN_30S(15),
    MAIDEN_0HP(16),
    MAIDEN_DESPAWNED(17),
    MATOMENOS_SPAWNED(18),
    MAIDEN_SCUFFED(19),

    BLOAT_SPAWNED(20),
    BLOAT_DOWN(21),
    BLOAT_0HP(22),
    BLOAT_DESPAWN(23),
    BLOAT_HP_1ST_DOWN(24),
    BLOAT_SCYTHE_1ST_WALK(25),

    NYLO_PILLAR_SPAWN(30),
    NYLO_STALL(31),
    RANGE_SPLIT(32),
    MAGE_SPLIT(33),
    MELEE_SPLIT(34),
    LAST_WAVE(35),
    LAST_DEAD(36),
    NYLO_WAVE(37),
    BOSS_SPAWN(40),
    MELEE_PHASE(41),
    MAGE_PHASE(42),
    RANGE_PHASE(43),
    NYLO_0HP(44),
    NYLO_DESPAWNED(45),
    NYLO_PILLAR_DESPAWNED(46), //tick
    SOTETSEG_STARTED(51),
    SOTETSEG_FIRST_MAZE_STARTED(52),
    SOTETSEG_FIRST_MAZE_ENDED(53),
    SOTETSEG_SECOND_MAZE_STARTED(54),
    SOTETSEG_SECOND_MAZE_ENDED(55),
    SOTETSEG_ENDED(57),
    XARPUS_SPAWNED(60),
    XARPUS_STARTED(61),
    XARPUS_HEAL(62),
    XARPUS_SCREECH(63),
    XARPUS_0HP(64),
    XARPUS_DESPAWNED(65),

    VERZIK_SPAWNED(70),
    VERZIK_P1_START(71),
    VERZIK_P1_0HP(72),
    VERZIK_P1_DESPAWNED(73),
    VERZIK_P2_END(74),
    VERZIK_P3_0HP(75),
    VERZIK_P3_DESPAWNED(76),
    VERZIK_BOUNCE(77),
    VERZIK_CRAB_SPAWNED(78),
    VERZIK_P2_REDS_PROC(80),

    LATE_START(98),
    SPECTATE(99),
    NOT_118(998),
    NO_PIETY(999),
    RANDOM_TRACKER(1000),
    RANDOM_TRACKER_2(1001),

    PARTY_COMPLETE(100),
    PARTY_INCOMPLETE(101),
    PARTY_ACCURATE_PREMAIDEN(102),

    MAIDEN_DINHS_SPEC(111), //Player, tick, primary target:primary target hp, targets~hp:,stats:stats
    MAIDEN_DINHS_TARGET(112), //

    MAIDEN_CHIN_THROWN(113), //player, distance

    ACCURATE_MAIDEN_START(201),
    ACCURATE_BLOAT_START(202),
    ACCURATE_NYLO_START(203),
    ACCURATE_SOTE_START(204),
    ACCURATE_XARP_START(205),
    ACCURATE_VERZIK_START(206),

    ACCURATE_MAIDEN_END(301),
    ACCURATE_BLOAT_END(302),
    ACCURATE_NYLO_END(303),
    ACCURATE_SOTE_END(304),
    ACCURATE_XARP_END(305),
    ACCURATE_VERZIK_END(306),
    IS_HARD_MODE(401),
    IS_STORY_MODE(402),

    THRALL_ATTACKED(403), // player, type

    THRALL_DAMAGED(404), // player, damage

    VENG_WAS_CAST(405), //target, source

    VENG_WAS_PROCCED(406), //player, source of veng, damage

    PLAYER_STOOD_IN_THROWN_BLOOD(411), //player, damage, blood tick
    PLAYER_STOOD_IN_SPAWNED_BLOOD(412),  //player, damage
    CRAB_HEALED_MAIDEN(413), //damage
    VERZIK_PURPLE_HEAL(701),
    VERZIK_RED_AUTO(702),
    VERZIK_THRALL_HEAL(703),
    VERZIK_PLAYER_HEAL(704),

    KODAI_BOP(501),
    DWH_BOP(502),
    BGS_WHACK(503),
    CHALLY_POKE(504),
    THRALL_SPAWN(410),
    THRALL_DESPAWN(498),
    DAWN_SPEC(487),
    DAWN_DAMAGE(488),
    MAIDEN_PLAYER_DRAINED(530),
    MAIDEN_AUTO(531),

    UPDATE_HP(576),
    ADD_NPC_MAPPING(587),
    UNKNOWN(-1);

    /*
    2:DWH //Player, 0, 0, 0, 0
    3:BGS //Player, Damage, 0, 0, 0
    4:Left tob region //Last region, 0, 0, 0, 0
    5:Player died //Player, 0, 0, 0, 0
    6:Entered new tob region //Region, 0, 0, 0, 0 // Regions: (Bloat 1, Nylo 2, Sote 3, Xarpus 4, Verzik 5)
    8: DB Specs // Player, DMG

    10: Blood Spawned //room time, 0, 0, 0, 0
    11: Crab leaked //room time, Description (E.G. N1 30s), Last known health, Current maiden dealth, 0
    12: Maiden spawned //0, 0, 0, 0, 0
    13: Maiden 70s //room time, 0, 0, 0, 0
    14: Maiden 50s //room time, 0, 0, 0, 0
    15: Maiden 30s //room time, 0, 0, 0, 0
    16: Maiden 0 hp //room time, 0, 0, 0, 0
    17: Maiden despawned //room time, 0, 0, 0, 0
    18: Matomenos spawned //position, 0, 0, 0, 0
    19: Maiden Scuffed

    20: Bloat spawned //0, 0, 0, 0, 0
    21: Bloat down //Room time, 0, 0, 0, 0
    22: Bloat 0 HP //room time, 0, 0, 0, 0
    23: Bloat despawn //room time, 0, 0, 0, 0
    24: Bloat HP on 1st down //HP, 0, 0, 0,0

    30: Nylo pillars spawned //0, 0, 0, 0 ,0
    31: Nylo stall //Wave, room time, 0, 0, 0
    32: Range split //Wave, room time, 0, 0, 0
    33: Mage split //Wave, room time, 0, 0, 0
    34: Melee split //Wave, room time, 0, 0, 0
    35: Last wave //Room time, 0, 0, 0, 0
    40: Boss spawn //Room time, 0, 0, 0, 0
    41: Melee rotation //room time, 0, 0, 0, 0
    42: Mage rotation //room time, 0, 0, 0, 0
    43: Range rotation //room time, 0, 0, 0, 0
    44: Nylo 0 HP // room time, 0, 0, 0, 0
    45: Nylo despawned // room time, 0, 0, 0, 0

    5x: sote

    60: xarpus spawned //0, 0, 0, 0, 0
    61: xarpus room started //0, 0, 0, 0, 0
    62: xarpus heal //amount, room time, 0, 0, 0
    63: xarpus screech //room time, 0, 0, 0, 0
    64: xarpus 0 hp //room time, 0, 0, 0, 0
    65: xarpus despawned //room time, 0, 0, 0, 0

    70: verzik spawned //room time, 0, 0, 0, 0
    71: verzik p1 started //0, 0, 0, 0, 0
    72: verzik p1 0 hp //room time, 0, 0, 0, 0
    73: verzik p1 despawned //room time, 0, 0, 0, 0
    80: verzik p2 reds proc // room time, 0, 0, 0, 0
    74: verzik p2 end
    75: verzik p3 0 hp
    76: verzik p3 despawned
    77: verzik bounce //player, room time, 0, 0, 0

    100: party complete
    101: party incomplete
    102: party accurate pre maiden

    201 accurate maiden start
    202 accurate bloat start
    203 accurate nylo start
    204 accurate sote start
    205 accurate xarp start
    206 accurate verzik start

    301 accurate maiden end
    302 accurate bloat end
    303 accurate nylo end
    304 accurate sote end
    305 accurate xarp end
    306 accurate verzik end
    */
    final int id;
    final String commonName;
    final TOBRoom room;

    LogID(int id)
    {
        this(id, TOBRoom.UNKNOWN, "");
    }

    LogID(int id, TOBRoom room, String commonName)
    {
        this.id = id;
        this.commonName = commonName;
        this.room = room;
    }

    public static LogID valueOf(int value)
    {
        Optional<LogID> o = Arrays.stream(values()).filter(logid -> logid.getId() == value).findFirst();
        return o.orElse(UNKNOWN);
    }
}
