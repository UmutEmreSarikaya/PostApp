package com.uesar.postapp.post.presentation.model

import com.uesar.postapp.post.domain.model.Post

internal data class PostUiModel(
    val id: Int,
    val title: String,
    val body: String,
    val imageUrl: String
)

internal fun Post.toUiModel() = PostUiModel(
    id = id,
    title = title,
    body = body,
    imageUrl = "https://picsum.photos/300/300?random=$id&grayscale"
)
