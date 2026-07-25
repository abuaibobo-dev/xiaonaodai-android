package com.meitu.generator.ui.gallery

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.meitu.generator.data.local.entity.ImageEntity
import com.meitu.generator.ui.theme.*
import java.io.File

@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel = hiltViewModel()
) {
    val filter by viewModel.filter.collectAsState()
    val images by viewModel.images.collectAsState()
    val total by viewModel.totalCount.collectAsState()
    val today by viewModel.todayCount.collectAsState()
    val favCount by viewModel.favoriteCount.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectionMode by viewModel.selectionMode.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val previewImage by viewModel.previewImage.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(BgPrimary)) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("图库", fontSize = 20.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text("共${total}张 今日${today}张 收藏${favCount}张", fontSize = 12.sp, color = TextTertiary)
                }
                if (selectionMode) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GalleryTextBtn("收藏", BrandCyan) { viewModel.batchFavorite() }
                        GalleryTextBtn("删除", ErrorRed) { viewModel.batchDelete() }
                        GalleryTextBtn("导出", BrandPurple) { }
                        GalleryTextBtn("取消", TextTertiary) { viewModel.toggleSelectionMode() }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                listOf(
                    GalleryFilter.ALL to "全部",
                    GalleryFilter.FAVORITE to "收藏",
                    GalleryFilter.TODAY to "今日",
                    GalleryFilter.MONTH to "本月"
                ).forEach { (f, label) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { viewModel.setFilter(f) }
                    ) {
                        Text(
                            text = label, fontSize = 14.sp,
                            color = if (filter == f) TextPrimary else TextTertiary,
                            fontWeight = if (filter == f) FontWeight.Medium else FontWeight.Normal
                        )
                        Spacer(Modifier.height(2.dp))
                        Box(
                            Modifier.width(20.dp).height(2.dp)
                                .background(if (filter == f) BrandPurple else Color.Transparent)
                        )
                    }
                }
            }
        }

        if (images.isEmpty() && !isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Image, null, tint = TextTertiary, modifier = Modifier.size(60.dp))
                    Text("暂无图片", fontSize = 14.sp, color = TextTertiary)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(images.size) { index ->
                    val img = images[index]
                    val isSelected = img.id in selectedIds
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BgSecondary)
                            .then(if (isSelected) Modifier.border(2.dp, BrandPurple, RoundedCornerShape(8.dp)) else Modifier)
                            .clickable {
                                if (selectionMode) viewModel.toggleSelection(img.id)
                                else viewModel.setPreviewImage(img)
                            }
                            .pointerInput(Unit) {
                                detectTapGestures(onLongPress = {
                                    if (!selectionMode) viewModel.toggleSelectionMode()
                                    viewModel.toggleSelection(img.id)
                                })
                            }
                    ) {
                        if (img.localPath.isNotEmpty() && File(img.localPath).exists()) {
                            AsyncImage(
                                model = File(img.localPath), contentDescription = null,
                                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                            )
                        }
                        Box(modifier = Modifier.align(Alignment.TopStart).padding(4.dp)) {
                            Icon(
                                Icons.Default.Favorite, null,
                                tint = if (img.isFavorite) ErrorRed else TextTertiary.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp).clickable { viewModel.toggleFavorite(img) }
                            )
                        }
                        Box(
                            modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(BgPrimary.copy(alpha = 0.8f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (img.imgbbUrl.isNotEmpty()) "C" else "L",
                                fontSize = 10.sp,
                                color = if (img.imgbbUrl.isNotEmpty()) BrandCyan else TextTertiary
                            )
                        }
                    }
                }
            }
        }
    }

    previewImage?.let { img ->
        Dialog(onDismissRequest = { viewModel.setPreviewImage(null) }) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)).background(BgSecondary)
            ) {
                if (img.localPath.isNotEmpty() && File(img.localPath).exists()) {
                    AsyncImage(
                        model = File(img.localPath), contentDescription = null,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(img.prompt, fontSize = 13.sp, color = TextSecondary, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        GalleryTextBtn("收藏", if (img.isFavorite) ErrorRed else BrandCyan) { viewModel.toggleFavorite(img) }
                        GalleryTextBtn("删除", ErrorRed) { viewModel.deleteImage(img); viewModel.setPreviewImage(null) }
                    }
                }
            }
        }
    }
}

@Composable
fun GalleryTextBtn(text: String, color: Color, onClick: () -> Unit) {
    Text(text = text, fontSize = 14.sp, color = color, modifier = Modifier.clickable(onClick = onClick))
}
