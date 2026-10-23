package com.example.ui.admin

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.util.FeeCalculator

@Composable
fun AdminFeeLedgerTab(
  students: List<Student>,
  courses: List<Course>,
  batches: List<Batch>,
  discounts: List<Discount>,
  payments: List<FeePayment>,
  centerInfo: CenterInfo = CenterInfo(),
  onOpenCollectFeeDialog: (Student?) -> Unit,
  onViewReceipt: (FeePayment) -> Unit,
  onDeletePayment: (String) -> Unit,
  onEditStudent: (Student) -> Unit,
  onSendReminder: (Student, Double, String) -> Unit = { _, _, _ -> }
) {
  val context = LocalContext.current
  var selectedSubTab by remember { mutableIntStateOf(0) } // 0: Receipts Ledger, 1: Outstanding Dues
  var duesFilterOnlyWithDues by remember { mutableStateOf(false) }

  // Compute stats across all students
  val studentSummaries = remember(students, courses, batches, discounts, payments) {
    students.map { st ->
      val course = courses.firstOrNull { it.id == st.courseId }
      val batch = batches.firstOrNull { it.id == st.batchId }
      val summary = FeeCalculator.calculateStudentDuesSummary(
        student = st,
        course = course,
        batch = batch,
        discounts = discounts,
        payments = payments
      )
      Pair(st, summary)
    }
  }

  val totalGross = remember(studentSummaries) {
    studentSummaries.sumOf { it.second.totalBaseFee }
  }
  val totalDiscounts = remember(studentSummaries) {
    studentSummaries.sumOf { it.second.totalDiscount }
  }
  val totalPaid = remember(studentSummaries) {
    studentSummaries.sumOf { it.second.totalPaid }
  }
  val totalPendingDue = remember(studentSummaries) {
    studentSummaries.sumOf { it.second.totalOutstandingDue }
  }
  val studentsWithDues = remember(studentSummaries) {
    studentSummaries.filter { it.second.totalOutstandingDue > 0 }
  }

  val filteredSummaries = if (duesFilterOnlyWithDues) studentsWithDues else studentSummaries

  Column(modifier = Modifier.fillMaxSize()) {
    // Header row matching Screenshot 4 & 5
    Surface(
      color = Color.White,
      tonalElevation = 2.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "Fee Collection & Dues",
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = Slate900
            )
            Text(
              text = "Desk counter collection, reminders & receipts",
              fontSize = 12.sp,
              color = Slate600
            )
          }

          Button(
            onClick = { onOpenCollectFeeDialog(null) },
            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Collect Fee", fontSize = 13.sp, fontWeight = FontWeight.Bold)
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Subtabs matching Screenshot 4 & 5
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Surface(
            modifier = Modifier
              .weight(1f)
              .height(44.dp),
            shape = RoundedCornerShape(12.dp),
            color = if (selectedSubTab == 0) Color.White else Color(0xFFE2E8F0),
            shadowElevation = if (selectedSubTab == 0) 3.dp else 0.dp,
            onClick = { selectedSubTab = 0 }
          ) {
            Row(
              modifier = Modifier.fillMaxSize(),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Receipts Ledger (${payments.size})",
                fontSize = 13.sp,
                fontWeight = if (selectedSubTab == 0) FontWeight.Bold else FontWeight.Medium,
                color = if (selectedSubTab == 0) Slate900 else Slate600
              )
            }
          }

          Surface(
            modifier = Modifier
              .weight(1f)
              .height(44.dp),
            shape = RoundedCornerShape(12.dp),
            color = if (selectedSubTab == 1) Color.White else Color(0xFFE2E8F0),
            shadowElevation = if (selectedSubTab == 1) 3.dp else 0.dp,
            onClick = { selectedSubTab = 1 }
          ) {
            Row(
              modifier = Modifier.fillMaxSize(),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = ErrorRed,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Outstanding Dues (${students.size})",
                fontSize = 13.sp,
                fontWeight = if (selectedSubTab == 1) FontWeight.Bold else FontWeight.Medium,
                color = if (selectedSubTab == 1) Slate900 else Slate600
              )
            }
          }
        }
      }
    }

    if (selectedSubTab == 0) {
      // Receipts Ledger View (Screenshot 4)
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        item {
          Column {
            Text(
              text = "Verified Fee Transactions",
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              color = Slate900
            )
            Text(
              text = "All historical fee receipts generated",
              fontSize = 12.sp,
              color = Slate600
            )
          }
        }

        if (payments.isEmpty()) {
          item {
            Card(
              shape = RoundedCornerShape(14.dp),
              colors = CardDefaults.cardColors(containerColor = Color.White),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = "No fee receipts recorded yet. Click 'Collect Fee' above to record one.",
                modifier = Modifier.padding(20.dp),
                fontSize = 13.sp,
                color = Slate600
              )
            }
          }
        } else {
          items(payments) { p ->
            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = Color.White),
              elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                // AMOUNT IN ONE LINE with smooth badges and receipt number
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    Surface(
                      shape = RoundedCornerShape(8.dp),
                      color = BrandPrimary.copy(alpha = 0.12f)
                    ) {
                      Text(
                        text = "₹${p.finalAmountPaid.toInt()}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BrandPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                      )
                    }
                    Surface(
                      shape = RoundedCornerShape(8.dp),
                      color = if (p.status.equals("approved", true)) SuccessGreenLight else WarningAmber.copy(alpha = 0.2f)
                    ) {
                      Text(
                        text = p.status.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (p.status.equals("approved", true)) SuccessGreen else BrandGoldDark,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                      )
                    }
                    Surface(
                      shape = RoundedCornerShape(8.dp),
                      color = Slate100
                    ) {
                      Text(
                        text = p.paymentMode,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate700,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                      )
                    }
                  }
                  Text(
                    text = p.receiptNo,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate500
                  )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(text = p.studentName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Slate900)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "Covered: ${p.month.ifBlank { p.monthsCovered.joinToString(", ") }}",
                  fontSize = 12.sp,
                  color = Slate600
                )

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = if (p.paymentDate.isNotBlank()) "Date: ${p.paymentDate.take(10)}" else "Recorded Desk",
                    fontSize = 11.sp,
                    color = Slate500
                  )

                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                      onClick = { onViewReceipt(p) },
                      colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                      shape = RoundedCornerShape(8.dp),
                      contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                      Icon(Icons.Default.Receipt, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("Receipt", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                      onClick = { onDeletePayment(p.id) },
                      modifier = Modifier.size(32.dp)
                    ) {
                      Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed, modifier = Modifier.size(16.dp))
                    }
                  }
                }
              }
            }
          }
        }
      }
    } else {
      // Outstanding Dues View (Screenshot 5)
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        item {
          Text(
            text = "Calculated from admission month with course/batch rates & website adjusted payments",
            fontSize = 12.sp,
            color = Slate600
          )
        }

        // 4 Stat Cards matching Screenshot 5
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            // Card 1: Payable from Adm
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Color(0xFFEFF6FF),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text("Payable from", fontSize = 10.sp, color = Slate600)
                Text("Adm.", fontSize = 10.sp, color = Slate600)
                Spacer(modifier = Modifier.height(4.dp))
                Text("₹${totalGross.toInt()}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Slate900)
                Text("Net billed", fontSize = 9.sp, color = Slate500)
              }
            }

            // Card 2: Discounts Given
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Color(0xFFFAF5FF),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text("Discounts Given", fontSize = 10.sp, color = Slate600)
                Spacer(modifier = Modifier.height(16.dp))
                Text("₹${totalDiscounts.toInt()}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B21A8))
                Text("Fee concessions", fontSize = 9.sp, color = Slate500)
              }
            }

            // Card 3: Total Paid
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Color(0xFFECFDF5),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text("Total Paid", fontSize = 10.sp, color = Slate600)
                Spacer(modifier = Modifier.height(16.dp))
                Text("₹${totalPaid.toInt()}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                Text("Verified receipts", fontSize = 9.sp, color = Slate500)
              }
            }

            // Card 4: Pending Due
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Color(0xFFFEF2F2),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text("Pending Due", fontSize = 10.sp, color = Slate600)
                Spacer(modifier = Modifier.height(16.dp))
                Text("₹${totalPendingDue.toInt()}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ErrorRed)
                Text("${studentsWithDues.size} students", fontSize = 9.sp, color = Slate500)
              }
            }
          }
        }

        // Filter Buttons & Remind All
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Button(
              onClick = { duesFilterOnlyWithDues = true },
              colors = ButtonDefaults.buttonColors(
                containerColor = if (duesFilterOnlyWithDues) ErrorRedLight else Color(0xFFF1F5F9),
                contentColor = if (duesFilterOnlyWithDues) ErrorRed else Slate700
              ),
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
              Text("With Dues (${studentsWithDues.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
              onClick = { duesFilterOnlyWithDues = false },
              colors = ButtonDefaults.buttonColors(
                containerColor = if (!duesFilterOnlyWithDues) Slate200 else Color(0xFFF1F5F9),
                contentColor = if (!duesFilterOnlyWithDues) Slate900 else Slate700
              ),
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
              Text("All Admitted (${students.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            if (studentsWithDues.isNotEmpty()) {
              Button(
                onClick = {
                  studentsWithDues.forEach { (s, sDues) ->
                    onSendReminder(
                      s,
                      sDues.totalOutstandingDue,
                      sDues.pendingMonths.joinToString { it.monthLabel }
                    )
                  }
                  Toast.makeText(context, "Reminders sent to all ${studentsWithDues.size} students!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
              ) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text("Remind All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }

        // Student Dues List Cards matching Screenshot 5
        items(filteredSummaries) { (st, dues) ->
          val course = courses.firstOrNull { it.id == st.courseId }
          val batch = batches.firstOrNull { it.id == st.batchId }

          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
              ) {
                Column {
                  Text(text = st.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Slate900)
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = "Roll: ${st.rollNo} • Mobile: ${st.mobile}",
                    fontSize = 11.sp,
                    color = Slate600
                  )
                }

                Column(horizontalAlignment = Alignment.End) {
                  Text(
                    text = "Due: ₹${dues.totalOutstandingDue.toInt()}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (dues.totalOutstandingDue > 0) ErrorRed else SuccessGreen
                  )
                  Text(
                    text = "Paid: ₹${dues.totalPaid.toInt()}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = SuccessGreen
                  )
                }
              }

              Spacer(modifier = Modifier.height(6.dp))

              Text(
                text = "${course?.title ?: "COMPUTER SCIENCE"} • ${batch?.name ?: "MORNING BATCH"}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = BrandPrimary
              )

              Spacer(modifier = Modifier.height(8.dp))

              // Rate Details Bar
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = Slate50,
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text("Rate: ₹${course?.monthlyFee?.toInt() ?: 400}/mo", fontSize = 11.sp, color = Slate700)
                  Text("Discount: -₹${st.monthlyDiscount?.toInt() ?: 100}/mo", fontSize = 11.sp, color = BrandGoldDark)
                  Text("Net: ₹${((course?.monthlyFee ?: 400.0) - (st.monthlyDiscount ?: 0.0)).toInt()}/mo", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandPrimary)
                }
              }

              Spacer(modifier = Modifier.height(8.dp))

              // Payable Summary Container
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF8FAFC),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    val startM = dues.allMonths.firstOrNull()?.monthLabel ?: "Admission"
                    val endM = dues.allMonths.lastOrNull()?.monthLabel ?: "Current"
                    Text("Payable ($startM -> $endM):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate800)
                    Text("₹${dues.totalBaseFee.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate900)
                  }
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    "${dues.allMonths.size} billing months • Gross: ₹${dues.totalBaseFee.toInt()} - Discount: ₹${dues.totalDiscount.toInt()}",
                    fontSize = 10.sp,
                    color = Slate500
                  )
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              // Monthly Status Badges
              Text(
                text = "Monthly Status (${dues.paidMonths.size} Paid, ${dues.pendingMonths.size} Due):",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Slate700
              )
              Spacer(modifier = Modifier.height(6.dp))

              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                dues.allMonths.forEach { m ->
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (m.isPaid) SuccessGreenLight else ErrorRedLight
                  ) {
                    Column(
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                      horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                      Text(
                        text = m.monthLabel.split(" ").firstOrNull() ?: m.monthLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (m.isPaid) SuccessGreen else ErrorRed
                      )
                      Text(
                        text = if (m.isPaid) "Paid (₹${m.amountPaid.toInt()})" else "Due (₹${m.remainingDue.toInt()})",
                        fontSize = 9.sp,
                        color = if (m.isPaid) SuccessGreen else ErrorRed
                      )
                    }
                  }
                }
              }

              Spacer(modifier = Modifier.height(14.dp))

              // Buttons row matching Screenshot 5
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                OutlinedButton(
                  onClick = { onEditStudent(st) },
                  shape = RoundedCornerShape(10.dp),
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                  modifier = Modifier.weight(1.2f)
                ) {
                  Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Edit Fee/Discount", fontSize = 11.sp)
                }

                Button(
                  onClick = { onOpenCollectFeeDialog(st) },
                  colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                  shape = RoundedCornerShape(10.dp),
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                  modifier = Modifier.weight(1.1f)
                ) {
                  Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Collect Fee", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                if (dues.totalOutstandingDue > 0) {
                  Button(
                    onClick = {
                      onSendReminder(
                        st,
                        dues.totalOutstandingDue,
                        dues.pendingMonths.joinToString { it.monthLabel }
                      )
                      Toast.makeText(context, "Fee reminder sent to ${st.name}!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1.0f)
                  ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Remind", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                  }
                }

                Button(
                  onClick = {
                    val msg = "Dear Parent/Student ${st.name}, Fee statement for ${course?.title ?: centerInfo.name}: Total Due: ₹${dues.totalOutstandingDue.toInt()} (${dues.pendingMonths.joinToString(", ") { it.monthLabel }}). Please deposit counter cash or UPI: ${centerInfo.upiId}. ${centerInfo.name} Helpline: ${centerInfo.phone}"
                    val phone = st.guardianPhone.ifBlank { st.mobile }
                    val cleanPhone = phone.replace("+", "").replace(" ", "").replace("-", "")
                    val fullPhone = if (cleanPhone.length == 10) "91$cleanPhone" else cleanPhone
                    val uri = Uri.parse("https://api.whatsapp.com/send?phone=$fullPhone&text=${Uri.encode(msg)}")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    try {
                      context.startActivity(intent)
                    } catch (_: Exception) {}
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                  shape = RoundedCornerShape(10.dp),
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                  modifier = Modifier.weight(1.1f)
                ) {
                  Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }
    }
  }
}
