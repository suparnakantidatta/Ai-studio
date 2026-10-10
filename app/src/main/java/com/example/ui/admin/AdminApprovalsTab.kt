package com.example.ui.admin

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
import com.example.data.model.AdmissionApplication
import com.example.data.model.FeePayment
import com.example.data.repository.PathsalaRepository
import com.example.ui.components.ReceiptDialog
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*

@Composable
fun AdminApprovalsTab(
  repository: PathsalaRepository
) {
  val context = LocalContext.current
  val payments by repository.payments.collectAsState()
  val admissions by repository.admissions.collectAsState()
  val courses by repository.courses.collectAsState()
  val batches by repository.batches.collectAsState()
  val centerInfo by repository.centerInfo.collectAsState()

  var selectedSubTab by remember { mutableStateOf("payments") } // "payments" or "admissions"

  val pendingPayments = payments.filter { it.status == "pending" }
  val pendingAdmissions = admissions.filter { it.status == "pending" }

  var rejectPaymentTarget by remember { mutableStateOf<FeePayment?>(null) }
  var rejectReasonInput by remember { mutableStateOf("") }

  var enrollAdmissionTarget by remember { mutableStateOf<AdmissionApplication?>(null) }
  var assignedRollNo by remember { mutableStateOf("") }
  var assignedBatchId by remember { mutableStateOf("") }
  var enrollAdmissionDate by remember { mutableStateOf("") }
  var enrollMonthlyDiscount by remember { mutableStateOf("") }
  var enrollDiscountReason by remember { mutableStateOf("") }
  var isFeePaidChecked by remember { mutableStateOf(true) }
  var feePaidAmount by remember { mutableStateOf("400") }

  var generatedReceiptToView by remember { mutableStateOf<FeePayment?>(null) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Top Tab Selector
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
            .background(if (selectedSubTab == "payments") Color.White else Color.Transparent)
            .clickable { selectedSubTab = "payments" }
            .padding(vertical = 8.dp),
          contentAlignment = Alignment.Center
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Default.Payment, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(16.dp))
            Text(
              text = "Fee Approvals (${pendingPayments.size})",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = if (selectedSubTab == "payments") Color(0xFF0F172A) else Color(0xFF64748B)
            )
          }
        }

        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selectedSubTab == "admissions") Color.White else Color.Transparent)
            .clickable { selectedSubTab = "admissions" }
            .padding(vertical = 8.dp),
          contentAlignment = Alignment.Center
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(16.dp))
            Text(
              text = "Admissions Queue (${pendingAdmissions.size})",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = if (selectedSubTab == "admissions") Color(0xFF0F172A) else Color(0xFF64748B)
            )
          }
        }
      }
    }

    if (selectedSubTab == "payments") {
      // PENDING PAYMENTS LIST
      item {
        SectionHeader(
          title = "Pending Student Fee Approvals",
          subtitle = "Student self-submitted UPI transfers & cash counter deposits"
        )
      }

      if (pendingPayments.isEmpty()) {
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
              Text("All Payment Requests Cleared!", fontWeight = FontWeight.Bold, fontSize = 15.sp)
              Text("There are no student fee submissions pending admin review.", fontSize = 12.sp, color = SlateTextSecondary)
            }
          }
        }
      } else {
        items(pendingPayments) { pay ->
          Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                StatusBadge(status = "pending")
                Text(
                  text = "₹${pay.finalAmountPaid.toInt()}",
                  fontSize = 20.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }

              Column {
                Text(
                  text = pay.studentName,
                  fontWeight = FontWeight.Black,
                  fontSize = 16.sp,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "Aadhaar: ${pay.studentAadhaar} • Mobile: ${pay.studentMobile}",
                  fontSize = 11.sp,
                  color = SlateTextSecondary,
                  fontFamily = FontFamily.Monospace
                )
              }

              // Details box
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .background(Color(0xFFF8FAFC))
                  .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Text(text = "Months: ${pay.month}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SlateTextPrimary)
                Text(text = "Mode: ${pay.paymentMode}", fontSize = 11.sp, color = SlateTextSecondary)
                if (!pay.transactionRef.isNullOrBlank()) {
                  Text(text = "Ref / UTR: ${pay.transactionRef}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = IndigoPrimary, fontWeight = FontWeight.Bold)
                }
                if (!pay.remarks.isNullOrBlank()) {
                  Text(text = "Note: ${pay.remarks}", fontSize = 11.sp, color = SlateTextSecondary)
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                OutlinedButton(
                  onClick = {
                    rejectPaymentTarget = pay
                    rejectReasonInput = "Invalid UTR / Payment not received"
                  },
                  shape = RoundedCornerShape(10.dp),
                  colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                  modifier = Modifier.weight(1f)
                ) {
                  Text("Reject", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                  onClick = {
                    repository.approvePayment(pay.id)
                    Toast.makeText(context, "Payment approved! Official receipt generated.", Toast.LENGTH_SHORT).show()
                  },
                  shape = RoundedCornerShape(10.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                  modifier = Modifier.weight(1.5f)
                ) {
                  Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Approve & Issue", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }
    } else {
      // ADMISSIONS QUEUE
      item {
        SectionHeader(
          title = "Online Admission Applications",
          subtitle = "New student enrollment requests ready for onboarding"
        )
      }

      if (pendingAdmissions.isEmpty()) {
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
              Text("No Pending Applications", fontWeight = FontWeight.Bold, fontSize = 15.sp)
              Text("All incoming admission requests have been reviewed and enrolled.", fontSize = 12.sp, color = SlateTextSecondary)
            }
          }
        }
      } else {
        items(pendingAdmissions) { app ->
          val targetCourse = courses.find { it.id == app.targetCourseId }

          Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Application #${app.applicationNo}",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace,
                  color = IndigoPrimary
                )
                StatusBadge(status = "pending")
              }

              Column {
                Text(
                  text = app.studentName,
                  fontWeight = FontWeight.Black,
                  fontSize = 16.sp,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "Class: ${app.studentClass ?: "Class 10"} • Mobile: ${app.mobile}",
                  fontSize = 12.sp,
                  color = SlateTextSecondary
                )
              }

              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .background(Color(0xFFF8FAFC))
                  .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Text(text = "Target Course: ${targetCourse?.title ?: app.targetCourseId}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SlateTextPrimary)
                Text(text = "Guardian: ${app.guardianName} (${app.guardianPhone})", fontSize = 11.sp, color = SlateTextSecondary)
                Text(text = "Address: ${app.address}", fontSize = 11.sp, color = SlateTextSecondary)
                if (!app.remarks.isNullOrBlank()) {
                  Text(text = "Student Note: \"${app.remarks}\"", fontSize = 11.sp, color = SlateTextSecondary)
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                OutlinedButton(
                  onClick = {
                    repository.rejectAdmission(app.id, "Rejected by admin")
                    Toast.makeText(context, "Application rejected.", Toast.LENGTH_SHORT).show()
                  },
                  shape = RoundedCornerShape(10.dp),
                  colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                  modifier = Modifier.weight(1f)
                ) {
                  Text("Reject", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                  onClick = {
                    enrollAdmissionTarget = app
                    assignedRollNo = "PP-2026-${(100..999).random()}"
                    assignedBatchId = app.preferredBatchId ?: (batches.firstOrNull()?.id ?: "batch-1")
                    enrollAdmissionDate = if (app.appliedDate.isNotBlank()) {
                      com.example.data.sheet.GoogleSheetSyncService.parseIsoDateToLocalDate(app.appliedDate).ifBlank {
                        com.example.data.sheet.GoogleSheetSyncService.getCurrentIstDate()
                      }
                    } else com.example.data.sheet.GoogleSheetSyncService.getCurrentIstDate()
                    enrollMonthlyDiscount = ""
                    enrollDiscountReason = "Merit Concession"
                    val stdFee = targetCourse?.monthlyFee?.toInt() ?: 400
                    feePaidAmount = stdFee.toString()
                  },
                  shape = RoundedCornerShape(10.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                  modifier = Modifier.weight(1.5f)
                ) {
                  Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Process & Enroll", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }
    }
  }

  // Reject Payment Dialog
  rejectPaymentTarget?.let { pay ->
    Dialog(onDismissRequest = { rejectPaymentTarget = null }) {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.padding(16.dp)
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Text("Reject Payment Request", fontWeight = FontWeight.Bold, fontSize = 16.sp)
          Text("Enter the reason for rejection (shown to student):", fontSize = 12.sp, color = SlateTextSecondary)

          OutlinedTextField(
            value = rejectReasonInput,
            onValueChange = { rejectReasonInput = it },
            label = { Text("Rejection Reason") },
            modifier = Modifier.fillMaxWidth()
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
          ) {
            TextButton(onClick = { rejectPaymentTarget = null }) {
              Text("Cancel")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
              onClick = {
                repository.rejectPayment(pay.id, rejectReasonInput)
                rejectPaymentTarget = null
                Toast.makeText(context, "Payment marked as rejected.", Toast.LENGTH_SHORT).show()
              },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
            ) {
              Text("Confirm Rejection")
            }
          }
        }
      }
    }
  }

  // Process & Enroll Student Dialog
  enrollAdmissionTarget?.let { app ->
    Dialog(onDismissRequest = { enrollAdmissionTarget = null }) {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.padding(16.dp)
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Text("Onboard & Enroll Student", fontWeight = FontWeight.Black, fontSize = 17.sp)
          Text(
            text = "Enrolling ${app.studentName} (${app.mobile}) into active database.",
            fontSize = 12.sp,
            color = SlateTextSecondary
          )

          OutlinedTextField(
            value = assignedRollNo,
            onValueChange = { assignedRollNo = it },
            label = { Text("Assign Roll Number *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          // Batch Selector
          var batchExpanded by remember { mutableStateOf(false) }
          val selectedBatch = batches.find { it.id == assignedBatchId }

          OutlinedTextField(
            value = selectedBatch?.name ?: "Select Batch",
            onValueChange = {},
            readOnly = true,
            label = { Text("Assigned Batch") },
            trailingIcon = {
              IconButton(onClick = { batchExpanded = true }) {
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
              }
            },
            modifier = Modifier
              .fillMaxWidth()
              .clickable { batchExpanded = true }
          )

          DropdownMenu(
            expanded = batchExpanded,
            onDismissRequest = { batchExpanded = false }
          ) {
            batches.forEach { b ->
              DropdownMenuItem(
                text = { Text("${b.name} (${b.timing})") },
                onClick = {
                  assignedBatchId = b.id
                  batchExpanded = false
                }
              )
            }
          }

          OutlinedTextField(
            value = enrollAdmissionDate,
            onValueChange = { enrollAdmissionDate = it },
            label = { Text("Admission Date (YYYY-MM-DD)") },
            placeholder = { Text("2026-08-15") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = enrollMonthlyDiscount,
            onValueChange = {
              enrollMonthlyDiscount = it.filter { c -> c.isDigit() || c == '.' }
              val disc = it.toDoubleOrNull() ?: 0.0
              val stdFee = (courses.find { c -> c.id == app.targetCourseId }?.monthlyFee ?: 400.0)
              feePaidAmount = maxOf(0.0, stdFee - disc).toInt().toString()
            },
            label = { Text("Monthly Discount Concession (₹)") },
            placeholder = { Text("e.g. 50") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          if (enrollMonthlyDiscount.isNotBlank() && (enrollMonthlyDiscount.toDoubleOrNull() ?: 0.0) > 0) {
            OutlinedTextField(
              value = enrollDiscountReason,
              onValueChange = { enrollDiscountReason = it },
              label = { Text("Concession Reason") },
              placeholder = { Text("Merit Concession / Sibling Waiver") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Checkbox(
              checked = isFeePaidChecked,
              onCheckedChange = { isFeePaidChecked = it }
            )
            Text("Record 1st Month Fee Payment Receipt", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }

          if (isFeePaidChecked) {
            OutlinedTextField(
              value = feePaidAmount,
              onValueChange = { feePaidAmount = it },
              label = { Text("Amount Paid (₹)") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
          ) {
            TextButton(onClick = { enrollAdmissionTarget = null }) {
              Text("Cancel")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
              onClick = {
                val disc = enrollMonthlyDiscount.toDoubleOrNull()
                val enrolled = repository.approveAdmission(
                  applicationId = app.id,
                  assignedBatchId = assignedBatchId,
                  customRollNo = assignedRollNo,
                  isFeePaid = isFeePaidChecked,
                  feeAmount = feePaidAmount.toDoubleOrNull() ?: 400.0,
                  paymentMode = app.initialPaymentMode ?: "UPI",
                  paymentRef = app.initialPaymentRef ?: "ADM-DESK-ONBOARD",
                  monthlyDiscount = disc,
                  discountReason = if (disc != null && disc > 0) enrollDiscountReason.ifBlank { "Merit Concession" } else null,
                  customAdmissionDate = enrollAdmissionDate.trim().ifBlank { null }
                )
                enrollAdmissionTarget = null
                Toast.makeText(context, "Student ${enrolled?.name} enrolled successfully!", Toast.LENGTH_LONG).show()
              },
              colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
            ) {
              Text("Confirm Enrollment", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }

  generatedReceiptToView?.let { receipt ->
    ReceiptDialog(
      payment = receipt,
      centerName = centerInfo.name,
      onDismiss = { generatedReceiptToView = null }
    )
  }
}
