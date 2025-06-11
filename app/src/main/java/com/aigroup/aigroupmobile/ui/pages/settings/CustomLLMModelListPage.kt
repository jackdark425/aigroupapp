package com.aigroup.aigroupmobile.ui.pages.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aigroup.aigroupmobile.connect.chat.CustomChatServiceProvider
import com.aigroup.aigroupmobile.data.models.CustomLLMModel
import com.aigroup.aigroupmobile.data.models.LoadingStatus
import com.aigroup.aigroupmobile.ui.components.ConfirmDialog
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme
import com.aigroup.aigroupmobile.viewmodels.CustomLLMProviderViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomLLMModelListPage(
    onBack: () -> Unit,
    provider: CustomChatServiceProvider,
    onNavigateToAddModel: (CustomChatServiceProvider) -> Unit,
    onNavigateToEditModel: (CustomChatServiceProvider, CustomLLMModel) -> Unit,
    viewModel: CustomLLMProviderViewModel = hiltViewModel()
) {
    val modelsState by viewModel.currentProviderModels.collectAsState()
    var modelToDelete by remember { mutableStateOf<CustomLLMModel?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    // Observe operation result
    val operationResult by viewModel.operationResult.collectAsState()
    
    LaunchedEffect(operationResult) {
        operationResult?.let {
            when (it) {
                is CustomLLMProviderViewModel.OperationResult.Success -> {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(it.message)
                    }
                }
                is CustomLLMProviderViewModel.OperationResult.Error -> {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(it.message)
                    }
                }
            }
            viewModel.clearOperationResult()
        }
    }
    
    // Set current provider
    LaunchedEffect(provider) {
        viewModel.setCurrentProvider(provider)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Models", fontWeight = FontWeight.SemiBold)
                        Text(
                            provider.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppCustomTheme.colorScheme.secondaryLabel
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "", Modifier.size(20.dp))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToAddModel(provider) },
                containerColor = AppCustomTheme.colorScheme.primaryAction,
                contentColor = AppCustomTheme.colorScheme.onPrimaryAction
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Model")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val models = modelsState) {
                is LoadingStatus.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = AppCustomTheme.colorScheme.primaryLabel
                    )
                }
                
                is LoadingStatus.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Error loading models",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = models.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppCustomTheme.colorScheme.secondaryLabel
                        )
                    }
                }
                
                is LoadingStatus.Success -> {
                    if (models.data.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "No models yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = AppCustomTheme.colorScheme.primaryLabel
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap the + button to add a new model",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppCustomTheme.colorScheme.secondaryLabel
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(models.data) { model ->
                                ModelListItem(
                                    model = model,
                                    onEdit = { onNavigateToEditModel(provider, model) },
                                    onDelete = { modelToDelete = model }
                                )
                            }
                            
                            item {
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Confirmation dialog for deleting a model
    if (modelToDelete != null) {
        ConfirmDialog(
            title = "Delete Model",
            message = "Are you sure you want to delete the model '${modelToDelete?.name}'? This action cannot be undone.",
            onConfirm = {
                modelToDelete?.let {
                    viewModel.removeModelFromCurrentProvider(it.id)
                }
                modelToDelete = null
            },
            onDismiss = {
                modelToDelete = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelListItem(
    model: CustomLLMModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        onClick = onEdit
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = model.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppCustomTheme.colorScheme.primaryLabel
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "ID: ${model.id}",
                style = MaterialTheme.typography.bodyMedium,
                color = AppCustomTheme.colorScheme.secondaryLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Context: ${model.contextSize} tokens",
                style = MaterialTheme.typography.bodyMedium,
                color = AppCustomTheme.colorScheme.secondaryLabel
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (model.supportsImage) {
                    Text(
                        text = "Images",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.small
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                if (model.supportsVideo) {
                    Text(
                        text = "Video",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = MaterialTheme.shapes.small
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                if (model.supportsStreaming) {
                    Text(
                        text = "Streaming",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = MaterialTheme.shapes.small
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = AppCustomTheme.colorScheme.primaryAction
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
} 