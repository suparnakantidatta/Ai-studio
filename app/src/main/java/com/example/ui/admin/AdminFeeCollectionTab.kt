package com.example.ui.admin

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
  var showManageDiscountsDialog by remember { mutableStateOf(false) }
  var showCreateDiscountDialog by remember { mutableStateOf(false) }
  var selectedReceipt by remember { mutableStateOf<FeePayment?>(null) }
  var paymentToDelete by remember { mutableStateOf<FeePayment?>(null) }
  var studentToEditFee by remember { mutableStateOf<StudentFeeSummary?>(null) }

  // Compute student fee summaries: payable per batch/course, duration from admission date, and website-adjusted payments
  val studentSummaries = remember(students, courses, batches, payments, admissions, discounts) {
    students.map { st ->
      FeeCalculator.calculateStudentFeeSummary(st, courses, batches, payments, admissions, discounts)
    }
  }
  val studentDuesList = remember(studentSummaries) { studentSummaries.filter { it.dueAmount > 0 } }
  val studentClearedList = remember(studentSummaries) { studentSummaries.filter { it.dueAmount <= 0 } }
  var duesFilter by remember { mutableStateOf("all") } // "all", "dues", "cleared"
  var duesSearchQuery by remember { mutableStateOf("") }
  var expandedLedgerStudentId by remember { mutableStateOf<String?>(null) }

  val displayedSummaries = remember(duesFilter, duesSearchQuery, studentSummaries, studentDuesList, studentClearedList) {
    val base = when (duesFilter) {
      "dues" -> studentDuesList
      "cleared" -> studentClearedList
      else -> studentSummaries
    }
    if (duesSearchQuery.isBlank()) {
      base
    } else {
      val q = duesSearchQuery.trim().lowercase(java.util.Locale.ROOT)
      base.filter { s ->
        s.student.name.lowercase(java.util.Locale.ROOT).contains(q) ||
        s.student.rollNo.lowercase(java.util.Locale.ROOT).contains(q) ||
        s.student.mobile.contains(q)
      }
    }
  }

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

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
          OutlinedButton(
            onClick = { showManageDiscountsDialog = true },
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Icon(Icons.Default.Discount, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF7C3AED))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Discounts (${discounts.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED))
          }

          Button(
            onClick = { showCollectDialog = true },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
          ) {
            Icon(Icons.Default.AddCard, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Collect Fee", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
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

          // Filter pills: All Students vs With Dues vs Cleared + Search Bar
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              FilterChip(
                selected = duesFilter == "all",
                onClick = { duesFilter = "all" },
                label = { Text("All Students (${studentSummaries.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = Color(0xFFE0E7FF),
                  selectedLabelColor = IndigoPrimary
                )
              )
              FilterChip(
                selected = duesFilter == "dues",
                onClick = { duesFilter = "dues" },
                label = { Text("With Dues (${studentDuesList.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = Color(0xFFFEE2E2),
                  selectedLabelColor = Color(0xFF991B1B)
                )
              )
              FilterChip(
                selected = duesFilter == "cleared",
                onClick = { duesFilter = "cleared" },
                label = { Text("Cleared (${studentClearedList.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = Color(0xFFDCFCE7),
                  selectedLabelColor = Color(0xFF166534)
                )
              )
            }

            OutlinedTextField(
              value = duesSearchQuery,
              onValueChange = { duesSearchQuery = it },
              placeholder = { Text("Search by student name, roll number, or phone...", fontSize = 12.sp) },
              leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
              trailingIcon = {
                if (duesSearchQuery.isNotEmpty()) {
                  IconButton(onClick = { duesSearchQuery = "" }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear search", modifier = Modifier.size(16.dp))
                  }
                }
              },
              singleLine = true,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth()
            )
          }
        }
      }

      if (displayedSummaries.isEmpty()) {
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
              Text("All enrolled students have cleared their fees for the current session.", fontSize = 12.sp, color = SlateTextSecondary)
            }
          }
        }
      } else {
        items(displayedSummaries) { summary ->
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

              // Full Months status breakdown (showing all months from admission to current month)
              if (summary.monthsElapsed.isNotEmpty()) {
                val isExpanded = expandedLedgerStudentId == student.id
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = "All Months Fee Status (${summary.paidMonthsCount} Paid, ${summary.dueMonthsCount} Due):",
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      color = SlateTextPrimary
                    )
                    Text(
                      text = if (isExpanded) "Hide Statement ▲" else "View Full Ledger ▼",
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      color = IndigoPrimary,
                      modifier = Modifier.clickable {
                        expandedLedgerStudentId = if (isExpanded) null else student.id
                      }
                    )
                  }

                  // Multi-column Grid showing EVERY single month clearly without horizontal hiding
                  summary.monthsElapsed.chunked(3).forEach { rowMonths ->
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                      rowMonths.forEach { m ->
                        val isMthPaid = m.isPaid
                        val isMthPending = m.isPending
                        Box(
                          modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                              when {
                                isMthPaid -> Color(0xFFDCFCE7)
                                isMthPending -> Color(0xFFFEF3C7)
                                else -> Color(0xFFFEE2E2)
                              }
                            )
                            .border(
                              1.dp,
                              when {
                                isMthPaid -> Color(0xFF86EFAC)
                                isMthPending -> Color(0xFFFCD34D)
                                else -> Color(0xFFFCA5A5)
                              },
                              RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 6.dp)
                        ) {
                          Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                          ) {
                            Text(
                              text = m.monthLabel,
                              fontSize = 10.sp,
                              fontWeight = FontWeight.Black,
                              maxLines = 1,
                              color = when {
                                isMthPaid -> Color(0xFF166534)
                                isMthPending -> Color(0xFF92400E)
                                else -> Color(0xFF991B1B)
                              }
                            )
                            Text(
                              text = when {
                                isMthPaid -> "✓ Paid ₹${m.paidAmount.toInt()}"
                                isMthPending -> "⏳ Pending"
                                else -> "✕ Due ₹${m.dueAmount.toInt()}"
                              },
                              fontSize = 9.sp,
                              fontWeight = FontWeight.Bold,
                              maxLines = 1,
                              color = when {
                                isMthPaid -> Color(0xFF15803D)
                                isMthPending -> Color(0xFFB45309)
                                else -> Color(0xFFB91C1C)
                              }
                            )
                          }
                        }
                      }
                      repeat(3 - rowMonths.size) {
                        Spacer(modifier = Modifier.weight(1f))
                      }
                    }
                  }

                  // Detailed expandable ledger statement
                  if (isExpanded) {
                    Card(
                      shape = RoundedCornerShape(10.dp),
                      colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                      border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                      modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) {
                      Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                          text = "Detailed Monthly Ledger Statement",
                          fontSize = 11.sp,
                          fontWeight = FontWeight.Bold,
                          color = Color(0xFF0F172A)
                        )
                        summary.monthsElapsed.forEach { m ->
                          Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                          ) {
                            Column(modifier = Modifier.weight(1.5f)) {
                              Text(m.monthLabel, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                              if (m.discount > 0) {
                                Text("Discount: -₹${m.discount.toInt()} (${m.discountTitle ?: "Concession"})", fontSize = 9.sp, color = Color(0xFF7C3AED))
                              }
                            }
                            Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                              Text(
                                text = if (m.isPaid) "₹${m.paidAmount.toInt()} Paid" else "₹${m.dueAmount.toInt()} Due",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (m.isPaid) Color(0xFF166534) else Color(0xFFDC2626)
                              )
                              if (m.matchingPayment != null) {
                                Text(m.matchingPayment.receiptNo, fontSize = 8.sp, color = SlateTextSecondary)
                              }
                            }
                          }
                          HorizontalDivider(color = Color(0xFFE2E8F0))
                        }
                      }
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
                  text = "Batch: ${selectedSummary.batch?.name ?: "All Batches"} • Admitted: ${com.example.data.sheet.GoogleSheetSyncService.parseIsoDateToLocalDate(selectedSummary.admissionDate)}",
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

  // Edit Admission Date & Discount Dialog (supporting One Student, Batch, or All, for One Month or All Months)
  studentToEditFee?.let { summ ->
    var targetScope by remember(summ) { mutableStateOf("student") } // "student", "batch", "all"
    var applicableMonthsType by remember(summ) { mutableStateOf("all_months") } // "all_months", "single_month", "selected_months"
    var selectedSingleMonth by remember(summ) { mutableStateOf(summ.unpaidMonths.firstOrNull()?.monthLabel ?: summ.currentMonth) }
    var selectedMonthsList by remember(summ) { mutableStateOf(setOf(summ.unpaidMonths.firstOrNull()?.monthLabel ?: summ.currentMonth)) }

    var discountType by remember(summ) { mutableStateOf("flat") } // "flat" or "percentage"
    var discountValueText by remember(summ) {
      val existingVal = summ.student.monthlyDiscount ?: if (summ.monthlyDiscount > 0) summ.monthlyDiscount else 50.0
      mutableStateOf(existingVal.toInt().toString())
    }
    var discountTitle by remember(summ) { mutableStateOf(summ.student.discountReason ?: "Tuition Fee Concession") }
    var editAdmDate by remember(summ) {
      mutableStateOf(com.example.data.sheet.GoogleSheetSyncService.parseIsoDateToLocalDate(summ.student.admissionDate).ifBlank { "2026-08-01" })
    }
    var showAdmissionEdit by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { studentToEditFee = null }) {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.padding(12.dp).fillMaxWidth().heightIn(max = 680.dp)
      ) {
        Column(
          modifier = Modifier
            .padding(18.dp)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text("Edit Fees & Concession", fontWeight = FontWeight.Black, fontSize = 17.sp)
              Text("${summ.student.name} • ${summ.course?.title ?: ""}", fontSize = 12.sp, color = SlateTextSecondary)
            }
            IconButton(onClick = { studentToEditFee = null }) {
              Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateTextSecondary)
            }
          }

          HorizontalDivider(color = Color(0xFFE2E8F0))

          // 1. Target Scope Selection
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("1. Discount Target Scope", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IndigoPrimary)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              FilterChip(
                selected = targetScope == "student",
                onClick = { targetScope = "student" },
                label = { Text("This Student (${summ.student.name.split(" ")[0]})", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
              )
              FilterChip(
                selected = targetScope == "batch",
                onClick = { targetScope = "batch" },
                label = { Text("All of Batch", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
              )
              FilterChip(
                selected = targetScope == "all",
                onClick = { targetScope = "all" },
                label = { Text("All Students", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
              )
            }
            Text(
              text = when (targetScope) {
                "student" -> "Applies specifically to ${summ.student.name} (Roll: ${summ.student.rollNo})"
                "batch" -> "Applies to ALL enrolled students in batch: ${summ.batch?.name ?: "Batch"}"
                else -> "Applies center-wide to all admitted students in the institute"
              },
              fontSize = 10.sp,
              color = SlateTextSecondary
            )
          }

          // 2. Applicable Months Selection
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("2. Duration / Billing Months", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IndigoPrimary)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              FilterChip(
                selected = applicableMonthsType == "all_months",
                onClick = { applicableMonthsType = "all_months" },
                label = { Text("All Months", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
              )
              FilterChip(
                selected = applicableMonthsType == "single_month",
                onClick = { applicableMonthsType = "single_month" },
                label = { Text("One Month Only", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
              )
              FilterChip(
                selected = applicableMonthsType == "selected_months",
                onClick = { applicableMonthsType = "selected_months" },
                label = { Text("Selected Months", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
              )
            }

            if (applicableMonthsType == "single_month") {
              Text("Select specific month for concession:", fontSize = 10.sp, color = SlateTextSecondary)
              val monthsList = summ.monthsElapsed.map { it.monthLabel }
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                monthsList.forEach { mLabel ->
                  FilterChip(
                    selected = selectedSingleMonth == mLabel,
                    onClick = { selectedSingleMonth = mLabel },
                    label = { Text(mLabel, fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                      selectedContainerColor = Color(0xFFDDD6FE),
                      selectedLabelColor = Color(0xFF5B21B6)
                    )
                  )
                }
              }
            } else if (applicableMonthsType == "selected_months") {
              Text("Select all months that qualify for this discount:", fontSize = 10.sp, color = SlateTextSecondary)
              val monthsList = summ.monthsElapsed.map { it.monthLabel }
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                monthsList.forEach { mLabel ->
                  val isSel = selectedMonthsList.contains(mLabel)
                  FilterChip(
                    selected = isSel,
                    onClick = {
                      selectedMonthsList = if (isSel) selectedMonthsList - mLabel else selectedMonthsList + mLabel
                    },
                    label = { Text(mLabel, fontSize = 10.sp) }
                  )
                }
              }
            }
          }

          // 3. Discount Amount and Type
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("3. Discount Type & Amount", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IndigoPrimary)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .background(Color(0xFFF1F5F9))
                  .padding(2.dp)
              ) {
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (discountType == "flat") Color.White else Color.Transparent)
                    .clickable { discountType = "flat" }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                  Text("Flat ₹", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (discountType == "percentage") Color.White else Color.Transparent)
                    .clickable { discountType = "percentage" }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                  Text("% Off", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }

              OutlinedTextField(
                value = discountValueText,
                onValueChange = { discountValueText = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text(if (discountType == "flat") "Amount (₹)" else "Percentage (%)") },
                singleLine = true,
                modifier = Modifier.weight(1f)
              )
            }

            // Quick suggestion chips
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              val quicks = if (discountType == "flat") listOf("50", "100", "150", "200") else listOf("10", "20", "25", "50")
              quicks.forEach { v ->
                SuggestionChip(
                  onClick = { discountValueText = v },
                  label = { Text(if (discountType == "flat") "₹$v" else "$v%", fontSize = 10.sp) }
                )
              }
            }
          }

          // 4. Concession Name / Reason
          OutlinedTextField(
            value = discountTitle,
            onValueChange = { discountTitle = it },
            label = { Text("Concession Title / Reason") },
            placeholder = { Text("e.g. Merit Concession / Batch Concession / Special Waiver") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          // 5. Expandable Admission Date adjustment
          Row(
            modifier = Modifier.fillMaxWidth().clickable { showAdmissionEdit = !showAdmissionEdit },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Adjust Admission Date & Billing Cycle", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateTextSecondary)
            Text(if (showAdmissionEdit) "▲" else "▼", fontSize = 11.sp, color = IndigoPrimary)
          }

          if (showAdmissionEdit) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              OutlinedTextField(
                value = editAdmDate,
                onValueChange = { editAdmDate = it },
                label = { Text("Admission Date (YYYY-MM-DD)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
              )
              Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("2026-03-01", "2026-04-01", "2026-07-01", "2026-08-01").forEach { dt ->
                  FilterChip(
                    selected = editAdmDate.startsWith(dt.take(7)),
                    onClick = { editAdmDate = dt },
                    label = { Text(dt.take(7), fontSize = 9.sp) }
                  )
                }
              }
            }
          }

          // Real-time Impact Calculation Card
          val stdFee = summ.standardMonthlyFee
          val rawVal = discountValueText.toDoubleOrNull() ?: 0.0
          val calculatedDiscAmt = if (discountType == "percentage") (stdFee * rawVal) / 100.0 else rawVal
          val effectiveNet = maxOf(0.0, stdFee - calculatedDiscAmt)

          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
              Text("Calculation Summary (Preview):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Standard: ₹${stdFee.toInt()}", fontSize = 11.sp, color = SlateTextSecondary)
                Text("Discount: -₹${calculatedDiscAmt.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED))
                Text("Net Fee: ₹${effectiveNet.toInt()}/mo", fontSize = 11.sp, fontWeight = FontWeight.Black, color = EmeraldSuccess)
              }
              Text(
                text = "Target: ${if (targetScope == "student") summ.student.name else if (targetScope == "batch") (summ.batch?.name ?: "Batch") else "All Center Students"} • Duration: ${if (applicableMonthsType == "all_months") "All Months" else if (applicableMonthsType == "single_month") selectedSingleMonth else "${selectedMonthsList.size} Specific Months"}",
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = IndigoPrimary
              )
            }
          }

          // Dialog Buttons
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
          ) {
            TextButton(onClick = { studentToEditFee = null }) {
              Text("Cancel")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
              onClick = {
                val discVal = discountValueText.toDoubleOrNull() ?: 0.0
                val discId = "disc-${System.currentTimeMillis()}"
                val targetBatchId = if (targetScope == "batch") (summ.batch?.id ?: summ.student.batchId) else null
                val targetStudentId = if (targetScope == "student") summ.student.id else null
                val targetCourseId = if (targetScope == "batch") (summ.course?.id ?: summ.student.courseId) else null
                val targetAcademicClass = summ.course?.academicClass ?: summ.student.studentClass

                val finalMonthsList = when (applicableMonthsType) {
                  "single_month" -> listOf(selectedSingleMonth)
                  "selected_months" -> selectedMonthsList.toList()
                  else -> null
                }

                val newDiscount = com.example.data.model.Discount(
                  id = discId,
                  title = discountTitle.ifBlank { "Fee Concession" },
                  type = discountType,
                  value = discVal,
                  scope = targetScope,
                  academicClass = targetAcademicClass,
                  courseId = targetCourseId,
                  batchId = targetBatchId,
                  studentId = targetStudentId,
                  applicableMonthsType = applicableMonthsType,
                  selectedMonths = finalMonthsList,
                  active = true,
                  reason = discountTitle
                )
                repository.addOrUpdateDiscount(newDiscount)

                if (targetScope == "student" || showAdmissionEdit) {
                  repository.updateStudentAdmissionAndDiscount(
                    studentId = summ.student.id,
                    newAdmissionDate = editAdmDate.trim(),
                    newMonthlyDiscount = if (applicableMonthsType == "all_months") discVal else summ.student.monthlyDiscount,
                    newCustomFeeOverride = summ.student.customMonthlyFeeOverride,
                    newDiscountReason = discountTitle
                  )
                }

                studentToEditFee = null
                Toast.makeText(context, "Discount saved to database & applied! Dues recalculated.", Toast.LENGTH_SHORT).show()
              },
              colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
              shape = RoundedCornerShape(10.dp)
            ) {
              Text("Save & Apply to DB", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }

  // Manage Discounts Dialog
  if (showManageDiscountsDialog) {
    Dialog(onDismissRequest = { showManageDiscountsDialog = false }) {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.padding(12.dp).fillMaxWidth().heightIn(max = 650.dp)
      ) {
        Column(
          modifier = Modifier
            .padding(18.dp)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text("Fee Concessions Database", fontWeight = FontWeight.Black, fontSize = 17.sp)
              Text("${discounts.size} active rules from sheet & local db", fontSize = 12.sp, color = SlateTextSecondary)
            }
            IconButton(onClick = { showManageDiscountsDialog = false }) {
              Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateTextSecondary)
            }
          }

          HorizontalDivider(color = Color(0xFFE2E8F0))

          if (discounts.isEmpty()) {
            Box(
              modifier = Modifier.fillMaxWidth().padding(24.dp),
              contentAlignment = Alignment.Center
            ) {
              Text("No discount rules configured yet.", fontSize = 12.sp, color = SlateTextSecondary)
            }
          } else {
            discounts.forEach { d ->
              Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(12.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(d.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    val targetName = when (d.scope) {
                      "student" -> students.find { it.id == d.studentId }?.let { "Student: ${it.name}" } ?: "Student ID: ${d.studentId}"
                      "batch" -> batches.find { it.id == d.batchId }?.let { "Batch: ${it.name}" } ?: "Batch ID: ${d.batchId}"
                      else -> "All Center Students"
                    }
                    val monthsStr = when (d.applicableMonthsType) {
                      "single_month", "selected_months" -> d.selectedMonths?.joinToString(", ") ?: "Specific Months"
                      else -> "All Months"
                    }
                    Text("$targetName • $monthsStr", fontSize = 10.sp, color = IndigoPrimary, fontWeight = FontWeight.SemiBold)
                    Text(
                      text = if (d.type == "percentage") "${d.value.toInt()}% Off Standard Fee" else "₹${d.value.toInt()} Flat Monthly Discount",
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Black,
                      color = EmeraldSuccess
                    )
                  }

                  IconButton(
                    onClick = {
                      repository.deleteDiscount(d.id)
                      Toast.makeText(context, "Concession rule deleted from database", Toast.LENGTH_SHORT).show()
                    }
                  ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                  }
                }
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
          ) {
            Button(
              onClick = {
                showManageDiscountsDialog = false
                val firstStudent = students.firstOrNull()
                if (firstStudent != null) {
                  val summ = studentSummaries.find { it.student.id == firstStudent.id }
                  if (summ != null) studentToEditFee = summ
                }
              },
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Add New Concession", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
