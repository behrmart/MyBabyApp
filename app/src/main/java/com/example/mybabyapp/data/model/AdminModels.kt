package com.example.mybabyapp.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AdminUser(
    val id: Int,
    val username: String,
    val role: UserRole
)

@Serializable
data class AdminPasswordChangeRequest(
    val password: String
)

@Serializable
data class CreateAdminAlbumRequest(
    val name: String,
    val description: String? = null
)
