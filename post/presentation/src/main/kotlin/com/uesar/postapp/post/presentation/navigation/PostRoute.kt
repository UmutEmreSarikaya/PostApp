package com.uesar.postapp.post.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface PostRoute {
    @Serializable
    data object PostList : PostRoute

    @Serializable
    data class PostDetail(val postId: Int) : PostRoute
}
