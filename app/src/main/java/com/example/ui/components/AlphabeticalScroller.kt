package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.CustomerEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val ALPHABETS = listOf(
    'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
    'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
    'U', 'V', 'W', 'X', 'Y', 'Z', '#'
)

/**
 * Alphabetical Fast-Scroller Sidebar with interactive touch/drag popup preview.
 * Tapping or dragging along the alphabet bar quickly scrolls the customer list
 * to the corresponding initial letter and displays a prominent visual bubble popup.
 */
@Composable
fun AlphabeticalFastScroller(
    customers: List<CustomerEntity>,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    onLetterSelected: ((Char) -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedLetter by remember { mutableStateOf<Char?>(null) }
    var isTouching by remember { mutableStateOf(false) }
    var dismissJob by remember { mutableStateOf<Job?>(null) }
    var columnHeightPx by remember { mutableFloatStateOf(1f) }
    var touchYRatio by remember { mutableFloatStateOf(0.5f) }

    // Map existing letters in customer list
    val existingLetters = remember(customers) {
        customers.mapNotNull { customer ->
            val firstChar = customer.name.trim().firstOrNull()?.uppercaseChar()
            when {
                firstChar == null -> null
                firstChar in 'A'..'Z' -> firstChar
                else -> '#'
            }
        }.toSet()
    }

    // Function to handle letter selection and scroll list
    fun selectLetterByIndex(index: Int) {
        val clampedIndex = index.coerceIn(0, ALPHABETS.lastIndex)
        val letter = ALPHABETS[clampedIndex]
        selectedLetter = letter
        isTouching = true
        touchYRatio = (clampedIndex.toFloat() / ALPHABETS.size.toFloat()).coerceIn(0.05f, 0.95f)

        dismissJob?.cancel()

        onLetterSelected?.invoke(letter)

        // Find matching customer index in list
        val targetIndex = if (letter == '#') {
            customers.indexOfFirst { customer ->
                val firstChar = customer.name.trim().firstOrNull()?.uppercaseChar()
                firstChar != null && firstChar !in 'A'..'Z'
            }
        } else {
            // Find first customer with this letter or nearest subsequent letter
            val exactIndex = customers.indexOfFirst { customer ->
                customer.name.trim().startsWith(letter, ignoreCase = true)
            }
            if (exactIndex >= 0) {
                exactIndex
            } else {
                // Find next alphabetical letter present
                val nextLetter = ALPHABETS.filter { it in 'A'..'Z' && it > letter }
                    .firstOrNull { it in existingLetters }
                if (nextLetter != null) {
                    customers.indexOfFirst { customer ->
                        customer.name.trim().startsWith(nextLetter, ignoreCase = true)
                    }
                } else {
                    -1
                }
            }
        }

        if (targetIndex >= 0) {
            coroutineScope.launch {
                listState.scrollToItem(targetIndex)
            }
        }
    }

    fun handleTouchPosition(yPx: Float) {
        if (columnHeightPx > 0) {
            val ratio = (yPx / columnHeightPx).coerceIn(0f, 1f)
            val index = (ratio * ALPHABETS.size).toInt().coerceIn(0, ALPHABETS.lastIndex)
            selectLetterByIndex(index)
        }
    }

    fun endTouch() {
        dismissJob?.cancel()
        dismissJob = coroutineScope.launch {
            delay(750)
            isTouching = false
            selectedLetter = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(end = 2.dp, top = 8.dp, bottom = 80.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        // 1. Floating Selected Letter Popup Bubble
        AnimatedVisibility(
            visible = isTouching && selectedLetter != null,
            enter = fadeIn() + scaleIn(spring(dampingRatio = 0.65f)),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-42).dp)
        ) {
            Surface(
                modifier = Modifier
                    .size(60.dp)
                    .shadow(10.dp, shape = CircleShape)
                    .testTag("alphabet_bubble_popup"),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                tonalElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = selectedLetter?.toString() ?: "",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 2. Alphabet Bar Column Strip
        Surface(
            modifier = Modifier
                .width(22.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
                .onGloballyPositioned { coordinates ->
                    columnHeightPx = coordinates.size.height.toFloat()
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { offset ->
                            handleTouchPosition(offset.y)
                            tryAwaitRelease()
                            endTouch()
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            handleTouchPosition(offset.y)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            handleTouchPosition(change.position.y)
                        },
                        onDragEnd = {
                            endTouch()
                        },
                        onDragCancel = {
                            endTouch()
                        }
                    )
                }
                .testTag("alphabet_fast_scroller_strip"),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ALPHABETS.forEach { char ->
                    val isPresent = char in existingLetters
                    val isSelected = selectedLetter == char

                    val textColor = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        isPresent -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                    }

                    val fontWeight = when {
                        isSelected -> FontWeight.ExtraBold
                        isPresent -> FontWeight.Bold
                        else -> FontWeight.Normal
                    }

                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                else Color.Transparent
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char.toString(),
                            fontSize = if (isSelected) 10.sp else 8.5.sp,
                            fontWeight = fontWeight,
                            color = textColor,
                            textAlign = TextAlign.Center,
                            lineHeight = 10.sp
                        )
                    }
                }
            }
        }
    }
}
