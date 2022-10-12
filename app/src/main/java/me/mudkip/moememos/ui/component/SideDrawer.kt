package me.mudkip.moememos.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import me.mudkip.moememos.ui.page.common.RouteName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SideDrawer(
    navController: NavHostController,
) {
    ModalDrawerSheet {
        LazyColumn {
            item {
                Text(
                    "Moe Memos",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(20.dp)
                )
            }
            item {
                NavigationDrawerItem(
                    label = { Text("Memos") },
                    icon = { Icon(Icons.Outlined.GridView, contentDescription = null) },
                    selected = true,
                    onClick = { /*TODO*/ },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
            item {
                NavigationDrawerItem(
                    label = { Text("Resources") },
                    icon = { Icon(Icons.Outlined.PhotoLibrary, contentDescription = null) },
                    selected = false,
                    onClick = { /*TODO*/ },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
            item {
                NavigationDrawerItem(
                    label = { Text("Archived") },
                    icon = { Icon(Icons.Outlined.Inventory2, contentDescription = null) },
                    selected = false,
                    onClick = { /*TODO*/ },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
            item {
                NavigationDrawerItem(
                    label = { Text("Settings") },
                    icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                    selected = false,
                    onClick = {
                        navController.navigate(RouteName.SETTINGS)
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }

            item {
                Divider(Modifier.padding(vertical = 10.dp))
            }

            item {
                Text("Tags", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(20.dp))
            }

            item {
                NavigationDrawerItem(
                    label = { Text("Hardware") },
                    icon = { Icon(Icons.Outlined.Tag, contentDescription = null) },
                    selected = false,
                    onClick = { /*TODO*/ },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    }
}
