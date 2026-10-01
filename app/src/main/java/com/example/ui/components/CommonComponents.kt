package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.FeePayment
import com.example.ui.theme.*

@Composable
fun CenterLogo(
  modifier: Modifier = Modifier,
  size: Int = 48,
  logoUrl: String? = null
) {
  Box(
    modifier = modifier
      .size(size.dp)
      .clip(CircleShape)
      .background(Color.Black)
      .border(1.5.dp, Color(0xFFD4AF37), CircleShape),
    contentAlignment = Alignment.Center
  ) {
    androidx.compose.foundation.Image(
      painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_pixel_pathsala_logo),
      contentDescription = "Pixel Pathsala Logo",
      contentScale = androidx.compose.ui.layout.ContentScale.Fit,
      modifier = Modifier.fillMaxSize().padding(1.dp)
    )
  }
}

@Composable
fun StatusBadge(
  status: String,
  modifier: Modifier = Modifier
) {
  val clean = status.trim().lowercase()
  val (bgColor, textColor, label, icon) = when {
    clean == "approved" || clean == "active" || clean == "paid" -> {
      Quadruple(EmeraldLight, EmeraldSuccess, if (clean == "paid") "PAID" else "ACTIVE", Icons.Default.CheckCircle)
    }
    clean == "pending" || clean == "scheduled" -> {
      Quadruple(AmberLight, Color(0xFFB45309), if (clean == "scheduled") "SCHEDULED" else "PENDING", Icons.Default.Schedule)
    }
    clean == "live" -> {
      Quadruple(RoseLight, RoseLive, "LIVE NOW", Icons.Default.Radio)
    }
    clean == "rejected" || clean == "dropped" || clean == "due" -> {
      Quadruple(Color(0xFFFEF2F2), Color(0xFFDC2626), clean.uppercase(), Icons.Default.Cancel)
    }
    else -> {
      Quadruple(Color(0xFFF1F5F9), Color(0xFF475569), status.uppercase(), Icons.Default.Info)
    }
  }

  Surface(
    color = bgColor,
    shape = RoundedCornerShape(12.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, textColor.copy(alpha = 0.3f)),
    modifier = modifier
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = textColor,
        modifier = Modifier.size(12.dp)
      )
      Text(
        text = label,
        color = textColor,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp
      )
    }
  }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun MetricCard(
  title: String,
  value: String,
  subtitle: String? = null,
  icon: ImageVector,
  iconTint: Color = IndigoPrimary,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = title.uppercase(),
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = SlateTextSecondary,
          letterSpacing = 0.5.sp
        )
        Box(
          modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(iconTint.copy(alpha = 0.12f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(18.dp)
          )
        }
      }

      Text(
        text = value,
        fontSize = 24.sp,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.onSurface,
        fontFamily = FontFamily.Monospace
      )

      if (!subtitle.isNullOrBlank()) {
        Text(
          text = subtitle,
          fontSize = 11.sp,
          color = SlateTextSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}

@Composable
fun SectionHeader(
  title: String,
  subtitle: String? = null,
  actionText: String? = null,
  onActionClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 18.sp,
        color = MaterialTheme.colorScheme.onBackground
      )
      if (!subtitle.isNullOrBlank()) {
        Text(
          text = subtitle,
          fontSize = 12.sp,
          color = SlateTextSecondary,
          modifier = Modifier.padding(top = 2.dp)
        )
      }
    }
    if (actionText != null && onActionClick != null) {
      TextButton(
        onClick = onActionClick,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
      ) {
        Text(
          text = actionText,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = IndigoPrimary
        )
      }
    }
  }
}

@Composable
fun ReceiptDialog(
  payment: FeePayment,
  centerName: String = "Pixel Pathsala",
  onDismiss: () -> Unit,
  onDelete: (() -> Unit)? = null
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = Color.White),
      border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 16.dp)
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            CenterLogo(size = 36)
            Column {
              Text(
                text = centerName,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                color = Color(0xFF0F172A)
              )
              Text(
                text = "Official Fee Receipt",
                fontSize = 11.sp,
                color = IndigoPrimary,
                fontWeight = FontWeight.Bold
              )
            }
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close")
          }
        }

        HorizontalDivider(color = SlateBorder)

        // Receipt details
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF8FAFC))
            .padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          ReceiptRow("Receipt No", payment.receiptNo)
          ReceiptRow("Date", payment.paymentDate)
          ReceiptRow("Student Name", payment.studentName)
          ReceiptRow("Enrolled Course", payment.courseTitle)
          ReceiptRow("Months Covered", payment.month)
          ReceiptRow("Payment Mode", "${payment.paymentMode} ${payment.transactionRef ?: ""}")
          ReceiptRow("Status", payment.status.uppercase())
        }

        // Amount Box
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFEEF2FF))
            .padding(14.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "Amount Paid",
              fontSize = 11.sp,
              color = IndigoPrimary,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "₹${payment.finalAmountPaid.toInt()}",
              fontSize = 28.sp,
              fontWeight = FontWeight.Black,
              color = Color(0xFF1E1B4B),
              fontFamily = FontFamily.Monospace
            )
          }
        }

        // Action Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = {
              val shareText = """
                *PIXEL PATHSALA FEE RECEIPT*
                Receipt No: ${payment.receiptNo}
                Date: ${payment.paymentDate}
                Student: ${payment.studentName}
                Course: ${payment.courseTitle}
                Month: ${payment.month}
                Amount Paid: ₹${payment.finalAmountPaid}
                Mode: ${payment.paymentMode}
                Status: VERIFIED & APPROVED
              """.trimIndent()
              clipboardManager.setText(AnnotatedString(shareText))
              Toast.makeText(context, "Receipt text copied to clipboard!", Toast.LENGTH_SHORT).show()
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Copy Info", fontSize = 12.sp)
          }

          Button(
            onClick = {
              val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(
                  Intent.EXTRA_TEXT,
                  "Fee Receipt from $centerName\nStudent: ${payment.studentName}\nReceipt No: ${payment.receiptNo}\nAmount: ₹${payment.finalAmountPaid}\nMonth: ${payment.month}"
                )
                type = "text/plain"
              }
              context.startActivity(Intent.createChooser(shareIntent, "Share Receipt"))
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Share", fontSize = 12.sp)
          }
        }

        if (onDelete != null) {
          OutlinedButton(
            onClick = onDelete,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Receipt", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Delete Receipt", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
fun ReceiptRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(text = label, fontSize = 11.sp, color = SlateTextSecondary)
    Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SlateTextPrimary)
  }
}
