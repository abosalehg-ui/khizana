package com.abosalehg.khizana.data.scanner

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * All-files-access helper. Android 11+ uses MANAGE_EXTERNAL_STORAGE granted
 * from a system settings screen; Android 10 falls back to legacy
 * READ_EXTERNAL_STORAGE plus requestLegacyExternalStorage.
 */
object StoragePermission {

    val needsAllFilesAccess: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    fun isGranted(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }

    /** Settings intent for Android 11+; falls back to the generic screen. */
    fun allFilesAccessIntent(context: Context): Intent {
        val specific = Intent(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            Uri.fromParts("package", context.packageName, null)
        )
        return if (specific.resolveActivity(context.packageManager) != null) {
            specific
        } else {
            Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
        }
    }
}
