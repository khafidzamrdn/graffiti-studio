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

    private class DisposableCommand(
        private val target: MutableList<String>,
        private val value: String,
        private val disposed: MutableList<String>
    ) : HistoryCommand {
        override val description: String = "Disposable $value"

        override fun execute() {
            target.add(value)
        }

        override fun undo() {
            target.removeLastOrNull()
        }

        override fun dispose() {
            disposed.add(value)
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
    fun `evicted commands are disposed`() {
        val items = mutableListOf<String>()
        val disposed = mutableListOf<String>()
        val manager = HistoryManager(maxHistorySize = 2)

        manager.execute(DisposableCommand(items, "1", disposed))
        manager.execute(DisposableCommand(items, "2", disposed))
        manager.execute(DisposableCommand(items, "3", disposed))

        assertEquals(listOf("1"), disposed)
    }

    @Test
    fun `clear disposes commands from both stacks`() {
        val items = mutableListOf<String>()
        val disposed = mutableListOf<String>()
        val manager = HistoryManager()

        manager.execute(DisposableCommand(items, "1", disposed))
        manager.execute(DisposableCommand(items, "2", disposed))
        manager.undo()

        manager.clear()

        assertEquals(setOf("1", "2"), disposed.toSet())
    }

    @Test
    fun `new command disposes the redo stack`() {
        val items = mutableListOf<String>()
        val disposed = mutableListOf<String>()
        val manager = HistoryManager()

        manager.execute(DisposableCommand(items, "1", disposed))
        manager.execute(DisposableCommand(items, "2", disposed))
        manager.undo()
        manager.execute(DisposableCommand(items, "3", disposed))

        assertEquals(listOf("2"), disposed)
    }

    @Test
    fun `composite dispose cascades to children`() {
        val items = mutableListOf<String>()
        val disposed = mutableListOf<String>()
        val manager = HistoryManager()

        manager.execute(
            CompositeCommand(
                description = "Batch",
                commands = listOf(
                    DisposableCommand(items, "first", disposed),
                    DisposableCommand(items, "second", disposed)
                )
            )
        )
        manager.clear()

        assertEquals(listOf("first", "second"), disposed)
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
