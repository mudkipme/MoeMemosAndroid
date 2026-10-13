package me.mudkip.moememos.ui.page.memos

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.mudkip.moememos.data.model.Settings
import me.mudkip.moememos.ext.settingsDataStore
import me.mudkip.moememos.util.archivedSearchResults
import me.mudkip.moememos.viewmodel.ArchivedMemoListViewModel
import me.mudkip.moememos.viewmodel.LocalArchivedMemos
import me.mudkip.moememos.R
import me.mudkip.moememos.ext.popBackStackIfLifecycleIsResumed
import me.mudkip.moememos.ui.component.ActionIconButton
import me.mudkip.moememos.ui.page.common.RouteName

@Composable
fun SearchPage(
    navController: NavHostController,
    archivedViewModel: ArchivedMemoListViewModel = hiltViewModel(),
) {
    val searchText = rememberTextFieldState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val settings by context.settingsDataStore.data.collectAsStateWithLifecycle(initialValue = Settings())
    val includeArchived = settings.usersList
        .firstOrNull { it.accountKey == settings.currentUser }
        ?.settings
        ?.searchIncludesArchived
        ?: false

    LaunchedEffect(includeArchived) {
        if (includeArchived) archivedViewModel.loadMemos()
    }

    val query = searchText.text.toString()
    val archivedResults = remember(archivedViewModel.memos.toList(), query, includeArchived) {
        archivedSearchResults(archivedViewModel.memos, query, includeArchived)
    }

    CompositionLocalProvider(LocalArchivedMemos provides archivedViewModel) {
        MemoBrowser { onMemoClick ->
            Scaffold(
                topBar = {
                    MemoSearchBar(searchText) {
                        navController.popBackStackIfLifecycleIsResumed(lifecycleOwner)
                    }
                },
            ) { innerPadding ->
                MemosList(
                    contentPadding = innerPadding,
                    searchString = query,
                    onMemoClick = onMemoClick,
                    onTagClick = { tag ->
                        navController.navigate("${RouteName.TAG}/${Uri.encode(tag)}") {
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    archivedResults = archivedResults,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MemoSearchBar(searchText: TextFieldState, onBack: () -> Unit) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    SearchBar(
        state = rememberSearchBarState(),
        modifier = Modifier
            .windowInsetsPadding(SearchBarDefaults.windowInsets)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
        inputField = {
            SearchBarDefaults.InputField(
                state = searchText,
                onSearch = { focusManager.clearFocus() },
                // Results occupy this route's list pane, rather than an overlay.
                expanded = false,
                onExpandedChange = {},
                modifier = Modifier.focusRequester(focusRequester),
                placeholder = { Text(stringResource(R.string.search)) },
                leadingIcon = {
                    ActionIconButton(label = stringResource(R.string.back), onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    }
                },
                trailingIcon = {
                    if (searchText.text.isNotEmpty()) {
                        ActionIconButton(
                            label = stringResource(R.string.clear_search),
                            onClick = {
                                searchText.edit { replace(0, length, "") }
                                focusRequester.requestFocus()
                            },
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = null)
                        }
                    }
                },
            )
        },
    )
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}
