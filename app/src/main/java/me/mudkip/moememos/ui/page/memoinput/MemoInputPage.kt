package me.mudkip.moememos.ui.page.memoinput

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
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
import me.mudkip.moememos.data.model.MemoVisibility
import me.mudkip.moememos.data.model.ShareContent
import me.mudkip.moememos.ext.icon
import me.mudkip.moememos.ext.popBackStackIfLifecycleIsResumed
import me.mudkip.moememos.ext.string
import me.mudkip.moememos.ext.suspendOnErrorMessage
import me.mudkip.moememos.ext.titleResource
import me.mudkip.moememos.ui.component.Attachment
import me.mudkip.moememos.ui.component.InputImage
import me.mudkip.moememos.ui.page.common.LocalRootNavController
import me.mudkip.moememos.util.extractCustomTags
import me.mudkip.moememos.viewmodel.LocalMemos
import me.mudkip.moememos.viewmodel.LocalUserState
import me.mudkip.moememos.viewmodel.MemoInputViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MemoInputPage(
    viewModel: MemoInputViewModel = hiltViewModel(),
    memoIdentifier: String? = null,
    shareContent: ShareContent? = null
) {
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val snackbarState = remember { SnackbarHostState() }
    val navController = LocalRootNavController.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val memosViewModel = LocalMemos.current
    val memo = remember { memosViewModel.memos.toList().find { it.identifier == memoIdentifier } }
    var initialContent by remember { mutableStateOf(memo?.content ?: "") }
    var text by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(memo?.content ?: "", TextRange(memo?.content?.length ?: 0)))
    }
    var visibilityMenuExpanded by remember { mutableStateOf(false) }
    var tagMenuExpanded by remember { mutableStateOf(false) }
    var photoImageUri by remember { mutableStateOf<Uri?>(null) }

    val context = LocalContext.current
    val defaultVisibility = LocalUserState.current.currentUser?.defaultVisibility ?: MemoVisibility.PRIVATE
    var currentVisibility by remember { mutableStateOf(memo?.visibility ?: defaultVisibility) }

    val validMimeTypePrefixes = remember {
        setOf("text/")
    }

    var showExitConfirmation by remember { mutableStateOf(false) }

    fun submit() = coroutineScope.launch {
        val tags = extractCustomTags(text.text)

        memo?.let {
            viewModel.editMemo(memo.identifier, text.text, currentVisibility, tags.toList()).suspendOnSuccess {
                navController.popBackStack()
            }.suspendOnErrorMessage { message ->
                snackbarState.showSnackbar(message)
            }
            return@launch
        }

        viewModel.createMemo(text.text, currentVisibility, tags.toList()).suspendOnSuccess {
            text = TextFieldValue("")
            viewModel.updateDraft("")
            navController.popBackStack()
        }.suspendOnErrorMessage { message ->
            snackbarState.showSnackbar(message)
        }
    }

    fun handleExit() {
        if (text.text.isNotEmpty() && text.text != initialContent) {
            showExitConfirmation = true
        } else {
            navController.popBackStackIfLifecycleIsResumed(lifecycleOwner)
        }
    }

    fun confirmExit() {
        showExitConfirmation = false
        submit()
    }

    fun discardExit() {
        showExitConfirmation = false
        text = TextFieldValue("")
        navController.popBackStackIfLifecycleIsResumed(lifecycleOwner)
    }

    BackHandler {
        handleExit()
    }

    fun uploadImage(uri: Uri) = coroutineScope.launch {
        try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
            viewModel.upload(bitmap, memo?.identifier).suspendOnSuccess {
                delay(300)
                focusRequester.requestFocus()
            }.suspendOnErrorMessage { message ->
                snackbarState.showSnackbar(message)
            }
        } catch (e: Exception) {
            snackbarState.showSnackbar(e.localizedMessage ?: e.toString())
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

    fun ClipData.textList(): List<CharSequence> {
        return (0 until itemCount)
            .mapNotNull(::getItemAt)
            .mapNotNull { item ->
                item.text
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
                        handleExit()
                    }) {
                        Icon(Icons.Filled.Close, contentDescription = R.string.close.string)
                    }
                },
            )
        },
        bottomBar = {
            BottomAppBar {
                Box {
                    DropdownMenu(
                        expanded = visibilityMenuExpanded,
                        onDismissRequest = { visibilityMenuExpanded = false },
                        properties = PopupProperties(focusable = false)
                    ) {
                        enumValues<MemoVisibility>().forEach { visibility ->
                            DropdownMenuItem(
                                text = { Text(stringResource(visibility.titleResource)) },
                                onClick = {
                                    currentVisibility = visibility
                                    visibilityMenuExpanded = false
                                },
                                leadingIcon = {
                                    Icon(visibility.icon, contentDescription = stringResource(visibility.titleResource))
                                },
                                trailingIcon = {
                                    if (currentVisibility == visibility) {
                                        Icon(Icons.Outlined.Check, contentDescription = null)
                                    }
                                }
                            )
                        }
                    }
                    IconButton(onClick = { visibilityMenuExpanded = !visibilityMenuExpanded }) {
                        Icon(
                            currentVisibility.icon,
                            contentDescription = stringResource(currentVisibility.titleResource)
                        )
                    }
                }

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
                        IconButton(onClick = { tagMenuExpanded = !tagMenuExpanded }) {
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
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = R.string.post.string)
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
                .fillMaxHeight()
                .dragAndDropTarget(
                    shouldStartDragAndDrop = accept@{ startEvent ->
                        val hasValidMimeType = startEvent
                            .mimeTypes()
                            .any { eventMimeType ->
                                validMimeTypePrefixes.any(eventMimeType::startsWith)
                            }
                        hasValidMimeType
                    },
                    target = object : DragAndDropTarget {
                        override fun onDrop(event: DragAndDropEvent): Boolean {
                            val androidDragEvent = event.toAndroidDragEvent()
                            val concatText = androidDragEvent.clipData
                                .textList()
                                .fold("") { acc, text ->
                                    if (acc.isNotBlank()) {
                                        acc.trimEnd { it == '\n' } + '\n' + '\n' + text.trimStart { it == '\n' }
                                    } else {
                                        text.toString()
                                    }
                                }
                            text = text.copy(text = text.text + concatText)
                            return true
                        }

                    }
                )
        ) {
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
                    items(viewModel.uploadResources.toList(), { it.identifier }) { resource ->
                        if (resource.mimeType?.type == "image") {
                            InputImage(resource = resource, inputViewModel = viewModel)
                        } else {
                            Attachment(resource = resource)
                        }
                    }
                }
            }
        }
    }

    if (showExitConfirmation) {
        AlertDialog(
            onDismissRequest = { showExitConfirmation = false },
            title = { Text("Save Changes?") },
            text = { Text("Do you want to save changes before exiting?") },
            confirmButton = {
                Button(onClick = {
                    confirmExit()
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                Button(onClick = {
                    discardExit()
                }) {
                    Text("Discard")
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        when {
            memo != null -> {
                viewModel.uploadResources.addAll(memo.resources)
                initialContent = memo.content
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