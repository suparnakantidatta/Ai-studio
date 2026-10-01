package com.example.ui.admin

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.data.model.FeePayment
import com.example.data.model.Student
import com.example.data.repository.PathsalaRepository
import com.example.ui.components.ReceiptDialog
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import com.example.util.FeeCalculator
import com.example.util.StudentFeeSummary
import java.net.URLEncoder

@Composable
fun AdminFeeCollectionTab(
  repository: PathsalaRepository
) {
  val context = LocalContext.current
  val students by repository.students.collectAsState()
  val courses by repository.courses.collectAsState()
  val batches by repository.batches.collectAsState()
  val payments by repository.payments.collectAsState()
  val centerInfo by repository.centerInfo.collectAsState()

  var selectedTab by remember { mutableStateOf("history") } // "history" or "dues"
  var showCollectDialog by remember { mutableStateOf(false) }
  var selectedReceipt by remember { mutableStateOf<FeePayment?>(null) }
  var paymentToDelete by remember { mutableStateOf<FeePayment?>(null) }

  // Compute student fee summaries: payable per batch/course, duration from admission date, and website-adjusted payments
  val studentSummaries = remember(students, courses, batches, payments) {
    students.map { st ->
      FeeCalculator.calculateStudentFeeSummary(st, courses, batches, payments)
    }
  }
  val studentDuesList = studentSummaries.filter { it.dueAmount > 0 }

  // Fee collection form inputs
  var selectedStudentId by remember { mutableStateOf(students.firstOrNull()?.id ?: "") }
  var collectAmount by remember { mutableStateOf("400") }
  var collectMode by remember { mutableStateOf("CASH") }
  var collectMonth by remember { mutableStateOf("October 2026") }
  var collectRef by remember { mutableStateOf("") }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Header & Quick Action
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(text = "Fee Collection & Dues", fontWeight = FontWeight.Black, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
          Text(text = "Desk counter collection, reminders & receipts", fontSize = 12.sp, color = SlateTextSecondary)
        }

        Button(
          onClick = { showCollectDialog = true },
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
        ) {
          Icon(Icons.Default.AddCard, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Collect Fee", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    // Switcher (Transactions vs Outstanding Dues)
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(Color(0xFFE2E8F0))
          .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selectedTab == "history") Color.White else Color.Transparent)
            .clickable { selectedTab = "history" }
            .padding(vertical = 8.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "Receipts Ledger (${payments.size})",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (selectedTab == "history") Color(0xFF0F172A) else Color(0xFF64748B)
          )
        }

        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selectedTab == "dues") Color.White else Color.Transparent)
            .clickable { selectedTab = "dues" }
            .padding(vertical = 8.dp),
          contentAlignment = Alignment.Center
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(14.dp))
            Text(
              text = "Outstanding Dues (${studentDuesList.size})",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = if (selectedTab == "dues") Color(0xFF0F172A) else Color(0xFF64748B)
            )
          }
        }
      }
    }

    if (selectedTab == "dues") {
      // DUES TRACKER WITH WHATSAPP REMINDER TRIGGER
      item {
        SectionHeader(
          title = "Students with Outstanding Tuition Fees",
          subtitle = "Dispatch one-tap WhatsApp payment reminders with UPI details"
        )
      }

      if (studentDuesList.isEmpty()) {
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
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(36.dp))
              Text("No Outstanding Dues!", fontWeight = FontWeight.Bold, fontSize = 15.sp)
              Text("All enrolled students have cleared their fees for current session.", fontSize = 12.sp, color = SlateTextSecondary)
            }
          }
        }
      } else {
        items(studentDuesList) { summary ->
          val student = summary.student
          val dueAmount = summary.dueAmount

          Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
              ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                  Text(text = student.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                  Text(text = "Roll: ${student.rollNo} • Mobile: ${student.mobile}", fontSize = 11.sp, color = SlateTextSecondary, fontFamily = FontFamily.Monospace)
                  Text(
                    text = "${summary.course?.title ?: "Academic Course"} • ${summary.batch?.name ?: "Assigned Batch"}",
                    fontSize = 11.sp,
                    color = IndigoPrimary,
                    fontWeight = FontWeight.Medium
                  )
                  Text(
                    text = "₹${summary.monthlyFee.toInt()}/mo • Admitted: ${summary.admissionDate} (${summary.monthsElapsed.size} mo billed: ₹${summary.totalBilled.toInt()})",
                    fontSize = 10.sp,
                    color = SlateTextSecondary
                  )
                }

                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                  Text(
                    text = "Due: ₹${dueAmount.toInt()}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFDC2626),
                    fontFamily = FontFamily.Monospace
                  )
                  Text(
                    text = "Paid: ₹${summary.totalPaid.toInt()}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EmeraldSuccess
                  )
                }
              }

              // Pending months chip text
              if (summary.unpaidMonths.isNotEmpty()) {
                Text(
                  text = "Unpaid: ${summary.unpaidMonths.joinToString { it.monthLabel.split(" ")[0] }}",
                  fontSize = 11.sp,
                  color = Color(0xFFB91C1C),
                  fontWeight = FontWeight.Medium
                )
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Button(
                  onClick = {
                    selectedStudentId = student.id
                    collectAmount = (summary.unpaidMonths.firstOrNull()?.dueAmount ?: summary.monthlyFee).toInt().toString()
                    collectMonth = summary.unpaidMonths.firstOrNull()?.monthLabel ?: "October 2026"
                    showCollectDialog = true
                  },
                  shape = RoundedCornerShape(10.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                  Icon(Icons.Default.AddCard, contentDescription = null, modifier = Modifier.size(13.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Collect Fee", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                  onClick = {
                    val reminderMessage = """
                      *FEE DUE NOTICE - ${centerInfo.name}*
                      Dear Parent/Student,
                      This is a reminder regarding the pending tuition fee for *${student.name}* (Roll: ${student.rollNo}).
                      
                      *Course:* ${summary.course?.title ?: "N/A"}
                      *Batch:* ${summary.batch?.name ?: "N/A"}
                      *Admission Date:* ${summary.admissionDate}
                      *Monthly Fee:* ₹${summary.monthlyFee.toInt()}
                      *Total Billed (${summary.monthsElapsed.size} Months):* ₹${summary.totalBilled.toInt()}
                      *Total Already Paid (Adjusted):* ₹${summary.totalPaid.toInt()}
                      *Outstanding Due:* ₹${dueAmount.toInt()}
                      *Unpaid Months:* ${summary.unpaidMonths.joinToString { it.monthLabel }}
                      
                      UPI ID: ${centerInfo.upiId} (${centerInfo.upiName})
                      Kindly clear the dues at your earliest convenience or pay at the center desk.
                      Helpline: ${centerInfo.phonePrimary}
                    """.trimIndent()

                    val cleanPhone = student.mobile.replace(Regex("[^0-9]"), "")
                    val phoneWithCode = if (cleanPhone.startsWith("91")) cleanPhone else "91$cleanPhone"
                    val waUrl = "https://wa.me/$phoneWithCode?text=${URLEncoder.encode(reminderMessage, "UTF-8")}"
                    try {
                      val intent = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl))
                      context.startActivity(intent)
                    } catch (e: Exception) {
                      Toast.makeText(context, "Could not open WhatsApp", Toast.LENGTH_SHORT).show()
                    }
                  },
                  shape = RoundedCornerShape(10.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                  Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(13.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }
    } else {
      // RECEIPTS LEDGER
      item {
        SectionHeader(
          title = "Verified Fee Transactions",
          subtitle = "All historical fee receipts generated"
        )
      }

      if (payments.isEmpty()) {
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
              Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = SlateTextSecondary, modifier = Modifier.size(36.dp))
              Text("No Fee Transactions Found", fontWeight = FontWeight.Bold, fontSize = 15.sp)
              Text("Collected fees and records from website will appear here.", fontSize = 12.sp, color = SlateTextSecondary)
            }
          }
        }
      } else {
        items(payments) { pay ->
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                Text(text = pay.studentName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(text = "${pay.receiptNo} • ${pay.month}", fontSize = 11.sp, color = SlateTextSecondary, fontFamily = FontFamily.Monospace)
                Text(text = "Mode: ${pay.paymentMode} • Date: ${pay.paymentDate}", fontSize = 10.sp, color = SlateTextSecondary)
              }

              Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                  text = "₹${pay.finalAmountPaid.toInt()}",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  TextButton(
                    onClick = { selectedReceipt = pay },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                  ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(13.dp), tint = IndigoPrimary)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Receipt", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IndigoPrimary)
                  }
                  IconButton(
                    onClick = { paymentToDelete = pay },
                    modifier = Modifier.size(30.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.Delete,
                      contentDescription = "Delete Receipt",
                      tint = Color(0xFFEF4444),
                      modifier = Modifier.size(16.dp)
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  // Collect Fee at Counter Dialog
  if (showCollectDialog) {
    Dialog(onDismissRequest = { showCollectDialog = false }) {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.padding(16.dp)
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Text("Collect Fee at Counter", fontWeight = FontWeight.Black, fontSize = 17.sp)

          // Student selector
          var studentExpanded by remember { mutableStateOf(false) }
          val selectedStudent = students.find { it.id == selectedStudentId } ?: students.firstOrNull()
          val selectedSummary = studentSummaries.find { it.student.id == selectedStudent?.id }

          OutlinedTextField(
            value = selectedStudent?.let { "${it.name} (${it.rollNo})" } ?: "Select Student",
            onValueChange = {},
            readOnly = true,
            label = { Text("Select Student *") },
            trailingIcon = {
              IconButton(onClick = { studentExpanded = true }) {
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
              }
            },
            modifier = Modifier
              .fillMaxWidth()
              .clickable { studentExpanded = true }
          )

          DropdownMenu(
            expanded = studentExpanded,
            onDismissRequest = { studentExpanded = false }
          ) {
            students.forEach { s ->
              val sSumm = studentSummaries.find { it.student.id == s.id }
              DropdownMenuItem(
                text = {
                  Column {
                    Text("${s.name} (${s.rollNo})", fontWeight = FontWeight.Bold)
                    Text("Due: ₹${sSumm?.dueAmount?.toInt() ?: 0} • Fee: ₹${sSumm?.monthlyFee?.toInt() ?: 400}/mo", fontSize = 11.sp, color = SlateTextSecondary)
                  }
                },
                onClick = {
                  selectedStudentId = s.id
                  val summ = studentSummaries.find { it.student.id == s.id }
                  collectAmount = (summ?.unpaidMonths?.firstOrNull()?.dueAmount ?: summ?.monthlyFee ?: 400.0).toInt().toString()
                  collectMonth = summ?.unpaidMonths?.firstOrNull()?.monthLabel ?: "October 2026"
                  studentExpanded = false
                }
              )
            }
          }

          // Student Batch & Course Fee Summary Card
          if (selectedSummary != null) {
            Card(
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
              border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                  text = "Course: ${selectedSummary.course?.title ?: "N/A"}",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF0F172A)
                )
                Text(
                  text = "Batch: ${selectedSummary.batch?.name ?: "All Batches"} • Admitted: ${selectedSummary.admissionDate}",
                  fontSize = 11.sp,
                  color = SlateTextSecondary
                )
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text(
                    text = "Rate: ₹${selectedSummary.monthlyFee.toInt()}/month",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = IndigoPrimary
                  )
                  Text(
                    text = "Paid: ₹${selectedSummary.totalPaid.toInt()} • Due: ₹${selectedSummary.dueAmount.toInt()}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedSummary.dueAmount > 0) Color(0xFFDC2626) else EmeraldSuccess
                  )
                }
              }
            }

            // Quick Amount Suggestion Chips
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              SuggestionChip(
                onClick = { collectAmount = selectedSummary.monthlyFee.toInt().toString() },
                label = { Text("1 Mo (₹${selectedSummary.monthlyFee.toInt()})", fontSize = 11.sp) }
              )
              if (selectedSummary.dueAmount > 0) {
                SuggestionChip(
                  onClick = { collectAmount = selectedSummary.dueAmount.toInt().toString() },
                  label = { Text("Full Due (₹${selectedSummary.dueAmount.toInt()})", fontSize = 11.sp, color = Color(0xFFDC2626)) }
                )
              }
            }
          }

          OutlinedTextField(
            value = collectAmount,
            onValueChange = { collectAmount = it },
            label = { Text("Amount Received (₹) *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = collectMonth,
            onValueChange = { collectMonth = it },
            label = { Text("Billing Month (e.g. October 2026)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          // Payment mode selection chips
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Mode:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SlateTextSecondary)
            listOf("CASH", "UPI", "BANK_TRANSFER").forEach { m ->
              FilterChip(
                selected = collectMode == m,
                onClick = { collectMode = m },
                label = { Text(m, fontSize = 10.sp) }
              )
            }
          }

          OutlinedTextField(
            value = collectRef,
            onValueChange = { collectRef = it },
            label = { Text("Voucher / UTR Reference") },
            placeholder = { Text("CSH-COUNTER") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
          ) {
            TextButton(onClick = { showCollectDialog = false }) {
              Text("Cancel")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
              onClick = {
                val s = students.find { it.id == selectedStudentId }
                if (s != null) {
                  val amt = collectAmount.toDoubleOrNull() ?: (selectedSummary?.monthlyFee ?: 400.0)
                  val newPay = repository.submitStudentFeePayment(
                    student = s,
                    months = listOf(collectMonth),
                    amount = amt,
                    mode = collectMode,
                    ref = collectRef.ifBlank { "CSH-DESK" },
                    remarks = "Collected at Center Counter"
                  )
                  // Approve immediately since admin collected it
                  repository.approvePayment(newPay.id)
                  showCollectDialog = false
                  selectedReceipt = newPay.copy(status = "approved")
                  Toast.makeText(context, "Fee collected & Receipt generated!", Toast.LENGTH_SHORT).show()
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
            ) {
              Text("Collect & Issue Receipt")
            }
          }
        }
      }
    }
  }

  selectedReceipt?.let { pay ->
    ReceiptDialog(
      payment = pay,
      centerName = centerInfo.name,
      onDismiss = { selectedReceipt = null },
      onDelete = {
        val toDel = pay
        selectedReceipt = null
        paymentToDelete = toDel
      }
    )
  }

  paymentToDelete?.let { pay ->
    AlertDialog(
      onDismissRequest = { paymentToDelete = null },
      title = { Text("Delete Fee Receipt?", fontWeight = FontWeight.Bold) },
      text = {
        Text("Are you sure you want to delete receipt ${pay.receiptNo} of ₹${pay.finalAmountPaid.toInt()} for ${pay.studentName}?\n\nThis will permanently delete this record from the receipt ledger and sync the removal with Google Sheets.")
      },
      confirmButton = {
        Button(
          onClick = {
            val recNo = pay.receiptNo
            repository.deletePayment(pay.id)
            paymentToDelete = null
            Toast.makeText(context, "Receipt $recNo deleted successfully", Toast.LENGTH_SHORT).show()
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
        ) {
          Text("Delete", color = Color.White)
        }
      },
      dismissButton = {
        TextButton(onClick = { paymentToDelete = null }) {
          Text("Cancel")
        }
      }
    )
  }
}
