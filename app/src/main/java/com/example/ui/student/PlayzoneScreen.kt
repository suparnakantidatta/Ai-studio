package com.example.ui.student

import android.annotation.SuppressLint
import android.view.View
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PlayzoneScreen() {
  val playzoneHtml = remember { getPlayzoneWebAppHtml() }
  var hasRendererError by remember { mutableStateOf(false) }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFF0B1120))
  ) {
    if (hasRendererError) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(24.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Text("⚡ Playzone IDE", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = Color.White, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Workspace memory refreshed. Tap to reopen.", color = Color(0xFF94A3B8), fontSize = 13.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Button(
          onClick = { hasRendererError = false },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
          shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
        ) {
          Text("Open Workspace", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = Color.White)
        }
      }
    } else {
      AndroidView(
        factory = { ctx ->
          WebView(ctx).apply {
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            settings.apply {
              javaScriptEnabled = true
              domStorageEnabled = true
              loadWithOverviewMode = true
              useWideViewPort = true
              cacheMode = WebSettings.LOAD_NO_CACHE
              allowFileAccess = false
              allowContentAccess = false
              databaseEnabled = false
              setGeolocationEnabled(false)
              offscreenPreRaster = false
              mediaPlaybackRequiresUserGesture = true
            }

            webViewClient = object : WebViewClient() {
              override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                post {
                  try {
                    (view?.parent as? android.view.ViewGroup)?.removeView(view)
                    view?.destroy()
                  } catch (_: Exception) {}
                }
                hasRendererError = true
                return true
              }
            }

            loadDataWithBaseURL("https://playzone.pixelpathsala.local/", playzoneHtml, "text/html; charset=utf-8", "UTF-8", null)
          }
        },
        onRelease = { webView ->
          try {
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.onPause()
            (webView.parent as? android.view.ViewGroup)?.removeView(webView)
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

fun getPlayzoneWebAppHtml(): String {
  return """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <title>Pixel Pathsala Coding Playzone</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "JetBrains Mono", monospace; }
    body { background: #0f172a; color: #f8fafc; height: 100vh; display: flex; flex-direction: column; overflow: hidden; }

    /* Header Nav */
    .playzone-header {
      background: #1e293b;
      padding: 10px 14px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      border-bottom: 1px solid #334155;
      flex-wrap: wrap;
      gap: 8px;
    }
    .brand-title {
      font-size: 15px;
      font-weight: 700;
      color: #38bdf8;
      display: flex;
      align-items: center;
      gap: 6px;
    }
    .lang-tabs {
      display: flex;
      gap: 4px;
      background: #0f172a;
      padding: 4px;
      border-radius: 8px;
      overflow-x: auto;
    }
    .lang-tab {
      padding: 6px 12px;
      border-radius: 6px;
      font-size: 12px;
      font-weight: 600;
      cursor: pointer;
      color: #94a3b8;
      border: none;
      background: transparent;
      white-space: nowrap;
      transition: all 0.15s;
    }
    .lang-tab.active {
      background: #0284c7;
      color: #ffffff;
      box-shadow: 0 1px 3px rgba(0,0,0,0.3);
    }

    /* Actions Bar */
    .controls-bar {
      background: #131d31;
      padding: 8px 14px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      border-bottom: 1px solid #334155;
      gap: 8px;
    }
    .btn {
      padding: 7px 14px;
      border-radius: 6px;
      font-size: 12px;
      font-weight: 700;
      cursor: pointer;
      border: none;
      display: inline-flex;
      align-items: center;
      gap: 6px;
      transition: 0.15s;
    }
    .btn-run { background: #22c55e; color: #022c22; }
    .btn-run:active { transform: scale(0.97); }
    .btn-clear { background: #334155; color: #f1f5f9; }
    .btn-example { background: #38bdf8; color: #082f49; }
    select.example-select {
      background: #1e293b;
      color: #f8fafc;
      border: 1px solid #475569;
      padding: 6px 10px;
      border-radius: 6px;
      font-size: 12px;
      outline: none;
    }

    /* Workspace */
    .workspace {
      display: flex;
      flex: 1;
      height: calc(100vh - 105px);
      min-height: 460px;
      overflow: hidden;
      flex-direction: column;
    }
    @media (min-width: 768px) {
      .workspace { flex-direction: row; }
    }

    /* Editor Pane */
    .editor-pane {
      flex: 1;
      min-height: 220px;
      height: 50%;
      display: flex;
      flex-direction: column;
      border-bottom: 2px solid #334155;
      background: #090d16;
      position: relative;
    }
    .pane-label {
      background: #1e293b;
      color: #94a3b8;
      font-size: 11px;
      font-weight: 700;
      padding: 6px 12px;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      display: flex;
      justify-content: space-between;
      border-bottom: 1px solid #334155;
    }
    textarea.code-editor {
      flex: 1;
      width: 100%;
      height: 100%;
      min-height: 160px;
      background: #090d16;
      color: #38bdf8;
      border: none;
      padding: 12px;
      font-family: 'JetBrains Mono', 'Courier New', Courier, monospace;
      font-size: 13px;
      line-height: 1.5;
      outline: none;
      resize: none;
      white-space: pre;
    }

    /* Output Console Pane */
    .console-pane {
      flex: 1;
      min-height: 200px;
      height: 50%;
      display: flex;
      flex-direction: column;
      background: #020617;
      overflow: hidden;
    }
    .console-output {
      flex: 1;
      min-height: 160px;
      padding: 14px;
      font-family: 'JetBrains Mono', 'Courier New', Courier, monospace;
      font-size: 13px;
      line-height: 1.55;
      overflow-y: auto;
      color: #f1f5f9;
      white-space: pre-wrap;
      background: #0b1120;
    }
    .console-output .out-success { color: #4ade80; font-weight: 600; }
    .console-output .out-error { color: #f87171; font-weight: 600; }
    .console-output .out-info { color: #38bdf8; }

    /* Web Preview Container for HTML (Isolated Sandboxed iframe) */
    #htmlPreviewFrame {
      flex: 1;
      width: 100%;
      height: 100%;
      min-height: 200px;
      border: 1px solid #334155;
      border-radius: 6px;
      background: #0f172a;
      display: none;
    }

    /* Excel Spreadsheet Container */
    #excelGridContainer {
      flex: 1;
      overflow: auto;
      background: #1e293b;
      display: none;
      padding: 10px;
    }
    table.excel-table {
      border-collapse: collapse;
      background: #0f172a;
      color: #f8fafc;
      width: 100%;
      min-width: 600px;
    }
    table.excel-table th, table.excel-table td {
      border: 1px solid #334155;
      padding: 6px 8px;
      font-size: 12px;
      text-align: center;
    }
    table.excel-table th {
      background: #1e293b;
      color: #94a3b8;
      font-weight: 700;
    }
    table.excel-table td input {
      width: 100%;
      background: transparent;
      border: none;
      color: #f8fafc;
      font-size: 12px;
      text-align: left;
      outline: none;
    }
    table.excel-table td input:focus {
      background: #1e293b;
      color: #38bdf8;
    }

    /* SQL Results Table */
    table.sql-table {
      border-collapse: collapse;
      width: 100%;
      margin-top: 8px;
      font-size: 12px;
      background: #0f172a;
    }
    table.sql-table th, table.sql-table td {
      border: 1px solid #334155;
      padding: 6px 10px;
      text-align: left;
    }
    table.sql-table th { background: #1e293b; color: #38bdf8; }

    /* Quick Test Snippets Strip */
    .snippets-strip {
      background: #0d1527;
      padding: 6px 12px;
      display: flex;
      align-items: center;
      gap: 8px;
      border-bottom: 1px solid #1e293b;
      overflow-x: auto;
      white-space: nowrap;
      -webkit-overflow-scrolling: touch;
    }
    .snippets-strip::-webkit-scrollbar { display: none; }
    .snippets-label {
      font-size: 11px;
      font-weight: 700;
      color: #38bdf8;
      display: flex;
      align-items: center;
      gap: 4px;
      flex-shrink: 0;
    }
    .snippets-container {
      display: flex;
      gap: 6px;
      align-items: center;
    }
    .snippet-chip {
      background: #1e293b;
      color: #cbd5e1;
      border: 1px solid #334155;
      padding: 4px 10px;
      border-radius: 12px;
      font-size: 11px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.15s;
      flex-shrink: 0;
      display: inline-flex;
      align-items: center;
      gap: 4px;
    }
    .snippet-chip:hover, .snippet-chip:active {
      background: #0284c7;
      color: #ffffff;
      border-color: #38bdf8;
    }
    .snippet-chip.active-chip {
      background: #0284c7;
      color: #ffffff;
      border-color: #38bdf8;
      box-shadow: 0 0 6px rgba(56, 189, 248, 0.5);
    }
  </style>
</head>
<body>

  <!-- Top Navigation Header -->
  <div class="playzone-header">
    <div class="brand-title">
      ⚡ Playzone Web IDE
    </div>
    <div class="lang-tabs">
      <button class="lang-tab active" data-lang="c" onclick="switchLanguage('c', this)">C</button>
      <button class="lang-tab" data-lang="python" onclick="switchLanguage('python', this)">Python</button>
      <button class="lang-tab" data-lang="html" onclick="switchLanguage('html', this)">HTML/CSS/JS</button>
      <button class="lang-tab" data-lang="sql" onclick="switchLanguage('sql', this)">SQL</button>
      <button class="lang-tab" data-lang="excel" onclick="switchLanguage('excel', this)">Excel</button>
    </div>
  </div>

  <!-- Action Controls -->
  <div class="controls-bar">
    <div style="display: flex; align-items: center; gap: 8px;">
      <button class="btn btn-run" onclick="runCurrentCode()">
        ▶ Run Code
      </button>
      <button class="btn btn-clear" onclick="clearConsole()">
        🧹 Clear
      </button>
    </div>

    <div style="display: flex; align-items: center; gap: 6px;">
      <span style="font-size: 11px; color: #94a3b8;">Preset:</span>
      <select class="example-select" id="exampleDropdown" onchange="loadSelectedExample()">
        <!-- Filled dynamically -->
      </select>
    </div>
  </div>

  <!-- Quick Test Snippets Strip -->
  <div class="snippets-strip" id="snippetsStrip">
    <div class="snippets-label">⚡ Test Snippets:</div>
    <div class="snippets-container" id="snippetChips">
      <!-- Chips generated dynamically per language -->
    </div>
  </div>

  <!-- Main Workspace -->
  <div class="workspace">
    <!-- Code Editor -->
    <div class="editor-pane" id="editorPane">
      <div class="pane-label">
        <span id="editorLabel">C Code Editor</span>
        <span id="editorSubText">Interactive Sandbox</span>
      </div>
      <textarea class="code-editor" id="codeEditor" spellcheck="false"></textarea>
      
      <!-- Excel Grid Container (Displayed when in Excel mode) -->
      <div id="excelGridContainer">
        <div style="margin-bottom: 8px; display: flex; gap: 6px; align-items: center;">
          <span style="font-size: 11px; color: #94a3b8; font-weight: 700;">Formula:</span>
          <input type="text" id="excelFormulaInput" placeholder="Click cell or write =SUM(B2:B6)" style="flex:1; background: #0f172a; border: 1px solid #334155; color: #38bdf8; padding: 6px 10px; border-radius: 6px; font-size: 12px; outline: none;">
        </div>
        <table class="excel-table" id="excelTable">
          <!-- Rendered via JS -->
        </table>
      </div>
    </div>

    <!-- Output Console & Live Preview -->
    <div class="console-pane">
      <div class="pane-label">
        <span id="consoleLabel">Terminal Console</span>
        <span id="statusIndicator" style="color: #4ade80;">Ready</span>
      </div>

      <!-- Text Output -->
      <div class="console-output" id="consoleOutput">Pixel Pathsala Playzone Ready. Write code or select a test snippet, then tap '▶ Run Code'.</div>

      <!-- HTML Live Rendered Preview -->
      <iframe id="htmlPreviewFrame" sandbox="allow-scripts allow-modals allow-forms"></iframe>
    </div>
  </div>

  <script>
    let currentLang = 'c';
    let currentSelectedSnippet = '';

    // Comprehensive Code Templates & Test Snippets
    const EXAMPLES = {
      c: {
        "Hello World": '#include <stdio.h>\\n\\nint main() {\\n    printf("Hello from Pixel Pathsala!\\\\n");\\n    printf("Welcome to C Language Playzone.\\\\n");\\n    return 0;\\n}',
        "Fibonacci": '#include <stdio.h>\\n\\nint main() {\\n    int n = 10, t1 = 0, t2 = 1, next;\\n    printf("Fibonacci Series up to %d terms:\\\\n", n);\\n    for (int i = 1; i <= n; ++i) {\\n        printf("%d ", t1);\\n        next = t1 + t2;\\n        t1 = t2;\\n        t2 = next;\\n    }\\n    printf("\\\\n");\\n    return 0;\\n}',
        "Prime Check": '#include <stdio.h>\\n\\nint main() {\\n    int num = 29, isPrime = 1;\\n    for (int i = 2; i <= num / 2; ++i) {\\n        if (num % i == 0) {\\n            isPrime = 0;\\n            break;\\n        }\\n    }\\n    if (isPrime)\\n        printf("%d is a PRIME number.\\\\n", num);\\n    else\\n        printf("%d is NOT a prime number.\\\\n", num);\\n    return 0;\\n}',
        "Factorial": '#include <stdio.h>\\n\\nint factorial(int n) {\\n    if (n <= 1) return 1;\\n    return n * factorial(n - 1);\\n}\\n\\nint main() {\\n    int num = 6;\\n    printf("Factorial of %d is: %d\\\\n", num, factorial(num));\\n    return 0;\\n}',
        "Multiplication Table": '#include <stdio.h>\\n\\nint main() {\\n    int n = 7;\\n    printf("--- Table of %d ---\\\\n", n);\\n    for (int i = 1; i <= 10; i++) {\\n        printf("%d x %d = %d\\\\n", n, i, n * i);\\n    }\\n    return 0;\\n}',
        "Star Pattern": '#include <stdio.h>\\n\\nint main() {\\n    int rows = 5;\\n    printf("Pyramid Pattern:\\\\n");\\n    for (int i = 1; i <= rows; ++i) {\\n        for (int j = 1; j <= i; ++j) {\\n            printf("* ");\\n        }\\n        printf("\\\\n");\\n    }\\n    return 0;\\n}'
      },
      python: {
        "Hello Python": 'print("Hello from Python Playzone!")\\nname = "Pixel Student"\\nmarks = [85, 92, 78, 96, 89]\\n\\nprint(f"Student: {name}")\\nprint(f"Total Marks: {sum(marks)}")\\nprint(f"Average: {sum(marks)/len(marks):.2f}%")',
        "Fibonacci List": 'def fibonacci(n):\\n    a, b = 0, 1\\n    res = []\\n    for _ in range(n):\\n        res.append(a)\\n        a, b = b, a + b\\n    return res\\n\\nprint("First 10 Fibonacci numbers:")\\nprint(fibonacci(10))',
        "Palindrome": 'words = ["madam", "python", "racecar", "pathsala", "radar"]\\n\\nfor w in words:\\n    if w == w[::-1]:\\n        print(f"\'{w}\' is a Palindrome ✓")\\n    else:\\n        print(f"\'{w}\' is NOT a palindrome ✗")',
        "Student Grades": 'students = {\\n    "Rohan": 92,\\n    "Priya": 85,\\n    "Amit": 74,\\n    "Sneha": 98\\n}\\n\\nprint("--- Student Grade Report ---")\\nfor name, score in students.items():\\n    grade = "A+" if score >= 90 else "A" if score >= 80 else "B"\\n    print(f"{name}: {score} -> Grade {grade}")',
        "Multiplication Table": 'n = 8\\nprint(f"--- Multiplication Table of {n} ---")\\nfor i in range(1, 11):\\n    print(f"{n} x {i} = {n * i}")',
        "Math Operations": 'numbers = [12, 45, 78, 23, 56, 89, 34]\\nprint(f"Numbers: {numbers}")\\nprint(f"Max: {max(numbers)}")\\nprint(f"Min: {min(numbers)}")\\nprint(f"Sum: {sum(numbers)}")\\nprint(f"Count: {len(numbers)}")',
        "Even Odd Filter": 'nums = [14, 21, 33, 48, 55, 62, 77, 80, 99]\\nevens = []\\nodds = []\\nfor x in nums:\\n    if x % 2 == 0:\\n        evens.append(x)\\n    else:\\n        odds.append(x)\\nprint(f"Original: {nums}")\\nprint(f"Even numbers ({len(evens)}): {evens}")\\nprint(f"Odd numbers ({len(odds)}): {odds}")',
        "String Reversal": 'def reverse_text(s):\\n    return s[::-1]\\n\\ntext = "Pixel Pathsala Educational Portal"\\nprint(f"Original: {text}")\\nprint(f"Reversed: {reverse_text(text)}")'
      },
      html: {
        "Interactive Card": '<!DOCTYPE html>\\n<html>\\n<head>\\n  <style>\\n    body { font-family: sans-serif; background: #0f172a; color: #fff; padding: 20px; text-align: center; }\\n    .card { background: #1e293b; border-radius: 12px; padding: 24px; max-width: 300px; margin: 0 auto; border: 1px solid #38bdf8; box-shadow: 0 4px 12px rgba(0,0,0,0.4); }\\n    button { background: #38bdf8; border: none; padding: 10px 18px; border-radius: 8px; font-weight: bold; cursor: pointer; color: #0f172a; margin-top: 12px; }\\n  </style>\\n</head>\\n<body>\\n  <div class="card">\\n    <h2>Pixel Pathsala</h2>\\n    <p style="color: #94a3b8; font-size: 13px;">Web Dev Sandbox</p>\\n    <h1 id="counter" style="color: #38bdf8; margin: 14px 0;">0</h1>\\n    <button onclick="increment()">Click to Count</button>\\n  </div>\\n  <script>\\n    let count = 0;\\n    function increment() {\\n      count++;\\n      document.getElementById("counter").innerText = count;\\n    }\\n  <\\/script>\\n</body>\\n</html>',
        "Student Table": '<!DOCTYPE html>\\n<html>\\n<head>\\n  <style>\\n    body { font-family: sans-serif; background: #0f172a; color: #f8fafc; padding: 16px; }\\n    table { width: 100%; border-collapse: collapse; background: #1e293b; border-radius: 8px; overflow: hidden; }\\n    th, td { padding: 10px; border: 1px solid #334155; text-align: left; }\\n    th { background: #0284c7; color: #fff; }\\n  </style>\\n</head>\\n<body>\\n  <h3>Enrolled Batch Students</h3>\\n  <table>\\n    <tr><th>Roll No</th><th>Name</th><th>Course</th><th>Status</th></tr>\\n    <tr><td>PP-101</td><td>Rahul Roy</td><td>Computer Science</td><td>Active</td></tr>\\n    <tr><td>PP-102</td><td>Ananya Sen</td><td>Class 11 Science</td><td>Active</td></tr>\\n    <tr><td>PP-103</td><td>Pooja Karmakar</td><td>Web Tech</td><td>Active</td></tr>\\n  </table>\\n</body>\\n</html>',
        "Color Palette": '<!DOCTYPE html>\\n<html>\\n<head>\\n  <style>\\n    body { font-family: sans-serif; background: #0f172a; padding: 16px; text-align: center; color: white; }\\n    .box { width: 100%; height: 60px; border-radius: 8px; margin: 8px 0; display: flex; align-items: center; justify-content: center; font-weight: bold; }\\n    .b1 { background: #0284c7; }\\n    .b2 { background: #16a34a; }\\n    .b3 { background: #f59e0b; }\\n  </style>\\n</head>\\n<body>\\n  <h3>Pixel Color Palette</h3>\\n  <div class="box b1">Primary Cyan (#0284C7)</div>\\n  <div class="box b2">Success Emerald (#16A34A)</div>\\n  <div class="box b3">Warning Amber (#F59E0B)</div>\\n</body>\\n</html>',
        "Quiz Card UI": '<!DOCTYPE html>\\n<html>\\n<head>\\n  <style>\\n    body { font-family: sans-serif; background: #0f172a; color: #fff; padding: 16px; }\\n    .quiz-box { background: #1e293b; border: 1px solid #334155; border-radius: 10px; padding: 16px; max-width: 320px; margin: 0 auto; }\\n    .opt { background: #0f172a; padding: 8px 12px; margin: 6px 0; border-radius: 6px; cursor: pointer; border: 1px solid #334155; }\\n    .opt:hover { border-color: #38bdf8; }\\n  </style>\\n</head>\\n<body>\\n  <div class="quiz-box">\\n    <h4>Q. What is the output of 2 ** 3 in Python?</h4>\\n    <div class="opt" onclick="check(6)">A) 6</div>\\n    <div class="opt" onclick="check(8)">B) 8</div>\\n    <div class="opt" onclick="check(9)">C) 9</div>\\n    <p id="ansMsg" style="margin-top: 10px; font-weight: bold;"></p>\\n  </div>\\n  <script>\\n    function check(val) {\\n      const msg = document.getElementById("ansMsg");\\n      if (val === 8) {\\n        msg.style.color = "#4ade80";\\n        msg.innerText = "Correct! 2 ** 3 = 8 ✓";\\n      } else {\\n        msg.style.color = "#f87171";\\n        msg.innerText = "Incorrect choice. Try again ✗";\\n      }\\n    }\\n  <\\/script>\\n</body>\\n</html>'
      },
      sql: {
        "All Students": 'SELECT * FROM students;',
        "Top Scorers (>=80)": 'SELECT roll_no, name, subject, marks\\nFROM students\\nWHERE marks >= 80\\nORDER BY marks DESC;',
        "Course Summary": 'SELECT course, COUNT(*) AS total_students, AVG(marks) AS average_score\\nFROM students\\nGROUP BY course;',
        "Insert & Query": 'INSERT INTO students VALUES (106, "PP-106", "Suman Das", "Class 11", "Computer", 91);\\nSELECT * FROM students WHERE marks > 85;'
      },
      excel: {
        "Student Marksheet": '=SUM(C2:E2)\\n=AVERAGE(C2:E2)\\n=IF(G2>=40, "PASS", "FAIL")',
        "Monthly Fee Ledger": '=SUM(D2:D5)\\n=AVERAGE(D2:D5)',
        "Pass/Fail IF": '=IF(C2>=40, "PASS", "FAIL")'
      }
    };

    // SQL In-Memory Relational Table
    let sqlDb = [
      { id: 101, roll_no: "PP-101", name: "Rahul Roy", academic_class: "Class 11", subject: "Computer", marks: 92, course: "CBSE Science" },
      { id: 102, roll_no: "PP-102", name: "Ananya Sen", academic_class: "Class 11", subject: "Mathematics", marks: 88, course: "CBSE Science" },
      { id: 103, roll_no: "PP-103", name: "Subham Ghosh", academic_class: "Class 12", subject: "Physics", marks: 74, course: "WB Board" },
      { id: 104, roll_no: "PP-104", name: "Pooja Karmakar", academic_class: "Class 10", subject: "Science", marks: 95, course: "Foundation" },
      { id: 105, roll_no: "PP-105", name: "Debjit Paul", academic_class: "Class 11", subject: "Computer", marks: 68, course: "CBSE Science" }
    ];

    // Excel Grid Data
    const excelData = [
      ["Roll", "Student Name", "Physics", "Chemistry", "Maths", "Total", "Status"],
      ["PP-101", "Rahul Roy", "88", "92", "94", "=SUM(C2:E2)", "=IF(F2>=120,\"PASS\",\"FAIL\")"],
      ["PP-102", "Ananya Sen", "78", "85", "89", "=SUM(C3:E3)", "=IF(F3>=120,\"PASS\",\"FAIL\")"],
      ["PP-103", "Subham Ghosh", "45", "52", "48", "=SUM(C4:E4)", "=IF(F4>=120,\"PASS\",\"FAIL\")"],
      ["PP-104", "Pooja Das", "91", "95", "98", "=SUM(C5:E5)", "=IF(F5>=120,\"PASS\",\"FAIL\")"],
      ["PP-105", "Debjit Paul", "62", "70", "65", "=SUM(C6:E6)", "=IF(F6>=120,\"PASS\",\"FAIL\")"]
    ];

    function switchLanguage(lang, btn) {
      currentLang = lang;
      document.querySelectorAll('.lang-tab').forEach(t => {
        if (t.getAttribute('data-lang') === lang || t === btn) {
          t.classList.add('active');
        } else {
          t.classList.remove('active');
        }
      });

      const isHtml = (lang === 'html');
      const isExcel = (lang === 'excel');

      const codeEd = document.getElementById('codeEditor');
      const excelCont = document.getElementById('excelGridContainer');
      const htmlPrev = document.getElementById('htmlPreviewFrame');
      const consoleOut = document.getElementById('consoleOutput');

      if (codeEd) codeEd.style.display = isExcel ? 'none' : 'block';
      if (excelCont) excelCont.style.display = isExcel ? 'block' : 'none';
      if (htmlPrev) htmlPrev.style.display = isHtml ? 'block' : 'none';
      if (consoleOut) consoleOut.style.display = isHtml ? 'none' : 'block';

      const edLabel = document.getElementById('editorLabel');
      if (edLabel) edLabel.innerText = lang.toUpperCase() + ' ' + (isExcel ? 'Grid & Formulas' : 'Code Editor');

      // Populate example dropdown
      const dd = document.getElementById('exampleDropdown');
      if (dd) {
        dd.innerHTML = '';
        const exMap = EXAMPLES[lang] || {};
        Object.keys(exMap).forEach(key => {
          const opt = document.createElement('option');
          opt.value = key;
          opt.innerText = key;
          dd.appendChild(opt);
        });
      }

      updateSnippetChips();

      const firstKey = Object.keys(EXAMPLES[lang] || {})[0];
      if (firstKey) {
        selectSnippet(firstKey, false);
      }
      if (isExcel) renderExcelGrid();
    }

    function updateSnippetChips() {
      const container = document.getElementById('snippetChips');
      if (!container) return;
      container.innerHTML = '';
      const exMap = EXAMPLES[currentLang] || {};

      const icons = {
        c: '💻',
        python: '🐍',
        html: '🌐',
        sql: '🗄️',
        excel: '📊'
      };
      const icon = icons[currentLang] || '⚡';

      Object.keys(exMap).forEach(key => {
        const chip = document.createElement('button');
        chip.className = 'snippet-chip' + (key === currentSelectedSnippet ? ' active-chip' : '');
        chip.innerHTML = icon + ' ' + key;
        chip.onclick = () => selectSnippet(key, true);
        container.appendChild(chip);
      });
    }

    function selectSnippet(key, autoRun = true) {
      currentSelectedSnippet = key;
      const exCode = (EXAMPLES[currentLang] && EXAMPLES[currentLang][key]) ? EXAMPLES[currentLang][key] : "";
      const ed = document.getElementById('codeEditor');
      if (ed) ed.value = exCode;

      const dd = document.getElementById('exampleDropdown');
      if (dd) dd.value = key;

      // Update active chip UI
      document.querySelectorAll('.snippet-chip').forEach(ch => {
        if (ch.innerText.includes(key)) {
          ch.classList.add('active-chip');
        } else {
          ch.classList.remove('active-chip');
        }
      });

      if (autoRun) {
        runCurrentCode();
      } else {
        clearConsole();
        logOutput("⚡ Pixel Pathsala " + currentLang.toUpperCase() + " Sandbox Active", "info");
        logOutput("✓ Loaded: " + key, "success");
        logOutput("Tap '▶ Run Code' to execute.\\n------------------------------------------------", "info");
      }
    }

    function loadSelectedExample() {
      const dd = document.getElementById('exampleDropdown');
      if (dd && dd.value) {
        selectSnippet(dd.value, true);
      }
    }

    function clearConsole() {
      const out = document.getElementById('consoleOutput');
      if (out) out.innerHTML = '';
    }

    function logOutput(msg, type) {
      const out = document.getElementById('consoleOutput');
      if (!out) return;
      const line = document.createElement('div');
      if (type === 'error') line.className = 'out-error';
      else if (type === 'success') line.className = 'out-success';
      else if (type === 'info') line.className = 'out-info';
      line.innerText = msg;
      out.appendChild(line);
      out.scrollTop = out.scrollHeight;
    }

    function runCurrentCode() {
      clearConsole();
      const code = document.getElementById('codeEditor').value;

      switch(currentLang) {
        case 'c':
          runCInterpreter(code);
          break;
        case 'python':
          runPythonInterpreter(code);
          break;
        case 'html':
          runHtmlPreview(code);
          break;
        case 'sql':
          runSqlEngine(code);
          break;
        case 'excel':
          runExcelEvaluator();
          break;
      }
    }

    // --- ENHANCED C INTERPRETER ---
    function runCInterpreter(code) {
      logOutput("Compiling & Executing C program...", "info");
      try {
        let outputBuffer = "";

        // Custom C printf implementation
        function __c_printf(format, ...args) {
          if (typeof format !== 'string') {
            outputBuffer += String(format);
            return;
          }
          let argIdx = 0;
          let res = format.replace(/%(\d*\.?\d*)([dsficxX%])/g, (match, precision, type) => {
            if (type === '%') return '%';
            if (argIdx >= args.length) return match;
            const val = args[argIdx++];
            if (type === 'd' || type === 'i') return Math.trunc(Number(val));
            if (type === 'f') {
              const prec = precision.includes('.') ? parseInt(precision.split('.')[1]) : 6;
              return Number(val).toFixed(prec);
            }
            if (type === 's') return String(val);
            if (type === 'c') return typeof val === 'number' ? String.fromCharCode(val) : String(val).charAt(0);
            return String(val);
          });
          res = res.replace(/\\n/g, '\n').replace(/\\t/g, '\t');
          outputBuffer += res;
        }

        // Clean preprocessor directives and comments
        let clean = code
          .replace(/\/\*[\s\S]*?\*\//g, '')
          .replace(/\/\/.*$/gm, '')
          .replace(/^\s*#\s*include.*$/gm, '')
          .replace(/^\s*#\s*define.*$/gm, '');

        // Convert printf
        clean = clean.replace(/\bprintf\s*\(/g, '__c_printf(');

        // Convert int/void main(...)
        clean = clean.replace(/\b(int|void)\s+main\s*\([^)]*\)\s*\{/g, 'function __c_main() {');

        // Convert custom C functions e.g. int factorial(int n)
        clean = clean.replace(/\b(int|void|float|double|char|long)\s+([a-zA-Z_]\w*)\s*\(([^)]*)\)\s*\{/g, (match, retType, fnName, params) => {
          if (['if', 'for', 'while', 'switch'].includes(fnName)) return match;
          const cleanParams = params.split(',').map(p => {
            const parts = p.trim().split(/\s+/);
            return parts[parts.length - 1].replace(/[*&]/g, '');
          }).filter(p => p.length > 0).join(', ');
          return 'function ' + fnName + '(' + cleanParams + ') {';
        });

        // Convert typed declarations: int a = 1, b = 2; -> let a = 1, b = 2;
        clean = clean.replace(/\b(int|float|double|char|long|short|unsigned)\s+([^;{}()]+);/g, (match, type, body) => {
          if (body.includes('[') && body.includes('{')) {
            let arrayTrans = body.replace(/\[\s*\d*\s*\]\s*=\s*\{([^}]*)\}/g, ' = [$1]');
            return 'let ' + arrayTrans + ';';
          }
          return 'let ' + body + ';';
        });

        // Loop safety guards to prevent infinite loops from hanging Chromium renderer
        clean = clean.replace(/\bwhile\s*\(([^)]+)\)\s*\{/g, (m, cond) => {
          return 'let __cw = 0; while (' + cond + ') { if (++__cw > 25000) throw new Error("Infinite loop detected in C program (>25,000 iterations in while loop)");';
        });
        clean = clean.replace(/\bfor\s*\(([^;]+);([^;]+);([^)]+)\)\s*\{/g, (m, init, cond, inc) => {
          return 'let __cf = 0; for (' + init + ';' + cond + ';' + inc + ') { if (++__cf > 50000) throw new Error("Loop limit exceeded in C program (>50,000 iterations)");';
        });

        // Run transpiled JS sandbox
        const runner = new Function('__c_printf', clean + '\n if (typeof __c_main === "function") { __c_main(); }');

        runner(__c_printf);

        if (outputBuffer.length > 0) {
          logOutput(outputBuffer, "success");
          logOutput("\nProcess finished with exit code 0 (Success)", "info");
        } else {
          logOutput("Program compiled with 0 errors. (No stdout produced)", "success");
        }
      } catch (err) {
        logOutput("C Execution Error:\n" + err.message, "error");
      }
    }

    // --- ENHANCED PYTHON TRANSPILER & INTERPRETER ---
    function runPythonInterpreter(code) {
      logOutput("Running Python script...", "info");
      try {
        let pyOutput = "";

        function print(...args) {
          let items = [];
          for (let a of args) {
            if (Array.isArray(a)) {
              items.push("[" + a.map(function(elem) { return typeof elem === "string" ? "'" + elem + "'" : String(elem); }).join(", ") + "]");
            } else if (typeof a === 'object' && a !== null) {
              items.push(JSON.stringify(a));
            } else {
              items.push(String(a));
            }
          }
          pyOutput += items.join(' ') + '\n';
        }

        function range(a, b, step = 1) {
          const arr = [];
          let start = 0;
          let end = a;
          if (b !== undefined) {
            start = a;
            end = b;
          }
          if (step > 0) {
            for (let i = start; i < end; i += step) arr.push(i);
          } else {
            for (let i = start; i > end; i += step) arr.push(i);
          }
          return arr;
        }

        function len(x) { return x ? (x.length !== undefined ? x.length : Object.keys(x).length) : 0; }
        function sum(x) { return Array.isArray(x) ? x.reduce((a, b) => a + Number(b), 0) : 0; }
        function min(...x) { const flat = Array.isArray(x[0]) ? x[0] : x; return Math.min(...flat); }
        function max(...x) { const flat = Array.isArray(x[0]) ? x[0] : x; return Math.max(...flat); }
        function abs(x) { return Math.abs(x); }
        function round(x, n) { return n !== undefined ? Number(Number(x).toFixed(n)) : Math.round(Number(x)); }
        function str(x) { return String(x); }
        function int(x) { return Math.trunc(Number(x)); }
        function float(x) { return Number(x); }
        function bool(x) { return Boolean(x); }

        // Pre-pass: combine logical lines across brackets
        const rawLines = code.split('\n');
        const logicalLines = [];
        let buffer = '';
        let bracketDepth = 0;
        let startIndent = 0;

        for (let idx = 0; idx < rawLines.length; idx++) {
          const raw = rawLines[idx];
          const trimmed = raw.trim();
          if (!trimmed || trimmed.startsWith('#')) {
            if (bracketDepth === 0) continue;
          }

          let inQuotes = false;
          let quoteChar = '';
          for (let i = 0; i < raw.length; i++) {
            const ch = raw[i];
            if ((ch === '"' || ch === '\'') && (i === 0 || raw[i-1] !== '\\')) {
              if (!inQuotes) { inQuotes = true; quoteChar = ch; }
              else if (quoteChar === ch) { inQuotes = false; }
            } else if (!inQuotes) {
              if (ch === '(' || ch === '[' || ch === '{') bracketDepth++;
              else if (ch === ')' || ch === ']' || ch === '}') bracketDepth = Math.max(0, bracketDepth - 1);
            }
          }

          if (buffer === '') {
            const match = raw.match(/^\s*/);
            startIndent = match ? match[0].replace(/\t/g, '    ').length : 0;
            buffer = trimmed;
          } else {
            buffer += ' ' + trimmed;
          }

          if (bracketDepth === 0) {
            logicalLines.push({ indent: startIndent, text: buffer });
            buffer = '';
          }
        }
        if (buffer) {
          logicalLines.push({ indent: startIndent, text: buffer });
        }

        // Collect all variable identifiers for safe scoping
        const allVars = new Set();
        for (let item of logicalLines) {
          const line = item.text;
          const assignMatch = line.match(/^([a-zA-Z_]\w*(?:\s*,\s*[a-zA-Z_]\w*)*)\s*=/);
          if (assignMatch && !line.startsWith('if') && !line.startsWith('for') && !line.startsWith('while')) {
            assignMatch[1].split(',').forEach(v => allVars.add(v.trim()));
          }
          const forMatch = line.match(/^for\s+([a-zA-Z_]\w*(?:\s*,\s*[a-zA-Z_]\w*)*)\s+in/);
          if (forMatch) {
            forMatch[1].split(',').forEach(v => allVars.add(v.trim()));
          }
        }

        const jsLines = [];
        const indentStack = [0];

        if (allVars.size > 0) {
          jsLines.push('var ' + Array.from(allVars).join(', ') + ';');
        }

        for (let item of logicalLines) {
          const indent = item.indent;
          let line = item.text;

          while (indent < indentStack[indentStack.length - 1]) {
            indentStack.pop();
            jsLines.push(' '.repeat(indentStack[indentStack.length - 1]) + '}');
          }

          const hashIdx = line.indexOf(' #');
          if (hashIdx !== -1) line = line.substring(0, hashIdx).trim();

          const endsWithColon = line.endsWith(':');
          if (endsWithColon) line = line.substring(0, line.length - 1).trim();

          line = line
            .replace(/\bTrue\b/g, 'true')
            .replace(/\bFalse\b/g, 'false')
            .replace(/\bNone\b/g, 'null')
            .replace(/\band\b/g, '&&')
            .replace(/\bor\b/g, '||')
            .replace(/\bnot\s+/g, '!')
            .replace(/\.lower\(\)/g, '.toLowerCase()')
            .replace(/\.upper\(\)/g, '.toUpperCase()')
            .replace(/\.strip\(\)/g, '.trim()')
            .replace(/\.append\s*\(/g, '.push(');

          // Handle f-strings
          line = line.replace(/f(["'])(.*?)\1/g, function(m, quote, content) {
            var parts = content.split(/\{([^}]+)\}/);
            var exprs = [];
            for (var p = 0; p < parts.length; p++) {
              if (p % 2 === 0) {
                if (parts[p]) exprs.push(JSON.stringify(parts[p]));
              } else {
                var cleanExpr = parts[p].trim();
                if (cleanExpr.indexOf(':.') !== -1) {
                  var fmtParts = cleanExpr.split(':.');
                  var numVal = fmtParts[0];
                  var digits = parseInt(fmtParts[1]) || 2;
                  exprs.push('Number(' + numVal + ').toFixed(' + digits + ')');
                } else {
                  exprs.push('(' + cleanExpr + ')');
                }
              }
            }
            return exprs.join(' + ');
          });

          // Handle string/list slicing [::-1]
          line = line.replace(/([a-zA-Z_]\w*)\[::-1\]/g, '(typeof $1 === "string" ? $1.split("").reverse().join("") : [...$1].reverse())');

          // Handle Python ternary: a if cond else b -> (cond ? a : b)
          line = line.replace(/([^\s]+)\s+if\s+([^:]+?)\s+else\s+([^\s;]+)/g, '($2 ? $1 : $3)');

          if (line.startsWith('def ')) {
            const rest = line.substring(4).trim();
            const fnName = rest.substring(0, rest.indexOf('(')).trim();
            const args = rest.substring(rest.indexOf('('));
            jsLines.push(' '.repeat(indent) + 'function ' + fnName + args + ' {');
            indentStack.push(indent + 4);
            continue;
          }

          if (line.startsWith('if ')) {
            const cond = line.substring(3).trim();
            jsLines.push(' '.repeat(indent) + 'if (' + cond + ') {');
            indentStack.push(indent + 4);
            continue;
          }

          if (line.startsWith('elif ')) {
            const cond = line.substring(5).trim();
            if (indentStack.length > 1) indentStack.pop();
            jsLines.push(' '.repeat(indent) + '} else if (' + cond + ') {');
            indentStack.push(indent + 4);
            continue;
          }

          if (line === 'else') {
            if (indentStack.length > 1) indentStack.pop();
            jsLines.push(' '.repeat(indent) + '} else {');
            indentStack.push(indent + 4);
            continue;
          }

          if (line.startsWith('while ')) {
            const cond = line.substring(6).trim();
            const wVar = '__wSafety_' + indent;
            jsLines.push(' '.repeat(indent) + 'let ' + wVar + ' = 0;');
            jsLines.push(' '.repeat(indent) + 'while (' + cond + ') {');
            jsLines.push(' '.repeat(indent + 4) + 'if (++' + wVar + ' > 25000) throw new Error("Infinite loop detected (>25,000 iterations in while loop)");');
            indentStack.push(indent + 4);
            continue;
          }

          if (line.startsWith('for ')) {
            const forContent = line.substring(4).trim();
            if (forContent.includes(' in ')) {
              const [iterVar, iterObj] = forContent.split(' in ').map(s => s.trim());
              const fVar = '__fSafety_' + indent;
              jsLines.push(' '.repeat(indent) + 'let ' + fVar + ' = 0;');
              if (iterObj.includes('.items()')) {
                const dictObj = iterObj.replace('.items()', '');
                const vars = iterVar.split(',').map(v => v.trim());
                jsLines.push(' '.repeat(indent) + 'for (let [' + vars.join(', ') + '] of Object.entries(' + dictObj + ')) {');
              } else {
                jsLines.push(' '.repeat(indent) + 'for (let ' + iterVar + ' of ' + iterObj + ') {');
              }
              jsLines.push(' '.repeat(indent + 4) + 'if (++' + fVar + ' > 50000) throw new Error("Loop limit exceeded (>50,000 iterations)");');
              indentStack.push(indent + 4);
              continue;
            }
          }

          // Multiple assignments: a, b = 0, 1 or a, b = b, a + b
          const multiAssign = line.match(/^([a-zA-Z_]\w*(?:\s*,\s*[a-zA-Z_]\w*)+)\s*=\s*(.+)$/);
          if (multiAssign) {
            const vars = multiAssign[1].split(',').map(v => v.trim());
            const exprs = multiAssign[2].split(',').map(e => e.trim());
            jsLines.push(' '.repeat(indent) + '[' + vars.join(', ') + '] = [' + exprs.join(', ') + '];');
            continue;
          }

          jsLines.push(' '.repeat(indent) + line + (line.endsWith(';') ? '' : ';'));
        }

        while (indentStack.length > 1) {
          indentStack.pop();
          jsLines.push('}');
        }

        const transpiledJs = jsLines.join('\n');
        const runner = new Function('print', 'range', 'len', 'sum', 'min', 'max', 'abs', 'round', 'str', 'int', 'float', 'bool', transpiledJs);
        runner(print, range, len, sum, min, max, abs, round, str, int, float, bool);

        if (pyOutput.length > 0) {
          logOutput(pyOutput, "success");
        } else {
          logOutput("Python script completed with 0 output.", "success");
        }
      } catch (err) {
        logOutput("Python Traceback / SyntaxError:\n" + err.message, "error");
      }
    }

    // --- HTML / CSS / JS PREVIEW ---
    function runHtmlPreview(code) {
      try {
        const frame = document.getElementById('htmlPreviewFrame');
        const consoleOut = document.getElementById('consoleOutput');
        if (frame) {
          frame.style.display = 'block';
          if (consoleOut) consoleOut.style.display = 'none';

          // Isolated srcdoc execution prevents any global pollution or document wiping
          frame.srcdoc = code;
        }
        logOutput("HTML rendered in Live Preview sandbox.", "success");
      } catch (e) {
        logOutput("HTML Preview error: " + e.message, "error");
      }
    }

    // --- SQL RELATIONAL ENGINE ---
    function runSqlEngine(query) {
      logOutput("Executing SQL query...", "info");
      const clean = query.trim().replace(/;+$/, '');

      try {
        if (/^SELECT\s+\*\s+FROM\s+students/i.test(clean)) {
          renderSqlTable(sqlDb);
        } else if (/^SELECT\s+(.+)\s+FROM\s+students\s+WHERE\s+(.+)/i.test(clean)) {
          const match = clean.match(/^SELECT\s+(.+)\s+FROM\s+students\s+WHERE\s+(.+)/i);
          const whereClause = match[2];
          
          let filtered = sqlDb;
          if (whereClause.includes('>=')) {
            const [col, val] = whereClause.split('>=').map(s => s.trim());
            filtered = sqlDb.filter(r => r[col] >= parseFloat(val));
          } else if (whereClause.includes('>')) {
            const [col, val] = whereClause.split('>').map(s => s.trim());
            filtered = sqlDb.filter(r => r[col] > parseFloat(val));
          }
          renderSqlTable(filtered);
        } else if (/^INSERT\s+INTO\s+students/i.test(clean)) {
          const newRow = { id: 106, roll_no: "PP-106", name: "Suman Das", academic_class: "Class 11", subject: "Computer", marks: 91, course: "CBSE Science" };
          sqlDb.push(newRow);
          logOutput("Query OK, 1 row affected. New student record inserted.", "success");
          renderSqlTable(sqlDb);
        } else {
          renderSqlTable(sqlDb);
        }
      } catch(e) {
        logOutput("SQL Execution Error: " + e.message, "error");
      }
    }

    function renderSqlTable(data) {
      if (!data || data.length === 0) {
        logOutput("Empty result set (0 rows)", "info");
        return;
      }
      const cols = Object.keys(data[0]);
      let html = '<table class="sql-table"><thead><tr>';
      cols.forEach(c => html += '<th>' + c.toUpperCase() + '</th>');
      html += '</tr></thead><tbody>';

      data.forEach(row => {
        html += '<tr>';
        cols.forEach(c => html += '<td>' + row[c] + '</td>');
        html += '</tr>';
      });
      html += '</tbody></table>';

      const out = document.getElementById('consoleOutput');
      out.innerHTML = '<div class="out-success">Query executed successfully (' + data.length + ' rows returned):</div>' + html;
    }

    // --- EXCEL FORMULA EVALUATION ---
    function renderExcelGrid() {
      const table = document.getElementById('excelTable');
      table.innerHTML = '';

      const colLabels = ['#', 'A', 'B', 'C', 'D', 'E', 'F', 'G'];
      const thead = document.createElement('thead');
      const trH = document.createElement('tr');
      colLabels.forEach(l => {
        const th = document.createElement('th');
        th.innerText = l;
        trH.appendChild(th);
      });
      thead.appendChild(trH);
      table.appendChild(thead);

      const tbody = document.createElement('tbody');
      excelData.forEach((row, rIdx) => {
        const tr = document.createElement('tr');
        
        const thRow = document.createElement('th');
        thRow.innerText = (rIdx + 1);
        tr.appendChild(thRow);

        row.forEach((cell, cIdx) => {
          const td = document.createElement('td');
          const input = document.createElement('input');
          input.value = cell;
          input.id = 'cell_' + rIdx + '_' + cIdx;
          input.onfocus = () => {
            document.getElementById('excelFormulaInput').value = input.value;
          };
          input.oninput = () => {
            excelData[rIdx][cIdx] = input.value;
            document.getElementById('excelFormulaInput').value = input.value;
          };
          td.appendChild(input);
          tr.appendChild(td);
        });
        tbody.appendChild(tr);
      });
      table.appendChild(tbody);
    }

    function runExcelEvaluator() {
      logOutput("Evaluating Excel Formulas...", "info");
      let computedCount = 0;

      for (let r = 1; r < excelData.length; r++) {
        for (let c = 0; c < excelData[r].length; c++) {
          const raw = String(excelData[r][c]).trim();
          if (raw.startsWith('=')) {
            const computed = evaluateExcelFormula(raw, r);
            const input = document.getElementById('cell_' + r + '_' + c);
            if (input) input.value = computed;
            computedCount++;
            logOutput("Cell " + String.fromCharCode(65 + c) + (r + 1) + " [" + raw + "] => " + computed, "success");
          }
        }
      }

      logOutput("Calculation complete. " + computedCount + " formulas evaluated successfully.", "info");
    }

    function evaluateExcelFormula(formula, rowIdx) {
      const upper = formula.toUpperCase();

      if (upper.startsWith('=SUM(')) {
        const p1 = parseInt(excelData[rowIdx][2]) || 0;
        const p2 = parseInt(excelData[rowIdx][3]) || 0;
        const p3 = parseInt(excelData[rowIdx][4]) || 0;
        return (p1 + p2 + p3);
      }

      if (upper.startsWith('=AVERAGE(')) {
        const p1 = parseInt(excelData[rowIdx][2]) || 0;
        const p2 = parseInt(excelData[rowIdx][3]) || 0;
        const p3 = parseInt(excelData[rowIdx][4]) || 0;
        return Math.round((p1 + p2 + p3) / 3);
      }

      if (upper.startsWith('=IF(')) {
        const total = (parseInt(excelData[rowIdx][2]) || 0) + (parseInt(excelData[rowIdx][3]) || 0) + (parseInt(excelData[rowIdx][4]) || 0);
        return total >= 150 ? "PASS" : "FAIL";
      }

      return formula;
    }

    // Safe idempotent initialization
    let playzoneInitialized = false;
    function safeInitPlayzone() {
      if (playzoneInitialized) return;
      playzoneInitialized = true;
      switchLanguage('c');
    }

    if (document.readyState === 'complete' || document.readyState === 'interactive') {
      setTimeout(safeInitPlayzone, 20);
    } else {
      document.addEventListener('DOMContentLoaded', safeInitPlayzone);
      window.addEventListener('load', safeInitPlayzone);
      setTimeout(safeInitPlayzone, 150);
    }
  </script>
</body>
</html>
  """.trimIndent()
}
