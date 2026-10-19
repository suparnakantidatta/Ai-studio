package com.example

import com.example.util.UpdateManager
import org.junit.Assert.*
import org.junit.Test

class UpdateManagerTest {

  @Test
  fun testGitHubUpdateUrl() {
    assertEquals(
      "https://github.com/suparnakantidatta/Ai-studio/releases/latest/download/Pixel_pathsala_1.0.0.1.apk",
      UpdateManager.GITHUB_UPDATE_URL
    )
    assertEquals("1.0.0.1", UpdateManager.LATEST_VERSION)
    assertEquals("1.0.0", UpdateManager.CURRENT_VERSION)
    assertTrue(UpdateManager.isUpdateAvailable())
    assertTrue(UpdateManager.RELEASE_CHANGELOG.isNotEmpty())
  }
}
