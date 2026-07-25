package com.meitu.generator.repository

import android.content.Context
import com.meitu.generator.data.local.dao.ImageDao
import com.meitu.generator.data.local.entity.ImageEntity
import com.meitu.generator.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageRepository @Inject constructor(
    private val imageDao: ImageDao,
    @ApplicationContext private val context: Context
) {
    fun getImagesPaged(limit: Int, offset: Int): Flow<List<ImageEntity>> =
        imageDao.getImagesPaged(limit, offset)

    fun getFavoriteImagesPaged(limit: Int, offset: Int): Flow<List<ImageEntity>> =
        imageDao.getFavoriteImagesPaged(limit, offset)

    fun getTodayImagesPaged(startOfDay: Long, limit: Int, offset: Int): Flow<List<ImageEntity>> =
        imageDao.getTodayImagesPaged(startOfDay, limit, offset)

    fun getMonthImagesPaged(startOfMonth: Long, limit: Int, offset: Int): Flow<List<ImageEntity>> =
        imageDao.getMonthImagesPaged(startOfMonth, limit, offset)

    fun getTotalCount(): Flow<Int> = imageDao.getTotalCount()
    fun getSuccessCount(): Flow<Int> = imageDao.getSuccessCount()
    fun getTodayCount(startOfDay: Long): Flow<Int> = imageDao.getTodayCount(startOfDay)
    fun getMonthCount(startOfMonth: Long): Flow<Int> = imageDao.getMonthCount(startOfMonth)
    fun getFailedCount(): Flow<Int> = imageDao.getFailedCount()
    fun getTodayFailedCount(startOfDay: Long): Flow<Int> = imageDao.getTodayFailedCount(startOfDay)
    fun getFavoriteCount(): Flow<Int> = imageDao.getFavoriteCount()
    fun getCloudBackupCount(): Flow<Int> = imageDao.getCloudBackupCount()

    suspend fun insert(image: ImageEntity): Long = imageDao.insert(image)
    suspend fun update(image: ImageEntity) = imageDao.update(image)
    suspend fun delete(image: ImageEntity) {
        // Delete local file
        if (image.localPath.isNotEmpty()) {
            File(image.localPath).delete()
        }
        imageDao.delete(image)
    }
    suspend fun getById(id: Long): ImageEntity? = imageDao.getById(id)
    suspend fun toggleFavorite(id: Long, fav: Boolean) = imageDao.toggleFavorite(id, fav)

    fun getImageDir(): File {
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val dir = File(context.getExternalFilesDir("Images"), dateStr)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun generateImagePath(): String {
        val dir = getImageDir()
        val timestamp = System.currentTimeMillis()
        return File(dir, "${timestamp}_001.jpg").absolutePath
    }

    fun clearCache() {
        val imagesDir = context.getExternalFilesDir("Images")
        imagesDir?.let {
            it.walkTopDown().filter { f -> f.isFile && f.name.contains("thumbnail") }.forEach { f -> f.delete() }
        }
        context.cacheDir.deleteRecursively()
    }
}
