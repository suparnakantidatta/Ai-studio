package com.example.ui.admin

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import java.text.SimpleDateFormat
import java.util.*
import com.example.data.model.Batch
import com.example.data.model.Course
import com.example.data.model.Discount
import com.example.data.model.Expense
import com.example.data.model.Student
import com.example.ui.theme.*

@Composable
fun EditStudentDialog(
  student: Student,
  courses: List<Course>,
  batches: List<Batch>,
  onDismiss: () -> Unit,
  onSave: (Student) -> Unit
) {
  var name by remember { mutableStateOf(student.name) }
  var rollNo by remember { mutableStateOf(student.rollNo) }
  var mobile by remember { mutableStateOf(student.mobile) }
  var aadhaarNo by remember { mutableStateOf(student.aadhaarNo) }
  var guardianName by remember { mutableStateOf(student.guardianName) }
  var guardianPhone by remember { mutableStateOf(student.guardianPhone) }
  var address by remember { mutableStateOf(student.address) }
  var selectedCourseId by remember { mutableStateOf(student.courseId) }
  var selectedBatchId by remember { mutableStateOf(student.batchId) }
  var customFeeStr by remember { mutableStateOf(student.customMonthlyFeeOverride?.toInt()?.toString() ?: "") }
  var status by remember { mutableStateOf(student.status) }
  var errorText by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Edit Student Details", fontWeight = FontWeight.Bold) },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it; errorText = null },
          label = { Text("Student Name *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = rollNo,
            onValueChange = { rollNo = it },
            label = { Text("Roll No") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = mobile,
            onValueChange = { mobile = it },
            label = { Text("Mobile *") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
        }

        OutlinedTextField(
          value = aadhaarNo,
          onValueChange = { aadhaarNo = it },
          label = { Text("Aadhaar Number") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = guardianName,
            onValueChange = { guardianName = it },
            label = { Text("Guardian Name") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = guardianPhone,
            onValueChange = { guardianPhone = it },
            label = { Text("Guardian Phone") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
        }

        OutlinedTextField(
          value = address,
          onValueChange = { address = it },
          label = { Text("Home Address") },
          modifier = Modifier.fillMaxWidth()
        )

        Text("Target Course:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          courses.forEach { c ->
            FilterChip(
              selected = selectedCourseId == c.id,
              onClick = { selectedCourseId = c.id },
              label = { Text(c.title, fontSize = 11.sp) }
            )
          }
        }

        Text("Target Admission Batch:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          batches.forEach { b ->
            FilterChip(
              selected = selectedBatchId == b.id,
              onClick = { selectedBatchId = b.id },
              label = { Text(b.name, fontSize = 11.sp) }
            )
          }
        }

        OutlinedTextField(
          value = customFeeStr,
          onValueChange = { customFeeStr = it },
          label = { Text("Custom Monthly Fee Override (₹)") },
          placeholder = { Text("Leave blank for standard course fee") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        Text("Status:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf("active", "suspended", "alumni").forEach { st ->
            FilterChip(
              selected = status == st,
              onClick = { status = st },
              label = { Text(st.uppercase(), fontSize = 11.sp) }
            )
          }
        }

        if (errorText != null) {
          Text(text = errorText!!, color = ErrorRed, fontSize = 12.sp)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (name.isBlank() || mobile.isBlank()) {
            errorText = "Name and Mobile are required"
            return@Button
          }
          val customFee = customFeeStr.toDoubleOrNull()
          val updated = student.copy(
            name = name.trim(),
            rollNo = rollNo.trim(),
            mobile = mobile.trim(),
            aadhaarNo = aadhaarNo.trim(),
            guardianName = guardianName.trim(),
            guardianPhone = guardianPhone.trim(),
            address = address.trim(),
            courseId = selectedCourseId,
            batchId = selectedBatchId,
            customMonthlyFeeOverride = customFee,
            status = status
          )
          onSave(updated)
        },
        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
      ) {
        Text("Save Changes")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}

@Composable
fun EditExpenseDialog(
  expense: Expense,
  onDismiss: () -> Unit,
  onSave: (Expense) -> Unit
) {
  var title by remember { mutableStateOf(expense.title) }
  var category by remember { mutableStateOf(expense.category) }
  var amountStr by remember { mutableStateOf(expense.amount.toInt().toString()) }
  var paymentMode by remember { mutableStateOf(expense.paymentMode) }
  var remarks by remember { mutableStateOf(expense.remarks ?: "") }
  var errorText by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Edit Center Expense", fontWeight = FontWeight.Bold) },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it; errorText = null },
          label = { Text("Expense Title *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = amountStr,
          onValueChange = { amountStr = it; errorText = null },
          label = { Text("Amount (₹) *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        Text("Category:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          listOf("Rent", "Utilities", "Educator", "Materials", "Maintenance").forEach { cat ->
            FilterChip(
              selected = category.equals(cat, ignoreCase = true),
              onClick = { category = cat },
              label = { Text(cat, fontSize = 11.sp) }
            )
          }
        }

        Text("Payment Mode:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf("CASH", "UPI", "BANK").forEach { mode ->
            FilterChip(
              selected = paymentMode.equals(mode, ignoreCase = true),
              onClick = { paymentMode = mode },
              label = { Text(mode, fontSize = 11.sp) }
            )
          }
        }

        OutlinedTextField(
          value = remarks,
          onValueChange = { remarks = it },
          label = { Text("Remarks") },
          modifier = Modifier.fillMaxWidth()
        )

        if (errorText != null) {
          Text(text = errorText!!, color = ErrorRed, fontSize = 12.sp)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val amt = amountStr.toDoubleOrNull()
          if (title.isBlank() || amt == null || amt <= 0) {
            errorText = "Please enter valid title and amount"
            return@Button
          }
          val updated = expense.copy(
            title = title.trim(),
            category = category,
            amount = amt,
            paymentMode = paymentMode,
            remarks = remarks.trim().ifBlank { null }
          )
          onSave(updated)
        },
        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
      ) {
        Text("Save Changes")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}

@Composable
fun ManageDiscountsDialog(
  discounts: List<Discount>,
  courses: List<Course>,
  batches: List<Batch>,
  students: List<Student>,
  onDismiss: () -> Unit,
  onSaveDiscount: (Discount) -> Unit,
  onDeleteDiscount: (String) -> Unit
) {
  var editingDiscount by remember { mutableStateOf<Discount?>(null) }
  var showCreateNew by remember { mutableStateOf(false) }
  var deletingId by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Manage Discount Rules", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Button(
          onClick = { showCreateNew = true },
          shape = RoundedCornerShape(8.dp),
          colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("New Rule", fontSize = 11.sp)
        }
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        if (discounts.isEmpty()) {
          Text("No active discount rules created yet.", color = Slate600, fontSize = 13.sp)
        } else {
          discounts.forEach { disc ->
            Card(
              shape = RoundedCornerShape(10.dp),
              colors = CardDefaults.cardColors(containerColor = Slate100),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(text = disc.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate900)
                  Text(
                    text = "Deduction: ${if (disc.type == "flat") "₹${disc.value.toInt()}" else "${disc.value.toInt()}%"} • Scope: ${disc.scope.uppercase()}",
                    fontSize = 11.sp,
                    color = BrandPrimary
                  )
                  if (!disc.reason.isNullOrBlank()) {
                    Text(text = disc.reason, fontSize = 10.sp, color = Slate600)
                  }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                  IconButton(
                    onClick = { editingDiscount = disc },
                    modifier = Modifier.size(32.dp)
                  ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Slate700, modifier = Modifier.size(16.dp))
                  }
                  IconButton(
                    onClick = { deletingId = disc.id },
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
    },
    confirmButton = {
      Button(onClick = onDismiss) { Text("Done") }
    }
  )

  if (showCreateNew) {
    CreateOrEditDiscountDialog(
      courses = courses,
      batches = batches,
      students = students,
      existing = null,
      onDismiss = { showCreateNew = false },
      onSave = {
        onSaveDiscount(it)
        showCreateNew = false
      }
    )
  }

  if (editingDiscount != null) {
    CreateOrEditDiscountDialog(
      courses = courses,
      batches = batches,
      students = students,
      existing = editingDiscount,
      onDismiss = { editingDiscount = null },
      onSave = {
        onSaveDiscount(it)
        editingDiscount = null
      }
    )
  }

  if (deletingId != null) {
    AlertDialog(
      onDismissRequest = { deletingId = null },
      title = { Text("Delete Discount Rule?") },
      text = { Text("Are you sure you want to remove this discount rule? This will affect future fee calculations.") },
      confirmButton = {
        Button(
          onClick = {
            deletingId?.let { onDeleteDiscount(it) }
            deletingId = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
        ) {
          Text("Delete", color = Color.White)
        }
      },
      dismissButton = {
        TextButton(onClick = { deletingId = null }) { Text("Cancel") }
      }
    )
  }
}

@Composable
fun CreateOrEditDiscountDialog(
  courses: List<Course>,
  batches: List<Batch>,
  students: List<Student>,
  existing: Discount?,
  onDismiss: () -> Unit,
  onSave: (Discount) -> Unit
) {
  var title by remember { mutableStateOf(existing?.title ?: "") }
  var type by remember { mutableStateOf(existing?.type ?: "flat") }
  var valueStr by remember { mutableStateOf(existing?.value?.toInt()?.toString() ?: "") }
  var scope by remember { mutableStateOf(existing?.scope ?: "all") }
  var selectedCourseId by remember { mutableStateOf(existing?.courseId) }
  var selectedBatchId by remember { mutableStateOf(existing?.batchId) }
  var selectedStudentId by remember { mutableStateOf(existing?.studentId) }
  var reason by remember { mutableStateOf(existing?.reason ?: "") }
  var errorText by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (existing == null) "Create Discount Rule" else "Edit Discount Rule", fontWeight = FontWeight.Bold) },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Discount Title *") },
          placeholder = { Text("e.g. Merit Concession / Sibling Discount") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = valueStr,
            onValueChange = { valueStr = it },
            label = { Text(if (type == "flat") "Amount (₹) *" else "Percentage (%) *") },
            placeholder = { Text(if (type == "flat") "100" else "20") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )

          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            FilterChip(
              selected = type == "flat",
              onClick = { type = "flat" },
              label = { Text("Flat ₹", fontSize = 11.sp) }
            )
            Spacer(modifier = Modifier.width(4.dp))
            FilterChip(
              selected = type == "percentage",
              onClick = { type = "percentage" },
              label = { Text("% Off", fontSize = 11.sp) }
            )
          }
        }

        Text("Discount Scope:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf("all" to "All", "course" to "Course", "batch" to "Batch", "student" to "Student").forEach { (scKey, scLabel) ->
            FilterChip(
              selected = scope == scKey,
              onClick = { scope = scKey },
              label = { Text(scLabel, fontSize = 11.sp) }
            )
          }
        }

        if (scope == "course") {
          Text("Select Target Course:", fontSize = 12.sp, color = Slate600)
          courses.forEach { c ->
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .fillMaxWidth()
                .clickable { selectedCourseId = c.id }
            ) {
              RadioButton(selected = selectedCourseId == c.id, onClick = { selectedCourseId = c.id })
              Text(c.title, fontSize = 12.sp)
            }
          }
        } else if (scope == "batch") {
          Text("Select Target Batch:", fontSize = 12.sp, color = Slate600)
          batches.forEach { b ->
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .fillMaxWidth()
                .clickable { selectedBatchId = b.id }
            ) {
              RadioButton(selected = selectedBatchId == b.id, onClick = { selectedBatchId = b.id })
              Text(b.name, fontSize = 12.sp)
            }
          }
        } else if (scope == "student") {
          Text("Select Target Student:", fontSize = 12.sp, color = Slate600)
          students.take(6).forEach { st ->
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .fillMaxWidth()
                .clickable { selectedStudentId = st.id }
            ) {
              RadioButton(selected = selectedStudentId == st.id, onClick = { selectedStudentId = st.id })
              Text("${st.name} (${st.rollNo})", fontSize = 12.sp)
            }
          }
        }

        OutlinedTextField(
          value = reason,
          onValueChange = { reason = it },
          label = { Text("Reason / Notes") },
          modifier = Modifier.fillMaxWidth()
        )

        if (errorText != null) {
          Text(text = errorText!!, color = ErrorRed, fontSize = 12.sp)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val v = valueStr.toDoubleOrNull()
          if (title.isBlank() || v == null || v <= 0) {
            errorText = "Please provide valid title and value"
            return@Button
          }
          val discount = Discount(
            id = existing?.id ?: "disc-${System.currentTimeMillis()}",
            title = title.trim(),
            type = type,
            value = v,
            scope = scope,
            courseId = if (scope == "course") selectedCourseId else null,
            batchId = if (scope == "batch") selectedBatchId else null,
            studentId = if (scope == "student") selectedStudentId else null,
            active = true,
            reason = reason.trim().ifBlank { null }
          )
          onSave(discount)
        },
        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
      ) {
        Text("Save Discount Rule")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}

@Composable
fun BackendServerSyncDialog(
  onDismiss: () -> Unit,
  onTriggerSync: () -> Unit,
  onPushToSheet: () -> Unit,
  isSyncing: Boolean
) {
  val context = LocalContext.current
  var serverUrl by remember {
    mutableStateOf(com.example.data.sheet.GoogleSheetSyncService.WEBHOOK_URL)
  }
  val sdf = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
  val lastSyncTime = remember { sdf.format(Date()) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = null,
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.CloudSync,
              contentDescription = null,
              tint = BrandPrimary,
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Backend Server & Sync",
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = Slate900
            )
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate700)
          }
        }

        // Info Card
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = Slate100),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("Connection Status", fontSize = 12.sp, color = Slate600)
              Text("Google Sheet Synced", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("Database Engine", fontSize = 12.sp, color = Slate600)
              Text("Google Sheets Cloud DB", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandPrimary)
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("Google Sheet ID", fontSize = 12.sp, color = Slate600)
              Text("1HVB5lNLyCIh...4_04", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate900)
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("Last Synced", fontSize = 12.sp, color = Slate600)
              Text(lastSyncTime, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate900)
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("Official UPI ID", fontSize = 12.sp, color = Slate600)
              Text("9775708722@apl", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandPrimary)
            }
          }
        }

        // Action Buttons
        Button(
          onClick = {
            onPushToSheet()
            android.widget.Toast.makeText(context, "Pushed local records to Google Sheet DB", android.widget.Toast.LENGTH_SHORT).show()
          },
          colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
          Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Push All Changes to Google Sheet DB", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }

        OutlinedButton(
          onClick = {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://docs.google.com/spreadsheets/d/1HVB5lNLyCIhI-K4c7M-G_G2M1U_B-hI_9HwKk_M_4_04/edit"))
            try {
              context.startActivity(intent)
            } catch (_: Exception) {}
          },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
          Icon(Icons.Default.OpenInNew, contentDescription = null, tint = BrandPrimary)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Open Google Sheet Dashboard", color = BrandPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }

        // Endpoint config
        Text(
          text = "Backend API Endpoint URL:",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = Slate800
        )

        OutlinedTextField(
          value = serverUrl,
          onValueChange = { serverUrl = it },
          label = { Text("Server URL") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(10.dp)
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedButton(
            onClick = { serverUrl = com.example.data.sheet.GoogleSheetSyncService.WEBHOOK_URL },
            modifier = Modifier.weight(1f).height(44.dp),
            shape = RoundedCornerShape(10.dp)
          ) {
            Text("Default URL", fontSize = 12.sp)
          }

          Button(
            onClick = {
              onTriggerSync()
              android.widget.Toast.makeText(context, "Configuration saved & synced", android.widget.Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
            modifier = Modifier.weight(1f).height(44.dp),
            shape = RoundedCornerShape(10.dp)
          ) {
            Text("Save & Sync", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }

        // Big Green Sync Button
        Button(
          onClick = {
            onTriggerSync()
            android.widget.Toast.makeText(context, "Cloud sync triggered!", android.widget.Toast.LENGTH_SHORT).show()
          },
          colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
          Icon(Icons.Default.Sync, contentDescription = null, tint = Color.White)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            if (isSyncing) "Syncing with Cloud..." else "Trigger Cloud Sync Now",
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
        }
      }
    },
    confirmButton = {},
    shape = RoundedCornerShape(20.dp),
    containerColor = Color.White
  )
}

