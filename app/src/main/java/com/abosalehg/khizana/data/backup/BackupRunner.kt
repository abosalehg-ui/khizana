package com.abosalehg.khizana.data.backup

import android.net.Uri
import com.abosalehg.khizana.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runs an export or a restore to completion regardless of which screen is
 * showing, one at a time. [running] lives here rather than in a ViewModel, so
 * reopening Settings mid-restore still shows the buttons disabled.
 *
 * [onResult] is called on the main thread; when the screen that asked has
 * gone, its snackbar simply has nowhere to appear — the work itself is done.
 */
@Singleton
class BackupRunner @Inject constructor(
    private val manager: BackupManager,
    @ApplicationScope private val scope: CoroutineScope
) {

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    fun export(uri: Uri, onResult: (Boolean) -> Unit) = run(onResult) { manager.exportTo(uri) }

    fun restore(uri: Uri, onResult: (Boolean) -> Unit) = run(onResult) { manager.importFrom(uri) }

    private fun run(onResult: (Boolean) -> Unit, block: suspend () -> Boolean) {
        if (!_running.compareAndSet(expect = false, update = true)) return
        scope.launch {
            val ok = try {
                block()
            } finally {
                _running.value = false
            }
            onResult(ok)
        }
    }
}
