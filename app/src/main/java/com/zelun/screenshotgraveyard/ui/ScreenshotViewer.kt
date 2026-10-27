package com.zelun.screenshotgraveyard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Scale
import com.zelun.screenshotgraveyard.domain.ImageViewport
import com.zelun.screenshotgraveyard.domain.ImageViewportBounds
import com.zelun.screenshotgraveyard.domain.ImageViewportMath
import java.io.File
import kotlin.math.roundToInt

private val ViewportSaver = listSaver<ImageViewport, Float>(
    save = { listOf(it.scale, it.x, it.y) },
    restore = { ImageViewport(it[0], it[1], it[2]) },
)

@Composable
fun ScreenshotViewer(file: File, title: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var transform by rememberSaveable(file.path, stateSaver = ViewportSaver) { mutableStateOf(ImageViewport()) }
    var viewport by remember(file.path) { mutableStateOf(IntSize.Zero) }
    var imageSize by remember(file.path) { mutableStateOf(IntSize.Zero) }
    var loaded by remember(file.path) { mutableStateOf(false) }
    var failed by remember(file.path) { mutableStateOf(false) }
    var retry by remember(file.path) { mutableIntStateOf(0) }
    val bounds = remember(viewport, imageSize) {
        if (viewport.width > 0 && viewport.height > 0 && imageSize.width > 0 && imageSize.height > 0)
            ImageViewportBounds.fit(viewport.width.toFloat(), viewport.height.toFloat(), imageSize.width.toFloat(), imageSize.height.toFloat())
        else null
    }
    val request = remember(file.path) {
        ImageRequest.Builder(context).data(file)
            // A separate key avoids reusing a tiny gallery thumbnail for zooming.
            .memoryCacheKey("${file.path}:viewer")
            .size(2048, 4096).scale(Scale.FIT).allowHardware(false).build()
    }
    LaunchedEffect(bounds) { bounds?.let { transform = ImageViewportMath.clamp(transform, it) } }
    val percent = (transform.scale * 100).roundToInt()

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(
        usePlatformDefaultWidth = false, decorFitsSystemWindows = false, dismissOnClickOutside = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Ink, contentColor = Cream) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Row(Modifier.fillMaxWidth().padding(start = 22.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, "Close full-screen viewer") }
            }
            Box(Modifier.weight(1f).fillMaxWidth().clipToBounds().background(Surface)
                .onSizeChanged { viewport = it }
                .testTag("image-viewport")
                .semantics { stateDescription = when {
                    failed -> "Image unavailable"
                    loaded -> "Zoom $percent percent"
                    else -> "Loading image"
                } }
                .pointerInput(file.path, bounds, loaded) {
                    detectTapGestures(onDoubleTap = { point ->
                        if (loaded) bounds?.let { transform = ImageViewportMath.doubleTap(transform, it, point.x, point.y) }
                    })
                }
                .pointerInput(file.path, bounds, loaded) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        if (loaded) bounds?.let {
                            transform = ImageViewportMath.transform(transform, it, zoom, centroid.x, centroid.y, pan.x, pan.y)
                        }
                    }
                }, contentAlignment = Alignment.Center) {
                key(retry) {
                    AsyncImage(model = request, contentDescription = title, contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().graphicsLayer {
                            scaleX = transform.scale; scaleY = transform.scale
                            translationX = transform.x; translationY = transform.y
                        },
                        onLoading = { loaded = false; failed = false },
                        onSuccess = { result ->
                            imageSize = IntSize(result.result.drawable.intrinsicWidth, result.result.drawable.intrinsicHeight)
                            failed = false; loaded = true
                        },
                        onError = { loaded = false; failed = true })
                }
                if (!loaded && !failed) CircularProgressIndicator(color = Lavender)
                if (failed) Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("This screenshot couldn't be opened.", color = Muted, textAlign = TextAlign.Center)
                    TextButton(onClick = { failed = false; loaded = false; retry++ }) { Text("Try again") }
                }
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    IconButton(onClick = { bounds?.let { transform = ImageViewportMath.transform(transform, it, 1f / 1.5f) } },
                        enabled = loaded && bounds != null && transform.scale > 1f,
                        modifier = Modifier.semantics { contentDescription = "Zoom out" }) { Text("−", fontSize = 26.sp) }
                    Text("$percent%", color = Cream, modifier = Modifier.width(58.dp), textAlign = TextAlign.Center)
                    IconButton(onClick = { bounds?.let { transform = ImageViewportMath.transform(transform, it, 1.5f) } },
                        enabled = loaded && bounds != null && transform.scale < ImageViewportMath.MAX_SCALE) {
                        Icon(Icons.Outlined.Add, "Zoom in")
                    }
                    OutlinedButton(onClick = { transform = ImageViewport() }, enabled = loaded, shape = CircleShape) { Text("Fit") }
                }
                Text("Pinch to zoom · drag to explore · double-tap to zoom/reset", fontSize = 10.sp, color = Muted,
                    modifier = Modifier.padding(top = 4.dp), textAlign = TextAlign.Center)
            }
        }
        }
    }
}
