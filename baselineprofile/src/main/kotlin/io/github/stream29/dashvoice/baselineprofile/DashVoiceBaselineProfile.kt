package io.github.stream29.dashvoice.baselineprofile

import android.os.SystemClock
import android.view.KeyEvent
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import java.util.regex.Pattern

class DashVoiceBaselineProfile {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val previousInputMethod = device
            .executeShellCommand("settings get secure default_input_method")
            .trim()
            .takeIf { it.contains('/') }

        try {
            prepareProfileConfiguration(device)
            baselineProfileRule.collect(
                packageName = PACKAGE_NAME,
                includeInStartupProfile = true,
            ) {
                exerciseVoiceImeStartup(previousInputMethod)
            }
        } finally {
            previousInputMethod?.let {
                device.executeShellCommand("ime set $it")
            }
        }
    }

    private fun prepareProfileConfiguration(device: UiDevice) {
        device.executeShellCommand(
            "pm grant $PACKAGE_NAME android.permission.RECORD_AUDIO",
        )
        device.executeShellCommand(
            "am start -W -n $PACKAGE_NAME/.MainActivity",
        )
        device.waitForIdle()

        check(
            device.wait(
                Until.hasObject(By.clazz("android.widget.EditText")),
                UI_TIMEOUT_MILLIS,
            ),
        ) {
            "Unable to find the DashVoice configuration fields"
        }
        val fields = device.findObjects(By.clazz("android.widget.EditText"))
        check(fields.size >= 3) {
            "Expected at least three DashVoice configuration fields"
        }
        fields[0].text = PROFILE_API_KEY
        fields[1].text = PROFILE_BASE_URL
        device.pressKeyCode(KeyEvent.KEYCODE_TAB)
        device.pressBack()
        device.waitForIdle()
        SystemClock.sleep(CONFIGURATION_SAVE_MILLIS)
        device.executeShellCommand("am force-stop $PACKAGE_NAME")
        device.executeShellCommand(
            "am start -W -n $PACKAGE_NAME/.MainActivity",
        )
        device.waitForIdle()
        check(
            device.wait(
                Until.hasObject(By.text(Pattern.compile("Ready|可用"))),
                UI_TIMEOUT_MILLIS,
            ),
        ) {
            "DashVoice profile configuration was not persisted"
        }
        device.pressHome()
        device.waitForIdle()
    }

    private fun MacrobenchmarkScope.exerciseVoiceImeStartup(previousInputMethod: String?) {
        previousInputMethod?.let {
            device.executeShellCommand("ime set $it")
        }
        device.executeShellCommand("am force-stop $PACKAGE_NAME")
        device.executeShellCommand("ime enable $DASHVOICE_INPUT_METHOD")
        device.executeShellCommand("am start -W -a android.settings.SETTINGS")
        device.waitForIdle()

        val searchEntry = device.wait(
            Until.findObject(By.res("android", "input")),
            UI_TIMEOUT_MILLIS,
        ) ?: error("Unable to find the Settings search entry")
        searchEntry.click()
        device.waitForIdle()

        device.executeShellCommand("ime set $DASHVOICE_INPUT_METHOD")
        waitForDashVoiceProcess()
        SystemClock.sleep(RECOGNITION_WARMUP_MILLIS)
        previousInputMethod?.let {
            // Profile collection flushes after five seconds. Switch away before
            // it force-stops the selected IME, otherwise Android restarts it.
            device.executeShellCommand(
                "(sleep $PROFILE_FLUSH_DELAY_SECONDS; ime set $it) " +
                    ">/dev/null 2>&1 &",
            )
        }
    }

    private fun MacrobenchmarkScope.waitForDashVoiceProcess() {
        val deadline = SystemClock.elapsedRealtime() + PROCESS_TIMEOUT_MILLIS
        while (SystemClock.elapsedRealtime() < deadline) {
            if (device.executeShellCommand("pidof $PACKAGE_NAME").isNotBlank()) {
                return
            }
            SystemClock.sleep(PROCESS_POLL_MILLIS)
        }
        error("DashVoice process did not start")
    }

    private companion object {
        const val PACKAGE_NAME = "io.github.stream29.dashvoice"
        const val DASHVOICE_INPUT_METHOD =
            "$PACKAGE_NAME/.ime.DashVoiceInputMethodService"
        const val UI_TIMEOUT_MILLIS = 5_000L
        const val PROCESS_TIMEOUT_MILLIS = 5_000L
        const val PROCESS_POLL_MILLIS = 50L
        const val RECOGNITION_WARMUP_MILLIS = 1_000L
        const val CONFIGURATION_SAVE_MILLIS = 1_000L
        const val PROFILE_FLUSH_DELAY_SECONDS = "5.5"
        const val PROFILE_API_KEY = "baseline-profile-key"
        const val PROFILE_BASE_URL =
            "wss://192.0.2.1/api-ws/v1/realtime"
    }
}
