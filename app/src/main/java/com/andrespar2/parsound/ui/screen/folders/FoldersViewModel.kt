package com.andrespar2.parsound.ui.screen.folders

import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrespar2.parsound.data.local.FolderSummary
import com.andrespar2.parsound.data.prefs.FolderConfig
import com.andrespar2.parsound.data.repo.MusicRepository
import com.andrespar2.parsound.scanner.ScanState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FoldersViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {
    val configuredFolders: StateFlow<List<FolderConfig>> = repository.configuredFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val folderSummaries: StateFlow<List<FolderSummary>> = repository.folders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val scanState: StateFlow<ScanState> = repository.scanState
    val lastScan = repository.lastScan.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)
    val songCount = repository.songCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun addFolder(folder: FolderConfig) {
        viewModelScope.launch { repository.addFolder(folder) }
    }

    fun removeFolder(folder: FolderConfig) {
        viewModelScope.launch { repository.removeFolder(folder) }
    }

    fun scan() = repository.scanNow()
    fun cancelScan() = repository.cancelScan()

    fun importMediaStore(onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val count = runCatching { repository.importMediaStore() }.getOrDefault(0)
            onResult(count)
        }
    }
}
