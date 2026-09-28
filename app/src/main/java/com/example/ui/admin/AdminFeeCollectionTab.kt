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
import java.net.URLEncoder

@Composable
fun AdminFeeCollectionTab(
  repository: PathsalaRepository
) {
  val context = LocalContext.current
  val students by repository.students.collectAsState()
  val courses by repository.courses.collectAsState()
  val payments by repository.payments.collectAsState()
  val centerInfo by repository.centerInfo.collectAsState()

  var selectedTab by remember { mutableStateOf("history") } // "history" or "dues"
  var showCollectDialog by remember { mutableStateOf(false) }
  var selectedReceipt by remember { mutableStateOf<FeePayment?>(null) }

  // Fee collection form inputs
  var selectedStudentId by remember { mutableStateOf(students.firstOrNull()?.id ?: "") }
  var collectAmount by remember { mutableStateOf("1200") }
  var collectMode by remember { mutableStateOf("CASH") }
  var collectMonth by remember { mutableStateOf("April 2026") }
  var collectRef by remember { mutableStateOf("") }

  // Compute student dues
  val approvedPayments = payments.filter { it.status == "approved" }
  val studentDuesList = students.map { st ->
    val course = courses.find { it.id == st.courseId }
    val monthly = st.customMonthlyFeeOverride ?: (course?.monthlyFee ?: 1200.0)
    val paid = approvedPayments.filter { it.studentId == st.id }.sumOf { it.finalAmountPaid }
    val totalBilled = monthly * 5 // 5 months (Apr - Aug)
    val due = maxOf(0.0, totalBilled - paid)
    Triple(st, due, paid)
  }.filter { it.second > 0 }

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
        items(studentDuesList) { (student, dueAmount, _) ->
          Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = student.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(text = "Roll: ${student.rollNo} • Mobile: ${student.mobile}", fontSize = 11.sp, color = SlateTextSecondary, fontFamily = FontFamily.Monospace)
                Text(
                  text = "Due Amount: ₹${dueAmount.toInt()}",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Black,
                  color = Color(0xFFDC2626),
                  fontFamily = FontFamily.Monospace
                )
              }

              Button(
                onClick = {
                  val reminderMessage = """
                    *FEE DUE NOTICE - ${centerInfo.name}*
                    Dear Parent/Student,
                    This is a reminder regarding the pending tuition fee for *${student.name}* (Roll: ${student.rollNo}).
                    
                    Due Amount: ₹${dueAmount.toInt()}
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
                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(text = pay.studentName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
              Text(text = "${pay.receiptNo} • ${pay.month}", fontSize = 11.sp, color = SlateTextSecondary, fontFamily = FontFamily.Monospace)
              Text(text = "Mode: ${pay.paymentMode}", fontSize = 10.sp, color = SlateTextSecondary)
            }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = "₹${pay.finalAmountPaid.toInt()}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface
              )
              TextButton(
                onClick = { selectedReceipt = pay },
                contentPadding = PaddingValues(0.dp)
              ) {
                Text("Print Receipt", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IndigoPrimary)
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
          val selectedStudent = students.find { it.id == selectedStudentId }

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
              DropdownMenuItem(
                text = { Text("${s.name} (${s.rollNo})") },
                onClick = {
                  selectedStudentId = s.id
                  studentExpanded = false
                }
              )
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
            label = { Text("Billing Month (e.g. May 2026)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = collectRef,
            onValueChange = { collectRef = it },
            label = { Text("Cash Voucher or UTR Reference") },
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
                  val amt = collectAmount.toDoubleOrNull() ?: 1200.0
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
      onDismiss = { selectedReceipt = null }
    )
  }
}
