package com.graffitistudio.core.domain.history

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryManagerTest {

    private class AppendCommand(
        private val target: MutableList<String>,
        private val value: String,
        override val description: String = "Append $value"
    ) : HistoryCommand {
        override fun execute() {
            target.add(value)
        }

        override fun undo() {
            target.removeLastOrNull()
        }
    }

    @Test
    fun `execute undo and redo update target and state`() {
        val items = mutableListOf<String>()
        val manager = HistoryManager(maxHistorySize = 10)

        manager.execute(AppendCommand(items, "A"))
        manager.execute(AppendCommand(items, "B"))

        assertEquals(listOf("A", "B"), items)
        assertTrue(manager.state.value.canUndo)
        assertFalse(manager.state.value.canRedo)
        assertEquals("Append B", manager.state.value.undoDescription)

        assertTrue(manager.undo())
        assertEquals(listOf("A"), items)
        assertTrue(manager.state.value.canUndo)
        assertTrue(manager.state.value.canRedo)
        assertEquals("Append B", manager.state.value.redoDescription)

        assertTrue(manager.redo())
        assertEquals(listOf("A", "B"), items)
        assertFalse(manager.state.value.canRedo)
    }

    @Test
    fun `new command clears redo stack`() {
        val items = mutableListOf<String>()
        val manager = HistoryManager()

        manager.execute(AppendCommand(items, "A"))
        manager.execute(AppendCommand(items, "B"))
        manager.undo()
        assertTrue(manager.state.value.canRedo)

        manager.execute(AppendCommand(items, "C"))
        assertEquals(listOf("A", "C"), items)
        assertFalse(manager.state.value.canRedo)
        assertNull(manager.state.value.redoDescription)
    }

    @Test
    fun `recordExecuted adds command without re-executing`() {
        val items = mutableListOf("live-stroke")
        val manager = HistoryManager()

        manager.recordExecuted(AppendCommand(items, "live-stroke"))
        assertEquals(listOf("live-stroke"), items)
        assertTrue(manager.state.value.canUndo)

        manager.undo()
        assertTrue(items.isEmpty())

        manager.redo()
        assertEquals(listOf("live-stroke"), items)
    }

    @Test
    fun `history is bounded by maxHistorySize`() {
        val items = mutableListOf<String>()
        val manager = HistoryManager(maxHistorySize = 2)

        manager.execute(AppendCommand(items, "1"))
        manager.execute(AppendCommand(items, "2"))
        manager.execute(AppendCommand(items, "3"))

        assertEquals(2, manager.state.value.undoCount)
        assertTrue(manager.undo())
        assertTrue(manager.undo())
        assertFalse(manager.undo())
        assertEquals(listOf("1"), items)
    }

    @Test
    fun `composite command executes forward and undoes in reverse`() {
        val log = mutableListOf<String>()
        val manager = HistoryManager()

        val composite = CompositeCommand(
            description = "Batch",
            commands = listOf(
                AppendCommand(log, "first"),
                AppendCommand(log, "second")
            )
        )

        manager.execute(composite)
        assertEquals(listOf("first", "second"), log)

        manager.undo()
        assertTrue(log.isEmpty())
    }
}
