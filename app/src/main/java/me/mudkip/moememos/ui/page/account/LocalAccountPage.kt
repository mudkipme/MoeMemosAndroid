package me.mudkip.moememos.ui.page.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import me.mudkip.moememos.R
import me.mudkip.moememos.ext.string

@Composable
fun LocalAccountPage(
    innerPadding: PaddingValues,
    showSwitchAccountButton: Boolean,
    onSwitchAccount: () -> Unit,
    onExportLocalAccount: () -> Unit
) {
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
}
