package dev.jarvis.assistant.core
import org.junit.Assert.*
import org.junit.Test

class EventPolicyTest {
    @Test fun batteryDecileCrossing() {
        assertTrue(EventPolicy.crossedBatteryDecile(91, 90, false))
        assertTrue(EventPolicy.crossedBatteryDecile(95, 89, false))
        assertTrue(EventPolicy.crossedBatteryDecile(85, 80, false))
        assertTrue(EventPolicy.crossedBatteryDecile(51, 48, false))
        assertFalse(EventPolicy.crossedBatteryDecile(90, 89, false))
        assertFalse(EventPolicy.crossedBatteryDecile(87, 86, false))
        assertFalse(EventPolicy.crossedBatteryDecile(80, 82, false))
        assertFalse(EventPolicy.crossedBatteryDecile(91, 90, true))
        assertFalse(EventPolicy.crossedBatteryDecile(-1, 90, false))
    }
    @Test fun hourlyDeduplication() {
        assertTrue(EventPolicy.hourlyDue(0,"2026-10-19-14","2026-10-19-13"))
        assertFalse(EventPolicy.hourlyDue(0,"2026-10-19-14","2026-10-19-14"))
        assertFalse(EventPolicy.hourlyDue(1,"2026-10-19-14","2026-10-19-13"))
        assertEquals("Сейчас 1 час",EventPolicy.hourPhrase(1))
        assertEquals("Сейчас 11 часов",EventPolicy.hourPhrase(11))
        assertEquals("Сейчас 23 часа",EventPolicy.hourPhrase(23))
    }
}
