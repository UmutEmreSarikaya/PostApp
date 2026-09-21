package com.uesar.postapp.post.presentation.detail

import androidx.annotation.StringRes
import com.uesar.postapp.post.presentation.model.PostUiModel

internal data class PostDetailUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val post: PostUiModel? = null,
    val title: String = "",
    val body: String = "",
    val isDiscardConfirmationVisible: Boolean = false,
    @param:StringRes val errorMessage: Int? = null,
    @param:StringRes val saveErrorMessage: Int? = null
) {
    val hasUnsavedChanges: Boolean
        get() = post != null && (title != post.title || body != post.body)

    val canSave: Boolean
        get() = hasUnsavedChanges && title.isNotBlank() && body.isNotBlank() && !isSaving
}
