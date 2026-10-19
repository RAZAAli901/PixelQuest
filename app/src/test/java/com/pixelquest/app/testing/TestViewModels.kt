package com.pixelquest.app.testing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.RoomDatabase
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * View models made in a test, kept so they can be cleared at its end: clearing cancels their
 * viewModelScope, so nothing they started (a refresh from init, say) outlives the test and reaches
 * the next one.
 */
class TestViewModels {
    private val store = ViewModelStore()
    private var made = 0

    /** [make]'s view model, kept until [clear]. Each is its own, never an earlier one again. */
    fun <T : ViewModel> keep(type: Class<T>, make: () -> T): T {
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <V : ViewModel> create(modelClass: Class<V>): V = make() as V
        }
        return ViewModelProvider(store, factory)["test-${made++}", type]
    }

    fun clear() = store.clear()
}

/**
 * Closes the database once Room is done with it. Flows that just ended (their view models cleared)
 * drop their table observers on Room's own threads a moment later, so that moment is given first;
 * then a write already under way finishes (Room runs transactions one at a time, so a task queued
 * after them waits for them). Closing in the middle of either throws on Room's thread, where it
 * would surface in a later test.
 */
fun RoomDatabase.closeAfterWrites() {
    Thread.sleep(OBSERVERS_SETTLE_MS)
    val drained = CountDownLatch(1)
    transactionExecutor.execute { drained.countDown() }
    drained.await(2, TimeUnit.SECONDS)
    close()
}

/** Room drops an ended flow's observer on its own thread: well within this, on an in-memory database. */
private const val OBSERVERS_SETTLE_MS = 100L
