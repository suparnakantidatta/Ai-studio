package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.AdminAccount
import com.example.data.model.AdmissionApplication
import com.example.data.model.Batch
import com.example.data.model.CenterInfo
import com.example.data.model.ClassRecording
import com.example.data.model.Course
import com.example.data.model.DatabaseDataDto
import com.example.data.model.Discount
import com.example.data.model.Educator
import com.example.data.model.ExamQuestion
import com.example.data.model.ExamSubmission
import com.example.data.model.Expense
import com.example.data.model.FeePayment
import com.example.data.model.LiveClassSession
import com.example.data.model.OnlineExam
import com.example.data.model.Student
import com.example.data.model.StudyMaterial
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class LocalDataStore(context: Context) {
  private val prefs: SharedPreferences = context.getSharedPreferences("pixel_pathsala_local_db", Context.MODE_PRIVATE)

  private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

  companion object {
    val INITIAL_CENTER = CenterInfo(
      name = "Pixel Pathsala",
      tagline = "Code your dream, decode the future",
      logoUrl = "https://lh3.googleusercontent.com/gps-cs-s/AHRPTWmkXJcvkJ81xwi4NxqpKxQtoyMrzY2k1dOryC5CWeY0bBVUxfHpORGdJ1W2wUYA8IR0y05v1RfvCb4McybkreQUJqtTNZCd9nT9b0zB7uelTL-7B5z7M1aqR9mDohOwKNOYF5qjO2RbDeN1=s1360-w1360-h1020-rw",
      bgUrl = "https://lh3.googleusercontent.com/gps-cs-s/AHRPTWkosGLnTUdSyNjD_qft1aTOQPyOnPZB0uy5j8dFgel7_leZVbbYyp_fjEkkRgsBF2C1512ZGpjzyCo0TBbLKl88Zx2eSYZHiuNJMx370aU9glHDJt3v48cFDsZR-ulR6OsHByk3KXJauy8T=s1360-w1360-h1020-rw",
      address = "Raina Main Road, Near Bus Stand",
      cityState = "Raina, Purba Bardhaman (Burdwan), West Bengal - 713424",
      phonePrimary = "+91 9775708722",
      phoneSecondary = "+91 9832014567",
      whatsapp = "+91 9775708722",
      email = "pixelpathsala.edu@gmail.com",
      upiId = "9775708722@apl",
      upiName = "Pixel Pathsala Center",
      establishedYear = "2021",
      admissionOpen = true,
      announcement = "Admissions Open for Session 2026-27! Free Registration & Special Early-Bird Discounts available.",
      welcomeMessage = "Welcome to Pixel Pathsala - Premier Institute for Classwise School & College Syllabus, Competitive Exams, and Interactive Coding & DBMS Training."
    )

    val INITIAL_COURSES = listOf(
      Course(
        id = "course-1",
        title = "Computer Science, C, Python & SQL Masterclass",
        code = "CS-PRO-101",
        academicClass = "Class 8-12, BCA & College",
        category = "Computer Science & Coding",
        monthlyFee = 1200.0,
        description = "Comprehensive hands-on programming course covering C logic, Python 3 automation & data structures, Web HTML/CSS basics, and SQL/DBMS database mastery.",
        syllabusPoints = listOf(
          "C Language: Data types, loops, arrays, pointers, dynamic memory & file handling",
          "Python: Object-oriented programming, modules, JSON, file operations & scripting",
          "HTML/CSS/JS: Responsive web pages and UI development fundamentals",
          "DBMS & SQL: ER diagrams, Normalization, DDL, DML, Joins, Subqueries & Transactions"
        ),
        highlights = listOf(
          "Interactive Browser Playzone Access",
          "Live Coding Assessments & Mini Projects",
          "Weekly Doubt Clearing & Dedicated Mentorship"
        ),
        isPopular = true,
        showOnWebsite = true
      ),
      Course(
        id = "course-2",
        title = "Class 11-12 Science & JEE/NEET Integrated Batch",
        code = "SCI-JEE-202",
        academicClass = "Class 11 & Class 12 Science",
        category = "Higher Secondary (11-12)",
        monthlyFee = 2500.0,
        description = "Targeted preparation for WBCHSE / CBSE Board Examinations coupled with foundational JEE Main & NEET entrance question solving techniques.",
        syllabusPoints = listOf(
          "Physics: Mechanics, Waves, Electromagnetism, Optics & Modern Physics",
          "Mathematics: Calculus, Coordinate Geometry, Vectors, Algebra & Trigonometry",
          "Chemistry: Physical, Organic Mechanisms & Inorganic Reactions",
          "Weekly Mock Tests with detailed analysis & OMR scoring"
        ),
        highlights = listOf(
          "Chapter-wise Printed DPP & Modules",
          "1-on-1 Mentorship by Expert Faculties",
          "Air-conditioned smart classrooms with projector"
        ),
        isPopular = true,
        showOnWebsite = true
      ),
      Course(
        id = "course-3",
        title = "Class 9-10 Secondary Board Mastery (Maths & Science)",
        code = "SEC-BD-303",
        academicClass = "Class 9 & Class 10",
        category = "School (Class 8-10)",
        monthlyFee = 1500.0,
        description = "Complete academic coaching for secondary school students covering Mathematics, Physical Science, and Life Science with rigorous periodic assessments.",
        syllabusPoints = listOf(
          "Mathematics: Arithmetic, Algebra, Geometry, Mensuration & Statistics",
          "Physical Science: Force, Motion, Light, Electricity & Chemical Bonding",
          "Previous 10 Years Board Papers Solution & Answer Writing Drills",
          "Special focus on conceptual clarity and exam confidence"
        ),
        highlights = listOf(
          "Small batch size (max 25 students)",
          "Monthly Progress Report for Parents",
          "Special Sunday revision clinics"
        ),
        isPopular = false,
        showOnWebsite = true
      ),
      Course(
        id = "course-4",
        title = "Web Design, Frontend & Full-Stack Basics",
        code = "WEB-DEV-404",
        academicClass = "Class 9-12 & BCA / Diploma",
        category = "Computer Science & Coding",
        monthlyFee = 1400.0,
        description = "Learn modern web crafting with HTML5, modern CSS3, JavaScript ES6+, backend REST APIs, and SQLite database storage with practical capstone websites.",
        syllabusPoints = listOf(
          "HTML5 semantic tags, Forms, Canvas & Media elements",
          "Modern CSS Flexbox, Grid, Animations & Tailwind utility classes",
          "JavaScript DOM Manipulation, Fetch API & Async/Await",
          "Building dynamic real-world interactive apps & hosting"
        ),
        highlights = listOf(
          "Portfolio Ready 4 Real Projects",
          "Pixel Pathsala Certificate on Completion",
          "Code reviews by industry developers"
        ),
        isPopular = false,
        showOnWebsite = true
      )
    )

    val INITIAL_BATCHES = listOf(
      Batch(
        id = "batch-1",
        name = "Batch C-Python Coding Alpha (MWF)",
        courseId = "course-1",
        academicClass = "Class 11 (Science)",
        educatorId = "edu-1",
        scheduleDays = listOf("Mon", "Wed", "Fri"),
        timing = "04:30 PM - 06:00 PM",
        roomNumber = "Lab-1 (Pixel Lab)",
        maxCapacity = 25,
        isActive = true,
        mode = "offline",
        startDate = "2026-04-01"
      ),
      Batch(
        id = "batch-2",
        name = "Batch Science & JEE Pinnacle (TTS)",
        courseId = "course-2",
        academicClass = "Class 12 (Science)",
        educatorId = "edu-2",
        scheduleDays = listOf("Tue", "Thu", "Sat"),
        timing = "05:30 PM - 07:30 PM",
        roomNumber = "Hall-A (Smart Room)",
        maxCapacity = 30,
        isActive = true,
        mode = "offline",
        startDate = "2026-04-01"
      ),
      Batch(
        id = "batch-3",
        name = "Batch Secondary Champions Class 10 (Daily)",
        courseId = "course-3",
        academicClass = "Class 10 (Secondary)",
        educatorId = "edu-4",
        scheduleDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri"),
        timing = "06:30 PM - 08:00 PM",
        roomNumber = "Room-201",
        maxCapacity = 25,
        isActive = true,
        mode = "offline",
        startDate = "2026-04-01"
      ),
      Batch(
        id = "batch-4",
        name = "Batch Web & DBMS Weekend Fast-track",
        courseId = "course-4",
        academicClass = "BCA / B.Sc (Computer Science)",
        educatorId = "edu-1",
        scheduleDays = listOf("Sat", "Sun"),
        timing = "10:00 AM - 01:00 PM",
        roomNumber = "Lab-2",
        maxCapacity = 20,
        isActive = true,
        mode = "online",
        startDate = "2026-05-01"
      )
    )

    val INITIAL_EDUCATORS = listOf(
      Educator(
        id = "edu-1",
        name = "Prof. Subhasish Mukherjee",
        qualification = "M.Tech (Computer Science), GATE Qualified",
        subject = "Computer Science, C, Python & DBMS",
        phone = "9832104561",
        email = "subhasish.cs@pixelpathsala.com",
        monthlySalary = 32000.0,
        assignedBatchIds = listOf("batch-1", "batch-4"),
        experienceYears = 9,
        joiningDate = "2022-01-15",
        status = "active"
      ),
      Educator(
        id = "edu-2",
        name = "Dr. Ananya Roy",
        qualification = "Ph.D. in Mathematics, Jadavpur University",
        subject = "Higher Mathematics & JEE Advanced Prep",
        phone = "9832298712",
        email = "ananya.maths@pixelpathsala.com",
        monthlySalary = 35000.0,
        assignedBatchIds = listOf("batch-2", "batch-3"),
        experienceYears = 11,
        joiningDate = "2022-04-01",
        status = "active"
      ),
      Educator(
        id = "edu-3",
        name = "Er. Rajesh Ghosh",
        qualification = "B.Tech (ECE), Ex-Fiitjee Faculty",
        subject = "Physics (Class 11, 12 & NEET/JEE)",
        phone = "9748561234",
        email = "rajesh.physics@pixelpathsala.com",
        monthlySalary = 30000.0,
        assignedBatchIds = listOf("batch-2"),
        experienceYears = 7,
        joiningDate = "2023-03-10",
        status = "active"
      ),
      Educator(
        id = "edu-4",
        name = "Ms. Sharmila Paul",
        qualification = "M.Sc (Chemistry), B.Ed",
        subject = "Chemistry & Secondary Science (Class 9-10)",
        phone = "9800456711",
        email = "sharmila.chem@pixelpathsala.com",
        monthlySalary = 26000.0,
        assignedBatchIds = listOf("batch-3"),
        experienceYears = 6,
        joiningDate = "2023-07-01",
        status = "active"
      )
    )

    val INITIAL_STUDENTS = listOf(
      Student(
        id = "stu-101",
        rollNo = "PP-2026-001",
        name = "Rohan Sharma",
        studentClass = "Class 11 (Science)",
        mode = "offline",
        mobile = "9876543210",
        aadhaarNo = "453289012345",
        email = "rohan.sharma@example.com",
        guardianName = "Manoj Sharma",
        guardianPhone = "9876543211",
        address = "Station Road, Raina, Purba Bardhaman",
        courseId = "course-1",
        batchId = "batch-1",
        admissionDate = "2026-04-05",
        admissionMonth = "2026-04",
        status = "active",
        notes = "Enthusiastic programmer, interested in C pointers and Python algorithms."
      ),
      Student(
        id = "stu-102",
        rollNo = "PP-2026-002",
        name = "Priyanka Das",
        studentClass = "Class 12 (Science)",
        mode = "offline",
        mobile = "9832145678",
        aadhaarNo = "789012345678",
        email = "priyanka.das@example.com",
        guardianName = "Soumen Das",
        guardianPhone = "9832145679",
        address = "Burdwan Town, Purba Bardhaman",
        courseId = "course-2",
        batchId = "batch-2",
        admissionDate = "2026-04-10",
        admissionMonth = "2026-04",
        status = "active",
        notes = "Aiming for JEE Advanced 2027."
      ),
      Student(
        id = "stu-103",
        rollNo = "PP-2026-003",
        name = "Aniket Sengupta",
        studentClass = "Class 10 (Secondary)",
        mode = "offline",
        mobile = "9748901234",
        aadhaarNo = "234567890123",
        email = "aniket.sen@example.com",
        guardianName = "Debabrata Sengupta",
        guardianPhone = "9748901235",
        address = "College Road, Raina, Purba Bardhaman",
        courseId = "course-3",
        batchId = "batch-3",
        admissionDate = "2026-05-02",
        admissionMonth = "2026-05",
        status = "active",
        notes = "Class 10 student preparing for WBBSE Board."
      ),
      Student(
        id = "stu-104",
        rollNo = "PP-2026-004",
        name = "Debojyoti Banerjee",
        studentClass = "Class 11 (Science)",
        mode = "offline",
        mobile = "9800112233",
        aadhaarNo = "901234567890",
        email = "debo.banerjee@example.com",
        guardianName = "Partha Banerjee",
        guardianPhone = "9800112234",
        address = "Madhabdihi, Raina, Purba Bardhaman",
        courseId = "course-1",
        batchId = "batch-1",
        admissionDate = "2026-05-15",
        admissionMonth = "2026-05",
        status = "active",
        notes = "Interested in C and Database applications."
      )
    )

    val INITIAL_PAYMENTS = listOf(
      FeePayment(
        id = "pay-101",
        receiptNo = "REC/2026/04-001",
        studentId = "stu-101",
        studentName = "Rohan Sharma",
        studentAadhaar = "453289012345",
        studentMobile = "9876543210",
        courseTitle = "Computer Science, C, Python & SQL Masterclass",
        batchName = "Batch C-Python Coding Alpha (MWF)",
        month = "April 2026",
        monthsCovered = listOf("2026-04"),
        baseMonthlyFee = 1200.0,
        totalBaseFee = 1200.0,
        totalDiscount = 0.0,
        finalAmountPaid = 1200.0,
        remainingDue = 0.0,
        paymentMode = "UPI",
        transactionRef = "UPI/20260405/9821034123",
        status = "approved",
        paymentDate = "2026-04-05",
        approvedBy = "admin",
        remarks = "Admission month fee paid via GPay",
        createdAt = "2026-04-05T10:30:00.000Z"
      ),
      FeePayment(
        id = "pay-102",
        receiptNo = "REC/2026/05-002",
        studentId = "stu-101",
        studentName = "Rohan Sharma",
        studentAadhaar = "453289012345",
        studentMobile = "9876543210",
        courseTitle = "Computer Science, C, Python & SQL Masterclass",
        batchName = "Batch C-Python Coding Alpha (MWF)",
        month = "May 2026",
        monthsCovered = listOf("2026-05"),
        baseMonthlyFee = 1200.0,
        totalBaseFee = 1200.0,
        totalDiscount = 0.0,
        finalAmountPaid = 1200.0,
        remainingDue = 0.0,
        paymentMode = "Cash",
        transactionRef = "CSH-2026-0510",
        status = "approved",
        paymentDate = "2026-05-10",
        approvedBy = "admin",
        remarks = "Monthly fee collected in cash at center",
        createdAt = "2026-05-10T16:45:00.000Z"
      ),
      FeePayment(
        id = "pay-103",
        receiptNo = "REC/2026/04-003",
        studentId = "stu-102",
        studentName = "Priyanka Das",
        studentAadhaar = "789012345678",
        studentMobile = "9832145678",
        courseTitle = "Class 11-12 Science & JEE/NEET Integrated Batch",
        batchName = "Batch Science & JEE Pinnacle (TTS)",
        month = "April 2026",
        monthsCovered = listOf("2026-04"),
        baseMonthlyFee = 2500.0,
        totalBaseFee = 2500.0,
        totalDiscount = 0.0,
        finalAmountPaid = 2500.0,
        remainingDue = 0.0,
        paymentMode = "UPI",
        transactionRef = "UPI/20260410/5544332211",
        status = "approved",
        paymentDate = "2026-04-10",
        approvedBy = "admin",
        remarks = "Paid via PhonePe QR",
        createdAt = "2026-04-10T11:20:00.000Z"
      )
    )

    val INITIAL_ADMISSIONS = listOf(
      AdmissionApplication(
        id = "adm-001",
        applicationNo = "APP/2026/101",
        studentName = "Sourav Mondal",
        studentClass = "Class 11 Science",
        guardianName = "Biplab Mondal",
        mobile = "9832445566",
        guardianPhone = "9832445567",
        aadhaarNo = "345678901234",
        email = "sourav.mondal@example.com",
        address = "Raina, Purba Bardhaman",
        targetCourseId = "course-1",
        preferredBatchId = "batch-1",
        appliedDate = "2026-08-15",
        status = "pending",
        initialPaymentStatus = "paid_advance",
        initialPaymentMode = "UPI",
        initialPaymentRef = "UPI/9775708722/ADM7781",
        initialPaymentAmount = 1200.0,
        remarks = "Interested in C and Python Weekend / Evening batch."
      ),
      AdmissionApplication(
        id = "adm-002",
        applicationNo = "APP/2026/102",
        studentName = "Ritika Saha",
        studentClass = "Class 10 Passed (92%)",
        guardianName = "Alok Saha",
        mobile = "9748112299",
        guardianPhone = "9748112298",
        aadhaarNo = "567890123456",
        email = "ritika.saha@example.com",
        address = "Burdwan Sadar, Purba Bardhaman",
        targetCourseId = "course-2",
        preferredBatchId = "batch-2",
        appliedDate = "2026-08-18",
        status = "pending",
        initialPaymentStatus = "at_center",
        initialPaymentMode = "CASH",
        initialPaymentAmount = 2500.0,
        remarks = "Merit discount candidate for Science & JEE Batch."
      )
    )

    val INITIAL_LIVE_CLASSES = listOf(
      LiveClassSession(
        id = "live-1",
        title = "Data Structures & Pointers in C/C++ Live Workshop",
        academicClass = "Class 11 (Science)",
        targetClass = "Class 11 (Science)",
        subject = "Computer Science & Coding",
        courseId = "course-1",
        batchId = "batch-1",
        educatorName = "Prof. Subhasish Mukherjee",
        scheduledDate = "2026-09-01",
        startTime = "04:30 PM",
        endTime = "06:00 PM",
        status = "live",
        platform = "google_meet",
        meetingUrl = "https://meet.google.com/pqp-vpxw-gky",
        isRecordingAvailable = true
      ),
      LiveClassSession(
        id = "live-2",
        title = "Class 12 Physics: Electromagnetic Induction & PYQ Sprint",
        academicClass = "Class 12 (Science)",
        targetClass = "Class 12 (Science)",
        subject = "Physics (Science)",
        courseId = "course-2",
        batchId = "batch-2",
        educatorName = "Er. Rajesh Ghosh",
        scheduledDate = "2026-09-02",
        startTime = "05:30 PM",
        endTime = "07:30 PM",
        status = "scheduled",
        platform = "google_meet",
        meetingUrl = "https://meet.google.com/abc-phys-xyz",
        isRecordingAvailable = false
      ),
      LiveClassSession(
        id = "live-3",
        title = "Class 10 Mathematics: Quadratic Equations & Board Mastery",
        academicClass = "Class 10 (Secondary)",
        targetClass = "Class 10 (Secondary)",
        subject = "Mathematics",
        courseId = "course-3",
        batchId = "batch-3",
        educatorName = "Ms. Sharmila Paul",
        scheduledDate = "2026-09-03",
        startTime = "06:30 PM",
        endTime = "08:00 PM",
        status = "scheduled",
        platform = "google_meet",
        meetingUrl = "https://meet.google.com/sec-math-live",
        isRecordingAvailable = false
      )
    )

    val INITIAL_RECORDINGS = listOf(
      ClassRecording(
        id = "rec-1",
        title = "Python 3 Core Fundamentals: Lists, Dictionaries & Functions",
        academicClass = "Class 11 (Science)",
        subject = "Computer Science",
        topic = "Python Collections & Modular Programming",
        chapter = "Chapter 3: Collections & Modular Code",
        courseId = "course-1",
        batchId = "batch-1",
        educatorName = "Prof. Subhasish Mukherjee",
        recordedDate = "2026-08-25",
        durationMinutes = 75,
        videoSourceType = "youtube",
        videoUrl = "https://www.youtube.com/watch?v=_uQrJ0TkZlc",
        notesPdfUrl = "https://docs.python.org/3/tutorial/",
        viewCount = 68
      ),
      ClassRecording(
        id = "rec-2",
        title = "HTML5 Semantic Layouts & Responsive CSS Flexbox",
        academicClass = "BCA / B.Sc (Computer Science)",
        subject = "Web Design & Frontend",
        topic = "Flexbox Architecture & Mobile First Layouts",
        chapter = "Chapter 2: Modern CSS Layout Systems",
        courseId = "course-4",
        batchId = "batch-4",
        educatorName = "Prof. Subhasish Mukherjee",
        recordedDate = "2026-08-26",
        durationMinutes = 80,
        videoSourceType = "youtube",
        videoUrl = "https://www.youtube.com/watch?v=kUMe1FH4CHE",
        notesPdfUrl = null,
        viewCount = 42
      )
    )

    val INITIAL_STUDY_MATERIALS = listOf(
      StudyMaterial(
        id = "mat-1",
        title = "Python Complete Reference Guide & Syntax Cheat Sheet",
        description = "Comprehensive chapter-wise reference manual covering Loops, Functions, OOPs and File Handling in Python 3.12.",
        batchId = "batch-1",
        courseId = "course-1",
        subject = "Computer Science (Python)",
        fileType = "pdf",
        fileName = "Python_Complete_Reference_2026.pdf",
        fileSize = "2.8 MB",
        fileUrl = "https://docs.python.org/3/tutorial/",
        uploadedBy = "Prof. Subhasish Mukherjee",
        uploadedDate = "2026-04-05",
        downloadCount = 24
      ),
      StudyMaterial(
        id = "mat-2",
        title = "Class 12 Physics & Chemistry Formula Handout (JEE/NEET)",
        description = "Instant revision formula booklet for Electrostatics, Magnetism, Optics and Organic Reaction Mechanisms.",
        batchId = "batch-2",
        courseId = "course-2",
        subject = "Physics & Chemistry",
        fileType = "pdf",
        fileName = "JEE_NEET_Formula_Bank_Class12.pdf",
        fileSize = "4.1 MB",
        fileUrl = "https://en.wikipedia.org/wiki/Physics",
        uploadedBy = "Er. Rajesh Ghosh",
        uploadedDate = "2026-04-08",
        downloadCount = 42
      ),
      StudyMaterial(
        id = "mat-3",
        title = "C Programming & Data Structures Lab Assignment Set",
        description = "Contains 30 practical lab coding problems with input test cases and logic dry-runs.",
        batchId = "all",
        courseId = "course-1",
        subject = "C Programming & Algorithms",
        fileType = "doc",
        fileName = "C_Programming_Lab_Problems_Set1.docx",
        fileSize = "1.2 MB",
        fileUrl = "https://en.cppreference.com/w/c",
        uploadedBy = "Prof. Subhasish Mukherjee",
        uploadedDate = "2026-04-12",
        downloadCount = 18
      )
    )

    val INITIAL_QUESTIONS = listOf(
      ExamQuestion(
        id = "q-1",
        courseId = "course-1",
        academicClass = "Class 11 (Science)",
        subject = "C Programming",
        topic = "Pointers & Memory",
        marks = 2,
        difficulty = "easy",
        questionText = "In C language, what will be the output of printing `*(&x)` if `int x = 42;`?",
        options = listOf("The memory address of x", "42 (Value of x)", "Syntax Error", "Garbage Value"),
        correctOptionIndex = 1,
        explanation = "&x gives the address of variable x, and dereferencing it with * gives the value stored at that address, which is 42."
      ),
      ExamQuestion(
        id = "q-2",
        courseId = "course-1",
        academicClass = "Class 11 (Science)",
        subject = "Python 3",
        topic = "Data Types & Mutability",
        marks = 2,
        difficulty = "easy",
        questionText = "Which of the following built-in data types in Python 3 is IMMUTABLE?",
        options = listOf("List", "Dictionary", "Tuple", "Set"),
        correctOptionIndex = 2,
        explanation = "Tuples and Strings in Python are immutable; once created, their elements cannot be modified in place."
      ),
      ExamQuestion(
        id = "q-3",
        courseId = "course-1",
        academicClass = "Class 11 (Science)",
        subject = "DBMS & SQL",
        topic = "Relational Model",
        marks = 2,
        difficulty = "medium",
        questionText = "Which SQL clause is used to filter records after aggregate calculations like COUNT() or SUM()?",
        options = listOf("WHERE", "HAVING", "GROUP BY", "ORDER BY"),
        correctOptionIndex = 1,
        explanation = "The HAVING clause was added to SQL because the WHERE keyword cannot be used with aggregate functions."
      ),
      ExamQuestion(
        id = "q-4",
        courseId = "course-1",
        academicClass = "Class 11 (Science)",
        subject = "Python 3",
        topic = "List Comprehensions",
        marks = 2,
        difficulty = "medium",
        questionText = "What is the output of `[x**2 for x in range(4) if x % 2 != 0]` in Python?",
        options = listOf("[0, 1, 4, 9]", "[1, 9]", "[0, 4]", "[1, 4]"),
        correctOptionIndex = 1,
        explanation = "range(4) produces 0, 1, 2, 3. The odd numbers are 1 and 3. Their squares are 1 and 9."
      )
    )

    val INITIAL_EXAMS = listOf(
      OnlineExam(
        id = "exam-1",
        examCode = "EXAM-PY-101",
        title = "Computer Science & Python Assessment (Mid-Term)",
        courseId = "course-1",
        batchId = "batch-1",
        academicClass = "Class 11 (Science)",
        subject = "Computer Science & Python",
        durationMinutes = 20,
        totalMarks = 8,
        passingMarks = 4,
        passingPercentage = 50,
        status = "active",
        mode = "online",
        scheduledDate = "2026-09-05",
        startTime = "10:00 AM",
        endTime = "11:00 AM",
        questionIds = listOf("q-1", "q-2", "q-3", "q-4"),
        instructions = "1. All 4 questions are compulsory.\n2. Each correct answer carries 2 marks.\n3. Negative marking: None.",
        isPublished = true
      )
    )

    val INITIAL_ADMIN = AdminAccount(
      id = "ADM-001",
      username = "admin",
      password = "admin123",
      name = "Director / Center Administrator",
      role = "Master Administrator",
      email = "admin@pixelpathsala.com",
      phone = "+91 9775708722",
      securityPin = "7722",
      status = "active"
    )
  }

  // Generic JSON load / save helpers
  private inline fun <reified T> getObject(key: String, defaultValue: T): T {
    val json = prefs.getString(key, null) ?: return defaultValue
    return try {
      val adapter = moshi.adapter(T::class.java)
      adapter.fromJson(json) ?: defaultValue
    } catch (_: Exception) {
      defaultValue
    }
  }

  private inline fun <reified T> putObject(key: String, value: T) {
    val adapter = moshi.adapter(T::class.java)
    prefs.edit().putString(key, adapter.toJson(value)).apply()
  }

  private inline fun <reified T> getNullableObject(key: String): T? {
    val json = prefs.getString(key, null) ?: return null
    return try {
      val adapter = moshi.adapter(T::class.java)
      adapter.fromJson(json)
    } catch (_: Exception) {
      null
    }
  }

  private inline fun <reified T> putNullableObject(key: String, value: T?) {
    if (value == null) {
      prefs.edit().remove(key).apply()
    } else {
      val adapter = moshi.adapter(T::class.java)
      prefs.edit().putString(key, adapter.toJson(value)).apply()
    }
  }

  // Active User Session Persistence
  fun getLoggedInStudent(): Student? = getNullableObject("active_session_student")
  fun saveLoggedInStudent(student: Student?) = putNullableObject("active_session_student", student)

  fun getLoggedInAdmin(): AdminAccount? = getNullableObject("active_session_admin")
  fun saveLoggedInAdmin(admin: AdminAccount?) = putNullableObject("active_session_admin", admin)

  fun clearSession() {
    saveLoggedInStudent(null)
    saveLoggedInAdmin(null)
  }

  private inline fun <reified T> getList(key: String, defaultValue: List<T>): List<T> {
    val json = prefs.getString(key, null) ?: return defaultValue
    return try {
      val type = Types.newParameterizedType(List::class.java, T::class.java)
      val adapter = moshi.adapter<List<T>>(type)
      adapter.fromJson(json) ?: defaultValue
    } catch (_: Exception) {
      defaultValue
    }
  }

  private inline fun <reified T> putList(key: String, value: List<T>) {
    val type = Types.newParameterizedType(List::class.java, T::class.java)
    val adapter = moshi.adapter<List<T>>(type)
    prefs.edit().putString(key, adapter.toJson(value)).apply()
  }

  // Center Info
  fun getCenterInfo(): CenterInfo = getObject("center_info", INITIAL_CENTER)
  fun saveCenterInfo(info: CenterInfo) = putObject("center_info", info)

  // Students
  fun getStudents(): List<Student> = getList("students", INITIAL_STUDENTS)
  fun saveStudents(list: List<Student>) = putList("students", list)

  // Courses
  fun getCourses(): List<Course> = getList("courses", INITIAL_COURSES)
  fun saveCourses(list: List<Course>) = putList("courses", list)

  // Batches
  fun getBatches(): List<Batch> = getList("batches", INITIAL_BATCHES)
  fun saveBatches(list: List<Batch>) = putList("batches", list)

  // Educators
  fun getEducators(): List<Educator> = getList("educators", INITIAL_EDUCATORS)
  fun saveEducators(list: List<Educator>) = putList("educators", list)

  // Fee Payments
  fun getPayments(): List<FeePayment> = getList("payments", INITIAL_PAYMENTS)
  fun savePayments(list: List<FeePayment>) = putList("payments", list)

  // Admissions
  fun getAdmissions(): List<AdmissionApplication> = getList("admissions", INITIAL_ADMISSIONS)
  fun saveAdmissions(list: List<AdmissionApplication>) = putList("admissions", list)

  // Live Classes
  fun getLiveClasses(): List<LiveClassSession> = getList("live_classes", INITIAL_LIVE_CLASSES)
  fun saveLiveClasses(list: List<LiveClassSession>) = putList("live_classes", list)

  // Recordings
  fun getRecordings(): List<ClassRecording> = getList("recordings", INITIAL_RECORDINGS)
  fun saveRecordings(list: List<ClassRecording>) = putList("recordings", list)

  // Study Materials
  fun getStudyMaterials(): List<StudyMaterial> = getList("materials", INITIAL_STUDY_MATERIALS)
  fun saveStudyMaterials(list: List<StudyMaterial>) = putList("materials", list)

  // Exams & Questions
  fun getExams(): List<OnlineExam> = getList("exams", INITIAL_EXAMS)
  fun saveExams(list: List<OnlineExam>) = putList("exams", list)

  fun getQuestions(): List<ExamQuestion> = getList("questions", INITIAL_QUESTIONS)
  fun saveQuestions(list: List<ExamQuestion>) = putList("questions", list)

  fun getExamSubmissions(): List<ExamSubmission> = getList("submissions", emptyList())
  fun saveExamSubmissions(list: List<ExamSubmission>) = putList("submissions", list)

  // Admin Account
  fun getAdminAccount(): AdminAccount = getObject("admin_account", INITIAL_ADMIN)
  fun saveAdminAccount(admin: AdminAccount) = putObject("admin_account", admin)

  // Save entire dataset snapshot
  fun saveAll(dto: DatabaseDataDto) {
    dto.centerInfo?.let { saveCenterInfo(it) }
    dto.students?.let { saveStudents(it) }
    dto.courses?.let { saveCourses(it) }
    dto.batches?.let { saveBatches(it) }
    dto.educators?.let { saveEducators(it) }
    dto.payments?.let { savePayments(it) }
    dto.admissions?.let { saveAdmissions(it) }
    dto.liveClasses?.let { saveLiveClasses(it) }
    dto.classRecordings?.let { saveRecordings(it) }
    dto.studyMaterials?.let { saveStudyMaterials(it) }
    dto.exams?.let { saveExams(it) }
    dto.questions?.let { saveQuestions(it) }
    dto.examSubmissions?.let { saveExamSubmissions(it) }
    dto.adminAccounts?.firstOrNull()?.let { saveAdminAccount(it) }
  }

  fun toDatabaseDataDto(): DatabaseDataDto {
    return DatabaseDataDto(
      centerInfo = getCenterInfo(),
      students = getStudents(),
      courses = getCourses(),
      batches = getBatches(),
      educators = getEducators(),
      payments = getPayments(),
      admissions = getAdmissions(),
      liveClasses = getLiveClasses(),
      classRecordings = getRecordings(),
      studyMaterials = getStudyMaterials(),
      exams = getExams(),
      questions = getQuestions(),
      examSubmissions = getExamSubmissions(),
      adminAccounts = listOf(getAdminAccount())
    )
  }
}
