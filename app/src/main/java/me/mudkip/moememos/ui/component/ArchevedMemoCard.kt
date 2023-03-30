package me.mudkip.moememos.ui.component

import android.text.format.DateUtils
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.skydoves.sandwich.suspendOnSuccess
import kotlinx.coroutines.launch
import me.mudkip.moememos.R
import me.mudkip.moememos.data.model.Memo
import me.mudkip.moememos.ext.string
import me.mudkip.moememos.viewmodel.LocalArchivedMemos
import me.mudkip.moememos.viewmodel.LocalMemos

@Composable
fun ArchivedMemoCard(
    memo: Memo
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 15.dp, vertical = 10.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(start = 15.dp, bottom = 15.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    DateUtils.getRelativeTimeSpanString(memo.createdTs * 1000, System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS).toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.weight(1f))
                ArchivedMemosCardActionButton(memo)
            }

            MemoContent(memo)
        }
    }
}

@Composable
fun ArchivedMemosCardActionButton(
    memo: Memo
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val archivedMemoListViewModel = LocalArchivedMemos.current
    val memosViewModel = LocalMemos.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier
        .fillMaxSize()
        .wrapContentSize(Alignment.TopEnd)) {
        IconButton(onClick = { menuExpanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = null)
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            DropdownMenuItem(
                text = { Text(R.string.restore.string) },
                onClick = {
                    scope.launch {
                        archivedMemoListViewModel.restoreMemo(memo.id).suspendOnSuccess {
                            menuExpanded = false
                            memosViewModel.loadMemos()
                        }
                    }
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Restore,
                        contentDescription = null
                    )
                })
            DropdownMenuItem(
                text = { Text(R.string.delete.string) },
                onClick = {
                    showDeleteDialog = true
                    menuExpanded = false
                },
                colors = MenuDefaults.itemColors(
                    textColor = MaterialTheme.colorScheme.error,
                    leadingIconColor = MaterialTheme.colorScheme.error,
                ),
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = null
                    )
                })
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(R.string.delete_this_memo.string) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            archivedMemoListViewModel.deleteMemo(memo.id).suspendOnSuccess {
                                showDeleteDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(R.string.confirm.string)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                    }
                ) {
                    Text(R.string.cancel.string)
                }
            }
        )
    }
}