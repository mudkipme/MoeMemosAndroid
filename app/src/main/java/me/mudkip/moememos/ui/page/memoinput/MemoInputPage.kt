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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.skydoves.sandwich.suspendOnSuccess
import kotlinx.coroutines.launch
import me.mudkip.moememos.ext.suspendOnErrorMessage
import me.mudkip.moememos.viewmodel.MemoInputViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoInputPage(
    navController: NavHostController,
    viewModel: MemoInputViewModel = hiltViewModel()
) {
    var text by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val snackbarState = remember { SnackbarHostState() }

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

                IconButton(
                    enabled = text.text.isNotEmpty(),
                    onClick = {
                        coroutineScope.launch {
                            viewModel.createMemo(text.text).suspendOnSuccess {
                                navController.popBackStack()
                            }.suspendOnErrorMessage { message ->
                                snackbarState.showSnackbar(message)
                            }
                        }
                    }
                ) {
                    Icon(Icons.Filled.Send, contentDescription = "Post")
                }
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarState)
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