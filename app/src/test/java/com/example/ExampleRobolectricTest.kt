package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.manager.BrightnessManager
import com.example.manager.EcoState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    assertEquals("EcoTV Pantalla", appName)
  }

  @Test
  fun `brightness manager initializes without saved brightness`() {
    assertNull(BrightnessManager.getSavedBrightness())
    assertFalse(BrightnessManager.hasSavedBrightness())
  }

  @Test
  fun `eco state tracks screen off toggle correctly`() {
    EcoState.setScreenOffActive(true)
    assertEquals(true, EcoState.isScreenOffActive.value)
    EcoState.setScreenOffActive(false)
    assertEquals(false, EcoState.isScreenOffActive.value)
  }
}

