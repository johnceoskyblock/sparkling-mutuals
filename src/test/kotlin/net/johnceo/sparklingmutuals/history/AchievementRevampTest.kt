package net.johnceo.sparklingmutuals.history

import java.time.ZoneOffset
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.resources.SafariBirdLedger
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AchievementRevampTest {
    @Test fun `catalog goals and visible hover text match the revised descriptions`() {
        fun goals(measure: Measure)=Achievements.definitions.filter { it.measure==measure }.map { it.goal }.sorted()
        assertEquals(79,Achievements.definitions.size)
        assertEquals(listOf(25L,50L,75L,100L,150L),goals(Measure.DRY_SPARKLING))
        assertEquals(listOf(10L,25L,50L,75L,100L,200L),goals(Measure.DUPLICATES))
        assertEquals(listOf(10L,20L,30L),goals(Measure.RUNS_SINCE_MACAW))
        assertEquals(listOf(50L,250L,500L,2000L,4000L),goals(Measure.RUNS))
        assertEquals(listOf(25L,50L,100L,200L,400L,1000L),goals(Measure.HOURS))
        assertEquals(listOf(10000L,50000L,100000L,250000L,500000L),goals(Measure.CAUGHT))
        assertEquals(listOf(75L,150L),goals(Measure.DAILY_RUNS))
        assertEquals(listOf(1L,3L,8L),goals(Measure.DYES))
        assertEquals((2L..10L).toList(),goals(Measure.DEX_SETS))
        assertEquals(setOf("CJ Luck","This Is Water… Use It.","Double Trouble"),Achievements.definitions.filter { it.secret }.map { it.name }.toSet())
        assertTrue(Achievements.definitions.filter { it.measure!=Measure.RUNS_SINCE_MACAW }.all { Achievements.explanation(it).isEmpty() })
        val goal=Achievements.definitions.associate { it.id to it.goal }
        assertEquals(listOf(150000L,120000L,90000L),listOf("speed_3","speed_230","speed_130").map(goal::get))
        assertEquals(20L,goal["ten_of_37"]);assertEquals(5L,goal["instant"])
        assertEquals(listOf(4L,6L,8L),listOf("macaw_3","macaw_4","macaw_8").map(goal::get))
    }
    @Test fun `rarities and secret flags match the requested catalog`() {
        val expected=javaClass.getResourceAsStream("/achievement-rarities.txt")!!.bufferedReader().use { it.readLines() }
            .filter { it.isNotBlank() }.associate { line -> val p=line.split('|');p[0] to (p[1] to p[2].toBoolean()) }
        assertEquals(expected.keys,Achievements.definitions.map { it.name }.toSet())
        Achievements.definitions.forEach { a -> assertEquals(expected.getValue(a.name),a.tier.name to a.secret,a.name) }
    }
    @Test fun `Double Trouble requires two personal same-type catches within one run`() {
        assertTrue(AchievementRules.doubleTrouble(listOf("Me:Foxtrot","Friend:Gazer","me:Foxtrot"),"Me"))
        assertFalse(AchievementRules.doubleTrouble(listOf("Me:Foxtrot","Me:Gazer"),"Me"))
        assertFalse(AchievementRules.doubleTrouble(listOf("Me:Foxtrot","Friend:Foxtrot"),"Me"))
        assertFalse(AchievementRules.doubleTrouble(listOf("Friend:Foxtrot","Friend:Foxtrot"),"Me"))
    }
    @Test fun `duplicate streak counts credited catches rather than runs and resets on a new type`() {
        val entries=(1L..25).map { SparklingEntry(at=it,player="Friend",critter="Foxtrot") }
        assertEquals(AchievementRules.Streak(25,25),AchievementRules.duplicateStreak(entries))
        val next=entries+SparklingEntry(at=26,player="Other",critter="Gazer",unique=true)+
            SparklingEntry(at=27,player="Other",critter="Gazer")
        assertEquals(AchievementRules.Streak(1,25),AchievementRules.duplicateStreak(next))
        val counts=AchievementRules.dexCounts(SafariRoster.all.flatMap { c -> listOf(
            SparklingEntry(player="Me",critter=c.name),SparklingEntry(player="Friend",critter=c.name)) })
        assertEquals(2,AchievementRules.fullDexCount(counts))
        assertEquals(37,AchievementRules.dex(counts,2).size)
        assertTrue(AchievementRules.dex(counts,3).isEmpty())
    }
    @Test fun `nine floor foods must produce birds and leave neither inventory nor feeder work`() {
        val birds=SafariBirdLedger()
        birds.inventory(emptyList());birds.report()
        repeat(9) { birds.spawn("A Bluebird was attracted to the Birdfeeder!") }
        assertFalse(birds.allNineFloorFoodUsed,"a report alone cannot prove the floor drops were exhausted")
        birds.floor(0);assertTrue(birds.allNineFloorFoodUsed)
        birds.feeder("Bag of Seeds",1);assertFalse(birds.allNineFloorFoodUsed)
        birds.feeder(null,0);birds.inventory(listOf("Yogi Berry" to 1));assertFalse(birds.allNineFloorFoodUsed)
        birds.inventory(emptyList());assertTrue(birds.allNineFloorFoodUsed)
    }
    @Test fun `Macaw drought rejects unverified runs and uncaught spawned Macaws`() {
        fun checked(start: Long)=RunRecord(start=start,allBirdFoodUsed=true,macawsSeen=0)
        assertEquals(AchievementRules.Streak(2,2),AchievementRules.macawDrought(listOf(checked(1),checked(2))))
        assertEquals(0,AchievementRules.macawDrought(listOf(checked(1),RunRecord(start=2,allBirdFoodUsed=true,macawsSeen=2))).current)
        assertEquals(0,AchievementRules.macawDrought(listOf(checked(1),RunRecord(start=2,macawChecked=true))).current)
    }
    @Test fun `daily milestones exclude exactly two minutes and group by completion date`() {
        val valid=(0L until 74).map { RunRecord(start=it*180000,length=120001) }
        val twoMinutes=RunRecord(start=14000000,length=120000)
        assertEquals(74,AchievementRules.mostDailyRuns(valid+twoMinutes,ZoneOffset.UTC))
        assertEquals(75,AchievementRules.mostDailyRuns(valid+RunRecord(start=14000000,length=120001),ZoneOffset.UTC))
    }
    @Test fun `personal sparkling gaps use runs instead of ticket counts and include the active run`() {
        val runs=(0L..5L).map { RunRecord(start=it*1000,length=999,tickets=50) }
        val own=listOf(SparklingEntry(at=10,player="Me",critter="Foxtrot"),SparklingEntry(at=5010,player="Me",critter="Gazer"))
        assertEquals(listOf(5),AchievementRules.personalSparklingGaps(runs,own))
        assertEquals(listOf(6),AchievementRules.personalSparklingGaps(runs,own.take(1)+SparklingEntry(at=6010,critter="Gazer"),6000L to 6999L))
    }
    @Test fun `migration rechecks changed requirements and retains unchanged proven unlocks`() {
        val saved=Achievements.Data(initialised=true,unlocked=linkedMapOf("run_10" to 1,"dedication" to 2,"party_double" to 3,"emerald_dye" to 4))
        assertTrue(Achievements.migrateRequirements(saved))
        assertEquals(setOf("party_double","emerald_dye"),saved.unlocked.keys)
        assertEquals(1,saved.emeraldDyes)
        assertFalse(Achievements.migrateRequirements(saved))
    }
    @Test fun `two rapid dye drops count twice while each matching broadcast is deduplicated`() {
        val proof=DyeDropProofs()
        assertTrue(proof.accept("RARE DROP! Emerald Dye",0))
        assertTrue(proof.accept("RARE DROP! Emerald Dye",10))
        assertFalse(proof.accept("WOW! You found Emerald Dye #1!",20))
        assertFalse(proof.accept("WOW! You found Emerald Dye #2!",30))
    }
}
