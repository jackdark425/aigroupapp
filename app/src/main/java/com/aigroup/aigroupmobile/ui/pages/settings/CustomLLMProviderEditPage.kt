package com.aigroup.aigroupmobile.ui.pages.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.connect.chat.CustomChatServiceProvider
import com.aigroup.aigroupmobile.data.models.CustomLLMModel
import com.aigroup.aigroupmobile.data.models.LoadingStatus
import com.aigroup.aigroupmobile.ui.components.ConfirmDialog
import com.aigroup.aigroupmobile.ui.components.SectionListItem
import com.aigroup.aigroupmobile.ui.components.SectionListSection
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme
import com.aigroup.aigroupmobile.viewmodels.CustomLLMProviderViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomLLMProviderEditPage(
    onBack: () -> Unit,
    provider: CustomChatServiceProvider? = null,
    onNavigateToAddModel: (CustomChatServiceProvider) -> Unit,
    onNavigateToEditModel: (CustomChatServiceProvider, CustomLLMModel) -> Unit,
    viewModel: CustomLLMProviderViewModel = hiltViewModel()
) {
    val isEditMode = provider != null
    val title = if (isEditMode) stringResource(R.string.label_custom_llm_provider_edit) else stringResource(R.string.label_custom_llm_provider_add)
    
    // Form state
    var name by remember { mutableStateOf(provider?.displayName ?: "") }
    var apiBaseUrl by remember { mutableStateOf(provider?.apiBase ?: "https://") }
    var apiKey by remember { mutableStateOf(provider?.apiKey ?: "") }
    var requiresApiKey by remember { mutableStateOf(provider?.requiresApiKey ?: true) }
    var isEnabled by remember { mutableStateOf(provider?.isEnabled ?: true) }
    var showApiKey by remember { mutableStateOf(false) }
    
    // Delete confirmation dialog state
    var showDeleteProviderDialog by remember { mutableStateOf(false) }
    var modelToDelete by remember { mutableStateOf<CustomLLMModel?>(null) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    
    // Observe operation result
    val operationResult by viewModel.operationResult.collectAsState()
    
    // Observe models if in edit mode
    val modelsState by viewModel.currentProviderModels.collectAsState()
    
    LaunchedEffect(operationResult) {
        operationResult?.let {
            when (it) {
                is CustomLLMProviderViewModel.OperationResult.Success -> {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(it.message)
                    }
                    if (!isEditMode) {
                        // Navigate back after successful creation
                        onBack()
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
    
    // Set current provider for editing
    LaunchedEffect(provider) {
        if (isEditMode) {
            viewModel.setCurrentProvider(provider)
        }
    }
    
    // Focus on name field when page opens
    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (e: Exception) {
            // Ignore focus request exception
        }
    }
    
    // Confirmation dialog for deleting a provider
    if (showDeleteProviderDialog) {
        ConfirmDialog(
            title = stringResource(R.string.label_custom_llm_provider_delete),
            message = stringResource(R.string.label_custom_llm_provider_delete_confirm),
            confirmText = stringResource(R.string.label_custom_llm_provider_delete),
            onConfirm = {
                provider?.let {
                    viewModel.deleteProvider(it.provider.id)
                    onBack()
                }
                showDeleteProviderDialog = false
            },
            onDismiss = {
                showDeleteProviderDialog = false
            }
        )
    }
    
    // Confirmation dialog for deleting a model
    if (modelToDelete != null) {
        ConfirmDialog(
            title = stringResource(R.string.label_custom_llm_model_delete),
            message = stringResource(R.string.label_custom_llm_model_delete_confirm),
            confirmText = stringResource(R.string.label_custom_llm_model_delete),
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
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "", Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                actions = {
                    if (isEditMode) {
                        IconButton(onClick = { showDeleteProviderDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.label_custom_llm_provider_delete),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (isEditMode) {
                FloatingActionButton(
                    onClick = { provider?.let { onNavigateToAddModel(it) } },
                    containerColor = AppCustomTheme.colorScheme.primaryAction,
                    contentColor = AppCustomTheme.colorScheme.onPrimaryAction
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.label_custom_llm_model_add))
                }
            }
        },
        containerColor = AppCustomTheme.colorScheme.groupedBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding,
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SectionListSection(
                    sectionHeader = stringResource(R.string.label_custom_llm_provider_details),
                    showTitle = true
                ) {
                    // Provider name
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.label_custom_llm_provider_name)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .focusRequester(focusRequester),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            focusedIndicatorColor = AppCustomTheme.colorScheme.primaryAction,
                        )
                    )
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.surfaceDim
                    )
                    
                    // API Base URL
                    OutlinedTextField(
                        value = apiBaseUrl,
                        onValueChange = { apiBaseUrl = it },
                        label = { Text(stringResource(R.string.label_custom_llm_provider_api_base)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            focusedIndicatorColor = AppCustomTheme.colorScheme.primaryAction,
                        )
                    )
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.surfaceDim
                    )
                    
                    // Requires API Key switch
                    SectionListItem(
                        icon = ImageVector.vectorResource(R.drawable.ic_lock_icon),
                        title = stringResource(R.string.label_custom_llm_provider_requires_api_key),
                        noIconBg = true,
                        iconModifier = Modifier.size(20.dp),
                        trailingContent = {
                            Switch(
                                checked = requiresApiKey,
                                onCheckedChange = { requiresApiKey = it }
                            )
                        }
                    )
                    
                    // API Key (only shown if requires API key)
                    if (requiresApiKey) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.surfaceDim
                        )
                        
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            label = { Text(stringResource(R.string.label_custom_llm_provider_api_key)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            singleLine = true,
                            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { showApiKey = !showApiKey }) {
                                    Icon(
                                        imageVector = if (showApiKey) Icons.Default.Lock else Icons.Default.Info,
                                        contentDescription = if (showApiKey) "Hide API Key" else "Show API Key"
                                    )
                                }
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                focusedIndicatorColor = AppCustomTheme.colorScheme.primaryAction,
                            )
                        )
                    }
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.surfaceDim
                    )
                    
                    // Enabled switch
                    SectionListItem(
                        icon = ImageVector.vectorResource(R.drawable.ic_power_icon),
                        title = stringResource(R.string.label_custom_llm_provider_enabled),
                        noIconBg = true,
                        iconModifier = Modifier.size(20.dp),
                        trailingContent = {
                            Switch(
                                checked = isEnabled,
                                onCheckedChange = { isEnabled = it }
                            )
                        }
                    )
                }
            }
            
            // Save button
            item {
                Button(
                    onClick = {
                        if (isEditMode) {
                            provider?.let {
                                viewModel.updateProvider(
                                    id = it.provider.id,
                                    name = name,
                                    apiBaseUrl = apiBaseUrl,
                                    apiKey = apiKey,
                                    isEnabled = isEnabled,
                                    requiresApiKey = requiresApiKey
                                )
                            }
                        } else {
                            viewModel.createProvider(
                                name = name,
                                apiBaseUrl = apiBaseUrl,
                                apiKey = apiKey,
                                isEnabled = isEnabled,
                                requiresApiKey = requiresApiKey
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppCustomTheme.colorScheme.primaryAction,
                        contentColor = AppCustomTheme.colorScheme.onPrimaryAction
                    )
                ) {
                    Text(if (isEditMode) stringResource(R.string.label_custom_llm_provider_update) else stringResource(R.string.label_custom_llm_provider_add))
                }
            }
            
            // Models section (only in edit mode)
            if (isEditMode) {
                item {
                    SectionListSection(
                        sectionHeader = stringResource(R.string.label_custom_llm_models),
                        showTitle = true
                    ) {
                        when (val models = modelsState) {
                            is LoadingStatus.Loading -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = AppCustomTheme.colorScheme.primaryAction
                                    )
                                }
                            }
                            
                            is LoadingStatus.Error -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
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
                                    SectionListItem(
                                        icon = ImageVector.vectorResource(R.drawable.ic_add_docs),
                                        title = stringResource(R.string.label_custom_llm_model_add_first),
                                        description = stringResource(R.string.label_custom_llm_model_add_first_desc),
                                        noIconBg = true,
                                        iconModifier = Modifier.size(20.dp),
                                        onClick = { provider?.let { onNavigateToAddModel(it) } }
                                    )
                                } else {
                                    models.data.forEachIndexed { index, model ->
                                        ModelListItem(
                                            model = model,
                                            onEdit = { provider?.let { onNavigateToEditModel(it, model) } },
                                            onDelete = { modelToDelete = model }
                                        )
                                        
                                        if (index < models.data.size - 1) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(horizontal = 16.dp),
                                                thickness = 0.5.dp,
                                                color = MaterialTheme.colorScheme.surfaceDim
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Add spacing at the bottom for FAB
            if (isEditMode) {
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun ModelListItem(
    model: CustomLLMModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    SectionListItem(
        icon = ImageVector.vectorResource(R.drawable.ic_model_icon),
        title = model.name,
        description = "ID: ${model.id} • Context: ${model.contextSize} tokens",
        noIconBg = true,
        iconModifier = Modifier.size(20.dp),
        onClick = onEdit,
        trailingContent = {
            Row {
                // Capability indicators
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    if (model.supportsImage) {
                        Box(
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(8.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = MaterialTheme.shapes.small
                                )
                        )
                    }
                    
                    if (model.supportsVideo) {
                        Box(
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(8.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.secondary,
                                    shape = MaterialTheme.shapes.small
                                )
                        )
                    }
                    
                    if (model.supportsStreaming) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.tertiary,
                                    shape = MaterialTheme.shapes.small
                                )
                        )
                    }
                }
                
                // Edit button
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = AppCustomTheme.colorScheme.primaryAction,
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                // Delete button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    )
} 