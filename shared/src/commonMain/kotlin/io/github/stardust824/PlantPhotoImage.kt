package io.github.stardust824

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.decodeToImageBitmap
import plant_app.shared.generated.resources.Res

/** Background shown while a photo loads, or when a plant has no photo. */
private val PlaceholderColor = Color(0xFFE6F2EA)

/**
 * Shows one plant photo from composeResources/files/photos.
 *
 * [contentScale]: Crop fills the space and trims the edges (good for thumbnails and cards);
 * Fit shows the whole photo (good for the enlarged view).
 * Photos with a "no derivatives" license (see [PlantPhoto.noDerivatives]) are always shown
 * whole, whatever [contentScale] says.
 * If [photo] is null or the file can't be read, a pale green box is shown instead.
 */
@Composable
fun PlantPhotoImage(
    photo: PlantPhoto?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    showNoPhotoText: Boolean = true,
    /** Color of the margins around an uncropped photo, and of the loading/no-photo box. */
    backgroundColor: Color = PlaceholderColor,
) {
    // Loads in the background; `bitmap` stays null until the photo is ready (or if it fails).
    val bitmap by produceState<ImageBitmap?>(initialValue = null, photo?.file) {
        value = photo?.let { PhotoCache.load(it.file) }
    }

    val image = bitmap
    if (image != null && photo != null && photo.noDerivatives) {
        // "No derivatives" license: never crop. Show the whole photo, with pale margins
        // where it doesn't fill the space.
        Box(modifier.background(backgroundColor), contentAlignment = Alignment.Center) {
            Image(
                bitmap = image,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
            )
        }
    } else if (image != null) {
        Image(
            bitmap = image,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
        )
    } else {
        Box(modifier.background(backgroundColor), contentAlignment = Alignment.Center) {
            if (photo == null && showNoPhotoText) {
                Text("No photo yet", color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

/**
 * Keeps decoded photos in memory so scrolling back and forth doesn't decode them again.
 * With a dozen ~1200px photos this is a few tens of MB; if the photo count grows a lot,
 * this should become a size-limited cache.
 */
private object PhotoCache {
    private val mutex = Mutex()
    private val cache = mutableMapOf<String, ImageBitmap?>()

    suspend fun load(file: String): ImageBitmap? {
        mutex.withLock { if (cache.containsKey(file)) return cache[file] }
        val bitmap = try {
            // Decoding a JPEG is slow-ish, so do it off the main thread.
            withContext(Dispatchers.Default) {
                Res.readBytes("files/photos/$file").decodeToImageBitmap()
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            println("Couldn't load photo '$file': ${e.message}")
            null // missing or unreadable file → placeholder
        }
        mutex.withLock { cache[file] = bitmap }
        return bitmap
    }
}
