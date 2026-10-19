package net.johnceo.sparklingmutuals.api

import net.johnceo.sparklingmutuals.config.ConfigManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ApiRetryWindowTest {
    @Test fun `captured provider remains stable across settings changes without exposing credentials`() {
        val cloud = ApiProvider.capture("", true)
        val disabled = ApiProvider.capture("", false)
        val direct = ApiProvider.capture("test-credential", true)
        assertNotEquals(cloud, disabled)
        assertTrue(cloud.enabled)
        assertFalse(disabled.enabled)
        assertEquals(direct, ApiProvider.capture("test-credential", false))
        assertFalse(direct.toString().contains("test-credential"))
        val retry = ApiRetryWindow()
        retry.failed(1000, disabled)
        assertFalse(retry.due(1001, disabled))
        assertTrue(retry.due(1001, cloud))
    }
    @Test fun `enabling cloud wakes a failed lookup with the same blank key`() {
        val saved = ConfigManager.useCloudApi
        try {
            ConfigManager.useCloudApi = false
            val retry = ApiRetryWindow()
            retry.failed(1000, "")
            assertFalse(retry.due(1001, ""))
            ConfigManager.useCloudApi = true
            assertTrue(retry.due(1001, ""))
        } finally { ConfigManager.useCloudApi = saved }
    }
    @Test fun `successful operations do not cancel unrelated failed functions`() {
        val tickets = ApiRetryWindow(); val discoveries = ApiRetryWindow()
        tickets.failed(0, "key"); discoveries.failed(1000, "key")
        tickets.reset()
        assertFalse(tickets.due(61000, "key"))
        assertTrue(discoveries.due(61000, "key"))
    }
    @Test fun `failure retries at one minute and reschedules from the latest failure without mode changes`() {
        val retry = ApiRetryWindow()
        assertFalse(retry.pending)
        retry.failed(1000, "old")
        assertFalse(retry.due(60999, "old"))
        assertTrue(retry.due(61000, "old"))
        retry.failed(61500, "old")
        assertFalse(retry.due(121499, "old"))
        assertTrue(retry.due(121500, "old"))
    }
    @Test fun `key updates wake only failed operations and reset cancels retries`() {
        val retry = ApiRetryWindow()
        assertFalse(retry.due(0, "new"))
        retry.failed(1000, "expired")
        assertTrue(retry.due(1001, "replacement"))
        retry.reset()
        assertFalse(retry.pending)
        assertFalse(retry.due(999999, "replacement"))
        retry.failed(1000, "")
        assertTrue(retry.due(1001, "configured"))
    }
}
