package net.johnceo.sparklingmutuals.skyblock.market

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ShardReplayTest {
    private val now=MarketFixtures.NOW
    @Test fun `coarse history and too few complete windows cannot qualify colored advice`() {
        val coarse=MarketFixtures.samples(14).filterIndexed {index,_->index%24==0}.map {it.copy(resolutionMillis=7_200_000)}
        assertFalse(ShardReplay.evaluate(coarse,now-MarketFixtures.DAY).eligible)
        assertFalse(ShardReplay.evaluate(MarketFixtures.samples(8),now-MarketFixtures.DAY).eligible)
    }
    @Test fun `flat market with no hold decisions stays inconclusive despite many windows`() {
        val report=ShardReplay.evaluate(MarketFixtures.samples(14),now-MarketFixtures.DAY)
        assertTrue(report.windows.size>=20)
        assertFalse(report.eligible)
        assertEquals(0,report.training.holds)
        assertEquals(0,report.holdout.holds)
    }
    @Test fun `replay freezes targets uses no future observations and never overlaps decisions`() {
        val samples=MarketFixtures.samples(14,price={at -> if((at/MarketFixtures.HOUR)%6<3)100.0 else 110.0})
        val original=ShardReplay.evaluate(samples,now-MarketFixtures.DAY)
        val cut=now-2*MarketFixtures.DAY
        val changed=samples.map { if(it.at>cut)it.copy(ask=10_000.0)else it }
        val other=ShardReplay.evaluate(changed,now-MarketFixtures.DAY)
        assertEquals(original.windows.filter {it.at+3*MarketFixtures.HOUR<=cut},other.windows.filter {it.at+3*MarketFixtures.HOUR<=cut})
        assertTrue(original.windows.zipWithNext().all {(a,b)->b.at-a.at>=3*MarketFixtures.HOUR})
        assertTrue(original.windows.filter {it.held}.all {it.target>=100.0 && it.target<=110.0})
    }
    @Test fun `holdout excludes an unfinished day and its training outcomes cannot cross the split`() {
        val split=now-MarketFixtures.DAY
        val report=ShardReplay.evaluate(MarketFixtures.samples(14),split)
        assertTrue(report.windows.all {it.at+3*MarketFixtures.HOUR<=split || it.at>=split && it.at+3*MarketFixtures.HOUR<=split+MarketFixtures.DAY})
        assertTrue(report.holdout.total<=8)
    }
    @Test fun `acceptance boundaries reject slow touches downside and insufficient hold decisions`() {
        assertTrue(ReplayMetrics(20,10,6,90.0,1).qualifies())
        assertFalse(ReplayMetrics(20,10,5,90.0,1).qualifies())
        assertFalse(ReplayMetrics(20,10,6,90.1,1).qualifies())
        assertFalse(ReplayMetrics(20,10,6,90.0,2).qualifies())
        assertFalse(ReplayMetrics(20,4,4,1.0,0).qualifies())
    }
    @Test fun `contradictory future timestamps cannot prove a target touch`() {
        val split=now-MarketFixtures.DAY
        val points=MarketFixtures.samples(14)
        val conflicting=points.filter {it.at>=split}.map {it.copy(ask=10_000.0)}
        val report=ShardReplay.evaluate(points+conflicting,split)
        assertTrue(report.windows.none {it.at>=split},"conflicting bins must be discarded for outcomes as well as decisions")
    }
    @Test fun `successful wait median averages the middle observations`() {
        val rows=listOf(1.0,180.0).map { minutes -> ReplayWindow(now,100.0,110.0,true,minutes,0.0,110.0,minutes) }
        assertEquals(90.5,ShardReplay.metrics(rows).medianMinutes)
    }
    @Test fun `parameter selection cannot inspect held out prices`() {
        val split=now-MarketFixtures.DAY
        val samples=MarketFixtures.samples(14,price={at -> if((at/MarketFixtures.HOUR)%3==0L)99.0 else 101.0})
        val first=ShardReplay.selectTrainingParameters(samples,split)
        assertNotNull(first,"fixture must exercise a successful training selection, not null equals null")
        val changed=samples.map {if(it.at>split)it.copy(ask=99_000.0)else it}
        assertEquals(first,ShardReplay.selectTrainingParameters(changed,split))
    }
    @Test fun `untagged historical references cannot qualify despite passing retrospective metrics`() {
        val split=now-MarketFixtures.DAY
        val samples=MarketFixtures.samples(14,price={at -> if((at/MarketFixtures.HOUR)%3==0L)99.0 else 101.0})
        val verified=ShardReplay.evaluate(samples,split)
        assertTrue(verified.eligible,"known-source synthetic fixture must satisfy the statistical gate")
        val unverified=samples.map {it.copy(basis=PriceBasis.UNVERIFIED_REFERENCE,availableAt=null)}
        val research=ShardReplay.evaluate(unverified,split)
        assertTrue(research.training.qualifies())
        assertTrue(research.holdout.qualifies())
        assertFalse(research.eligible,"a public graph's untagged fallback cannot enable advice")
    }
    @Test fun `a late published observation cannot be consumed at its earlier bin close`() {
        val points=MarketFixtures.samples(14,price={at -> if((at/MarketFixtures.HOUR)%3==0L)99.0 else 101.0})
        assertTrue(ShardReplay.evaluate(points,now-MarketFixtures.DAY).eligible)
        val late=points.map {it.copy(availableAt=it.at+30*60_000)}
        val result=ShardReplay.evaluate(late,now-MarketFixtures.DAY)
        assertTrue(result.windows.isEmpty(),"30-minute delivery lag makes the 15-minute fresh-history requirement fail")
        assertFalse(result.eligible)
    }
}
