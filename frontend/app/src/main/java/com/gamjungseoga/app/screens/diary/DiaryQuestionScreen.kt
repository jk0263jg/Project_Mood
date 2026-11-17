package com.gamjungseoga.app.screens.diary

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gamjungseoga.app.ui.theme.NavInactiveGray
import com.gamjungseoga.app.ui.theme.OmyuPrettyFontFamily
import com.gamjungseoga.app.ui.theme.TitleBrown
import java.time.LocalDate

// 답변 입력칸 줄 수. 최소 3줄 높이로 시작해서 내용에 따라 늘어나고, 최대 줄 수를 넘으면 입력칸 안에서 스크롤.
private const val ANSWER_MIN_LINES = 3
private const val ANSWER_MAX_LINES = 10

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DiaryQuestionScreen(
    date: LocalDate,
    question: String,
    answer: String,
    onAnswerChange: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    charLimit: Int = 500
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    // 커서 위치(selection)를 알아야 커서가 키보드에 가리지 않게 스크롤할 수 있어서 TextFieldValue로 들고 있음.
    // 한글 조합(composition) 상태도 여기 같이 보존된다. ViewModel 쪽 값(answer)이 밖에서 바뀐 경우에만 새로 맞춤.
    var fieldValue by remember { mutableStateOf(TextFieldValue(answer, TextRange(answer.length))) }
    val shownValue = if (fieldValue.text == answer) fieldValue else TextFieldValue(answer, TextRange(answer.length))
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var fieldHeightPx by remember { mutableIntStateOf(0) }
    var isFocused by remember { mutableStateOf(false) }

    // 루트: 키보드 높이만큼 아래를 띄우고(imePadding), 그 안을 통째로 세로 스크롤.
    // 키보드가 올라와 보이는 영역이 줄어도 입력칸/글자수/"다음으로" 버튼까지 스크롤로 도달 가능.
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            // 입력창 바깥(빈 공간, 질문 텍스트 등)을 탭하면 포커스를 잃고 키보드가 내려감.
            // BasicTextField 위를 탭한 경우는 그쪽에서 먼저 소비하므로 여기까지 전달되지 않음.
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                })
            }
            .imePadding()
    ) {
        val viewportHeight = maxHeight

        // 포커스를 얻었을 때, 커서가 움직이거나 줄이 늘었을 때, 키보드가 올라오며 보이는 영역이 줄었을 때
        // 커서 줄이 화면 안(키보드 위)에 오도록 바깥 스크롤을 맞춘다.
        LaunchedEffect(isFocused, shownValue.selection, textLayout, viewportHeight, fieldHeightPx) {
            val layout = textLayout ?: return@LaunchedEffect
            if (!isFocused) return@LaunchedEffect
            val offset = shownValue.selection.end.coerceIn(0, layout.layoutInput.text.length)
            val cursor = layout.getCursorRect(offset)
            // 최대 줄 수를 넘어 입력칸 안에서 스크롤 중이면 커서 좌표가 입력칸 높이를 넘을 수 있어서
            // 입력칸 범위 안으로 잘라서 요청(입력칸 자체는 내부 스크롤로 커서를 따라감).
            val bottom = cursor.bottom.coerceAtMost(fieldHeightPx.toFloat())
            val top = (bottom - cursor.height).coerceAtLeast(0f)
            bringIntoViewRequester.bringIntoView(Rect(0f, top, cursor.right.coerceAtLeast(1f), bottom))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                // 내용이 짧을 때는 화면 높이를 꽉 채워서, 아래 weight Spacer가 글자수/버튼을
                // 예전처럼 화면 맨 아래로 밀어줌. 내용이 길면 그만큼 스크롤.
                .heightIn(min = viewportHeight)
        ) {
            DiaryTopBar(onBack = onBack)
            Spacer(Modifier.height(16.dp))
            DiaryDateLabel(date, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(16.dp))
            Text(
                question,
                style = MaterialTheme.typography.headlineSmall,
                color = TitleBrown,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(24.dp))
            BasicTextField(
                value = shownValue,
                onValueChange = {
                    if (it.text.length <= charLimit) {
                        fieldValue = it
                        if (it.text != answer) onAnswerChange(it.text)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .bringIntoViewRequester(bringIntoViewRequester)
                    .onFocusChanged { isFocused = it.isFocused }
                    .onSizeChanged { fieldHeightPx = it.height },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = OmyuPrettyFontFamily, color = Color.Black),
                minLines = ANSWER_MIN_LINES,
                maxLines = ANSWER_MAX_LINES,
                onTextLayout = { textLayout = it },
                cursorBrush = SolidColor(TitleBrown),
                decorationBox = { innerTextField ->
                    if (shownValue.text.isEmpty()) {
                        Text(
                            "이곳을 눌러서 답변을 입력해주세요.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = OmyuPrettyFontFamily),
                            color = NavInactiveGray
                        )
                    }
                    innerTextField()
                }
            )
            Spacer(Modifier.weight(1f))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text(
                    "${answer.length}/$charLimit",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = OmyuPrettyFontFamily),
                    color = NavInactiveGray
                )
            }
            Spacer(Modifier.height(16.dp))
            DiaryNextButton(onClick = onNext, enabled = answer.isNotBlank())
            Spacer(Modifier.height(16.dp))
        }
    }
}
