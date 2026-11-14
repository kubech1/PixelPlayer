@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalLayoutApi::class,
    ExperimentalMaterial3Api::class
)

package com.theveloper.pixelplay.presentation.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.MicExternalOn
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Piano
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.theveloper.pixelplay.data.model.Song
import com.theveloper.pixelplay.presentation.components.ImageCropView
import com.theveloper.pixelplay.data.model.PlaylistShapeType
import com.theveloper.pixelplay.data.model.SmartPlaylistRule
// import com.theveloper.pixelplay.presentation.screens.ShapeType // Removed local enum
import com.theveloper.pixelplay.presentation.components.SongPickerSelectionPane
import com.theveloper.pixelplay.ui.theme.GoogleSansRounded
import androidx.compose.material3.Slider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import com.theveloper.pixelplay.utils.shapes.RoundedStarShape
import com.theveloper.pixelplay.utils.resolvePlaylistCoverContentColor
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Matrix
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.TransformOrigin
import com.theveloper.pixelplay.data.model.StorageFilter
import com.theveloper.pixelplay.presentation.viewmodel.PlayerViewModel
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.compose.material3.MediumExtendedFloatingActionButton
import androidx.compose.material3.SliderDefaults


import kotlinx.coroutines.launch
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.presentation.screens.TabAnimation


@Composable
fun CreatePlaylistDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String, String?, Int?, String?, List<String>, Float, Float, Float, String?, Float?, Float?, Float?, Float?, String?) -> Unit
) {
    val transitionState = remember { MutableTransitionState(false) }
    transitionState.targetState = visible

    if (transitionState.currentState || transitionState.targetState) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            AnimatedVisibility(
                visibleState = transitionState,
                enter = slideInVertically(initialOffsetY = { it / 6 }) + fadeIn(animationSpec = tween(220)),
                exit = slideOutVertically(targetOffsetY = { it / 6 }) + fadeOut(animationSpec = tween(200)),
                label = "create_playlist_dialog"
            ) {
                CreatePlaylistContent(
                    onDismiss = onDismiss,
                    onCreate = onCreate
                )
            }
        }
    }
}

// Enum removed, using com.theveloper.pixelplay.data.model.PlaylistShapeType

@Composable
fun EditPlaylistDialog(
    visible: Boolean,
    currentName: String,
    currentImageUri: String?,
    currentColor: Int?,
    currentIconName: String?,
    currentShapeType: PlaylistShapeType?,
    currentShapeDetail1: Float?,
    currentShapeDetail2: Float?,
    currentShapeDetail3: Float?,
    currentShapeDetail4: Float?,
    onDismiss: () -> Unit,
    onSave: (String, String?, Int?, String?, Float, Float, Float, String?, Float?, Float?, Float?, Float?) -> Unit
) {
    val transitionState = remember { MutableTransitionState(false) }
    transitionState.targetState = visible

    if (transitionState.currentState || transitionState.targetState) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            AnimatedVisibility(
                visibleState = transitionState,
                enter = slideInVertically(initialOffsetY = { it / 6 }) + fadeIn(animationSpec = tween(220)),
                exit = slideOutVertically(targetOffsetY = { it / 6 }) + fadeOut(animationSpec = tween(200)),
                label = "edit_playlist_dialog"
            ) {
                 EditPlaylistContent(
                    initialName = currentName,
                    initialImageUri = currentImageUri,
                    initialColor = currentColor,
                    initialIconName = currentIconName,
                    initialShapeType = currentShapeType,
                    initialShapeDetail1 = currentShapeDetail1,
                    initialShapeDetail2 = currentShapeDetail2,
                    initialShapeDetail3 = currentShapeDetail3,
                    initialShapeDetail4 = currentShapeDetail4,
                    onDismiss = onDismiss,
                    onSave = onSave
                )
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun CreatePlaylistContent(
    onDismiss: () -> Unit,
    onCreate: (String, String?, Int?, String?, List<String>, Float, Float, Float, String?, Float?, Float?, Float?, Float?, String?) -> Unit,
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // Shared State
    var playlistName by remember { mutableStateOf("") }
    
    // Step 1: Info State
    var currentStep by remember { mutableStateOf(0) } // 0: Info, 1: Songs
    var selectedTab by remember { mutableStateOf(0) } // 0: Default, 1: Image, 2: Icon
    var creationMode by remember { mutableStateOf(PlaylistCreationMode.MANUAL) }
    var selectedSmartRule by remember { mutableStateOf(SmartPlaylistRule.TOP_PLAYED) }
    
    // Songs State
    val selectedSongIds = remember { mutableStateMapOf<String, Boolean>() }
    
    // Image State
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showCropUi by remember { mutableStateOf(false) }
    // Lifted Bitmap State for Preview Consistency
    var imageBitmap by remember(selectedImageUri) { mutableStateOf<ImageBitmap?>(null) }

    // Crop State
    var cropScale by remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
    var cropOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    // Custom Color/Icon State
    val defaultColor = MaterialTheme.colorScheme.primaryContainer.toArgb()
    var selectedColor by remember { mutableStateOf<Int?>(defaultColor) }
    var selectedIconName by remember { mutableStateOf<String?>("MusicNote") }

    // Shape State
    var selectedShapeType by remember { mutableStateOf(PlaylistShapeType.Circle) }
    
    // SmoothRect Params
    var smoothRectCornerRadius by remember { androidx.compose.runtime.mutableFloatStateOf(20f) } // 0-50
    var smoothRectSmoothness by remember { androidx.compose.runtime.mutableFloatStateOf(60f) } // 0-100
    
    var starCurve by remember { androidx.compose.runtime.mutableDoubleStateOf(0.15) } // 0.0 - 0.5
    var starRotation by remember { androidx.compose.runtime.mutableFloatStateOf(0f) } // 0 - 360
    var starScale by remember { androidx.compose.runtime.mutableFloatStateOf(1f) } // 0.5 - 1.5
    var starSides by remember { androidx.compose.runtime.mutableIntStateOf(5) } // 3 - 20

    LaunchedEffect(selectedImageUri) {
         if (selectedImageUri != null) {
             val loader = ImageLoader(context)
             val request = ImageRequest.Builder(context)
                 .data(selectedImageUri)
                 .allowHardware(false)
                 .build()
             
             val result = loader.execute(request)
             val drawable = result.drawable
             if (drawable is android.graphics.drawable.BitmapDrawable) {
                 imageBitmap = drawable.bitmap.asImageBitmap()
             }
         } else {
             imageBitmap = null
             cropScale = 1f
             cropOffset = androidx.compose.ui.geometry.Offset.Zero
         }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            cropScale = 1f
            cropOffset = androidx.compose.ui.geometry.Offset.Zero
            showCropUi = true
        }
    }
    
    // Back Handler for navigation flow
    BackHandler(enabled = showCropUi || (currentStep == 1 && creationMode == PlaylistCreationMode.MANUAL)) {
        when {
            showCropUi -> showCropUi = false
            currentStep == 1 -> currentStep = 0
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    AnimatedContent(targetState = if (showCropUi) 2 else currentStep, label = "Title Animation") { displayStep ->
                        Text(
                            when (displayStep) {
                                2 -> stringResource(R.string.playlist_creation_edit_cover)
                                0 -> {
                                    if (creationMode == PlaylistCreationMode.SMART) {
                                        stringResource(R.string.playlist_creation_new_smart)
                                    } else {
                                        stringResource(R.string.playlist_creation_new)
                                    }
                                }
                                else -> stringResource(R.string.playlist_creation_add_songs)
                            },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 24.sp,
                                textGeometricTransform = TextGeometricTransform(scaleX = 1.2f),
                            ),
                            fontFamily = GoogleSansRounded,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    FilledIconButton(
                        modifier = Modifier.padding(start = 6.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        onClick = {
                            when {
                                showCropUi -> showCropUi = false
                                currentStep == 1 && creationMode == PlaylistCreationMode.MANUAL -> currentStep = 0
                                else -> onDismiss()
                            }
                        }
                    ) {
                        Icon(
                            if (showCropUi || (currentStep == 1 && creationMode == PlaylistCreationMode.MANUAL)) {
                                Icons.AutoMirrored.Rounded.ArrowBack
                            } else {
                                Icons.Rounded.Close
                            },
                            contentDescription = stringResource(R.string.playlist_creation_cd_back_or_cancel)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        floatingActionButton = {
            if (!showCropUi && !(currentStep == 1 && creationMode == PlaylistCreationMode.MANUAL)) {
                MediumExtendedFloatingActionButton(
                    text = {
                        Text(
                            if (currentStep == 0 && creationMode == PlaylistCreationMode.MANUAL) {
                                stringResource(R.string.playlist_creation_next)
                            } else {
                                stringResource(R.string.playlist_creation_create)
                            }
                        )
                    },
                    icon = { 
                        Icon(
                            if (currentStep == 0 && creationMode == PlaylistCreationMode.MANUAL) {
                                Icons.AutoMirrored.Rounded.ArrowForward
                            } else {
                                Icons.Rounded.Check
                            }, 
                            contentDescription = null
                        ) 
                    },
                    onClick = {
                        if (currentStep == 0) {
                            if (playlistName.isNotBlank()) {
                                if (creationMode == PlaylistCreationMode.MANUAL) {
                                    currentStep = 1
                                } else {
                                    val imageUriString = if(selectedTab == 1) selectedImageUri?.toString() else null
                                    val color = if(selectedTab == 2) selectedColor else null
                                    val icon = if(selectedTab == 2) selectedIconName else null

                                    val scale = if(selectedTab == 1) cropScale else 1f
                                    val panX = if(selectedTab == 1) cropOffset.x else 0f
                                    val panY = if(selectedTab == 1) cropOffset.y else 0f

                                    val shapeTypeForSave = if (selectedTab == 2) selectedShapeType.name else null
                                    val (d1, d2, d3, d4) = if (selectedTab == 2) {
                                        when (selectedShapeType) {
                                            PlaylistShapeType.SmoothRect -> Quadruple(smoothRectCornerRadius, smoothRectSmoothness, 0f, 0f)
                                            PlaylistShapeType.Star -> Quadruple(starCurve.toFloat(), starRotation, starScale, starSides.toFloat())
                                            else -> Quadruple(0f, 0f, 0f, 0f)
                                        }
                                    } else Quadruple(null, null, null, null)

                                    onCreate(
                                        playlistName,
                                        imageUriString,
                                        color,
                                        icon,
                                        emptyList(),
                                        scale,
                                        panX,
                                        panY,
                                        shapeTypeForSave,
                                        d1, d2, d3, d4,
                                        selectedSmartRule.storageKey
                                    )
                                }
                            }
                        } else {
                            val imageUriString = if(selectedTab == 1) selectedImageUri?.toString() else null
                            val color = if(selectedTab == 2) selectedColor else null
                            val icon = if(selectedTab == 2) selectedIconName else null
                            
                            val scale = if(selectedTab == 1) cropScale else 1f
                            val panX = if(selectedTab == 1) cropOffset.x else 0f
                            val panY = if(selectedTab == 1) cropOffset.y else 0f
                            
                            val shapeTypeForSave = if (selectedTab == 2) selectedShapeType.name else null
                            val (d1, d2, d3, d4) = if (selectedTab == 2) {
                                when (selectedShapeType) {
                                    PlaylistShapeType.SmoothRect -> Quadruple(smoothRectCornerRadius, smoothRectSmoothness, 0f, 0f)
                                    PlaylistShapeType.Star -> Quadruple(starCurve.toFloat(), starRotation, starScale, starSides.toFloat())
                                    else -> Quadruple(0f, 0f, 0f, 0f)
                                }
                            } else Quadruple(null, null, null, null)

                            onCreate(
                                playlistName, 
                                imageUriString, 
                                color, 
                                icon, 
                                selectedSongIds.filterValues { it }.keys.toList(),
                                scale,
                                panX,
                                panY,
                                shapeTypeForSave,
                                d1, d2, d3, d4,
                                null
                            )
                        }
                    },
                    expanded = true,
                    shape = CircleShape,
                    modifier = Modifier
                        .padding(bottom = 8.dp, end = 8.dp)
                        .height(56.dp), // Standard height, feels substantial
                    containerColor = if (currentStep == 0 && playlistName.isBlank()) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = if (currentStep == 0 && playlistName.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        },
        bottomBar = {
            if (currentStep == 1 && creationMode == PlaylistCreationMode.MANUAL) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val storageFilter by playerViewModel.playlistPickerStorageFilter.collectAsStateWithLifecycle()
                    val tabs = listOf(
                        StorageFilter.OFFLINE to R.string.library_storage_filter_offline,
                        StorageFilter.ONLINE to R.string.library_storage_filter_online
                    )
                    val selectedTabIndex = tabs.indexOfFirst { it.first == storageFilter }.coerceAtLeast(0)

                    PrimaryTabRow(
                        selectedTabIndex = selectedTabIndex,
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(5.dp),
                        containerColor = Color.Transparent,
                        divider = {},
                        indicator = {}
                    ) {
                        tabs.forEachIndexed { index, (filter, labelRes) ->
                            TabAnimation(
                                index = index,
                                title = stringResource(labelRes),
                                selectedIndex = selectedTabIndex,
                                onClick = { playerViewModel.setPlaylistPickerStorageFilter(filter) },
                                transformOrigin = if (index == 0) TransformOrigin(0f, 0.5f) else TransformOrigin(1f, 0.5f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (filter == StorageFilter.OFFLINE) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_phonef),
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Rounded.Cloud,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(labelRes),
                                        fontFamily = GoogleSansRounded,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    FilledIconButton(
                        onClick = {
                            val imageUriString = if(selectedTab == 1) selectedImageUri?.toString() else null
                            val color = if(selectedTab == 2) selectedColor else null
                            val icon = if(selectedTab == 2) selectedIconName else null
                            
                            val scale = if(selectedTab == 1) cropScale else 1f
                            val panX = if(selectedTab == 1) cropOffset.x else 0f
                            val panY = if(selectedTab == 1) cropOffset.y else 0f
                            
                            val shapeTypeForSave = if (selectedTab == 2) selectedShapeType.name else null
                            val (d1, d2, d3, d4) = if (selectedTab == 2) {
                                when (selectedShapeType) {
                                    PlaylistShapeType.SmoothRect -> Quadruple(smoothRectCornerRadius, smoothRectSmoothness, 0f, 0f)
                                    PlaylistShapeType.Star -> Quadruple(starCurve.toFloat(), starRotation, starScale, starSides.toFloat())
                                    else -> Quadruple(0f, 0f, 0f, 0f)
                                }
                            } else Quadruple(null, null, null, null)

                            onCreate(
                                playlistName, 
                                imageUriString, 
                                color, 
                                icon, 
                                selectedSongIds.filterValues { it }.keys.toList(),
                                scale,
                                panX,
                                panY,
                                shapeTypeForSave,
                                d1, d2, d3, d4,
                                null
                            )
                        },
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    ) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = stringResource(R.string.playlist_creation_create),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> -width } + fadeOut()
                } else {
                    slideInHorizontally { width -> -width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> width } + fadeOut()
                }
            },
            modifier = Modifier.padding(top = paddingValues.calculateTopPadding()),
            label = "Step Transition"
        ) { step ->
            if (step == 0) {
                 PlaylistFormContent(
                     playlistName = playlistName,
                     onNameChange = { playlistName = it },
                     selectedTab = selectedTab,
                     onTabChange = { selectedTab = it },
                     selectedImageUri = selectedImageUri,
                     showCropUi = showCropUi,
                     onShowCropUiChange = { showCropUi = it },
                     cropScale = cropScale,
                     onCropScaleChange = { cropScale = it },
                     cropOffset = cropOffset,
                     onCropOffsetChange = { cropOffset = it },
                     imageBitmap = imageBitmap,
                     imagePickerLauncher = imagePickerLauncher,
                     selectedColor = selectedColor,
                     onColorChange = { selectedColor = it },
                     selectedIconName = selectedIconName,
                     onIconChange = { selectedIconName = it },
                     selectedShapeType = selectedShapeType,
                     onShapeTypeChange = { selectedShapeType = it },
                     smoothRectCornerRadius = smoothRectCornerRadius,
                     onSmoothRectCornerRadiusChange = { smoothRectCornerRadius = it },
                     smoothRectSmoothness = smoothRectSmoothness,
                     onSmoothRectSmoothnessChange = { smoothRectSmoothness = it },
                     starSides = starSides,
                     onStarSidesChange = { starSides = it },
                     starCurve = starCurve,
                     onStarCurveChange = { starCurve = it },
                     starRotation = starRotation,
                     onStarRotationChange = { starRotation = it },
                     starScale = starScale,
                     onStarScaleChange = { starScale = it },
                     creationMode = creationMode,
                     onCreationModeChange = { creationMode = it },
                     selectedSmartRule = selectedSmartRule,
                     onSmartRuleChange = { selectedSmartRule = it },
                     onImageUriChange = { selectedImageUri = it }
                 )
            } else {
                SongPickerSelectionPane(
                    selectedSongIds = selectedSongIds,
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding(),
                    contentPadding = PaddingValues(bottom = 120.dp, top = 8.dp),
                    playerViewModel = playerViewModel
                )
            }
        }
    }
}
