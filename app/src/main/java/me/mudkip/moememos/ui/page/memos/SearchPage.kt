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
import me.mudkip.moememos.R
import me.mudkip.moememos.ext.popBackStackIfLifecycleIsResumed
import me.mudkip.moememos.ui.component.ActionIconButton
import me.mudkip.moememos.ui.page.common.RouteName

@Composable
fun SearchPage(navController: NavHostController) {
    val searchText = rememberTextFieldState()
    val lifecycleOwner = LocalLifecycleOwner.current
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
                searchString = searchText.text.toString(),
                onMemoClick = onMemoClick,
                onTagClick = { tag ->
                    navController.navigate("${RouteName.TAG}/${Uri.encode(tag)}") {
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
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
