package me.mudkip.moememos.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skydoves.sandwich.suspendOnSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import me.mudkip.moememos.data.api.MemosV0Resource
import me.mudkip.moememos.data.repository.MemosV0Repository
import javax.inject.Inject

@HiltViewModel
class ResourceListViewModel @Inject constructor(
    private val memoRepository: MemosV0Repository
): ViewModel() {
    var resources = mutableStateListOf<MemosV0Resource>()
        private set

    fun loadResources() = viewModelScope.launch {
        memoRepository.loadResources().suspendOnSuccess {
            resources.clear()
            resources.addAll(data.filter { it.type.startsWith("image/") })
        }
    }
}