package me.mudkip.moememos.ui.component

import android.content.Intent
import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.skydoves.sandwich.suspendOnSuccess
import kotlinx.coroutines.launch
import me.mudkip.moememos.R
import me.mudkip.moememos.data.model.Memo
import me.mudkip.moememos.data.model.MemosRowStatus
import me.mudkip.moememos.ext.icon
import me.mudkip.moememos.ext.string
import me.mudkip.moememos.ext.titleResource
import me.mudkip.moememos.ui.page.common.LocalRootNavController
import me.mudkip.moememos.ui.page.common.RouteName
import me.mudkip.moememos.viewmodel.LocalMemos
import me.mudkip.moememos.viewmodel.LocalUserState

@Composable
fun MemosCard(
    memo: Memo
) {
    val memosViewModel = LocalMemos.current
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .padding(horizontal = 15.dp, vertical = 10.dp)
            .fillMaxWidth(),
        border = if (memo.pinned && memo.rowStatus == MemosRowStatus.NORMAL) { BorderStroke(1.dp, MaterialTheme.colorScheme.primary) } else { null }
    ) {
        Column(
            modifier = Modifier.padding(start = 15.dp, bottom = 15.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(DateUtils.getRelativeTimeSpanString(memo.createdTs * 1000, System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS).toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.outline
                )
                if (LocalUserState.current.currentUser?.memoVisibility != memo.visibility) {
                    Icon(
                        memo.visibility.icon,
                        contentDescription = stringResource(memo.visibility.titleResource),
                        modifier = Modifier
                            .padding(start = 5.dp)
                            .size(20.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                MemosCardActionButton(memo)
            }

            MemoContent(memo, checkboxChange = { checked, startOffset, endOffset ->
                scope.launch {
                    var text = memo.content.substring(startOffset, endOffset)
                    text = if (checked) {
                        text.replace("[ ]", "[x]")
                    } else {
                        text.replace("[x]", "[ ]")
                    }
                    memosViewModel.editMemo(memo.id, memo.content.replaceRange(startOffset, endOffset, text), memo.resourceList)
                }
            })
        }
    }
}

@Composable
fun MemosCardActionButton(
    memo: Memo
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val memosViewModel = LocalMemos.current
    val rootNavController = LocalRootNavController.current
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier
        .fillMaxSize()
        .wrapContentSize(Alignment.TopEnd)) {
        IconButton(onClick = { menuExpanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = null)
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            if (memo.pinned) {
                DropdownMenuItem(
                    text = { Text(R.string.unpin.string) },
                    onClick = {
                        scope.launch {
                            memosViewModel.updateMemoPinned(memo.id, false).suspendOnSuccess {
                                menuExpanded = false
                            }
                        }
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.PinDrop,
                            contentDescription = null
                        )
                    })
            } else {
                DropdownMenuItem(
                    text = { Text(R.string.pin.string) },
                    onClick = {
                        scope.launch {
                            memosViewModel.updateMemoPinned(memo.id, true).suspendOnSuccess {
                                menuExpanded = false
                            }
                        }
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.PushPin,
                            contentDescription = null
                        )
                    })
            }
            DropdownMenuItem(
                text = { Text(R.string.edit.string) },
                onClick = {
                    rootNavController.navigate("${RouteName.EDIT}?memoId=${memo.id}")
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = null
                    )
                })
            DropdownMenuItem(
                text = { Text(R.string.share.string) },
                onClick = {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, memo.content)
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, null)
                    context.startActivity(shareIntent)
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Share,
                        contentDescription = null
                    )
                })
            DropdownMenuItem(
                text = { Text(R.string.archive.string) },
                onClick = {
                    scope.launch {
                        memosViewModel.archiveMemo(memo.id).suspendOnSuccess {
                            menuExpanded = false
                        }
                    }
                },
                colors = MenuDefaults.itemColors(
                    textColor = MaterialTheme.colorScheme.error,
                    leadingIconColor = MaterialTheme.colorScheme.error,
                ),
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Archive,
                        contentDescription = null
                    )
                })
        }
    }
}