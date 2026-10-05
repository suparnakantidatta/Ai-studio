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
  val admissions by repository.admissions.collectAsState()
  val discounts by repository.discounts.collectAsState()

  var selectedTab by remember { mutableStateOf("history") } // "history" or "dues"
  var showCollectDialog by remember { mutableStateOf(false) }
  var selectedReceipt by remember { mutableStateOf<FeePayment?>(null) }
  var paymentToDelete by remember { mutableStateOf<FeePayment?>(null) }
  var studentToEditFee by remember { mutableStateOf<StudentFeeSummary?>(null) }

  // Compute student fee summaries: payable per batch/course, duration from admission date, and website-adjusted payments
  val studentSummaries = remember(students, courses, batches, payments, admissions, discounts) {
    students.map { st ->
      FeeCalculator.calculateStudentFeeSummary(st, courses, batches, payments, admissions, discounts)
    }
  }
  val studentDuesList = studentSummaries.filter { it.dueAmount > 0 }

  // Fee collection form inputs
  var selectedStudentId by remember { mutableStateOf(students.firstOrNull()?.id ?: "") }
  var collectAmount by remember { mutableStateOf("400") }
  var collectDiscount by remember { mutableStateOf("") }
  var collectDiscountReason by remember { mutableStateOf("") }
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
      // OVERVIEW METRIC SUMMARY FOR OUTSTANDING DUES
      item {
        val totalPayableAll = studentSummaries.sumOf { it.payableFromAdmissionMonth }
        val totalDiscountAll = studentSummaries.sumOf { it.totalDiscountAllowed }
        val totalPaidAll = studentSummaries.sumOf { it.totalPaid }
        val totalDueAll = studentSummaries.sumOf { it.dueAmount }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          SectionHeader(
            title = "Tuition Fee Dues & Billing Overview",
            subtitle = "Calculated from admission month with course/batch rates & website adjusted payments"
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Card(
              shape = RoundedCornerShape(14.dp),
              colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text("Payable from Adm.", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = IndigoPrimary)
                Text("₹${totalPayableAll.toInt()}", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color(0xFF1E3A8A))
                Text("Net billed", fontSize = 9.sp, color = SlateTextSecondary)
              }
            }

            Card(
              shape = RoundedCornerShape(14.dp),
              colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF5FF)),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text("Discounts Given", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED))
                Text("₹${totalDiscountAll.toInt()}", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color(0xFF581C87))
                Text("Fee concessions", fontSize = 9.sp, color = SlateTextSecondary)
              }
            }

            Card(
              shape = RoundedCornerShape(14.dp),
              colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text("Total Paid", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                Text("₹${totalPaidAll.toInt()}", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color(0xFF065F46))
                Text("Verified receipts", fontSize = 9.sp, color = SlateTextSecondary)
              }
            }

            Card(
              shape = RoundedCornerShape(14.dp),
              colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text("Pending Due", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                Text("₹${totalDueAll.toInt()}", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color(0xFF991B1B))
                Text("${studentDuesList.size} students", fontSize = 9.sp, color = SlateTextSecondary)
              }
            }
          }
        }
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
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
              ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                  Text(text = student.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                  Text(text = "Roll: ${student.rollNo} • Mobile: ${student.mobile}", fontSize = 11.sp, color = SlateTextSecondary, fontFamily = FontFamily.Monospace)
                  Text(
                    text = "${summary.course?.title ?: "Academic Course"} • ${summary.batch?.name ?: "Assigned Batch"}",
                    fontSize = 12.sp,
                    color = IndigoPrimary,
                    fontWeight = FontWeight.SemiBold
                  )
                }

                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                  Text(
                    text = "Due: ₹${dueAmount.toInt()}",
                    fontSize = 17.sp,
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

              // Rate & Discount Breakdown Chip Row
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(8.dp))
                  .background(Color(0xFFF8FAFC))
                  .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Rate: ₹${summary.standardMonthlyFee.toInt()}/mo",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Medium,
                  color = SlateTextPrimary
                )
                if (summary.monthlyDiscount > 0) {
                  Text(
                    text = "Discount: -₹${summary.monthlyDiscount.toInt()}/mo",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7C3AED)
                  )
                }
                Text(
                  text = "Net: ₹${summary.effectiveMonthlyFee.toInt()}/mo",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = IndigoPrimary
                )
              }

              // Highlight Card: Net Payable from Admission Month & Paid Adjustment
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF1F5F9),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Text(
                      text = "Payable (${summary.admissionMonth} → ${summary.currentMonth}):",
                      fontSize = 11.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = Color(0xFF1E293B)
                    )
                    Text(
                      text = "₹${summary.payableFromAdmissionMonth.toInt()}",
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Black,
                      color = Color(0xFF0F172A)
                    )
                  }
                  Text(
                    text = "${summary.billedMonthsCount} billing months (${summary.admissionMonth} to ${summary.currentMonth}) • Gross: ₹${summary.grossBilledFromAdmissionMonth.toInt()} - Discount: ₹${summary.totalDiscountAllowed.toInt()}",
                    fontSize = 10.sp,
                    color = SlateTextSecondary
                  )
                }
              }

              // Months status preview
              if (summary.monthsElapsed.isNotEmpty()) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  summary.monthsElapsed.forEach { m ->
                    val isMthPaid = m.isPaid
                    Box(
                      modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isMthPaid) Color(0xFFDCFCE7) else Color(0xFFFEE2E2))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                      Text(
                        text = "${m.monthLabel.split(" ")[0]}: ${if (isMthPaid) "Paid" else "Due"}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isMthPaid) Color(0xFF166534) else Color(0xFF991B1B)
                      )
                    }
                  }
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
              ) {
                OutlinedButton(
                  onClick = { studentToEditFee = summary },
                  shape = RoundedCornerShape(10.dp),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                  Icon(Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(13.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Edit Fee/Discount", fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.width(6.dp))

                Button(
                  onClick = {
                    selectedStudentId = student.id
                    val firstUnpaidDue = summary.unpaidMonths.firstOrNull()?.dueAmount ?: summary.monthlyFee
                    collectAmount = firstUnpaidDue.toInt().toString()
                    collectDiscount = ""
                    collectDiscountReason = ""
                    collectMonth = summary.unpaidMonths.firstOrNull()?.monthLabel ?: "October 2026"
                    showCollectDialog = true
                  },
                  shape = RoundedCornerShape(10.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                  Icon(Icons.Default.AddCard, contentDescription = null, modifier = Modifier.size(13.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Collect Fee", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(6.dp))

                Button(
                  onClick = {
                    val reminderMessage = """
                      *FEE DUE NOTICE - ${centerInfo.name}*
                      Dear Parent/Student,
                      This is an official fee reminder for *${student.name}* (Roll: ${student.rollNo}).
                      
                      *Course:* ${summary.course?.title ?: "N/A"}
                      *Batch:* ${summary.batch?.name ?: "N/A"}
                      *Admission Month:* ${summary.admissionMonth}
                      *Standard Monthly Fee:* ₹${summary.standardMonthlyFee.toInt()}
                      *Monthly Discount Allowed:* ₹${summary.monthlyDiscount.toInt()}
                      *Net Monthly Fee:* ₹${summary.effectiveMonthlyFee.toInt()}
                      *Total Payable from Admission (${summary.billedMonthsCount} Months):* ₹${summary.payableFromAdmissionMonth.toInt()}
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
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
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

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = collectAmount,
              onValueChange = { collectAmount = it },
              label = { Text("Amount Received (₹) *") },
              singleLine = true,
              modifier = Modifier.weight(1.2f)
            )

            OutlinedTextField(
              value = collectDiscount,
              onValueChange = { collectDiscount = it.filter { c -> c.isDigit() || c == '.' } },
              label = { Text("Discount / Waiver (₹)") },
              placeholder = { Text("0") },
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
          }

          if (collectDiscount.isNotBlank() && (collectDiscount.toDoubleOrNull() ?: 0.0) > 0) {
            OutlinedTextField(
              value = collectDiscountReason,
              onValueChange = { collectDiscountReason = it },
              label = { Text("Discount Concession Reason") },
              placeholder = { Text("e.g. Merit Concession / Sibling Waiver") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
          }

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
                  val disc = collectDiscount.toDoubleOrNull() ?: 0.0
                  val discReason = collectDiscountReason.ifBlank { null }
                  val newPay = repository.submitStudentFeePayment(
                    student = s,
                    months = listOf(collectMonth),
                    amount = amt,
                    mode = collectMode,
                    ref = collectRef.ifBlank { "CSH-DESK" },
                    remarks = "Collected at Center Counter",
                    discount = disc,
                    discountReason = discReason
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

  // Edit Admission Date & Discount Dialog
  studentToEditFee?.let { summ ->
    var editAdmDate by remember(summ) { mutableStateOf(summ.student.admissionDate.ifBlank { "2026-08-15" }) }
    var editDiscountText by remember(summ) { mutableStateOf(summ.student.monthlyDiscount?.toInt()?.toString() ?: if (summ.monthlyDiscount > 0) summ.monthlyDiscount.toInt().toString() else "") }
    var editDiscountReason by remember(summ) { mutableStateOf(summ.student.discountReason ?: "Merit Scholarship") }
    var editOverrideText by remember(summ) { mutableStateOf(summ.student.customMonthlyFeeOverride?.toInt()?.toString() ?: "") }

    AlertDialog(
      onDismissRequest = { studentToEditFee = null },
      title = {
        Column {
          Text("Edit Admission & Discount", fontWeight = FontWeight.Bold, fontSize = 17.sp)
          Text("${summ.student.name} • ${summ.course?.title ?: ""}", fontSize = 12.sp, color = SlateTextSecondary)
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
          Text("1. Admission Date (Determines months elapsed)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IndigoPrimary)
          OutlinedTextField(
            value = editAdmDate,
            onValueChange = { editAdmDate = it },
            label = { Text("Admission Date (YYYY-MM-DD)") },
            placeholder = { Text("e.g. 2026-08-15") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("2026-08-01" to "August 2026", "2026-09-01" to "September 2026", "2026-10-01" to "October 2026").forEach { (dt, label) ->
              FilterChip(
                selected = editAdmDate.startsWith(dt.take(7)),
                onClick = { editAdmDate = dt },
                label = { Text(label, fontSize = 9.sp) }
              )
            }
          }

          Text("2. Monthly Discount & Fee Concession", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IndigoPrimary)
          OutlinedTextField(
            value = editDiscountText,
            onValueChange = { editDiscountText = it.filter { c -> c.isDigit() || c == '.' } },
            label = { Text("Monthly Discount Amount (₹)") },
            placeholder = { Text("e.g. 50 (Standard fee: ₹${summ.standardMonthlyFee.toInt()})") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("0" to "₹0", "50" to "₹50/mo", "100" to "₹100/mo", "150" to "₹150/mo").forEach { (disc, label) ->
              FilterChip(
                selected = editDiscountText == disc,
                onClick = { editDiscountText = disc },
                label = { Text(label, fontSize = 9.sp) }
              )
            }
          }

          OutlinedTextField(
            value = editDiscountReason,
            onValueChange = { editDiscountReason = it },
            label = { Text("Discount Reason / Concession Type") },
            placeholder = { Text("Merit Scholarship / Sibling Waiver / Early Bird") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          val previewStd = summ.standardMonthlyFee
          val previewDisc = editDiscountText.toDoubleOrNull() ?: 0.0
          val previewNet = maxOf(0.0, previewStd - previewDisc)
          Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(8.dp)) {
              Text(
                text = "Preview: Standard ₹${previewStd.toInt()} - Discount ₹${previewDisc.toInt()} = Net ₹${previewNet.toInt()}/month",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = IndigoPrimary
              )
              Text(
                text = "Dues will be automatically recalculated from ${editAdmDate.take(7)} with adjustments.",
                fontSize = 10.sp,
                color = SlateTextSecondary
              )
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val discVal = editDiscountText.toDoubleOrNull()
            val overrideVal = editOverrideText.toDoubleOrNull()
            repository.updateStudentAdmissionAndDiscount(
              studentId = summ.student.id,
              newAdmissionDate = editAdmDate.trim(),
              newMonthlyDiscount = discVal,
              newCustomFeeOverride = overrideVal,
              newDiscountReason = editDiscountReason.trim().ifBlank { null }
            )
            studentToEditFee = null
            Toast.makeText(context, "Admission date & discount updated! Dues recalculated.", Toast.LENGTH_SHORT).show()
          },
          colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
        ) {
          Text("Save & Recalculate")
        }
      },
      dismissButton = {
        TextButton(onClick = { studentToEditFee = null }) {
          Text("Cancel")
        }
      }
    )
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
