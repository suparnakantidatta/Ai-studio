package com.example.util

import com.example.data.model.ExamQuestion
import com.example.data.model.OnlineExam

object ExamQuestionsProvider {

  fun getQuestionsForExam(exam: OnlineExam): List<ExamQuestion> {
    val subjectLower = exam.subject.lowercase()
    val titleLower = exam.title.lowercase()

    return when {
      subjectLower.contains("computer") || subjectLower.contains("cs") || subjectLower.contains("code") ||
      subjectLower.contains("c") || subjectLower.contains("python") || subjectLower.contains("sql") ||
      titleLower.contains("computer") || titleLower.contains("c") || titleLower.contains("python") || titleLower.contains("sql") -> {
        getComputerScienceQuestions(exam.id)
      }
      subjectLower.contains("physic") || titleLower.contains("physic") -> {
        getPhysicsQuestions(exam.id)
      }
      subjectLower.contains("chem") || titleLower.contains("chem") -> {
        getChemistryQuestions(exam.id)
      }
      subjectLower.contains("math") || titleLower.contains("math") -> {
        getMathQuestions(exam.id)
      }
      subjectLower.contains("bio") || titleLower.contains("bio") -> {
        getBiologyQuestions(exam.id)
      }
      else -> {
        getGeneralAcademicQuestions(exam.id, exam.title, exam.subject)
      }
    }
  }

  private fun getComputerScienceQuestions(examId: String): List<ExamQuestion> = listOf(
    ExamQuestion(
      id = "q-$examId-1",
      examId = examId,
      questionText = "Which of the following is the correct format specifier for printing an integer in C programming?",
      optionA = "%s",
      optionB = "%d",
      optionC = "%c",
      optionD = "%f",
      correctOption = "B",
      marks = 2
    ),
    ExamQuestion(
      id = "q-$examId-2",
      examId = examId,
      questionText = "In Python, which built-in function is used to determine the length of a list or string?",
      optionA = "size()",
      optionB = "count()",
      optionC = "len()",
      optionD = "length()",
      correctOption = "C",
      marks = 2
    ),
    ExamQuestion(
      id = "q-$examId-3",
      examId = examId,
      questionText = "Which SQL clause is used to filter records returned by a SELECT query based on specified conditions?",
      optionA = "ORDER BY",
      optionB = "WHERE",
      optionC = "GROUP BY",
      optionD = "LIMIT",
      correctOption = "B",
      marks = 2
    ),
    ExamQuestion(
      id = "q-$examId-4",
      examId = examId,
      questionText = "What does HTML stand for in web technology?",
      optionA = "Hyper Text Markup Language",
      optionB = "High Tech Modern Language",
      optionC = "Hyper Transfer Mode Link",
      optionD = "Home Tool Management Logic",
      correctOption = "A",
      marks = 2
    ),
    ExamQuestion(
      id = "q-$examId-5",
      examId = examId,
      questionText = "In spreadsheet applications like MS Excel, which symbol MUST precede any mathematical formula?",
      optionA = "#",
      optionB = "@",
      optionC = "=",
      optionD = ":",
      correctOption = "C",
      marks = 2
    ),
    ExamQuestion(
      id = "q-$examId-6",
      examId = examId,
      questionText = "What is the output of the Python expression: 3 * (2 + 4) // 2?",
      optionA = "9",
      optionB = "7",
      optionC = "18",
      optionD = "6",
      correctOption = "A",
      marks = 2
    ),
    ExamQuestion(
      id = "q-$examId-7",
      examId = examId,
      questionText = "In C programming, what is the default return type of the main() function?",
      optionA = "void",
      optionB = "int",
      optionC = "char",
      optionD = "float",
      correctOption = "B",
      marks = 2
    ),
    ExamQuestion(
      id = "q-$examId-8",
      examId = examId,
      questionText = "Which SQL command is used to add new rows of data into an existing database table?",
      optionA = "INSERT INTO",
      optionB = "UPDATE",
      optionC = "ADD ROW",
      optionD = "MODIFY",
      correctOption = "A",
      marks = 2
    ),
    ExamQuestion(
      id = "q-$examId-9",
      examId = examId,
      questionText = "Which HTML tag is used to create a hyperlink to another web page?",
      optionA = "<link>",
      optionB = "<href>",
      optionC = "<a>",
      optionD = "<url>",
      correctOption = "C",
      marks = 2
    ),
    ExamQuestion(
      id = "q-$examId-10",
      examId = examId,
      questionText = "In Excel, which formula correctly calculates the sum of all cells from A1 through A10?",
      optionA = "=ADD(A1:A10)",
      optionB = "=SUM(A1..A10)",
      optionC = "=SUM(A1:A10)",
      optionD = "=TOTAL(A1:A10)",
      correctOption = "C",
      marks = 2
    )
  )

  private fun getPhysicsQuestions(examId: String): List<ExamQuestion> = listOf(
    ExamQuestion(
      id = "q-$examId-1",
      examId = examId,
      questionText = "What is the SI unit of electrical resistance?",
      optionA = "Ampere",
      optionB = "Volt",
      optionC = "Ohm",
      optionD = "Watt",
      correctOption = "C",
      marks = 2
    ),
    ExamQuestion(
      id = "q-$examId-2",
      examId = examId,
      questionText = "According to Newton's Second Law of Motion, Force equals:",
      optionA = "Mass × Velocity",
      optionB = "Mass × Acceleration",
      optionC = "Mass / Acceleration",
      optionD = "Velocity / Time",
      correctOption = "B",
      marks = 2
    ),
    ExamQuestion(
      id = "q-$examId-3",
      examId = examId,
      questionText = "What is the value of acceleration due to gravity (g) near the surface of the Earth?",
      optionA = "9.8 m/s²",
      optionB = "8.9 m/s²",
      optionC = "11.2 m/s²",
      optionD = "6.67 m/s²",
      correctOption = "A",
      marks = 2
    ),
    ExamQuestion(
      id = "q-$examId-4",
      examId = examId,
      questionText = "Which law states that current through a conductor is directly proportional to voltage across it?",
      optionA = "Coulomb's Law",
      optionB = "Faraday's Law",
      optionC = "Ohm's Law",
      optionD = "Ampere's Law",
      correctOption = "C",
      marks = 2
    ),
    ExamQuestion(
      id = "q-$examId-5",
      examId = examId,
      questionText = "Kinetic energy of a body having mass 'm' and moving with velocity 'v' is given by:",
      optionA = "m * v",
      optionB = "1/2 * m * v²",
      optionC = "m * g * h",
      optionD = "2 * m * v",
      correctOption = "B",
      marks = 2
    )
  )

  private fun getChemistryQuestions(examId: String): List<ExamQuestion> = listOf(
    ExamQuestion(
      id = "q-$examId-1",
      examId = examId,
      questionText = "What is the atomic number of Carbon?",
      optionA = "6",
      optionB = "12",
      optionC = "14",
      optionD = "8",
      correctOption = "A",
      marks = 2
    ),
    ExamQuestion(
      id = "q-$examId-2",
      examId = examId,
      questionText = "What is the pH value of pure distilled water at 25°C?",
      optionA = "0",
      optionB = "5",
      optionC = "7",
      optionD = "14",
      correctOption = "C",
      marks = 2
    ),
    ExamQuestion(
      id = "q-$examId-3",
      examId = examId,
      questionText = "Which gas is liberated when an active metal reacts with dilute hydrochloric acid?",
      optionA = "Oxygen",
      optionB = "Hydrogen",
      optionC = "Chlorine",
      optionD = "Carbon dioxide",
      correctOption = "B",
      marks = 2
    ),
    ExamQuestion(
      id = "q-$examId-4",
      examId = examId,
      questionText = "The chemical formula of Washing Soda is:",
      optionA = "NaHCO3",
      optionB = "Na2CO3 . 10H2O",
      optionC = "CaSO4 . 2H2O",
      optionD = "NaOH",
      correctOption = "B",
      marks = 2
    ),
    ExamQuestion(
      id = "q-$examId-5",
      examId = examId,
      questionText = "Which type of chemical bond involves the sharing of electron pairs between atoms?",
      optionA = "Ionic Bond",
      optionB = "Covalent Bond",
      optionC = "Metallic Bond",
      optionD = "Hydrogen Bond",
      correctOption = "B",
      marks = 2
    )
  )

  private fun getMathQuestions(examId: String): List<ExamQuestion> = listOf(
    ExamQuestion(
      id = "q-$examId-1",
      examId = examId,
      questionText = "What are the roots of the quadratic equation x² - 5x + 6 = 0?",
      optionA = "2 and 3",
      optionB = "-2 and -3",
      optionC = "1 and 6",
      optionD = "-1 and -6",
      correctOption = "A",
      marks = 2
    ),
    ExamQuestion(
      id = "q-$examId-2",
      examId = examId,
      questionText = "What is the value of sin(90°) + cos(0°)?",
      optionA = "0",
      optionB = "1",
      optionC = "2",
      optionD = "undefined",
      correctOption = "C",
      marks = 2
    ),
    ExamQuestion(
      id = "q-$examId-3",
      examId = examId,
      questionText = "If the radius of a circle is 7 cm, what is its circumference? (Use π = 22/7)",
      optionA = "22 cm",
      optionB = "44 cm",
      optionC = "154 cm",
      optionD = "88 cm",
      correctOption = "B",
      marks = 2
    ),
    ExamQuestion(
      id = "q-$examId-4",
      examId = examId,
      questionText = "What is the derivative of f(x) = x³ with respect to x?",
      optionA = "3x²",
      optionB = "x²",
      optionC = "3x",
      optionD = "x⁴/4",
      correctOption = "A",
      marks = 2
    ),
    ExamQuestion(
      id = "q-$examId-5",
      examId = examId,
      questionText = "What is the 10th term of the Arithmetic Progression: 2, 5, 8, 11...?",
      optionA = "27",
      optionB = "29",
      optionC = "31",
      optionD = "33",
      correctOption = "B",
      marks = 2
    )
  )

  private fun getBiologyQuestions(examId: String): List<ExamQuestion> = listOf(
    ExamQuestion(
      id = "q-$examId-1",
      examId = examId,
      questionText = "Which organelle is universally referred to as the 'Powerhouse of the Cell'?",
      optionA = "Ribosome",
      optionB = "Nucleus",
      optionC = "Mitochondria",
      optionD = "Golgi Apparatus",
      correctOption = "C",
      marks = 2
    ),
    ExamQuestion(
      id = "q-$examId-2",
      examId = examId,
      questionText = "What is the primary green pigment in plant leaves responsible for photosynthesis?",
      optionA = "Carotene",
      optionB = "Chlorophyll",
      optionC = "Xanthophyll",
      optionD = "Anthocyanin",
      correctOption = "B",
      marks = 2
    ),
    ExamQuestion(
      id = "q-$examId-3",
      examId = examId,
      questionText = "Which blood cells are primarily responsible for transporting oxygen throughout the human body?",
      optionA = "Platelets",
      optionB = "White Blood Cells",
      optionC = "Red Blood Cells (Erythrocytes)",
      optionD = "Plasma",
      correctOption = "C",
      marks = 2
    ),
    ExamQuestion(
      id = "q-$examId-4",
      examId = examId,
      questionText = "Which chamber of the human heart pumps oxygenated blood to the aorta and whole body?",
      optionA = "Right Atrium",
      optionB = "Left Atrium",
      optionC = "Right Ventricle",
      optionD = "Left Ventricle",
      correctOption = "D",
      marks = 2
    ),
    ExamQuestion(
      id = "q-$examId-5",
      examId = examId,
      questionText = "What is the genetic material in human cells?",
      optionA = "RNA only",
      optionB = "DNA",
      optionC = "ATP",
      optionD = "Glucose",
      correctOption = "B",
      marks = 2
    )
  )

  private fun getGeneralAcademicQuestions(examId: String, title: String, subject: String): List<ExamQuestion> = listOf(
    ExamQuestion(
      id = "q-$examId-1",
      examId = examId,
      questionText = "In academic study of $subject, what is considered the primary foundation for systematic learning?",
      optionA = "Continuous practice and concept clarity",
      optionB = "Rote memorization only",
      optionC = "Skipping revision",
      optionD = "Ignoring syllabus benchmarks",
      correctOption = "A",
      marks = 2
    ),
    ExamQuestion(
      id = "q-$examId-2",
      examId = examId,
      questionText = "Regarding $title, which method best guarantees accurate problem solving?",
      optionA = "Guesswork",
      optionB = "Step-by-step analytical reasoning",
      optionC = "Rushing without verification",
      optionD = "Memorizing only final answers",
      correctOption = "B",
      marks = 2
    ),
    ExamQuestion(
      id = "q-$examId-3",
      examId = examId,
      questionText = "What is the standard unit of time in the International System of Units (SI)?",
      optionA = "Hour",
      optionB = "Minute",
      optionC = "Second",
      optionD = "Millisecond",
      correctOption = "C",
      marks = 2
    ),
    ExamQuestion(
      id = "q-$examId-4",
      examId = examId,
      questionText = "Which of the following is essential during an online Computer Based Test (CBT)?",
      optionA = "Regular time tracking and review of marked answers",
      optionB = "Leaving all answers unsubmitted",
      optionC = "Submitting immediately without reading",
      optionD = "Closing the test prematurely",
      correctOption = "A",
      marks = 2
    ),
    ExamQuestion(
      id = "q-$examId-5",
      examId = examId,
      questionText = "What is the passing criteria standard across board exams and center assessments?",
      optionA = "10%",
      optionB = "40% or above",
      optionC = "99% only",
      optionD = "5%",
      correctOption = "B",
      marks = 2
    )
  )
}
