package com.uesar.postapp.post.presentation.list

import androidx.annotation.StringRes
import com.uesar.postapp.post.presentation.model.PostUiModel

internal data class PostListUiState(
    val isLoading: Boolean = false,
    val posts: List<PostUiModel> = emptyList(),
    @param:StringRes val errorMessage: Int? = null
)
