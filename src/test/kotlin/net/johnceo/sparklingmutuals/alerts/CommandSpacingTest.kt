package net.johnceo.sparklingmutuals.alerts
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
class CommandSpacingTest {
 @Test fun `idle time does not permit a burst when more commands arrive`() {
  val q=CommandSpacing<String>();q.add("first");assertEquals("first",q.next(0))
  q.add("later");q.add("last");assertEquals("later",q.next(10000));assertNull(q.next(10000))
  assertEquals("last",q.next(10500))
 }
 @Test fun `all commands share a five hundred millisecond interval`() {
  val q=CommandSpacing<String>();q.add("leave");q.add("accept");q.add("pc")
  assertEquals("leave",q.next(0));assertNull(q.next(499));assertEquals("accept",q.next(500))
  assertNull(q.next(999));assertEquals("pc",q.next(1000))
 }
 @Test fun `clear removes commands from the old session`() { val q=CommandSpacing<String>();q.add("old");q.clear();assertNull(q.next(0)) }
}
