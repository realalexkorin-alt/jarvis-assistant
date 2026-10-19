package dev.jarvis.assistant.updates

import org.junit.Assert.*
import org.junit.Test

class VersioningTest {
    @Test fun versionCodes() {
        assertEquals(1000L, Versioning.versionCode("v0.1.0"))
        assertEquals(1001L, Versioning.versionCode("0.1.1"))
        assertEquals(1000000L, Versioning.versionCode("v1.0.0"))
    }
    @Test fun rejectInvalidVersions() {
        assertNull(Versioning.versionCode("latest"))
        assertNull(Versioning.versionCode("v0.1.0-beta"))
        assertNull(Versioning.versionCode("v0.1000.0"))
    }
}
