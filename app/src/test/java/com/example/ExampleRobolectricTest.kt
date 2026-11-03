package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("محرابي", appName)
  }

  @Test
  fun `verify QuranTextMatcher identifies bracket citation with Arabic digits`() {
    val input = "أَرَادَ اللَّهُ بِهَٰذَا مَثَلًا يُضِلُّ بِهِۦ كَثِيرًا وَيَهْدِى بِهِۦ كَثِيرًا وَمَا يُضِلُّ بِهِۦ إِلَّا الْفَاسِقِينَ { [سُورَةُ البَقَرَةِ: ٢٦]"
    val match = com.example.data.QuranTextMatcher.findAyahMatch(input)
    org.junit.Assert.assertNotNull(match)
    assertEquals("البقرة", match?.surahName)
    assertEquals(26, match?.ayahNumber)
    assertEquals(5, match?.pageNumber)
  }

  @Test
  fun `verify QuranTextMatcher identifies free text citation`() {
    val input = "سورة البقرة آية 26"
    val match = com.example.data.QuranTextMatcher.findAyahMatch(input)
    org.junit.Assert.assertNotNull(match)
    assertEquals("البقرة", match?.surahName)
    assertEquals(26, match?.ayahNumber)
  }
}
