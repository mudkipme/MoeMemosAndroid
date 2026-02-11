package me.mudkip.moememos.ui.page.resource

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items as staggeredGridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import me.mudkip.moememos.R
import me.mudkip.moememos.ext.popBackStackIfLifecycleIsResumed
import me.mudkip.moememos.ext.string
import me.mudkip.moememos.ui.component.Attachment
import me.mudkip.moememos.ui.component.MemoImage
import me.mudkip.moememos.viewmodel.ResourceListViewModel

private enum class ResourceFilter {
    IMAGE,
    OTHER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceListPage(
    navController: NavHostController,
    viewModel: ResourceListViewModel = hiltViewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var selectedFilter by rememberSaveable { mutableStateOf(ResourceFilter.IMAGE) }
    val imageResources = viewModel.resources.filter { it.mimeType?.startsWith("image/") == true }
    val otherResources = viewModel.resources.filterNot { it.mimeType?.startsWith("image/") == true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = R.string.resources.string) },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStackIfLifecycleIsResumed(lifecycleOwner)
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = R.string.back.string)
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    selected = selectedFilter == ResourceFilter.IMAGE,
                    onClick = { selectedFilter = ResourceFilter.IMAGE },
                    label = { Text(R.string.image.string) }
                )
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    selected = selectedFilter == ResourceFilter.OTHER,
                    onClick = { selectedFilter = ResourceFilter.OTHER },
                    label = { Text(R.string.other.string) }
                )
            }

            if (selectedFilter == ResourceFilter.IMAGE) {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalItemSpacing = 10.dp,
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    staggeredGridItems(imageResources, key = { it.identifier }) { resource ->
                        MemoImage(
                            url = resource.localUri ?: resource.uri,
                            resourceIdentifier = resource.identifier,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    lazyItems(otherResources, key = { it.identifier }) { resource ->
                        Attachment(resource = resource)
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadResources()
    }
}
