package com.example.data.api

import com.example.data.model.AdminDashboardStats
import com.example.data.model.AdminLoginRequest
import com.example.data.model.AdminLoginResponse
import com.example.data.model.AdmissionApplication
import com.example.data.model.AdmissionApplyRequest
import com.example.data.model.BaseApiResponse
import com.example.data.model.Batch
import com.example.data.model.CenterInfo
import com.example.data.model.Course
import com.example.data.model.DatabaseDataDto
import com.example.data.model.FeePayment
import com.example.data.model.LoadAllResponse
import com.example.data.model.MutateRequest
import com.example.data.model.PayFeeRequest
import com.example.data.model.Student
import com.example.data.model.StudentFeesResponse
import com.example.data.model.StudentLoginRequest
import com.example.data.model.StudentLoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface PixelPathsalaApi {

  // OpenAPI Endpoints (/api/v1/...)

  @GET("health")
  suspend fun checkHealth(): Response<Map<String, Any>>

  @GET("meta")
  suspend fun getMeta(): Response<Map<String, Any>>

  @GET("center")
  suspend fun getCenterInfo(): Response<CenterInfo>

  @POST("auth/student-login")
  suspend fun loginStudent(
    @Body request: StudentLoginRequest
  ): Response<StudentLoginResponse>

  @POST("auth/admin-login")
  suspend fun loginAdmin(
    @Body request: AdminLoginRequest
  ): Response<AdminLoginResponse>

  @GET("student/profile")
  suspend fun getStudentProfile(
    @Query("studentId") studentId: String? = null
  ): Response<Student>

  @GET("student/fees")
  suspend fun getStudentFees(
    @Query("studentId") studentId: String? = null
  ): Response<StudentFeesResponse>

  @POST("student/pay-fee")
  suspend fun payFee(
    @Body request: PayFeeRequest
  ): Response<BaseApiResponse>

  @GET("courses")
  suspend fun getCourses(): Response<List<Course>>

  @GET("batches")
  suspend fun getBatches(): Response<List<Batch>>

  @POST("admissions/apply")
  suspend fun applyAdmission(
    @Body request: AdmissionApplyRequest
  ): Response<BaseApiResponse>

  @GET("admin/dashboard")
  suspend fun getAdminDashboard(): Response<AdminDashboardStats>

  @GET("admin/students")
  suspend fun getAdminStudents(): Response<List<Student>>

  // Master Database Sync Endpoints (can be accessed via full path)
  @GET("/api/db/load-all")
  suspend fun loadAllDatabase(): Response<LoadAllResponse>

  @POST("/api/db/save-all")
  suspend fun saveAllDatabase(
    @Body payload: DatabaseDataDto
  ): Response<BaseApiResponse>

  @POST("/api/db/mutate")
  suspend fun mutateRecord(
    @Body request: MutateRequest
  ): Response<BaseApiResponse>
}
