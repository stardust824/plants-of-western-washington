package io.github.stardust824

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything the ID screen needs to draw itself. */
data class IdUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    /** Every question, including hidden ones. */
    val questions: List<Question> = emptyList(),
    /** The questions currently shown (e.g. leaf questions are hidden once "needles" is picked). */
    val visibleQuestions: List<Question> = emptyList(),
    /** trait → selected chip */
    val choices: Map<String, String> = emptyMap(),
    /** trait → text typed in the number box (kept as text so half-typed input like "4." works) */
    val numbers: Map<String, String> = emptyMap(),
    /** The valid answers, as passed to the matcher. */
    val answers: Map<String, TraitValue> = emptyMap(),
    /** All plants, best match first. */
    val matches: List<PlantMatch> = emptyList(),
)

/**
 * Holds the ID screen's state. Living in a ViewModel means answers survive screen rotation.
 *
 * [load] is a parameter so tests can pass in plants without reading the YAML file.
 */
class IdViewModel(
    private val load: suspend () -> List<Plant> = PlantRepository::plants,
) : ViewModel() {

    private val _state = MutableStateFlow(IdUiState())
    val state: StateFlow<IdUiState> = _state.asStateFlow()

    // Created once the plants have loaded.
    private var matcher: PlantMatcher? = null

    init {
        viewModelScope.launch {
            try {
                val plants = load()
                val newMatcher = PlantMatcher(plants)
                matcher = newMatcher
                val questions = IdQuestions.build(plants)
                _state.update {
                    it.copy(
                        isLoading = false,
                        questions = questions,
                        visibleQuestions = questions,
                        matches = newMatcher.rank(emptyMap()),
                    )
                }
            } catch (e: CancellationException) {
                throw e // the screen went away; let the coroutine stop normally
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Couldn't load plants") }
            }
        }
    }

    /** Tapping a chip selects it; tapping the selected chip again clears that question. */
    fun selectChoice(trait: String, option: String) = updateAnswers { s ->
        val newChoices = if (s.choices[trait] == option) s.choices - trait else s.choices + (trait to option)
        s.copy(choices = newChoices)
    }

    /** Called on every keystroke in a number box. */
    fun setNumber(trait: String, text: String) = updateAnswers { s ->
        s.copy(numbers = if (text.isEmpty()) s.numbers - trait else s.numbers + (trait to text))
    }

    fun clearAll() = updateAnswers { it.copy(choices = emptyMap(), numbers = emptyMap()) }

    /** Applies [change], then works out which questions show, recomputes the answers and re-ranks the plants. */
    private fun updateAnswers(change: (IdUiState) -> IdUiState) {
        _state.update { old ->
            val s = change(old)
            val visible = IdQuestions.visible(s.questions, s.choices)
            val visibleTraits = visible.map { it.trait }.toSet()
            // Answers to hidden questions are kept in choices/numbers (so switching back restores them)
            // but don't count toward the matches while hidden.
            val answers = IdQuestions.answers(s.choices, s.numbers).filterKeys { it in visibleTraits }
            s.copy(
                visibleQuestions = visible,
                answers = answers,
                matches = matcher?.rank(answers) ?: s.matches,
            )
        }
    }
}
