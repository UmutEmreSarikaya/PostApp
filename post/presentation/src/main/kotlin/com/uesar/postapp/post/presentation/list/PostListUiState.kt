package com.uesar.postapp.post.presentation.list

import androidx.annotation.StringRes
import com.uesar.postapp.post.presentation.model.PostUiModel

internal data class PostListUiState(
    val isLoading: Boolean = true,
    val isRefreshConfirmationVisible: Boolean = false,
    val posts: List<PostUiModel> = emptyList(),
    val snackbarMessage: PostListSnackbarMessage? = null,
    @param:StringRes val errorMessage: Int? = null
)

internal data class PostListSnackbarMessage(
    @param:StringRes val message: Int,
    val deletedPostId: Int? = null
)
