package com.example.ui.student

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.FeePayment
import com.example.data.model.Student
import com.example.data.repository.PathsalaRepository
import com.example.ui.components.MetricCard
import com.example.ui.components.ReceiptDialog
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.util.FeeCalculator
import java.net.URLEncoder

@Composable
fun StudentDashboardTab(
  student: Student,
  repository: PathsalaRepository
) {
  val context = LocalContext.current
  val courses by repository.courses.collectAsState()
  val batches by repository.batches.collectAsState()
  val payments by repository.payments.collectAsState()
  val centerInfo by repository.centerInfo.collectAsState()
  val admissions by repository.admissions.collectAsState()

  val studentCourse = courses.find { it.id == student.courseId }
  val studentBatch = batches.find { it.id == student.batchId }

  // Dynamic fee calculation as per batch & course, admission date, and website-adjusted payments
  val feeSummary = remember(student, courses, batches, payments, admissions) {
    FeeCalculator.calculateStudentFeeSummary(student, courses, batches, payments, admissions)
  }

  val monthlyFee = feeSummary.monthlyFee
  val totalPaid = feeSummary.totalPaid
  val totalDue = feeSummary.dueAmount
  val advanceAmount = feeSummary.advanceAmount
  val monthsElapsed = feeSummary.monthsElapsed
  val unpaidMonths = feeSummary.unpaidMonths
  val studentPayments = feeSummary.payments
  val approvedPayments = studentPayments.filter { it.status == "approved" }

  var showPaymentDialog by remember { mutableStateOf(false) }
  var selectedReceipt by remember { mutableStateOf<FeePayment?>(null) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Student Enrolled Card Banner
    item {
      Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "ENROLLED PROGRAM",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = IndigoPrimary,
                letterSpacing = 1.sp
              )
              Text(
                text = studentCourse?.title ?: "Standard Academic Curriculum",
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 2.dp)
              )
            }
            StatusBadge(status = student.status)
          }

          // Metadata Grid
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .background(Color(0xFFF8FAFC))
              .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text(text = "Class Standard", fontSize = 11.sp, color = SlateTextSecondary)
              Text(
                text = student.studentClass ?: "Class 10",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = SlateTextPrimary
              )
            }
            Column {
              Text(text = "Batch Schedule", fontSize = 11.sp, color = SlateTextSecondary)
              Text(
                text = studentBatch?.timing ?: "Evening Batch",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = SlateTextPrimary
              )
            }
            Column {
              Text(text = "Delivery Mode", fontSize = 11.sp, color = SlateTextSecondary)
              Text(
                text = if (student.mode == "online") "Online Live" else "Offline",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (student.mode == "online") CyanAccent else IndigoPrimary
              )
            }
          }

          // Fee Structure & Discount Info Row
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(Color(0xFFEFF6FF))
              .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "Standard: ₹${feeSummary.standardMonthlyFee.toInt()}/mo" +
                  if (feeSummary.monthlyDiscount > 0) " • Discount: -₹${feeSummary.monthlyDiscount.toInt()}/mo" else "",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1E3A8A)
              )
              Text(
                text = "Admitted: ${feeSummary.admissionMonth} (${feeSummary.billedMonthsCount} billing months elapsed)",
                fontSize = 10.sp,
                color = SlateTextSecondary
              )
            }
            Text(
              text = "Net: ₹${feeSummary.effectiveMonthlyFee.toInt()}/mo",
              fontSize = 12.sp,
              fontWeight = FontWeight.Black,
              color = IndigoPrimary
            )
          }
        }
      }
    }

    // Financial Overview Metric Cards (4 Cards)
    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          MetricCard(
            title = "Payable from Admission",
            value = "₹${feeSummary.payableFromAdmissionMonth.toInt()}",
            subtitle = "Since ${feeSummary.admissionMonth} (${feeSummary.billedMonthsCount} Mos)",
            icon = Icons.Default.AccountBalanceWallet,
            iconTint = IndigoPrimary,
            modifier = Modifier.weight(1f)
          )
          MetricCard(
            title = "Discount Allowed",
            value = "₹${feeSummary.totalDiscountAllowed.toInt()}",
            subtitle = if (feeSummary.monthlyDiscount > 0) "₹${feeSummary.monthlyDiscount.toInt()}/mo Applied" else "Concessions",
            icon = Icons.Default.Discount,
            iconTint = Color(0xFF7C3AED),
            modifier = Modifier.weight(1f)
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          MetricCard(
            title = "Total Paid",
            value = "₹${totalPaid.toInt()}",
            subtitle = "${approvedPayments.size} Verified Receipts",
            icon = Icons.Default.CheckCircle,
            iconTint = EmeraldSuccess,
            modifier = Modifier.weight(1f)
          )
          MetricCard(
            title = "Pending Due",
            value = if (totalDue > 0) "₹${totalDue.toInt()}" else "₹0",
            subtitle = when {
              totalDue > 0 -> "${unpaidMonths.size} Months Due"
              advanceAmount > 0 -> "Advance: ₹${advanceAmount.toInt()}"
              else -> "All Cleared"
            },
            icon = if (totalDue > 0) Icons.Default.Warning else Icons.Default.Verified,
            iconTint = if (totalDue > 0) Color(0xFFDC2626) else EmeraldSuccess,
            modifier = Modifier.weight(1f)
          )
        }
      }
    }

    // Pay Fee CTA Banner if there is any due
    if (totalDue > 0) {
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
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
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Tuition Fee Due Notice",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF991B1B)
              )
              Text(
                text = "₹${totalDue.toInt()} due since admission (${feeSummary.admissionDate}): ${unpaidMonths.joinToString { it.monthLabel.split(" ")[0] }}",
                fontSize = 12.sp,
                color = Color(0xFFB91C1C),
                modifier = Modifier.padding(top = 2.dp)
              )
            }
            Button(
              onClick = { showPaymentDialog = true },
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
            ) {
              Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Pay Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // Month-by-Month Schedule
    item {
      SectionHeader(
        title = "Monthly Fee Schedule",
        subtitle = "From admission date (${feeSummary.admissionDate}) with payment adjustments"
      )
    }

    items(monthsElapsed) { m ->
      val isPaid = m.isPaid
      val isPending = m.isPending
      val matchingPayment = m.matchingPayment

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
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                  when {
                    isPaid -> EmeraldLight
                    isPending -> AmberLight
                    else -> Color(0xFFFEF2F2)
                  }
                ),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = when {
                  isPaid -> Icons.Default.Check
                  isPending -> Icons.Default.Schedule
                  else -> Icons.Default.CalendarMonth
                },
                contentDescription = null,
                tint = when {
                  isPaid -> EmeraldSuccess
                  isPending -> AmberPending
                  else -> Color(0xFFDC2626)
                },
                modifier = Modifier.size(18.dp)
              )
            }

            Column {
              Text(
                text = m.monthLabel,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Standard: ₹${m.standardFee.toInt()}" +
                  (if (m.discount > 0) " • Disc: -₹${m.discount.toInt()}" else "") +
                  " • Net: ₹${m.netPayable.toInt()}",
                fontSize = 11.sp,
                color = SlateTextSecondary
              )
              Text(
                text = when {
                  isPaid -> "Status: Paid in Full (₹${m.paidAmount.toInt()})"
                  isPending -> "Status: Verification Pending"
                  m.paidAmount > 0 -> "Status: Partially Paid (Paid: ₹${m.paidAmount.toInt()} • Due: ₹${m.dueAmount.toInt()})"
                  else -> "Status: Due (₹${m.dueAmount.toInt()})"
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isPaid) EmeraldSuccess else if (isPending) AmberPending else Color(0xFFDC2626),
                fontFamily = FontFamily.Monospace
              )
            }
          }

          if (isPaid && matchingPayment != null) {
            TextButton(
              onClick = { selectedReceipt = matchingPayment },
              shape = RoundedCornerShape(10.dp)
            ) {
              Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Receipt", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = IndigoPrimary)
            }
          } else if (isPending) {
            StatusBadge(status = "pending")
          } else {
            Button(
              onClick = { showPaymentDialog = true },
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
              Text("Pay ₹${m.dueAmount.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // Payment History List
    if (studentPayments.isNotEmpty()) {
      item {
        Spacer(modifier = Modifier.height(8.dp))
        SectionHeader(
          title = "Fee Payment History",
          subtitle = "All submitted receipts and approval statuses"
        )
      }

      items(studentPayments) { pay ->
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
              Text(
                text = pay.month,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "${pay.receiptNo} • ${pay.paymentDate}",
                fontSize = 11.sp,
                color = SlateTextSecondary,
                fontFamily = FontFamily.Monospace
              )
              if (!pay.transactionRef.isNullOrBlank()) {
                Text(
                  text = "Ref: ${pay.transactionRef}",
                  fontSize = 10.sp,
                  color = IndigoPrimary,
                  fontFamily = FontFamily.Monospace
                )
              }
            }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = "₹${pay.finalAmountPaid.toInt()}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = FontFamily.Monospace
              )
              if (pay.status == "approved") {
                TextButton(
                  onClick = { selectedReceipt = pay },
                  contentPadding = PaddingValues(0.dp)
                ) {
                  Text("View Receipt", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IndigoPrimary)
                }
              } else {
                StatusBadge(status = pay.status)
              }
            }
          }
        }
      }
    }
  }

  // Payment Dialog with Live UPI QR and App Intent
  if (showPaymentDialog) {
    StudentUpiPaymentDialog(
      student = student,
      monthlyFee = monthlyFee,
      unpaidMonths = if (unpaidMonths.isNotEmpty()) {
        unpaidMonths.map { Pair(it.monthKey, it.monthLabel) }
      } else {
        listOf(Pair("Current", "Monthly Fee"))
      },
      centerUpiId = centerInfo.upiId,
      centerUpiName = centerInfo.upiName,
      onDismiss = { showPaymentDialog = false },
      onSubmit = { monthsToPay, amount, mode, ref, remarks ->
        repository.submitStudentFeePayment(student, monthsToPay, amount, mode, ref, remarks)
        showPaymentDialog = false
        Toast.makeText(context, "Payment submitted for Admin approval! Receipt generated once verified.", Toast.LENGTH_LONG).show()
      }
    )
  }

  // Receipt Viewer Dialog
  selectedReceipt?.let { pay ->
    ReceiptDialog(
      payment = pay,
      centerName = centerInfo.name,
      onDismiss = { selectedReceipt = null }
    )
  }
}

@Composable
fun StudentUpiPaymentDialog(
  student: Student,
  monthlyFee: Double,
  unpaidMonths: List<Pair<String, String>>,
  centerUpiId: String,
  centerUpiName: String,
  onDismiss: () -> Unit,
  onSubmit: (List<String>, Double, String, String, String?) -> Unit
) {
  val context = LocalContext.current
  val clipboard = LocalClipboardManager.current

  val selectedMonths = remember { mutableStateListOf<String>().apply {
    if (unpaidMonths.isNotEmpty()) add(unpaidMonths[0].first)
  } }

  var paymentMode by remember { mutableStateOf("UPI") } // "UPI" or "CASH"
  var utrNumber by remember { mutableStateOf("") }
  var remarks by remember { mutableStateOf("") }

  val totalAmount = selectedMonths.size * monthlyFee

  val encodedNote = try {
    URLEncoder.encode("Fee-${student.name}", "UTF-8")
  } catch (_: Exception) {
    "Fee"
  }
  val upiUriString = "upi://pay?pa=$centerUpiId&pn=${URLEncoder.encode(centerUpiName, "UTF-8")}&am=${totalAmount.toInt()}&cu=INR&tn=$encodedNote"
  val qrCodeUrl = "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=" + URLEncoder.encode(upiUriString, "UTF-8")

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = Color.White),
      border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 12.dp)
    ) {
      Column(
        modifier = Modifier
          .padding(20.dp)
          .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(text = "Pay Tuition Fee", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF0F172A))
            Text(text = "Select months & pay via UPI or Cash", fontSize = 11.sp, color = SlateTextSecondary)
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close")
          }
        }

        HorizontalDivider(color = SlateBorder)

        // Select Months to Pay
        Text(text = "1. Select Months:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SlateTextPrimary)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          unpaidMonths.forEach { (mKey, mLabel) ->
            val isSelected = selectedMonths.contains(mKey)
            FilterChip(
              selected = isSelected,
              onClick = {
                if (isSelected) {
                  if (selectedMonths.size > 1) selectedMonths.remove(mKey)
                } else {
                  selectedMonths.add(mKey)
                }
              },
              label = { Text(mLabel.split(" ")[0], fontSize = 11.sp) },
              shape = RoundedCornerShape(10.dp)
            )
          }
        }

        // Amount Box
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFEEF2FF))
            .padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(text = "Total Payable Amount:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = IndigoPrimary)
          Text(
            text = "₹${totalAmount.toInt()}",
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF1E1B4B),
            fontFamily = FontFamily.Monospace
          )
        }

        // Mode Switcher (UPI / Cash)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(12.dp))
              .background(if (paymentMode == "UPI") IndigoPrimary else Color(0xFFF1F5F9))
              .clickable { paymentMode = "UPI" }
              .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "UPI QR / App",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = if (paymentMode == "UPI") Color.White else Color(0xFF475569)
            )
          }
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(12.dp))
              .background(if (paymentMode == "CASH") EmeraldSuccess else Color(0xFFF1F5F9))
              .clickable { paymentMode = "CASH" }
              .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "Cash at Center",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = if (paymentMode == "CASH") Color.White else Color(0xFF475569)
            )
          }
        }

        if (paymentMode == "UPI") {
          // UPI QR and Deep link button
          Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            AsyncImage(
              model = qrCodeUrl,
              contentDescription = "UPI Payment QR",
              modifier = Modifier
                .size(130.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, SlateBorder, RoundedCornerShape(12.dp))
            )

            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Text(text = "UPI ID: $centerUpiId", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
              IconButton(
                onClick = {
                  clipboard.setText(AnnotatedString(centerUpiId))
                  Toast.makeText(context, "UPI ID copied!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(24.dp)
              ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy UPI", modifier = Modifier.size(14.dp))
              }
            }

            // Launch UPI app intent
            OutlinedButton(
              onClick = {
                try {
                  val intent = Intent(Intent.ACTION_VIEW, Uri.parse(upiUriString))
                  context.startActivity(Intent.createChooser(intent, "Pay via UPI App"))
                } catch (e: Exception) {
                  Toast.makeText(context, "No UPI app found. Please scan the QR code.", Toast.LENGTH_SHORT).show()
                }
              },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Open in GPay / PhonePe / Paytm", fontSize = 12.sp)
            }

            OutlinedTextField(
              value = utrNumber,
              onValueChange = { utrNumber = it },
              label = { Text("Enter UPI UTR / Transaction Ref *") },
              placeholder = { Text("e.g. 423981023412") },
              singleLine = true,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth()
            )
          }
        } else {
          // Cash notice
          Surface(
            color = EmeraldLight,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0))
          ) {
            Text(
              text = "Please deposit ₹${totalAmount.toInt()} at the Pixel Pathsala center reception desk. Once submitted, your educator or admin will verify and issue an official receipt.",
              fontSize = 12.sp,
              color = Color(0xFF065F46),
              modifier = Modifier.padding(12.dp)
            )
          }
        }

        Button(
          onClick = {
            onSubmit(selectedMonths.toList(), totalAmount, paymentMode, utrNumber, remarks)
          },
          enabled = if (paymentMode == "UPI") utrNumber.isNotBlank() else true,
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
        ) {
          Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Submit Fee Payment for Approval", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
      }
    }
  }
}
