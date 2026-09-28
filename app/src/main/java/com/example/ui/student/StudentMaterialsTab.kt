package com.example.ui.student

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.data.model.Student
import com.example.data.model.StudyMaterial
import com.example.data.repository.PathsalaRepository
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*

@Composable
fun StudentMaterialsTab(
  student: Student,
  repository: PathsalaRepository
) {
  val context = LocalContext.current
  val materials by repository.studyMaterials.collectAsState()
  val batches by repository.batches.collectAsState()

  val studentBatch = batches.find { it.id == student.batchId }

  // Filter materials for this student's batch or all-batch materials
  val studentMaterials = materials.filter { it.batchId == "all" || it.batchId == student.batchId }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      SectionHeader(
        title = "Study Materials & Notes",
        subtitle = "Syllabus guides, formula sheets & assignments for ${studentBatch?.name ?: "your batch"}"
      )
    }

    if (studentMaterials.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(36.dp))
            Text("No study materials uploaded yet", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("Your faculty will upload lecture PDFs and worksheets here.", fontSize = 12.sp, color = SlateTextSecondary)
          }
        }
      }
    } else {
      items(studentMaterials) { mat ->
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Surface(
                color = when (mat.fileType) {
                  "pdf" -> Color(0xFFFEF2F2)
                  "doc" -> Color(0xFFEFF6FF)
                  "zip" -> Color(0xFFFFFBEB)
                  else -> Color(0xFFF1F5F9)
                },
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(
                  1.dp,
                  when (mat.fileType) {
                    "pdf" -> Color(0xFFFECACA)
                    "doc" -> Color(0xFFBFDBFE)
                    "zip" -> Color(0xFFFDE68A)
                    else -> SlateBorder
                  }
                )
              ) {
                Text(
                  text = mat.fileType.uppercase(),
                  color = when (mat.fileType) {
                    "pdf" -> Color(0xFFDC2626)
                    "doc" -> Color(0xFF2563EB)
                    "zip" -> Color(0xFFD97706)
                    else -> Color(0xFF475569)
                  },
                  fontWeight = FontWeight.Black,
                  fontSize = 10.sp,
                  fontFamily = FontFamily.Monospace,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
              }

              Text(
                text = mat.uploadedDate,
                fontSize = 11.sp,
                color = SlateTextSecondary,
                fontFamily = FontFamily.Monospace
              )
            }

            Text(
              text = mat.title,
              fontWeight = FontWeight.Black,
              fontSize = 15.sp,
              color = MaterialTheme.colorScheme.onSurface
            )

            if (!mat.subject.isNullOrBlank()) {
              Surface(
                color = Color(0xFFEEF2FF),
                shape = RoundedCornerShape(6.dp)
              ) {
                Text(
                  text = mat.subject,
                  color = IndigoPrimary,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }

            if (!mat.description.isNullOrBlank()) {
              Text(
                text = mat.description,
                fontSize = 12.sp,
                color = SlateTextSecondary,
                lineHeight = 16.sp
              )
            }

            HorizontalDivider(color = SlateBorder)

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = "By ${mat.uploadedBy}",
                  fontSize = 11.sp,
                  color = SlateTextPrimary,
                  fontWeight = FontWeight.Medium
                )
                Text(
                  text = mat.fileSize ?: "Digital File",
                  fontSize = 10.sp,
                  color = SlateTextSecondary,
                  fontFamily = FontFamily.Monospace
                )
              }

              Button(
                onClick = {
                  val url = mat.fileUrl
                  if (url.isNotBlank() && (url.startsWith("http://") || url.startsWith("https://"))) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                  } else {
                    Toast.makeText(context, "Opening ${mat.fileName}...", Toast.LENGTH_SHORT).show()
                  }
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
              ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Download", fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }
  }
}
