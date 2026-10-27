package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiService
import com.example.data.local.AppDatabase
import com.example.data.model.AnalyzedDocumentData
import com.example.data.model.BillLineItem
import com.example.data.model.ChatMessageItem
import com.example.data.model.DocumentEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.FlashcardItem
import com.example.data.model.NormalizedBox
import com.example.data.model.PointNorm
import com.example.data.model.TranslatedTermItem
import com.example.data.repository.DocumentRepository
import com.example.data.repository.ExpenseRepository
import com.example.utils.ExportFormat
import com.example.utils.FileUtils
import com.example.utils.FilterType
import com.example.utils.ImageProcessor
import com.example.utils.PageSizeOption
import com.example.utils.PdfExporter
import com.example.utils.PdfQuality
import com.example.utils.QrCodeGenerator
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class AuraScanViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val docRepository = DocumentRepository(db.documentDao())
    private val expenseRepository = ExpenseRepository(db.expenseDao())
    private var geminiService = GeminiService()

    // Navigation State
    private val _currentScreen = MutableStateFlow("home")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    // Active Scan Pages
    private val _scanPages = MutableStateFlow<List<Bitmap>>(emptyList())
    val scanPages: StateFlow<List<Bitmap>> = _scanPages.asStateFlow()

    // Pristine unedited original pages for Reset functionality
    private val _originalPages = MutableStateFlow<List<Bitmap>>(emptyList())
    val originalPages: StateFlow<List<Bitmap>> = _originalPages.asStateFlow()

    private val _selectedPageIndex = MutableStateFlow(0)
    val selectedPageIndex: StateFlow<Int> = _selectedPageIndex.asStateFlow()

    private val _activeFilter = MutableStateFlow(FilterType.ORIGINAL)
    val activeFilter: StateFlow<FilterType> = _activeFilter.asStateFlow()

    private val _applyToAllPages = MutableStateFlow(false)
    val applyToAllPages: StateFlow<Boolean> = _applyToAllPages.asStateFlow()

    // 4-Corner Draggable Handles (Normalized 0.0 .. 1.0)
    private val _cornerTL = MutableStateFlow(PointNorm(0.06f, 0.05f))
    val cornerTL: StateFlow<PointNorm> = _cornerTL.asStateFlow()

    private val _cornerTR = MutableStateFlow(PointNorm(0.94f, 0.05f))
    val cornerTR: StateFlow<PointNorm> = _cornerTR.asStateFlow()

    private val _cornerBR = MutableStateFlow(PointNorm(0.94f, 0.95f))
    val cornerBR: StateFlow<PointNorm> = _cornerBR.asStateFlow()

    private val _cornerBL = MutableStateFlow(PointNorm(0.06f, 0.95f))
    val cornerBL: StateFlow<PointNorm> = _cornerBL.asStateFlow()

    private val _isDetectingEdges = MutableStateFlow(false)
    val isDetectingEdges: StateFlow<Boolean> = _isDetectingEdges.asStateFlow()

    // AI Analysis State
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _analysisStatusText = MutableStateFlow("Understanding your document...")
    val analysisStatusText: StateFlow<String> = _analysisStatusText.asStateFlow()

    private val _analyzedData = MutableStateFlow<AnalyzedDocumentData?>(null)
    val analyzedData: StateFlow<AnalyzedDocumentData?> = _analyzedData.asStateFlow()

    // Scans Library State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow("all")
    val selectedCategoryFilter: StateFlow<String> = _selectedCategoryFilter.asStateFlow()

    private val _isGridView = MutableStateFlow(true)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    private val _sortBy = MutableStateFlow("newest") // newest, oldest, name
    val sortBy: StateFlow<String> = _sortBy.asStateFlow()

    private val _isMultiSelectMode = MutableStateFlow(false)
    val isMultiSelectMode: StateFlow<Boolean> = _isMultiSelectMode.asStateFlow()

    private val _selectedDocIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedDocIds: StateFlow<Set<Long>> = _selectedDocIds.asStateFlow()

    private val _selectedDocForDetail = MutableStateFlow<DocumentEntity?>(null)
    val selectedDocForDetail: StateFlow<DocumentEntity?> = _selectedDocForDetail.asStateFlow()

    private val _totalStorageBytes = MutableStateFlow(0L)
    val totalStorageBytes: StateFlow<Long> = _totalStorageBytes.asStateFlow()

    private var lastDeletedDoc: DocumentEntity? = null

    val allDocuments = docRepository.allDocuments.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val allExpenses = expenseRepository.allExpenses.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Bill Smart Actions
    private val _editableBillItems = MutableStateFlow<List<BillLineItem>>(emptyList())
    val editableBillItems: StateFlow<List<BillLineItem>> = _editableBillItems.asStateFlow()

    private val _upiId = MutableStateFlow("aurascan.pay@okhdfcbank")
    val upiId: StateFlow<String> = _upiId.asStateFlow()

    private val _upiQrBitmap = MutableStateFlow<Bitmap?>(null)
    val upiQrBitmap: StateFlow<Bitmap?> = _upiQrBitmap.asStateFlow()

    private val _splitCount = MutableStateFlow(3)
    val splitCount: StateFlow<Int> = _splitCount.asStateFlow()

    private val _expenseSaved = MutableStateFlow(false)
    val expenseSaved: StateFlow<Boolean> = _expenseSaved.asStateFlow()

    // ID Masking Smart Actions
    private val _idMaskBoxes = MutableStateFlow<List<NormalizedBox>>(emptyList())
    val idMaskBoxes: StateFlow<List<NormalizedBox>> = _idMaskBoxes.asStateFlow()

    private val _idWatermarkPurpose = MutableStateFlow("SIM Card KYC Verification")
    val idWatermarkPurpose: StateFlow<String> = _idWatermarkPurpose.asStateFlow()

    private val _maskedBitmapPreview = MutableStateFlow<Bitmap?>(null)
    val maskedBitmapPreview: StateFlow<Bitmap?> = _maskedBitmapPreview.asStateFlow()

    private val _showMaskedComparison = MutableStateFlow(true)
    val showMaskedComparison: StateFlow<Boolean> = _showMaskedComparison.asStateFlow()

    private val _isMaskingConfirmed = MutableStateFlow(false)
    val isMaskingConfirmed: StateFlow<Boolean> = _isMaskingConfirmed.asStateFlow()

    // Study Notes Flashcards & Q&A
    private val _currentFlashcardIndex = MutableStateFlow(0)
    val currentFlashcardIndex: StateFlow<Int> = _currentFlashcardIndex.asStateFlow()

    private val _isCardFlipped = MutableStateFlow(false)
    val isCardFlipped: StateFlow<Boolean> = _isCardFlipped.asStateFlow()

    private val _knownFlashcardIds = MutableStateFlow<Set<String>>(emptySet())
    val knownFlashcardIds: StateFlow<Set<String>> = _knownFlashcardIds.asStateFlow()

    private val _isDeckCompleted = MutableStateFlow(false)
    val isDeckCompleted: StateFlow<Boolean> = _isDeckCompleted.asStateFlow()

    private val _translatedSummary = MutableStateFlow<String?>(null)
    val translatedSummary: StateFlow<String?> = _translatedSummary.asStateFlow()

    private val _translatedKeyTerms = MutableStateFlow<List<TranslatedTermItem>>(emptyList())
    val translatedKeyTerms: StateFlow<List<TranslatedTermItem>> = _translatedKeyTerms.asStateFlow()

    private val _selectedLanguage = MutableStateFlow("Hindi")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    private val _isTranslating = MutableStateFlow(false)
    val isTranslating: StateFlow<Boolean> = _isTranslating.asStateFlow()

    // Ask This Page Chat
    private val _pageChatMessages = MutableStateFlow<List<ChatMessageItem>>(emptyList())
    val pageChatMessages: StateFlow<List<ChatMessageItem>> = _pageChatMessages.asStateFlow()

    private val _isAskingQuestion = MutableStateFlow(false)
    val isAskingQuestion: StateFlow<Boolean> = _isAskingQuestion.asStateFlow()

    // Export State
    private val _selectedPdfQuality = MutableStateFlow(PdfQuality.MEDIUM)
    val selectedPdfQuality: StateFlow<PdfQuality> = _selectedPdfQuality.asStateFlow()

    private val _exportedPdfFile = MutableStateFlow<File?>(null)
    val exportedPdfFile: StateFlow<File?> = _exportedPdfFile.asStateFlow()

    // Settings
    private val _appThemeMode = MutableStateFlow("system") // system, light, dark
    val appThemeMode: StateFlow<String> = _appThemeMode.asStateFlow()

    private val _appLanguage = MutableStateFlow("en") // en, hi
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    private val _customApiKey = MutableStateFlow("")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    // Preferences & Toggles
    private val _isAiFreeMode = MutableStateFlow(false)
    val isAiFreeMode: StateFlow<Boolean> = _isAiFreeMode.asStateFlow()

    private val _askBeforeSendingToAi = MutableStateFlow(false)
    val askBeforeSendingToAi: StateFlow<Boolean> = _askBeforeSendingToAi.asStateFlow()

    private val _showAiConfirmation = MutableStateFlow(false)
    val showAiConfirmation: StateFlow<Boolean> = _showAiConfirmation.asStateFlow()
    private var pendingAiAction: (() -> Unit)? = null

    private val _defaultPdfQuality = MutableStateFlow(PdfQuality.MEDIUM)
    val defaultPdfQuality: StateFlow<PdfQuality> = _defaultPdfQuality.asStateFlow()

    private val _defaultPageSize = MutableStateFlow(PageSizeOption.A4)
    val defaultPageSize: StateFlow<PageSizeOption> = _defaultPageSize.asStateFlow()

    private val _hasCompletedOnboarding = MutableStateFlow(false)
    val hasCompletedOnboarding: StateFlow<Boolean> = _hasCompletedOnboarding.asStateFlow()

    // Rewarded Ads & Credits (ca-app-pub-3940256099942544/5224354917)
    private val _rewardCredits = MutableStateFlow(5)
    val rewardCredits: StateFlow<Int> = _rewardCredits.asStateFlow()

    private val _rewardToast = MutableStateFlow<String?>(null)
    val rewardToast: StateFlow<String?> = _rewardToast.asStateFlow()

    fun addRewardCredits(amount: Int, rewardType: String = "Credits") {
        _rewardCredits.value += amount
        _rewardToast.value = "+$amount $rewardType earned! Total: ${_rewardCredits.value}"
    }

    fun dismissRewardToast() {
        _rewardToast.value = null
    }

    // Export Progress
    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportProgress = MutableStateFlow(0f)
    val exportProgress: StateFlow<Float> = _exportProgress.asStateFlow()

    // Expenses Tab Month
    private val _selectedExpenseMonth = MutableStateFlow(Calendar.getInstance())
    val selectedExpenseMonth: StateFlow<Calendar> = _selectedExpenseMonth.asStateFlow()

    // Handwritten / Other Actions State
    private val _isCleaningUpText = MutableStateFlow(false)
    val isCleaningUpText: StateFlow<Boolean> = _isCleaningUpText.asStateFlow()

    private val _isConvertingBullets = MutableStateFlow(false)
    val isConvertingBullets: StateFlow<Boolean> = _isConvertingBullets.asStateFlow()

    private val _isReOcringRegion = MutableStateFlow(false)
    val isReOcringRegion: StateFlow<Boolean> = _isReOcringRegion.asStateFlow()

    // Legal Document Actions State
    private val _isTranslatingLegal = MutableStateFlow(false)
    val isTranslatingLegal: StateFlow<Boolean> = _isTranslatingLegal.asStateFlow()

    private val _translatedLegalBullets = MutableStateFlow<List<String>?>(null)
    val translatedLegalBullets: StateFlow<List<String>?> = _translatedLegalBullets.asStateFlow()

    private val _selectedLegalLanguage = MutableStateFlow("Hindi")
    val selectedLegalLanguage: StateFlow<String> = _selectedLegalLanguage.asStateFlow()

    init {
        refreshTotalStorage()
    }

    // Navigation Methods
    fun navigateTo(screen: String) {
        _currentScreen.value = screen
    }

    fun startNewScan() {
        _scanPages.value = emptyList()
        _originalPages.value = emptyList()
        _selectedPageIndex.value = 0
        _activeFilter.value = FilterType.ORIGINAL
        _applyToAllPages.value = false
        _analyzedData.value = null
        _expenseSaved.value = false
        _isMaskingConfirmed.value = false
        resetCornerHandles()
        navigateTo("camera")
    }

    fun addScanPage(bitmap: Bitmap) {
        _scanPages.value = _scanPages.value + bitmap
        _originalPages.value = _originalPages.value + bitmap
        _selectedPageIndex.value = _scanPages.value.size - 1
        resetCornerHandles()
    }

    fun removePageAt(index: Int) {
        val current = _scanPages.value.toMutableList()
        val orig = _originalPages.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            if (index in orig.indices) orig.removeAt(index)
            _scanPages.value = current
            _originalPages.value = orig
            if (_selectedPageIndex.value >= current.size) {
                _selectedPageIndex.value = (current.size - 1).coerceAtLeast(0)
            }
        }
    }

    fun selectPage(index: Int) {
        if (index in _scanPages.value.indices) {
            _selectedPageIndex.value = index
            resetCornerHandles()
        }
    }

    fun movePage(fromIndex: Int, toIndex: Int) {
        val list = _scanPages.value.toMutableList()
        val orig = _originalPages.value.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices && fromIndex != toIndex) {
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            if (fromIndex in orig.indices && toIndex in orig.indices) {
                val origItem = orig.removeAt(fromIndex)
                orig.add(toIndex, origItem)
            }
            _scanPages.value = list
            _originalPages.value = orig
            _selectedPageIndex.value = toIndex
        }
    }

    // Corner Handles & Edge Detection
    fun updateCornerTL(x: Float, y: Float) {
        _cornerTL.value = PointNorm(x.coerceIn(0f, 0.45f), y.coerceIn(0f, 0.45f))
    }

    fun updateCornerTR(x: Float, y: Float) {
        _cornerTR.value = PointNorm(x.coerceIn(0.55f, 1f), y.coerceIn(0f, 0.45f))
    }

    fun updateCornerBR(x: Float, y: Float) {
        _cornerBR.value = PointNorm(x.coerceIn(0.55f, 1f), y.coerceIn(0.55f, 1f))
    }

    fun updateCornerBL(x: Float, y: Float) {
        _cornerBL.value = PointNorm(x.coerceIn(0f, 0.45f), y.coerceIn(0.55f, 1f))
    }

    private fun resetCornerHandles() {
        _cornerTL.value = PointNorm(0.06f, 0.05f)
        _cornerTR.value = PointNorm(0.94f, 0.05f)
        _cornerBR.value = PointNorm(0.94f, 0.95f)
        _cornerBL.value = PointNorm(0.06f, 0.95f)
    }

    fun autoDetectEdges() {
        val index = _selectedPageIndex.value
        val pages = _scanPages.value
        if (index !in pages.indices) return

        _isDetectingEdges.value = true
        viewModelScope.launch {
            val currentBitmap = pages[index]
            val result = geminiService.detectDocumentCorners(currentBitmap)
            result.getOrNull()?.let { corners ->
                if (corners.size >= 4) {
                    _cornerTL.value = corners[0]
                    _cornerTR.value = corners[1]
                    _cornerBR.value = corners[2]
                    _cornerBL.value = corners[3]
                }
            }
            _isDetectingEdges.value = false
        }
    }

    fun applyPerspectiveCrop() {
        val index = _selectedPageIndex.value
        val pages = _scanPages.value.toMutableList()
        if (index in pages.indices) {
            val current = pages[index]
            val warped = ImageProcessor.applyPerspectiveCrop(
                source = current,
                tl = _cornerTL.value,
                tr = _cornerTR.value,
                br = _cornerBR.value,
                bl = _cornerBL.value
            )
            pages[index] = warped
            _scanPages.value = pages
            resetCornerHandles()
        }
    }

    // Filter controls
    fun toggleApplyToAllPages(enabled: Boolean) {
        _applyToAllPages.value = enabled
    }

    fun setFilter(filter: FilterType) {
        _activeFilter.value = filter
        if (_applyToAllPages.value) {
            applyFilterToAllPages(filter)
        } else {
            applyFilterToCurrentPage(filter)
        }
    }

    private fun applyFilterToCurrentPage(filter: FilterType) {
        val index = _selectedPageIndex.value
        val pages = _scanPages.value.toMutableList()
        val orig = _originalPages.value
        if (index in pages.indices && index in orig.indices) {
            pages[index] = ImageProcessor.applyFilter(orig[index], filter)
            _scanPages.value = pages
        }
    }

    private fun applyFilterToAllPages(filter: FilterType) {
        val pages = _scanPages.value.toMutableList()
        val orig = _originalPages.value
        for (i in pages.indices) {
            if (i in orig.indices) {
                pages[i] = ImageProcessor.applyFilter(orig[i], filter)
            }
        }
        _scanPages.value = pages
    }

    fun cleanUpCurrentPage() {
        val index = _selectedPageIndex.value
        val pages = _scanPages.value.toMutableList()
        if (index in pages.indices) {
            pages[index] = ImageProcessor.cleanUpDocument(pages[index])
            _scanPages.value = pages
        }
    }

    fun rotateLeftCurrentPage() {
        val index = _selectedPageIndex.value
        val pages = _scanPages.value.toMutableList()
        if (index in pages.indices) {
            pages[index] = ImageProcessor.rotateLeft(pages[index])
            _scanPages.value = pages
        }
    }

    fun rotateRightCurrentPage() {
        val index = _selectedPageIndex.value
        val pages = _scanPages.value.toMutableList()
        if (index in pages.indices) {
            pages[index] = ImageProcessor.rotateRight(pages[index])
            _scanPages.value = pages
        }
    }

    fun resetCurrentPage() {
        val index = _selectedPageIndex.value
        val pages = _scanPages.value.toMutableList()
        val orig = _originalPages.value
        if (index in pages.indices && index in orig.indices) {
            pages[index] = orig[index]
            _scanPages.value = pages
            _activeFilter.value = FilterType.ORIGINAL
            resetCornerHandles()
        }
    }

    fun updateCurrentPageBitmap(newBitmap: Bitmap) {
        val index = _selectedPageIndex.value
        val pages = _scanPages.value.toMutableList()
        if (index in pages.indices) {
            pages[index] = newBitmap
            _scanPages.value = pages
        }
    }

    // AI Analysis
    fun analyzeCurrentScan() {
        val pages = _scanPages.value
        if (pages.isEmpty()) return

        navigateTo("analysis")
        _isAnalyzing.value = true
        _analysisStatusText.value = "Understanding your document..."

        viewModelScope.launch {
            _analysisStatusText.value = "Recognizing Indian document format..."
            val firstPage = pages.first()
            val result = geminiService.analyzeDocument(firstPage)
            val data = result.getOrNull() ?: AnalyzedDocumentData()
            _analyzedData.value = data

            if (data.lineItems.isNotEmpty()) {
                _editableBillItems.value = data.lineItems
            }
            if (data.sensitiveBoxes.isNotEmpty()) {
                _idMaskBoxes.value = data.sensitiveBoxes
            } else if (data.documentType == "aadhaar" || data.documentType == "pan") {
                _idMaskBoxes.value = listOf(
                    NormalizedBox(0.68f, 0.20f, 0.77f, 0.60f, "Sensitive ID Number")
                )
            }

            val total = if (data.totalAmount > 0.0) data.totalAmount else data.subtotal
            generateUpiQr(_upiId.value, total, data.merchantName)
            updateMaskedPreview()

            _isAnalyzing.value = false
        }
    }

    fun updateDocumentType(newType: String) {
        val current = _analyzedData.value ?: return
        _analyzedData.value = current.copy(documentType = newType)
    }

    fun updateSuggestedTitle(newTitle: String) {
        val current = _analyzedData.value ?: return
        _analyzedData.value = current.copy(suggestedTitle = newTitle)
    }

    // Bill actions
    fun updateBillItem(index: Int, name: String, qty: Double, price: Double) {
        val items = _editableBillItems.value.toMutableList()
        if (index in items.indices) {
            items[index] = BillLineItem(name, qty, price)
            _editableBillItems.value = items
            recalculateBillTotals()
        }
    }

    fun addBillItem() {
        val items = _editableBillItems.value.toMutableList()
        items.add(BillLineItem("New Item", 1.0, 50.0))
        _editableBillItems.value = items
        recalculateBillTotals()
    }

    fun removeBillItem(index: Int) {
        val items = _editableBillItems.value.toMutableList()
        if (index in items.indices) {
            items.removeAt(index)
            _editableBillItems.value = items
            recalculateBillTotals()
        }
    }

    private fun recalculateBillTotals() {
        val items = _editableBillItems.value
        val sub = items.sumOf { it.qty * it.price }
        val tax = sub * 0.05
        val tot = sub + tax
        val curr = _analyzedData.value ?: return
        _analyzedData.value = curr.copy(
            subtotal = sub,
            totalAmount = tot,
            taxBreakdown = "GST 5% (₹${String.format("%.2f", tax)})"
        )
        generateUpiQr(_upiId.value, tot, curr.merchantName)
    }

    fun setUpiId(newId: String) {
        _upiId.value = newId
        val total = _analyzedData.value?.totalAmount ?: 0.0
        generateUpiQr(newId, total, _analyzedData.value?.merchantName ?: "")
    }

    fun setSplitCount(count: Int) {
        _splitCount.value = count.coerceIn(1, 20)
    }

    private fun generateUpiQr(vpa: String, amount: Double, name: String) {
        val cleanName = if (name.isBlank()) "Merchant" else name
        val amtStr = String.format("%.2f", amount)
        val upiLink = "upi://pay?pa=$vpa&pn=$cleanName&am=$amtStr&cu=INR"
        _upiQrBitmap.value = QrCodeGenerator.generateQrBitmap(upiLink)
    }

    fun saveExpenseToLog() {
        val data = _analyzedData.value ?: return
        viewModelScope.launch {
            val amount = if (data.totalAmount > 0) data.totalAmount else data.subtotal
            val merchant = if (data.merchantName.isNotBlank()) data.merchantName else data.suggestedTitle
            expenseRepository.insert(
                ExpenseEntity(
                    merchant = merchant,
                    amount = amount,
                    category = data.suggestedExpenseCategory,
                    note = "Saved from scan: ${data.suggestedTitle}"
                )
            )
            _expenseSaved.value = true
        }
    }

    // ID actions
    fun setIdWatermarkPurpose(purpose: String) {
        _idWatermarkPurpose.value = purpose
        updateMaskedPreview()
    }

    fun toggleMaskedComparison() {
        _showMaskedComparison.value = !_showMaskedComparison.value
    }

    fun addManualMaskBox() {
        val current = _idMaskBoxes.value.toMutableList()
        current.add(NormalizedBox(0.40f, 0.20f, 0.50f, 0.80f, "Custom Mask Area"))
        _idMaskBoxes.value = current
        updateMaskedPreview()
    }

    fun confirmMasking() {
        val masked = _maskedBitmapPreview.value
        if (masked != null && _scanPages.value.isNotEmpty()) {
            val pages = _scanPages.value.toMutableList()
            pages[0] = masked
            _scanPages.value = pages
        }
        _isMaskingConfirmed.value = true
    }

    private fun updateMaskedPreview() {
        val pages = _scanPages.value
        if (pages.isEmpty()) return
        val base = pages.first()
        _maskedBitmapPreview.value = ImageProcessor.applyMaskAndWatermark(
            source = base,
            boxes = _idMaskBoxes.value,
            purpose = _idWatermarkPurpose.value
        )
    }

    // Notes / Flashcards / Ask This Page
    fun nextFlashcard() {
        val list = _analyzedData.value?.flashcards ?: emptyList()
        if (list.isNotEmpty()) {
            if (_currentFlashcardIndex.value >= list.size - 1) {
                _isDeckCompleted.value = true
            } else {
                _currentFlashcardIndex.value += 1
                _isCardFlipped.value = false
            }
        }
    }

    fun flipCard() {
        _isCardFlipped.value = !_isCardFlipped.value
    }

    fun markCardResult(cardId: String, isMastered: Boolean) {
        val set = _knownFlashcardIds.value.toMutableSet()
        if (isMastered) set.add(cardId) else set.remove(cardId)
        _knownFlashcardIds.value = set
        nextFlashcard()
    }

    fun restartFlashcardDeck() {
        _currentFlashcardIndex.value = 0
        _isCardFlipped.value = false
        _isDeckCompleted.value = false
    }

    fun generateMoreFlashcards() {
        val text = _analyzedData.value?.extractedText ?: return
        viewModelScope.launch {
            val res = geminiService.generateMoreFlashcards(text, 8)
            res.getOrNull()?.let { newCards ->
                if (newCards.isNotEmpty()) {
                    val current = _analyzedData.value ?: return@let
                    _analyzedData.value = current.copy(
                        flashcards = current.flashcards + newCards
                    )
                }
            }
        }
    }

    fun translateNotesWithKeyTerms(language: String) {
        _selectedLanguage.value = language
        val data = _analyzedData.value ?: return
        val summary = data.summaryBullets.joinToString("\n• ")
        val terms = if (data.keyTerms.isNotEmpty()) data.keyTerms else listOf("Newton's Law", "Inertia", "Momentum", "Impulse")

        _isTranslating.value = true
        viewModelScope.launch {
            val res = geminiService.translateWithKeyTerms(summary, terms, language)
            res.getOrNull()?.let { (translatedSum, translatedTerms) ->
                _translatedSummary.value = translatedSum
                _translatedKeyTerms.value = translatedTerms
            }
            _isTranslating.value = false
        }
    }

    fun translateText(text: String, targetLanguage: String) {
        if (text.isBlank()) return
        executeAiWithSafetyCheck {
            viewModelScope.launch {
                val res = geminiService.translateText(text, targetLanguage)
                res.getOrNull()?.let { translated ->
                    updateExtractedText(translated)
                }
            }
        }
    }

    fun askPageQuestion(questionText: String) {
        if (questionText.isBlank()) return
        val data = _analyzedData.value ?: return

        val userMsg = ChatMessageItem(
            id = "${System.currentTimeMillis()}_u",
            sender = "user",
            text = questionText.trim()
        )
        _pageChatMessages.value = _pageChatMessages.value + userMsg
        _isAskingQuestion.value = true

        viewModelScope.launch {
            val res = geminiService.askPageQuestion(data.extractedText, questionText.trim())
            val aiMsg = ChatMessageItem(
                id = "${System.currentTimeMillis()}_ai",
                sender = "ai",
                text = res.getOrNull() ?: "Unable to answer question."
            )
            _pageChatMessages.value = _pageChatMessages.value + aiMsg
            _isAskingQuestion.value = false
        }
    }

    // Scans Library Operations
    fun toggleGridView() {
        _isGridView.value = !_isGridView.value
    }

    fun setSortBy(sort: String) {
        _sortBy.value = sort
    }

    fun toggleMultiSelectMode() {
        _isMultiSelectMode.value = !_isMultiSelectMode.value
        if (!_isMultiSelectMode.value) {
            _selectedDocIds.value = emptySet()
        }
    }

    fun toggleDocSelection(id: Long) {
        val current = _selectedDocIds.value.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        _selectedDocIds.value = current
    }

    fun openDocDetail(doc: DocumentEntity) {
        _selectedDocForDetail.value = doc
        navigateTo("scan_detail")
    }

    fun renameDocument(id: Long, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            val doc = docRepository.getDocumentById(id)
            if (doc != null) {
                docRepository.update(doc.copy(title = newTitle.trim()))
                if (_selectedDocForDetail.value?.id == id) {
                    _selectedDocForDetail.value = _selectedDocForDetail.value?.copy(title = newTitle.trim())
                }
            }
        }
    }

    fun deleteWithUndo(doc: DocumentEntity, onDeleted: () -> Unit) {
        lastDeletedDoc = doc
        viewModelScope.launch {
            docRepository.deleteById(doc.id)
            refreshTotalStorage()
            onDeleted()
        }
    }

    fun undoLastDelete() {
        val doc = lastDeletedDoc ?: return
        viewModelScope.launch {
            docRepository.insert(doc)
            lastDeletedDoc = null
            refreshTotalStorage()
        }
    }

    fun deleteSelectedDocuments() {
        val ids = _selectedDocIds.value
        viewModelScope.launch {
            ids.forEach { id -> docRepository.deleteById(id) }
            _selectedDocIds.value = emptySet()
            _isMultiSelectMode.value = false
            refreshTotalStorage()
        }
    }

    fun exportSelectedDocuments(onComplete: (File) -> Unit) {
        viewModelScope.launch {
            val docs = allDocuments.value.filter { _selectedDocIds.value.contains(it.id) }
            val allPages = mutableListOf<Bitmap>()
            docs.forEach { doc ->
                val paths = doc.pageImagePaths.split(",")
                paths.forEach { path ->
                    FileUtils.loadBitmapFromFile(path)?.let { allPages.add(it) }
                }
            }
            if (allPages.isNotEmpty()) {
                val res = PdfExporter.exportToPdf(
                    context = getApplication(),
                    pages = allPages,
                    fileName = "AuraScan_BatchExport",
                    quality = _selectedPdfQuality.value
                )
                res.getOrNull()?.let { file -> onComplete(file) }
            }
        }
    }

    fun refreshTotalStorage() {
        viewModelScope.launch {
            val dir = File(getApplication<Application>().filesDir, "scans")
            var size = 0L
            if (dir.exists()) {
                dir.walkTopDown().forEach { file ->
                    if (file.isFile) size += file.length()
                }
            }
            _totalStorageBytes.value = size
        }
    }

    fun saveScanToLibrary(onSaved: () -> Unit) {
        val pages = _scanPages.value
        if (pages.isEmpty()) return

        viewModelScope.launch {
            val paths = mutableListOf<String>()
            for ((idx, page) in pages.withIndex()) {
                val path = FileUtils.saveBitmapToFile(
                    getApplication(),
                    page,
                    prefix = "page_${idx + 1}"
                )
                paths.add(path)
            }

            val data = _analyzedData.value ?: AnalyzedDocumentData()
            val entity = DocumentEntity(
                title = data.suggestedTitle.ifBlank { "Scanned Document" },
                documentType = data.documentType,
                pageImagePaths = paths.joinToString(","),
                extractedText = data.extractedText,
                language = data.language,
                confidence = data.confidence,
                structuredJson = serializeAnalyzedData(data)
            )

            docRepository.insert(entity)
            refreshTotalStorage()
            onSaved()
        }
    }

    fun deleteDocument(id: Long) {
        viewModelScope.launch {
            docRepository.deleteById(id)
            refreshTotalStorage()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(category: String) {
        _selectedCategoryFilter.value = category
    }

    fun setPdfQuality(quality: PdfQuality) {
        _selectedPdfQuality.value = quality
    }

    // Expense Log Month Navigation & Actions
    fun previousExpenseMonth() {
        val cal = _selectedExpenseMonth.value.clone() as Calendar
        cal.add(Calendar.MONTH, -1)
        _selectedExpenseMonth.value = cal
    }

    fun nextExpenseMonth() {
        val cal = _selectedExpenseMonth.value.clone() as Calendar
        cal.add(Calendar.MONTH, 1)
        _selectedExpenseMonth.value = cal
    }

    fun updateExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            expenseRepository.update(expense)
        }
    }

    fun deleteExpense(id: Long) {
        viewModelScope.launch {
            expenseRepository.deleteById(id)
        }
    }

    fun exportExpensesToCsv(onFileReady: (File) -> Unit) {
        viewModelScope.launch {
            val list = allExpenses.value
            val csvBuilder = StringBuilder()
            csvBuilder.append("Date,Merchant,Category,Subtotal,GST,Total,Notes\n")
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US)
            for (item in list) {
                val dateStr = sdf.format(java.util.Date(item.date))
                val cleanMerchant = item.merchant.replace(",", " ")
                val cleanCat = item.category.replace(",", " ")
                val cleanNotes = item.note.replace(",", " ")
                val subtotalStr = String.format(java.util.Locale.US, "%.2f", item.subtotal)
                val gstStr = String.format(java.util.Locale.US, "%.2f", item.gstAmount)
                val totalStr = String.format(java.util.Locale.US, "%.2f", item.amount)
                csvBuilder.append("$dateStr,$cleanMerchant,$cleanCat,$subtotalStr,$gstStr,$totalStr,$cleanNotes\n")
            }
            val outputDir = File(getApplication<Application>().cacheDir, "exports").apply { mkdirs() }
            val file = File(outputDir, "AuraScan_Expenses_${System.currentTimeMillis()}.csv")
            file.writeText(csvBuilder.toString())
            onFileReady(file)
        }
    }

    // Legal Document Actions
    fun setLegalLanguage(language: String) {
        _selectedLegalLanguage.value = language
    }

    fun translateLegalSummary(language: String) {
        val data = _analyzedData.value ?: return
        val bullets = if (data.legalSummaryBullets.isNotEmpty()) {
            data.legalSummaryBullets
        } else {
            listOf(data.legalSummary)
        }

        executeAiWithSafetyCheck {
            _isTranslatingLegal.value = true
            viewModelScope.launch {
                val result = geminiService.translateLegalSummary(bullets, language)
                val list = result.getOrNull() ?: bullets.map { "[$language] $it" }
                _translatedLegalBullets.value = list
                _analyzedData.value = _analyzedData.value?.copy(legalTranslatedSummary = list.joinToString("\n• ", prefix = "• "))
                _isTranslatingLegal.value = false
            }
        }
    }

    // Handwritten / Other Actions
    fun updateExtractedText(newText: String) {
        _analyzedData.value = _analyzedData.value?.copy(extractedText = newText)
    }

    fun cleanUpHandwrittenText() {
        val text = _analyzedData.value?.extractedText ?: return
        if (text.isBlank()) return

        executeAiWithSafetyCheck {
            _isCleaningUpText.value = true
            viewModelScope.launch {
                val result = geminiService.cleanUpText(text)
                val cleaned = result.getOrNull() ?: text
                _analyzedData.value = _analyzedData.value?.copy(
                    extractedText = cleaned,
                    handwrittenCleanedText = cleaned
                )
                _isCleaningUpText.value = false
            }
        }
    }

    fun convertHandwrittenToBullets() {
        val text = _analyzedData.value?.extractedText ?: return
        if (text.isBlank()) return

        executeAiWithSafetyCheck {
            _isConvertingBullets.value = true
            viewModelScope.launch {
                val result = geminiService.convertToBullets(text)
                val bullets = result.getOrNull() ?: listOf(text)
                _analyzedData.value = _analyzedData.value?.copy(handwrittenBulletNotes = bullets)
                _isConvertingBullets.value = false
            }
        }
    }

    fun reOcrHandwrittenRegion(cropBox: NormalizedBox) {
        val pages = _scanPages.value
        if (pages.isEmpty()) return

        val original = pages.first()
        val left = (cropBox.xmin.coerceIn(0f, 1f) * original.width).toInt().coerceAtLeast(0)
        val top = (cropBox.ymin.coerceIn(0f, 1f) * original.height).toInt().coerceAtLeast(0)
        val right = (cropBox.xmax.coerceIn(0f, 1f) * original.width).toInt().coerceAtMost(original.width)
        val bottom = (cropBox.ymax.coerceIn(0f, 1f) * original.height).toInt().coerceAtMost(original.height)

        val w = (right - left).coerceAtLeast(20)
        val h = (bottom - top).coerceAtLeast(20)
        val cropped = Bitmap.createBitmap(original, left, top, w, h)

        executeAiWithSafetyCheck {
            _isReOcringRegion.value = true
            viewModelScope.launch {
                val result = geminiService.reOcrRegion(cropped)
                val recognized = result.getOrNull() ?: ""
                if (recognized.isNotBlank()) {
                    val curr = _analyzedData.value?.extractedText ?: ""
                    val updated = if (curr.isBlank()) recognized else "$curr\n[Re-scanned]: $recognized"
                    _analyzedData.value = _analyzedData.value?.copy(
                        extractedText = updated,
                        reOcredRegionText = recognized
                    )
                }
                _isReOcringRegion.value = false
            }
        }
    }

    // Advanced Export & Merging
    fun exportDocumentWithOptions(
        format: ExportFormat,
        quality: PdfQuality = _defaultPdfQuality.value,
        pageSize: PageSizeOption = _defaultPageSize.value,
        onComplete: (File) -> Unit
    ) {
        val pages = _scanPages.value
        val extracted = _analyzedData.value?.extractedText ?: ""
        val title = _analyzedData.value?.suggestedTitle ?: "AuraScan_Document"

        _isExporting.value = true
        _exportProgress.value = 0.05f

        viewModelScope.launch {
            val result = PdfExporter.exportDocument(
                context = getApplication(),
                pages = pages,
                extractedText = extracted,
                fileName = title,
                format = format,
                quality = quality,
                pageSize = pageSize,
                onProgress = { progress ->
                    _exportProgress.value = progress
                }
            )
            _isExporting.value = false
            result.getOrNull()?.let { file ->
                _exportedPdfFile.value = file
                onComplete(file)
            }
        }
    }

    fun combineMultipleScans(selectedIds: List<Long>, onComplete: (File) -> Unit) {
        _isExporting.value = true
        _exportProgress.value = 0.05f

        viewModelScope.launch {
            val docs = allDocuments.value.filter { it.id in selectedIds }
            val allPaths = mutableListOf<String>()
            for (d in docs) {
                val paths = d.pageImagePaths.split(",").filter { it.isNotBlank() }
                allPaths.addAll(paths)
            }

            val result = PdfExporter.combineScansToPdf(
                context = getApplication(),
                pageImagePaths = allPaths,
                outputTitle = "AuraScan_Combined_${docs.size}_Docs",
                quality = _defaultPdfQuality.value,
                onProgress = { progress ->
                    _exportProgress.value = progress
                }
            )
            _isExporting.value = false
            result.getOrNull()?.let { file ->
                onComplete(file)
            }
        }
    }

    // AI Control & Safety Checks
    private fun executeAiWithSafetyCheck(action: () -> Unit) {
        if (_isAiFreeMode.value) {
            // In AI-free mode, do not call Gemini API
            return
        }
        if (_askBeforeSendingToAi.value) {
            pendingAiAction = action
            _showAiConfirmation.value = true
        } else {
            action()
        }
    }

    fun confirmPendingAiAction() {
        _showAiConfirmation.value = false
        pendingAiAction?.invoke()
        pendingAiAction = null
    }

    fun dismissPendingAiAction() {
        _showAiConfirmation.value = false
        pendingAiAction = null
    }

    fun toggleAiFreeMode(enabled: Boolean) {
        _isAiFreeMode.value = enabled
    }

    fun toggleAskBeforeAi(enabled: Boolean) {
        _askBeforeSendingToAi.value = enabled
    }

    fun setDefaultPdfQuality(q: PdfQuality) {
        _defaultPdfQuality.value = q
    }

    fun setDefaultPageSize(size: PageSizeOption) {
        _defaultPageSize.value = size
    }

    fun completeOnboarding() {
        _hasCompletedOnboarding.value = true
    }

    fun clearAllScansOnly() {
        viewModelScope.launch {
            docRepository.clearAll()
            _scanPages.value = emptyList()
            _originalPages.value = emptyList()
            refreshTotalStorage()
        }
    }

    fun clearEverythingAll() {
        viewModelScope.launch {
            docRepository.clearAll()
            expenseRepository.clearAll()
            _scanPages.value = emptyList()
            _originalPages.value = emptyList()
            _analyzedData.value = null
            refreshTotalStorage()
        }
    }

    fun exportCurrentAsPdf(onComplete: (File) -> Unit) {
        val pages = _scanPages.value
        if (pages.isEmpty()) return

        viewModelScope.launch {
            val title = _analyzedData.value?.suggestedTitle ?: "AuraScan_Export"
            val result = PdfExporter.exportToPdf(
                context = getApplication(),
                pages = pages,
                fileName = title,
                quality = _selectedPdfQuality.value
            )
            result.getOrNull()?.let { file ->
                _exportedPdfFile.value = file
                onComplete(file)
            }
        }
    }

    fun setAppTheme(mode: String) {
        _appThemeMode.value = mode
    }

    fun setAppLanguage(lang: String) {
        _appLanguage.value = lang
    }

    fun setCustomApiKey(key: String) {
        _customApiKey.value = key
        geminiService = GeminiService(key)
    }

    fun clearAllData() {
        clearEverythingAll()
    }

    private fun serializeAnalyzedData(data: AnalyzedDocumentData): String {
        return try {
            val json = JSONObject().apply {
                put("documentType", data.documentType)
                put("suggestedTitle", data.suggestedTitle)
                put("extractedText", data.extractedText)
                put("merchantName", data.merchantName)
                put("totalAmount", data.totalAmount)
                put("subtotal", data.subtotal)
                put("taxBreakdown", data.taxBreakdown)
                put("idType", data.idType)
                put("idNumber", data.idNumber)
                put("maskedIdNumber", data.maskedIdNumber)
                put("holderName", data.holderName)
                put("legalSummary", data.legalSummary)
            }
            json.toString()
        } catch (_: Exception) {
            ""
        }
    }
}
