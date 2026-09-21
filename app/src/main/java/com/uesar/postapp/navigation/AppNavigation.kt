package com.uesar.postapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.uesar.postapp.post.presentation.detail.PostDetailScreenRoot
import com.uesar.postapp.post.presentation.list.PostListScreenRoot
import com.uesar.postapp.post.presentation.navigation.PostRoute

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = PostRoute.PostList,
        modifier = modifier
    ) {
        composable<PostRoute.PostList> {
            PostListScreenRoot(
                onPostClick = { postId ->
                    navController.navigate(PostRoute.PostDetail(postId))
                }
            )
        }
        composable<PostRoute.PostDetail> {
            PostDetailScreenRoot(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
