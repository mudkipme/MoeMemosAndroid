package me.mudkip.moememos.ui

import android.Manifest
import android.location.LocationManager
import android.os.ParcelFileDescriptor
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.filters.SdkSuppress
import me.mudkip.moememos.R
import me.mudkip.moememos.data.model.MemoLocation
import me.mudkip.moememos.ui.page.memoinput.MemoLocationDialog
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class MemoLocationDialogTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun text(id: Int) = context.getString(id)

    private fun shell(command: String) {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command)).bufferedReader().use {
            assertEquals("Shell command failed: $command", "", it.readText().trim())
        }
    }

    @Test @SdkSuppress(minSdkVersion = 31)
    fun currentLocationUsesTheAndroidProvider() {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val enabled = context.getSystemService(LocationManager::class.java).isLocationEnabled
        automation.grantRuntimePermission(context.packageName, Manifest.permission.ACCESS_COARSE_LOCATION)
        automation.grantRuntimePermission(context.packageName, Manifest.permission.ACCESS_FINE_LOCATION)
        var result: MemoLocation? = null
        shell("appops set com.android.shell android:mock_location allow")
        shell("cmd location set-location-enabled true")
        shell("cmd location providers add-test-provider gps")
        shell("cmd location providers add-test-provider network")
        try {
            shell("cmd location providers set-test-provider-enabled gps true")
            shell("cmd location providers set-test-provider-enabled network false")
            compose.setContent {
                MaterialTheme { MemoLocationDialog(null, onConfirm = { result = it }, onDismiss = {}) }
            }
            compose.onNodeWithText(text(R.string.use_current_location)).performClick()
            shell("cmd location providers set-test-provider-location gps --location -1.234567,2.345678 --accuracy 1")
            compose.waitUntil(35_000) {
                compose.onAllNodesWithText("-1.234567").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText(text(R.string.save)).performClick()
            compose.runOnIdle {
                assertEquals(-1.234567, requireNotNull(result).latitude, 0.000001)
                assertEquals(2.345678, requireNotNull(result).longitude, 0.000001)
            }
        } finally {
            shell("cmd location providers remove-test-provider gps")
            shell("cmd location providers remove-test-provider network")
            shell("cmd location set-location-enabled $enabled")
            shell("appops set com.android.shell android:mock_location default")
        }
    }

    @Test fun manualCoordinatesSurviveRecreationAndSaveWithoutDevicePermission() {
        var result: MemoLocation? = null
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            MaterialTheme { MemoLocationDialog(null, onConfirm = { result = it }, onDismiss = {}) }
        }
        compose.onNodeWithText(text(R.string.save)).assertIsNotEnabled()
        compose.onNodeWithText(text(R.string.location_name)).performTextInput("Home")
        compose.onNodeWithText(text(R.string.latitude)).performTextInput("-1.234567")
        compose.onNodeWithText(text(R.string.longitude)).performTextInput("2.345678")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText(text(R.string.save)).performClick()
        compose.runOnIdle { assertEquals(MemoLocation("Home", -1.234567, 2.345678), result) }
    }

    @Test fun existingLocationCanBeRemoved() {
        var called = false
        var result: MemoLocation? = MemoLocation("Home", 0.0, 0.0)
        compose.setContent {
            MaterialTheme {
                MemoLocationDialog(result, onConfirm = { called = true; result = it }, onDismiss = {})
            }
        }
        compose.onNodeWithText(text(R.string.remove_location)).performClick()
        compose.runOnIdle { assertTrue(called); assertNull(result) }
    }
}
