package com.georgeslebatoon.frenchvo
import org.junit.Assert.*
import org.junit.Test
class TomChunkTest {
    @Test fun longNarrationKeepsEveryWord() {
        val text = (1..5000).joinToString(" ") { "mot$it" }
        val chunks = TomEngine.splitText(text)
        assertEquals(text, chunks.joinToString(" "))
        assertTrue(chunks.all { it.length <= 600 })
    }
    @Test fun sentencesRemainTogetherWherePossible() {
        val sentence = "Bonjour, voici une phrase française. "
        val chunks = TomEngine.splitText(sentence.repeat(60))
        assertTrue(chunks.dropLast(1).all { it.trimEnd().endsWith(".") })
    }
    @Test fun textWithoutSpacesAlwaysFinishes() {
        val text = "é".repeat(30000)
        val chunks = TomEngine.splitText(text)
        assertEquals(text, chunks.joinToString(""))
        assertEquals(50, chunks.size)
    }
}
