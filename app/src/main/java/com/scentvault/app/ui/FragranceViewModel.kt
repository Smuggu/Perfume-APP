package com.scentvault.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scentvault.app.ScentVaultApp
import com.scentvault.app.backup.BackupManager
import com.scentvault.app.data.Fragrance
import com.scentvault.app.data.FragranceRepository
import com.scentvault.app.data.FragranceWithTags
import com.scentvault.app.photo.PhotoStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface BackupEvent {
    data class ExportSuccess(val count: Int) : BackupEvent
    data class ImportSuccess(val count: Int) : BackupEvent
    data class Failure(val message: String) : BackupEvent
}

class FragranceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FragranceRepository = (application as ScentVaultApp).repository

    private val searchQuery = MutableStateFlow("")
    private val selectedTags = MutableStateFlow<Set<String>>(emptySet())

    val searchQueryState: StateFlow<String> = searchQuery
    val selectedTagsState: StateFlow<Set<String>> = selectedTags

    val allTags: StateFlow<List<String>> = repository.observeAllTags()
        .map { tags -> tags.map { it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val filteredFragrances: StateFlow<List<FragranceWithTags>> = combine(
        repository.observeAllWithTags(),
        searchQuery,
        selectedTags
    ) { all, query, tags ->
        all.filter { entry ->
            matchesQuery(entry, query) && matchesTags(entry, tags)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _backupEvents = MutableStateFlow<BackupEvent?>(null)
    val backupEvents: StateFlow<BackupEvent?> = _backupEvents

    private fun matchesQuery(entry: FragranceWithTags, query: String): Boolean {
        if (query.isBlank()) return true
        val needle = query.trim()
        val f = entry.fragrance
        return listOf(f.name, f.brand, f.batchCode, f.notes)
            .any { it.contains(needle, ignoreCase = true) } ||
            entry.tags.any { it.name.contains(needle, ignoreCase = true) }
    }

    private fun matchesTags(entry: FragranceWithTags, tags: Set<String>): Boolean {
        if (tags.isEmpty()) return true
        val entryTagNames = entry.tags.map { it.name }.toSet()
        return tags.all { it in entryTagNames }
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun toggleTagFilter(tag: String) {
        selectedTags.value = if (tag in selectedTags.value) {
            selectedTags.value - tag
        } else {
            selectedTags.value + tag
        }
    }

    fun clearTagFilters() {
        selectedTags.value = emptySet()
    }

    fun observeFragrance(id: Long): Flow<FragranceWithTags?> = repository.observeOneWithTags(id)

    fun save(fragrance: Fragrance, tagNames: List<String>, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.save(fragrance, tagNames)
            onSaved(id)
        }
    }

    fun delete(fragrance: Fragrance) {
        viewModelScope.launch {
            repository.delete(fragrance)
            PhotoStore.delete(getApplication(), fragrance.photoPath)
        }
    }

    fun importPhoto(uri: Uri, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val name = withContext(Dispatchers.IO) {
                PhotoStore.importImage(getApplication(), uri)
            }
            onResult(name)
        }
    }

    fun createCameraCaptureUri() = PhotoStore.createCameraCaptureUri(getApplication())

    fun exportBackup(destination: Uri) {
        viewModelScope.launch {
            try {
                val result = BackupManager.export(getApplication(), repository, destination)
                _backupEvents.value = BackupEvent.ExportSuccess(result.fragranceCount)
            } catch (e: Exception) {
                _backupEvents.value = BackupEvent.Failure(e.message ?: "Export failed")
            }
        }
    }

    fun importBackup(source: Uri) {
        viewModelScope.launch {
            try {
                val result = BackupManager.import(getApplication(), repository, source)
                _backupEvents.value = BackupEvent.ImportSuccess(result.fragranceCount)
            } catch (e: Exception) {
                _backupEvents.value = BackupEvent.Failure(e.message ?: "Import failed")
            }
        }
    }

    fun consumeBackupEvent() {
        _backupEvents.value = null
    }
}
