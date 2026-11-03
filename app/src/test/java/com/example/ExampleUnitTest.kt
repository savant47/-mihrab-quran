package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun verifyOfflineQuranJsonAssetIntegrity() {
    val file = java.io.File("src/main/assets/quran_full_offline.json")
    assertTrue("Offline Quran JSON asset must exist", file.exists())
    val content = file.readText(Charsets.UTF_8)
    assertTrue(content.contains("الفاتحة"))
    assertTrue(content.contains("البقرة"))
    assertTrue(content.contains("الناس"))
    assertEquals(114, Regex("\"num\":").findAll(content).count())
    assertEquals(6236, Regex("\"tf\":").findAll(content).count())
  }
}
