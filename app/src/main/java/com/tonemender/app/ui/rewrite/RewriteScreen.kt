package com.tonemender.app.ui.rewrite

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.graphics.Bitmap
import androidx.core.graphics.withSave
import androidx.core.graphics.toColorInt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tonemender.app.data.local.drafts.Draft
import java.io.File
import java.io.FileOutputStream

private val toneOptions = listOf("soft", "calm", "clear")
private val recipientOptions = listOf("partner", "friend", "family", "coworker")

@Composable
fun RewriteScreen(
    onGoToDrafts: () -> Unit,
    onGoToAccount: () -> Unit,
    onGoToUpgrade: () -> Unit,
    initialDraft: Draft? = null,
    onDraftConsumed: () -> Unit = {},
    viewModel: RewriteViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboard = rememberClipboard(context)

    LaunchedEffect(Unit) {
        viewModel.refreshUser()
        viewModel.refreshUsage()
    }

    LaunchedEffect(initialDraft?.id) {
        initialDraft?.let {
            viewModel.loadDraft(it)
            onDraftConsumed()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HeaderSection(uiState.isPro, onGoToUpgrade)

        if (!uiState.isPro && uiState.rewritesLeft != null && !uiState.limitReached) {
            FreeUsageCard(
                rewritesLeft = uiState.rewritesLeft,
                onUpgrade = onGoToUpgrade
            )
        }

        if (!uiState.isPro && uiState.limitReached) {
            LimitReachedCard(onUpgrade = onGoToUpgrade)
        }

        MessageInputSection(uiState, viewModel)

        if (uiState.isPro) {
            ToneSection(uiState, viewModel)
            RecipientSection(uiState, viewModel)
        } else {
            ProUpsellCard(onGoToUpgrade)
        }

        RewriteButton(
            isLoading = uiState.isLoading,
            enabled = uiState.canRewrite,
            onClick = viewModel::rewrite
        )

        uiState.errorMessage?.let {
            ErrorText(it)
        }

        if (uiState.hasRewrite) {
            RewriteInsightsSection(uiState)
            BeforeAfterCard(uiState)
            RewriteResultSection(uiState, viewModel, clipboard, context)
        }

        NavigationSection(onGoToDrafts, onGoToAccount, onGoToUpgrade)
    }
}

@Composable
private fun HeaderSection(isPro: Boolean, onUpgrade: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("ToneMender Rewrite", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Rewrite your message with a better tone",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        AssistChip(
            onClick = { if (!isPro) onUpgrade() },
            label = { Text(if (isPro) "Pro" else "Free") }
        )
    }
}

@Composable
private fun FreeUsageCard(
    rewritesLeft: Int?,
    onUpgrade: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val title = when (rewritesLeft) {
                null -> ""
                0 -> "No free rewrites left today"
                1 -> "⚠️ 1 free rewrite left today"
                else -> "$rewritesLeft free rewrites left today"
            }

            Text(title, style = MaterialTheme.typography.titleMedium)

            Text(
                "Upgrade to ToneMender Pro for unlimited rewrites, tone control, and relationship types.",
                style = MaterialTheme.typography.bodySmall
            )

            Button(
                onClick = onUpgrade,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Upgrade to Pro")
            }
        }
    }
}

@Composable
private fun LimitReachedCard(onUpgrade: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "You’ve used all 3 free rewrites for today.",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                "Upgrade to ToneMender Pro to unlock tone control, relationship types, and unlimited rewrites.",
                style = MaterialTheme.typography.bodySmall
            )

            Button(
                onClick = onUpgrade,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Upgrade to Pro")
            }
        }
    }
}

@Composable
private fun MessageInputSection(uiState: RewriteUiState, viewModel: RewriteViewModel) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = uiState.message,
                onValueChange = viewModel::updateMessage,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Original message") },
                minLines = 6,
                maxLines = 12
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${uiState.messageCount}/2000", style = MaterialTheme.typography.bodySmall)

                if (uiState.hasEditedOriginalSinceRewrite) {
                    TextButton(onClick = viewModel::revertToOriginalMessage) {
                        Text("Revert to original")
                    }
                }
            }

            OutlinedButton(
                onClick = viewModel::clearOriginalMessage,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clear")
            }
        }
    }
}

@Composable
private fun ToneSection(uiState: RewriteUiState, viewModel: RewriteViewModel) {
    SelectionCard(
        title = "Tone",
        selected = uiState.selectedTone,
        options = toneOptions,
        onSelect = { value ->
            viewModel.updateTone(if (uiState.selectedTone == value) null else value)
        }
    )
}

@Composable
private fun RecipientSection(uiState: RewriteUiState, viewModel: RewriteViewModel) {
    SelectionCard(
        title = "Recipient",
        selected = uiState.selectedRecipient,
        options = recipientOptions,
        onSelect = { value ->
            viewModel.updateRecipient(if (uiState.selectedRecipient == value) null else value)
        }
    )
}

@Composable
private fun SelectionCard(
    title: String,
    selected: String?,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach {
                    FilterChip(
                        selected = selected == it,
                        onClick = { onSelect(it) },
                        label = { Text(it.replaceFirstChar { c -> c.uppercase() }) }
                    )
                }
            }

            Text(
                text = "Selected: ${selected ?: "None"}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ProUpsellCard(onUpgrade: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Tone + Recipient are Pro features", style = MaterialTheme.typography.titleMedium)
            Text(
                "Upgrade to ToneMender Pro for better rewrites.",
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedButton(onClick = onUpgrade, modifier = Modifier.fillMaxWidth()) {
                Text("Upgrade to Pro")
            }
        }
    }
}

@Composable
private fun RewriteButton(isLoading: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp
            )
        } else {
            Text("Rewrite")
        }
    }
}

@Composable
private fun RewriteInsightsSection(uiState: RewriteUiState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        uiState.toneScore?.let { score ->
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ScoreCircle(score = score)
                    Text(
                        "Tone Score — higher means calmer, clearer, safer to send.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        uiState.emotionalImpact?.takeIf { it.isNotBlank() }?.let { emotion ->
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = emotion,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun ScoreCircle(score: Int) {
    Surface(
        modifier = Modifier
            .size(96.dp)
            .clip(CircleShape),
        tonalElevation = 2.dp
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = score.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun BeforeAfterCard(uiState: RewriteUiState) {
    val beforeText = uiState.originalMessageSnapshot?.takeIf { it.isNotBlank() }
        ?: uiState.message.takeIf { it.isNotBlank() }
        ?: return

    val afterText = uiState.rewrittenMessage.takeIf { it.isNotBlank() } ?: return

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "ToneMender — Before & After",
                style = MaterialTheme.typography.titleMedium
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Before", style = MaterialTheme.typography.labelMedium)
                OutlinedTextField(
                    value = beforeText,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("After", style = MaterialTheme.typography.labelMedium)
                OutlinedTextField(
                    value = afterText,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        }
    }
}

@Composable
private fun RewriteResultSection(
    uiState: RewriteUiState,
    viewModel: RewriteViewModel,
    clipboard: ClipboardManager,
    context: Context
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Rewritten message",
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = uiState.rewrittenMessage,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                minLines = 4
            )

            Button(onClick = viewModel::saveDraft, modifier = Modifier.fillMaxWidth()) {
                Text("Save Draft")
            }

            OutlinedButton(
                onClick = viewModel::useRewriteAsOriginal,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Use this")
            }

            OutlinedButton(
                onClick = {
                    clipboard.setPrimaryClip(
                        ClipData.newPlainText("Rewrite", uiState.rewrittenMessage)
                    )
                    viewModel.copyRewriteToClipboard()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Copy")
            }

            OutlinedButton(
                onClick = {
                    viewModel.shareRewrite()
                    context.startActivity(
                        Intent.createChooser(
                            Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, uiState.rewrittenMessage)
                                type = "text/plain"
                            },
                            "Share rewrite"
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Share Rewrite")
            }

            OutlinedButton(
                onClick = {
                    val beforeText = uiState.originalMessageSnapshot?.takeIf { it.isNotBlank() }
                        ?: uiState.message

                    shareBeforeAfterImage(
                        context = context,
                        before = beforeText,
                        after = uiState.rewrittenMessage,
                        toneLabel = if (uiState.isPro) {
                            uiState.selectedTone?.replaceFirstChar { it.uppercase() } ?: "Rewrite"
                        } else {
                            "Default"
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Share Before/After Card")
            }
        }
    }
}

@Composable
private fun NavigationSection(
    onGoToDrafts: () -> Unit,
    onGoToAccount: () -> Unit,
    onGoToUpgrade: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onGoToDrafts) { Text("Drafts") }
        TextButton(onClick = onGoToAccount) { Text("Account") }
        TextButton(onClick = onGoToUpgrade) { Text("Upgrade") }
    }
}

@Composable
private fun ErrorText(message: String) {
    Text(message, color = MaterialTheme.colorScheme.error)
}

@Composable
private fun rememberClipboard(context: Context): ClipboardManager {
    return remember(context) {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }
}

private fun shareBeforeAfterImage(
    context: Context,
    before: String,
    after: String,
    toneLabel: String
) {
    try {
        val bitmap = createBeforeAfterBitmap(
            context = context,
            before = before,
            after = after,
            toneLabel = toneLabel
        )

        val file = File(context.cacheDir, "tonemender_before_after.png")
        FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Share Before/After"))
    } catch (_: Exception) {
        val fallbackText = "Before:\n$before\n\nAfter:\n$after\n\nWritten with ToneMender"
        context.startActivity(
            Intent.createChooser(
                Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, fallbackText)
                    type = "text/plain"
                },
                "Share Before/After"
            )
        )
    }
}

private fun createBeforeAfterBitmap(
    context: Context,
    before: String,
    after: String,
    toneLabel: String
): Bitmap {
    val density = context.resources.displayMetrics.density
    val width = 1080

    val horizontalPadding = 28f * density
    val topPadding = 28f * density
    val sectionGap = 24f * density
    val blockGap = 18f * density
    val textInset = 22f * density
    val cornerRadius = 24f * density
    val headerHeight = 110f * density
    val chipHeight = 52f * density
    val sectionLabelHeight = 40f * density
    val footerHeight = 50f * density

    val titlePaint = boldTextPaint(Color.BLACK, 42f)
    val subtitlePaint = boldTextPaint("#64748B".toColorInt(), 24f)
    val chipPaint = boldTextPaint("#2563EB".toColorInt(), 22f)
    val sectionTitlePaint = boldTextPaint(Color.BLACK, 28f)
    val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#0F172A".toColorInt()
        textSize = 26f
    }
    val footerPaint = boldTextPaint("#64748B".toColorInt(), 20f)

    val textWidth =
        width - (horizontalPadding * 2).toInt() - (textInset * 2).toInt()

    val beforeLayout = buildTextLayout(before, bodyPaint, textWidth)
    val afterLayout = buildTextLayout(after, bodyPaint, textWidth)

    val beforeBoxHeight = beforeLayout.height + textInset * 2
    val afterBoxHeight = afterLayout.height + textInset * 2

    val totalHeight = (
            topPadding +
                    headerHeight +
                    sectionGap +
                    sectionLabelHeight +
                    blockGap +
                    beforeBoxHeight +
                    sectionGap +
                    sectionLabelHeight +
                    blockGap +
                    afterBoxHeight +
                    sectionGap +
                    footerHeight +
                    topPadding
            ).toInt()

    val bitmap = createBitmap(width, totalHeight)

    bitmap.applyCanvas {
        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = android.graphics.LinearGradient(
                0f,
                0f,
                width.toFloat(),
                totalHeight.toFloat(),
                intArrayOf(Color.WHITE, "#F7FBFF".toColorInt()),
                null,
                android.graphics.Shader.TileMode.CLAMP
            )
        }

        drawRect(0f, 0f, width.toFloat(), totalHeight.toFloat(), backgroundPaint)

        var y = topPadding

        drawText("ToneMender", horizontalPadding, y + 42f, titlePaint)
        drawText("Before / After", horizontalPadding, y + 78f, subtitlePaint)

        val chipWidth = chipPaint.measureText(toneLabel) + 64f
        val chipLeft = width - horizontalPadding - chipWidth
        val chipTop = y + 8f
        val chipRect = RectF(chipLeft, chipTop, chipLeft + chipWidth, chipTop + chipHeight)

        val chipBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#EAF2FF".toColorInt()
        }

        drawRoundRect(chipRect, chipHeight / 2, chipHeight / 2, chipBgPaint)

        val chipTextY =
            chipTop + chipHeight / 2 - (chipPaint.descent() + chipPaint.ascent()) / 2
        drawText(toneLabel, chipLeft + 32f, chipTextY, chipPaint)

        y += headerHeight + sectionGap

        drawText("Before", horizontalPadding, y + 28f, sectionTitlePaint)
        y += sectionLabelHeight + blockGap

        val beforeRect = RectF(
            horizontalPadding,
            y,
            width - horizontalPadding,
            y + beforeBoxHeight
        )
        val beforeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#F1F5F9".toColorInt()
        }
        drawRoundRect(beforeRect, cornerRadius, cornerRadius, beforeBgPaint)

        withSave {
            translate(horizontalPadding + textInset, y + textInset)
            afterLayout.draw(this)
        }

        y += beforeBoxHeight + sectionGap

        drawText("After", horizontalPadding, y + 28f, sectionTitlePaint)
        y += sectionLabelHeight + blockGap

        val afterRect = RectF(
            horizontalPadding,
            y,
            width - horizontalPadding,
            y + afterBoxHeight
        )
        val afterBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#EAF2FF".toColorInt()
        }
        drawRoundRect(afterRect, cornerRadius, cornerRadius, afterBgPaint)

        withSave {
            translate(horizontalPadding + textInset, y + textInset)
            beforeLayout.draw(this)
        }

        y += afterBoxHeight + sectionGap

        val footerText = "Made with ToneMender"
        val footerWidth = footerPaint.measureText(footerText)
        drawText(
            footerText,
            width - horizontalPadding - footerWidth,
            y + 20f,
            footerPaint
        )
    }

    return bitmap
}

private fun buildTextLayout(
    text: String,
    paint: TextPaint,
    width: Int
): StaticLayout {
    return StaticLayout.Builder
        .obtain(text, 0, text.length, paint, width)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setLineSpacing(0f, 1.15f)
        .setIncludePad(false)
        .build()
}

private fun boldTextPaint(color: Int, textSize: Float): TextPaint {
    return TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        this.textSize = textSize
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.DEFAULT,
            android.graphics.Typeface.BOLD
        )
    }
}