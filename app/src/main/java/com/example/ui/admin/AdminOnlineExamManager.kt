package com.example.ui.admin

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminOnlineExamManager(
  exams: List<OnlineExam>,
  questions: List<ExamQuestion>,
  submissions: List<ExamSubmission>,
  courses: List<Course>,
  batches: List<Batch>,
  onAddExam: (OnlineExam) -> Unit,
  onUpdateExam: (OnlineExam) -> Unit,
  onDeleteExam: (String) -> Unit,
  onToggleExamPublish: (String) -> Unit,
  onAddQuestion: (ExamQuestion) -> Unit,
  onDeleteQuestion: (String) -> Unit,
  onDeleteSubmission: (String) -> Unit
) {
  val context = LocalContext.current
  var activeSection by remember { mutableIntStateOf(0) } // 0: Exams, 1: Question Bank, 2: Submissions

  var showCreateExamModal by remember { mutableStateOf(false) }
  var showCreateQuestionModal by remember { mutableStateOf(false) }

  Column(modifier = Modifier.fillMaxSize()) {
    // Header & Section Filter Chips
    Surface(
      color = Color.White,
      tonalElevation = 2.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "Online Exam & CBT Assessment System",
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              color = Slate900
            )
            Text(
              text = "Cross-Platform Exam Engine (Matches Web Project)",
              fontSize = 11.sp,
              color = Slate600
            )
          }

          if (activeSection == 0) {
            Button(
              onClick = { showCreateExamModal = true },
              colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.testTag("create_exam_button")
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Create Exam", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          } else if (activeSection == 1) {
            Button(
              onClick = { showCreateQuestionModal = true },
              colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.testTag("add_question_button")
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Add Question", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          FilterChip(
            selected = activeSection == 0,
            onClick = { activeSection = 0 },
            leadingIcon = { Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(16.dp)) },
            label = { Text("Exams (${exams.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BrandPrimary, selectedLabelColor = Color.White),
            modifier = Modifier.weight(1f)
          )
          FilterChip(
            selected = activeSection == 1,
            onClick = { activeSection = 1 },
            leadingIcon = { Icon(Icons.Default.FormatListNumbered, contentDescription = null, modifier = Modifier.size(16.dp)) },
            label = { Text("Questions (${questions.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BrandPrimary, selectedLabelColor = Color.White),
            modifier = Modifier.weight(1f)
          )
          FilterChip(
            selected = activeSection == 2,
            onClick = { activeSection = 2 },
            leadingIcon = { Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null, modifier = Modifier.size(16.dp)) },
            label = { Text("Submissions (${submissions.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BrandPrimary, selectedLabelColor = Color.White),
            modifier = Modifier.weight(1f)
          )
        }
      }
    }

    // Section Content
    when (activeSection) {
      0 -> AdminExamsListView(
        exams = exams,
        questions = questions,
        courses = courses,
        batches = batches,
        onTogglePublish = onToggleExamPublish,
        onDelete = onDeleteExam
      )
      1 -> AdminQuestionsListView(
        questions = questions,
        onDelete = onDeleteQuestion
      )
      2 -> AdminSubmissionsListView(
        submissions = submissions,
        exams = exams,
        onDelete = onDeleteSubmission
      )
    }
  }

  // --- CREATE EXAM DIALOG ---
  if (showCreateExamModal) {
    CreateExamDialog(
      questions = questions,
      courses = courses,
      batches = batches,
      onDismiss = { showCreateExamModal = false },
      onSave = { exam ->
        onAddExam(exam)
        Toast.makeText(context, "Exam '${exam.title}' published successfully!", Toast.LENGTH_SHORT).show()
        showCreateExamModal = false
      }
    )
  }

  // --- CREATE QUESTION DIALOG ---
  if (showCreateQuestionModal) {
    CreateQuestionDialog(
      courses = courses,
      onDismiss = { showCreateQuestionModal = false },
      onSave = { q ->
        onAddQuestion(q)
        Toast.makeText(context, "Question added to Question Bank!", Toast.LENGTH_SHORT).show()
        showCreateQuestionModal = false
      }
    )
  }
}

@Composable
private fun AdminExamsListView(
  exams: List<OnlineExam>,
  questions: List<ExamQuestion>,
  courses: List<Course>,
  batches: List<Batch>,
  onTogglePublish: (String) -> Unit,
  onDelete: (String) -> Unit
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize().padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    if (exams.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("No online exams created yet. Tap '+ Create Exam' to publish one.", modifier = Modifier.padding(16.dp), color = Slate600, fontSize = 13.sp)
        }
      }
    } else {
      items(exams) { exam ->
        val attachedQCount = if (!exam.questionIds.isNullOrBlank()) {
          exam.questionIds.split(",", ";", " ").filter { it.isNotBlank() }.size
        } else 0

        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(exam.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Slate900)
                Text(
                  text = "Code: ${exam.examCode.ifBlank { exam.id }} • Subject: ${exam.subject}",
                  fontSize = 12.sp,
                  color = Slate600
                )
              }

              Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (exam.isPublished) SuccessGreenLight else Slate100
              ) {
                Text(
                  text = if (exam.isPublished) "PUBLISHED" else "DRAFT",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (exam.isPublished) SuccessGreen else Slate600,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Slate50,
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text("⏱ ${exam.durationMinutes} Mins", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
                Text("🎯 ${exam.totalMarks} Marks (Pass: ${exam.passingMarks})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
                Text("📝 $attachedQCount Questions", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = BrandPrimary)
              }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Published", fontSize = 12.sp, color = Slate600)
                Spacer(modifier = Modifier.width(6.dp))
                Switch(
                  checked = exam.isPublished,
                  onCheckedChange = { onTogglePublish(exam.id) },
                  colors = SwitchDefaults.colors(checkedThumbColor = SuccessGreen, checkedTrackColor = SuccessGreenLight)
                )
              }

              IconButton(
                onClick = { onDelete(exam.id) }
              ) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Exam", tint = Color(0xFFDC2626))
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AdminQuestionsListView(
  questions: List<ExamQuestion>,
  onDelete: (String) -> Unit
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize().padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    if (questions.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Question Bank is empty. Tap '+ Add Question' to create questions.", modifier = Modifier.padding(16.dp), color = Slate600, fontSize = 13.sp)
        }
      }
    } else {
      items(questions) { q ->
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = BrandPrimary.copy(alpha = 0.1f)
                ) {
                  Text(
                    text = q.subject,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandPrimary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text("+${q.marks} Marks", fontSize = 11.sp, color = SuccessGreen, fontWeight = FontWeight.Bold)
              }

              IconButton(onClick = { onDelete(q.id) }) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
              }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(q.questionText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Slate900)

            if (!q.codeSnippet.isNullOrBlank()) {
              Spacer(modifier = Modifier.height(6.dp))
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF0F172A),
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = q.codeSnippet!!,
                  fontFamily = FontFamily.Monospace,
                  fontSize = 12.sp,
                  color = Color(0xFF38BDF8),
                  modifier = Modifier.padding(8.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(8.dp))
            // Options
            listOf(
              "A" to q.optionA,
              "B" to q.optionB,
              "C" to q.optionC,
              "D" to q.optionD
            ).forEach { (optKey, optText) ->
              val isCorrect = optKey.equals(q.correctOption, ignoreCase = true)
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "$optKey) $optText",
                  fontSize = 12.sp,
                  color = if (isCorrect) SuccessGreen else Slate700,
                  fontWeight = if (isCorrect) FontWeight.Bold else FontWeight.Normal,
                  modifier = Modifier.weight(1f)
                )
                if (isCorrect) {
                  Icon(Icons.Default.CheckCircle, contentDescription = "Correct", tint = SuccessGreen, modifier = Modifier.size(16.dp))
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AdminSubmissionsListView(
  submissions: List<ExamSubmission>,
  exams: List<OnlineExam>,
  onDelete: (String) -> Unit
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize().padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    if (submissions.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("No student submissions recorded yet.", modifier = Modifier.padding(16.dp), color = Slate600, fontSize = 13.sp)
        }
      }
    } else {
      items(submissions.reversed()) { sub ->
        val exam = exams.firstOrNull { it.id == sub.examId }
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(sub.studentName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Slate900)
                Text(exam?.title ?: "Assessment: ${sub.examId}", fontSize = 12.sp, color = Slate600)
              }
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (sub.passed) SuccessGreenLight else Color(0xFFFEE2E2)
              ) {
                Text(
                  text = if (sub.passed) "PASSED (${sub.percentage}%)" else "NEEDS WORK (${sub.percentage}%)",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (sub.passed) SuccessGreen else Color(0xFFDC2626),
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Score: ${sub.score} / ${sub.totalMarks} • ${sub.submittedAt}",
                fontSize = 12.sp,
                color = Slate600
              )
              IconButton(onClick = { onDelete(sub.id) }) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
              }
            }
          }
        }
      }
    }
  }
}

// --- MODAL: CREATE EXAM ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateExamDialog(
  questions: List<ExamQuestion>,
  courses: List<Course>,
  batches: List<Batch>,
  onDismiss: () -> Unit,
  onSave: (OnlineExam) -> Unit
) {
  var title by remember { mutableStateOf("") }
  var examCode by remember { mutableStateOf("EXAM-${System.currentTimeMillis().toString().takeLast(4)}") }
  var subject by remember { mutableStateOf("Computer Science & Python") }
  var durationMins by remember { mutableStateOf("30") }
  var totalMarks by remember { mutableStateOf("10") }
  var passingMarks by remember { mutableStateOf("4") }
  var selectedCourseId by remember { mutableStateOf(courses.firstOrNull()?.id ?: "all") }
  var selectedBatchId by remember { mutableStateOf(batches.firstOrNull()?.id ?: "all") }
  val selectedQIds = remember { mutableStateListOf<String>() }
  var instructions by remember { mutableStateOf("1. All questions are compulsory.\n2. Timer will auto-submit when remaining time reaches 00:00.") }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = Color.White),
      modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(20.dp)
      ) {
        Text("Create Online Exam", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Slate900)
        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Exam Title") },
          placeholder = { Text("e.g. Periodic Assessment 1") },
          modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = examCode,
            onValueChange = { examCode = it },
            label = { Text("Exam Code") },
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = subject,
            onValueChange = { subject = it },
            label = { Text("Subject") },
            modifier = Modifier.weight(1f)
          )
        }
        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = durationMins,
            onValueChange = { durationMins = it },
            label = { Text("Duration (Mins)") },
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = totalMarks,
            onValueChange = { totalMarks = it },
            label = { Text("Total Marks") },
            modifier = Modifier.weight(1f)
          )
        }
        Spacer(modifier = Modifier.height(14.dp))

        Text("Select Questions to Attach (${selectedQIds.size} Selected):", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))

        // Selectable questions list
        Surface(
          border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp)
        ) {
          Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(8.dp)) {
            if (questions.isEmpty()) {
              Text("No questions in bank. Please add questions first.", fontSize = 12.sp, color = Slate600)
            } else {
              questions.forEach { q ->
                val isChecked = selectedQIds.contains(q.id)
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                      if (isChecked) selectedQIds.remove(q.id) else selectedQIds.add(q.id)
                    }
                    .padding(vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Checkbox(
                    checked = isChecked,
                    onCheckedChange = {
                      if (it) selectedQIds.add(q.id) else selectedQIds.remove(q.id)
                    }
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(q.questionText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text("${q.subject} • +${q.marks} Marks", fontSize = 10.sp, color = Slate600)
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))
        OutlinedTextField(
          value = instructions,
          onValueChange = { instructions = it },
          label = { Text("Instructions") },
          modifier = Modifier.fillMaxWidth(),
          minLines = 2
        )

        Spacer(modifier = Modifier.height(18.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          TextButton(onClick = onDismiss) { Text("Cancel") }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              if (title.isBlank()) return@Button
              val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
              val exam = OnlineExam(
                id = "exam-${System.currentTimeMillis()}",
                title = title.trim(),
                examCode = examCode.trim(),
                subject = subject.trim(),
                courseId = selectedCourseId,
                batchId = selectedBatchId,
                durationMinutes = durationMins.toIntOrNull() ?: 30,
                totalMarks = totalMarks.toIntOrNull() ?: (selectedQIds.size * 2),
                passingMarks = passingMarks.toIntOrNull() ?: 4,
                questionIds = selectedQIds.joinToString(", "),
                instructions = instructions.trim(),
                scheduledDate = sdfDate,
                startTime = "10:00 AM",
                endTime = "11:00 AM",
                isPublished = true,
                status = "active",
                createdAt = sdfDate
              )
              onSave(exam)
            },
            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
          ) {
            Text("Publish Exam")
          }
        }
      }
    }
  }
}

// --- MODAL: CREATE QUESTION ---
@Composable
private fun CreateQuestionDialog(
  courses: List<Course>,
  onDismiss: () -> Unit,
  onSave: (ExamQuestion) -> Unit
) {
  var questionText by remember { mutableStateOf("") }
  var codeSnippet by remember { mutableStateOf("") }
  var subject by remember { mutableStateOf("C Programming") }
  var topic by remember { mutableStateOf("Pointers & Arrays") }
  var marks by remember { mutableStateOf("2") }
  var optA by remember { mutableStateOf("") }
  var optB by remember { mutableStateOf("") }
  var optC by remember { mutableStateOf("") }
  var optD by remember { mutableStateOf("") }
  var correctOpt by remember { mutableStateOf("A") }
  var explanation by remember { mutableStateOf("") }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = Color.White),
      modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(20.dp)
      ) {
        Text("Add MCQ Question", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Slate900)
        Spacer(modifier = Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = subject,
            onValueChange = { subject = it },
            label = { Text("Subject") },
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = topic,
            onValueChange = { topic = it },
            label = { Text("Topic") },
            modifier = Modifier.weight(1f)
          )
        }
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = questionText,
          onValueChange = { questionText = it },
          label = { Text("Question Statement") },
          placeholder = { Text("Enter question...") },
          modifier = Modifier.fillMaxWidth(),
          minLines = 2
        )
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = codeSnippet,
          onValueChange = { codeSnippet = it },
          label = { Text("Optional Code Snippet") },
          placeholder = { Text("int x = 10; printf(\"%d\", x);") },
          modifier = Modifier.fillMaxWidth(),
          minLines = 2
        )
        Spacer(modifier = Modifier.height(14.dp))

        Text("Four MCQ Options:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(value = optA, onValueChange = { optA = it }, label = { Text("Option A") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(value = optB, onValueChange = { optB = it }, label = { Text("Option B") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(value = optC, onValueChange = { optC = it }, label = { Text("Option C") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(value = optD, onValueChange = { optD = it }, label = { Text("Option D") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(10.dp))

        Text("Correct Option:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceAround
        ) {
          listOf("A", "B", "C", "D").forEach { opt ->
            Row(verticalAlignment = Alignment.CenterVertically) {
              RadioButton(
                selected = correctOpt == opt,
                onClick = { correctOpt = opt }
              )
              Text("Option $opt", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = explanation,
          onValueChange = { explanation = it },
          label = { Text("Explanation (Shown after exam)") },
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(18.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          TextButton(onClick = onDismiss) { Text("Cancel") }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              if (questionText.isBlank() || optA.isBlank() || optB.isBlank()) return@Button
              val q = ExamQuestion(
                id = "q-${System.currentTimeMillis()}",
                questionText = questionText.trim(),
                codeSnippet = codeSnippet.ifBlank { null },
                optionA = optA.trim(),
                optionB = optB.trim(),
                optionC = optC.trim(),
                optionD = optD.trim(),
                correctOption = correctOpt,
                marks = marks.toIntOrNull() ?: 2,
                subject = subject.trim(),
                topic = topic.trim(),
                explanation = explanation.ifBlank { null },
                createdAt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
              )
              onSave(q)
            },
            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
          ) {
            Text("Save Question")
          }
        }
      }
    }
  }
}
