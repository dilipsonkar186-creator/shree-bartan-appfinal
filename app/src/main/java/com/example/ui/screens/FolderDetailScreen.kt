package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AiAssistantBottomSheet
import com.example.ui.viewmodel.AiAssistantViewModel
import com.example.ui.viewmodel.AiAssistantViewModelFactory
import com.example.util.NameMatcher
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import java.util.Calendar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.CustomerEntity
import com.example.ui.components.AddEditCustomerDialog
import com.example.ui.components.AlphabeticalFastScroller
import com.example.ui.components.CustomerCard
import com.example.ui.components.EditShopProfileDialog
import com.example.ui.components.PublishAnnouncementDialog
import androidx.compose.material.icons.filled.Campaign
import com.example.ui.components.ShopProfileBanner
import com.example.ui.components.UpiQrGeneratorDialog
import com.example.ui.viewmodel.CustomerPaymentFilterOption
import com.example.ui.viewmodel.CustomerSortOption
import com.example.ui.viewmodel.FolderViewModel
import com.example.util.CsvExportUtils
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailScreen(
    folderId: Long,
    viewModel: FolderViewModel,
    onBack: () -> Unit,
    onCustomerClick: (Long) -> Unit = {},
    onAddCustomerClick: (Long) -> Unit = {},
    onEditCustomerClick: (Long, Long) -> Unit = { _, _ -> },
    onOpenFinancialSummary: () -> Unit,
    onOpenRecycleBin: () -> Unit = {}
) {
    val context = LocalContext.current

    LaunchedEffect(folderId) {
        viewModel.selectFolder(folderId)
    }

    val currentFolder by viewModel.currentFolder.collectAsStateWithLifecycle()
    val customers by viewModel.customersInCurrentFolder.collectAsStateWithLifecycle()
    val searchQuery by viewModel.customerSearchQuery.collectAsStateWithLifecycle()
    val shopProfile by viewModel.shopProfile.collectAsStateWithLifecycle()

    val currentSortOption by viewModel.customerSortOption.collectAsStateWithLifecycle()
    val currentFilterOption by viewModel.customerPaymentFilter.collectAsStateWithLifecycle()
    val currentBookFilter by viewModel.customerBookFilter.collectAsStateWithLifecycle()
    val availableBookNumbers by viewModel.availableBookNumbers.collectAsStateWithLifecycle()
    val currentFilterMonth by viewModel.customerFilterMonth.collectAsStateWithLifecycle()
    val currentFilterYear by viewModel.customerFilterYear.collectAsStateWithLifecycle()
    val customerFinancialsMap by viewModel.customerFinancialsMap.collectAsStateWithLifecycle()
    val pullDownSyncEnabled by viewModel.pullDownSyncEnabled.collectAsStateWithLifecycle()

    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var customerToEdit by remember { mutableStateOf<CustomerEntity?>(null) }
    var customerToDelete by remember { mutableStateOf<CustomerEntity?>(null) }
    var showTopMenu by remember { mutableStateOf(false) }
    var showEditShopDialog by remember { mutableStateOf(false) }
    var showAnnouncementDialog by remember { mutableStateOf(false) }
    var showAiAssistantBottomSheet by remember { mutableStateOf(false) }
    var showUpiQrDialog by remember { mutableStateOf(false) }
    var showQuickToolsPopup by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var isListeningByVoice by remember { mutableStateOf(false) }
    var isPullRefreshing by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val customerListState = rememberLazyListState()

    // Auto-scroll to the top immediately when search query updates so the matched customer is right in front of user
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            customerListState.scrollToItem(0)
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListeningByVoice = false
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val rawSpokenText = matches?.firstOrNull { it.isNotBlank() }
            if (!rawSpokenText.isNullOrBlank()) {
                isSearchActive = true

                // Convert to clean English if speech engine ever returned Devanagari
                val spokenTextInEnglish = if (NameMatcher.containsDevanagari(rawSpokenText)) {
                    NameMatcher.transliterateDevanagariToLatin(rawSpokenText, mode = 1)
                } else {
                    rawSpokenText
                }.trim()

                val allFolderCust = viewModel.allCustomers.value.filter { it.folderId == folderId && !it.isDeleted }
                val bestMatch = (matches?.firstNotNullOfOrNull { m -> NameMatcher.findBestMatch(m, allFolderCust) })
                    ?: NameMatcher.findBestMatch(spokenTextInEnglish, allFolderCust)

                val queryToSet = if (bestMatch != null && bestMatch.score >= 60) {
                    bestMatch.customer.name
                } else {
                    spokenTextInEnglish
                }

                viewModel.setCustomerSearchQuery(queryToSet)
                keyboardController?.hide()

                if (bestMatch != null) {
                    Toast.makeText(context, "Search: \"${bestMatch.customer.name}\"", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Search: \"$spokenTextInEnglish\"", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun startCustomerVoiceSearch() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-IN")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak customer name in English (e.g. Ritesh Sonkar, Dilip)...")
        }
        try {
            isListeningByVoice = true
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            isListeningByVoice = false
            Toast.makeText(context, "Voice Search could not be started", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            delay(100)
            try {
                searchFocusRequester.requestFocus()
                keyboardController?.show()
            } catch (e: Exception) {
                // Focus fallback
            }
        }
    }

    val aiViewModel: AiAssistantViewModel = viewModel(
        factory = AiAssistantViewModelFactory(viewModel.repository)
    )

    val folderColor = remember(currentFolder?.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(currentFolder?.colorHex ?: "#4F46E5"))
        } catch (e: Exception) {
            Color(0xFF4F46E5)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (isSearchActive) {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setCustomerSearchQuery(it) },
                            placeholder = { Text("Search by name or phone...") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            shape = RoundedCornerShape(24.dp),
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.setCustomerSearchQuery("") }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                        }
                                    }
                                    IconButton(
                                        onClick = { startCustomerVoiceSearch() },
                                        modifier = Modifier.testTag("customer_voice_search_mic_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Mic,
                                            contentDescription = "Voice Search Customers",
                                            tint = if (isListeningByVoice) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(searchFocusRequester)
                                .testTag("customer_search_input")
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                isSearchActive = false
                                viewModel.setCustomerSearchQuery("")
                            },
                            modifier = Modifier.testTag("close_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Close search"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            } else {
                TopAppBar(
                    title = {
                        Column(modifier = Modifier.padding(start = 2.dp)) {
                            Text(
                                text = currentFolder?.name ?: "Folder Details",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${customers.size} Customer${if (customers.size != 1) "s" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Folders"
                            )
                        }
                    },
                    actions = {
                        // Single unified Quick Tools popup button with anchor
                        Box {
                            IconButton(
                                onClick = { showQuickToolsPopup = !showQuickToolsPopup },
                                modifier = Modifier.testTag("folder_quick_tools_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GridView,
                                    contentDescription = "Quick Tools & Actions",
                                    tint = if (showQuickToolsPopup) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Horizontal Floating Glass Pill Popup
                            if (showQuickToolsPopup) {
                                Popup(
                                    alignment = Alignment.TopEnd,
                                    offset = IntOffset(x = 48, y = 125),
                                    onDismissRequest = { showQuickToolsPopup = false },
                                    properties = PopupProperties(focusable = true, dismissOnClickOutside = true)
                                ) {
                                    FloatingQuickToolsPillBar(
                                        onExportCsvClick = {
                                            showQuickToolsPopup = false
                                            val folderName = currentFolder?.name ?: "Folder"
                                            CsvExportUtils.exportAndShareCsv(context, folderName, customers)
                                        },
                                        onSearchClick = {
                                            showQuickToolsPopup = false
                                            isSearchActive = true
                                        },
                                        onQrCodeClick = {
                                            showQuickToolsPopup = false
                                            showUpiQrDialog = true
                                        },
                                        onAiAssistantClick = {
                                            showQuickToolsPopup = false
                                            showAiAssistantBottomSheet = true
                                        },
                                        onVoiceSearchClick = {
                                            showQuickToolsPopup = false
                                            startCustomerVoiceSearch()
                                        }
                                    )
                                }
                            }
                        }

                        // Top Right Menu Bar
                        Box {
                            IconButton(
                                onClick = { showTopMenu = true },
                                modifier = Modifier.testTag("folder_detail_menu_button")
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Menu options")
                            }

                            DropdownMenu(
                                expanded = showTopMenu,
                                onDismissRequest = {
                                    showTopMenu = false
                                }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Export Customers to CSV") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.FileDownload,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    onClick = {
                                        showTopMenu = false
                                        val folderName = currentFolder?.name ?: "Folder"
                                        CsvExportUtils.exportAndShareCsv(context, folderName, customers)
                                    },
                                    modifier = Modifier.testTag("menu_export_csv")
                                )

                                DropdownMenuItem(
                                    text = { Text("Financial Summary & Reports") },
                                    leadingIcon = { Icon(Icons.Default.Assessment, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        showTopMenu = false
                                        onOpenFinancialSummary()
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Recycle Bin") },
                                    leadingIcon = { Icon(Icons.Default.Recycling, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showTopMenu = false
                                        onOpenRecycleBin()
                                    }
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                DropdownMenuItem(
                                    text = { Text("शॉप प्रोफाइल व लाइव नोटिस (Shop Profile & Notice)") },
                                    leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        showTopMenu = false
                                        showEditShopDialog = true
                                    },
                                    modifier = Modifier.testTag("menu_shop_profile_notice")
                                )

                                DropdownMenuItem(
                                    text = { Text("ग्राहकों को बधाई / ऑफर भेजें (Announcement)") },
                                    leadingIcon = { Icon(Icons.Default.Campaign, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        showTopMenu = false
                                        showAnnouncementDialog = true
                                    },
                                    modifier = Modifier.testTag("menu_publish_announcement")
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { startCustomerVoiceSearch() },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice Search Customer",
                        modifier = Modifier.size(24.dp)
                    )
                },
                text = {
                    Text(
                        text = if (isListeningByVoice) "Listening..." else "Voice Search",
                        fontWeight = FontWeight.Bold
                    )
                },
                containerColor = if (isListeningByVoice) MaterialTheme.colorScheme.error else Color(0xFF1B5E20),
                contentColor = Color.White,
                modifier = Modifier.testTag("voice_search_fab")
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        val folderDetailContent: @Composable () -> Unit = {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Customer Sort & Monthly Filter Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Month Selector Row
                val currentCalMonth = remember { Calendar.getInstance().get(Calendar.MONTH) }
                val currentCalYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
                val monthNames = remember { arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec") }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = currentFilterMonth == currentCalMonth && currentFilterYear == currentCalYear,
                        onClick = {
                            viewModel.setCustomerFilterMonth(currentCalMonth)
                            viewModel.setCustomerFilterYear(currentCalYear)
                        },
                        label = { Text("This Month (${monthNames[currentCalMonth]})") },
                        leadingIcon = {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        modifier = Modifier.testTag("month_chip_this_month")
                    )

                    listOf(1, 2, 3).forEach { i ->
                        val pastCal = Calendar.getInstance().apply { add(Calendar.MONTH, -i) }
                        val pMonth = pastCal.get(Calendar.MONTH)
                        val pYear = pastCal.get(Calendar.YEAR)

                        FilterChip(
                            selected = currentFilterMonth == pMonth && currentFilterYear == pYear,
                            onClick = {
                                viewModel.setCustomerFilterMonth(pMonth)
                                viewModel.setCustomerFilterYear(pYear)
                            },
                            label = { Text("${monthNames[pMonth]} $pYear") },
                            modifier = Modifier.testTag("month_chip_prev_$i")
                        )
                    }

                    FilterChip(
                        selected = currentFilterMonth == -1,
                        onClick = { viewModel.setCustomerFilterMonth(-1) },
                        label = { Text("All Months") },
                        modifier = Modifier.testTag("month_chip_all")
                    )

                    Box {
                        var showMonthMenu by remember { mutableStateOf(false) }

                        AssistChip(
                            onClick = { showMonthMenu = true },
                            label = { Text("Month Picker ▾") },
                            leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            modifier = Modifier.testTag("month_picker_chip")
                        )

                        DropdownMenu(
                            expanded = showMonthMenu,
                            onDismissRequest = { showMonthMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Months", fontWeight = FontWeight.Bold) },
                                onClick = {
                                    viewModel.setCustomerFilterMonth(-1)
                                    showMonthMenu = false
                                }
                            )
                            HorizontalDivider()
                            monthNames.forEachIndexed { idx, mName ->
                                val isSel = currentFilterMonth == idx
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "$mName $currentCalYear${if (idx == currentCalMonth) " (This Month)" else ""}",
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    leadingIcon = if (isSel) { { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) } } else null,
                                    onClick = {
                                        viewModel.setCustomerFilterMonth(idx)
                                        viewModel.setCustomerFilterYear(currentCalYear)
                                        showMonthMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                // 3. Filter & Sort Dropdowns Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Payment Filter Dropdown Menu
                    Box {
                        var showFilterMenu by remember { mutableStateOf(false) }

                        AssistChip(
                            onClick = { showFilterMenu = true },
                            label = { Text("Filter: ${currentFilterOption.label}") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (currentFilterOption != CustomerPaymentFilterOption.ALL)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                                labelColor = if (currentFilterOption != CustomerPaymentFilterOption.ALL)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.testTag("customer_filter_chip")
                        )

                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false }
                        ) {
                            CustomerPaymentFilterOption.values().forEach { option ->
                                val isSelected = option == currentFilterOption
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option.label,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) }
                                    } else null,
                                    onClick = {
                                        viewModel.setCustomerPaymentFilter(option)
                                        showFilterMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Book Filter Dropdown Menu
                    Box {
                        var showBookMenu by remember { mutableStateOf(false) }
                        val isBookFiltered = !currentBookFilter.isNullOrBlank()

                        AssistChip(
                            onClick = { showBookMenu = true },
                            label = {
                                Text(
                                    text = if (currentBookFilter.isNullOrBlank()) "Book: All"
                                    else if (currentBookFilter == "__NO_BOOK__") "Book: None"
                                    else "Book: $currentBookFilter"
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (isBookFiltered)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                                labelColor = if (isBookFiltered)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.testTag("customer_book_filter_chip")
                        )

                        DropdownMenu(
                            expanded = showBookMenu,
                            onDismissRequest = { showBookMenu = false }
                        ) {
                            val isAllSelected = currentBookFilter.isNullOrBlank()
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "All Books",
                                        fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                leadingIcon = if (isAllSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) }
                                } else null,
                                onClick = {
                                    viewModel.setCustomerBookFilter(null)
                                    showBookMenu = false
                                }
                            )

                            availableBookNumbers.forEach { book ->
                                val isSelected = currentBookFilter == book
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Book $book",
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) }
                                    } else null,
                                    onClick = {
                                        viewModel.setCustomerBookFilter(book)
                                        showBookMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Sort Dropdown Menu
                    Box {
                        var showSortMenu by remember { mutableStateOf(false) }

                        AssistChip(
                            onClick = { showSortMenu = true },
                            label = { Text("Sort: ${currentSortOption.label}") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (currentSortOption != CustomerSortOption.NAME_ASC)
                                    MaterialTheme.colorScheme.secondaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                                labelColor = if (currentSortOption != CustomerSortOption.NAME_ASC)
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.testTag("customer_sort_chip")
                        )

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            CustomerSortOption.values().forEach { option ->
                                val isSelected = option == currentSortOption
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option.label,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) }
                                    } else null,
                                    onClick = {
                                        viewModel.setCustomerSortOption(option)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                // 4. Customer Count & Add Customer Button Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${customers.size} Customer(s)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = { onAddCustomerClick(folderId) },
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("month_bar_add_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Add Customer",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Add Customer",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            // Customer List or Empty State
            if (customers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        Text(
                            text = if (searchQuery.isNotBlank())
                                "No customers match your search"
                            else
                                "No Customers in this Folder",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = if (searchQuery.isNotBlank())
                                "Try adjusting your search query or filter options."
                            else
                                "Tap the '+' button above to add customers to this folder.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                val monthNames = remember { arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec") }
                val currentMonthName = if (currentFilterMonth < 0) "All Months" else monthNames.getOrElse(currentFilterMonth) { "This Month" }

                val (paidInMonthList, noPaymentInMonthList, allClearList) = remember(customers, customerFinancialsMap) {
                    val paid = mutableListOf<CustomerEntity>()
                    val unpaidMonth = mutableListOf<CustomerEntity>()
                    val clear = mutableListOf<CustomerEntity>()

                    customers.forEach { c ->
                        val fin = customerFinancialsMap[c.id]
                        if (fin?.hasReceivedPaymentInMonth == true) {
                            paid.add(c)
                        } else if ((fin?.overallDues ?: 0.0) > 0.01) {
                            unpaidMonth.add(c)
                        } else {
                            clear.add(c)
                        }
                    }
                    Triple(paid, unpaidMonth, clear)
                }

                val isSequentialSort = currentSortOption in listOf(
                    CustomerSortOption.NAME_ASC,
                    CustomerSortOption.NAME_DESC,
                    CustomerSortOption.BOOK_ASC,
                    CustomerSortOption.PAGE_ASC,
                    CustomerSortOption.HIGHEST_DUE,
                    CustomerSortOption.RECENT_PAYMENT
                ) && currentFilterOption == CustomerPaymentFilterOption.ALL

                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = customerListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("customer_list"),
                        contentPadding = PaddingValues(start = 14.dp, end = 26.dp, top = 8.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (isSequentialSort) {
                            items(
                                items = customers,
                                key = { it.id }
                            ) { customer ->
                                CustomerCard(
                                    customer = customer,
                                    viewModel = viewModel,
                                    onCustomerClick = { onCustomerClick(customer.id) },
                                    onEdit = { onEditCustomerClick(folderId, customer.id) },
                                    onDelete = { customerToDelete = customer }
                                )
                            }
                        } else {
                            // SECTION 1: Paid in Month
                            if (paidInMonthList.isNotEmpty() && (currentFilterOption == CustomerPaymentFilterOption.ALL || currentFilterOption == CustomerPaymentFilterOption.PAID_THIS_MONTH)) {
                                val totalPaidInMonth = paidInMonthList.sumOf { customerFinancialsMap[it.id]?.monthPaid ?: 0.0 }
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFE8F5E9),
                                        border = BorderStroke(1.dp, Color(0xFFA5D6A7)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                                                Text(
                                                    text = "Paid in $currentMonthName (${paidInMonthList.size})",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF1B5E20)
                                                )
                                            }
                                            if (totalPaidInMonth > 0) {
                                                Text(
                                                    text = "Total Received: ₹${String.format("%.2f", totalPaidInMonth)}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF2E7D32)
                                                )
                                            }
                                        }
                                    }
                                }

                                items(
                                    items = paidInMonthList,
                                    key = { it.id }
                                ) { customer ->
                                    CustomerCard(
                                        customer = customer,
                                        viewModel = viewModel,
                                        onCustomerClick = { onCustomerClick(customer.id) },
                                        onEdit = { onEditCustomerClick(folderId, customer.id) },
                                        onDelete = { customerToDelete = customer }
                                    )
                                }
                            }

                            // SECTION 2: No Payment in Month (Has Dues)
                            if (noPaymentInMonthList.isNotEmpty() && (currentFilterOption == CustomerPaymentFilterOption.ALL || currentFilterOption == CustomerPaymentFilterOption.UNPAID_THIS_MONTH || currentFilterOption == CustomerPaymentFilterOption.UNPAID)) {
                                val totalDuesInMonth = noPaymentInMonthList.sumOf { customerFinancialsMap[it.id]?.overallDues ?: 0.0 }
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFFFF3E0),
                                        border = BorderStroke(1.dp, Color(0xFFFFCC80)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 10.dp, bottom = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(18.dp))
                                                Text(
                                                    text = "No Payment in $currentMonthName (${noPaymentInMonthList.size})",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFE65100)
                                                )
                                            }
                                            Text(
                                                text = "Pending Dues: ₹${String.format("%.2f", totalDuesInMonth)}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFE65100)
                                            )
                                        }
                                    }
                                }

                                items(
                                    items = noPaymentInMonthList,
                                    key = { it.id }
                                ) { customer ->
                                    CustomerCard(
                                        customer = customer,
                                        viewModel = viewModel,
                                        onCustomerClick = { onCustomerClick(customer.id) },
                                        onEdit = { onEditCustomerClick(folderId, customer.id) },
                                        onDelete = { customerToDelete = customer }
                                    )
                                }
                            }

                            // SECTION 3: Paid in Full / 0 Due
                            if (allClearList.isNotEmpty() && (currentFilterOption == CustomerPaymentFilterOption.ALL || currentFilterOption == CustomerPaymentFilterOption.PAID)) {
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 10.dp, bottom = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                                Text(
                                                    text = "Paid in Full / No Dues (${allClearList.size})",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }

                                items(
                                    items = allClearList,
                                    key = { it.id }
                                ) { customer ->
                                    CustomerCard(
                                        customer = customer,
                                        viewModel = viewModel,
                                        onCustomerClick = { onCustomerClick(customer.id) },
                                        onEdit = { onEditCustomerClick(folderId, customer.id) },
                                        onDelete = { customerToDelete = customer }
                                    )
                                }
                            }
                        }
                    }

                    // Side Alphabetical Index Strip with Interactive Letter Bubble Popup
                    AlphabeticalFastScroller(
                        customers = customers,
                        listState = customerListState,
                        onLetterSelected = {
                            if (currentSortOption != CustomerSortOption.NAME_ASC && currentFilterOption == CustomerPaymentFilterOption.ALL) {
                                viewModel.setCustomerSortOption(CustomerSortOption.NAME_ASC)
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }
        }
    }

        if (pullDownSyncEnabled) {
            PullToRefreshBox(
                isRefreshing = isPullRefreshing,
                onRefresh = {
                    isPullRefreshing = true
                    viewModel.syncAllDataToCloudAndDrive(context) { success, message ->
                        isPullRefreshing = false
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("folder_detail_pull_to_refresh")
            ) {
                folderDetailContent()
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("folder_detail_pull_to_refresh_disabled")
            ) {
                folderDetailContent()
            }
        }
    }

    // Delete Customer Confirmation Dialog
    customerToDelete?.let { customer ->
        val customerDues = customerFinancialsMap[customer.id]?.overallDues ?: 0.0
        val isBalanceZero = kotlin.math.abs(customerDues) <= 0.01

        if (!isBalanceZero) {
            AlertDialog(
                onDismissRequest = { customerToDelete = null },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        "Cannot Delete Account",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Customer '${customer.name}' account cannot be deleted because the balance is not zero.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFEBEE),
                            border = BorderStroke(1.dp, Color(0xFFEF9A9A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (customerDues > 0) "Pending Dues:" else "Advance Balance:",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC62828),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "₹${String.format("%.2f", kotlin.math.abs(customerDues))}",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFC62828),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                        Text(
                            text = "⚠️ Accounting Rule: A customer's account balance must reach ₹0.00 before it can be deleted. Please settle all line items and payments first.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { customerToDelete = null },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Understood")
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        } else {
            AlertDialog(
                onDismissRequest = { customerToDelete = null },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = { Text("Delete Customer Account?", fontWeight = FontWeight.Bold) },
                text = {
                    Text("The balance for '${customer.name}' is ₹0.00 (All clear). Are you sure you want to move this customer account to the Recycle Bin?")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteCustomer(customer) { success, msg ->
                                Toast.makeText(context, msg, if (success) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
                            }
                            customerToDelete = null
                        },
                        modifier = Modifier.testTag("confirm_delete_customer")
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { customerToDelete = null }) {
                        Text("Cancel")
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }
    }

    // Edit Shop Profile Dialog
    if (showEditShopDialog) {
        EditShopProfileDialog(
            currentProfile = shopProfile,
            onDismiss = { showEditShopDialog = false },
            onSave = { updated ->
                viewModel.updateShopProfile(updated)
                showEditShopDialog = false
                Toast.makeText(context, "Shop Profile Updated!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Publish Announcement / Greetings Dialog
    if (showAnnouncementDialog) {
        PublishAnnouncementDialog(
            onDismiss = { showAnnouncementDialog = false },
            viewModel = viewModel
        )
    }

    // AI Voice & Text Assistant BottomSheet
    if (showAiAssistantBottomSheet) {
        AiAssistantBottomSheet(
            viewModel = aiViewModel,
            onDismiss = { showAiAssistantBottomSheet = false },
            onCustomerClick = { customerId ->
                showAiAssistantBottomSheet = false
                onCustomerClick(customerId)
            }
        )
    }

    // Shop UPI QR Code Dialog
    if (showUpiQrDialog) {
        UpiQrGeneratorDialog(
            shopName = "श्री बर्तन भंडार",
            onDismiss = { showUpiQrDialog = false }
        )
    }

    // Floating Quick Tools Bar Composable is displayed via Popup in TopAppBar
}

@Composable
private fun FloatingQuickToolsPillBar(
    onExportCsvClick: () -> Unit,
    onSearchClick: () -> Unit,
    onQrCodeClick: () -> Unit,
    onAiAssistantClick: () -> Unit,
    onVoiceSearchClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        tonalElevation = 10.dp,
        shadowElevation = 12.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. CSV Download (Cyan/Teal)
            PillQuickToolIconButton(
                icon = Icons.Default.FileDownload,
                bgColor = Color(0xFFE0F7FA),
                tintColor = Color(0xFF00838F),
                contentDescription = "Export CSV",
                onClick = onExportCsvClick
            )

            // 2. Search (Blue)
            PillQuickToolIconButton(
                icon = Icons.Default.Search,
                bgColor = Color(0xFFE3F2FD),
                tintColor = Color(0xFF1565C0),
                contentDescription = "Search",
                onClick = onSearchClick
            )

            // 3. QR Code (Purple)
            PillQuickToolIconButton(
                icon = Icons.Default.QrCode,
                bgColor = Color(0xFFF3E5F5),
                tintColor = Color(0xFF7B1FA2),
                contentDescription = "UPI QR Code",
                onClick = onQrCodeClick
            )

            // 4. AI Assistant (Amber/Gold)
            PillQuickToolIconButton(
                icon = Icons.Default.AutoAwesome,
                bgColor = Color(0xFFFFF8E1),
                tintColor = Color(0xFFF57F17),
                contentDescription = "AI Assistant",
                onClick = onAiAssistantClick
            )

            // 5. Voice Search (Green)
            PillQuickToolIconButton(
                icon = Icons.Default.Mic,
                bgColor = Color(0xFFE8F5E9),
                tintColor = Color(0xFF2E7D32),
                contentDescription = "Voice Search",
                onClick = onVoiceSearchClick
            )
        }
    }
}

@Composable
private fun PillQuickToolIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bgColor: Color,
    tintColor: Color,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = bgColor,
        modifier = Modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tintColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
