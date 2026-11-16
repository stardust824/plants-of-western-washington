package io.github.stardust824

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.remember
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.resources.Font
import plant_app.shared.generated.resources.Merriweather
import plant_app.shared.generated.resources.Res

// Same greens used by the drawer and Home buttons.
private val Green = Color(0xFF3DBB6C)
private val SelectedGreen = Color(0xFF4FBA77)

// Number boxes match the chips: Material filter chips are 32dp tall with 8dp corners.
private val ChipHeight = 32.dp
private val ChipShape = RoundedCornerShape(8.dp)
private val NumberBoxWidth = 96.dp // fits a few digits plus "cm" or "petals"

/** How many matches the results panel shows before "Show all". */
private const val TOP_MATCHES = 3

@Composable
fun IdScreen(
    onPlantClick: (plantId: String) -> Unit = {},
    viewModel: IdViewModel = viewModel { IdViewModel() },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val merriweather = FontFamily(Font(Res.font.Merriweather))

    MaterialTheme {
        Column(
            modifier = Modifier
                .background(Color.White)
                .safeContentPadding()
                .fillMaxSize(),
        ) {
            Spacer(modifier = Modifier.height(26.dp))
            Text(
                "ID a Plant",
                fontSize = 24.sp,
                fontFamily = merriweather,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Green)
                }

                state.error != null -> Text(
                    "Couldn't load plants: ${state.error}",
                    modifier = Modifier.padding(16.dp),
                )

                else -> {
                    // Questions scroll; the results panel stays pinned at the bottom.
                    QuestionList(
                        state = state,
                        onChoice = viewModel::selectChoice,
                        onNumber = viewModel::setNumber,
                        modifier = Modifier.weight(1f),
                    )
                    HorizontalDivider()
                    ResultsPanel(
                        matches = state.matches,
                        hasAnswers = state.answers.isNotEmpty(),
                        onClear = viewModel::clearAll,
                        onPlantClick = onPlantClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuestionList(
    state: IdUiState,
    onChoice: (trait: String, option: String) -> Unit,
    onNumber: (trait: String, text: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Questions arrive sorted by section, so groupBy keeps the sections in order.
    val sections = state.visibleQuestions.groupBy { it.group }

    LazyColumn(modifier = modifier.fillMaxWidth()) {
        sections.forEach { (group, questions) ->
            item(key = "header-${group.name}") {
                Text(
                    group.title.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = Green,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                )
            }
            items(questions, key = { it.trait }) { question ->
                QuestionItem(
                    question = question,
                    selected = state.choices[question.trait],
                    numberText = state.numbers[question.trait] ?: "",
                    onChoice = { onChoice(question.trait, it) },
                    onNumber = { onNumber(question.trait, it) },
                )
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuestionItem(
    question: Question,
    selected: String?,
    numberText: String,
    onChoice: (String) -> Unit,
    onNumber: (String) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        when (val kind = question.kind) {
            is QuestionKind.Choice -> {
                Text(question.label, style = MaterialTheme.typography.titleSmall)
                // FlowRow wraps chips onto the next line when they don't fit.
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    kind.options.forEach { option ->
                        // Color questions (flower_color, berry_color, ...) get chips filled with that color.
                        val swatch = if (question.trait.endsWith("_color")) swatchFor(option) else null
                        if (swatch != null) {
                            ColorChip(
                                option = option,
                                swatch = swatch,
                                selected = option == selected,
                                onClick = { onChoice(option) },
                            )
                        } else {
                            FilterChip(
                                selected = option == selected,
                                onClick = { onChoice(option) },
                                label = { Text(option) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SelectedGreen,
                                    selectedLabelColor = Color.White,
                                ),
                            )
                        }
                    }
                }
            }

            is QuestionKind.Number -> {
                // Label above the box, the same as the chip questions.
                Text(question.label, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                val invalid = numberText.isNotBlank() && IdQuestions.parseNumber(numberText) == null
                NumberBox(value = numberText, unit = kind.unit, isError = invalid, onValueChange = onNumber)
                if (invalid) {
                    Text(
                        "Enter a number",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}

/**
 * The border color for each color word used in plants.yaml.
 * A color word that isn't listed here just gets a normal chip, so nothing breaks;
 * add it here to give it a swatch.
 */
private val SWATCHES = mapOf(
    "black" to Color(0xFF222222),
    "blue" to Color(0xFF092C74),
    "brown" to Color(0xFF8B5A2B),
    "green" to Color(0xFF5DA34A),
    "grey" to Color(0xFF9E9E9E),
    "gray" to Color(0xFF9E9E9E),
    "orange" to Color(0xFFF08C2E),
    "pink" to Color(0xFFF29CB7),
    "purple" to Color(0xFF3C0D66),
    "red" to Color(0xFF970C07),
    "white" to Color(0xFFFFFFFF),
    "yellow" to Color(0xFFF2D13D),
)

private fun swatchFor(option: String): Color? = SWATCHES[option]

/**
 * A white chip with a border in the color it names. When selected, the whole chip fills
 * with its border color and the border gets thicker.
 */
@Composable
private fun ColorChip(
    option: String,
    swatch: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    // A white border wouldn't show up on the white page, so the white chip gets a light grey one.
    // The chip also fills with this color when selected, so selecting "white" turns it grey.
    val borderColor = if (swatch == Color.White) Color.LightGray else swatch
    // When filled: dark text on light colors (yellow, pink, grey), white text on dark ones (black, blue, ...).
    val selectedTextColor = if (borderColor.luminance() > 0.4f) Color.Black else Color.White

    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(option) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.White,
            labelColor = Color.Black,
            selectedContainerColor = borderColor,
            selectedLabelColor = selectedTextColor,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = borderColor,
            selectedBorderColor = borderColor,
            borderWidth = 2.dp,
            selectedBorderWidth = 4.dp,
        ),
    )
}

/**
 * A compact outlined number box, about the height of a chip row.
 *
 * Material's OutlinedTextField is at least 56dp tall, so this builds the same look from
 * BasicTextField + the outlined decoration, with smaller padding.
 * There's no label inside the box, which keeps the unit ("cm") visible all the time:
 * Material hides the unit while an empty box is showing its label.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NumberBox(
    value: String,
    unit: String?,
    isError: Boolean,
    onValueChange: (String) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val textStyle = MaterialTheme.typography.bodyMedium
    val colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Green,
        cursorColor = Green,
    )

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.width(NumberBoxWidth).height(ChipHeight),
        singleLine = true,
        textStyle = textStyle,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        interactionSource = interactionSource,
        cursorBrush = SolidColor(Green),
    ) { innerTextField ->
        OutlinedTextFieldDefaults.DecorationBox(
            value = value,
            innerTextField = innerTextField,
            enabled = true,
            singleLine = true,
            visualTransformation = VisualTransformation.None,
            interactionSource = interactionSource,
            isError = isError,
            suffix = unit?.let { { Text(it, style = textStyle) } },
            colors = colors,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            container = {
                OutlinedTextFieldDefaults.Container(
                    enabled = true,
                    isError = isError,
                    interactionSource = interactionSource,
                    colors = colors,
                    shape = ChipShape, // rounded corners like the chips
                )
            },
        )
    }
}

@Composable
private fun ResultsPanel(
    matches: List<PlantMatch>,
    hasAnswers: Boolean,
    onClear: () -> Unit,
    onPlantClick: (plantId: String) -> Unit,
) {
    // Remembered across rotation; it's only about how this panel looks, so it isn't in the ViewModel.
    var showAll by rememberSaveable { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        // Header: title on the left, small text links on the right.
        // (Plain clickable text instead of TextButtons, which are at least 40dp tall.)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Best matches", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            if (hasAnswers && matches.size > TOP_MATCHES) {
                SmallLink(if (showAll) "Top $TOP_MATCHES" else "Show all") { showAll = !showAll }
            }
            if (hasAnswers) {
                Spacer(Modifier.width(16.dp))
                SmallLink("Clear") { onClear() }
            }
        }

        if (!hasAnswers) {
            Text(
                "Answer a question to start",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(vertical = 4.dp),
            )
            return@Column
        }

        val shown = if (showAll) matches else matches.take(TOP_MATCHES)
        // Cap the height so "Show all" doesn't push the questions off screen.
        Column(modifier = Modifier.heightIn(max = 150.dp).verticalScroll(rememberScrollState())) {
            shown.forEachIndexed { index, match ->
                MatchRow(rank = index + 1, match = match, onClick = { onPlantClick(match.plant.id) })
            }
        }
    }
}

/**
 * One line per match: small photo, "1. Name  Scientific name", and the match bar on the right.
 * Tapping the row opens the plant's page.
 */
@Composable
private fun MatchRow(rank: Int, match: PlantMatch, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 3.dp),
    ) {
        PlantPhotoImage(
            photo = match.plant.photos.firstOrNull(),
            contentDescription = match.plant.name,
            showNoPhotoText = false, // too small for text; the pale box is enough
            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(6.dp)),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            buildAnnotatedString {
                append("$rank. ${match.plant.name}  ")
                withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = Color.Gray, fontSize = 12.sp)) {
                    append(match.plant.scientificName)
                }
            },
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis, // long names end in "…" instead of wrapping
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        MatchBar(match)
    }
}

// Match bar colors: green = good, yellow = okay, red = poor.
private val GoodColor = Green
private val OkayColor = Color(0xFFF2C94C)
private val PoorColor = Color(0xFFD8342C)
private val MatchBarWidth = 64.dp

/**
 * A small bar showing how well a plant matches: longer and green for a good match,
 * medium and yellow for an okay one, short and red for a poor one.
 * The light grey track behind it shows what a perfect match would look like.
 */
@Composable
private fun MatchBar(match: PlantMatch) {
    val color = when (match.quality) {
        MatchQuality.GOOD -> GoodColor
        MatchQuality.OKAY -> OkayColor
        MatchQuality.POOR -> PoorColor
    }
    // Never fully empty, so even the worst match shows a sliver of red.
    val fraction = match.score.toFloat().coerceIn(0.08f, 1f)

    Box(
        modifier = Modifier
            .width(MatchBarWidth)
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFEAEAEA)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .clip(RoundedCornerShape(4.dp))
                .background(color),
        )
    }
}

@Composable
private fun SmallLink(text: String, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = Green,
        modifier = Modifier.clickable(onClick = onClick).padding(vertical = 4.dp),
    )
}
