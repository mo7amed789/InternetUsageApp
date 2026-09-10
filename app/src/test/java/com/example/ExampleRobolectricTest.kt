package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.NetworkUtils
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
    assertEquals("DataPulse", appName)
  }

  @Test
  fun `test byte formatting`() {
    assertEquals("0 B", NetworkUtils.formatBytes(0L))
    assertEquals("1 KB", NetworkUtils.formatBytes(1024L))
    assertEquals("1 MB", NetworkUtils.formatBytes(1024L * 1024L))
  }
}
