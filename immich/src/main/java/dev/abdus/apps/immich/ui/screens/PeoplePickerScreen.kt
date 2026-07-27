package dev.abdus.apps.immich.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import dev.abdus.apps.immich.data.ImmichPersonUiModel
import dev.abdus.apps.immich.ui.PeoplePickerUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeoplePickerScreen(
    state: PeoplePickerUiState,
    imageLoader: ImageLoader,
    onPersonClick: (String) -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit
) {
    var filterText by remember { mutableStateOf("") }

    val filteredPeople = remember(state.people, filterText) {
        if (filterText.isBlank()) {
            state.people
        } else {
            state.people.filter { person ->
                person.name.contains(filterText, ignoreCase = true)
            }
        }
    }

    val selectedPersonIds = remember(state.config.selectedPersonIds) {
        state.config.selectedPersonIds
    }

    // Partition into picked and available, and sort each section by name
    val (pickedPeople, availablePeople) = remember(filteredPeople, selectedPersonIds) {
        val (picked, available) = filteredPeople.partition { it.id in selectedPersonIds }
        Pair(picked.sortedBy { it.name.lowercase() }, available.sortedBy { it.name.lowercase() })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select People") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = filterText,
                        onValueChange = { filterText = it },
                        label = { Text("Filter people") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    state.errorMessage?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Picked people section
                item {
                    Text(
                        text = "Picked (${pickedPeople.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                if (pickedPeople.isEmpty()) {
                    item {
                        Text(
                            text = "No picked people",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(
                        items = pickedPeople,
                        key = { person -> person.id },
                        contentType = { "person_item" }
                    ) { person ->
                        PersonPickerRow(
                            person = person,
                            selected = true,
                            imageLoader = imageLoader,
                            onClick = { onPersonClick(person.id) }
                        )
                    }
                }

                // Available people section
                item {
                    Text(
                        text = "Available (${availablePeople.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                if (availablePeople.isEmpty()) {
                    item {
                        Text(
                            text = if (state.people.isEmpty()) {
                                "No named people found. Name some faces in Immich, then pull to refresh."
                            } else {
                                "No available people"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(
                        items = availablePeople,
                        key = { person -> person.id },
                        contentType = { "person_item" }
                    ) { person ->
                        PersonPickerRow(
                            person = person,
                            selected = false,
                            imageLoader = imageLoader,
                            onClick = { onPersonClick(person.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonPickerRow(
    person: ImmichPersonUiModel,
    selected: Boolean,
    imageLoader: ImageLoader,
    onClick: () -> Unit
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Checkbox(
                checked = selected,
                onCheckedChange = { onClick() }
            )
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (person.thumbnailUrl != null) {
                    AsyncImage(
                        model = person.thumbnailUrl,
                        imageLoader = imageLoader,
                        contentDescription = person.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = person.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
