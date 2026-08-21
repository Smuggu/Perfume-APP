package com.scentvault.app.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.scentvault.app.data.FragranceWithTags
import com.scentvault.app.photo.PhotoStore
import com.scentvault.app.ui.FragranceViewModel
import com.scentvault.app.ui.formatDateMillis

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FragranceListScreen(
    viewModel: FragranceViewModel,
    onAddNew: () -> Unit,
    onOpen: (Long) -> Unit,
    onOpenSettings: () -> Unit
) {
    val fragrances by viewModel.filteredFragrances.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val selectedTags by viewModel.selectedTagsState.collectAsState()
    val searchQuery by viewModel.searchQueryState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ScentVault") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddNew) {
                Icon(Icons.Filled.Add, contentDescription = "Add fragrance")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::setSearchQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search name, brand, notes, batch code, tags…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true
            )

            if (allTags.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allTags) { tag ->
                        FilterChip(
                            selected = tag in selectedTags,
                            onClick = { viewModel.toggleTagFilter(tag) },
                            label = { Text(tag) }
                        )
                    }
                    if (selectedTags.isNotEmpty()) {
                        item {
                            AssistChip(
                                onClick = { viewModel.clearTagFilters() },
                                label = { Text("Clear") }
                            )
                        }
                    }
                }
            }

            if (fragrances.isEmpty()) {
                EmptyState(hasFilters = searchQuery.isNotBlank() || selectedTags.isNotEmpty())
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(fragrances, key = { it.fragrance.id }) { entry ->
                        FragranceCard(entry = entry, onClick = { onOpen(entry.fragrance.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun FragranceCard(entry: FragranceWithTags, onClick: () -> Unit) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                contentAlignment = Alignment.Center
            ) {
                val photoPath = entry.fragrance.photoPath
                if (photoPath != null) {
                    AsyncImage(
                        model = PhotoStore.fileFor(context, photoPath),
                        contentDescription = entry.fragrance.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Filled.LocalFlorist,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.fillMaxSize().padding(28.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    entry.fragrance.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                if (entry.fragrance.brand.isNotBlank()) {
                    Text(
                        entry.fragrance.brand,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                formatDateMillis(entry.fragrance.purchaseDateMillis)?.let { date ->
                    Text(
                        date,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(hasFilters: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            if (hasFilters) "No bottles match your search." else "Your collection is empty.\nTap + to add your first bottle.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(32.dp)
        )
    }
}
