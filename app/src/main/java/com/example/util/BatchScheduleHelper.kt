package com.example.util

import com.example.data.model.Batch
import com.example.data.model.Course
import com.example.data.model.LiveClassSession
import com.example.data.model.Student
import java.text.SimpleDateFormat
import java.util.*

object BatchScheduleHelper {

  private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
  private val DISPLAY_DATE_FORMAT = SimpleDateFormat("EEE, dd MMM yyyy", Locale.US)
  private val DAY_OF_WEEK_FORMAT = SimpleDateFormat("EEE", Locale.US)

  private val ALL_TOKENS = setOf(
    "all", "all batches", "all_batches", "all-batches", "all batch",
    "all courses", "all_courses", "all-courses", "all course",
    "all classes", "all_classes", "all-classes", "all class",
    "all subjects", "all_subjects", "all-subjects", "all subject",
    "any", "everyone", "-"
  )

  fun isAllToken(value: String?): Boolean {
    val trimmed = value?.trim()?.lowercase(Locale.US) ?: ""
    return trimmed.isEmpty() ||
      trimmed in ALL_TOKENS ||
      trimmed.startsWith("all ") ||
      trimmed.startsWith("all_") ||
      trimmed.startsWith("all-")
  }

  private fun normalizeClassGrade(raw: String?): String? {
    val clean = raw?.trim()?.lowercase(Locale.US) ?: return null
    if (clean.isEmpty() || isAllToken(clean)) return null
    val match = Regex("""\b(xii|xi|ix|viii|vii|vi|iv|x|v|12th|11th|10th|9th|8th|7th|6th|5th|4th|12|11|10|9|8|7|6|5|4)\b""").find(clean)
    if (match != null) {
      return when (match.value) {
        "xii", "12", "12th" -> "12"
        "xi", "11", "11th" -> "11"
        "x", "10", "10th" -> "10"
        "ix", "9", "9th" -> "9"
        "viii", "8", "8th" -> "8"
        "vii", "7", "7th" -> "7"
        "vi", "6", "6th" -> "6"
        "v", "5", "5th" -> "5"
        "iv", "4", "4th" -> "4"
        else -> match.value
      }
    }
    if (clean.contains("higher secondary") || clean == "hs") return "11-12"
    if (clean.contains("secondary") || clean == "madhyamik") return "10"
    return clean
  }

  fun matchesAcademicClass(
    targetClassRaw: String?,
    student: Student,
    studentCourse: Course?,
    studentBatch: Batch?
  ): Boolean {
    if (isAllToken(targetClassRaw)) return true
    val targetTokens = (targetClassRaw ?: "").split(",", ";").map { it.trim() }.filter { it.isNotEmpty() }
    if (targetTokens.isEmpty()) return true

    val studentClassSources = listOfNotNull(
      student.studentClass?.takeIf { it.isNotBlank() },
      studentCourse?.academicClass?.takeIf { it.isNotBlank() },
      studentCourse?.title?.takeIf { it.isNotBlank() },
      studentBatch?.name?.takeIf { it.isNotBlank() }
    )
    if (studentClassSources.isEmpty()) return false

    val studentGrades = studentClassSources.mapNotNull { normalizeClassGrade(it) }.toSet()

    return targetTokens.any { token ->
      val targetLower = token.lowercase(Locale.US)
      val targetGrade = normalizeClassGrade(token)

      // 1. Direct string equality or containment
      val directMatch = studentClassSources.any { src ->
        val srcLower = src.trim().lowercase(Locale.US)
        srcLower == targetLower ||
          (srcLower.length >= 3 && targetLower.length >= 3 && (srcLower.contains(targetLower) || targetLower.contains(srcLower)))
      }
      if (directMatch) return@any true

      // 2. Normalized grade match (e.g., "Class 11" <-> "XI" <-> "Class 11 (Science)")
      if (targetGrade != null && studentGrades.isNotEmpty()) {
        if (targetGrade in studentGrades) return@any true
        if (targetGrade == "11-12" && ("11" in studentGrades || "12" in studentGrades)) return@any true
      }
      false
    }
  }

  private val SUBJECT_STOP_WORDS = setOf(
    "class", "batch", "batches", "course", "subject", "subjects", "general", "all",
    "xi", "xii", "x", "ix", "10", "11", "12", "10th", "11th", "12th",
    "science", "arts", "commerce", "secondary", "higher", "board",
    "and", "for", "of", "the", "with", "in", "to", "a", "an"
  )

  private fun expandSubjectTerms(raw: String): Set<String> {
    val lower = raw.trim().lowercase(Locale.US)
    val terms = mutableSetOf(lower)
    if (Regex("""\bcs\b""").containsMatchIn(lower) || lower.contains("computer science") || lower.contains("comp sci") || lower.contains("computer application")) {
      terms.add("computer science")
      terms.add("computer")
      terms.add("cs")
    }
    if (Regex("""\bmath(s)?\b""").containsMatchIn(lower) || lower.contains("mathematics")) {
      terms.add("mathematics")
      terms.add("math")
      terms.add("maths")
    }
    if (lower.contains("physic")) {
      terms.add("physics")
      terms.add("phy")
    }
    if (lower.contains("chem")) {
      terms.add("chemistry")
      terms.add("chem")
    }
    if (lower.contains("bio") || lower.contains("life science")) {
      terms.add("biology")
      terms.add("life science")
    }
    if (lower.contains("english") || Regex("""\beng\b""").containsMatchIn(lower)) {
      terms.add("english")
    }
    if (lower.contains("bengali") || Regex("""\bbeng\b""").containsMatchIn(lower)) {
      terms.add("bengali")
    }
    val words = lower.split(Regex("""[^a-z0-9]+""")).filter { it.length >= 2 && it !in SUBJECT_STOP_WORDS }
    terms.addAll(words)
    return terms
  }

  fun matchesSubject(
    targetSubjectRaw: String?,
    course: Course?,
    batch: Batch?
  ): Boolean {
    val target = targetSubjectRaw?.trim() ?: ""
    if (isAllToken(target) || target.equals("general", ignoreCase = true)) return true
    if (course == null && batch == null) return false

    val courseSources = listOfNotNull(
      course?.title?.takeIf { it.isNotBlank() },
      course?.category?.takeIf { it.isNotBlank() && !it.equals("general", ignoreCase = true) },
      course?.code?.takeIf { it.isNotBlank() },
      batch?.name?.takeIf { it.isNotBlank() }
    )
    if (courseSources.isEmpty()) return false

    val targetTokens = target.split(",", ";", "&", "/").map { it.trim() }.filter { it.isNotEmpty() }
    val expandedCourseTerms = courseSources.flatMap { expandSubjectTerms(it) }.toSet()

    return targetTokens.any { token ->
      val tLower = token.lowercase(Locale.US)
      val directMatch = courseSources.any { src ->
        val sLower = src.trim().lowercase(Locale.US)
        sLower == tLower || (tLower.length >= 3 && sLower.contains(tLower)) || (sLower.length >= 3 && tLower.contains(sLower))
      }
      if (directMatch) return@any true

      val expandedTarget = expandSubjectTerms(token)
      expandedTarget.any { it in expandedCourseTerms }
    }
  }

  /**
   * Determines whether a scheduled class session applies to an enrolled student.
   * Supports:
   * 1) Specific batch (by batch ID or name, or comma-separated list)
   * 2) All batches of a specific course/subject
   * 3) All batches of an academic class (e.g., Class 11 / XI)
   * 4) All batches of a subject across classes (e.g., Computer Science / Mathematics)
   * 5) Center-wide sessions for all students
   */
  fun isClassSessionForStudent(
    session: LiveClassSession,
    student: Student,
    courses: List<Course>,
    batches: List<Batch>
  ): Boolean {
    val studentCourse = courses.firstOrNull { it.id.equals(student.courseId, ignoreCase = true) }
    val studentBatch = batches.firstOrNull { it.id.equals(student.batchId, ignoreCase = true) }

    val sessBatch = session.batchId?.trim() ?: ""
    val sessCourse = session.courseId?.trim() ?: ""
    val sessClass = session.academicClass?.trim() ?: ""
    val sessSubject = session.subject.trim()

    // 1. If session specifies one or more concrete batches (not "all"/blank)
    if (!isAllToken(sessBatch)) {
      val batchTokens = sessBatch.split(",", ";").map { it.trim() }.filter { it.isNotEmpty() }
      return batchTokens.any { token ->
        token.equals(student.batchId.trim(), ignoreCase = true) ||
          (studentBatch != null && token.equals(studentBatch.name.trim(), ignoreCase = true))
      }
    }

    // 2. Session is scheduled for ALL batches (batchId is blank or "all"/"all batches"/etc.)
    val hasSpecificCourse = !isAllToken(sessCourse)
    val hasSpecificClass = !isAllToken(sessClass)
    val hasSpecificSubject = !isAllToken(sessSubject) && !sessSubject.equals("general", ignoreCase = true)

    // 2a. Specific Course ID(s) / Course Name provided
    if (hasSpecificCourse) {
      val courseTokens = sessCourse.split(",", ";").map { it.trim() }.filter { it.isNotEmpty() }
      val matchesCourse = courseTokens.any { token ->
        token.equals(student.courseId.trim(), ignoreCase = true) ||
          (studentCourse != null && (
            token.equals(studentCourse.title.trim(), ignoreCase = true) ||
              token.equals(studentCourse.code.trim(), ignoreCase = true)
            ))
      }
      if (matchesCourse) return true
      // If session explicitly specifies another courseId, don't bleed into unrelated courses
      // unless no class/subject distinction applies? No, specific courseId excludes other courses.
      return false
    }

    // 2b. courseId is "all" or blank: filter by academicClass and/or subject
    if (!hasSpecificClass && !hasSpecificSubject) {
      // Center-wide session for all batches of all classes & subjects
      return true
    }

    if (hasSpecificClass && !hasSpecificSubject) {
      // All batches of a specific academic class
      return matchesAcademicClass(sessClass, student, studentCourse, studentBatch)
    }

    if (!hasSpecificClass && hasSpecificSubject) {
      // All batches of a specific subject across classes
      return matchesSubject(sessSubject, studentCourse, studentBatch)
    }

    // 2c. Both academicClass AND subject are present (with batchId="all" and courseId="all"/blank)
    val classMatches = matchesAcademicClass(sessClass, student, studentCourse, studentBatch)
    if (!classMatches) return false

    if (matchesSubject(sessSubject, studentCourse, studentBatch)) return true

    // If session subject clearly identifies another distinct course in the center, exclude non-matching student;
    // otherwise treat session.subject as a topic/chapter label for the matched academic class.
    val matchesAnotherCourse = courses.any { c ->
      !c.id.equals(student.courseId, ignoreCase = true) && matchesSubject(sessSubject, c, null)
    }
    return !matchesAnotherCourse
  }

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
    filterBatchId: String? = null,
    filterCourseId: String? = null
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
      if (!autoIds.contains(custom.id)) {
        val matchesBatch = when {
          filterBatchId.isNullOrBlank() -> true
          custom.batchId == filterBatchId -> true
          isAllToken(custom.batchId) -> {
            filterCourseId.isNullOrBlank() || isAllToken(custom.courseId) || custom.courseId == filterCourseId
          }
          else -> false
        }
        if (matchesBatch) {
          list.add(custom)
        }
      }
    }

    return list.sortedBy { it.scheduledDate }
  }
}
