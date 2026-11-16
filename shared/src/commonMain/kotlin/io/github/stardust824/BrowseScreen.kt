package io.github.stardust824

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import plant_app.shared.generated.resources.Merriweather
import plant_app.shared.generated.resources.Res

/** "Browse Plants": a grid of every plant with its main photo, name and scientific name. */
@Composable
fun BrowseScreen(onPlantClick: (plantId: String) -> Unit) {
    val merriweather = FontFamily(Font(Res.font.Merriweather))
    // null while loading
    val plants by produceState<Result<List<Plant>>?>(initialValue = null) {
        value = runCatching { PlantRepository.plants() }
    }

    MaterialTheme {
        Column(
            modifier = Modifier
                .background(Color.White)
                .safeContentPadding()
                .fillMaxSize(),
        ) {
            Spacer(modifier = Modifier.height(26.dp))
            Text(
                "Browse Plants",
                fontSize = 24.sp,
                fontFamily = merriweather,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))

            val result = plants
            when {
                result == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF3DBB6C))
                }

                result.isFailure -> Text(
                    "Couldn't load plants: ${result.exceptionOrNull()?.message}",
                    modifier = Modifier.padding(16.dp),
                )

                // Adaptive: as many ~150dp-wide columns as fit (2 on a phone, more on a wide window).
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(result.getOrThrow(), key = { it.id }) { plant ->
                        PlantCard(plant, onClick = { onPlantClick(plant.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun PlantCard(plant: Plant, onClick: () -> Unit) {
    Column(modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick)) {
        PlantPhotoImage(
            photo = plant.photos.firstOrNull(),
            contentDescription = plant.name,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(RoundedCornerShape(8.dp)),
        )
        Text(
            plant.name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            plant.scientificName,
            style = MaterialTheme.typography.bodySmall,
            fontStyle = FontStyle.Italic,
            color = Color.Gray,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
