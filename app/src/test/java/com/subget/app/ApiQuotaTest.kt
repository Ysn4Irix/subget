package com.subget.app

import com.subget.app.data.api.SubdlApiService
import com.subget.app.data.api.models.ApiQuotaInfo
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiQuotaTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Test
    fun testApiQuotaCalculations() {
        val quota = ApiQuotaInfo(
            limit = 2000,
            remaining = 1893,
            resetEpochSeconds = 1790100000L,
            plan = "Free"
        )

        assertEquals(107, quota.used)
        assertEquals(107f / 2000f, quota.percentage, 0.0001f)
        assertFalse(quota.isLow)
        assertFalse(quota.isExhausted)
        assertNotNull(quota.formattedResetTime)
    }

    @Test
    fun testApiQuotaLowAndExhaustedConditions() {
        val normalQuota = ApiQuotaInfo(limit = 2000, remaining = 500)
        assertFalse(normalQuota.isLow)
        assertFalse(normalQuota.isExhausted)

        // Low by remaining count (<= 250)
        val lowByCount = ApiQuotaInfo(limit = 2000, remaining = 200)
        assertTrue(lowByCount.isLow)
        assertFalse(lowByCount.isExhausted)

        // Low by percentage (>= 80% used)
        val lowByPercentage = ApiQuotaInfo(limit = 1000, remaining = 150)
        assertTrue(lowByPercentage.isLow)
        assertFalse(lowByPercentage.isExhausted)

        // Exhausted (remaining <= 0)
        val exhaustedQuota = ApiQuotaInfo(limit = 2000, remaining = 0)
        assertTrue(exhaustedQuota.isLow)
        assertTrue(exhaustedQuota.isExhausted)
        assertEquals(2000, exhaustedQuota.used)
        assertEquals(1.0f, exhaustedQuota.percentage, 0.0001f)
    }

    @Test
    fun testApiQuotaResetCountdownText() {
        val now = System.currentTimeMillis() / 1000

        val inTwoHours = ApiQuotaInfo(limit = 2000, remaining = 100, resetEpochSeconds = now + 7200 + 300)
        assertTrue(inTwoHours.resetCountdownText?.startsWith("in 2h") == true)

        val inTwentyMinutes = ApiQuotaInfo(limit = 2000, remaining = 100, resetEpochSeconds = now + 1200)
        assertTrue(inTwentyMinutes.resetCountdownText?.startsWith("in 20m") == true)

        val pastReset = ApiQuotaInfo(limit = 2000, remaining = 100, resetEpochSeconds = now - 60)
        assertEquals("Resets soon", pastReset.resetCountdownText)

        val noReset = ApiQuotaInfo(limit = 2000, remaining = 100, resetEpochSeconds = null)
        assertNull(noReset.resetCountdownText)
    }

    @Test
    fun testApiQuotaSerialization() {
        val quota = ApiQuotaInfo(
            limit = 2000,
            remaining = 1500,
            resetEpochSeconds = 1790100000L,
            plan = "Pro"
        )

        val serialized = json.encodeToString(quota)
        val deserialized = json.decodeFromString<ApiQuotaInfo>(serialized)

        assertEquals(quota.limit, deserialized.limit)
        assertEquals(quota.remaining, deserialized.remaining)
        assertEquals(quota.resetEpochSeconds, deserialized.resetEpochSeconds)
        assertEquals(quota.plan, deserialized.plan)
        assertEquals(quota.used, deserialized.used)
    }

    @Test
    fun testExtractQuotaFromHttpHeaders() {
        val apiService = SubdlApiService()
        val dummyRequest = Request.Builder().url("https://api.subdl.com/api/v1/subtitles").build()

        // Free tier response
        val freeResponse = Response.Builder()
            .request(dummyRequest)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .header("X-RateLimit-Limit", "2000")
            .header("X-RateLimit-Remaining", "1893")
            .header("X-RateLimit-Reset", "1790100000")
            .build()

        val freeQuota = apiService.extractQuota(freeResponse)
        assertNotNull(freeQuota)
        assertEquals(2000, freeQuota?.limit)
        assertEquals(1893, freeQuota?.remaining)
        assertEquals(107, freeQuota?.used)
        assertEquals(1790100000L, freeQuota?.resetEpochSeconds)
        assertEquals("Free", freeQuota?.plan)

        // Pro tier response
        val proResponse = Response.Builder()
            .request(dummyRequest)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .header("X-RateLimit-Limit", "30000")
            .header("X-RateLimit-Remaining", "29500")
            .header("X-RateLimit-Reset", "1790100000")
            .build()

        val proQuota = apiService.extractQuota(proResponse)
        assertNotNull(proQuota)
        assertEquals(30000, proQuota?.limit)
        assertEquals(29500, proQuota?.remaining)
        assertEquals("Pro", proQuota?.plan)

        // Missing headers
        val missingHeadersResponse = Response.Builder()
            .request(dummyRequest)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .build()

        assertNull(apiService.extractQuota(missingHeadersResponse))
    }
}
