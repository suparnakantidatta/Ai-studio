package com.example.ui.student

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ExamQuestion
import com.example.data.model.ExamSubmission
import com.example.data.model.OnlineExam
import com.example.data.model.Student
import com.example.data.repository.PathsalaRepository
import com.example.ui.theme.BrandSecondary
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class ExamWebBridge(
  private val onFinish: (Int, Int) -> Unit
) {
  @JavascriptInterface
  fun onExamSubmitted(examId: String, studentId: String, studentName: String, score: Int, totalMarks: Int) {
    onFinish(score, totalMarks)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ExamWebPlayerDialog(
  exam: OnlineExam,
  student: Student,
  repository: PathsalaRepository,
  onDismiss: () -> Unit
) {
  val allQuestions by repository.questions.collectAsState()
  val questions = remember(exam.id, allQuestions) { repository.getQuestionsForExam(exam) }
  var showConfirmExit by remember { mutableStateOf(false) }

  val questionsJson = remember(questions) {
    val arr = JSONArray()
    for (q in questions) {
      val obj = JSONObject().apply {
        put("id", q.id)
        put("text", q.questionText)
        put("optionA", q.optionA)
        put("optionB", q.optionB)
        put("optionC", q.optionC)
        put("optionD", q.optionD)
        put("correctOption", q.correctOption.uppercase())
        put("marks", q.marks)
      }
      arr.put(obj)
    }
    arr.toString()
  }

  val examHtml = remember(exam, student, questionsJson) {
    generateExamWebProjectHtml(
      exam = exam,
      student = student,
      questionsJson = questionsJson,
      durationMinutes = exam.durationMinutes
    )
  }

  var webViewKey by remember { mutableStateOf(0) }

  Dialog(
    onDismissRequest = { showConfirmExit = true },
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Scaffold(
      topBar = {
        TopAppBar(
          title = {
            Column {
              Text(
                text = exam.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
              Text(
                text = "${exam.subject} • Candidate: ${student.name} (${student.rollNo})",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.8f)
              )
            }
          },
          navigationIcon = {
            IconButton(onClick = { showConfirmExit = true }) {
              Icon(Icons.Default.Close, contentDescription = "Close Exam", tint = Color.White)
            }
          },
          colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandSecondary)
        )
      }
    ) { padding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding)
          .background(Color(0xFF0F172A))
      ) {
        key(webViewKey) {
          AndroidView(
            factory = { ctx ->
              WebView(ctx).apply {
                setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                settings.apply {
                  javaScriptEnabled = true
                  domStorageEnabled = true
                  loadWithOverviewMode = true
                  useWideViewPort = true
                  cacheMode = WebSettings.LOAD_NO_CACHE
                  allowFileAccess = false
                  allowContentAccess = false
                }

                webViewClient = object : WebViewClient() {
                  override fun onRenderProcessGone(view: WebView?, detail: android.webkit.RenderProcessGoneDetail?): Boolean {
                    try {
                      (view?.parent as? android.view.ViewGroup)?.removeView(view)
                      view?.destroy()
                    } catch (_: Exception) {}
                    webViewKey++
                    return true
                  }
                }

                addJavascriptInterface(
                  ExamWebBridge { score, totalMarks ->
                    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)
                    val submission = ExamSubmission(
                      id = "sub-${System.currentTimeMillis()}",
                      examId = exam.id,
                      studentId = student.id,
                      studentName = student.name,
                      score = score,
                      totalMarks = totalMarks,
                      submittedAt = sdf.format(Date())
                    )
                    repository.submitExamSubmission(submission)
                    post {
                      Toast.makeText(ctx, "Exam Completed! Score: $score / $totalMarks", Toast.LENGTH_LONG).show()
                    }
                  },
                  "AndroidExamBridge"
                )

                loadDataWithBaseURL("https://exam.pixelpathsala.local/", examHtml, "text/html; charset=utf-8", "UTF-8", null)
              }
            },
            onRelease = { webView ->
              try {
                (webView.parent as? android.view.ViewGroup)?.removeView(webView)
                webView.stopLoading()
                webView.onPause()
                webView.clearHistory()
                webView.removeAllViews()
                webView.destroy()
              } catch (_: Exception) {}
            },
            modifier = Modifier.fillMaxSize()
          )
        }
      }
    }
  }

  if (showConfirmExit) {
    AlertDialog(
      onDismissRequest = { showConfirmExit = false },
      title = { Text("Exit Online Exam?", fontWeight = FontWeight.Bold) },
      text = { Text("Are you sure you want to exit? Any unsubmitted responses will not be saved.") },
      confirmButton = {
        Button(
          onClick = {
            showConfirmExit = false
            onDismiss()
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
        ) {
          Text("Exit Test")
        }
      },
      dismissButton = {
        TextButton(onClick = { showConfirmExit = false }) {
          Text("Continue Test")
        }
      }
    )
  }
}

fun generateExamWebProjectHtml(
  exam: OnlineExam,
  student: Student,
  questionsJson: String,
  durationMinutes: Int
): String {
  return """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <title>${exam.title} - Online CBT Exam</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
    body { background-color: #f1f5f9; color: #1e293b; display: flex; flex-direction: column; height: 100vh; overflow: hidden; }
    
    /* Top Bar */
    .exam-header {
      background: #0f172a;
      color: #fff;
      padding: 12px 16px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      box-shadow: 0 2px 8px rgba(0,0,0,0.2);
    }
    .exam-timer {
      background: #dc2626;
      color: #fff;
      padding: 6px 14px;
      border-radius: 20px;
      font-weight: 700;
      font-size: 14px;
      letter-spacing: 0.5px;
      display: flex;
      align-items: center;
      gap: 6px;
    }
    .exam-timer.safe { background: #0284c7; }
    
    /* Main Layout */
    .exam-container {
      display: flex;
      flex: 1;
      overflow: hidden;
      position: relative;
    }
    .question-panel {
      flex: 1;
      padding: 18px;
      overflow-y: auto;
      display: flex;
      flex-direction: column;
    }
    .q-meta {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;
      font-size: 13px;
      font-weight: 600;
      color: #64748b;
    }
    .q-badge {
      background: #e2e8f0;
      color: #334155;
      padding: 4px 10px;
      border-radius: 6px;
    }
    .q-marks { color: #0284c7; }
    
    .q-text {
      font-size: 16px;
      font-weight: 600;
      line-height: 1.5;
      color: #0f172a;
      margin-bottom: 18px;
      background: #fff;
      padding: 16px;
      border-radius: 12px;
      border: 1px solid #cbd5e1;
    }
    
    /* Options */
    .options-group {
      display: flex;
      flex-direction: column;
      gap: 10px;
      margin-bottom: 20px;
    }
    .option-card {
      background: #fff;
      border: 2px solid #e2e8f0;
      border-radius: 10px;
      padding: 12px 16px;
      display: flex;
      align-items: center;
      gap: 12px;
      cursor: pointer;
      transition: all 0.15s ease;
      font-size: 14px;
    }
    .option-card:hover { border-color: #94a3b8; }
    .option-card.selected {
      border-color: #2563eb;
      background: #eff6ff;
      color: #1d4ed8;
      font-weight: 600;
    }
    .opt-circle {
      width: 28px;
      height: 28px;
      border-radius: 50%;
      background: #f1f5f9;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 700;
      font-size: 13px;
      color: #475569;
      flex-shrink: 0;
    }
    .option-card.selected .opt-circle {
      background: #2563eb;
      color: #fff;
    }
    
    /* Palette Drawer */
    .palette-drawer {
      width: 130px;
      background: #fff;
      border-left: 1px solid #cbd5e1;
      padding: 12px;
      overflow-y: auto;
      display: flex;
      flex-direction: column;
      gap: 12px;
    }
    .palette-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 6px;
    }
    .pal-btn {
      aspect-ratio: 1;
      border-radius: 8px;
      border: 1px solid #cbd5e1;
      background: #f8fafc;
      font-weight: 700;
      font-size: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
    }
    .pal-btn.active { outline: 2px solid #2563eb; }
    .pal-btn.answered { background: #22c55e; color: #fff; border-color: #16a34a; }
    .pal-btn.review { background: #8b5cf6; color: #fff; border-color: #7c3aed; }
    .pal-btn.visited { background: #ef4444; color: #fff; border-color: #dc2626; }
    
    /* Bottom Actions */
    .bottom-bar {
      background: #fff;
      border-top: 1px solid #cbd5e1;
      padding: 10px 16px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 8px;
    }
    .btn {
      padding: 10px 16px;
      border-radius: 8px;
      font-size: 13px;
      font-weight: 600;
      border: none;
      cursor: pointer;
      display: flex;
      align-items: center;
      gap: 6px;
    }
    .btn-secondary { background: #e2e8f0; color: #334155; }
    .btn-primary { background: #2563eb; color: #fff; }
    .btn-purple { background: #8b5cf6; color: #fff; }
    .btn-danger { background: #16a34a; color: #fff; }
    
    /* Result Screen */
    #resultModal {
      position: fixed;
      top: 0; left: 0; right: 0; bottom: 0;
      background: #fff;
      z-index: 100;
      display: none;
      flex-direction: column;
      overflow-y: auto;
      padding: 20px;
    }
    .score-card {
      background: linear-gradient(135deg, #1e293b, #0f172a);
      color: #fff;
      padding: 24px;
      border-radius: 16px;
      text-align: center;
      margin-bottom: 20px;
    }
    .score-num { font-size: 42px; font-weight: 800; color: #38bdf8; margin: 8px 0; }
    .status-badge {
      display: inline-block;
      padding: 4px 16px;
      border-radius: 20px;
      font-weight: 700;
      font-size: 13px;
      background: #22c55e;
      color: #fff;
    }
    .review-item {
      background: #fff;
      border: 1px solid #cbd5e1;
      border-radius: 10px;
      padding: 14px;
      margin-bottom: 12px;
    }
  </style>
</head>
<body>

  <!-- Header -->
  <div class="exam-header">
    <div>
      <div style="font-weight: 700; font-size: 14px;">${exam.title}</div>
      <div style="font-size: 11px; color: #94a3b8;">${student.name} • ${exam.subject}</div>
    </div>
    <div class="exam-timer safe" id="timerBox">
      ⏱️ <span id="timerText">00:00</span>
    </div>
  </div>

  <!-- Exam Container -->
  <div class="exam-container" id="examLayout">
    <!-- Question Area -->
    <div class="question-panel">
      <div class="q-meta">
        <span class="q-badge" id="qNumBadge">Question 1 of 10</span>
        <span class="q-marks" id="qMarksBadge">+2 Marks</span>
      </div>

      <div class="q-text" id="qText">Loading question...</div>

      <div class="options-group" id="optionsGroup">
        <!-- Rendered via JS -->
      </div>

      <div style="display: flex; gap: 8px; margin-top: auto;">
        <button class="btn btn-secondary" onclick="clearChoice()">Clear Choice</button>
        <button class="btn btn-purple" onclick="toggleMarkReview()">Mark for Review</button>
      </div>
    </div>

    <!-- Palette -->
    <div class="palette-drawer">
      <div style="font-size: 11px; font-weight: 700; color: #64748b; text-transform: uppercase;">Palette</div>
      <div class="palette-grid" id="paletteGrid">
        <!-- Rendered via JS -->
      </div>
      <div style="margin-top: auto;">
        <button class="btn btn-danger" style="width: 100%; justify-content: center;" onclick="confirmSubmit()">Submit</button>
      </div>
    </div>
  </div>

  <!-- Bottom Bar -->
  <div class="bottom-bar">
    <button class="btn btn-secondary" id="btnPrev" onclick="prevQuestion()">← Previous</button>
    <div id="statSummary" style="font-size: 12px; color: #64748b; font-weight: 600;">0 / 0 Answered</div>
    <button class="btn btn-primary" id="btnNext" onclick="nextQuestion()">Next →</button>
  </div>

  <!-- Result Screen -->
  <div id="resultModal">
    <div class="score-card">
      <div style="font-size: 14px; opacity: 0.8;">CBT Assessment Results</div>
      <div class="score-num" id="finalScoreText">0 / 0</div>
      <div class="status-badge" id="passStatusBadge">PASSED</div>
      <div style="font-size: 12px; margin-top: 10px; color: #cbd5e1;" id="summaryStats">Accuracy: 0%</div>
    </div>

    <h3 style="font-size: 16px; margin-bottom: 12px;">Detailed Solutions & Explanations</h3>
    <div id="reviewContainer">
      <!-- Solutions rendered via JS -->
    </div>
  </div>

  <script>
    const questions = $questionsJson;
    let currentIndex = 0;
    const userAnswers = {}; // qId -> 'A' | 'B' | 'C' | 'D'
    const reviewStatus = {}; // qId -> bool
    const visited = {}; // qId -> bool

    let timeRemaining = ${durationMinutes * 60};
    let timerInterval = null;

    function initExam() {
      if (!questions || questions.length === 0) {
        document.getElementById('qText').innerText = "No questions found for this exam.";
        return;
      }
      renderPalette();
      loadQuestion(0);
      startTimer();
    }

    function startTimer() {
      timerInterval = setInterval(() => {
        timeRemaining--;
        if (timeRemaining <= 0) {
          clearInterval(timerInterval);
          autoSubmit();
          return;
        }
        updateTimerDisplay();
      }, 1000);
      updateTimerDisplay();
    }

    function updateTimerDisplay() {
      const m = Math.floor(timeRemaining / 60);
      const s = timeRemaining % 60;
      const str = String(m).padStart(2, '0') + ':' + String(s).padStart(2, '0');
      document.getElementById('timerText').innerText = str;
      const box = document.getElementById('timerBox');
      if (timeRemaining <= 300) {
        box.classList.remove('safe');
      } else {
        box.classList.add('safe');
      }
    }

    function loadQuestion(idx) {
      if (idx < 0 || idx >= questions.length) return;
      currentIndex = idx;
      const q = questions[idx];
      visited[q.id] = true;

      document.getElementById('qNumBadge').innerText = 'Question ' + (idx + 1) + ' of ' + questions.length;
      document.getElementById('qMarksBadge').innerText = '+' + q.marks + ' Marks';
      document.getElementById('qText').innerText = q.text;

      const optsGroup = document.getElementById('optionsGroup');
      optsGroup.innerHTML = '';

      const options = [
        { key: 'A', text: q.optionA },
        { key: 'B', text: q.optionB },
        { key: 'C', text: q.optionC },
        { key: 'D', text: q.optionD }
      ];

      options.forEach(opt => {
        const isSelected = userAnswers[q.id] === opt.key;
        const card = document.createElement('div');
        card.className = 'option-card' + (isSelected ? ' selected' : '');
        card.onclick = () => selectOption(opt.key);
        card.innerHTML = '<div class="opt-circle">' + opt.key + '</div><div>' + opt.text + '</div>';
        optsGroup.appendChild(card);
      });

      document.getElementById('btnPrev').disabled = (idx === 0);
      document.getElementById('btnNext').innerText = (idx === questions.length - 1) ? 'Finish →' : 'Next →';

      updateStats();
      renderPalette();
    }

    function selectOption(key) {
      const q = questions[currentIndex];
      userAnswers[q.id] = key;
      loadQuestion(currentIndex);
    }

    function clearChoice() {
      const q = questions[currentIndex];
      delete userAnswers[q.id];
      loadQuestion(currentIndex);
    }

    function toggleMarkReview() {
      const q = questions[currentIndex];
      reviewStatus[q.id] = !reviewStatus[q.id];
      renderPalette();
    }

    function nextQuestion() {
      if (currentIndex < questions.length - 1) {
        loadQuestion(currentIndex + 1);
      } else {
        confirmSubmit();
      }
    }

    function prevQuestion() {
      if (currentIndex > 0) {
        loadQuestion(currentIndex - 1);
      }
    }

    function renderPalette() {
      const grid = document.getElementById('paletteGrid');
      grid.innerHTML = '';
      questions.forEach((q, i) => {
        const btn = document.createElement('div');
        btn.className = 'pal-btn';
        if (i === currentIndex) btn.classList.add('active');
        if (userAnswers[q.id]) {
          btn.classList.add('answered');
        } else if (reviewStatus[q.id]) {
          btn.classList.add('review');
        } else if (visited[q.id]) {
          btn.classList.add('visited');
        }
        btn.innerText = (i + 1);
        btn.onclick = () => loadQuestion(i);
        grid.appendChild(btn);
      });
    }

    function updateStats() {
      const ansCount = Object.keys(userAnswers).length;
      document.getElementById('statSummary').innerText = ansCount + ' / ' + questions.length + ' Answered';
    }

    function confirmSubmit() {
      const ansCount = Object.keys(userAnswers).length;
      const unans = questions.length - ansCount;
      const ok = confirm("Submit Exam?\n\nAnswered: " + ansCount + "\nUnanswered: " + unans + "\n\nClick OK to finalize your score.");
      if (ok) {
        submitExam();
      }
    }

    function autoSubmit() {
      alert("Time is up! Submitting exam automatically.");
      submitExam();
    }

    function submitExam() {
      clearInterval(timerInterval);

      let totalScore = 0;
      let maxMarks = 0;
      let correctCount = 0;

      questions.forEach(q => {
        maxMarks += q.marks;
        if (userAnswers[q.id] && userAnswers[q.id].toUpperCase() === q.correctOption.toUpperCase()) {
          totalScore += q.marks;
          correctCount++;
        }
      });

      // Pass result to Android Bridge
      try {
        if (window.AndroidExamBridge && window.AndroidExamBridge.onExamSubmitted) {
          window.AndroidExamBridge.onExamSubmitted("${exam.id}", "${student.id}", "${student.name}", totalScore, maxMarks);
        }
      } catch(e) {
        console.error(e);
      }

      // Show Result View
      document.getElementById('resultModal').style.display = 'flex';
      document.getElementById('finalScoreText').innerText = totalScore + ' / ' + maxMarks;
      
      const pct = maxMarks > 0 ? Math.round((totalScore / maxMarks) * 100) : 0;
      const passed = pct >= 40;
      const badge = document.getElementById('passStatusBadge');
      badge.innerText = passed ? "PASSED (" + pct + "%)" : "NEEDS IMPROVEMENT (" + pct + "%)";
      badge.style.background = passed ? "#22c55e" : "#ef4444";

      document.getElementById('summaryStats').innerText = "Correct: " + correctCount + " of " + questions.length + " questions (" + pct + "% Accuracy)";

      // Render solutions
      const revBox = document.getElementById('reviewContainer');
      revBox.innerHTML = '';
      questions.forEach((q, idx) => {
        const uAns = userAnswers[q.id] || "Not Answered";
        const isRight = (uAns === q.correctOption);

        const card = document.createElement('div');
        card.className = 'review-item';
        card.style.borderLeft = isRight ? "4px solid #22c55e" : "4px solid #ef4444";

        card.innerHTML = 
          '<div style="font-weight: 700; margin-bottom: 6px;">Q' + (idx + 1) + '. ' + q.text + '</div>' +
          '<div style="font-size: 13px; color: ' + (isRight ? '#16a34a' : '#dc2626') + '; font-weight: 600;">Your Answer: Option ' + uAns + ' ' + (isRight ? '✓' : '✗') + '</div>' +
          '<div style="font-size: 13px; color: #16a34a; font-weight: 600;">Correct Answer: Option ' + q.correctOption + '</div>';

        revBox.appendChild(card);
      });
    }

    let examInitialized = false;
    function safeInitExam() {
      if (examInitialized) return;
      examInitialized = true;
      initExam();
    }

    if (document.readyState === 'complete' || document.readyState === 'interactive') {
      setTimeout(safeInitExam, 20);
    } else {
      document.addEventListener('DOMContentLoaded', safeInitExam);
      window.addEventListener('load', safeInitExam);
      setTimeout(safeInitExam, 150);
    }
  </script>
</body>
</html>
  """.trimIndent()
}
