package com.example

import com.example.data.model.Batch
import com.example.data.model.Course
import com.example.data.model.LiveClassSession
import com.example.data.model.Student
import com.example.util.BatchScheduleHelper
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatchScheduleHelperTest {

  private val courseCs11 = Course(
    id = "course-cs-11",
    title = "COMPUTER SCIENCE (XI)",
    academicClass = "Class 11",
    monthlyFee = 400.0
  )

  private val courseCs12 = Course(
    id = "course-cs-12",
    title = "COMPUTER SCIENCE (XII)",
    academicClass = "Class 12",
    monthlyFee = 500.0
  )

  private val courseMath11 = Course(
    id = "course-math-11",
    title = "MATHEMATICS (XI)",
    academicClass = "Class 11",
    monthlyFee = 450.0
  )

  private val batchCs11A = Batch(
    id = "batch-cs11-a",
    courseId = courseCs11.id,
    name = "CS XI Morning"
  )

  private val batchCs11B = Batch(
    id = "batch-cs11-b",
    courseId = courseCs11.id,
    name = "CS XI Evening"
  )

  private val batchCs12A = Batch(
    id = "batch-cs12-a",
    courseId = courseCs12.id,
    name = "CS XII Morning"
  )

  private val batchMath11A = Batch(
    id = "batch-math11-a",
    courseId = courseMath11.id,
    name = "Math XI Batch"
  )

  private val studentCs11A = Student(
    id = "stu-cs11-a",
    rollNo = "PP-001",
    name = "Arjun Das",
    mobile = "9876543210",
    aadhaarNo = "111122223333",
    courseId = courseCs11.id,
    batchId = batchCs11A.id
  )

  private val studentCs11B = Student(
    id = "stu-cs11-b",
    rollNo = "PP-002",
    name = "Riya Sen",
    mobile = "9876543211",
    aadhaarNo = "222233334444",
    courseId = courseCs11.id,
    batchId = batchCs11B.id
  )

  private val studentCs12A = Student(
    id = "stu-cs12-a",
    rollNo = "PP-003",
    name = "Sourav Roy",
    mobile = "9876543212",
    aadhaarNo = "333344445555",
    courseId = courseCs12.id,
    batchId = batchCs12A.id
  )

  private val studentMath11A = Student(
    id = "stu-math11-a",
    rollNo = "PP-004",
    name = "Neha Pal",
    mobile = "9876543213",
    aadhaarNo = "444455556666",
    courseId = courseMath11.id,
    batchId = batchMath11A.id
  )

  private val courses = listOf(courseCs11, courseCs12, courseMath11)
  private val batches = listOf(batchCs11A, batchCs11B, batchCs12A, batchMath11A)

  @Test
  fun testSpecificBatchSessionOnlyMatchesThatBatch() {
    val session = LiveClassSession(
      id = "cls-1",
      title = "SQL Practice",
      subject = "Computer Science",
      academicClass = "Class 11",
      courseId = courseCs11.id,
      batchId = batchCs11A.id
    )

    assertTrue(BatchScheduleHelper.isClassSessionForStudent(session, studentCs11A, courses, batches))
    assertFalse(BatchScheduleHelper.isClassSessionForStudent(session, studentCs11B, courses, batches))
    assertFalse(BatchScheduleHelper.isClassSessionForStudent(session, studentCs12A, courses, batches))
    assertFalse(BatchScheduleHelper.isClassSessionForStudent(session, studentMath11A, courses, batches))
  }

  @Test
  fun testAllBatchesOfAClassMatchesAllBatchesInClassAndNormalizesGrade() {
    // Scheduled for all batches of Class 11 (using Roman numeral XI and courseId="all")
    val sessionClassXI = LiveClassSession(
      id = "cls-2",
      title = "Class XI Exam Orientation",
      subject = "All",
      academicClass = "XI",
      courseId = "all",
      batchId = "all"
    )

    assertTrue(BatchScheduleHelper.isClassSessionForStudent(sessionClassXI, studentCs11A, courses, batches))
    assertTrue(BatchScheduleHelper.isClassSessionForStudent(sessionClassXI, studentCs11B, courses, batches))
    assertTrue(BatchScheduleHelper.isClassSessionForStudent(sessionClassXI, studentMath11A, courses, batches))
    assertFalse(BatchScheduleHelper.isClassSessionForStudent(sessionClassXI, studentCs12A, courses, batches))
  }

  @Test
  fun testAllBatchesOfASubjectMatchesAcrossClasses() {
    // Scheduled for all batches of Computer Science across Class 11 and Class 12
    val sessionCsSubject = LiveClassSession(
      id = "cls-3",
      title = "Python Coding Workshop",
      subject = "Computer Science",
      academicClass = "all",
      courseId = "all",
      batchId = "all batches"
    )

    assertTrue(BatchScheduleHelper.isClassSessionForStudent(sessionCsSubject, studentCs11A, courses, batches))
    assertTrue(BatchScheduleHelper.isClassSessionForStudent(sessionCsSubject, studentCs11B, courses, batches))
    assertTrue(BatchScheduleHelper.isClassSessionForStudent(sessionCsSubject, studentCs12A, courses, batches))
    assertFalse(BatchScheduleHelper.isClassSessionForStudent(sessionCsSubject, studentMath11A, courses, batches))
  }

  @Test
  fun testAllBatchesOfASpecificCourseMatchesCourseBatchesOnly() {
    val sessionCourse11 = LiveClassSession(
      id = "cls-4",
      title = "Class 11 CS Combined",
      subject = "Computer Science",
      academicClass = "all",
      courseId = courseCs11.id,
      batchId = "all"
    )

    assertTrue(BatchScheduleHelper.isClassSessionForStudent(sessionCourse11, studentCs11A, courses, batches))
    assertTrue(BatchScheduleHelper.isClassSessionForStudent(sessionCourse11, studentCs11B, courses, batches))
    assertFalse(BatchScheduleHelper.isClassSessionForStudent(sessionCourse11, studentCs12A, courses, batches))
    assertFalse(BatchScheduleHelper.isClassSessionForStudent(sessionCourse11, studentMath11A, courses, batches))
  }

  @Test
  fun testCenterWideAllStudentsSessionMatchesEveryone() {
    val sessionAll = LiveClassSession(
      id = "cls-5",
      title = "Annual Seminar",
      subject = "",
      academicClass = "",
      courseId = "all",
      batchId = "all"
    )

    assertTrue(BatchScheduleHelper.isClassSessionForStudent(sessionAll, studentCs11A, courses, batches))
    assertTrue(BatchScheduleHelper.isClassSessionForStudent(sessionAll, studentCs11B, courses, batches))
    assertTrue(BatchScheduleHelper.isClassSessionForStudent(sessionAll, studentCs12A, courses, batches))
    assertTrue(BatchScheduleHelper.isClassSessionForStudent(sessionAll, studentMath11A, courses, batches))
  }
}
