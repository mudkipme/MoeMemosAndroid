package me.mudkip.moememos.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skydoves.sandwich.suspendOnSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import me.mudkip.moememos.data.local.entity.ResourceEntity
import me.mudkip.moememos.data.service.MemoService
import javax.inject.Inject

@HiltViewModel
class ResourceListViewModel @Inject constructor(
    private val memoService: MemoService
): ViewModel() {
    var resources = mutableStateListOf<ResourceEntity>()
        private set

    fun loadResources() = viewModelScope.launch {
        memoService.repository.listResources().suspendOnSuccess {
            resources.clear()
            resources.addAll(data.filter { it.mimeType?.startsWith("image/") == true })
        }
    }
}
