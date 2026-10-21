package com.example.ui.admin

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdmissionApplication
import com.example.data.model.Batch
import com.example.data.model.Course
import com.example.data.model.FeePayment
import com.example.ui.theme.*

@Composable
fun AdminApprovalsTab(
  payments: List<FeePayment>,
  admissions: List<AdmissionApplication>,
  courses: List<Course>,
  batches: List<Batch>,
  onApprovePayment: (String) -> Unit,
  onRejectPayment: (FeePayment) -> Unit,
  onApproveAdmission: (AdmissionApplication) -> Unit,
  onRejectAdmission: (String, String) -> Unit,
  onDeleteAdmission: (String) -> Unit
) {
  var selectedSubTab by remember { mutableIntStateOf(0) } // 0: Fee Approvals, 1: Admissions Queue

  val pendingPayments = remember(payments) {
    payments.filter { it.status.equals("pending", ignoreCase = true) }
  }
  val pendingAdmissions = remember(admissions) {
    admissions.filter { it.status.equals("pending", ignoreCase = true) }
  }

  Column(modifier = Modifier.fillMaxSize()) {
    // Top Subtabs matching Screenshot 1
    Surface(
      color = Color.White,
      tonalElevation = 2.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Fee Approvals Subtab
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
            Icon(
              imageVector = Icons.Default.CreditCard,
              contentDescription = null,
              tint = if (selectedSubTab == 0) BrandPrimary else Slate600,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Fee Approvals (${pendingPayments.size})",
              fontSize = 13.sp,
              fontWeight = if (selectedSubTab == 0) FontWeight.Bold else FontWeight.Medium,
              color = if (selectedSubTab == 0) Slate900 else Slate600
            )
          }
        }

        // Admissions Queue Subtab
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
              imageVector = Icons.Default.PersonAdd,
              contentDescription = null,
              tint = if (selectedSubTab == 1) BrandPrimary else Slate600,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Admissions Queue (${pendingAdmissions.size})",
              fontSize = 13.sp,
              fontWeight = if (selectedSubTab == 1) FontWeight.Bold else FontWeight.Medium,
              color = if (selectedSubTab == 1) Slate900 else Slate600
            )
          }
        }
      }
    }

    if (selectedSubTab == 0) {
      // Fee Approvals View (Screenshot 1)
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        item {
          Column {
            Text(
              text = "Pending Student Fee Approvals",
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = Slate900
            )
            Text(
              text = "Student self-submitted UPI transfers & cash counter deposits",
              fontSize = 12.sp,
              color = Slate600
            )
          }
        }

        if (pendingPayments.isEmpty()) {
          item {
            Spacer(modifier = Modifier.height(10.dp))
            // Empty State Card matching Screenshot 1
            Card(
              shape = RoundedCornerShape(18.dp),
              colors = CardDefaults.cardColors(containerColor = Color.White),
              elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 42.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
              ) {
                Box(
                  modifier = Modifier
                    .size(54.dp)
                    .background(SuccessGreen, CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                  )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                  text = "All Payment Requests Cleared!",
                  fontSize = 17.sp,
                  fontWeight = FontWeight.Bold,
                  color = Slate900,
                  textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                  text = "There are no student fee submissions pending admin review.",
                  fontSize = 12.sp,
                  color = Slate600,
                  textAlign = TextAlign.Center
                )
              }
            }
          }
        } else {
          items(pendingPayments) { p ->
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
                  Column {
                    Text(text = p.studentName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Slate900)
                    Text(text = "Receipt: ${p.receiptNo} • ${p.month}", fontSize = 12.sp, color = Slate600)
                  }
                  Text(
                    text = "₹${p.finalAmountPaid.toInt()}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BrandPrimary
                  )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Mode: ${p.paymentMode} • Ref: ${p.transactionRef ?: "Direct Submit"}", fontSize = 12.sp, color = Slate700)
                if (!p.remarks.isNullOrBlank()) {
                  Text(text = "Notes: ${p.remarks}", fontSize = 11.sp, color = Slate500)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  OutlinedButton(
                    onClick = { onRejectPayment(p) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                  ) {
                    Text("Reject", fontWeight = FontWeight.Bold)
                  }

                  Button(
                    onClick = { onApprovePayment(p.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                  ) {
                    Text("Approve Payment", fontWeight = FontWeight.Bold, color = Color.White)
                  }
                }
              }
            }
          }
        }
      }
    } else {
      // Admissions Queue View
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        item {
          Column {
            Text(
              text = "Student Admissions Queue",
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = Slate900
            )
            Text(
              text = "Review, approve, or reject new admission applications",
              fontSize = 12.sp,
              color = Slate600
            )
          }
        }

        if (pendingAdmissions.isEmpty()) {
          item {
            Spacer(modifier = Modifier.height(10.dp))
            Card(
              shape = RoundedCornerShape(18.dp),
              colors = CardDefaults.cardColors(containerColor = Color.White),
              elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 42.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
              ) {
                Box(
                  modifier = Modifier
                    .size(54.dp)
                    .background(SuccessGreen, CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                  )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                  text = "All Admissions Cleared!",
                  fontSize = 17.sp,
                  fontWeight = FontWeight.Bold,
                  color = Slate900,
                  textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                  text = "There are no student admission forms pending admin review.",
                  fontSize = 12.sp,
                  color = Slate600,
                  textAlign = TextAlign.Center
                )
              }
            }
          }
        } else {
          items(pendingAdmissions) { adm ->
            val course = courses.firstOrNull { it.id == adm.targetCourseId }
            val batch = batches.firstOrNull { it.courseId == adm.targetCourseId }

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
                  Column {
                    Text(text = adm.studentName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Slate900)
                    Text(text = "App ID: ${adm.applicationNo} • ${course?.title ?: "Standard Course"}", fontSize = 12.sp, color = Slate600)
                  }
                  Surface(
                    color = BrandGoldLight,
                    shape = RoundedCornerShape(6.dp)
                  ) {
                    Text(
                      text = "PENDING",
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      color = BrandGoldDark,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                  }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Course: ${course?.title ?: "Computer Science"}", fontSize = 12.sp, color = Slate800)
                Text(text = "Batch: ${batch?.name ?: "Morning Batch"}", fontSize = 12.sp, color = Slate600)
                Text(text = "Mobile: ${adm.mobile} • Guardian: ${adm.guardianName} (${adm.guardianPhone})", fontSize = 11.sp, color = Slate600)

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  IconButton(
                    onClick = { onDeleteAdmission(adm.id) }
                  ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed)
                  }

                  OutlinedButton(
                    onClick = { onRejectAdmission(adm.id, "Seat unavailable or documentation pending") },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                  ) {
                    Text("Reject", fontWeight = FontWeight.Bold)
                  }

                  Button(
                    onClick = { onApproveAdmission(adm) },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                  ) {
                    Text("Approve", fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
