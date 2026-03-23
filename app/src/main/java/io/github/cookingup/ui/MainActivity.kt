package io.github.cookingup.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.cookingup.ui.screens.*
import io.github.cookingup.ui.theme.CookingUpTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CookingUpTheme {
                val navController = rememberNavController()
                val viewModel: RecipeViewModel = viewModel()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "recipe_list",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("recipe_list") {
                            RecipeListScreen(
                                viewModel = viewModel,
                                onRecipeClick = { id -> navController.navigate("recipe_detail/$id") },
                                onAddRecipeClick = { navController.navigate("add_edit_recipe/0") },
                                onSettingsClick = { navController.navigate("settings") }
                            )
                        }
                        composable("recipe_detail/{recipeId}") { backStackEntry ->
                            val recipeId = backStackEntry.arguments?.getString("recipeId")?.toLong() ?: 0L
                            RecipeDetailScreen(
                                viewModel = viewModel,
                                recipeId = recipeId,
                                onEditClick = { id -> navController.navigate("add_edit_recipe/$id") },
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                        composable("add_edit_recipe/{recipeId}") { backStackEntry ->
                            val recipeId = backStackEntry.arguments?.getString("recipeId")?.toLong() ?: 0L
                            AddEditRecipeScreen(
                                viewModel = viewModel,
                                recipeId = recipeId,
                                onSave = { navController.popBackStack() },
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel,
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
