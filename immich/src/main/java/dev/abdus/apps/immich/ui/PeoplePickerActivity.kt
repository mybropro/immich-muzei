package dev.abdus.apps.immich.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abdus.apps.immich.ui.screens.PeoplePickerScreen

class PeoplePickerActivity : ComponentActivity() {
    private val viewModel: PeoplePickerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Refresh people from API when picker is opened
        viewModel.refreshFromApi()

        setContent {
            MaterialTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                val imageLoader = remember { ImmichImageLoaderProvider.get(this) }
                PeoplePickerScreen(
                    state = state,
                    imageLoader = imageLoader,
                    onPersonClick = viewModel::togglePerson,
                    onRefresh = viewModel::refreshFromApi,
                    onBack = { finish() }
                )
            }
        }
    }
}
