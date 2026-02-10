package me.mudkip.moememos.ui.page.memoinput

import android.content.ActivityNotFoundException
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.skydoves.sandwich.suspendOnSuccess
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.mudkip.moememos.MoeMemosFileProvider
import me.mudkip.moememos.data.model.MemoVisibility
import me.mudkip.moememos.data.model.ShareContent
import me.mudkip.moememos.ext.popBackStackIfLifecycleIsResumed
import me.mudkip.moememos.ext.suspendOnErrorMessage
import me.mudkip.moememos.ui.page.common.LocalRootNavController
import me.mudkip.moememos.util.extractCustomTags
import me.mudkip.moememos.viewmodel.LocalMemos
import me.mudkip.moememos.viewmodel.LocalUserState
import me.mudkip.moememos.viewmodel.MemoInputViewModel

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
    val userStateViewModel = LocalUserState.current
    val currentAccount by userStateViewModel.currentAccount.collectAsState()
    val memo = remember { memosViewModel.memos.toList().find { it.identifier == memoIdentifier } }
    var initialContent by remember { mutableStateOf(memo?.content ?: "") }
    var text by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(memo?.content ?: "", TextRange(memo?.content?.length ?: 0)))
    }
    var visibilityMenuExpanded by remember { mutableStateOf(false) }
    var tagMenuExpanded by remember { mutableStateOf(false) }
    var photoImageUri by remember { mutableStateOf<Uri?>(null) }
    var showExitConfirmation by remember { mutableStateOf(false) }

    val defaultVisibility = userStateViewModel.currentUser?.defaultVisibility ?: MemoVisibility.PRIVATE
    var currentVisibility by remember { mutableStateOf(memo?.visibility ?: defaultVisibility) }

    val validMimeTypePrefixes = remember {
        setOf("text/")
    }

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
        if (text.text != initialContent || viewModel.uploadResources.size != (memo?.resources?.size ?: 0)) {
            showExitConfirmation = true
        } else {
            navController.popBackStackIfLifecycleIsResumed(lifecycleOwner)
        }
    }

    fun uploadImage(uri: Uri) = coroutineScope.launch {
        try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(navController.context.contentResolver, uri))
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(navController.context.contentResolver, uri)
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

    val pickImage = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        uri?.let { uploadImage(it) }
    }

    val takePhoto = rememberLauncherForActivityResult(TakePicture()) { success ->
        if (success) {
            photoImageUri?.let { uploadImage(it) }
        }
    }

    BackHandler {
        handleExit()
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            MemoInputTopBar(
                isEditMode = memo != null,
                canSubmit = text.text.isNotEmpty() || viewModel.uploadResources.isNotEmpty(),
                onClose = { handleExit() },
                onSubmit = { submit() }
            )
        },
        bottomBar = {
            MemoInputBottomBar(
                currentAccount = currentAccount,
                currentVisibility = currentVisibility,
                visibilityMenuExpanded = visibilityMenuExpanded,
                onVisibilityExpandedChange = { visibilityMenuExpanded = it },
                onVisibilitySelected = { currentVisibility = it },
                tags = memosViewModel.tags.toList(),
                tagMenuExpanded = tagMenuExpanded,
                onTagExpandedChange = { tagMenuExpanded = it },
                onHashTagClick = {
                    text = replaceSelection(text, "#")
                },
                onTagSelected = { tag ->
                    text = replaceSelection(text, "#$tag ")
                },
                onToggleTodoItem = {
                    text = toggleTodoItemInText(text)
                },
                onPickImage = {
                    pickImage.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                },
                onTakePhoto = {
                    try {
                        val uri = MoeMemosFileProvider.getImageUri(navController.context)
                        photoImageUri = uri
                        takePhoto.launch(uri)
                    } catch (e: ActivityNotFoundException) {
                        coroutineScope.launch {
                            snackbarState.showSnackbar(e.localizedMessage ?: "Unable to take picture.")
                        }
                    }
                },
                onFormat = { format ->
                    text = applyMarkdownFormatToText(text, format)
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarState)
        }
    ) { innerPadding ->
        MemoInputEditor(
            modifier = Modifier.padding(innerPadding),
            text = text,
            onTextChange = { updated ->
                if (
                    text.text != updated.text &&
                    updated.selection.start == updated.selection.end &&
                    updated.text.length == text.text.length + 1 &&
                    updated.selection.start > 0 &&
                    updated.text[updated.selection.start - 1] == '\n'
                ) {
                    val handled = handleEnterInText(text)
                    if (handled != null) {
                        text = handled
                        return@MemoInputEditor
                    }
                }
                text = updated
            },
            focusRequester = focusRequester,
            validMimeTypePrefixes = validMimeTypePrefixes,
            onDroppedText = { droppedText ->
                text = text.copy(text = text.text + droppedText)
            },
            uploadResources = viewModel.uploadResources.toList(),
            inputViewModel = viewModel
        )
    }

    if (showExitConfirmation) {
        SaveChangesDialog(
            onSave = {
                showExitConfirmation = false
                submit()
            },
            onDiscard = {
                showExitConfirmation = false
                text = TextFieldValue("")
                navController.popBackStackIfLifecycleIsResumed(lifecycleOwner)
            },
            onDismiss = {
                showExitConfirmation = false
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
