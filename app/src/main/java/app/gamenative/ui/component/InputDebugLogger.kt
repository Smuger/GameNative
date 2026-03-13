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
        X11_KEY,
        WINE_KEY,
        WINE_MOUSE,
        WINE_LAYOUT,
        WINE_INFO,
    }

    private const val MAX_EVENTS = 300
    private const val MAX_SYSTEM_LOG = 80

    val events = mutableStateListOf<DebugEvent>()
    val systemLog = mutableStateListOf<String>()
    val isEnabled = mutableStateOf(false)
    val keyboardEventCount = mutableStateOf(0)
    val mouseEventCount = mutableStateOf(0)
    val unmappedKeyCount = mutableStateOf(0)
    val wineEventCount = mutableStateOf(0)
    val x11KeyCount = mutableStateOf(0)
    val wineDebugStatus = mutableStateOf("unknown")
    val detectedLayout = mutableStateOf<String?>(null)

    fun log(event: DebugEvent) {
        if (!isEnabled.value) return
        events.add(0, event)
        if (events.size > MAX_EVENTS) events.removeRange(MAX_EVENTS, events.size)
    }

    fun addSystemLog(message: String) {
        if (!isEnabled.value) return
        val ts = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US)
            .format(java.util.Date())
        systemLog.add(0, "[$ts] $message")
        if (systemLog.size > MAX_SYSTEM_LOG) systemLog.removeRange(MAX_SYSTEM_LOG, systemLog.size)
    }

    fun clear() {
        events.clear()
        systemLog.clear()
        keyboardEventCount.value = 0
        mouseEventCount.value = 0
        unmappedKeyCount.value = 0
        wineEventCount.value = 0
        x11KeyCount.value = 0
    }

    /**
     * Called from Keyboard.OnKeyboardListener — fires for ALL key injections
     * regardless of source (IME, physical keyboard, on-screen controller, touchpad hotkeys).
     */
    fun logX11KeyPress(keycode: Byte, keysym: Int) {
        if (!isEnabled.value) return
        x11KeyCount.value++
        keyboardEventCount.value++
        val xkName = XKeycode.entries.find { it.id == keycode }?.name ?: "unknown($keycode)"
        val details = "$xkName (keycode=$keycode, keysym=0x${keysym.toString(16)})"
        addSystemLog("X11 Server: KeyPress $details → sent to focused window via Unix socket")
        log(DebugEvent(
            type = EventType.X11_KEY,
            source = "X11/Press",
            details = details,
            mapped = true,
        ))
    }

    fun logX11KeyRelease(keycode: Byte) {
        if (!isEnabled.value) return
        x11KeyCount.value++
        val xkName = XKeycode.entries.find { it.id == keycode }?.name ?: "unknown($keycode)"
        val details = "$xkName (keycode=$keycode)"
        addSystemLog("X11 Server: KeyRelease $details → sent to focused window via Unix socket")
        log(DebugEvent(
            type = EventType.X11_KEY,
            source = "X11/Release",
            details = details,
            mapped = true,
        ))
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

        val src = when {
            isGamepadDevice -> "Gamepad"
            isKeyboardDevice -> "Keyboard"
            else -> "Unknown"
        }

        addSystemLog("Android: $src ${KeyEvent.keyCodeToString(event.keyCode)} $action" +
            " → mapped to ${xKeycode?.name ?: "UNMAPPED"}" +
            " → Keyboard.onKeyEvent()")

        log(DebugEvent(
            type = type,
            source = src,
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
     *   or: "00e4:trace:event:call_event_handler 118 KeyPress for hwnd/window 0x10086/2800001"
     */
    fun processWineLogLine(line: String) {
        if (!isEnabled.value) return

        when {
            // X11 event handler: KeyPress/KeyRelease received by Wine
            line.contains("call_event_handler") && line.contains("KeyPress") -> {
                wineEventCount.value++
                val short = extractAfterFunction(line)
                addSystemLog("Wine: X11 KeyPress received → $short")
                log(DebugEvent(
                    type = EventType.WINE_KEY,
                    source = "Wine/X11",
                    details = "KeyPress $short",
                    mapped = true,
                ))
            }

            line.contains("call_event_handler") && line.contains("KeyRelease") -> {
                wineEventCount.value++
                val short = extractAfterFunction(line)
                log(DebugEvent(
                    type = EventType.WINE_KEY,
                    source = "Wine/X11",
                    details = "KeyRelease $short",
                    mapped = true,
                ))
            }

            // Wine keyboard processing: vkey mapping and unicode translation
            line.contains("NtUserToUnicodeEx") -> {
                wineEventCount.value++
                val short = extractAfterFunction(line)
                val vkeyMatch = Regex("virt (0x[0-9a-fA-F]+)").find(line)
                val scanMatch = Regex("scan (0x[0-9a-fA-F]+)").find(line)
                val vkey = vkeyMatch?.groupValues?.get(1) ?: "?"
                val scan = scanMatch?.groupValues?.get(1) ?: "?"
                addSystemLog("Wine: NtUserToUnicodeEx vkey=$vkey scan=$scan → game received key")
                log(DebugEvent(
                    type = EventType.WINE_KEY,
                    source = "Wine/Win32",
                    details = "ToUnicode vkey=$vkey scan=$scan",
                    mapped = true,
                ))
            }

            // Wine keyboard key event processing
            line.contains("keyboard:X11DRV_KeyEvent") || line.contains("GN_KEY_DEBUG") -> {
                wineEventCount.value++
                val isDeliver = line.contains("DELIVERING")
                val type = if (isDeliver) EventType.WINE_KEY else EventType.WINE_INFO
                val short = extractAfterFunction(line)
                if (isDeliver) {
                    addSystemLog("Wine: X11DRV_KeyEvent DELIVERING → $short")
                }
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

            // MapVirtualKeyEx — shows vkey ↔ scancode resolution
            line.contains("X11DRV_MapVirtualKeyEx") && line.contains("returning") -> {
                wineEventCount.value++
                val short = extractAfterFunction(line)
                log(DebugEvent(
                    type = EventType.WINE_KEY,
                    source = "Wine/Map",
                    details = short,
                    mapped = !line.contains("returning 0x0."),
                ))
            }

            // Layout detection
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
                addSystemLog("Wine: Keyboard layout detected: ${detectedLayout.value}")
                log(DebugEvent(
                    type = EventType.WINE_LAYOUT,
                    source = "Wine/Layout",
                    details = "Detected: ${detectedLayout.value ?: line}",
                    mapped = true,
                ))
            }

            // Mouse events from Wine X11 event handler
            line.contains("call_event_handler") && line.contains("MotionNotify") -> {
                wineEventCount.value++
                val short = extractAfterFunction(line)
                log(DebugEvent(
                    type = EventType.WINE_MOUSE,
                    source = "Wine/X11",
                    details = "MotionNotify $short",
                    mapped = true,
                ))
            }

            line.contains("call_event_handler") && (
                line.contains("ButtonPress") || line.contains("ButtonRelease")) -> {
                wineEventCount.value++
                val short = extractAfterFunction(line)
                val evType = if (line.contains("ButtonPress")) "ButtonPress" else "ButtonRelease"
                addSystemLog("Wine: X11 $evType → $short")
                log(DebugEvent(
                    type = EventType.WINE_MOUSE,
                    source = "Wine/X11",
                    details = "$evType $short",
                    mapped = true,
                ))
            }

            // Cursor/clipping traces
            line.contains("grab_clipping") || line.contains("clipping to") ||
                line.contains("no longer clipping") || line.contains("needs_relative") -> {
                wineEventCount.value++
                val short = extractAfterFunction(line)
                addSystemLog("Wine: Mouse clipping → $short")
                log(DebugEvent(
                    type = EventType.WINE_MOUSE,
                    source = "Wine/Clip",
                    details = short,
                    mapped = true,
                ))
            }

            // XInput2 traces
            line.contains("xinput2") || line.contains("XInput2") -> {
                wineEventCount.value++
                val short = extractAfterFunction(line)
                addSystemLog("Wine: XInput2 → $short")
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
