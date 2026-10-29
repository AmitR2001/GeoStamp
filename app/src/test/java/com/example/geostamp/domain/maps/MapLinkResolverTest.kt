package com.example.geostamp.domain.maps

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class MapLinkResolverTest {

    private class FakeRedirects(private val map: Map<String, String?>, private val fail: Boolean = false) : RedirectSource {
        val requested = mutableListOf<String>()
        override suspend fun nextLocation(url: String): String? {
            requested.add(url)
            if (fail) throw IOException("offline")
            return map[url]
        }
    }

    @Test
    fun text_with_coordinates_never_uses_the_internet() = runBlocking {
        val fake = FakeRedirects(emptyMap())
        val result = MapLinkResolver(fake).resolve("12.9716, 77.5946")
        assertTrue(result is MapLookup.Found)
        assertFalse((result as MapLookup.Found).usedInternet)
        assertTrue(fake.requested.isEmpty())
    }

    @Test
    fun short_link_is_followed_to_a_long_link() = runBlocking {
        val fake = FakeRedirects(
            mapOf("https://maps.app.goo.gl/abc" to "https://www.google.com/maps/place/Home/@12.9716,77.5946,17z")
        )
        val result = MapLinkResolver(fake).resolve("Home\nhttps://maps.app.goo.gl/abc")
        assertTrue(result is MapLookup.Found)
        val found = result as MapLookup.Found
        assertTrue(found.usedInternet)
        assertEquals(12.9716, found.location.latitude, 0.000001)
        assertEquals("Home", found.location.name)
    }

    @Test
    fun several_redirects_and_a_consent_page_are_handled() = runBlocking {
        val fake = FakeRedirects(
            mapOf(
                "https://maps.app.goo.gl/abc" to "https://consent.google.com/m?continue=https%3A%2F%2Fwww.google.com%2Fmaps%2Fplace%2FX",
                "https://www.google.com/maps/place/X" to "https://www.google.com/maps/place/X/@1.5,2.5,17z"
            )
        )
        val result = MapLinkResolver(fake).resolve("https://maps.app.goo.gl/abc")
        assertTrue(result is MapLookup.Found)
        assertEquals(1.5, (result as MapLookup.Found).location.latitude, 0.000001)
    }

    @Test
    fun redirect_to_an_untrusted_site_is_not_followed() = runBlocking {
        val fake = FakeRedirects(mapOf("https://maps.app.goo.gl/abc" to "https://evil.example.com/steal"))
        val result = MapLinkResolver(fake).resolve("https://maps.app.goo.gl/abc")
        assertTrue(result is MapLookup.Failed)
        assertEquals(listOf("https://maps.app.goo.gl/abc"), fake.requested)
    }

    @Test
    fun offline_gives_a_friendly_failure() = runBlocking {
        val result = MapLinkResolver(FakeRedirects(emptyMap(), fail = true)).resolve("https://maps.app.goo.gl/abc")
        assertTrue(result is MapLookup.Failed)
        assertTrue((result as MapLookup.Failed).message.contains("internet"))
    }

    @Test
    fun a_short_link_with_no_redirect_fails_cleanly() = runBlocking {
        val result = MapLinkResolver(FakeRedirects(emptyMap())).resolve("https://maps.app.goo.gl/abc")
        assertTrue(result is MapLookup.Failed)
    }

    @Test
    fun text_without_anything_useful_fails_without_network() = runBlocking {
        val fake = FakeRedirects(emptyMap())
        val result = MapLinkResolver(fake).resolve("see you there")
        assertTrue(result is MapLookup.Failed)
        assertTrue(fake.requested.isEmpty())
    }

    @Test
    fun a_redirect_loop_stops() = runBlocking {
        val fake = FakeRedirects(mapOf("https://maps.app.goo.gl/abc" to "https://maps.app.goo.gl/abc"))
        val result = MapLinkResolver(fake).resolve("https://maps.app.goo.gl/abc")
        assertTrue(result is MapLookup.Failed)
        assertTrue(fake.requested.size <= 6)
    }
}
