package com.meitu.generator.ui.assistant

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.meitu.generator.data.local.entity.PresetEntity
import com.meitu.generator.ui.components.*
import com.meitu.generator.ui.theme.*

@Composable
fun AssistantScreen(
    viewModel: AssistantViewModel = hiltViewModel()
) {
    val uri by viewModel.referenceImageUri.collectAsState()
    val fileName by viewModel.referenceFileName.collectAsState()
    val fileSize by viewModel.referenceFileSize.collectAsState()
    val prompt by viewModel.prompt.collectAsState()
    val isEditing by viewModel.isPromptEditing.collectAsState()
    val isReversing by viewModel.isReversing.collectAsState()
    val error by viewModel.reverseError.collectAsState()
    val ratio by viewModel.ratio.collectAsState()
    val quality by viewModel.quality.collectAsState()
    val model by viewModel.model.collectAsState()
    val suggestedTags by viewModel.suggestedTags.collectAsState()
    val categories by viewModel.tagCategories.collectAsState()
    val presets by viewModel.allPresets.collectAsState()
    val showDialog by viewModel.showPresetDialog.collectAsState()
    val presetName by viewModel.presetName.collectAsState()

    var showModelDropdown by remember { mutableStateOf(false) }
    var showQualityDropdown by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { pickedUri: Uri? ->
        pickedUri?.let { viewModel.onReferenceImageSelected(it) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BgPrimary),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text("AI\u52A9\u624B", fontSize = 20.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                Text("\u4E0A\u4F20\u53C2\u8003\u56FE AI\u81EA\u52A8\u53CD\u63A8\u63D0\u793A\u8BCD", fontSize = 12.sp, color = TextTertiary)
            }
        }

        item {
            if (uri == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth().height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BgSecondary)
                        .border(2.dp, Divider, RoundedCornerShape(12.dp))
                        .clickable { imagePicker.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CloudUpload, null, tint = TextTertiary, modifier = Modifier.size(60.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("\u70B9\u51FB\u4E0A\u4F20\u53C2\u8003\u56FE", fontSize = 14.sp, color = TextTertiary)
                        Text("\u652F\u6301JPG/PNG \u6700\u592710MB", fontSize = 11.sp, color = TextTertiary)
                    }
                }
            } else {
                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = uri, contentDescription = null,
                            modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(fileName, fontSize = 13.sp, color = TextSecondary)
                            Text(fileSize, fontSize = 11.sp, color = TextTertiary)
                        }
                        TextButton(text = "\u91CD\u65B0\u4E0A\u4F20", onClick = { imagePicker.launch("image/*") }, color = TextSecondary)
                    }
                }
            }
        }

        item {
            if (uri != null && prompt.isEmpty() && !isReversing) {
                GradientButton(text = "\u5F00\u59CB\u53CD\u63A8", onClick = { viewModel.startReversePrompt() }, modifier = Modifier.fillMaxWidth())
            }
            if (isReversing) {
                Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = BrandPurple, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("AI\u5206\u6790\u4E2D...", color = TextSecondary, fontSize = 14.sp)
                    }
                }
            }
            error?.let { Text(it, color = ErrorRed, fontSize = 13.sp) }
        }

        if (prompt.isNotEmpty()) {
            item {
                GlassCard {
                    Column {
                        if (isEditing) {
                            GlassTextField(value = prompt, onValueChange = { viewModel.updatePrompt(it) }, maxLines = 8)
                        } else {
                            Text(prompt, fontSize = 13.sp, color = TextSecondary, lineHeight = 22.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (isEditing) {
                                TextButton(text = "\u5B8C\u6210\u7F16\u8F91", onClick = { viewModel.setPromptEditing(false) })
                            } else {
                                TextButton(text = "\u7F16\u8F91", onClick = { viewModel.setPromptEditing(true) })
                                TextButton(text = "\u91CD\u65B0\u53CD\u63A8", onClick = { viewModel.startReversePrompt() })
                            }
                        }
                    }
                }
            }

            items(categories.size) { idx ->
                val cat = categories[idx]
                Column {
                    Text(cat.name, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        cat.options.forEach { tag ->
                            TagChip(
                                text = tag,
                                selected = tag in cat.selected,
                                suggested = tag in suggestedTags,
                                onClick = { viewModel.toggleTag(idx, tag) }
                            )
                        }
                    }
                }
            }

            item {
                SectionTitle("\u751F\u6210\u53C2\u6570")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("1:1", "3:4", "9:16", "16:9").forEach { r ->
                        TagChip(text = r, selected = ratio == r, onClick = { viewModel.setRatio(r) })
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        SecondaryButton(text = "\u753B\u8D28: $quality", onClick = { showQualityDropdown = !showQualityDropdown })
                        if (showQualityDropdown) {
                            Popup(onDismissRequest = { showQualityDropdown = false }) {
                                Card(colors = CardDefaults.cardColors(containerColor = BgTertiary), shape = RoundedCornerShape(8.dp)) {
                                    listOf("SD", "HD").forEach { q ->
                                        Text(
                                            text = q, fontSize = 14.sp,
                                            color = if (q == quality) BrandPurple else TextSecondary,
                                            modifier = Modifier.padding(12.dp).clickable { viewModel.setQuality(q); showQualityDropdown = false }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        SecondaryButton(text = "\u6A21\u578B: $model", onClick = { showModelDropdown = !showModelDropdown })
                        if (showModelDropdown) {
                            Popup(onDismissRequest = { showModelDropdown = false }) {
                                Card(colors = CardDefaults.cardColors(containerColor = BgTertiary), shape = RoundedCornerShape(8.dp)) {
                                    listOf("\u771F\u5B9E\u5199\u5B9E", "\u827A\u672F\u98CE\u683C", "\u52A8\u6F2B\u4E8C\u6B21\u5143", "\u7535\u5F71\u8D28\u611F", "\u6027\u611F\u65F6\u5C1A").forEach { m ->
                                        Text(
                                            text = m, fontSize = 14.sp,
                                            color = if (m == model) BrandPurple else TextSecondary,
                                            modifier = Modifier.padding(12.dp).clickable { viewModel.setModel(m); showModelDropdown = false }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                GradientButton(text = "\u4FDD\u5B58\u4E3A\u9884\u8BBE", onClick = { viewModel.showSavePresetDialog() }, modifier = Modifier.fillMaxWidth())
            }
        }

        if (presets.isNotEmpty()) {
            item { SectionTitle("\u5386\u53F2\u9884\u8BBE") }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(presets) { preset ->
                        PresetCard(
                            preset = preset,
                            onActivate = { viewModel.activatePreset(preset.id) },
                            onDelete = { viewModel.deletePreset(preset) },
                            canDelete = presets.size > 1
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissPresetDialog() },
            containerColor = BgSecondary,
            title = { Text("\u4FDD\u5B58\u9884\u8BBE", color = TextPrimary) },
            text = {
                GlassTextField(value = presetName, onValueChange = { viewModel.setPresetName(it) }, placeholder = "\u9884\u8BBE\u540D\u79F0", singleLine = true)
            },
            confirmButton = { TextButton(text = "\u4FDD\u5B58", onClick = { viewModel.savePreset() }, color = BrandPurple) },
            dismissButton = { TextButton(text = "\u53D6\u6D88", onClick = { viewModel.dismissPresetDialog() }, color = TextTertiary) }
        )
    }
}

@Composable
fun PresetCard(preset: PresetEntity, onActivate: () -> Unit, onDelete: () -> Unit, canDelete: Boolean) {
    Column(
        modifier = Modifier
            .width(160.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(BgTertiary)
            .then(if (preset.isActive) Modifier.border(1.dp, BrandPurple, RoundedCornerShape(10.dp)) else Modifier)
            .clickable { onActivate() }
            .padding(12.dp)
    ) {
        Text(preset.name, fontSize = 13.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
            fontWeight = if (preset.isActive) FontWeight.Medium else FontWeight.Normal)
        if (preset.isActive) { Spacer(Modifier.height(2.dp)); Box(Modifier.width(30.dp).height(2.dp).background(BrandPurple)) }
        Spacer(Modifier.height(4.dp))
        Text(preset.prompt, fontSize = 11.sp, color = TextTertiary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        Text("${preset.ratio} / ${preset.model}", fontSize = 11.sp, color = TextTertiary)
        Spacer(Modifier.height(4.dp))
        Row {
            Text("\u590D\u5236", fontSize = 11.sp, color = BrandCyan, modifier = Modifier.clickable { onActivate() })
            if (canDelete) {
                Spacer(Modifier.width(8.dp))
                Text("\u5220\u9664", fontSize = 11.sp, color = ErrorRed, modifier = Modifier.clickable { onDelete() })
            }
        }
    }
}
