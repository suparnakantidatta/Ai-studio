package com.example.ui.admin

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.repository.PathsalaRepository
import com.example.ui.student.FeeReceiptDialog
import com.example.ui.theme.*
import com.example.util.FeeCalculator
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMainScreen(
  admin: AdminAccount,
  repository: PathsalaRepository,
  onLogout: () -> Unit
) {
  var selectedTab by remember { mutableIntStateOf(0) }
  val coroutineScope = rememberCoroutineScope()

  val students by repository.students.collectAsState()
  val courses by repository.courses.collectAsState()
  val batches by repository.batches.collectAsState()
  val payments by repository.payments.collectAsState()
  val discounts by repository.discounts.collectAsState()
  val admissions by repository.admissions.collectAsState()
  val liveClasses by repository.liveClasses.collectAsState()
  val studyMaterials by repository.studyMaterials.collectAsState()
  val expenses by repository.expenses.collectAsState()
  val telemetries by repository.studentTelemetries.collectAsState()
  val isSyncing by repository.isSyncing.collectAsState()

  var viewReceiptPayment by remember { mutableStateOf<FeePayment?>(null) }
  var showManageDiscountsDialog by remember { mutableStateOf(false) }
  var showNewAdmissionDialog by remember { mutableStateOf(false) }
  var rejectingPayment by remember { mutableStateOf<FeePayment?>(null) }
  var showUpdateDialog by remember { mutableStateOf(false) }

  var editingStudent by remember { mutableStateOf<Student?>(null) }
  var deletingStudentId by remember { mutableStateOf<String?>(null) }
  var editingExpense by remember { mutableStateOf<Expense?>(null) }
  var deletingExpenseId by remember { mutableStateOf<String?>(null) }
  var deletingAdmissionId by remember { mutableStateOf<String?>(null) }

  val pendingPaymentsCount = remember(payments) {
    payments.count { it.status.equals("pending", ignoreCase = true) }
  }
  val pendingAdmissionsCount = remember(admissions) {
    admissions.count { it.status.equals("pending", ignoreCase = true) }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(text = "Admin Portal • Pixel Pathsala", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = "${admin.fullName} (${admin.roleTitle})", fontSize = 11.sp, color = BrandGoldLight)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandSecondary),
        actions = {
          IconButton(onClick = { showUpdateDialog = true }) {
            BadgedBox(
              badge = {
                if (com.example.util.UpdateManager.isUpdateAvailable()) {
                  Badge(containerColor = BrandGold) { Text("NEW", fontSize = 8.sp, color = BrandSecondary) }
                }
              }
            ) {
              Icon(
                imageVector = Icons.Default.SystemUpdate,
                contentDescription = "Check for Updates",
                tint = if (com.example.util.UpdateManager.isUpdateAvailable()) BrandGold else Color.White
              )
            }
          }
          IconButton(onClick = { repository.refreshData() }) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Refresh",
              tint = if (isSyncing) BrandGold else Color.White
            )
          }
          IconButton(onClick = onLogout) {
            Icon(imageVector = Icons.Default.Logout, contentDescription = "Logout", tint = Color.White)
          }
        }
      )
    },
    bottomBar = {
      NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
      ) {
        NavigationBarItem(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          icon = { Icon(Icons.Default.Dashboard, contentDescription = "Overview") },
          label = { Text("Overview") }
        )
        NavigationBarItem(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          icon = {
            BadgedBox(
              badge = {
                if (pendingPaymentsCount > 0) {
                  Badge(containerColor = BrandGold) { Text("$pendingPaymentsCount") }
                }
              }
            ) {
              Icon(Icons.Default.Payments, contentDescription = "Fee Desk")
            }
          },
          label = { Text("Fee Desk") }
        )
        NavigationBarItem(
          selected = selectedTab == 2,
          onClick = { selectedTab = 2 },
          icon = { Icon(Icons.Default.School, contentDescription = "Classes & Notes") },
          label = { Text("Classes & Notes") }
        )
        NavigationBarItem(
          selected = selectedTab == 3,
          onClick = { selectedTab = 3 },
          icon = {
            BadgedBox(
              badge = {
                if (pendingAdmissionsCount > 0) {
                  Badge { Text("$pendingAdmissionsCount") }
                }
              }
            ) {
              Icon(Icons.Default.AssignmentInd, contentDescription = "Admissions")
            }
          },
          label = { Text("Admissions") }
        )
        NavigationBarItem(
          selected = selectedTab == 4,
          onClick = { selectedTab = 4 },
          icon = { Icon(Icons.Default.ManageAccounts, contentDescription = "Operations") },
          label = { Text("Operations") }
        )
      }
    }
  ) { padding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .background(Slate50)
    ) {
      when (selectedTab) {
        0 -> AdminOverviewTab(
          students = students,
          payments = payments,
          courses = courses,
          admissions = admissions,
          expenses = expenses,
          onNavigateToFees = { selectedTab = 1 },
          onNavigateToAcademic = { selectedTab = 2 },
          onNavigateToAdmissions = { selectedTab = 3 },
          onNavigateToTracker = { selectedTab = 4 },
          onOpenNewAdmission = { showNewAdmissionDialog = true },
          onOpenUpdateDialog = { showUpdateDialog = true }
        )
        1 -> AdminFeeDeskTab(
          students = students,
          courses = courses,
          batches = batches,
          discounts = discounts,
          payments = payments,
          onCollectFee = { studentId, months, mode, ref, remarks ->
            coroutineScope.launch {
              repository.collectFeePayment(
                studentId = studentId,
                selectedMonths = months,
                paymentMode = mode,
                transactionRef = ref,
                remarks = remarks,
                collectorName = admin.fullName
              )
            }
          },
          onApprovePayment = { paymentId ->
            coroutineScope.launch {
              repository.approveFeePayment(paymentId, admin.fullName)
            }
          },
          onRejectPayment = { payment ->
            rejectingPayment = payment
          },
          onOpenDiscountDialog = { showManageDiscountsDialog = true },
          onViewReceipt = { viewReceiptPayment = it }
        )
        2 -> AdminAcademicTab(
          liveClasses = liveClasses,
          studyMaterials = studyMaterials,
          courses = courses,
          batches = batches,
          onAddClass = { session ->
            repository.addLiveClass(session)
          },
          onUpdateClass = { session ->
            repository.updateLiveClass(session)
          },
          onDeleteClass = { id ->
            repository.deleteLiveClass(id)
          },
          onAddNote = { note ->
            repository.addStudyMaterial(note)
          },
          onUpdateNote = { note ->
            repository.updateStudyMaterial(note)
          },
          onDeleteNote = { id ->
            repository.deleteStudyMaterial(id)
          }
        )
        3 -> AdminAdmissionsTab(
          admissions = admissions,
          courses = courses,
          batches = batches,
          onApproveAdmission = { appId, cId, bId ->
            coroutineScope.launch {
              repository.approveAdmissionApplication(appId, cId, bId)
            }
          },
          onRejectAdmission = { appId, reason ->
            coroutineScope.launch {
              repository.rejectAdmissionApplication(appId, reason)
            }
          },
          onDeleteAdmission = { appId ->
            deletingAdmissionId = appId
          },
          onOpenNewAdmission = { showNewAdmissionDialog = true }
        )
        4 -> AdminOperationsTab(
          students = students,
          telemetries = telemetries,
          courses = courses,
          batches = batches,
          expenses = expenses,
          payments = payments,
          onOpenNewAdmission = { showNewAdmissionDialog = true },
          onEditStudent = { editingStudent = it },
          onDeleteStudent = { deletingStudentId = it },
          onAddExpense = { exp -> repository.recordExpense(exp) },
          onEditExpense = { exp -> repository.updateExpense(exp) },
          onDeleteExpense = { expId -> repository.deleteExpense(expId) }
        )
      }

      if (viewReceiptPayment != null) {
        FeeReceiptDialog(
          payment = viewReceiptPayment!!,
          onDismiss = { viewReceiptPayment = null }
        )
      }

      if (showManageDiscountsDialog) {
        ManageDiscountsDialog(
          discounts = discounts,
          courses = courses,
          batches = batches,
          students = students,
          onDismiss = { showManageDiscountsDialog = false },
          onSaveDiscount = { newDisc ->
            coroutineScope.launch {
              repository.addDiscountRule(newDisc)
            }
          },
          onDeleteDiscount = { discId ->
            repository.deleteDiscountRule(discId)
          }
        )
      }

      if (editingStudent != null) {
        EditStudentDialog(
          student = editingStudent!!,
          courses = courses,
          batches = batches,
          onDismiss = { editingStudent = null },
          onSave = { updated ->
            repository.updateStudent(updated)
            editingStudent = null
          }
        )
      }

      if (deletingStudentId != null) {
        AlertDialog(
          onDismissRequest = { deletingStudentId = null },
          title = { Text("Delete Student Record?") },
          text = { Text("Are you sure you want to remove this student from the center directory?") },
          confirmButton = {
            Button(
              onClick = {
                deletingStudentId?.let { repository.deleteStudent(it) }
                deletingStudentId = null
              },
              colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
            ) {
              Text("Delete", color = Color.White)
            }
          },
          dismissButton = {
            TextButton(onClick = { deletingStudentId = null }) { Text("Cancel") }
          }
        )
      }

      if (deletingAdmissionId != null) {
        AlertDialog(
          onDismissRequest = { deletingAdmissionId = null },
          title = { Text("Discard Admission Inquiry?") },
          text = { Text("Are you sure you want to discard this inquiry?") },
          confirmButton = {
            Button(
              onClick = {
                deletingAdmissionId?.let {
                  coroutineScope.launch {
                    repository.rejectAdmissionApplication(it, "Discarded by admin")
                  }
                }
                deletingAdmissionId = null
              },
              colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
            ) {
              Text("Discard", color = Color.White)
            }
          },
          dismissButton = {
            TextButton(onClick = { deletingAdmissionId = null }) { Text("Cancel") }
          }
        )
      }

      if (editingExpense != null) {
        EditExpenseDialog(
          expense = editingExpense!!,
          onDismiss = { editingExpense = null },
          onSave = { updated ->
            repository.updateExpense(updated)
            editingExpense = null
          }
        )
      }

      if (showNewAdmissionDialog) {
        NewAdmissionDialog(
          courses = courses,
          batches = batches,
          existingStudentsCount = students.size,
          onDismiss = { showNewAdmissionDialog = false },
          onEnroll = { student, initAmt, mode ->
            coroutineScope.launch {
              repository.enrollNewStudent(student, initAmt, mode)
              showNewAdmissionDialog = false
            }
          }
        )
      }

      if (rejectingPayment != null) {
        RejectPaymentDialog(
          payment = rejectingPayment!!,
          onDismiss = { rejectingPayment = null },
          onConfirmReject = { reason ->
            coroutineScope.launch {
              repository.rejectFeePayment(rejectingPayment!!.id, reason, admin.fullName)
              rejectingPayment = null
            }
          }
        )
      }

      if (showUpdateDialog) {
        com.example.ui.common.AppUpdateDialog(
          onDismiss = { showUpdateDialog = false }
        )
      }
    }
  }
}

@Composable
fun AdminOverviewTab(
  students: List<Student>,
  payments: List<FeePayment>,
  courses: List<Course>,
  admissions: List<AdmissionApplication>,
  expenses: List<Expense>,
  onNavigateToFees: () -> Unit,
  onNavigateToAcademic: () -> Unit,
  onNavigateToAdmissions: () -> Unit,
  onNavigateToTracker: () -> Unit,
  onOpenNewAdmission: () -> Unit,
  onOpenUpdateDialog: () -> Unit
) {
  val totalCollected = remember(payments) {
    payments.filter { it.status == "approved" }.sumOf { it.finalAmountPaid }
  }
  val totalExpenses = remember(expenses) {
    expenses.sumOf { it.amount }
  }
  val pendingFees = remember(payments) {
    payments.count { it.status.equals("pending", ignoreCase = true) }
  }
  val pendingAdmissions = remember(admissions) {
    admissions.count { it.status.equals("pending", ignoreCase = true) }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    if (com.example.util.UpdateManager.isUpdateAvailable()) {
      com.example.ui.common.AppUpdateBanner(onClick = onOpenUpdateDialog)
    }

    // Prominent Pending Fee Approval Notification Card
    if (pendingFees > 0) {
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = WarningAmber.copy(alpha = 0.15f)),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onNavigateToFees() }
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.HourglassTop, contentDescription = null, tint = BrandGoldDark)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "$pendingFees Fee Approvals Pending",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Slate900
              )
              Text("Online & desk payments awaiting review", fontSize = 11.sp, color = Slate600)
            }
          }
          Button(
            onClick = onNavigateToFees,
            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
          ) {
            Text("Approve Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    // Prominent Pending Admission Inquiry Notification Card
    if (pendingAdmissions > 0) {
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BrandPrimary.copy(alpha = 0.12f)),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onNavigateToAdmissions() }
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = BrandPrimary)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "$pendingAdmissions New Admissions Pending",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Slate900
              )
              Text("Review & enroll pending student applicants", fontSize = 11.sp, color = Slate600)
            }
          }
          Button(
            onClick = onNavigateToAdmissions,
            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
          ) {
            Text("Enroll Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    Text(text = "Center Performance & Management", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Slate900)

    // Metric Cards Grid
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BrandPrimary),
        modifier = Modifier.weight(1f)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text(text = "Total Fees Collected", fontSize = 11.sp, color = BrandGoldLight)
          Spacer(modifier = Modifier.height(4.dp))
          Text(text = "₹${totalCollected.toInt()}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
          Text(text = "${payments.size} Receipts", fontSize = 10.sp, color = Slate200)
        }
      }

      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BrandGoldDark),
        modifier = Modifier.weight(1f)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text(text = "Active Students", fontSize = 11.sp, color = BrandGoldLight)
          Spacer(modifier = Modifier.height(4.dp))
          Text(text = "${students.size}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
          Text(text = "${courses.size} Active Courses", fontSize = 10.sp, color = Slate200)
        }
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.weight(1f)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text(text = "Center Expenses", fontSize = 11.sp, color = Slate600)
          Spacer(modifier = Modifier.height(4.dp))
          Text(text = "₹${totalExpenses.toInt()}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ErrorRed)
          Text(text = "Rent, Power, Materials", fontSize = 10.sp, color = Slate400)
        }
      }

      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.weight(1f)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text(text = "Pending Approvals", fontSize = 11.sp, color = Slate600)
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "${pendingFees + pendingAdmissions}",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = if ((pendingFees + pendingAdmissions) > 0) WarningAmber else SuccessGreen
          )
          Text(text = "Fees & Admissions", fontSize = 10.sp, color = Slate400)
        }
      }
    }

    // Quick Actions
    Card(
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = Color.White),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Quick Operations", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Slate900)
        Spacer(modifier = Modifier.height(12.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Button(
            onClick = onNavigateToFees,
            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Fee Desk", fontSize = 12.sp)
          }

          Button(
            onClick = onOpenNewAdmission,
            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("+ Admission", fontSize = 12.sp)
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Button(
            onClick = onNavigateToAcademic,
            colors = ButtonDefaults.buttonColors(containerColor = BrandSecondary),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Classes & Notes", fontSize = 12.sp)
          }

          Button(
            onClick = onNavigateToTracker,
            colors = ButtonDefaults.buttonColors(containerColor = BrandGoldDark),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.ManageAccounts, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Operations & GPS", fontSize = 12.sp)
          }
        }
      }
    }

    // Recent Payments
    Text(text = "Recent Transactions", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Slate900)
    payments.takeLast(4).reversed().forEach { p ->
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column {
            Text(text = p.studentName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Slate900)
            Text(text = "${p.receiptNo} • ${p.month.ifBlank { p.monthsCovered.joinToString() }}", fontSize = 11.sp, color = Slate600)
          }
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (p.status == "approved") SuccessGreenLight else WarningAmber.copy(alpha = 0.2f)
          ) {
            Text(
              text = "₹${p.finalAmountPaid.toInt()} (${p.status.uppercase()})",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = if (p.status == "approved") SuccessGreen else BrandGoldDark,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
fun AdminFeeDeskTab(
  students: List<Student>,
  courses: List<Course>,
  batches: List<Batch>,
  discounts: List<Discount>,
  payments: List<FeePayment>,
  onCollectFee: (studentId: String, months: List<String>, mode: String, ref: String?, remarks: String?) -> Unit,
  onApprovePayment: (paymentId: String) -> Unit,
  onRejectPayment: (FeePayment) -> Unit,
  onOpenDiscountDialog: () -> Unit,
  onViewReceipt: (FeePayment) -> Unit
) {
  var selectedStudent by remember { mutableStateOf<Student?>(students.firstOrNull()) }
  var searchQuery by remember { mutableStateOf("") }
  var paymentMode by remember { mutableStateOf("CASH") }
  var transactionRef by remember { mutableStateOf("") }
  var remarks by remember { mutableStateOf("") }
  var messageText by remember { mutableStateOf<String?>(null) }

  val pendingPayments = remember(payments) {
    payments.filter { it.status.equals("pending", ignoreCase = true) }
  }

  val filteredStudents = remember(searchQuery, students) {
    if (searchQuery.isBlank()) students
    else students.filter {
      it.name.contains(searchQuery, ignoreCase = true) ||
      it.rollNo.contains(searchQuery, ignoreCase = true) ||
      it.mobile.contains(searchQuery)
    }
  }

  val selectedCourse = courses.firstOrNull { it.id == selectedStudent?.courseId }
  val selectedBatch = batches.firstOrNull { it.id == selectedStudent?.batchId }

  val studentDues = remember(selectedStudent, courses, batches, discounts, payments) {
    selectedStudent?.let {
      FeeCalculator.calculateStudentDuesSummary(
        student = it,
        course = selectedCourse,
        batch = selectedBatch,
        discounts = discounts,
        payments = payments
      )
    }
  }

  val selectedMonths = remember(selectedStudent) { mutableStateListOf<String>() }

  LaunchedEffect(studentDues) {
    selectedMonths.clear()
    studentDues?.pendingMonths?.forEach {
      selectedMonths.add(it.yearMonth)
    }
  }

  val calculatedPayable = remember(selectedMonths.toList(), studentDues) {
    studentDues?.allMonths
      ?.filter { it.yearMonth in selectedMonths }
      ?.sumOf { it.netPayable } ?: 0.0
  }

  val calculatedDiscount = remember(selectedMonths.toList(), studentDues) {
    studentDues?.allMonths
      ?.filter { it.yearMonth in selectedMonths }
      ?.sumOf { it.discountAmount } ?: 0.0
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Header & Manage Discounts
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(text = "Fee Collection & Approval Desk", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Slate900)
          Text(text = "Review online submissions & collect fees", fontSize = 12.sp, color = Slate600)
        }
        OutlinedButton(
          onClick = onOpenDiscountDialog,
          shape = RoundedCornerShape(8.dp)
        ) {
          Icon(Icons.Default.Discount, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Discount Rules", fontSize = 12.sp)
        }
      }
    }

    // PENDING APPROVALS QUEUE (CRITICAL REQUEST)
    if (pendingPayments.isNotEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = BrandGoldLight.copy(alpha = 0.6f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.HourglassTop, contentDescription = null, tint = BrandGoldDark)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Pending Fee Approvals (${pendingPayments.size})",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  color = BrandGoldDark
                )
              }
            }
            Spacer(modifier = Modifier.height(10.dp))

            pendingPayments.forEach { p ->
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.White,
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp)
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Text(text = p.studentName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Slate900)
                    Text(text = "₹${p.finalAmountPaid.toInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = BrandPrimary)
                  }
                  Text(text = "Months: ${p.month.ifBlank { p.monthsCovered.joinToString() }}", fontSize = 11.sp, color = Slate600)
                  Text(text = "Mode: ${p.paymentMode} • Ref/UTR: ${p.transactionRef ?: "Center Desk"}", fontSize = 11.sp, color = Slate800)

                  Spacer(modifier = Modifier.height(8.dp))
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    TextButton(onClick = { onRejectPayment(p) }) {
                      Text("Reject", color = ErrorRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                      onClick = { onApprovePayment(p.id) },
                      colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                      shape = RoundedCornerShape(8.dp),
                      contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                      Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("Approve Payment", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    // Student Selector & Collection Form
    item {
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text(text = "Fee Collection Form", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Slate900)
          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search student by name, roll, mobile...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            filteredStudents.take(4).forEach { st ->
              val isSel = st.id == selectedStudent?.id
              FilterChip(
                selected = isSel,
                onClick = { selectedStudent = st; messageText = null },
                label = { Text("${st.name} (${st.rollNo})", fontSize = 12.sp) }
              )
            }
          }
        }
      }
    }

    // Multi-month Fee Box
    if (selectedStudent != null && studentDues != null) {
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(text = selectedStudent!!.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Slate900)
                Text(text = "Course: ${studentDues.courseTitle} • Batch: ${studentDues.batchName}", fontSize = 12.sp, color = Slate600)
                Text(text = "Monthly Fee: ₹${studentDues.standardMonthlyFee.toInt()} • Adm: ${studentDues.admissionMonth}", fontSize = 12.sp, color = BrandPrimary, fontWeight = FontWeight.SemiBold)
              }
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (studentDues.totalOutstandingDue > 0) ErrorRedLight else SuccessGreenLight
              ) {
                Text(
                  text = if (studentDues.totalOutstandingDue > 0) "DUE: ₹${studentDues.totalOutstandingDue.toInt()}" else "CLEAR",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (studentDues.totalOutstandingDue > 0) ErrorRed else SuccessGreen,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
              }
            }

            HorizontalDivider(color = Slate200)

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(text = "Months to Collect:", fontSize = 14.sp, fontWeight = FontWeight.Bold)
              Row {
                TextButton(
                  onClick = {
                    selectedMonths.clear()
                    studentDues.allMonths.filter { !it.isPaid }.forEach { selectedMonths.add(it.yearMonth) }
                  }
                ) {
                  Text("Select All Pending", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = { selectedMonths.clear() }) {
                  Text("Clear", fontSize = 11.sp)
                }
              }
            }

            studentDues.allMonths.forEach { m ->
              val isSelected = m.yearMonth in selectedMonths
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (m.isPaid) Slate100 else if (isSelected) BrandGoldLight.copy(alpha = 0.5f) else Color.White,
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(8.dp))
                  .clickable(enabled = !m.isPaid) {
                    if (isSelected) selectedMonths.remove(m.yearMonth)
                    else selectedMonths.add(m.yearMonth)
                  }
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Checkbox(
                    checked = isSelected || m.isPaid,
                    enabled = !m.isPaid,
                    onCheckedChange = { chk ->
                      if (chk == true) selectedMonths.add(m.yearMonth)
                      else selectedMonths.remove(m.yearMonth)
                    }
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(text = m.monthLabel, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    if (m.discountAmount > 0) {
                      Text(
                        text = "${m.discountName ?: "Discount"}: -₹${m.discountAmount.toInt()}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandGoldDark
                      )
                    }
                  }
                  Text(
                    text = if (m.isPaid) "PAID" else "₹${m.netPayable.toInt()}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (m.isPaid) SuccessGreen else Slate900
                  )
                }
              }
            }

            Surface(
              shape = RoundedCornerShape(10.dp),
              color = Slate100,
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text(text = "Selected Months (${selectedMonths.size}):", fontSize = 12.sp, color = Slate600)
                  Text(text = selectedMonths.joinToString(), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                if (calculatedDiscount > 0) {
                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "Discount Deductions:", fontSize = 12.sp, color = BrandGoldDark)
                    Text(text = "-₹${calculatedDiscount.toInt()}", fontSize = 12.sp, color = BrandGoldDark, fontWeight = FontWeight.Bold)
                  }
                }
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = Slate200)
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text(text = "Total Collection Amount:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Slate900)
                  Text(text = "₹${calculatedPayable.toInt()}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = BrandPrimary)
                }
              }
            }

            Text(text = "Payment Mode & Details:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              FilterChip(selected = paymentMode == "CASH", onClick = { paymentMode = "CASH" }, label = { Text("CASH") })
              FilterChip(selected = paymentMode == "UPI", onClick = { paymentMode = "UPI" }, label = { Text("UPI / Online") })
              FilterChip(selected = paymentMode == "BANK_TRANSFER", onClick = { paymentMode = "BANK_TRANSFER" }, label = { Text("Bank") })
            }

            OutlinedTextField(
              value = transactionRef,
              onValueChange = { transactionRef = it },
              label = { Text("Transaction Reference / Receipt Remarks") },
              placeholder = { Text("e.g. Center Desk Cash Receipt / UTR Ref") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )

            if (messageText != null) {
              Text(text = messageText!!, fontSize = 12.sp, color = SuccessGreen, fontWeight = FontWeight.Bold)
            }

            Button(
              onClick = {
                if (selectedMonths.isEmpty()) {
                  messageText = "Please select at least one month to collect."
                  return@Button
                }
                onCollectFee(
                  selectedStudent!!.id,
                  selectedMonths.toList(),
                  paymentMode,
                  transactionRef.ifBlank { null },
                  remarks.ifBlank { "Center desk fee collection" }
                )
                messageText = "Payment recorded successfully! Receipt generated."
              },
              colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("collect_fee_submit_button")
            ) {
              Icon(Icons.Default.Receipt, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Collect ₹${calculatedPayable.toInt()} & Issue Receipt", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // Transaction History Table
    item {
      Spacer(modifier = Modifier.height(12.dp))
      Text(text = "All Fee Transactions (${payments.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Slate900)
    }

    items(payments.reversed()) { p ->
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onViewReceipt(p) }
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column {
            Text(text = "${p.receiptNo} • ${p.studentName}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Slate900)
            Text(text = "Months: ${p.month.ifBlank { p.monthsCovered.joinToString() }}", fontSize = 12.sp, color = Slate600)
            Text(text = "Mode: ${p.paymentMode} • Status: ${p.status.uppercase()}", fontSize = 11.sp, color = if (p.status == "approved") SuccessGreen else WarningAmber)
          }
          Column(horizontalAlignment = Alignment.End) {
            Text(text = "₹${p.finalAmountPaid.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
            Text(text = "View Slip", fontSize = 11.sp, color = BrandPrimary, fontWeight = FontWeight.SemiBold)
          }
        }
      }
    }
  }
}

@Composable
fun RejectPaymentDialog(
  payment: FeePayment,
  onDismiss: () -> Unit,
  onConfirmReject: (reason: String) -> Unit
) {
  var reason by remember { mutableStateOf("Invalid transaction reference or unconfirmed receipt") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(text = "Reject Fee Payment", fontWeight = FontWeight.Bold) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Student: ${payment.studentName} (₹${payment.finalAmountPaid.toInt()})", fontSize = 13.sp)
        Text(text = "Provide reason for rejection:", fontSize = 12.sp, color = Slate600)
        OutlinedTextField(
          value = reason,
          onValueChange = { reason = it },
          label = { Text("Rejection Reason") },
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirmReject(reason) },
        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
      ) {
        Text("Confirm Rejection")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}

@Composable
fun AdminStudentsTab(
  students: List<Student>,
  courses: List<Course>,
  batches: List<Batch>,
  onOpenNewAdmission: () -> Unit,
  onEditStudent: (Student) -> Unit,
  onDeleteStudent: (String) -> Unit
) {
  var query by remember { mutableStateOf("") }
  val filtered = remember(query, students) {
    if (query.isBlank()) students
    else students.filter {
      it.name.contains(query, ignoreCase = true) ||
      it.rollNo.contains(query, ignoreCase = true) ||
      it.mobile.contains(query)
    }
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(text = "Student Directory (${students.size})", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Slate900)
        Button(
          onClick = onOpenNewAdmission,
          colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
          shape = RoundedCornerShape(8.dp)
        ) {
          Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("New Admission", fontSize = 12.sp)
        }
      }
      Spacer(modifier = Modifier.height(6.dp))
      OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        placeholder = { Text("Search by name, roll, mobile...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
      )
    }

    items(filtered) { st ->
      val course = courses.firstOrNull { it.id == st.courseId }
      val batch = batches.firstOrNull { it.id == st.batchId }
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(text = st.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Slate900)
            Text(text = st.rollNo, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = BrandPrimary)
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(text = "Course: ${course?.title ?: "Standard"}", fontSize = 12.sp, color = Slate600)
          Text(text = "Batch: ${batch?.name ?: "Morning"}", fontSize = 12.sp, color = Slate600)
          Text(text = "Mobile: ${st.mobile} • Guardian: ${st.guardianName}", fontSize = 12.sp, color = Slate800)
          Text(text = "Aadhaar: ${st.aadhaarNo} • Address: ${st.address}", fontSize = 11.sp, color = Slate600)
          Text(text = "Admission: ${st.admissionDate.take(10)}", fontSize = 11.sp, color = Slate400)

          Spacer(modifier = Modifier.height(10.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
          ) {
            OutlinedButton(
              onClick = { onEditStudent(st) },
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
              Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Edit", fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
              onClick = { onDeleteStudent(st.id) },
              modifier = Modifier.size(32.dp)
            ) {
              Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed, modifier = Modifier.size(18.dp))
            }
          }
        }
      }
    }
  }
}

@Composable
fun AdminTelemetryTrackerTab(
  students: List<Student>,
  telemetries: List<StudentTelemetry>
) {
  val context = LocalContext.current

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Text(text = "Student Live Location & Study Focus", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Slate900)
      Text(text = "GPS Coordinates, Battery level & Installed Games Monitoring", fontSize = 12.sp, color = Slate600)
    }

    items(students) { st ->
      val tel = telemetries.firstOrNull { it.studentId == st.id }
      val lat = tel?.latitude ?: 23.2324
      val lng = tel?.longitude ?: 87.8615
      val battery = tel?.batteryLevel ?: 85
      val isCharging = tel?.isCharging ?: false
      val games = tel?.installedGames ?: emptyList()
      val focusScore = tel?.studyFocusScore ?: 100

      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(text = st.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Slate900)
              Text(text = "Roll: ${st.rollNo} • Mob: ${st.mobile}", fontSize = 11.sp, color = Slate600)
            }
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (focusScore >= 80) SuccessGreenLight else ErrorRedLight
            ) {
              Text(
                text = "$focusScore% Focus",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (focusScore >= 80) SuccessGreen else ErrorRed,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Live Location Row
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Slate100,
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                  Text(text = "Live Location: $lat, $lng", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                  Text(text = "Area: Raina, Purba Bardhaman", fontSize = 11.sp, color = Slate600)
                }
              }

              OutlinedButton(
                onClick = {
                  val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=$lat,$lng(${st.name})"))
                  context.startActivity(mapIntent)
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
              ) {
                Text("View Map", fontSize = 11.sp)
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Battery & Device Info
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = if (isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
                contentDescription = null,
                tint = if (battery > 20) SuccessGreen else ErrorRed,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(text = "$battery% Battery", fontSize = 12.sp, color = Slate700)
            }

            Text(text = "Network: ${tel?.networkType ?: "Wi-Fi"}", fontSize = 12.sp, color = Slate600)
            Text(text = "Model: ${tel?.deviceModel?.ifBlank { "Android Device" } ?: "Android Device"}", fontSize = 11.sp, color = Slate400)
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Installed Games & Distractions
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.SportsEsports, contentDescription = null, tint = BrandGoldDark, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "Gaming & Distraction Scan:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate800)
          }

          Spacer(modifier = Modifier.height(4.dp))
          if (games.isEmpty()) {
            Text(
              text = "✓ Clean: No gaming distraction apps detected during study hours.",
              fontSize = 11.sp,
              color = SuccessGreen,
              fontWeight = FontWeight.Medium
            )
          } else {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              games.forEach { g ->
                Surface(color = ErrorRedLight, shape = RoundedCornerShape(6.dp)) {
                  Text(
                    text = "⚠ $g",
                    fontSize = 11.sp,
                    color = ErrorRed,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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

@Composable
fun AdminAdmissionsTab(
  admissions: List<AdmissionApplication>,
  courses: List<Course>,
  batches: List<Batch>,
  onApproveAdmission: (appId: String, courseId: String?, batchId: String?) -> Unit,
  onRejectAdmission: (appId: String, reason: String) -> Unit,
  onDeleteAdmission: (appId: String) -> Unit = {},
  onOpenNewAdmission: () -> Unit
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(text = "Admission Inquiries (${admissions.size})", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Slate900)
        Button(
          onClick = onOpenNewAdmission,
          colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
          shape = RoundedCornerShape(8.dp)
        ) {
          Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Direct Admission", fontSize = 12.sp)
        }
      }
    }

    if (admissions.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(text = "No pending admission inquiries.", modifier = Modifier.padding(16.dp), fontSize = 13.sp, color = Slate600)
        }
      }
    } else {
      items(admissions) { adm ->
        val targetCourse = courses.firstOrNull { it.id == adm.targetCourseId }
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(text = adm.studentName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Slate900)
              Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = if (adm.status == "approved") SuccessGreenLight else BrandGoldLight
                ) {
                  Text(
                    text = adm.status.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (adm.status == "approved") SuccessGreen else BrandGoldDark,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
                IconButton(
                  onClick = { onDeleteAdmission(adm.id) },
                  modifier = Modifier.size(28.dp)
                ) {
                  Icon(Icons.Default.Delete, contentDescription = "Discard", tint = ErrorRed, modifier = Modifier.size(16.dp))
                }
              }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Mobile: ${adm.mobile} • Guardian: ${adm.guardianName}", fontSize = 12.sp, color = Slate600)
            Text(text = "Target Course: ${targetCourse?.title ?: "Standard"}", fontSize = 12.sp, color = Slate800)
            Text(text = "Aadhaar: ${adm.aadhaarNo} • Address: ${adm.address}", fontSize = 11.sp, color = Slate600)

            if (adm.status == "pending") {
              Spacer(modifier = Modifier.height(10.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
              ) {
                TextButton(onClick = { onRejectAdmission(adm.id, "Seat unavailable or documentation incomplete") }) {
                  Text("Reject", color = ErrorRed, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                  onClick = { onApproveAdmission(adm.id, adm.targetCourseId, null) },
                  colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                  shape = RoundedCornerShape(8.dp)
                ) {
                  Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Approve & Enroll", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun AdminExpensesTab(
  expenses: List<Expense>,
  payments: List<FeePayment>,
  onAddExpense: (Expense) -> Unit,
  onEditExpense: (Expense) -> Unit,
  onDeleteExpense: (String) -> Unit
) {
  var showAddDialog by remember { mutableStateOf(false) }

  val totalFeeCollected = remember(payments) {
    payments.filter { it.status == "approved" }.sumOf { it.finalAmountPaid }
  }
  val totalExpenses = remember(expenses) {
    expenses.sumOf { it.amount }
  }
  val netSurplus = totalFeeCollected - totalExpenses

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(text = "Center Financials & Expenses", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Slate900)
          Text(text = "Rent, Utilities, Educator Payouts", fontSize = 12.sp, color = Slate600)
        }
        Button(
          onClick = { showAddDialog = true },
          colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
          shape = RoundedCornerShape(8.dp)
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Add Expense", fontSize = 12.sp)
        }
      }
    }

    item {
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BrandSecondary),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(text = "Net Cashflow Balance", fontSize = 12.sp, color = BrandGoldLight)
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "₹${netSurplus.toInt()}",
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (netSurplus >= 0) BrandGold else ErrorRed
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Income: ₹${totalFeeCollected.toInt()} • Expenses: ₹${totalExpenses.toInt()}",
            fontSize = 12.sp,
            color = Slate300
          )
        }
      }
    }

    if (expenses.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(text = "No center expenses recorded yet.", modifier = Modifier.padding(16.dp), fontSize = 13.sp, color = Slate600)
        }
      }
    } else {
      items(expenses.reversed()) { exp ->
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(text = exp.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Slate900)
              Text(text = "Category: ${exp.category} • Mode: ${exp.paymentMode}", fontSize = 12.sp, color = Slate600)
              Text(text = "Date: ${exp.expenseDate.take(10)} • Approved: ${exp.approvedBy}", fontSize = 11.sp, color = Slate400)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(text = "₹${exp.amount.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ErrorRed)
              Spacer(modifier = Modifier.width(6.dp))
              IconButton(onClick = { onEditExpense(exp) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Slate700, modifier = Modifier.size(16.dp))
              }
              IconButton(onClick = { onDeleteExpense(exp.id) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed, modifier = Modifier.size(16.dp))
              }
            }
          }
        }
      }
    }
  }

  if (showAddDialog) {
    AddExpenseDialog(
      onDismiss = { showAddDialog = false },
      onSave = { exp ->
        onAddExpense(exp)
        showAddDialog = false
      }
    )
  }
}

@Composable
fun AddExpenseDialog(
  onDismiss: () -> Unit,
  onSave: (Expense) -> Unit
) {
  var title by remember { mutableStateOf("") }
  var category by remember { mutableStateOf("Utilities") }
  var amountStr by remember { mutableStateOf("") }
  var paymentMode by remember { mutableStateOf("CASH") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Record Center Expense", fontWeight = FontWeight.Bold) },
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
          label = { Text("Expense Title") },
          placeholder = { Text("e.g. Center Electricity / Broadband / Books") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = amountStr,
          onValueChange = { amountStr = it },
          label = { Text("Amount (₹)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        Text(text = "Category:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf("Rent", "Power", "Internet", "Faculty", "Misc").forEach { cat ->
            FilterChip(
              selected = category == cat,
              onClick = { category = cat },
              label = { Text(cat, fontSize = 11.sp) }
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val amt = amountStr.toDoubleOrNull() ?: 0.0
          if (title.isNotBlank() && amt > 0) {
            val exp = Expense(
              id = "exp-${System.currentTimeMillis()}",
              title = title,
              category = category,
              amount = amt,
              expenseDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
              paymentMode = paymentMode,
              approvedBy = "Admin"
            )
            onSave(exp)
          }
        }
      ) {
        Text("Save Expense")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}

@Composable
fun NewAdmissionDialog(
  courses: List<Course>,
  batches: List<Batch>,
  existingStudentsCount: Int,
  onDismiss: () -> Unit,
  onEnroll: (student: Student, initialPaymentAmount: Double?, paymentMode: String?) -> Unit
) {
  var name by remember { mutableStateOf("") }
  var mobile by remember { mutableStateOf("") }
  var aadhaar by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var guardianName by remember { mutableStateOf("") }
  var guardianPhone by remember { mutableStateOf("") }
  var address by remember { mutableStateOf("Raina, Purba Bardhaman") }
  var selectedCourseId by remember { mutableStateOf(courses.firstOrNull()?.id ?: "") }
  var selectedBatchId by remember { mutableStateOf(batches.firstOrNull()?.id ?: "") }
  var initialAmountStr by remember { mutableStateOf("400") }
  var initialMode by remember { mutableStateOf("CASH") }
  var errorText by remember { mutableStateOf<String?>(null) }

  val courseBatches = remember(selectedCourseId, batches) {
    batches.filter { it.courseId == selectedCourseId }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Direct Admission & Enrollment", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Full Name *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = mobile,
            onValueChange = { mobile = it },
            label = { Text("Mobile *") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = aadhaar,
            onValueChange = { aadhaar = it },
            label = { Text("Aadhaar No *") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = guardianName,
            onValueChange = { guardianName = it },
            label = { Text("Guardian Name *") },
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
          label = { Text("Residential Address") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        Text(text = "Target Academic Course:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        courses.forEach { c ->
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .fillMaxWidth()
              .clickable { selectedCourseId = c.id }
          ) {
            RadioButton(selected = selectedCourseId == c.id, onClick = { selectedCourseId = c.id })
            Text(text = "${c.title} (₹${c.monthlyFee.toInt()}/mo)", fontSize = 12.sp)
          }
        }

        if (courseBatches.isNotEmpty()) {
          Text(text = "Batch Schedule:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
          courseBatches.forEach { b ->
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .fillMaxWidth()
                .clickable { selectedBatchId = b.id }
            ) {
              RadioButton(selected = selectedBatchId == b.id, onClick = { selectedBatchId = b.id })
              Text(text = "${b.name} (${b.timings})", fontSize = 12.sp)
            }
          }
        }

        Text(text = "Initial Admission Fee Payment:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = initialAmountStr,
            onValueChange = { initialAmountStr = it },
            label = { Text("Amount (₹)") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            FilterChip(selected = initialMode == "CASH", onClick = { initialMode = "CASH" }, label = { Text("CASH") })
            Spacer(modifier = Modifier.width(4.dp))
            FilterChip(selected = initialMode == "UPI", onClick = { initialMode = "UPI" }, label = { Text("UPI") })
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
          if (name.isBlank() || mobile.isBlank() || aadhaar.isBlank() || guardianName.isBlank()) {
            errorText = "Please fill all required fields marked with *"
            return@Button
          }

          val nextRoll = "PP-2026-${(100 + existingStudentsCount + 1)}"
          val studentId = "stu-${System.currentTimeMillis()}"
          val initAmt = initialAmountStr.toDoubleOrNull()

          val student = Student(
            id = studentId,
            rollNo = nextRoll,
            name = name,
            mobile = mobile,
            aadhaarNo = aadhaar,
            email = email.ifBlank { null },
            guardianName = guardianName,
            guardianPhone = guardianPhone.ifBlank { mobile },
            address = address,
            courseId = selectedCourseId,
            batchId = selectedBatchId.ifBlank { "batch-1" },
            admissionDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date()),
            admissionMonth = FeeCalculator.getCurrentYearMonth(),
            status = "active"
          )

          onEnroll(student, initAmt, initialMode)
        }
      ) {
        Text("Enroll Student")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}

@Composable
fun AdminOperationsTab(
  students: List<Student>,
  telemetries: List<StudentTelemetry>,
  courses: List<Course>,
  batches: List<Batch>,
  expenses: List<Expense>,
  payments: List<FeePayment>,
  onOpenNewAdmission: () -> Unit,
  onEditStudent: (Student) -> Unit,
  onDeleteStudent: (String) -> Unit,
  onAddExpense: (Expense) -> Unit,
  onEditExpense: (Expense) -> Unit,
  onDeleteExpense: (String) -> Unit
) {
  var activeView by remember { mutableIntStateOf(0) } // 0: Directory, 1: Live Tracker & Games, 2: Financials & Expenses

  Column(modifier = Modifier.fillMaxSize()) {
    Surface(
      color = Color.White,
      tonalElevation = 2.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        FilterChip(
          selected = activeView == 0,
          onClick = { activeView = 0 },
          leadingIcon = { Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(14.dp)) },
          label = { Text("Directory (${students.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = BrandPrimary,
            selectedLabelColor = Color.White
          ),
          modifier = Modifier.weight(1f)
        )
        FilterChip(
          selected = activeView == 1,
          onClick = { activeView = 1 },
          leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp)) },
          label = { Text("Tracker & Games", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = BrandPrimary,
            selectedLabelColor = Color.White
          ),
          modifier = Modifier.weight(1f)
        )
        FilterChip(
          selected = activeView == 2,
          onClick = { activeView = 2 },
          leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(14.dp)) },
          label = { Text("Expenses (${expenses.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = BrandPrimary,
            selectedLabelColor = Color.White
          ),
          modifier = Modifier.weight(1f)
        )
      }
    }

    when (activeView) {
      0 -> AdminStudentsTab(
        students = students,
        courses = courses,
        batches = batches,
        onOpenNewAdmission = onOpenNewAdmission,
        onEditStudent = onEditStudent,
        onDeleteStudent = onDeleteStudent
      )
      1 -> AdminTelemetryTrackerTab(
        students = students,
        telemetries = telemetries
      )
      2 -> AdminExpensesTab(
        expenses = expenses,
        payments = payments,
        onAddExpense = onAddExpense,
        onEditExpense = onEditExpense,
        onDeleteExpense = onDeleteExpense
      )
    }
  }
}

@Composable
fun CreateDiscountDialog(
  students: List<Student>,
  courses: List<Course>,
  batches: List<Batch>,
  onDismiss: () -> Unit,
  onSaveDiscount: (Discount) -> Unit
) {
  var title by remember { mutableStateOf("") }
  var type by remember { mutableStateOf("flat") }
  var valueStr by remember { mutableStateOf("") }
  var scope by remember { mutableStateOf("all") }
  var selectedCourseId by remember { mutableStateOf<String?>(null) }
  var selectedBatchId by remember { mutableStateOf<String?>(null) }
  var selectedStudentId by remember { mutableStateOf<String?>(null) }
  var reason by remember { mutableStateOf("") }
  var errorText by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Create Discount / Concession Rule", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
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
          placeholder = { Text("e.g. Starter Discount / Merit Concession") },
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

        Text("Discount Scope:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
          label = { Text("Reason / Description") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        if (errorText != null) {
          Text(errorText!!, color = ErrorRed, fontSize = 12.sp)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val v = valueStr.toDoubleOrNull()
          if (title.isBlank() || v == null || v <= 0) {
            errorText = "Please enter valid title and discount value."
            return@Button
          }
          val discount = Discount(
            id = "disc-${System.currentTimeMillis()}",
            title = title,
            type = type,
            value = v,
            scope = scope,
            courseId = if (scope == "course") selectedCourseId else null,
            batchId = if (scope == "batch") selectedBatchId else null,
            studentId = if (scope == "student") selectedStudentId else null,
            applicableMonthsType = "all_months",
            reason = reason.ifBlank { "Administrative concession" },
            active = true
          )
          onSaveDiscount(discount)
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
