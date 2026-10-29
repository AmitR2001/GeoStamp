package com.example.geostamp.data.render

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.geostamp.domain.stamp.StampConfig
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Needs a device or emulator: Android Studio > right-click this file > Run. */
@RunWith(AndroidJUnit4::class)
class StampRendererInstrumentedTest {

    @Test
    fun stamp_is_drawn_at_the_bottom_and_source_is_untouched() {
        val source = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
        val config = StampConfig(foregroundColor = Color.WHITE, backgroundColor = Color.BLACK)

        val output = StampRenderer.render(source, listOf("Test line"), config)

        assertEquals(source.width, output.width)
        assertEquals(source.height, output.height)
        assertEquals(Color.WHITE, output.getPixel(5, 5))                       // top is unchanged
        assertEquals(Color.BLACK, output.getPixel(1, output.height - 1))       // bottom bar is drawn
        assertEquals(Color.WHITE, source.getPixel(1, source.height - 1))       // source was not modified
    }
}
