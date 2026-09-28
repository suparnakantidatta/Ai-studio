package com.example.ui.student

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ExamQuestion
import com.example.data.model.ExamSubmission
import com.example.data.model.OnlineExam
import com.example.data.model.Student
import com.example.data.repository.PathsalaRepository
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun StudentExamsTab(
  student: Student,
  repository: PathsalaRepository
) {
  val exams by repository.exams.collectAsState()
  val questions by repository.questions.collectAsState()
  val submissions by repository.submissions.collectAsState()

  var activeExamToTake by remember { mutableStateOf<OnlineExam?>(null) }
  var selectedSubmissionToReview by remember { mutableStateOf<ExamSubmission?>(null) }

  // Filter exams for student
  val studentExams = exams.filter { it.isPublished }
  val studentSubmissions = submissions.filter { it.studentId == student.id }

  val currentExam = activeExamToTake
  if (currentExam != null) {
    // Interactive Test Runner Mode
    ActiveExamRunner(
      exam = currentExam,
      student = student,
      allQuestions = questions,
      onFinish = { score, total, pct, passed, timeSpent ->
        val sub = repository.submitExamAttempt(
          examId = currentExam.id,
          studentId = student.id,
          score = score,
          totalMarks = total,
          percentage = pct,
          passed = passed,
          timeSpentSeconds = timeSpent
        )
        activeExamToTake = null
        selectedSubmissionToReview = sub
      },
      onCancel = { activeExamToTake = null }
    )
    return
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      SectionHeader(
        title = "Scheduled Online Tests",
        subtitle = "Timed MCQ assessments mapped to your batch & syllabus"
      )
    }

    items(studentExams) { exam ->
      val lastSubmission = studentSubmissions.find { it.examId == exam.id }

      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              color = Color(0xFFEEF2FF),
              shape = RoundedCornerShape(8.dp)
            ) {
              Text(
                text = exam.examCode,
                color = IndigoPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }

            if (lastSubmission != null) {
              Surface(
                color = if (lastSubmission.passed) EmeraldLight else Color(0xFFFEF2F2),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (lastSubmission.passed) Color(0xFFA7F3D0) else Color(0xFFFECACA))
              ) {
                Text(
                  text = "SCORE: ${lastSubmission.score}/${lastSubmission.totalMarks} (${lastSubmission.percentage}%)",
                  color = if (lastSubmission.passed) Color(0xFF047857) else Color(0xFFB91C1C),
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.sp,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
              }
            } else {
              StatusBadge(status = exam.status)
            }
          }

          Text(
            text = exam.title,
            fontWeight = FontWeight.Black,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
          )

          // Meta row
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(Color(0xFFF8FAFC))
              .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text(text = "Subject", fontSize = 10.sp, color = SlateTextSecondary)
              Text(text = exam.subject, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SlateTextPrimary)
            }
            Column {
              Text(text = "Duration", fontSize = 10.sp, color = SlateTextSecondary)
              Text(text = "${exam.durationMinutes} Mins", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SlateTextPrimary)
            }
            Column {
              Text(text = "Total Marks", fontSize = 10.sp, color = SlateTextSecondary)
              Text(text = "${exam.totalMarks} (Pass: ${exam.passingMarks})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SlateTextPrimary)
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Window: ${exam.scheduledDate} (${exam.startTime})",
              fontSize = 11.sp,
              color = SlateTextSecondary
            )

            if (lastSubmission != null) {
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                  onClick = { selectedSubmissionToReview = lastSubmission },
                  shape = RoundedCornerShape(10.dp),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                  Text("Scorecard", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                  onClick = { activeExamToTake = exam },
                  shape = RoundedCornerShape(10.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                  Text("Retake", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }
            } else {
              Button(
                onClick = { activeExamToTake = exam },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
              ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Start Test", fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }
  }

  // Scorecard Dialog
  selectedSubmissionToReview?.let { sub ->
    ScorecardDialog(
      submission = sub,
      onDismiss = { selectedSubmissionToReview = null }
    )
  }
}

@Composable
fun ActiveExamRunner(
  exam: OnlineExam,
  student: Student,
  allQuestions: List<ExamQuestion>,
  onFinish: (score: Int, total: Int, percentage: Int, passed: Boolean, timeSpentSecs: Int) -> Unit,
  onCancel: () -> Unit
) {
  val examQuestions = remember {
    exam.questionIds.mapNotNull { id -> allQuestions.find { it.id == id } }
  }

  var currentIdx by remember { mutableIntStateOf(0) }
  val answers = remember { mutableStateMapOf<String, Int>() }
  var secondsLeft by remember { mutableIntStateOf(exam.durationMinutes * 60) }
  var timeSpent by remember { mutableIntStateOf(0) }

  // Countdown timer
  LaunchedEffect(key1 = exam.id) {
    while (secondsLeft > 0) {
      delay(1000)
      secondsLeft--
      timeSpent++
    }
    // Auto-submit when time expires
    submitAnswers(exam, examQuestions, answers, timeSpent, onFinish)
  }

  val currentQ = examQuestions.getOrNull(currentIdx)

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(SlateBackground)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // Top Exam Header with Timer & Submit
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(text = exam.title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
          Text(text = "Question ${currentIdx + 1} of ${examQuestions.size}", color = Color(0xFF94A3B8), fontSize = 11.sp)
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Timer
          Surface(
            color = if (secondsLeft < 180) Color(0xFFE11D48) else Color(0xFF334155),
            shape = RoundedCornerShape(8.dp)
          ) {
            val mins = secondsLeft / 60
            val secs = secondsLeft % 60
            Text(
              text = String.format("%02d:%02d", mins, secs),
              color = Color.White,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              fontSize = 14.sp,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }

          Button(
            onClick = {
              submitAnswers(exam, examQuestions, answers, timeSpent, onFinish)
            },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
          ) {
            Text("Finish", fontWeight = FontWeight.Bold, fontSize = 12.sp)
          }
        }
      }
    }

    if (currentQ != null) {
      // Question Card
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "Question ${currentIdx + 1}",
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              color = IndigoPrimary
            )
            Text(
              text = "${currentQ.marks} Marks",
              fontWeight = FontWeight.Bold,
              fontSize = 11.sp,
              color = SlateTextSecondary
            )
          }

          Text(
            text = currentQ.questionText,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 22.sp
          )

          if (!currentQ.codeSnippet.isNullOrBlank()) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0F172A))
                .padding(12.dp)
            ) {
              Text(
                text = currentQ.codeSnippet,
                color = Color(0xFF38BDF8),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
              )
            }
          }

          Spacer(modifier = Modifier.weight(1f))

          // 4 Options
          currentQ.options.forEachIndexed { optIndex, optText ->
            val isSelected = answers[currentQ.id] == optIndex
            val optLetter = ('A' + optIndex).toString()

            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (isSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC))
                .border(
                  width = if (isSelected) 2.dp else 1.dp,
                  color = if (isSelected) IndigoPrimary else Color(0xFFCBD5E1),
                  shape = RoundedCornerShape(12.dp)
                )
                .clickable { answers[currentQ.id] = optIndex }
                .padding(12.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) IndigoPrimary else Color(0xFFE2E8F0)),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = optLetter,
                    color = if (isSelected) Color.White else Color(0xFF334155),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                  )
                }
                Text(
                  text = optText,
                  fontSize = 13.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                  color = if (isSelected) Color(0xFF1E1B4B) else Color(0xFF0F172A)
                )
              }
            }
          }
        }
      }

      // Bottom Navigation Buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedButton(
          onClick = { if (currentIdx > 0) currentIdx-- },
          enabled = currentIdx > 0,
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Prev")
        }

        Text(
          text = "${answers.size}/${examQuestions.size} Answered",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = SlateTextSecondary
        )

        Button(
          onClick = {
            if (currentIdx < examQuestions.size - 1) {
              currentIdx++
            } else {
              submitAnswers(exam, examQuestions, answers, timeSpent, onFinish)
            }
          },
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
        ) {
          Text(if (currentIdx < examQuestions.size - 1) "Next" else "Finish")
          Spacer(modifier = Modifier.width(4.dp))
          Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
        }
      }
    }
  }
}

private fun submitAnswers(
  exam: OnlineExam,
  questions: List<ExamQuestion>,
  answers: Map<String, Int>,
  timeSpentSecs: Int,
  onFinish: (score: Int, total: Int, percentage: Int, passed: Boolean, timeSpentSecs: Int) -> Unit
) {
  var score = 0
  var total = 0

  questions.forEach { q ->
    total += q.marks
    val chosen = answers[q.id]
    if (chosen != null && chosen == q.correctOptionIndex) {
      score += q.marks
    }
  }

  val percentage = if (total > 0) (score * 100) / total else 0
  val passed = percentage >= exam.passingPercentage

  onFinish(score, total, percentage, passed, timeSpentSecs)
}

@Composable
fun ScorecardDialog(
  submission: ExamSubmission,
  onDismiss: () -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = Color.White),
      border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Box(
          modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(if (submission.passed) EmeraldLight else Color(0xFFFEF2F2)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = if (submission.passed) Icons.Default.EmojiEvents else Icons.Default.Cancel,
            contentDescription = null,
            tint = if (submission.passed) EmeraldSuccess else Color(0xFFDC2626),
            modifier = Modifier.size(32.dp)
          )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = if (submission.passed) "Congratulations!" else "Keep Practicing!",
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = if (submission.passed) Color(0xFF065F46) else Color(0xFF991B1B)
          )
          Text(
            text = submission.examTitle,
            fontSize = 12.sp,
            color = SlateTextSecondary,
            modifier = Modifier.padding(top = 2.dp)
          )
        }

        // Score summary
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF8FAFC))
            .padding(14.dp),
          horizontalArrangement = Arrangement.SpaceAround
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Score", fontSize = 11.sp, color = SlateTextSecondary)
            Text("${submission.score}/${submission.totalMarks}", fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
          }
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Percentage", fontSize = 11.sp, color = SlateTextSecondary)
            Text("${submission.percentage}%", fontSize = 18.sp, fontWeight = FontWeight.Black, color = IndigoPrimary, fontFamily = FontFamily.Monospace)
          }
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Result", fontSize = 11.sp, color = SlateTextSecondary)
            Text(if (submission.passed) "PASSED" else "NEEDS WORK", fontSize = 14.sp, fontWeight = FontWeight.Black, color = if (submission.passed) EmeraldSuccess else Color(0xFFDC2626))
          }
        }

        Button(
          onClick = onDismiss,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Done", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
