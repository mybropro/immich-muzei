package dev.abdus.apps.immich.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.abdus.apps.immich.api.ImmichClientProvider
import dev.abdus.apps.immich.data.AppPreferences
import dev.abdus.apps.immich.data.ImmichConfig
import dev.abdus.apps.immich.data.ImmichPersonUiModel
import dev.abdus.apps.immich.data.ImmichRepository
import dev.abdus.apps.immich.provider.ArtworkStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class PeoplePickerUiState(
    val config: ImmichConfig = ImmichConfig(null, null, emptySet(), emptySet(), false),
    val people: List<ImmichPersonUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class PeoplePickerViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = AppPreferences(application)
    private lateinit var repository: ImmichRepository
    private val artworkStore = ArtworkStore(application)

    private val _state = MutableStateFlow(PeoplePickerUiState())
    val state: StateFlow<PeoplePickerUiState> = _state

    private var loadJob: Job? = null

    companion object {
        private const val TAG = "PeoplePickerVM"
    }

    init {
        // Load cached data immediately
        loadCachedData()

        viewModelScope.launch {
            prefs.configFlow.collectLatest { config ->
                Log.d(TAG, "Config changed: serverUrl=${config.serverUrl}, hasApiKey=${!config.apiKey.isNullOrBlank()}")
                val oldConfig = _state.value.config
                _state.value = _state.value.copy(config = config)

                val client = ImmichClientProvider.fromConfig(config)
                if (client != null) {
                    repository = ImmichRepository(client)
                }

                // Clear cached data if credentials changed
                if (config.serverUrl != oldConfig.serverUrl || config.apiKey != oldConfig.apiKey) {
                    if (!config.isConfigured) {
                        _state.value = _state.value.copy(people = emptyList())
                    }
                }
            }
        }
    }

    private fun loadCachedData() {
        val cachedPeople = prefs.getCachedPeople()
        Log.d(TAG, "Loaded ${cachedPeople.size} cached people")
        _state.value = _state.value.copy(people = cachedPeople)
    }

    fun refreshFromApi() {
        val config = _state.value.config
        if (!config.isConfigured) return
        if (!::repository.isInitialized) return
        loadPeople()
    }

    fun togglePerson(id: String) {
        val currentSelection = _state.value.config.selectedPersonIds
        val newSelection = currentSelection.toMutableSet().apply {
            if (!add(id)) remove(id)
        }
        Log.d(TAG, "Toggling person $id, new selection size: ${newSelection.size}")
        prefs.updateSelectedPeople(newSelection)
        clearPhotos()
    }

    private fun clearPhotos() {
        Log.d(TAG, "Clearing all photos")
        viewModelScope.launch {
            artworkStore.clearAll()
        }
    }

    private fun loadPeople() {
        val config = _state.value.config
        if (!config.isConfigured) return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            try {
                Log.d(TAG, "Loading people from ${config.apiBaseUrl}")

                val people = repository.fetchPeople()
                Log.d(TAG, "Fetched ${people.size} named people")
                val uiPeople = people.map { person ->
                    ImmichPersonUiModel(
                        id = person.id,
                        name = person.name,
                        thumbnailUrl = person.thumbnailUrl
                    )
                }.sortedBy { it.name.lowercase() }

                prefs.saveCachedPeople(uiPeople)

                _state.value = _state.value.copy(
                    people = uiPeople,
                    isLoading = false,
                    errorMessage = null
                )
            } catch (t: Throwable) {
                Log.e(TAG, "Error loading people", t)
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = t.message
                )
            }
        }
    }
}
