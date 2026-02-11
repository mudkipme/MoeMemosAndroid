package me.mudkip.moememos.ui.page.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImage
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import me.mudkip.moememos.R
import me.mudkip.moememos.data.api.MemosProfile
import me.mudkip.moememos.data.model.MemosAccount
import me.mudkip.moememos.ext.string
import me.mudkip.moememos.ui.component.MemosIcon
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient

@OptIn(ExperimentalCoilApi::class)
@Composable
fun MemosAccountPage(
    innerPadding: PaddingValues,
    account: MemosAccount,
    profile: MemosProfile?,
    okHttpClient: OkHttpClient,
    showSwitchAccountButton: Boolean,
    onSwitchAccount: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val accountHost = account.host.toHttpUrlOrNull()?.host.orEmpty()
    val accountName = account.name.ifBlank { accountHost }
    val accountAvatarUrl = resolveAvatarUrl(account.host, account.avatarUrl)
    val imageLoader = remember(context, okHttpClient) {
        ImageLoader.Builder(context)
            .components {
                add(
                    OkHttpNetworkFetcherFactory(
                        callFactory = { okHttpClient }
                    )
                )
            }
            .build()
    }

    LazyColumn(contentPadding = innerPadding) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(15.dp)
            ) {
                Column(Modifier.padding(15.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (accountAvatarUrl.isNullOrBlank()) {
                            Icon(
                                imageVector = Icons.Outlined.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape),
                            )
                        } else {
                            AsyncImage(
                                model = accountAvatarUrl,
                                imageLoader = imageLoader,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape),
                            )
                        }
                        Column(Modifier.padding(start = 10.dp)) {
                            Text(
                                accountName,
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                accountHost,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    if (profile?.version?.isNotEmpty() == true) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 8.dp),
                        ) {
                            Icon(
                                imageVector = MemosIcon,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .clip(CircleShape),
                            )
                            Text(
                                "memos v${profile.version}",
                                modifier = Modifier.padding(top = 5.dp),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
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
                onClick = onSignOut,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                contentPadding = PaddingValues(10.dp)
            ) {
                Text(R.string.sign_out.string)
            }
        }
    }
}

private fun resolveAvatarUrl(host: String, avatarUrl: String): String? {
    if (avatarUrl.isBlank()) {
        return null
    }
    if (avatarUrl.toHttpUrlOrNull() != null || "://" in avatarUrl) {
        return avatarUrl
    }
    val baseUrl = host.toHttpUrlOrNull() ?: return avatarUrl
    return runCatching {
        baseUrl.toUrl().toURI().resolve(avatarUrl).toString()
    }.getOrDefault(avatarUrl)
}
