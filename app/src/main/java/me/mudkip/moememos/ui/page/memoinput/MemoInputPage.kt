package me.mudkip.moememos.ui.page.memoinput

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import me.mudkip.moememos.util.extractCustomTags
import me.mudkip.moememos.viewmodel.LocalMemos
import me.mudkip.moememos.viewmodel.LocalUserState
import me.mudkip.moememos.viewmodel.MemoInputViewModel

private const val MaxSelectableImages = 100

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
    val currentAccount by userStateViewModel.currentAccount.collectAsStateWithLifecycle()
    val memo = remember { memosViewModel.memos.toList().find { it.identifier == memoIdentifier } }
    val autosaveEnabled by viewModel.autosaveEnabled.collectAsStateWithLifecycle(initialValue = false)
    var autosaveIdentifier by rememberSaveable { mutableStateOf(memo?.identifier) }
    var autosaveDirty by remember { mutableStateOf(false) }
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

        if (autosaveEnabled) {
            viewModel.flushAutosave(text.text, currentVisibility, tags.toList(), clearDraftOnCreate = shareContent == null).suspendOnSuccess {
                memosViewModel.refreshLocalSnapshot()
                navController.popBackStack()
            }.suspendOnErrorMessage { message ->
                snackbarState.showSnackbar(message)
            }
            return@launch
        }

        memo?.let {
            viewModel.editMemo(memo.identifier, text.text, currentVisibility, tags.toList()).suspendOnSuccess {
                memosViewModel.refreshLocalSnapshot()
                navController.popBackStack()
            }.suspendOnErrorMessage { message ->
                snackbarState.showSnackbar(message)
            }
            return@launch
        }

        viewModel.createMemo(text.text, currentVisibility, tags.toList()).suspendOnSuccess {
            text = TextFieldValue("")
            viewModel.updateDraft("")
            memosViewModel.refreshLocalSnapshot()
            navController.popBackStack()
        }.suspendOnErrorMessage { message ->
            snackbarState.showSnackbar(message)
        }
    }

    fun handleExit() {
        if (autosaveEnabled) {
            coroutineScope.launch {
                if (memo == null && text.text.isEmpty() && viewModel.uploadResources.isEmpty()) {
                    viewModel.discardEmptyAutosave()
                } else {
                    viewModel.flushAutosave(
                        text.text,
                        currentVisibility,
                        extractCustomTags(text.text).toList(),
                        clearDraftOnCreate = shareContent == null
                    )
                }
                memosViewModel.refreshLocalSnapshot()
                navController.popBackStackIfLifecycleIsResumed(lifecycleOwner)
            }
            return
        }
        if (text.text != initialContent || viewModel.uploadResources.size != (memo?.resources?.size ?: 0)) {
            showExitConfirmation = true
        } else {
            navController.popBackStackIfLifecycleIsResumed(lifecycleOwner)
        }
    }

    fun uploadImages(uris: List<Uri>) = coroutineScope.launch {
        uris.take(MaxSelectableImages).forEach { uri ->
            viewModel.upload(uri, autosaveIdentifier ?: memo?.identifier).suspendOnErrorMessage { message ->
                snackbarState.showSnackbar(message)
            }
        }
        delay(300)
        focusRequester.requestFocus()
    }

    fun uploadImage(uri: Uri) {
        uploadImages(listOf(uri))
    }

    val pickImages = rememberLauncherForActivityResult(
        PickMultipleVisualMedia(MaxSelectableImages)
    ) { uris ->
        if (uris.isNotEmpty()) {
            uploadImages(uris)
        }
    }

    val takePhoto = rememberLauncherForActivityResult(TakePicture()) { success ->
        if (success) {
            photoImageUri?.let { uploadImage(it) }
        }
    }

    val pickAttachment = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        uri?.let {
            coroutineScope.launch {
                viewModel.upload(it, autosaveIdentifier ?: memo?.identifier).suspendOnErrorMessage { message ->
                    snackbarState.showSnackbar(message)
                }
            }
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
                showSpaceVisibility = memo?.visibility == MemoVisibility.SPACE,
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
                    pickImages.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                },
                onPickAttachment = {
                    pickAttachment.launch(arrayOf("*/*"))
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
        viewModel.autosaveIdentifier = autosaveIdentifier
        viewModel.uploadResources.clear()
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
                // After process death with an autosaved row, the restored text is newer than the draft
                if (autosaveIdentifier == null) {
                    viewModel.draft.first()?.let {
                        text = TextFieldValue(it, TextRange(it.length))
                    }
                }
            }
        }
        delay(300)
        focusRequester.requestFocus()
    }

    // Restarts per change; a restart cancels a previous run still waiting for the autosave mutex,
    // so at most one write runs and one (the newest) waits.
    LaunchedEffect(autosaveEnabled, text.text, currentVisibility, viewModel.uploadResources.size) {
        if (!autosaveEnabled) {
            return@LaunchedEffect
        }
        autosaveIdentifier = autosaveIdentifier ?: viewModel.autosaveIdentifier
        if (autosaveIdentifier == null && text.text.isEmpty() && viewModel.uploadResources.isEmpty()) {
            return@LaunchedEffect
        }
        if (!autosaveDirty && memo != null &&
            text.text == memo.content &&
            currentVisibility == memo.visibility &&
            viewModel.uploadResources.size == memo.resources.size
        ) {
            return@LaunchedEffect
        }
        autosaveDirty = true
        viewModel.autosave(
            text.text,
            currentVisibility,
            extractCustomTags(text.text).toList(),
            clearDraftOnCreate = shareContent == null
        ).suspendOnSuccess {
            autosaveIdentifier = data.identifier
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (autosaveEnabled) {
                // Already flushed by submit/handleExit; a system-initiated dispose leaves needsSync
                // set, so the deferred push or the next sync uploads the row.
                return@onDispose
            }
            if (memo == null && shareContent == null) {
                viewModel.updateDraft(text.text)
            }
        }
    }
}
