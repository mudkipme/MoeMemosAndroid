package me.mudkip.moememos.ui.component

import android.icu.text.DateFormatSymbols
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.mudkip.moememos.ui.page.common.RouteName
import me.mudkip.moememos.viewmodel.MemosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SideDrawer(
    navController: NavHostController,
    memosViewModel: MemosViewModel,
    drawerState: DrawerState
) {
    val weekDays = remember {
        DateFormatSymbols.getInstance().shortWeekdays
    }
    var showHeatMap by remember {
        mutableStateOf(false)
    }
    val scope = rememberCoroutineScope()

    ModalDrawerSheet {
        LazyColumn {
            item {
                Stats(memosViewModel = memosViewModel)
            }
            
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .padding(10.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(end = 5.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(weekDays[1],
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline)
                        Text(weekDays[4],
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline)
                        Text(weekDays[7],
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline)
                    }
                    if (showHeatMap) {
                        Heatmap(memosViewModel = memosViewModel)
                    }
                }
            }
            
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
                        scope.launch {
                            drawerState.close()
                            navController.navigate(RouteName.SETTINGS)
                        }
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

            memosViewModel.tags.forEach { tag ->
                item {
                    NavigationDrawerItem(
                        label = { Text(tag) },
                        icon = { Icon(Icons.Outlined.Tag, contentDescription = null) },
                        selected = false,
                        onClick = { /*TODO*/ },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        memosViewModel.loadTags()
        delay(500)
        showHeatMap = true
    }
}
