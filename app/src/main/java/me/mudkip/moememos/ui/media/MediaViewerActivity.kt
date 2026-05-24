package me.mudkip.moememos.ui.media

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.VideoView
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImage
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.mudkip.moememos.R
import me.mudkip.moememos.ext.string
import me.mudkip.moememos.ui.security.AppLockGate
import me.mudkip.moememos.ui.theme.MoeMemosTheme
import me.mudkip.moememos.viewmodel.UserStateViewModel
import okhttp3.OkHttpClient
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import kotlin.math.abs
import kotlin.math.min

@AndroidEntryPoint
class MediaViewerActivity : FragmentActivity() {

    private val userStateViewModel: UserStateViewModel by viewModels()

    companion object {
        const val EXTRA_IMAGE_URLS = "image_urls"
        const val EXTRA_INITIAL_INDEX = "initial_index"
        const val EXTRA_CAPTION = "caption"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        overridePendingTransitionCompat(0, 0)
        super.onCreate(savedInstanceState)
        hideStatusBar()

        val imageUrls = intent.getStringArrayExtra(EXTRA_IMAGE_URLS)?.toList().orEmpty()
        if (imageUrls.isEmpty()) {
            finishWithoutAnimation()
            return
        }
        val initialIndex = intent.getIntExtra(EXTRA_INITIAL_INDEX, 0).coerceIn(imageUrls.indices)
        val caption = intent.getStringExtra(EXTRA_CAPTION).orEmpty()

        setContent {
            MoeMemosTheme(darkTheme = true, dynamicColor = false) {
                AppLockGate {
                    MediaViewerScreen(
                        imageUrls = imageUrls,
                        initialIndex = initialIndex,
                        caption = caption,
                        okHttpClient = userStateViewModel.okHttpClient,
                        onClose = { finishWithoutAnimation() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideStatusBar()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideStatusBar()
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransitionCompat(0, 0)
    }

    private fun hideStatusBar() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun finishWithoutAnimation() {
        finish()
    }

    @Suppress("DEPRECATION")
    private fun overridePendingTransitionCompat(enterAnim: Int, exitAnim: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, enterAnim, exitAnim)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, enterAnim, exitAnim)
        } else {
            overridePendingTransition(enterAnim, exitAnim)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaViewerScreen(
    imageUrls: List<String>,
    initialIndex: Int,
    caption: String,
    okHttpClient: OkHttpClient,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val imageLoader = remember(context, okHttpClient) {
        ImageLoader.Builder(context)
            .components {
                add(
                    OkHttpNetworkFetcherFactory(
                        callFactory = { okHttpClient }
                    )
                )
            }
            .build()
    }
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { imageUrls.size }
    )
    var pagerScrollEnabled by remember { mutableStateOf(true) }
    var backgroundAlpha by remember { mutableFloatStateOf(1f) }
    var closing by remember { mutableStateOf(false) }
    val entryProgress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    fun closeWithAnimation() {
        if (closing) {
            return
        }
        closing = true
        scope.launch {
            entryProgress.animateTo(0f, animationSpec = tween(140))
            onClose()
        }
    }

    LaunchedEffect(Unit) {
        entryProgress.animateTo(1f, animationSpec = tween(220))
    }

    LaunchedEffect(pagerState.currentPage) {
        pagerScrollEnabled = true
        backgroundAlpha = 1f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = backgroundAlpha * entryProgress.value))
    ) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = pagerScrollEnabled,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = entryProgress.value
                    val entryScale = 0.985f + 0.015f * entryProgress.value
                    scaleX = entryScale
                    scaleY = entryScale
                }
        ) { page ->
            MediaViewerPage(
                imageUrl = imageUrls[page],
                imageLoader = imageLoader,
                isCurrentPage = page == pagerState.currentPage,
                onClose = ::closeWithAnimation,
                onBackgroundAlphaChange = { alpha ->
                    if (page == pagerState.currentPage) {
                        backgroundAlpha = alpha
                    }
                },
                onPagerScrollEnabledChange = { enabled ->
                    if (page == pagerState.currentPage) {
                        pagerScrollEnabled = enabled
                    }
                }
            )
        }

        if (caption.isNotBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.74f),
                                Color.Black.copy(alpha = 0.92f),
                            )
                        )
                    )
                    .navigationBarsPadding()
                    .graphicsLayer { alpha = entryProgress.value }
                    .padding(start = 20.dp, top = 48.dp, end = 20.dp, bottom = 20.dp)
            ) {
                Text(
                    text = caption,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp)
                        .verticalScroll(rememberScrollState())
                )
            }
        }

        IconButton(
            onClick = ::closeWithAnimation,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .graphicsLayer { alpha = entryProgress.value }
                .padding(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = R.string.close.string,
                tint = Color.White
            )
        }
    }
}

@OptIn(ExperimentalCoilApi::class)
@Composable
private fun MediaViewerPage(
    imageUrl: String,
    imageLoader: ImageLoader,
    isCurrentPage: Boolean,
    onClose: () -> Unit,
    onBackgroundAlphaChange: (Float) -> Unit,
    onPagerScrollEnabledChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gestureState = remember(imageUrl) { MediaGestureState() }
    val modelFile = remember(imageUrl) {
        Uri.parse(imageUrl).takeIf { it.scheme == "file" }?.path?.let(::File)
    }
    var loading by remember(imageUrl) { mutableStateOf(true) }
    var failed by remember(imageUrl) { mutableStateOf(false) }
    var sourceFile by remember(imageUrl) { mutableStateOf<File?>(null) }
    var motionVideoFile by remember(imageUrl) { mutableStateOf<File?>(null) }
    var motionVideoPlaying by remember(imageUrl) { mutableStateOf(false) }
    var autoPlayed by remember(imageUrl) { mutableStateOf(false) }
    var interactionVersion by remember(imageUrl) { mutableIntStateOf(0) }

    fun markInteraction() {
        interactionVersion++
    }

    fun playMotionVideo() {
        val videoFile = motionVideoFile ?: return
        markInteraction()
        autoPlayed = true
        scope.launch {
            gestureState.animateTo(1f, Offset.Zero)
            if (videoFile.exists()) {
                motionVideoPlaying = true
            }
        }
    }

    LaunchedEffect(sourceFile) {
        motionVideoFile = null
        motionVideoPlaying = false
        autoPlayed = false
        val file = sourceFile ?: return@LaunchedEffect
        motionVideoFile = extractMotionVideoFile(context.cacheDir, file)
    }

    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) {
            motionVideoPlaying = false
            gestureState.resetImmediately()
        }
    }

    LaunchedEffect(isCurrentPage, gestureState.scale, motionVideoPlaying, gestureState.dismissOffsetY) {
        if (isCurrentPage) {
            onPagerScrollEnabledChange(
                gestureState.scale <= 1.01f && !motionVideoPlaying && gestureState.dismissOffsetY == 0f
            )
        }
    }

    LaunchedEffect(isCurrentPage, gestureState.dismissOffsetY, gestureState.containerSize) {
        if (isCurrentPage) {
            onBackgroundAlphaChange(gestureState.backgroundAlpha())
        }
    }

    LaunchedEffect(
        isCurrentPage,
        loading,
        motionVideoFile,
        motionVideoPlaying,
        autoPlayed,
        interactionVersion,
        gestureState.scale,
        gestureState.dismissOffsetY,
    ) {
        if (
            isCurrentPage &&
            !loading &&
            motionVideoFile != null &&
            !motionVideoPlaying &&
            !autoPlayed &&
            gestureState.scale <= 1.01f &&
            gestureState.dismissOffsetY == 0f
        ) {
            delay(600)
            if (
                isCurrentPage &&
                !motionVideoPlaying &&
                !autoPlayed &&
                gestureState.scale <= 1.01f &&
                gestureState.dismissOffsetY == 0f
            ) {
                autoPlayed = true
                motionVideoPlaying = true
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { gestureState.containerSize = it },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = imageUrl,
            imageLoader = imageLoader,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = gestureState.scale
                    scaleY = gestureState.scale
                    translationX = gestureState.offset.x
                    translationY = gestureState.offset.y + gestureState.dismissOffsetY
                }
                .pointerInput(imageUrl, motionVideoPlaying) {
                    if (!motionVideoPlaying) {
                        detectTapGestures(
                            onDoubleTap = { tapPosition ->
                                markInteraction()
                                scope.launch {
                                    gestureState.animateDoubleTap(tapPosition)
                                }
                            }
                        )
                    }
                }
                .mediaGestureInput(
                    state = gestureState,
                    enabled = !motionVideoPlaying,
                    animationScope = scope,
                    onInteraction = ::markInteraction,
                    onTransformStart = {
                        onPagerScrollEnabledChange(false)
                    },
                    onTransformEnd = {
                        onPagerScrollEnabledChange(gestureState.scale <= 1.01f)
                    },
                    onDismiss = onClose,
                ),
            onLoading = {
                loading = true
                failed = false
                sourceFile = null
                motionVideoFile = null
                motionVideoPlaying = false
            },
            onSuccess = { state ->
                loading = false
                failed = false
                val diskCache = imageLoader.diskCache
                val diskCacheKey = state.result.diskCacheKey
                val cachedFile = if (diskCache != null && diskCacheKey != null) {
                    diskCache.openSnapshot(diskCacheKey)?.data?.toFile()
                } else {
                    null
                }
                sourceFile = cachedFile ?: modelFile
            },
            onError = {
                loading = false
                failed = true
                sourceFile = null
                motionVideoFile = null
                motionVideoPlaying = false
            }
        )

        if (motionVideoPlaying) {
            motionVideoFile?.let { videoFile ->
                MotionVideoPlayer(
                    videoFile = videoFile,
                    modifier = Modifier.fillMaxSize(),
                    onCompleted = { motionVideoPlaying = false }
                )
            }
        }

        if (loading) {
            CircularProgressIndicator(color = Color.White)
        }

        if (failed) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.BrokenImage,
                    contentDescription = null,
                    tint = Color.White
                )
                Text(
                    text = R.string.failed_to_load_image.string,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (motionVideoFile != null && !motionVideoPlaying) {
            LivePhotoButton(
                onClick = ::playMotionVideo,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun LivePhotoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.46f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = Color.White
        )
        Text(
            text = "LIVE",
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MotionVideoPlayer(
    videoFile: File,
    modifier: Modifier = Modifier,
    onCompleted: () -> Unit,
) {
    var videoView by remember(videoFile) { mutableStateOf<VideoView?>(null) }

    AndroidView(
        modifier = modifier.background(Color.Black),
        factory = { context ->
            VideoView(context).apply {
                tag = videoFile.absolutePath
                setVideoURI(Uri.fromFile(videoFile))
                setOnPreparedListener { mediaPlayer ->
                    mediaPlayer.isLooping = false
                    mediaPlayer.setVolume(0f, 0f)
                    start()
                }
                setOnCompletionListener { onCompleted() }
                setOnErrorListener { _, _, _ ->
                    onCompleted()
                    true
                }
                videoView = this
            }
        },
        update = { view ->
            if (view.tag != videoFile.absolutePath) {
                view.tag = videoFile.absolutePath
                view.setVideoURI(Uri.fromFile(videoFile))
            }
            if (!view.isPlaying) {
                view.start()
            }
        }
    )

    DisposableEffect(videoFile) {
        onDispose {
            videoView?.stopPlayback()
            videoView = null
        }
    }
}

private fun Modifier.mediaGestureInput(
    state: MediaGestureState,
    enabled: Boolean,
    animationScope: CoroutineScope,
    onInteraction: () -> Unit,
    onTransformStart: () -> Unit,
    onTransformEnd: () -> Unit,
    onDismiss: () -> Unit,
): Modifier = pointerInput(state, enabled) {
    if (!enabled) {
        return@pointerInput
    }
    val touchSlop = viewConfiguration.touchSlop
    val singlePointerGestureSlop = touchSlop * 1.35f
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var previousCentroid = down.position
        var previousSpan = 0f
        var totalSinglePointerDrag = Offset.Zero
        var mode = GestureMode.Undecided

        while (true) {
            val event = awaitPointerEvent()
            val pressedChanges = event.changes.filter { it.pressed }
            if (pressedChanges.isEmpty()) {
                break
            }

            if (pressedChanges.size >= 2) {
                val centroid = pressedChanges.calculateCentroid()
                val span = pressedChanges.calculateAverageDistance(centroid)
                if (mode != GestureMode.Transform) {
                    mode = GestureMode.Transform
                    totalSinglePointerDrag = Offset.Zero
                    onInteraction()
                    onTransformStart()
                }
                if (previousSpan == 0f) {
                    previousSpan = span
                    previousCentroid = centroid
                    event.changes.forEach { it.consume() }
                    continue
                }

                val zoom = if (previousSpan > 0f) span / previousSpan else 1f
                val pan = centroid - previousCentroid

                state.applyTransform(centroid = centroid, pan = pan, zoom = zoom)
                event.changes.forEach { it.consume() }

                previousSpan = span
                previousCentroid = centroid
                continue
            }

            previousSpan = 0f
            val change = pressedChanges.first()
            val pan = change.positionChange()
            totalSinglePointerDrag += pan

            if (state.scale > 1.01f) {
                if (mode == GestureMode.Undecided && totalSinglePointerDrag.getDistance() > touchSlop) {
                    mode = GestureMode.ZoomedPan
                    onInteraction()
                }
                if (mode == GestureMode.ZoomedPan) {
                    state.applyZoomedPan(pan)
                    change.consume()
                }
                continue
            }

            if (mode == GestureMode.Undecided) {
                val absX = abs(totalSinglePointerDrag.x)
                val absY = abs(totalSinglePointerDrag.y)
                if (absX > singlePointerGestureSlop && absX > absY * 1.1f) {
                    mode = GestureMode.Pager
                    break
                }
                if (absY > singlePointerGestureSlop && absY > absX * 1.15f) {
                    mode = GestureMode.Dismiss
                    onInteraction()
                }
            }

            if (mode == GestureMode.Dismiss) {
                state.applyDismissPan(pan.y)
                change.consume()
            }
        }

        when (mode) {
            GestureMode.Transform,
            GestureMode.ZoomedPan -> animationScope.launch {
                state.settleScaleAndOffset()
                onTransformEnd()
            }
            GestureMode.Dismiss -> {
                if (state.shouldDismiss()) {
                    onDismiss()
                } else {
                    animationScope.launch {
                        state.animateDismissBack()
                    }
                }
            }
            GestureMode.Pager,
            GestureMode.Undecided -> Unit
        }
    }
}

private class MediaGestureState {
    var scale by mutableFloatStateOf(1f)
    var offset by mutableStateOf(Offset.Zero)
    var dismissOffsetY by mutableFloatStateOf(0f)
    var containerSize by mutableStateOf(IntSize.Zero)

    fun applyTransform(centroid: Offset, pan: Offset, zoom: Float) {
        val oldScale = scale
        val newScale = (oldScale * zoom).coerceIn(MinScale, MaxScale)
        val ratio = if (oldScale > 0f) newScale / oldScale else 1f
        val center = containerCenter()
        scale = newScale
        offset = if (newScale <= 1.01f) {
            Offset.Zero
        } else {
            clampOffset(offset * ratio + pan + (centroid - center) * (1f - ratio), newScale)
        }
        dismissOffsetY = 0f
    }

    fun applyZoomedPan(pan: Offset) {
        if (scale <= 1.01f) {
            offset = Offset.Zero
            return
        }
        offset = clampOffset(offset + pan, scale)
    }

    fun applyDismissPan(deltaY: Float) {
        dismissOffsetY += deltaY
        offset = Offset.Zero
    }

    fun shouldDismiss(): Boolean {
        val height = containerSize.height.takeIf { it > 0 } ?: return abs(dismissOffsetY) > DefaultDismissThresholdPx
        return abs(dismissOffsetY) > min(height * DismissThresholdRatio, DefaultDismissThresholdPx)
    }

    fun backgroundAlpha(): Float {
        val height = containerSize.height.takeIf { it > 0 } ?: return 1f
        val progress = (abs(dismissOffsetY) / (height * 0.62f)).coerceIn(0f, 0.78f)
        return 1f - progress
    }

    suspend fun animateDoubleTap(tapPosition: Offset) {
        val targetScale = if (scale > 1.01f) MinScale else DoubleTapScale
        val targetOffset = if (targetScale == MinScale) {
            Offset.Zero
        } else {
            val center = containerCenter()
            clampOffset((center - tapPosition) * (targetScale - 1f), targetScale)
        }
        animateTo(targetScale, targetOffset)
    }

    suspend fun animateDismissBack() {
        val animatable = Animatable(dismissOffsetY)
        animatable.animateTo(0f, animationSpec = tween(180)) {
            dismissOffsetY = value
        }
    }

    suspend fun settleScaleAndOffset() {
        if (scale <= 1.02f) {
            animateTo(MinScale, Offset.Zero)
            return
        }
        val clampedOffset = clampOffset(offset, scale)
        if ((clampedOffset - offset).getDistance() > 0.5f) {
            animateOffsetTo(clampedOffset)
        }
    }

    suspend fun animateTo(targetScale: Float, targetOffset: Offset) {
        val scaleAnimatable = Animatable(scale)
        val offsetAnimatable = Animatable(offset, Offset.VectorConverter)
        coroutineScope {
            launch {
                scaleAnimatable.animateTo(targetScale, animationSpec = tween(220)) {
                    scale = value
                }
            }
            launch {
                offsetAnimatable.animateTo(targetOffset, animationSpec = tween(220)) {
                    offset = value
                }
            }
        }
        if (targetScale <= 1.01f) {
            scale = MinScale
            offset = Offset.Zero
        }
        dismissOffsetY = 0f
    }

    fun resetImmediately() {
        scale = MinScale
        offset = Offset.Zero
        dismissOffsetY = 0f
    }

    private suspend fun animateOffsetTo(targetOffset: Offset) {
        val offsetAnimatable = Animatable(offset, Offset.VectorConverter)
        offsetAnimatable.animateTo(targetOffset, animationSpec = tween(180)) {
            offset = value
        }
    }

    private fun clampOffset(candidate: Offset, targetScale: Float): Offset {
        val width = containerSize.width
        val height = containerSize.height
        if (width <= 0 || height <= 0 || targetScale <= 1f) {
            return Offset.Zero
        }
        val maxX = width * (targetScale - 1f) / 2f
        val maxY = height * (targetScale - 1f) / 2f
        return Offset(
            x = candidate.x.coerceIn(-maxX, maxX),
            y = candidate.y.coerceIn(-maxY, maxY)
        )
    }

    private fun containerCenter(): Offset {
        return Offset(containerSize.width / 2f, containerSize.height / 2f)
    }

    private companion object {
        const val MinScale = 1f
        const val DoubleTapScale = 2.5f
        const val MaxScale = 5f
        const val DismissThresholdRatio = 0.22f
        const val DefaultDismissThresholdPx = 180f
    }
}

private enum class GestureMode {
    Undecided,
    Transform,
    ZoomedPan,
    Dismiss,
    Pager,
}

private fun List<PointerInputChange>.calculateCentroid(): Offset {
    if (isEmpty()) {
        return Offset.Zero
    }
    var x = 0f
    var y = 0f
    forEach { change ->
        x += change.position.x
        y += change.position.y
    }
    return Offset(x / size, y / size)
}

private fun List<PointerInputChange>.calculateAverageDistance(centroid: Offset): Float {
    if (isEmpty()) {
        return 0f
    }
    var distance = 0f
    forEach { change ->
        distance += (change.position - centroid).getDistance()
    }
    return distance / size
}

private suspend fun extractMotionVideoFile(cacheDir: File, sourceFile: File): File? = withContext(Dispatchers.IO) {
    runCatching {
        if (!sourceFile.isFile || sourceFile.length() <= MinMotionPhotoLengthBytes) {
            return@runCatching null
        }

        val mp4Start = findEmbeddedMp4Start(sourceFile) ?: return@runCatching null
        val videoLength = sourceFile.length() - mp4Start
        if (videoLength <= MinMotionVideoLengthBytes) {
            return@runCatching null
        }

        val motionCacheDir = File(cacheDir, MotionVideoCacheDirectory).apply { mkdirs() }
        val targetFile = File(
            motionCacheDir,
            "${sourceFile.absolutePath.hashCode()}_${sourceFile.length()}_${sourceFile.lastModified()}.mp4"
        )
        if (targetFile.isFile && targetFile.length() == videoLength) {
            return@runCatching targetFile
        }

        RandomAccessFile(sourceFile, "r").use { input ->
            FileOutputStream(targetFile).use { output ->
                input.seek(mp4Start)
                val buffer = ByteArray(DefaultBufferSize)
                var remaining = videoLength
                while (remaining > 0) {
                    val read = input.read(buffer, 0, min(buffer.size.toLong(), remaining).toInt())
                    if (read <= 0) {
                        break
                    }
                    output.write(buffer, 0, read)
                    remaining -= read
                }
            }
        }

        if (targetFile.length() == videoLength) {
            targetFile
        } else {
            targetFile.delete()
            null
        }
    }.onFailure { throwable ->
        Timber.d(throwable, "Failed to extract motion photo video")
    }.getOrNull()
}

private fun findEmbeddedMp4Start(sourceFile: File): Long? {
    RandomAccessFile(sourceFile, "r").use { file ->
        val length = file.length()
        val buffer = ByteArray(DefaultBufferSize)
        val overlap = ByteArray(Mp4SearchOverlapBytes)
        var overlapSize = 0
        var position = 0L
        var lastCandidate: Long? = null

        while (position < length) {
            file.seek(position)
            val read = file.read(buffer)
            if (read <= 0) {
                break
            }

            val combined = ByteArray(overlapSize + read)
            System.arraycopy(overlap, 0, combined, 0, overlapSize)
            System.arraycopy(buffer, 0, combined, overlapSize, read)
            val combinedStart = position - overlapSize

            for (index in 4 until combined.size - 8) {
                if (combined[index] == 'f'.code.toByte() &&
                    combined[index + 1] == 't'.code.toByte() &&
                    combined[index + 2] == 'y'.code.toByte() &&
                    combined[index + 3] == 'p'.code.toByte()
                ) {
                    val boxStart = combinedStart + index - 4
                    if (boxStart > 0 && isPlausibleMp4FileTypeBox(combined, index - 4)) {
                        lastCandidate = boxStart
                    }
                }
            }

            overlapSize = min(Mp4SearchOverlapBytes, combined.size)
            System.arraycopy(combined, combined.size - overlapSize, overlap, 0, overlapSize)
            position += read
        }

        return lastCandidate
    }
}

private fun isPlausibleMp4FileTypeBox(data: ByteArray, boxStart: Int): Boolean {
    if (boxStart < 0 || boxStart + 16 > data.size) {
        return false
    }
    val boxSize = readBigEndianInt(data, boxStart)
    if (boxSize < 16 || boxSize > 4096) {
        return false
    }
    val majorBrandStart = boxStart + 8
    val majorBrand = String(data, majorBrandStart, 4)
    return majorBrand in Mp4MajorBrands
}

private fun readBigEndianInt(data: ByteArray, offset: Int): Int {
    return ((data[offset].toInt() and 0xff) shl 24) or
        ((data[offset + 1].toInt() and 0xff) shl 16) or
        ((data[offset + 2].toInt() and 0xff) shl 8) or
        (data[offset + 3].toInt() and 0xff)
}

private val Mp4MajorBrands = setOf(
    "isom",
    "iso2",
    "iso4",
    "iso5",
    "iso6",
    "mp41",
    "mp42",
    "mp4v",
    "M4V ",
    "qt  ",
)

private const val MotionVideoCacheDirectory = "motion_videos"
private const val DefaultBufferSize = 1024 * 1024
private const val Mp4SearchOverlapBytes = 32
private const val MinMotionPhotoLengthBytes = 64L * 1024L
private const val MinMotionVideoLengthBytes = 32L * 1024L
