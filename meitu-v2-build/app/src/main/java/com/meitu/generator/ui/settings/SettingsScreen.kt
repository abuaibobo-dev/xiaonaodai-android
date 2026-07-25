package com.meitu.generator.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meitu.generator.ui.components.*
import com.meitu.generator.ui.theme.*

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val interval by viewModel.submitInterval.collectAsState()
    val autoSave by viewModel.autoSaveAlbum.collectAsState()
    val defaultModel by viewModel.defaultModel.collectAsState()
    val defaultQuality by viewModel.defaultQuality.collectAsState()
    val imgbbAutoUpload by viewModel.imgbbAutoUpload.collectAsState()
    val batterySafe by viewModel.batterySafeMode.collectAsState()
    val imgbbKey by viewModel.imgbbKey.collectAsState()
    val imgbbStatus by viewModel.imgbbStatus.collectAsState()
    val showClearConfirm by viewModel.showClearConfirm.collectAsState()
    val exportResult by viewModel.exportResult.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BgPrimary),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item { SectionTitle("\u751F\u6210\u63A7\u5236") }
        item {
            SettingRow("\u63D0\u4EA4\u95F4\u9694", "${interval}\u79D2")
        }
        item {
            SettingSwitchRow("\u4FDD\u5B58\u5230\u76F8\u518C", autoSave) { viewModel.setAutoSave(it) }
        }

        item { SectionTitle("\u9ED8\u8BA4\u914D\u7F6E") }
        item { SettingRow("\u9ED8\u8BA4\u6A21\u578B", defaultModel) }
        item { SettingRow("\u9ED8\u8BA4\u753B\u8D28", defaultQuality) }
        item { SettingSwitchRow("\u7535\u91CF\u4FDD\u62A4", batterySafe) { viewModel.setBatterySafe(it) } }

        item { SectionTitle("\u4E91\u5B58\u50A8") }
        item {
            GlassCard {
                Column {
                    Text("ImgBB API Key", fontSize = 13.sp, color = TextSecondary)
                    Spacer(Modifier.height(6.dp))
                    GlassTextField(value = imgbbKey, onValueChange = { viewModel.setImgbbKey(it) }, placeholder = "\u8F93\u5165ImgBB API Key", singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(if (imgbbStatus == "\u5DF2\u8FDE\u63A5") SuccessGreen else if (imgbbStatus.isEmpty()) TextTertiary else ErrorRed))
                            Spacer(Modifier.width(6.dp))
                            Text(imgbbStatus.ifEmpty { "\u672A\u9A8C\u8BC1" }, fontSize = 12.sp, color = TextTertiary)
                        }
                        TextButton(text = "\u9A8C\u8BC1", onClick = { viewModel.verifyImgbbKey() }, color = BrandCyan)
                    }
                    Spacer(Modifier.height(8.dp))
                    SettingSwitchRow("\u81EA\u52A8\u4E0A\u4F20", imgbbAutoUpload) { viewModel.setImgbbAutoUpload(it) }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        TextButton(text = "\u590D\u5236\u6240\u6709\u4E91\u7AEF\u94FE\u63A5", onClick = { viewModel.exportCloudLinks() }, color = BrandCyan)
                    }
                }
            }
        }

        item { SectionTitle("\u6570\u636E\u7BA1\u7406") }
        item { SettingRow("\u5BFC\u51FA\u9884\u8BBEJSON", "") { viewModel.exportPresets() } }
        item { SettingRow("\u5BFC\u51FA\u4E91\u7AEF\u94FE\u63A5TXT", "") { viewModel.exportCloudLinks() } }
        item { SettingRow("\u6E05\u7406\u7F13\u5B58", "") { viewModel.clearCache() } }
        item { SettingRow("\u6E05\u7A7A\u6240\u6709\u6570\u636E", "", color = ErrorRed) { viewModel.showClearAllConfirm() } }

        item { SectionTitle("\u5173\u4E8E") }
        item {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                Text("\u7F8E\u56FE\u751F\u6210\u5668 v2.0.0", fontSize = 12.sp, color = TextTertiary)
            }
        }

        if (exportResult != null) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(BrandPurple.copy(alpha = 0.2f)).padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(exportResult ?: "", fontSize = 14.sp, color = BrandPurple)
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissClearAll() },
            containerColor = BgSecondary,
            title = { Text("\u786E\u8BA4\u6E05\u7A7A", color = TextPrimary) },
            text = { Text("\u6B64\u64CD\u4F5C\u5C06\u6E05\u9664\u6240\u6709\u56FE\u7247\u3001\u9884\u8BBE\u3001\u65E5\u5FD7\u548C\u8BBE\u7F6E\uFF0C\u4E0D\u53EF\u6062\u590D\uFF01", fontSize = 14.sp, color = TextSecondary) },
            confirmButton = { TextButton(text = "\u786E\u8BA4\u6E05\u7A7A", onClick = { viewModel.clearAllData() }, color = ErrorRed) },
            dismissButton = { TextButton(text = "\u53D6\u6D88", onClick = { viewModel.dismissClearAll() }, color = TextTertiary) }
        )
    }
}

@Composable
fun SettingRow(title: String, value: String, color: Color = TextSecondary, onClick: (() -> Unit)? = null) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth().height(52.dp)
                .background(BgSecondary)
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontSize = 15.sp, color = color)
            if (value.isNotEmpty()) Text(value, fontSize = 14.sp, color = TextTertiary)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(BgTertiary))
    }
}

@Composable
fun SettingSwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().height(52.dp).background(BgSecondary).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontSize = 15.sp, color = TextSecondary)
            Switch(
                checked = checked, onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = BrandPurple, checkedTrackColor = BrandPurple.copy(alpha = 0.3f),
                    uncheckedThumbColor = TextTertiary, uncheckedTrackColor = Divider
                )
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(BgTertiary))
    }
}
