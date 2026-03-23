package io.github.cookingup.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.cookingup.model.Recipe
import io.github.cookingup.ui.RecipeViewModel
import android.content.Intent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    viewModel: RecipeViewModel,
    recipeId: Long,
    onEditClick: (Long) -> Unit,
    onBackClick: () -> Unit
) {
    var recipe by remember { mutableStateOf<Recipe?>(null) }
    var servingScale by remember { mutableStateOf(1f) }
    val context = LocalContext.current

    LaunchedEffect(recipeId) {
        recipe = viewModel.getRecipeById(recipeId)
        servingScale = 1f
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(recipe?.title ?: "Loading...") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        recipe?.let {
                            val uri = viewModel.exportRecipe(context, it)
                            uri?.let { shareUri ->
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/x-curcp"
                                    putExtra(Intent.EXTRA_STREAM, shareUri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Recipe"))
                            }
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = { onEditClick(recipeId) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                }
            )
        }
    ) { padding ->
        recipe?.let { r ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(text = r.description, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "Servings: ${(r.servings * servingScale).toInt()}", style = MaterialTheme.typography.titleMedium)
                    Row {
                        Button(onClick = { if (servingScale > 0.5f) servingScale -= 0.5f }) { Text("-") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = { servingScale += 0.5f }) { Text("+") }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Ingredients", style = MaterialTheme.typography.titleLarge)
                r.ingredients.forEach { ingredient ->
                    Text(
                        text = "• ${String.format("%.2f", ingredient.amount * servingScale)} ${ingredient.unit} ${ingredient.name}",
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Instructions", style = MaterialTheme.typography.titleLarge)
                Text(text = r.instructions, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
