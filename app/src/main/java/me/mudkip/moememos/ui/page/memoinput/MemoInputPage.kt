package me.mudkip.moememos.ui.page.memoinput

import android.content.ActivityNotFoundException
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.skydoves.sandwich.suspendOnSuccess
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.mudkip.moememos.MoeMemosFileProvider
import me.mudkip.moememos.R
import me.mudkip.moememos.data.constant.LIST_ITEM_SYMBOL_LIST
import me.mudkip.moememos.data.model.ShareContent
import me.mudkip.moememos.ext.string
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
    memoId: Long? = null,
    shareContent: ShareContent? = null
) {
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val snackbarState = remember { SnackbarHostState() }
    val navController = LocalRootNavController.current
    val memosViewModel = LocalMemos.current
    val memo = remember { memosViewModel.memos.toList().find { it.id == memoId } }
    var text by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(memo?.content ?: "", TextRange(memo?.content?.length ?: 0)))
    }
    var tagMenuExpanded by remember { mutableStateOf(false) }
    var photoImageUri by remember { mutableStateOf<Uri?>(null) }

    val context = LocalContext.current

    fun uploadImage(uri: Uri) = coroutineScope.launch {
        try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
            viewModel.upload(bitmap).suspendOnSuccess {
                delay(300)
                focusRequester.requestFocus()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleTodoItem() {
        val contentBefore = text.text.substring(0, text.selection.min)
        val lastLineBreak = contentBefore.indexOfLast { it == '\n' }
        val nextLineBreak = text.text.indexOf('\n', lastLineBreak + 1)
        val currentLine = text.text.substring(
            lastLineBreak + 1,
            if (nextLineBreak == -1) text.text.length else nextLineBreak
        )
        val contentBeforeCurrentLine = contentBefore.substring(0, lastLineBreak + 1)
        val contentAfterCurrentLine = if (nextLineBreak == -1) "" else text.text.substring(nextLineBreak)

        for (prefix in LIST_ITEM_SYMBOL_LIST) {
            if (!currentLine.startsWith(prefix)) {
                continue
            }

            if (prefix == "- [ ] ") {
                text = text.copy(contentBeforeCurrentLine + "- [x] " + currentLine.substring(prefix.length) + contentAfterCurrentLine)
                return
            }

            val offset =  "- [ ] ".length - prefix.length
            text = text.copy(
                contentBeforeCurrentLine + "- [ ] " + currentLine.substring(prefix.length) + contentAfterCurrentLine,
                TextRange(text.selection.start + offset, text.selection.end + offset)
            )
            return
        }

        text = text.copy(
            "$contentBeforeCurrentLine- [ ] $currentLine$contentAfterCurrentLine",
            TextRange(text.selection.start + "- [ ] ".length, text.selection.end + "- [ ] ".length)
        )
    }

    fun handleEnter(): Boolean {
        val contentBefore = text.text.substring(0, text.selection.min)
        val lastLineBreak = contentBefore.indexOfLast { it == '\n' }
        val nextLineBreak = text.text.indexOf('\n', lastLineBreak + 1)
        val currentLine = text.text.substring(
            lastLineBreak + 1,
            if (nextLineBreak == -1) text.text.length else nextLineBreak
        )

        for (prefix in LIST_ITEM_SYMBOL_LIST) {
            if (!currentLine.startsWith(prefix)) {
                continue
            }

            if (currentLine.length <= prefix.length || text.selection.min - lastLineBreak <= prefix.length) {
                break
            }

            text = text.copy(
                text.text.substring(0, text.selection.min) + "\n" + prefix + text.text.substring(text.selection.max),
                TextRange(text.selection.min + 1 + prefix.length, text.selection.min + 1 + prefix.length)
            )
            return true
        }

        return false
    }

    val pickImage = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        uri?.let { uploadImage(it) }
    }

    val takePhoto = rememberLauncherForActivityResult(TakePicture()) { success ->
        if (success) {
            photoImageUri?.let { uploadImage(it) }
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
                title = {
                    if (memo == null) {
                        Text(R.string.compose.string)
                    } else {
                        Text(R.string.edit.string)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(Icons.Filled.Close, contentDescription = R.string.close.string)
                    }
                },
            )
        },
        bottomBar = {
            BottomAppBar {
                if (memosViewModel.tags.toList().isEmpty()) {
                    IconButton(onClick = {
                        text = text.copy(
                            text.text.replaceRange(text.selection.min, text.selection.max, "#"),
                            TextRange(text.selection.min + 1)
                        )
                    }) {
                        Icon(Icons.Outlined.Tag, contentDescription = R.string.tag.string)
                    }
                } else {
                    Box {
                        DropdownMenu(
                            expanded = tagMenuExpanded,
                            onDismissRequest = { tagMenuExpanded = false },
                            properties = PopupProperties(focusable = false)
                        ) {
                            memosViewModel.tags.toList().forEach { tag ->
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
                            Icon(Icons.Outlined.Tag, contentDescription = R.string.tag.string)
                        }
                    }
                }

                IconButton(onClick = {
                    toggleTodoItem()
                }) {
                    Icon(Icons.Outlined.CheckBox, contentDescription = R.string.add_task.string)
                }

                IconButton(onClick = {
                    pickImage.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                }) {
                    Icon(Icons.Outlined.Image, contentDescription = R.string.add_image.string)
                }

                IconButton(onClick = {
                    try {
                        val uri = MoeMemosFileProvider.getImageUri(context)
                        photoImageUri = uri
                        takePhoto.launch(uri)
                    } catch (e: ActivityNotFoundException) {
                        coroutineScope.launch {
                            snackbarState.showSnackbar(e.localizedMessage ?: "Unable to take picture.")
                        }
                    }
                }) {
                    Icon(
                        Icons.Outlined.PhotoCamera,
                        contentDescription = R.string.take_photo.string
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    enabled = text.text.isNotEmpty(),
                    onClick = { submit() }
                ) {
                    Icon(Icons.Filled.Send, contentDescription = R.string.post.string)
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
//                    .onKeyEvent { event ->
//                        if (event.key == Key.Enter) {
//                            return@onKeyEvent handleEnter()
//                        }
//                        false
//                    },
                value = text,
                label = { Text(R.string.any_thoughts.string) },
                onValueChange = {
                    // an ugly hack to handle enter event, as `onKeyEvent` modifier only handles hardware keyboard,
                    // please submit a pull request if there's a native way to handle software key event
                    if (text.text != it.text && it.selection.start == it.selection.end && it.text.length == text.text.length + 1
                        && it.selection.start > 0 && it.text[it.selection.start - 1] == '\n'
                    ) {
                        if (handleEnter()) {
                            return@OutlinedTextField
                        }
                    }
                    text = it
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            if (viewModel.uploadResources.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .height(80.dp)
                        .padding(start = 15.dp, end = 15.dp, bottom = 15.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(viewModel.uploadResources.toList(), { it.id }) { resource ->
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
        when {
            memo != null -> {
                memo.resourceList?.let { resourceList -> viewModel.uploadResources.addAll(resourceList) }
            }

            shareContent != null -> {
                text = TextFieldValue(shareContent.text, TextRange(shareContent.text.length))
                for (item in shareContent.images) {
                    uploadImage(item)
                }
            }

            else -> {
                viewModel.draft.first()?.let {
                    text = TextFieldValue(it, TextRange(it.length))
                }
            }
        }
        delay(300)
        focusRequester.requestFocus()
    }

    DisposableEffect(Unit) {
        onDispose {
            if (memo == null && shareContent == null) {
                viewModel.updateDraft(text.text)
            }
        }
    }
}