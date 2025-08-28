package com.wyq0918dev.liquidglassdemo

import android.os.Build
import androidx.compose.animation.Animatable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.liquidglass.GlassStyle
import com.kyant.liquidglass.LiquidGlassProviderState
import com.kyant.liquidglass.liquidGlass
import com.kyant.liquidglass.liquidGlassProvider
import com.kyant.liquidglass.material.GlassMaterial
import com.kyant.liquidglass.refraction.InnerRefraction
import com.kyant.liquidglass.refraction.RefractionAmount
import com.kyant.liquidglass.refraction.RefractionHeight
import com.kyant.liquidglass.rememberLiquidGlassProviderState
import com.kyant.liquidglass.sampler.ContinuousLuminanceSampler
import com.kyant.liquidglass.sampler.ExperimentalLuminanceSamplerApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow

data class NavigationItem(
    val label: String,
    val icon: ImageVector,
)

@OptIn(ExperimentalLuminanceSamplerApi::class, ExperimentalLuminanceSamplerApi::class)
@Composable
fun LiquidGlassNavigationBar(
    modifier: Modifier = Modifier,
    liquidGlassProviderState: LiquidGlassProviderState,
    useMaterial: Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU,
    tabs: List<NavigationItem>,
    selectedIndexState: MutableState<Int>,
    onTabSelected: (index: Int) -> Unit,
) {
    val bottomTabsLiquidGlassProviderState = rememberLiquidGlassProviderState(
        backgroundColor = null
    )
    val animationScope = rememberCoroutineScope()
    val initialContentColor = MaterialTheme.colorScheme.onSurface
    val contentColor = remember {
        Animatable(initialValue = initialContentColor)
    }
    val luminanceSampler = remember {
        ContinuousLuminanceSampler { _, luminance ->
            val isLight = luminance.pow(2f) > 0.5f
            animationScope.launch {
                contentColor.animateTo(
                    targetValue = if (isLight) {
                        Color.Black
                    } else {
                        Color.White
                    },
                    animationSpec = tween(
                        durationMillis = 300,
                        delayMillis = 0,
                        easing = LinearEasing,
                    ),
                )
            }
        }
    }

    val density = LocalDensity.current
    var isDragging by remember { mutableStateOf(false) }
    val offset = remember { Animatable(0f) }
    val padding = 4.dp
    val paddingPx = with(density) { padding.roundToPx() }

    val localContentColor: ProvidableCompositionLocal<Color> =
        compositionLocalOf { Color.Unspecified }

    val bottomBarColor = if (useMaterial) {
        NavigationBarDefaults.containerColor
    } else {
        Color.Transparent
    }

    NavigationBar(
        modifier = modifier,
        containerColor = bottomBarColor,
    ) {
        when {
            useMaterial -> tabs.forEachIndexed { index, tab ->
                NavigationBarItem(
                    selected = selectedIndexState.value == index,
                    onClick = {
                        selectedIndexState.value = index
                        onTabSelected(index)
                    },
                    icon = {
                        Image(
                            imageVector = tab.icon,
                            contentDescription = null,
                        )
                    },
                    label = {
                        Text(text = tab.label)
                    },
                    alwaysShowLabel = false
                )
            }

            else -> Row(
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(space = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(weight = 1f)
                        .height(height = 64.dp)
                        .fillMaxWidth()
                        .pointerInput(key1 = Unit) {
                            detectTapGestures {}
                        },
                ) {
                    val widthWithoutPaddings =
                        (constraints.maxWidth.toFloat() - paddingPx * 2f).fastCoerceAtLeast(
                            minimumValue = 0f
                        )
                    val tabWidth = if (tabs.isEmpty()) 0f else widthWithoutPaddings / tabs.size
                    val maxWidth =
                        (widthWithoutPaddings - tabWidth).fastCoerceAtLeast(minimumValue = 0f)

                    LaunchedEffect(
                        key1 = selectedIndexState.value,
                        key2 = tabWidth,
                        key3 = isDragging
                    ) {
                        if (tabWidth > 0 && !isDragging) {
                            offset.animateTo(
                                targetValue = (selectedIndexState.value * tabWidth).fastCoerceIn(
                                    0f, maxWidth
                                ),
                                animationSpec = SpringSpec(
                                    dampingRatio = 0.8f,
                                    stiffness = 380f,
                                ),
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .liquidGlassProvider(
                                state = bottomTabsLiquidGlassProviderState,
                            )
                            .liquidGlass(
                                state = liquidGlassProviderState,
                                luminanceSampler = luminanceSampler,
                            ) {
                                val luminance = luminanceSampler.luminance.pow(x = 2f)
                                GlassStyle(
                                    shape = RoundedCornerShape(percent = 50),
                                    innerRefraction = InnerRefraction(
                                        height = RefractionHeight(value = 12.dp),
                                        amount = RefractionAmount.Half
                                    ),
                                    material = if (luminance > 0.5f) {
                                        GlassMaterial(
                                            brush = SolidColor(value = Color.White),
                                            alpha = (luminance - 0.5f) * 2f * 0.8f
                                        )
                                    } else {
                                        GlassMaterial(
                                            brush = SolidColor(value = Color.Black),
                                            alpha = (0.5f - luminance) * 2f * 0.3f
                                        )
                                    },
                                )
                            }
                            .fillMaxSize()
                            .padding(all = padding),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CompositionLocalProvider(
                            value = localContentColor provides contentColor.value
                        ) {
                            tabs.forEachIndexed { index, tab ->
                                key(tab) {
                                    val itemBackgroundAlpha by animateFloatAsState(
                                        targetValue = if (selectedIndexState.value == index && !isDragging) {
                                            0.8f
                                        } else {
                                            0f
                                        },
                                        animationSpec = spring(
                                            dampingRatio = 0.8f,
                                            stiffness = 200f
                                        )
                                    )
                                    val itemContentColor by animateColorAsState(
                                        targetValue = if (selectedIndexState.value == index && !isDragging) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            contentColor.value
                                        },
                                        animationSpec = spring(
                                            dampingRatio = 0.8f,
                                            stiffness = 200f
                                        )
                                    )
                                    val itemContainerColor =
                                        MaterialTheme.colorScheme.primaryContainer
                                    Column(
                                        modifier = Modifier
                                            .clip(shape = RoundedCornerShape(percent = 50))
                                            .drawBehind {
                                                drawRect(
                                                    color = itemContainerColor,
                                                    alpha = itemBackgroundAlpha,
                                                )
                                            }
                                            .pointerInput(key1 = Unit) {
                                                detectTapGestures {
                                                    if (selectedIndexState.value != index) {
                                                        selectedIndexState.value = index
                                                        onTabSelected(index)
                                                        animationScope.launch {
                                                            launch {
                                                                offset.animateTo(
                                                                    targetValue = (index * tabWidth).fastCoerceIn(
                                                                        0f, maxWidth
                                                                    ),
                                                                    animationSpec = spring(
                                                                        dampingRatio = 0.8f,
                                                                        stiffness = 200f,
                                                                    )
                                                                )
                                                            }
                                                            launch {
                                                                isDragging = true
                                                                delay(timeMillis = 200)
                                                                isDragging = false
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            .weight(weight = 1f)
                                            .height(height = 56.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(
                                            space = 2.dp,
                                            alignment = Alignment.CenterVertically
                                        ),
                                    ) {
                                        Image(
                                            modifier = Modifier.size(size = 24.dp),
                                            imageVector = tab.icon,
                                            contentDescription = null,
                                            colorFilter = ColorFilter.tint(
                                                color = itemContentColor
                                            ),
                                        )
                                        Text(
                                            text = tab.label,
                                            color = itemContentColor
                                        )
                                    }
                                }
                            }
                        }
                    }

                    val scaleXFraction: Float by animateFloatAsState(
                        targetValue = if (!isDragging) 0f else 1f,
                        animationSpec = spring(
                            dampingRatio = 0.5f,
                            stiffness = 300f,
                        ),
                    )
                    val scaleYFraction: Float by animateFloatAsState(
                        targetValue = if (!isDragging) 0f else 1f,
                        animationSpec = spring(
                            dampingRatio = 0.5f,
                            stiffness = 600f,
                        ),
                    )

                    Spacer(
                        modifier = Modifier
                            .layout { measurable, constraints ->
                                val width = tabWidth.fastRoundToInt()
                                val height = 56.dp.roundToPx()
                                val placeable = measurable.measure(
                                    Constraints.fixed(
                                        width = (width * lerp(
                                            start = 1f,
                                            stop = 1.5f,
                                            fraction = scaleXFraction,
                                        )).fastRoundToInt(),
                                        height = (height * lerp(
                                            start = 1f,
                                            stop = 1.5f,
                                            fraction = scaleYFraction,
                                        )).fastRoundToInt(),
                                    ),
                                )
                                layout(width, height) {
                                    placeable.place(
                                        x = (width - placeable.width) / 2 + paddingPx,
                                        y = (height - placeable.height) / 2 + paddingPx
                                    )
                                }
                            }
                            .drawWithContent {
                                translate(
                                    left = 0f,
                                    top = lerp(
                                        start = 0f,
                                        stop = 4f,
                                        fraction = scaleYFraction,
                                    ).dp.toPx()
                                ) {
                                    this@drawWithContent.drawContent()
                                }
                            }
                            .graphicsLayer {
                                translationX = offset.value
                                scaleX = lerp(
                                    start = 1f,
                                    stop = 0.9f,
                                    fraction = scaleXFraction,
                                )
                                scaleY = lerp(
                                    start = 1f,
                                    stop = 0.9f,
                                    fraction = scaleYFraction,
                                )
                                transformOrigin = TransformOrigin(
                                    pivotFractionX = 0f,
                                    pivotFractionY = 0f,
                                )
                            }
                            .background(
                                color = Color.Transparent,
                                shape = RoundedCornerShape(percent = 50),
                            )
                            .liquidGlass(
                                bottomTabsLiquidGlassProviderState, GlassStyle(
                                    RoundedCornerShape(50), innerRefraction = InnerRefraction(
                                        height = RefractionHeight(
                                            animateFloatAsState(
                                                if (!isDragging) 0f else 10f
                                            ).value.dp
                                        ), amount = RefractionAmount.Half
                                    ), material = GlassMaterial.None
                                )
                            )
                            .draggable(
                                rememberDraggableState { delta ->
                                    animationScope.launch {
                                        offset.snapTo(
                                            targetValue = (offset.value + delta).fastCoerceIn(
                                                0f,
                                                maxWidth
                                            )
                                        )
                                    }
                                },
                                Orientation.Horizontal,
                                startDragImmediately = true,
                                onDragStarted = { isDragging = true },
                                onDragStopped = { velocity ->
                                    isDragging = false
                                    val currentIndex = offset.value / tabWidth
                                    val targetIndex = when {
                                        velocity > 0f -> ceil(currentIndex).toInt()
                                        velocity < 0f -> floor(currentIndex).toInt()
                                        else -> currentIndex.fastRoundToInt()
                                    }.fastCoerceIn(0, tabs.lastIndex)

                                    if (selectedIndexState.value != targetIndex) {
                                        selectedIndexState.value = targetIndex
                                        onTabSelected(targetIndex)
                                    }

                                    animationScope.launch {
                                        offset.animateTo(
                                            (targetIndex * tabWidth).fastCoerceIn(0f, maxWidth),
                                            spring(0.8f, 200f)
                                        )
                                    }
                                },
                            )
                    )
                }
            }
        }
    }
}
