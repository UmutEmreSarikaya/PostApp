package com.uesar.postapp.post.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uesar.postapp.post.domain.repository.PostRepository
import com.uesar.postapp.post.presentation.R
import com.uesar.postapp.post.presentation.model.PostUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
internal class PostListViewModel @Inject constructor(
    private val postRepository: PostRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PostListUiState())
    internal val state: StateFlow<PostListUiState> = _state.asStateFlow()

    init {
        loadPosts()
    }

    private fun loadPosts() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val posts = postRepository.getPosts().mapIndexed { index, post ->
                    PostUiModel(
                        title = post.title,
                        body = post.body,
                        imageUrl = "https://picsum.photos/300/300?random=${index + 1}&grayscale"
                    )
                }
                _state.update { it.copy(
                    posts = posts,
                    isLoading = false,
                    errorMessage = null
                ) }
            } catch (_: IOException) {
                _state.update { it.copy(
                    isLoading = false,
                    errorMessage = R.string.error_no_internet
                ) }
            } catch (_: HttpException) {
                _state.update { it.copy(
                    isLoading = false,
                    errorMessage = R.string.error_server
                ) }
            }
        }
    }
}
