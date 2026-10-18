package com.pixelquest.app.domain.namaz

import kotlinx.coroutines.sync.Mutex

/**
 * One at a time for marking and undoing prayers, across the whole app: a double tap, or the
 * screen's button and a reminder's Prayed racing each other, would otherwise both see the prayer
 * unmarked and both award its XP. Each mark or undo reads the day and writes it inside the lock.
 */
internal object NamazXpLock {
    val mutex = Mutex()
}
