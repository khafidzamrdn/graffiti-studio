package com.graffitistudio.core.domain.history

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Represents a reversible operation in the drawing studio.
 *
 * Commands encapsulate both the forward mutation ([execute]/[redo]) and its inverse ([undo]),
 * avoiding expensive full-canvas bitmap snapshots per action.
 */
interface HistoryCommand {
    val description: String

    fun execute()

    fun undo()

    fun redo() = execute()

    fun canMergeWith(next: HistoryCommand): Boolean = false

    fun mergeWith(next: HistoryCommand): HistoryCommand {
        throw UnsupportedOperationException("Command $description does not support merging")
    }
}

/**
 * Groups multiple [HistoryCommand] instances into a single atomic undo/redo step.
 */
class CompositeCommand(
    override val description: String,
    private val commands: List<HistoryCommand>
) : HistoryCommand {

    override fun execute() {
        commands.forEach { it.execute() }
    }

    override fun undo() {
        commands.asReversed().forEach { it.undo() }
    }

    override fun redo() {
        commands.forEach { it.redo() }
    }
}

data class HistoryState(
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val undoDescription: String? = null,
    val redoDescription: String? = null,
    val undoCount: Int = 0,
    val redoCount: Int = 0
)

/**
 * Manages bounded undo and redo stacks using the Command pattern.
 */
class HistoryManager(
    private val maxHistorySize: Int = 50
) {
    init {
        require(maxHistorySize > 0) { "maxHistorySize must be positive" }
    }

    private val lock = Any()
    private val undoStack = ArrayDeque<HistoryCommand>()
    private val redoStack = ArrayDeque<HistoryCommand>()

    private val _state = MutableStateFlow(HistoryState())
    val state: StateFlow<HistoryState> = _state.asStateFlow()

    /**
     * Executes [command] and pushes it onto the undo stack, clearing any redo history.
     */
    fun execute(command: HistoryCommand) {
        synchronized(lock) {
            command.execute()
            pushCommandLocked(command)
        }
    }

    /**
     * Records a [command] whose mutation was already applied incrementally (for example,
     * a live brush stroke rasterized during pointer movement and finalized on pointer up).
     */
    fun recordExecuted(command: HistoryCommand) {
        synchronized(lock) {
            pushCommandLocked(command)
        }
    }

    fun undo(): Boolean {
        synchronized(lock) {
            val command = undoStack.removeLastOrNull() ?: return false
            command.undo()
            redoStack.addLast(command)
            emitStateLocked()
            return true
        }
    }

    fun redo(): Boolean {
        synchronized(lock) {
            val command = redoStack.removeLastOrNull() ?: return false
            command.redo()
            undoStack.addLast(command)
            emitStateLocked()
            return true
        }
    }

    fun clear() {
        synchronized(lock) {
            undoStack.clear()
            redoStack.clear()
            emitStateLocked()
        }
    }

    private fun pushCommandLocked(command: HistoryCommand) {
        redoStack.clear()
        val last = undoStack.lastOrNull()
        if (last != null && last.canMergeWith(command)) {
            undoStack.removeLast()
            undoStack.addLast(last.mergeWith(command))
        } else {
            if (undoStack.size >= maxHistorySize) {
                undoStack.removeFirst()
            }
            undoStack.addLast(command)
        }
        emitStateLocked()
    }

    private fun emitStateLocked() {
        _state.value = HistoryState(
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty(),
            undoDescription = undoStack.lastOrNull()?.description,
            redoDescription = redoStack.lastOrNull()?.description,
            undoCount = undoStack.size,
            redoCount = redoStack.size
        )
    }
}
