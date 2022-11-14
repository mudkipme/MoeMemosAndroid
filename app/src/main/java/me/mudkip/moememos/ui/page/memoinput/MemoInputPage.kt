package me.mudkip.moememos.ui.page.memoinput

import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.skydoves.sandwich.suspendOnSuccess
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.mudkip.moememos.ext.suspendOnErrorMessage
import me.mudkip.moememos.ui.component.Attachment
import me.mudkip.moememos.ui.component.InputImage
import me.mudkip.moememos.ui.page.common.LocalRootNavController
import me.mudkip.moememos.viewmodel.LocalMemos
import me.mudkip.moememos.viewmodel.MemoInputViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoInputPage(
    viewModel: MemoInputViewModel = hiltViewModel(),
    memoId: Long? = null
) {
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val snackbarState = remember { SnackbarHostState() }
    val navController = LocalRootNavController.current
    val memosViewModel = LocalMemos.current
    val memo = remember { memosViewModel.memos.find { it.id == memoId } }
    var text by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(memo?.content ?: "", TextRange(memo?.content?.length ?: 0)))
    }
    var tagMenuExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val pickImage = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        uri?.let {
            coroutineScope.launch {
                try {
                    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, it))
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                    }
                    viewModel.upload(bitmap).suspendOnSuccess {
                        delay(300)
                        focusRequester.requestFocus()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun submit() = coroutineScope.launch {
        memo?.let {
            viewModel.editMemo(memo.id, text.text).suspendOnSuccess {
                navController.popBackStack()
            }.suspendOnErrorMessage { message ->
                snackbarState.showSnackbar(message)
            }
            return@launch
        }

        viewModel.createMemo(text.text).suspendOnSuccess {
            text = TextFieldValue("")
            viewModel.updateDraft("")
            navController.popBackStack()
        }.suspendOnErrorMessage { message ->
            snackbarState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { if (memo == null) { Text("Compose") } else { Text("Edit") } },
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
                if (memosViewModel.tags.isEmpty()) {
                    IconButton(onClick = {
                        text = text.copy(
                            text.text.replaceRange(text.selection.min, text.selection.max, "#"),
                            TextRange(text.selection.min + 1)
                        )
                    }) {
                        Icon(Icons.Outlined.Tag, contentDescription = "Tag")
                    }
                } else {
                    Box {
                        DropdownMenu(
                            expanded = tagMenuExpanded,
                            onDismissRequest = { tagMenuExpanded = false },
                            properties = PopupProperties(focusable = false)
                        ) {
                            memosViewModel.tags.forEach { tag ->
                                DropdownMenuItem(
                                    text = { Text(tag) },
                                    onClick = {
                                        val tagText = "#${tag} "
                                        text = text.copy(
                                            text.text.replaceRange(text.selection.min, text.selection.max, tagText),
                                            TextRange(text.selection.min + tagText.length)
                                        )
                                        tagMenuExpanded = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Outlined.Tag,
                                            contentDescription = null
                                        )
                                    })
                            }
                        }
                        IconButton(onClick = { tagMenuExpanded = true }) {
                            Icon(Icons.Outlined.Tag, contentDescription = "Tag")
                        }
                    }
                }

                IconButton(onClick = {
                    pickImage.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                }) {
                    Icon(Icons.Outlined.Image, contentDescription = "Add Image")
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    enabled = text.text.isNotEmpty(),
                    onClick = { submit() }
                ) {
                    Icon(Icons.Filled.Send, contentDescription = "Post")
                }
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarState)
        }
    ) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .fillMaxHeight()) {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 30.dp)
                    .weight(1f)
                    .focusRequester(focusRequester),
                value = text,
                label = { Text("Any thoughts…" )},
                onValueChange = {
                    text = it
                },
            )

            if (viewModel.uploadResources.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .height(80.dp)
                        .padding(start = 15.dp, end = 15.dp, bottom = 15.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(viewModel.uploadResources, { it.id }) { resource ->
                        if (resource.type.startsWith("image/")) {
                            InputImage(resource = resource, inputViewModel = viewModel)
                        } else {
                            Attachment(resource = resource)
                        }
                    }
                }
            }
        }
    }
    
    LaunchedEffect(Unit) {
        if (memo == null) {
            viewModel.draft.first()?.let {
                text = TextFieldValue(it, TextRange(it.length))
            }
        } else {
            memo.resourceList?.let { resourceList -> viewModel.uploadResources.addAll(resourceList) }
        }
        delay(300)
        focusRequester.requestFocus()
    }

    DisposableEffect(Unit) {
        onDispose {
            if (memo == null) {
                viewModel.updateDraft(text.text)
            }
        }
    }
}