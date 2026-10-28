package com.example.ui.student

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ExamQuestion
import com.example.data.model.ExamSubmission
import com.example.data.model.OnlineExam
import com.example.data.model.Student
import com.example.data.repository.PathsalaRepository
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NativeStudentExamPlayerDialog(
  exam: OnlineExam,
  student: Student,
  repository: PathsalaRepository,
  onDismiss: () -> Unit
) {
  val allQuestions by repository.questions.collectAsState()
  val questions = remember(exam.id, allQuestions) {
    repository.getQuestionsForExam(exam)
  }

  // Exam taking state
  var currentQIndex by remember { mutableIntStateOf(0) }
  val selectedAnswers = remember { mutableStateMapOf<String, String>() } // qId -> "A" | "B" | "C" | "D"
  val flaggedQuestions = remember { mutableStateListOf<String>() }
  val visitedQuestions = remember { mutableStateListOf<String>() }

  var secondsRemaining by remember(exam.durationMinutes) {
    mutableIntStateOf(exam.durationMinutes * 60)
  }
  var isSubmitted by remember { mutableStateOf(false) }
  var completedSubmission by remember { mutableStateOf<ExamSubmission?>(null) }
  var examStartedAt by remember { mutableLongStateOf(System.currentTimeMillis()) }

  var showExitConfirm by remember { mutableStateOf(false) }
  var showSubmitConfirm by remember { mutableStateOf(false) }

  // Record initial visit
  LaunchedEffect(currentQIndex, questions) {
    if (questions.isNotEmpty() && currentQIndex in questions.indices) {
      val qId = questions[currentQIndex].id
      if (qId !in visitedQuestions) {
        visitedQuestions.add(qId)
      }
    }
  }

  // Live Timer Countdown
  LaunchedEffect(isSubmitted, secondsRemaining) {
    if (!isSubmitted && secondsRemaining > 0) {
      delay(1000L)
      secondsRemaining -= 1
      if (secondsRemaining <= 0) {
        // Auto-submit when time expires
        isSubmitted = true
        val timeSpent = ((System.currentTimeMillis() - examStartedAt) / 1000L).toInt()
        val sub = calculateAndSaveSubmission(exam, student, questions, selectedAnswers, timeSpent, repository)
        completedSubmission = sub
      }
    }
  }

  Dialog(
    onDismissRequest = {
      if (isSubmitted) onDismiss() else showExitConfirm = true
    },
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
                color = Color.White,
                maxLines = 1
              )
              Text(
                text = "${exam.subject} • Candidate: ${student.name} (${student.rollNo})",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.8f)
              )
            }
          },
          navigationIcon = {
            IconButton(
              onClick = {
                if (isSubmitted) onDismiss() else showExitConfirm = true
              }
            ) {
              Icon(Icons.Default.Close, contentDescription = "Exit Exam", tint = Color.White)
            }
          },
          actions = {
            if (!isSubmitted) {
              val isLowTime = secondsRemaining <= 300
              Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isLowTime) Color(0xFFDC2626) else BrandPrimary,
                modifier = Modifier.padding(end = 12.dp)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = formatTimer(secondsRemaining),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                  )
                }
              }
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
          .background(Color(0xFFF8FAFC))
      ) {
        if (completedSubmission != null) {
          // --- RESULT & SCORECARD SCREEN ---
          ExamResultScorecardView(
            exam = exam,
            submission = completedSubmission!!,
            questions = questions,
            userAnswers = selectedAnswers,
            onClose = onDismiss
          )
        } else if (questions.isEmpty()) {
          // Empty State
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(Icons.Default.HelpOutline, contentDescription = null, tint = Slate400, modifier = Modifier.size(56.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("No Questions Attached", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Slate900)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              "No questions were found for this assessment. Please contact the administration or sync database.",
              fontSize = 13.sp,
              color = Slate600,
              textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)) {
              Text("Back to Exams")
            }
          }
        } else {
          // --- ACTIVE EXAM CBT VIEW ---
          val currentQuestion = questions.getOrNull(currentQIndex) ?: questions.first()

          Column(
            modifier = Modifier
              .fillMaxSize()
          ) {
            // Top Status & Meta Strip
            Surface(
              color = Color.White,
              tonalElevation = 2.dp,
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = BrandPrimary.copy(alpha = 0.12f)
                  ) {
                    Text(
                      text = "Q ${currentQIndex + 1} of ${questions.size}",
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold,
                      color = BrandPrimary,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                  }
                  Spacer(modifier = Modifier.width(8.dp))
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SuccessGreenLight
                  ) {
                    Text(
                      text = "+${currentQuestion.marks} Marks",
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      color = SuccessGreen,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                  }
                  if (currentQuestion.topic.isNotBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = currentQuestion.topic,
                      fontSize = 11.sp,
                      color = Slate600,
                      maxLines = 1
                    )
                  }
                }

                // Answered counter
                Text(
                  text = "${selectedAnswers.size}/${questions.size} Answered",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = if (selectedAnswers.size == questions.size) SuccessGreen else Slate600
                )
              }
            }

            // Scrollable Question & Options Body
            Column(
              modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
            ) {
              // Question Card
              Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(18.dp)) {
                  Text(
                    text = currentQuestion.questionText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate900,
                    lineHeight = 24.sp
                  )

                  // Optional Code Snippet
                  if (!currentQuestion.codeSnippet.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                      shape = RoundedCornerShape(8.dp),
                      color = Color(0xFF0F172A),
                      modifier = Modifier.fillMaxWidth()
                    ) {
                      Box(
                        modifier = Modifier
                          .horizontalScroll(rememberScrollState())
                          .padding(12.dp)
                      ) {
                        Text(
                          text = currentQuestion.codeSnippet!!,
                          color = Color(0xFF38BDF8),
                          fontFamily = FontFamily.Monospace,
                          fontSize = 13.sp,
                          lineHeight = 19.sp
                        )
                      }
                    }
                  }
                }
              }

              Spacer(modifier = Modifier.height(16.dp))
              Text("Choose Option:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate700)
              Spacer(modifier = Modifier.height(8.dp))

              // Options A, B, C, D
              val currentSelection = selectedAnswers[currentQuestion.id]
              val optionsList = listOf(
                "A" to currentQuestion.optionA,
                "B" to currentQuestion.optionB,
                "C" to currentQuestion.optionC,
                "D" to currentQuestion.optionD
              ).filter { it.second.isNotBlank() }

              optionsList.forEach { (key, text) ->
                val isSelected = currentSelection == key
                val isCorrectAnswerPreview = false // Never preview during active test

                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = if (isSelected) BrandPrimary.copy(alpha = 0.08f) else Color.White,
                  border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) BrandPrimary else Color(0xFFE2E8F0)
                  ),
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                      selectedAnswers[currentQuestion.id] = key
                    }
                    .testTag("option_${key.lowercase()}")
                ) {
                  Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Box(
                      modifier = Modifier
                        .size(32.dp)
                        .background(
                          color = if (isSelected) BrandPrimary else Slate100,
                          shape = CircleShape
                        ),
                      contentAlignment = Alignment.Center
                    ) {
                      Text(
                        text = key,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isSelected) Color.White else Slate700
                      )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                      text = text,
                      fontSize = 14.sp,
                      color = if (isSelected) Slate900 else Slate800,
                      fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                      modifier = Modifier.weight(1f)
                    )
                    if (isSelected) {
                      Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = BrandPrimary,
                        modifier = Modifier.size(20.dp)
                      )
                    }
                  }
                }
              }
            }

            // Question Navigation Palette Bar
            Surface(
              color = Color.White,
              tonalElevation = 4.dp,
              border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text("Question Palette", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate700)
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    val isFlagged = flaggedQuestions.contains(currentQuestion.id)
                    TextButton(
                      onClick = {
                        if (isFlagged) flaggedQuestions.remove(currentQuestion.id)
                        else flaggedQuestions.add(currentQuestion.id)
                      }
                    ) {
                      Icon(
                        imageVector = if (isFlagged) Icons.Default.Flag else Icons.Default.OutlinedFlag,
                        contentDescription = null,
                        tint = if (isFlagged) WarningAmber else Slate500,
                        modifier = Modifier.size(16.dp)
                      )
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(
                        text = if (isFlagged) "Flagged" else "Flag for Review",
                        fontSize = 11.sp,
                        color = if (isFlagged) WarningAmber else Slate600
                      )
                    }

                    if (selectedAnswers.containsKey(currentQuestion.id)) {
                      TextButton(onClick = { selectedAnswers.remove(currentQuestion.id) }) {
                        Text("Clear Choice", fontSize = 11.sp, color = Color(0xFFDC2626))
                      }
                    }
                  }
                }

                // Palette Chips 1..N
                LazyRow(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                  itemsIndexed(questions) { idx, q ->
                    val isCurrent = idx == currentQIndex
                    val isAnswered = selectedAnswers.containsKey(q.id)
                    val isFlagged = flaggedQuestions.contains(q.id)
                    val isVisited = visitedQuestions.contains(q.id)

                    val chipColor = when {
                      isAnswered -> SuccessGreen
                      isFlagged -> WarningAmber
                      isCurrent -> BrandPrimary
                      isVisited -> Slate300
                      else -> Slate100
                    }

                    val textColor = when {
                      isAnswered || isCurrent || isFlagged -> Color.White
                      else -> Slate700
                    }

                    Box(
                      modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(chipColor)
                        .clickable { currentQIndex = idx },
                      contentAlignment = Alignment.Center
                    ) {
                      Text(
                        text = "${idx + 1}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                      )
                    }
                  }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Prev/Next/Submit Controls
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  OutlinedButton(
                    onClick = {
                      if (currentQIndex > 0) currentQIndex -= 1
                    },
                    enabled = currentQIndex > 0,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                  ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Previous")
                  }

                  if (currentQIndex < questions.size - 1) {
                    Button(
                      onClick = { currentQIndex += 1 },
                      modifier = Modifier.weight(1f),
                      shape = RoundedCornerShape(10.dp),
                      colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                    ) {
                      Text("Next")
                      Spacer(modifier = Modifier.width(6.dp))
                      Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                  } else {
                    Button(
                      onClick = { showSubmitConfirm = true },
                      modifier = Modifier.weight(1f),
                      shape = RoundedCornerShape(10.dp),
                      colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                    ) {
                      Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text("Submit Exam", fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  // Confirm Submit Dialog
  if (showSubmitConfirm) {
    AlertDialog(
      onDismissRequest = { showSubmitConfirm = false },
      title = { Text("Submit Exam?", fontWeight = FontWeight.Bold) },
      text = {
        Column {
          Text("Are you ready to submit your exam responses?")
          Spacer(modifier = Modifier.height(10.dp))
          Surface(
            color = Slate50,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text("• Total Questions: ${questions.size}", fontSize = 13.sp)
              Text("• Answered: ${selectedAnswers.size}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SuccessGreen)
              Text("• Unanswered: ${questions.size - selectedAnswers.size}", fontSize = 13.sp, color = if (selectedAnswers.size < questions.size) Color(0xFFDC2626) else Slate600)
              if (flaggedQuestions.isNotEmpty()) {
                Text("• Flagged for Review: ${flaggedQuestions.size}", fontSize = 13.sp, color = WarningAmber)
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            showSubmitConfirm = false
            isSubmitted = true
            val timeSpent = ((System.currentTimeMillis() - examStartedAt) / 1000L).toInt()
            val sub = calculateAndSaveSubmission(exam, student, questions, selectedAnswers, timeSpent, repository)
            completedSubmission = sub
          },
          colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
        ) {
          Text("Confirm & Submit")
        }
      },
      dismissButton = {
        TextButton(onClick = { showSubmitConfirm = false }) {
          Text("Continue Test")
        }
      }
    )
  }

  // Confirm Exit Dialog
  if (showExitConfirm) {
    AlertDialog(
      onDismissRequest = { showExitConfirm = false },
      title = { Text("Exit Online Assessment?", fontWeight = FontWeight.Bold) },
      text = {
        Text("If you exit now, any unsaved responses will be discarded. Are you sure you want to quit?")
      },
      confirmButton = {
        Button(
          onClick = {
            showExitConfirm = false
            onDismiss()
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
        ) {
          Text("Exit Exam")
        }
      },
      dismissButton = {
        TextButton(onClick = { showExitConfirm = false }) {
          Text("Stay & Complete")
        }
      }
    )
  }
}

// Calculate score, record submission, and synchronize with repository
private fun calculateAndSaveSubmission(
  exam: OnlineExam,
  student: Student,
  questions: List<ExamQuestion>,
  userAnswers: Map<String, String>,
  timeSpentSeconds: Int,
  repository: PathsalaRepository
): ExamSubmission {
  var totalScore = 0
  val maxMarks = if (exam.totalMarks > 0) exam.totalMarks else questions.sumOf { it.marks }

  val answersJsonObj = JSONObject()

  for (q in questions) {
    val userAns = userAnswers[q.id]?.uppercase()
    answersJsonObj.put(q.id, userAns ?: "")
    if (userAns != null && userAns == q.correctOption.uppercase()) {
      totalScore += q.marks
    }
  }

  val percentage = if (maxMarks > 0) (totalScore * 100) / maxMarks else 0
  val passed = percentage >= exam.passingPercentage

  val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)
  val submission = ExamSubmission(
    id = "sub-${System.currentTimeMillis()}",
    examId = exam.id,
    studentId = student.id,
    studentName = student.name,
    score = totalScore,
    totalMarks = maxMarks,
    submittedAt = sdf.format(Date()),
    percentage = percentage,
    passed = passed,
    timeSpentSeconds = timeSpentSeconds,
    answersJson = answersJsonObj.toString()
  )

  repository.submitExamSubmission(submission)
  return submission
}

private fun formatTimer(secs: Int): String {
  val m = secs / 60
  val s = secs % 60
  return "%02d:%02d".format(m, s)
}

@Composable
private fun ExamResultScorecardView(
  exam: OnlineExam,
  submission: ExamSubmission,
  questions: List<ExamQuestion>,
  userAnswers: Map<String, String>,
  onClose: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(18.dp)
  ) {
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = Color.White),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Box(
          modifier = Modifier
            .size(64.dp)
            .background(
              color = if (submission.passed) SuccessGreenLight else Color(0xFFFEE2E2),
              shape = CircleShape
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = if (submission.passed) Icons.Default.EmojiEvents else Icons.Default.Info,
            contentDescription = null,
            tint = if (submission.passed) SuccessGreen else Color(0xFFDC2626),
            modifier = Modifier.size(36.dp)
          )
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(
          text = if (submission.passed) "Congratulations! You Passed!" else "Assessment Completed",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = Slate900
        )
        Text(
          text = exam.title,
          fontSize = 13.sp,
          color = Slate600,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Large Score Display
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = BrandPrimary.copy(alpha = 0.08f),
          border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.2f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text("Score", fontSize = 12.sp, color = Slate600)
              Text(
                "${submission.score} / ${submission.totalMarks}",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = BrandPrimary
              )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text("Percentage", fontSize = 12.sp, color = Slate600)
              Text(
                "${submission.percentage}%",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (submission.passed) SuccessGreen else Color(0xFFDC2626)
              )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text("Status", fontSize = 12.sp, color = Slate600)
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (submission.passed) SuccessGreenLight else Color(0xFFFEE2E2)
              ) {
                Text(
                  text = if (submission.passed) "PASSED" else "NEEDS WORK",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (submission.passed) SuccessGreen else Color(0xFFDC2626),
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))
    Text(
      text = "Question-by-Question Review (${questions.size} Questions)",
      fontSize = 15.sp,
      fontWeight = FontWeight.Bold,
      color = Slate900
    )
    Spacer(modifier = Modifier.height(8.dp))

    // Review items
    questions.forEachIndexed { idx, q ->
      val userAns = userAnswers[q.id]?.uppercase()
      val correctAns = q.correctOption.uppercase()
      val isCorrect = userAns == correctAns

      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, if (isCorrect) SuccessGreen.copy(alpha = 0.5f) else Color(0xFFFECACA)),
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 6.dp)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Question ${idx + 1}",
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
              color = Slate900
            )
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = if (isCorrect) SuccessGreenLight else Color(0xFFFEE2E2)
            ) {
              Text(
                text = if (isCorrect) "✓ Correct (+${q.marks})" else if (userAns == null) "Skipped (0)" else "✗ Incorrect (0)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCorrect) SuccessGreen else Color(0xFFDC2626),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))
          Text(text = q.questionText, fontSize = 13.sp, color = Slate800)

          if (!q.codeSnippet.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = Color(0xFF0F172A),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = q.codeSnippet!!,
                color = Color(0xFF38BDF8),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                modifier = Modifier.padding(8.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))
          Row {
            Text("Your Answer: ", fontSize = 12.sp, color = Slate600)
            Text(
              text = if (userAns != null) "Option $userAns" else "None",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = if (isCorrect) SuccessGreen else Color(0xFFDC2626)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text("Correct: ", fontSize = 12.sp, color = Slate600)
            Text("Option $correctAns", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
          }

          if (!q.explanation.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = Slate50,
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(modifier = Modifier.padding(8.dp)) {
                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(q.explanation!!, fontSize = 11.sp, color = Slate700)
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))
    Button(
      onClick = onClose,
      colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
    ) {
      Text("Back to Student Portal", fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
  }
}
