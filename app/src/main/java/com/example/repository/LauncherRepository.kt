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
    val icon: Bitmap? = null,
    // True when the icon has a meaningful transparent area (a logo/glyph), so it looks good
    // rendered as a flat white silhouette. Full-bleed square icons are false (would become a
    // white square in WP icon mode).
    val hasTransparency: Boolean = false
)

class LauncherRepository(
    private val context: Context,
    private val launcherDao: LauncherDao
) {
    val allTiles: Flow<List<TileEntity>> = launcherDao.getAllTiles()
    val settings: Flow<SettingsEntity?> = launcherDao.getSettings()

    suspend fun getAllTilesBlock(): List<TileEntity> =
        withContext(Dispatchers.IO) { launcherDao.getAllTilesBlock() }

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
                icon = bitmap,
                hasTransparency = bitmap?.let { hasMeaningfulTransparency(it) } ?: false
            )
        }.sortedBy { it.label.lowercase() }
    }

    // Sample the icon's alpha channel to decide if it has a real transparent area (logo) vs being
    // a full-bleed square. Cheap (samples ~1/16 of pixels), runs off the main thread at load.
    private fun hasMeaningfulTransparency(bmp: Bitmap): Boolean {
        val w = bmp.width
        val h = bmp.height
        if (w == 0 || h == 0) return false
        var transparent = 0
        var total = 0
        val step = 4
        var y = 0
        while (y < h) {
            var x = 0
            while (x < w) {
                val alpha = (bmp.getPixel(x, y) ushr 24) and 0xFF
                if (alpha < 240) transparent++
                total++
                x += step
            }
            y += step
        }
        return total > 0 && transparent.toFloat() / total > 0.12f
    }

    // Cap rasterized icons to the max displayed size (~44dp). App launcher icons are often
    // 144-512px full-res; keeping all of them at full resolution wastes a lot of RAM and GPU
    // upload bandwidth on a weak device. We downscale every icon to iconPx once, off the main
    // thread, so the list and tiles stay light and fluid.
    private val iconPx: Int =
        (44f * context.resources.displayMetrics.density).toInt().coerceAtLeast(48)

    private fun drawableToBitmap(drawable: Drawable): Bitmap? {
        return try {
            val bitmap = Bitmap.createBitmap(iconPx, iconPx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, iconPx, iconPx)
            drawable.draw(canvas)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
