package me.mudkip.moememos.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import me.mudkip.moememos.ui.page.common.RouteName
import java.net.URLEncoder

@Composable
fun TagDrawerItem(
    tag: String,
    memosNavController: NavHostController,
    drawerState: DrawerState? = null
) {
    val scope = rememberCoroutineScope()

    NavigationDrawerItem(
        label = { Text(tag) },
        icon = { Icon(Icons.Outlined.Tag, contentDescription = null) },
        selected = false,
        onClick = {
            scope.launch {
                memosNavController.navigate("${RouteName.TAG}/${URLEncoder.encode(tag, "UTF-8")}") {
                    launchSingleTop = true
                    restoreState = true
                }
                drawerState?.close()
            }
        },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}