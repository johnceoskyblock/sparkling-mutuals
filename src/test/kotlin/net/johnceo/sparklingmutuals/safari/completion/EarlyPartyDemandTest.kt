package net.johnceo.sparklingmutuals.safari.completion

import java.util.UUID
import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class EarlyPartyDemandTest {
    private fun party() = PartySparklingState().apply {
        select(setOf("me")); accept(setOf("me"), mapOf("me" to SafariRoster.all.map { it.name }.toSet() - "Gimmiegold"))
    }
    @Test fun `old outside hd cannot cover newly learned starting demand`() {
        val run = SafariRun(0)
        run.visitBiome(SafariBiome.FOREST)
        run.inventoryEvidence(emptyList(), 0)
        run.partyFullClear("Party > Friend: hd")
        run.resources.partyNotice("Party > Friend: [SM] Starting items: 1x Shining Coin")
        assertFalse(run.sparklingComplete("Gimmiegold", party()))
    }
    @Test fun `outside hd cannot waive a locally held starting coin`() {
        val run = SafariRun(0)
        run.visitBiome(SafariBiome.FOREST)
        run.inventoryEvidence(listOf("Shining Coin" to 1), 0)
        run.partyFullClear("Party > Friend: hd")
        assertFalse(run.sparklingComplete("Gimmiegold", party()))
    }
    @Test fun `a single known floor slot is not a complete initial source catalog`() {
        val check = GimmiegoldSparklingCheck()
        check.floor(1, false)
        check.inventory(0)
        check.scan(1, emptyList(), true)
        assertFalse(check.complete)
    }
    @Test fun `outside Forest pickup does not become another starting food opportunity`() {
        val birds = net.johnceo.sparklingmutuals.safari.resources.SafariBirdLedger()
        birds.pickup("FLOOR DROP! You found 1x Yogi Berry on the ground!", SafariBiome.CAVERN, 10000)
        assertEquals(9, birds.feedingTarget)
    }
}
