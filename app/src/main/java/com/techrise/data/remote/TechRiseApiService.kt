package com.techrise.data.remote

import retrofit2.http.*

// --- Data Transfer Objects (DTOs) ---

data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String,
    val role: String = "CUSTOMER",
    val adminSecret: String? = null,
    val mobile: String? = null
)

data class RegisterResponse(
    val message: String,
    val userId: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class UserDto(
    val id: String,
    val email: String,
    val role: String
)

data class LoginResponse(
    val message: String,
    val token: String,
    val user: UserDto
)

data class CreateComplaintRequest(
    val title: String,
    val description: String,
    val priority: String,
    val imageBase64: String? = null
)

data class CreateComplaintResponse(
    val message: String,
    val complaintId: String,
    val title: String,
    val description: String,
    val status: String,
    val priority: String,
    val customerId: String
)

data class ComplaintResponse(
    val id: String,
    val title: String,
    val description: String,
    val status: String, // PENDING, IN_PROGRESS, RESOLVED
    val priority: String, // LOW, MEDIUM, HIGH
    val customerId: String,
    val customerEmail: String?,
    val assignedAdminId: String?,
    val assignedAdminEmail: String?,
    val rating: Int?,
    val feedbackComment: String?,
    val imageBase64: String?,
    val createdAt: FirestoreTimestamp?,
    val updatedAt: FirestoreTimestamp?
)

data class FirestoreTimestamp(
    val _seconds: Long,
    val _nanoseconds: Int
)

data class ComplaintLogResponse(
    val id: String,
    val complaintId: String,
    val actionBy: String,
    val actionByEmail: String?,
    val oldStatus: String,
    val newStatus: String,
    val comment: String,
    val createdAt: FirestoreTimestamp
)

data class FeedbackRequest(
    val rating: Int, // 1 to 5
    val comment: String
)

data class FeedbackResponse(
    val message: String
)

data class EmployeeResponse(
    val id: String,
    val email: String
)

data class NewsResponse(
    val id: String,
    val title: String,
    val content: String,
    val authorId: String,
    val createdAt: FirestoreTimestamp
)

data class BannerResponse(
    val id: String,
    val title: String,
    val imageBase64: String,
    val authorId: String,
    val redirectUrl: String? = null,
    val createdAt: FirestoreTimestamp? = null
)

data class CreateNewsRequest(
    val title: String,
    val content: String
)

data class UpdateStatusRequest(
    val status: String,
    val comment: String,
    val priority: String? = null,
    val assignedAdminId: String? = null
)

data class UpdateStatusResponse(
    val message: String,
    val complaintId: String,
    val oldStatus: String,
    val newStatus: String,
    val assignedAdminId: String
)

// --- Retrofit API Service Interface ---

interface TechRiseApiService {

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("complaints")
    suspend fun createComplaint(
        @Header("Authorization") token: String,
        @Body request: CreateComplaintRequest
    ): CreateComplaintResponse

    @GET("complaints")
    suspend fun getComplaints(
        @Header("Authorization") token: String
    ): List<ComplaintResponse>

    @GET("complaints/{id}")
    suspend fun getComplaintById(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): ComplaintResponse

    @GET("complaints/{id}/logs")
    suspend fun getComplaintLogs(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): List<ComplaintLogResponse>

    @POST("complaints/{id}/feedback")
    suspend fun submitFeedback(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Body request: FeedbackRequest
    ): FeedbackResponse

    @GET("news")
    suspend fun getNewsList(
        @Header("Authorization") token: String
    ): List<NewsResponse>

    @GET("banners")
    suspend fun getBanners(
        @Header("Authorization") token: String
    ): List<BannerResponse>

    @POST("news")
    suspend fun createNews(
        @Header("Authorization") token: String,
        @Body request: CreateNewsRequest
    ): NewsResponse

    @DELETE("news/{id}")
    suspend fun deleteNews(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): FeedbackResponse

    @PUT("complaints/{id}/status")
    suspend fun updateComplaintStatus(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Body request: UpdateStatusRequest
    ): UpdateStatusResponse

    @GET("auth/employees")
    suspend fun getEmployees(
        @Header("Authorization") token: String
    ): List<EmployeeResponse>

    @GET("auth/profile")
    suspend fun getProfile(
        @Header("Authorization") token: String
    ): UserProfileResponse

    @PUT("auth/profile")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Body request: UpdateProfileRequest
    ): UpdateProfileResponse
}

data class UserProfileResponse(
    val id: String,
    val name: String,
    val email: String,
    val mobile: String,
    val role: String
)

data class UpdateProfileRequest(
    val name: String,
    val mobile: String
)

data class UpdateProfileResponse(
    val message: String
)
