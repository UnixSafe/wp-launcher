package com.example.repository

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.example.data.LauncherDao
import com.example.data.SettingsEntity
import com.example.data.TileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

data class AppItem(
    val packageName: String,
    val className: String,
    val label: String,
    val icon: Bitmap? = null
)

class LauncherRepository(
    private val context: Context,
    private val launcherDao: LauncherDao
) {
    val allTiles: Flow<List<TileEntity>> = launcherDao.getAllTiles()
    val settings: Flow<SettingsEntity?> = launcherDao.getSettings()

    suspend fun insertTile(tile: TileEntity) = launcherDao.insertTile(tile)
    suspend fun insertTiles(tiles: List<TileEntity>) = launcherDao.insertTiles(tiles)
    suspend fun updateTile(tile: TileEntity) = launcherDao.updateTile(tile)
    suspend fun deleteTile(tile: TileEntity) = launcherDao.deleteTile(tile)
    suspend fun deleteTileByPackage(packageName: String) = launcherDao.deleteTileByPackage(packageName)
    suspend fun clearAllTiles() = launcherDao.clearAllTiles()

    suspend fun getSettingsDirect(): SettingsEntity {
        return launcherDao.getSettingsDirect() ?: SettingsEntity().also {
            launcherDao.saveSettings(it)
        }
    }

    suspend fun saveSettings(settings: SettingsEntity) = launcherDao.saveSettings(settings)

    // Load installed applications that reside in launcher
    suspend fun getInstalledApps(): List<AppItem> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(intent, 0)
        
        resolveInfos.map { info ->
            val packageName = info.activityInfo.packageName
            val className = info.activityInfo.name
            val label = info.loadLabel(pm).toString()
            val rawIcon = info.loadIcon(pm)
            val bitmap = drawableToBitmap(rawIcon)
            
            AppItem(
                packageName = packageName,
                className = className,
                label = label,
                icon = bitmap
            )
        }.sortedBy { it.label.lowercase() }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap? {
        try {
            if (drawable is BitmapDrawable) {
                if (drawable.bitmap != null) {
                    return drawable.bitmap
                }
            }
            // Handle Adaptive or XML drawables
            val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
            val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            return bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
