package net.johnceo.sparklingmutuals.skyblock.market

import java.util.concurrent.Executor
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class WatchSaveQueueTest {
    @Test fun `unchanged snapshots retry after failure and are acknowledged only after successful save`() {
        val work=ArrayDeque<Runnable>();var now=0L;var attempts=0;val saved=mutableListOf<String>()
        val queue=WatchSaveQueue(Executor {work.add(it)},{raw ->attempts++;if(attempts==1)false else {saved.add(raw);true}},{now})
        queue.offer("same");work.removeFirst().run();assertEquals(1,attempts)
        queue.offer("same");assertTrue(work.isEmpty())
        now=14999;queue.offer("same");assertTrue(work.isEmpty())
        now=15000;queue.offer("same");assertEquals(1,work.size);work.removeFirst().run()
        assertEquals(listOf("same"),saved)
        now+=30000;queue.offer("same");assertTrue(work.isEmpty());assertEquals(2,attempts)
    }
    @Test fun `exceptions release scheduling flag and latest snapshot replaces a failed older one`() {
        val work=ArrayDeque<Runnable>();var now=0L;var attempts=0;val saved=mutableListOf<String>()
        val queue=WatchSaveQueue(Executor {work.add(it)},{raw ->if(attempts++==0)error("disk unavailable");saved.add(raw);true},{now})
        queue.offer("old");work.removeFirst().run()
        repeat(100){queue.offer("latest")};assertTrue(work.isEmpty())
        now=15000;queue.offer("latest");queue.offer("newest");assertEquals(1,work.size)
        work.removeFirst().run();assertEquals(listOf("newest"),saved)
    }
}
