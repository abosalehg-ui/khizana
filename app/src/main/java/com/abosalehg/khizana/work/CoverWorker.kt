package com.abosalehg.khizana.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.abosalehg.khizana.data.covers.CoverGenerator
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Generates missing covers in the background. Chained after every scan. */
@HiltWorker
class CoverWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val coverGenerator: CoverGenerator
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            coverGenerator.generateMissing { processed, total ->
                setProgress(workDataOf(KEY_PROCESSED to processed, KEY_TOTAL to total))
            }
            Result.success()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.w(TAG, "Cover generation failed", e)
            Result.failure()
        }
    }

    companion object {
        private const val TAG = "CoverWorker"
        const val UNIQUE_NAME = "cover_generation"
        const val KEY_PROCESSED = "processed"
        const val KEY_TOTAL = "total"
    }
}
