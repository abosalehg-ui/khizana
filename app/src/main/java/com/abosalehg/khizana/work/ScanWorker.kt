package com.abosalehg.khizana.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.abosalehg.khizana.domain.repo.LibraryRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Runs a manual library rescan in the background with cancellable progress.
 * Enqueued as unique work so only one scan runs at a time.
 */
@HiltWorker
class ScanWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: LibraryRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val deep = inputData.getBoolean(KEY_DEEP, false)
        return try {
            val report = repository.rescan(deep = deep) { processed, total ->
                setProgress(workDataOf(KEY_PROCESSED to processed, KEY_TOTAL to total))
            }
            // Newly discovered books need covers; chain the generator.
            WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                CoverWorker.UNIQUE_NAME,
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<CoverWorker>().build()
            )
            Result.success(
                workDataOf(
                    KEY_SCANNED to report.scanned,
                    KEY_ADDED to report.added,
                    KEY_RELOCATED to report.relocated,
                    KEY_MISSING to report.missing
                )
            )
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure()
        }
    }

    companion object {
        const val UNIQUE_NAME = "library_scan"
        const val KEY_DEEP = "deep"
        const val KEY_PROCESSED = "processed"
        const val KEY_TOTAL = "total"
        const val KEY_SCANNED = "scanned"
        const val KEY_ADDED = "added"
        const val KEY_RELOCATED = "relocated"
        const val KEY_MISSING = "missing"
    }
}
