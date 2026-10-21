package com.example.util

import com.example.data.model.Batch
import com.example.data.model.Course
import com.example.data.model.LiveClassSession
import java.text.SimpleDateFormat
import java.util.*

object BatchScheduleHelper {

  private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
  private val DISPLAY_DATE_FORMAT = SimpleDateFormat("EEE, dd MMM yyyy", Locale.US)
  private val DAY_OF_WEEK_FORMAT = SimpleDateFormat("EEE", Locale.US)

  /**
   * Generates automated upcoming class sessions for a batch based on its scheduleDays and timings.
   */
  fun getAutomatedBatchSchedule(
    batch: Batch,
    course: Course?,
    existingSessions: List<LiveClassSession>,
    daysAhead: Int = 21
  ): List<LiveClassSession> {
    val results = mutableListOf<LiveClassSession>()
    val cal = Calendar.getInstance()

    // Parse batch timing
    val timeParts = batch.timings.split("-")
    val startTime = timeParts.firstOrNull()?.trim() ?: "08:00 AM"
    val endTime = timeParts.getOrNull(1)?.trim() ?: "10:00 AM"

    // Parse target days of week (e.g. "Sat, Sun", "Mon, Wed, Fri", "Daily")
    val rawDays = batch.scheduleDays.lowercase(Locale.US)
    val matchesAny = rawDays.contains("daily") || rawDays.contains("all")

    val targetDayIndices = mutableSetOf<Int>()
    if (matchesAny) {
      targetDayIndices.addAll(listOf(Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY))
    } else {
      if (rawDays.contains("sun")) targetDayIndices.add(Calendar.SUNDAY)
      if (rawDays.contains("mon")) targetDayIndices.add(Calendar.MONDAY)
      if (rawDays.contains("tue")) targetDayIndices.add(Calendar.TUESDAY)
      if (rawDays.contains("wed")) targetDayIndices.add(Calendar.WEDNESDAY)
      if (rawDays.contains("thu")) targetDayIndices.add(Calendar.THURSDAY)
      if (rawDays.contains("fri")) targetDayIndices.add(Calendar.FRIDAY)
      if (rawDays.contains("sat")) targetDayIndices.add(Calendar.SATURDAY)
    }

    if (targetDayIndices.isEmpty()) {
      // Default to weekends if unparseable
      targetDayIndices.add(Calendar.SATURDAY)
      targetDayIndices.add(Calendar.SUNDAY)
    }

    // Iterate next daysAhead days
    for (i in 0 until daysAhead) {
      val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
      if (targetDayIndices.contains(dayOfWeek)) {
        val dateIso = DATE_FORMAT.format(cal.time)
        val dateDisplay = DISPLAY_DATE_FORMAT.format(cal.time)

        // Check if an existing stored session matches this batch and date
        val existing = existingSessions.firstOrNull {
          it.batchId == batch.id && (it.scheduledDate == dateIso || it.scheduledDate == dateDisplay)
        }

        if (existing != null) {
          results.add(existing)
        } else {
          val autoSession = LiveClassSession(
            id = "auto-${batch.id}-$dateIso",
            title = "${course?.title ?: "Class"} • ${batch.name}",
            subject = course?.title ?: "Computer Training",
            academicClass = course?.academicClass,
            courseId = batch.courseId,
            batchId = batch.id,
            educatorName = "Faculty / Mentor",
            scheduledDate = dateDisplay,
            startTime = startTime,
            endTime = endTime,
            status = "scheduled",
            platform = if (batch.mode.equals("online", true)) "Google Meet" else "Offline Lab (Pixel Pathsala)",
            meetingUrl = if (batch.mode.equals("online", true)) "https://meet.google.com/new" else null,
            isPostponed = false,
            postponeReason = null,
            notificationSent = false
          )
          results.add(autoSession)
        }
      }
      cal.add(Calendar.DAY_OF_YEAR, 1)
    }

    return results
  }

  /**
   * Generates automated upcoming class schedule across all batches or for enrolled batch.
   */
  fun getAllBatchesAutomatedSchedule(
    batches: List<Batch>,
    courses: List<Course>,
    existingSessions: List<LiveClassSession>,
    filterBatchId: String? = null
  ): List<LiveClassSession> {
    val targetBatches = if (!filterBatchId.isNullOrBlank()) {
      batches.filter { it.id == filterBatchId }
    } else {
      batches
    }

    val list = mutableListOf<LiveClassSession>()
    for (b in targetBatches) {
      val c = courses.firstOrNull { it.id == b.courseId }
      list.addAll(getAutomatedBatchSchedule(b, c, existingSessions))
    }

    // Also include any custom one-off live classes from existingSessions
    val autoIds = list.map { it.id }.toSet()
    existingSessions.forEach { custom ->
      if (!autoIds.contains(custom.id) && (filterBatchId.isNullOrBlank() || custom.batchId == filterBatchId)) {
        list.add(custom)
      }
    }

    return list.sortedBy { it.scheduledDate }
  }
}
