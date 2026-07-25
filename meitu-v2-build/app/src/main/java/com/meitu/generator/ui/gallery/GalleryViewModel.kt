package com.meitu.generator.ui.gallery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.meitu.generator.data.local.entity.ImageEntity
import com.meitu.generator.repository.ImageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

enum class GalleryFilter { ALL, FAVORITE, TODAY, MONTH }

@HiltViewModel
class GalleryViewModel @Inject constructor(
    application: Application,
    private val imageRepo: ImageRepository
) : AndroidViewModel(application) {

    private val _filter = MutableStateFlow(GalleryFilter.ALL)
    val filter: StateFlow<GalleryFilter> = _filter.asStateFlow()

    private val _images = MutableStateFlow<List<ImageEntity>>(emptyList())
    val images: StateFlow<List<ImageEntity>> = _images.asStateFlow()

    private val _totalCount = MutableStateFlow(0)
    val totalCount: StateFlow<Int> = _totalCount.asStateFlow()

    private val _todayCount = MutableStateFlow(0)
    val todayCount: StateFlow<Int> = _todayCount.asStateFlow()

    private val _favoriteCount = MutableStateFlow(0)
    val favoriteCount: StateFlow<Int> = _favoriteCount.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectionMode = MutableStateFlow(false)
    val selectionMode: StateFlow<Boolean> = _selectionMode.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    private val _previewImage = MutableStateFlow<ImageEntity?>(null)
    val previewImage: StateFlow<ImageEntity?> = _previewImage.asStateFlow()

    private var currentPage = 0
    private val pageSize = 20

    init {
        loadImages()
        loadCounts()
    }

    private fun loadImages() {
        viewModelScope.launch {
            _isLoading.value = true
            currentPage = 0

            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            val startOfDay = cal.timeInMillis
            cal.set(Calendar.DAY_OF_MONTH, 1)
            val startOfMonth = cal.timeInMillis

            val flow = when (_filter.value) {
                GalleryFilter.ALL -> imageRepo.getImagesPaged(pageSize, 0)
                GalleryFilter.FAVORITE -> imageRepo.getFavoriteImagesPaged(pageSize, 0)
                GalleryFilter.TODAY -> imageRepo.getTodayImagesPaged(startOfDay, pageSize, 0)
                GalleryFilter.MONTH -> imageRepo.getMonthImagesPaged(startOfMonth, pageSize, 0)
            }
            flow.collect { list ->
                _images.value = list
                _isLoading.value = false
            }
        }
    }

    private fun loadCounts() {
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            val startOfDay = cal.timeInMillis
            cal.set(Calendar.DAY_OF_MONTH, 1)
            val startOfMonth = cal.timeInMillis

            combine(
                imageRepo.getTotalCount(),
                imageRepo.getTodayCount(startOfDay),
                imageRepo.getFavoriteCount()
            ) { total, today, fav ->
                Triple(total, today, fav)
            }.collect { (total, today, fav) ->
                _totalCount.value = total
                _todayCount.value = today
                _favoriteCount.value = fav
            }
        }
    }

    fun setFilter(f: GalleryFilter) {
        _filter.value = f
        loadImages()
    }

    fun loadMore() {
        viewModelScope.launch {
            currentPage++
            val offset = currentPage * pageSize

            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            val startOfDay = cal.timeInMillis
            cal.set(Calendar.DAY_OF_MONTH, 1)
            val startOfMonth = cal.timeInMillis

            val flow = when (_filter.value) {
                GalleryFilter.ALL -> imageRepo.getImagesPaged(pageSize, offset)
                GalleryFilter.FAVORITE -> imageRepo.getFavoriteImagesPaged(pageSize, offset)
                GalleryFilter.TODAY -> imageRepo.getTodayImagesPaged(startOfDay, pageSize, offset)
                GalleryFilter.MONTH -> imageRepo.getMonthImagesPaged(startOfMonth, pageSize, offset)
            }
            flow.first().let { newItems ->
                _images.value = _images.value + newItems
            }
        }
    }

    fun toggleFavorite(image: ImageEntity) {
        viewModelScope.launch {
            imageRepo.toggleFavorite(image.id, !image.isFavorite)
            _images.value = _images.value.map {
                if (it.id == image.id) it.copy(isFavorite = !it.isFavorite) else it
            }
        }
    }

    fun toggleSelectionMode() {
        _selectionMode.value = !_selectionMode.value
        _selectedIds.value = emptySet()
    }

    fun toggleSelection(id: Long) {
        _selectedIds.value = if (id in _selectedIds.value) _selectedIds.value - id else _selectedIds.value + id
    }

    fun batchDelete() {
        viewModelScope.launch {
            _selectedIds.value.forEach { id ->
                val img = _images.value.find { it.id == id }
                if (img != null) imageRepo.delete(img)
            }
            _selectionMode.value = false
            _selectedIds.value = emptySet()
            loadImages()
        }
    }

    fun batchFavorite() {
        viewModelScope.launch {
            _selectedIds.value.forEach { id ->
                imageRepo.toggleFavorite(id, true)
            }
            _selectionMode.value = false
            _selectedIds.value = emptySet()
            loadImages()
        }
    }

    fun deleteImage(image: ImageEntity) {
        viewModelScope.launch {
            imageRepo.delete(image)
            loadImages()
        }
    }

    fun setPreviewImage(image: ImageEntity?) { _previewImage.value = image }
}
