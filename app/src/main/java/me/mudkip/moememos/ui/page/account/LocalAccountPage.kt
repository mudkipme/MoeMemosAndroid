package me.mudkip.moememos.ui.page.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import me.mudkip.moememos.R
import me.mudkip.moememos.data.model.Account
import me.mudkip.moememos.data.model.displayTitle
import me.mudkip.moememos.ext.string

@Composable
fun LocalAccountPage(
    innerPadding: PaddingValues,
    showSwitchAccountButton: Boolean,
    transferTargets: List<Account>,
    transferInProgress: Boolean,
    onSwitchAccount: () -> Unit,
    onTransferLocalMemos: (Account) -> Unit,
    onExportLocalAccount: () -> Unit
) {
    var showTransferDialog by remember { mutableStateOf(false) }

    LazyColumn(contentPadding = innerPadding) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(15.dp)
            ) {
                Column(Modifier.padding(15.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Home,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clip(CircleShape),
                        )
                        Text(
                            R.string.local_account.string,
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                    Text(
                        R.string.local_account_description.string,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    Text(
                        R.string.local_account_non_removable.string,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }
        }

        if (showSwitchAccountButton) {
            item {
                FilledTonalButton(
                    onClick = onSwitchAccount,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 10.dp),
                    contentPadding = PaddingValues(10.dp)
                ) {
                    Text(R.string.switch_account.string)
                }
            }
        }

        if (transferTargets.isNotEmpty()) {
            item {
                FilledTonalButton(
                    onClick = { showTransferDialog = true },
                    enabled = !transferInProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 10.dp),
                    contentPadding = PaddingValues(10.dp)
                ) {
                    Text(R.string.transfer_local_memos.string)
                }
            }
        }

        item {
            FilledTonalButton(
                onClick = onExportLocalAccount,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 20.dp),
                contentPadding = PaddingValues(10.dp)
            ) {
                Text(R.string.export_local_account.string)
            }
        }
    }

    if (showTransferDialog) {
        TransferLocalMemosDialog(
            targets = transferTargets,
            onConfirm = { target ->
                showTransferDialog = false
                onTransferLocalMemos(target)
            },
            onDismiss = { showTransferDialog = false }
        )
    }
}

@Composable
private fun TransferLocalMemosDialog(
    targets: List<Account>,
    onConfirm: (Account) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf(targets.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(R.string.transfer_local_memos.string) },
        text = {
            Column {
                Text(R.string.transfer_local_memos_message.string)
                targets.forEach { account ->
                    val info = account.getAccountInfo()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .selectable(
                                selected = selected.accountKey() == account.accountKey(),
                                onClick = { selected = account },
                                role = Role.RadioButton
                            )
                    ) {
                        RadioButton(
                            selected = selected.accountKey() == account.accountKey(),
                            onClick = null
                        )
                        Column(Modifier.padding(start = 8.dp)) {
                            Text(info?.displayTitle().orEmpty())
                            Text(
                                info?.host.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) {
                Text(R.string.transfer_local_memos.string)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(R.string.cancel.string)
            }
        }
    )
}
