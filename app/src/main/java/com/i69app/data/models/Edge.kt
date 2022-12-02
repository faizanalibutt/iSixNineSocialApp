package com.i69app.data.models

import com.i69app.type.UserGender

data class Edge(
    /**
     * A cursor for use in pagination
     */
    val cursor: String,
    /**
     * The item at the end of the edge
     */
    val node: Node?,
)

data class Node(
    val pk: Int?,
    val comment: Int?,
    val createdDate: Any,
    val `file`: String?,
    /**
     * The ID of the object.
     */
    val id: String,
    val like: Int?,
    val momentDescription: String,
    val momentDescriptionPaginated: List<String?>?,
    val user: User2?,
)

data class User2(
    public val id: Any,
    public val email: String,
    public val fullName: String,
    /**
     * Required. 150 characters or fewer. Letters, digits and @/./+/-/_ only.
     */
    public val username: String,
    public val gender: UserGender?,
    public val avatar: Avatar?,
    public val onesignalPlayerId: String?,
    public val avatarPhotos: List<AvatarPhoto>,
)

 data class Avatar(
    public val url: String?,
    public val id: String,
    public val user: String?,
)

 data class AvatarPhoto(
    public val url: String?,
    public val id: String,
    public val user: String?,
)
