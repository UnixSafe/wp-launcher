package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LauncherDao {
    @Query("SELECT * FROM tiles ORDER BY position ASC")
    fun getAllTilesBlock(): List<TileEntity>

    @Query("SELECT * FROM tiles ORDER BY position ASC")
    fun getAllTiles(): Flow<List<TileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTile(tile: TileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTiles(tiles: List<TileEntity>)

    @Update
    suspend fun updateTile(tile: TileEntity)

    @Delete
    suspend fun deleteTile(tile: TileEntity)

    @Query("DELETE FROM tiles WHERE packageName = :packageName")
    suspend fun deleteTileByPackage(packageName: String)

    @Query("DELETE FROM tiles")
    suspend fun clearAllTiles()

    @Query("SELECT * FROM settings WHERE id = 1")
    fun getSettings(): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = 1")
    suspend fun getSettingsDirect(): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: SettingsEntity)
}
