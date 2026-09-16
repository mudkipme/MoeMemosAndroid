package me.mudkip.moememos.ui.page.memos

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import me.mudkip.moememos.R
import me.mudkip.moememos.viewmodel.LocalUserState

@Composable
internal fun MemoBrowser(listPane: @Composable ((String) -> Unit) -> Unit) {
    val account by LocalUserState.current.currentAccount.collectAsStateWithLifecycle()
    // A selected memo and its pane history belong to one account only.
    key(account?.accountKey()) {
        MemoListDetailLayout(
            listPane = listPane,
            detailPane = { identifier, onBack -> MemoDetailContent(identifier, onBack) },
        )
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun MemoListDetailLayout(
    modifier: Modifier = Modifier,
    listPane: @Composable ((String) -> Unit) -> Unit,
    detailPane: @Composable (String, () -> Unit) -> Unit,
) {
    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val focusManager = LocalFocusManager.current
    BoxWithConstraints(modifier.fillMaxSize()) {
        // Window width also includes the permanent drawer. Use the space actually
        // available to these panes, while retaining folding/hinge information.
        val directive = calculatePaneScaffoldDirective(adaptiveInfo).copy(
            maxHorizontalPartitions = if (maxWidth >= 840.dp) 2 else 1,
        )
        val navigator = rememberListDetailPaneScaffoldNavigator<String>(
            scaffoldDirective = directive,
        )
        val scope = rememberCoroutineScope()
        val selectedMemo = navigator.currentDestination?.contentKey
        NavigableListDetailPaneScaffold(
            navigator = navigator,
            defaultBackBehavior = BackNavigationBehavior.PopUntilScaffoldValueChange,
            listPane = {
                AnimatedPane {
                    listPane { identifier ->
                        focusManager.clearFocus()
                        if (identifier != selectedMemo) {
                            scope.launch {
                                navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, identifier)
                            }
                        }
                    }
                }
            },
            detailPane = {
                AnimatedPane {
                    if (selectedMemo == null) {
                        Surface(Modifier.fillMaxSize()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(stringResource(R.string.select_memo))
                            }
                        }
                    } else {
                        key(selectedMemo) {
                            detailPane(selectedMemo) {
                                scope.launch {
                                    navigator.navigateBack(
                                        if (directive.maxHorizontalPartitions == 1)
                                            BackNavigationBehavior.PopUntilScaffoldValueChange
                                        else BackNavigationBehavior.PopUntilContentChange
                                    )
                                }
                            }
                        }
                    }
                }
            },
        )
    }
}
