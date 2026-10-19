package dev.jarvis.assistant.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceTextNormalizerTest {
    @Test fun batteryWords() {
        assertEquals("Заряд батареи: девяносто процентов.", VoiceTextNormalizer.normalize("Заряд батареи 90 процентов"))
        assertEquals("Заряд батареи: двадцать один процент.", VoiceTextNormalizer.normalize("Заряд батареи 21 процентов"))
        assertEquals("Заряд батареи: двенадцать процентов.", VoiceTextNormalizer.normalize("Заряд батареи 12 процентов"))
        assertEquals("Заряд батареи: сто процентов.", VoiceTextNormalizer.normalize("Заряд батареи 100 процентов"))
    }
    @Test fun clockWords() {
        assertEquals("Сейчас двадцать один час.", VoiceTextNormalizer.normalize("Сейчас 21 час"))
        assertEquals("Сейчас одиннадцать часов.", VoiceTextNormalizer.normalize("Сейчас 11 часов"))
        assertEquals("Сейчас два часа.", VoiceTextNormalizer.normalize("Сейчас 2 часа"))
    }
    @Test fun ordinaryPrivateMessagesUnchanged() {
        assertEquals("Привет! Как дела?", VoiceTextNormalizer.normalize("Привет!   Как дела?"))
    }
}
