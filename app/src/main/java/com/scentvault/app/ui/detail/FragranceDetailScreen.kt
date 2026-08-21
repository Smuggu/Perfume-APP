package com.scentvault.app.ui.detail

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.scentvault.app.data.Fragrance
import com.scentvault.app.photo.PhotoStore
import com.scentvault.app.ui.FragranceViewModel
import com.scentvault.app.ui.formatDateMillis
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FragranceDetailScreen(
    fragranceId: Long?,
    viewModel: FragranceViewModel,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val isNew = fragranceId == null

    val existing by remember(fragranceId) {
        fragranceId?.let { viewModel.observeFragrance(it) } ?: flowOf(null)
    }.collectAsState(initial = null)

    var loadedOnce by rememberSaveable { mutableStateOf(false) }

    var name by rememberSaveable { mutableStateOf("") }
    var brand by rememberSaveable { mutableStateOf("") }
    var bottleSizeText by rememberSaveable { mutableStateOf("") }
    var purchaseDateMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var batchCode by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var photoPath by rememberSaveable { mutableStateOf<String?>(null) }
    val tags = remember { mutableStateListOf<String>() }
    var tagInput by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(existing) {
        val entry = existing
        if (!loadedOnce && entry != null) {
            name = entry.fragrance.name
            brand = entry.fragrance.brand
            bottleSizeText = entry.fragrance.bottleSizeMl?.toString() ?: ""
            purchaseDateMillis = entry.fragrance.purchaseDateMillis
            batchCode = entry.fragrance.batchCode
            notes = entry.fragrance.notes
            photoPath = entry.fragrance.photoPath
            tags.clear()
            tags.addAll(entry.tags.map { it.name })
            loadedOnce = true
        }
    }

    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    fun replacePhoto(uri: Uri) {
        viewModel.importPhoto(uri) { newName ->
            if (newName != null) {
                val old = photoPath
                photoPath = newName
                if (old != null && old != newName) PhotoStore.delete(context, old)
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let(::replacePhoto) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) pendingCameraUri?.let(::replacePhoto)
        pendingCameraUri = null
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val (_, uri) = viewModel.createCameraCaptureUri()
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        }
    }

    fun launchCamera() {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            val (_, uri) = viewModel.createCameraCaptureUri()
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "Add bottle" else "Edit bottle") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isNew) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PhotoPicker(
                photoPath = photoPath,
                onPickGallery = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onPickCamera = { launchCamera() },
                onRemovePhoto = {
                    PhotoStore.delete(context, photoPath)
                    photoPath = null
                }
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = brand,
                onValueChange = { brand = it },
                label = { Text("Brand / house") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = bottleSizeText,
                    onValueChange = { input -> if (input.all { it.isDigit() }) bottleSizeText = input },
                    label = { Text("Size (ml)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = batchCode,
                    onValueChange = { batchCode = it },
                    label = { Text("Batch code") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text(formatDateMillis(purchaseDateMillis) ?: "Set purchase date")
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            TagEditor(
                tags = tags,
                tagInput = tagInput,
                onTagInputChange = { tagInput = it },
                onAddTag = {
                    val cleaned = tagInput.trim()
                    if (cleaned.isNotEmpty() && cleaned !in tags) tags.add(cleaned)
                    tagInput = ""
                },
                onRemoveTag = { tags.remove(it) }
            )

            Button(
                onClick = {
                    val fragrance = Fragrance(
                        id = fragranceId ?: 0L,
                        name = name.trim(),
                        brand = brand.trim(),
                        bottleSizeMl = bottleSizeText.toIntOrNull(),
                        purchaseDateMillis = purchaseDateMillis,
                        batchCode = batchCode.trim(),
                        notes = notes.trim(),
                        photoPath = photoPath
                    )
                    viewModel.save(fragrance, tags.toList()) { onDone() }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = purchaseDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    purchaseDateMillis = state.selectedDateMillis
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state)
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this bottle?") },
            text = { Text("This removes it and its photo from ScentVault. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    existing?.fragrance?.let { viewModel.delete(it) }
                    showDeleteConfirm = false
                    onDone()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun PhotoPicker(
    photoPath: String?,
    onPickGallery: () -> Unit,
    onPickCamera: () -> Unit,
    onRemovePhoto: () -> Unit
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.3f)
                .clip(RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (photoPath != null) {
                AsyncImage(
                    model = PhotoStore.fileFor(context, photoPath),
                    contentDescription = "Bottle photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    Icons.Filled.LocalFlorist,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(64.dp)
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onPickCamera) {
                Icon(Icons.Filled.CameraAlt, contentDescription = null)
                Text(" Camera")
            }
            OutlinedButton(onClick = onPickGallery) {
                Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                Text(" Gallery")
            }
            if (photoPath != null) {
                OutlinedButton(onClick = onRemovePhoto) {
                    Icon(Icons.Filled.Close, contentDescription = null)
                    Text(" Remove")
                }
            }
        }
    }
}

@Composable
private fun TagEditor(
    tags: List<String>,
    tagInput: String,
    onTagInputChange: (String) -> Unit,
    onAddTag: () -> Unit,
    onRemoveTag: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Tags", style = MaterialTheme.typography.labelLarge)
        if (tags.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tags) { tag ->
                    InputChip(
                        selected = false,
                        onClick = { onRemoveTag(tag) },
                        label = { Text(tag) },
                        trailingIcon = {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Remove $tag",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = tagInput,
                onValueChange = onTagInputChange,
                label = { Text("Add a tag (e.g. woody, evening, favorite)") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = onAddTag, enabled = tagInput.isNotBlank()) {
                Text("Add")
            }
        }
    }
}

