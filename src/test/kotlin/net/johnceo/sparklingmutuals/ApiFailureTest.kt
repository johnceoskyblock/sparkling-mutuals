package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.api.ApiFailure
import net.johnceo.sparklingmutuals.api.ApiFailureKind
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ApiFailureTest {
    @Test
    fun `invalid credentials are distinct from a request limit`() {
        assertEquals(ApiFailureKind.INVALID_KEY, ApiFailure.fromResponse(403, "{}")?.kind)
        assertEquals(ApiFailureKind.INVALID_KEY, ApiFailure.fromResponse(200,
            """{"success":false,"cause":"Invalid API key"}""")?.kind)
        assertEquals(ApiFailureKind.RATE_LIMIT, ApiFailure.fromResponse(429, "{}")?.kind)
        assertEquals(ApiFailureKind.RATE_LIMIT, ApiFailure.fromResponse(200,
            """{"success":false,"throttle":true}""")?.kind)
    }

    @Test
    fun `malformed and failed responses never become empty discoveries`() {
        assertEquals(ApiFailureKind.UNAVAILABLE, ApiFailure.fromResponse(503, "upstream unavailable")?.kind)
        assertEquals(ApiFailureKind.UNAVAILABLE, ApiFailure.fromResponse(200, "not json")?.kind)
        assertEquals(ApiFailureKind.UNAVAILABLE, ApiFailure.fromResponse(200, """{"success":false}""")?.kind)
        assertNull(ApiFailure.fromResponse(200, """{"success":true,"profiles":[]}"""))
        assertEquals(ApiFailureKind.DATA_UNAVAILABLE, ApiFailure.dataUnavailable().kind)
    }

    @Test
    fun `server supplied causes cannot expose secrets or send arbitrary chat`() {
        val failure = ApiFailure.fromResponse(403, """{"cause":"secret-key pc forged text"}""")!!
        assertFalse(failure.message!!.contains("secret-key"))
        assertFalse(failure.message!!.contains("forged"))
    }
}
