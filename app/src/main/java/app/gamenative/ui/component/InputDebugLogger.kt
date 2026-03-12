package app.gamenative.ui.component

import android.view.InputDevice
import android.view.KeyEvent
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.winlator.xserver.XKeycode

object InputDebugLogger {

    data class DebugEvent(
        val timestamp: Long = System.currentTimeMillis(),
        val type: EventType,
        val source: String,
        val details: String,
        val mapped: Boolean,
    )

    enum class EventType {
        KEY_DOWN,
        KEY_UP,
        MOUSE_MOVE,
        MOUSE_BUTTON,
        TOUCH,
        GAMEPAD,
        WINE_KEY,
        WINE_MOUSE,
        WINE_LAYOUT,
        WINE_INFO,
    }

    private const val MAX_EVENTS = 300

    val events = mutableStateListOf<DebugEvent>()
    val isEnabled = mutableStateOf(false)
    val keyboardEventCount = mutableStateOf(0)
    val mouseEventCount = mutableStateOf(0)
    val unmappedKeyCount = mutableStateOf(0)
    val wineEventCount = mutableStateOf(0)
    val wineDebugStatus = mutableStateOf("unknown")
    val detectedLayout = mutableStateOf<String?>(null)

    fun log(event: DebugEvent) {
        if (!isEnabled.value) return
        events.add(0, event)
        if (events.size > MAX_EVENTS) events.removeRange(MAX_EVENTS, events.size)
    }

    fun clear() {
        events.clear()
        keyboardEventCount.value = 0
        mouseEventCount.value = 0
        unmappedKeyCount.value = 0
        wineEventCount.value = 0
    }

    fun logKeyEvent(
        event: KeyEvent,
        isKeyboardDevice: Boolean,
        isGamepadDevice: Boolean,
        xKeycode: XKeycode?,
        handled: Boolean,
    ) {
        if (!isEnabled.value) return

        val action = if (event.action == KeyEvent.ACTION_DOWN) "DOWN" else "UP"
        val androidKeyName = KeyEvent.keyCodeToString(event.keyCode)
        val mappedTo = xKeycode?.name ?: "UNMAPPED"
        val deviceSource = describeDevice(event.device)

        val details = buildString {
            append("$androidKeyName ($action)")
            append(" → $mappedTo")
            append(" | scanCode=${event.scanCode}")
            append(" | unicode=${event.unicodeChar}")
            append(" | device=$deviceSource")
            if (isGamepadDevice) append(" [GAMEPAD]")
            if (isKeyboardDevice) append(" [KEYBOARD]")
        }

        val type = if (event.action == KeyEvent.ACTION_DOWN) EventType.KEY_DOWN else EventType.KEY_UP

        if (event.action == KeyEvent.ACTION_DOWN) {
            keyboardEventCount.value++
            if (xKeycode == null) unmappedKeyCount.value++
        }

        log(DebugEvent(
            type = type,
            source = when {
                isGamepadDevice -> "Gamepad"
                isKeyboardDevice -> "Keyboard"
                else -> "Unknown"
            },
            details = details,
            mapped = xKeycode != null,
        ))
    }

    fun logMouseEvent(
        source: String,
        dx: Int,
        dy: Int,
        isRelative: Boolean,
        flags: String = "",
    ) {
        if (!isEnabled.value) return
        mouseEventCount.value++

        val mode = if (isRelative) "REL" else "ABS"
        val details = "$mode dx=$dx dy=$dy $flags".trim()

        log(DebugEvent(
            type = EventType.MOUSE_MOVE,
            source = source,
            details = details,
            mapped = true,
        ))
    }

    fun logMouseButton(source: String, button: String, isDown: Boolean) {
        if (!isEnabled.value) return
        mouseEventCount.value++

        val action = if (isDown) "DOWN" else "UP"
        log(DebugEvent(
            type = EventType.MOUSE_BUTTON,
            source = source,
            details = "$button $action",
            mapped = true,
        ))
    }

    fun logTouch(x: Float, y: Float, action: String) {
        if (!isEnabled.value) return

        log(DebugEvent(
            type = EventType.TOUCH,
            source = "Touch",
            details = "$action at (${"%.0f".format(x)}, ${"%.0f".format(y)})",
            mapped = true,
        ))
    }

    /**
     * Process a line from Wine process stdout/stderr. Filters for input-related
     * traces and logs them. Standard Wine debug format:
     *   THREADID:LEVEL:CHANNEL:Function message
     * e.g. "00d0:trace:keyboard:X11DRV_InitKeyboard keycode 24 => vkey 0051"
     */
    fun processWineLogLine(line: String) {
        if (!isEnabled.value) return

        // Match keyboard key event traces
        when {
            line.contains("keyboard:X11DRV_KeyEvent") || line.contains("GN_KEY_DEBUG") -> {
                wineEventCount.value++
                val isDeliver = line.contains("DELIVERING")
                val type = if (isDeliver) EventType.WINE_KEY else EventType.WINE_INFO
                val short = extractAfterFunction(line)
                log(DebugEvent(
                    type = type,
                    source = "Wine/Key",
                    details = short,
                    mapped = !line.contains("vkey=0x00") && !line.contains("vkey=0xFC"),
                ))
            }

            line.contains("key:") && (line.contains("keysym=") || line.contains("vkey")) -> {
                wineEventCount.value++
                val short = extractAfterFunction(line)
                log(DebugEvent(
                    type = EventType.WINE_KEY,
                    source = "Wine/Key",
                    details = short,
                    mapped = true,
                ))
            }

            line.contains("keyboard:X11DRV_InitKeyboard") && line.contains("vkey") -> {
                wineEventCount.value++
                val short = extractAfterFunction(line)
                log(DebugEvent(
                    type = EventType.WINE_LAYOUT,
                    source = "Wine/Layout",
                    details = short,
                    mapped = !line.contains("vkey 0000"),
                ))
            }

            line.contains("keyboard:X11DRV_KEYBOARD_DetectLayout") -> {
                wineEventCount.value++
                val short = extractAfterFunction(line)
                log(DebugEvent(
                    type = EventType.WINE_LAYOUT,
                    source = "Wine/Layout",
                    details = short,
                    mapped = true,
                ))
            }

            line.contains("detected layout is") -> {
                wineEventCount.value++
                val layoutMatch = Regex("\"(.+?)\"").find(line)
                detectedLayout.value = layoutMatch?.groupValues?.get(1)
                log(DebugEvent(
                    type = EventType.WINE_LAYOUT,
                    source = "Wine/Layout",
                    details = "Detected: ${detectedLayout.value ?: line}",
                    mapped = true,
                ))
            }

            // Mouse/cursor traces
            line.contains("cursor:") || (line.contains("mouse:") && line.contains("trace:")) -> {
                wineEventCount.value++
                val short = extractAfterFunction(line)
                log(DebugEvent(
                    type = EventType.WINE_MOUSE,
                    source = "Wine/Mouse",
                    details = short,
                    mapped = true,
                ))
            }

            line.contains("MotionNotify") || line.contains("grab_clipping") ||
                line.contains("needs_relative") || line.contains("clipping to") ||
                line.contains("no longer clipping") -> {
                wineEventCount.value++
                val short = extractAfterFunction(line)
                log(DebugEvent(
                    type = EventType.WINE_MOUSE,
                    source = "Wine/Mouse",
                    details = short,
                    mapped = true,
                ))
            }

            line.contains("ButtonPress") || line.contains("ButtonRelease") -> {
                wineEventCount.value++
                val short = extractAfterFunction(line)
                log(DebugEvent(
                    type = EventType.WINE_MOUSE,
                    source = "Wine/Mouse",
                    details = short,
                    mapped = true,
                ))
            }

            // XInput2 traces (relevant for understanding mouse path)
            line.contains("xinput2") || line.contains("XInput2") -> {
                wineEventCount.value++
                val short = extractAfterFunction(line)
                log(DebugEvent(
                    type = EventType.WINE_INFO,
                    source = "Wine/XI2",
                    details = short,
                    mapped = true,
                ))
            }
        }
    }

    private fun extractAfterFunction(line: String): String {
        // Wine format: "THREAD:level:channel:FunctionName rest of message"
        // Try to extract just "FunctionName rest of message" or at least the useful part
        val colonParts = line.split(":")
        return if (colonParts.size >= 4) {
            colonParts.drop(3).joinToString(":").trim()
        } else {
            line.trim()
        }
    }

    private fun describeDevice(device: InputDevice?): String {
        if (device == null) return "null"
        val sources = device.sources
        val parts = mutableListOf<String>()
        if (sources and InputDevice.SOURCE_KEYBOARD == InputDevice.SOURCE_KEYBOARD) parts.add("KBD")
        if (sources and InputDevice.SOURCE_MOUSE == InputDevice.SOURCE_MOUSE) parts.add("MOUSE")
        if (sources and InputDevice.SOURCE_TOUCHSCREEN == InputDevice.SOURCE_TOUCHSCREEN) parts.add("TOUCH")
        if (sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD) parts.add("GAMEPAD")
        if (sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK) parts.add("JOYSTICK")
        return "${device.name}(${parts.joinToString("+")})"
    }

    fun describeInputPaths(
        isRelativeMouse: Boolean,
        winHandlerConnected: Boolean,
    ): List<InputPathInfo> {
        return listOf(
            InputPathInfo(
                name = "Keyboard",
                path = "Android KeyEvent → Keyboard.java → X11 KeyPress → Wine X11 driver → Win32",
                active = true,
                protocol = "X11",
            ),
            InputPathInfo(
                name = "Mouse (absolute)",
                path = "Touch/Mouse → TouchpadView → X11 MotionNotify → Wine X11 driver",
                active = !isRelativeMouse,
                protocol = "X11",
            ),
            InputPathInfo(
                name = "Mouse (relative)",
                path = "Touch/Mouse → TouchpadView → WinHandler UDP:7946 → winhandler.exe → Win32 SendInput",
                active = isRelativeMouse,
                protocol = "WinHandler",
            ),
            InputPathInfo(
                name = "WinHandler bridge",
                path = "UDP localhost:7946↔7947",
                active = winHandlerConnected,
                protocol = "UDP",
            ),
        )
    }

    data class InputPathInfo(
        val name: String,
        val path: String,
        val active: Boolean,
        val protocol: String,
    )
}
