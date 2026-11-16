package io.github.stardust824

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.jetbrains.compose.resources.Font
import plant_app.shared.generated.resources.Merriweather
import plant_app.shared.generated.resources.Res

private val Green = Color(0xFF3DBB6C)

/**
 * One plant's page: large photo (tap to enlarge), other photos as thumbnails,
 * then name, scientific name and description.
 * Opened from Browse Plants and from Best matches on the ID screen.
 */
@Composable
fun PlantDetailScreen(plantId: String?) {
    val merriweather = FontFamily(Font(Res.font.Merriweather))
    // null while loading
    val loaded by produceState<Result<Plant?>?>(initialValue = null, plantId) {
        value = runCatching { plantId?.let { PlantRepository.plant(it) } }
    }

    // Which photo is shown large, and whether the full-screen view is open.
    // rememberSaveable keeps these across screen rotation.
    var selectedPhoto by rememberSaveable(plantId) { mutableStateOf(0) }
    var enlarged by rememberSaveable(plantId) { mutableStateOf(false) }

    MaterialTheme {
        Column(
            modifier = Modifier
                .background(Color.White)
                .safeContentPadding()
                .fillMaxSize(),
        ) {
            val result = loaded
            val plant = result?.getOrNull()
            when {
                result == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Green)
                }

                plant == null -> Text(
                    result.exceptionOrNull()?.let { "Couldn't load plant: ${it.message}" }
                        ?: "Plant not found",
                    modifier = Modifier.padding(top = 64.dp, start = 16.dp, end = 16.dp),
                )

                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                ) {
                    Spacer(Modifier.height(26.dp))
                    Text(
                        plant.name,
                        fontSize = 24.sp,
                        fontFamily = merriweather,
                        textAlign = TextAlign.Center,
                        // keep clear of the floating back button in the top-left corner
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp),
                    )
                    Text(
                        plant.scientificName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 12.dp),
                    )

                    PhotoSection(
                        plant = plant,
                        selected = selectedPhoto.coerceIn(0, (plant.photos.size - 1).coerceAtLeast(0)),
                        onSelect = { selectedPhoto = it },
                        onEnlarge = { enlarged = true },
                    )

                    Spacer(Modifier.height(16.dp))
                    Text(
                        plant.description.ifEmpty { "No description yet." },
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (plant.description.isEmpty()) Color.Gray else Color.Unspecified,
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }

            if (plant != null && enlarged && plant.photos.isNotEmpty()) {
                EnlargedPhoto(
                    plant = plant,
                    index = selectedPhoto.coerceIn(0, plant.photos.size - 1),
                    onIndexChange = { selectedPhoto = it },
                    onClose = { enlarged = false },
                )
            }
        }
    }
}

/** The large photo, its credit, and (if there's more than one photo) a row of thumbnails. */
@Composable
private fun PhotoSection(
    plant: Plant,
    selected: Int,
    onSelect: (Int) -> Unit,
    onEnlarge: () -> Unit,
) {
    val photo = plant.photos.getOrNull(selected)

    PlantPhotoImage(
        photo = photo,
        contentDescription = "${plant.name} photo ${selected + 1}",
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = photo != null, onClick = onEnlarge),
    )
    if (photo != null) {
        Text(
            listOf(photo.credit, "Tap photo to enlarge").filter { it.isNotEmpty() }.joinToString("  ·  "),
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp),
        )
    }

    if (plant.photos.size > 1) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 10.dp).horizontalScroll(rememberScrollState()),
        ) {
            plant.photos.forEachIndexed { index, thumb ->
                val shape = RoundedCornerShape(6.dp)
                PlantPhotoImage(
                    photo = thumb,
                    contentDescription = "${plant.name} photo ${index + 1}",
                    modifier = Modifier
                        .size(64.dp)
                        .clip(shape)
                        // green outline on the photo currently shown large
                        .border(if (index == selected) 3.dp else 0.dp, Green, shape)
                        .clickable { onSelect(index) },
                )
            }
        }
    }
}

/**
 * Full-screen view of one photo on a black background. Tap anywhere to close;
 * ‹ › switch between photos when there's more than one.
 */
@Composable
private fun EnlargedPhoto(
    plant: Plant,
    index: Int,
    onIndexChange: (Int) -> Unit,
    onClose: () -> Unit,
) {
    val photo = plant.photos[index]
    val count = plant.photos.size

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false), // use the whole screen
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(onClick = onClose),
        ) {
            PlantPhotoImage(
                photo = photo,
                contentDescription = "${plant.name} photo ${index + 1}, enlarged",
                contentScale = ContentScale.Fit, // whole photo, no cropping
                backgroundColor = Color.Black,
                modifier = Modifier.fillMaxSize().padding(vertical = 48.dp),
            )

            Text(
                "✕",
                color = Color.White,
                fontSize = 24.sp,
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).clickable(onClick = onClose),
            )

            if (count > 1) {
                // Wraps around: › on the last photo goes back to the first.
                ArrowButton("‹", Modifier.align(Alignment.CenterStart)) { onIndexChange((index - 1 + count) % count) }
                ArrowButton("›", Modifier.align(Alignment.CenterEnd)) { onIndexChange((index + 1) % count) }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp),
            ) {
                if (count > 1) {
                    Text("${index + 1} / $count", color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
                if (photo.credit.isNotEmpty()) {
                    Text(photo.credit, color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun ArrowButton(symbol: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .padding(8.dp)
            .size(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(onClick = onClick),
    ) {
        Text(symbol, color = Color.White, fontSize = 32.sp)
    }
}
