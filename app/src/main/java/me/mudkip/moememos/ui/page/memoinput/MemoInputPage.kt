package me.mudkip.moememos.ui.page.memoinput

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoInputPage(
    navController: NavHostController
) {
    var text by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    val focusRequester = remember { FocusRequester() }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { Text("Compose") },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                },
            )
        },
        bottomBar = {
            BottomAppBar {
                IconButton(onClick = {
                }) {
                    Icon(Icons.Outlined.Tag, contentDescription = "Tag")
                }

                IconButton(onClick = {
                }) {
                    Icon(Icons.Outlined.Image, contentDescription = "Add Image")
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = {
                }) {
                    Icon(Icons.Filled.Send, contentDescription = "Post")
                }
            }
        }
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            OutlinedTextField(
                modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp, bottom = 30.dp)
                    .fillMaxWidth()
                    .weight(1f)
                    .focusRequester(focusRequester),
                value = text,
                label = { Text("Any thoughts…" )},
                onValueChange = {
                    text = it
                },
            )
        }
    }
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}