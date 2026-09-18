package com.uesar.postapp.post.presentation.list

import com.uesar.postapp.post.presentation.model.PostUiModel

data class PostListUiState(
    val isLoading: Boolean = false,
    val posts: List<PostUiModel> = emptyList(),
    val errorMessage: String? = null
)
