package com.example.geostamp.domain.maps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapLinkParserTest {

    private fun assertAt(text: String, lat: Double, lon: Double) {
        val result = MapLinkParser.parse(text)
        assertNotNull("no location found in: $text", result)
        assertEquals(lat, result!!.latitude, 0.000001)
        assertEquals(lon, result.longitude, 0.000001)
    }

    @Test fun plain_with_comma() = assertAt("12.9716, 77.5946", 12.9716, 77.5946)
    @Test fun plain_with_space_only() = assertAt("12.9716 77.5946", 12.9716, 77.5946)
    @Test fun plain_without_spaces() = assertAt("12.9716,77.5946", 12.9716, 77.5946)
    @Test fun plain_in_brackets() = assertAt("(12.9716, 77.5946)", 12.9716, 77.5946)
    @Test fun plain_negative_values() = assertAt("-33.8688, 151.2093", -33.8688, 151.2093)
    @Test fun degrees_with_hemisphere_letters() = assertAt("12.9716° N, 77.5946° E", 12.9716, 77.5946)
    @Test fun letters_directly_after_numbers() = assertAt("33.8688S 151.2093E", -33.8688, 151.2093)
    @Test fun letters_before_numbers() = assertAt("N 12.9716 E 77.5946", 12.9716, 77.5946)
    @Test fun west_is_negative() = assertAt("40.7128 N, 74.0060 W", 40.7128, -74.0060)

    @Test
    fun degrees_minutes_seconds_as_shown_by_google_maps() =
        assertAt("12°58'18.0\"N 77°35'40.6\"E", 12.971667, 77.594611)

    @Test
    fun full_google_maps_place_link_prefers_the_pin_over_the_view() {
        val link = "https://www.google.com/maps/place/Cubbon+Park/@12.9763,77.5929,17z/data=!3m1!4b1!4m6!3m5!1s0x0:0x1!8m2!3d12.9779!4d77.5952"
        val result = MapLinkParser.parse(link)!!
        assertEquals(12.9779, result.latitude, 0.000001)
        assertEquals(77.5952, result.longitude, 0.000001)
        assertEquals("Cubbon Park", result.name)
    }

    @Test fun at_link_without_pin() =
        assertAt("https://www.google.com/maps/@12.9716,77.5946,15z", 12.9716, 77.5946)

    @Test fun q_parameter() = assertAt("https://maps.google.com/?q=12.9716,77.5946", 12.9716, 77.5946)
    @Test fun ll_parameter() = assertAt("https://maps.google.com/maps?ll=12.9716,77.5946&z=15", 12.9716, 77.5946)
    @Test fun q_parameter_url_encoded_comma() = assertAt("https://www.google.com/maps?q=12.9716%2C77.5946", 12.9716, 77.5946)
    @Test fun q_with_loc_prefix() = assertAt("https://maps.google.com/?q=loc:12.9716,77.5946", 12.9716, 77.5946)
    @Test fun geo_uri() = assertAt("geo:12.9716,77.5946?z=17", 12.9716, 77.5946)

    @Test
    fun q_with_a_name_in_brackets_gives_the_name() {
        val result = MapLinkParser.parse("geo:0,0?q=12.9716,77.5946(Office)".replace("geo:0,0", "https://maps.google.com/"))!!
        assertEquals("Office", result.name)
    }

    @Test
    fun shared_text_with_a_name_and_a_long_link() {
        val shared = "Cubbon Park\nhttps://www.google.com/maps/place/Cubbon+Park/@12.9763,77.5929,17z"
        assertAt(shared, 12.9763, 77.5929)
    }

    @Test
    fun trailing_punctuation_after_a_link_is_ignored() =
        assertAt("See https://www.google.com/maps/@12.9716,77.5946,15z.", 12.9716, 77.5946)

    @Test
    fun short_links_alone_have_no_coordinates() {
        assertNull(MapLinkParser.parse("https://maps.app.goo.gl/AbCdEf123"))
        assertEquals("https://maps.app.goo.gl/AbCdEf123", MapLinkParser.shortLink("Place\nhttps://maps.app.goo.gl/AbCdEf123"))
    }

    @Test
    fun nonsense_and_out_of_range_values_are_rejected() {
        assertNull(MapLinkParser.parse(""))
        assertNull(MapLinkParser.parse("hello there"))
        assertNull(MapLinkParser.parse("123.5, 77.5"))
        assertNull(MapLinkParser.parse("12.5, 190.5"))
        assertNull(MapLinkParser.parse("0, 0"))
        assertNull(MapLinkParser.parse("12.5"))
        assertNull(MapLinkParser.parse("1, 2, 3"))
    }

    @Test
    fun words_ending_in_direction_letters_are_not_hemispheres() {
        assertAt("Lane 12.9716, 77.5946", 12.9716, 77.5946)
    }

    @Test
    fun only_google_https_links_are_trusted() {
        assertTrue(MapLinkParser.isTrustedMapUrl("https://maps.app.goo.gl/abc"))
        assertTrue(MapLinkParser.isTrustedMapUrl("https://www.google.com/maps/place/x"))
        assertTrue(MapLinkParser.isTrustedMapUrl("https://www.google.co.in/maps/place/x"))
        assertTrue(MapLinkParser.isTrustedMapUrl("https://consent.google.com/ml?continue=x"))
        assertFalse(MapLinkParser.isTrustedMapUrl("http://maps.app.goo.gl/abc"))
        assertFalse(MapLinkParser.isTrustedMapUrl("https://evil.example.com/maps"))
        assertFalse(MapLinkParser.isTrustedMapUrl("https://google.com.evil.example/maps"))
        assertFalse(MapLinkParser.isTrustedMapUrl("not a url"))
    }

    @Test
    fun consent_pages_are_unwrapped() {
        val wrapped = "https://consent.google.com/m?continue=https%3A%2F%2Fwww.google.com%2Fmaps%2F%4012.9716%2C77.5946%2C15z&gl=IN"
        val inner = MapLinkParser.unwrapConsent(wrapped)
        assertTrue(inner.startsWith("https://www.google.com/maps/"))
        assertEquals("https://www.google.com/maps/@12.9716,77.5946,15z", inner)
    }
}
