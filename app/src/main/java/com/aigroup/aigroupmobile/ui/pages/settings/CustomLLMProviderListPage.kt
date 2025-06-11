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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aigroup.aigroupmobile.connect.chat.CustomChatServiceProvider
import com.aigroup.aigroupmobile.data.models.LoadingStatus
import com.aigroup.aigroupmobile.ui.components.ConfirmDialog
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme
import com.aigroup.aigroupmobile.viewmodels.CustomLLMProviderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomLLMProviderListPage(
    onBack: () -> Unit,
    onNavigateToAddProvider: () -> Unit,
    onNavigateToEditProvider: (CustomChatServiceProvider) -> Unit,
    viewModel: CustomLLMProviderViewModel = hiltViewModel()
) {
    val providersState by viewModel.providers.collectAsState()
    var providerToDelete by remember { mutableStateOf<CustomChatServiceProvider?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Custom LLM Providers", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "", Modifier.size(20.dp))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddProvider,
                containerColor = AppCustomTheme.colorScheme.primaryAction,
                contentColor = AppCustomTheme.colorScheme.onPrimaryAction
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Provider")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val providers = providersState) {
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
                            text = "Error loading providers",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = providers.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppCustomTheme.colorScheme.secondaryLabel
                        )
                    }
                }
                
                is LoadingStatus.Success -> {
                    if (providers.data.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "No custom providers yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = AppCustomTheme.colorScheme.primaryLabel
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap the + button to add a new provider",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppCustomTheme.colorScheme.secondaryLabel
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(providers.data) { provider ->
                                ProviderListItem(
                                    provider = provider,
                                    onEdit = { onNavigateToEditProvider(provider) },
                                    onDelete = { providerToDelete = provider },
                                    onToggleEnabled = { isEnabled ->
                                        viewModel.updateProvider(
                                            id = provider.provider.id,
                                            isEnabled = isEnabled
                                        )
                                    }
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
    
    // Confirmation dialog for deleting a provider
    if (providerToDelete != null) {
        ConfirmDialog(
            title = "Delete Provider",
            message = "Are you sure you want to delete the provider '${providerToDelete?.displayName}'? This action cannot be undone.",
            onConfirm = {
                providerToDelete?.let {
                    viewModel.deleteProvider(it.provider.id)
                }
                providerToDelete = null
            },
            onDismiss = {
                providerToDelete = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderListItem(
    provider: CustomChatServiceProvider,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = provider.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppCustomTheme.colorScheme.primaryLabel
                )
                
                Switch(
                    checked = provider.isEnabled,
                    onCheckedChange = onToggleEnabled
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = provider.apiBase,
                style = MaterialTheme.typography.bodyMedium,
                color = AppCustomTheme.colorScheme.secondaryLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
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