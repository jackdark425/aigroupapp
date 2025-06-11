package com.aigroup.aigroupmobile.ui.pages.settings

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.appStringResource
import com.aigroup.aigroupmobile.connect.chat.CustomChatServiceProvider
import com.aigroup.aigroupmobile.data.models.CustomLLMModel
import com.aigroup.aigroupmobile.ui.components.ConfirmDialog
import com.aigroup.aigroupmobile.ui.components.SectionListItem
import com.aigroup.aigroupmobile.ui.components.SectionListSection
import com.aigroup.aigroupmobile.ui.theme.AppCustomTheme
import com.aigroup.aigroupmobile.viewmodels.CustomLLMProviderViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomLLMModelEditPage(
    onBack: () -> Unit,
    provider: CustomChatServiceProvider,
    model: CustomLLMModel? = null,
    viewModel: CustomLLMProviderViewModel = hiltViewModel()
) {
    val isEditMode = model != null
    val title = if (isEditMode) stringResource(R.string.label_custom_llm_model_edit) else stringResource(R.string.label_custom_llm_model_add)
    
    // Form state
    var modelId by remember { mutableStateOf(model?.id ?: "") }
    var modelName by remember { mutableStateOf(model?.name ?: "") }
    var contextSize by remember { mutableStateOf(model?.contextSize?.toString() ?: "4096") }
    var supportsImage by remember { mutableStateOf(model?.supportsImage ?: false) }
    var supportsVideo by remember { mutableStateOf(model?.supportsVideo ?: false) }
    var supportsStreaming by remember { mutableStateOf(model?.supportsStreaming ?: true) }
    
    // Confirmation dialog state
    var showDiscardChangesDialog by remember { mutableStateOf(false) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    
    // Observe operation result
    val operationResult by viewModel.operationResult.collectAsState()
    
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
    
    // Set current provider
    LaunchedEffect(provider) {
        viewModel.setCurrentProvider(provider)
    }
    
    // Focus on model ID field when page opens
    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (e: Exception) {
            // Ignore focus request exception
        }
    }
    
    // Confirmation dialog for discarding changes
    if (showDiscardChangesDialog) {
        ConfirmDialog(
            title = stringResource(R.string.label_discard_changes),
            message = stringResource(R.string.label_discard_changes_confirm),
            confirmText = stringResource(R.string.label_discard),
            onConfirm = {
                showDiscardChangesDialog = false
                onBack()
            },
            onDismiss = {
                showDiscardChangesDialog = false
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(title, fontWeight = FontWeight.SemiBold)
                        Text(
                            provider.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppCustomTheme.colorScheme.secondaryLabel
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showDiscardChangesDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "", Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
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
                    sectionHeader = stringResource(R.string.label_custom_llm_model_information),
                    showTitle = true
                ) {
                    // Model ID
                    OutlinedTextField(
                        value = modelId,
                        onValueChange = { modelId = it },
                        label = { Text(stringResource(R.string.label_custom_llm_model_id)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .focusRequester(focusRequester),
                        singleLine = true,
                        enabled = !isEditMode, // Can't change ID in edit mode
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            focusedIndicatorColor = AppCustomTheme.colorScheme.primaryAction,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    )
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.surfaceDim
                    )
                    
                    // Model Name
                    OutlinedTextField(
                        value = modelName,
                        onValueChange = { modelName = it },
                        label = { Text(stringResource(R.string.label_custom_llm_model_name)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
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
                    
                    // Context Size
                    OutlinedTextField(
                        value = contextSize,
                        onValueChange = { contextSize = it },
                        label = { Text(stringResource(R.string.label_custom_llm_model_context_size)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            focusedIndicatorColor = AppCustomTheme.colorScheme.primaryAction,
                        )
                    )
                }
            }
            
            item {
                SectionListSection(
                    sectionHeader = stringResource(R.string.label_custom_llm_model_capabilities),
                    showTitle = true
                ) {
                    // Supports Image switch
                    SectionListItem(
                        icon = ImageVector.vectorResource(R.drawable.ic_image_icon),
                        title = stringResource(R.string.label_custom_llm_model_supports_images),
                        description = stringResource(R.string.label_custom_llm_model_supports_images_desc),
                        noIconBg = true,
                        iconModifier = Modifier.size(20.dp),
                        trailingContent = {
                            Switch(
                                checked = supportsImage,
                                onCheckedChange = { supportsImage = it }
                            )
                        }
                    )
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.surfaceDim
                    )
                    
                    // Supports Video switch
                    SectionListItem(
                        icon = ImageVector.vectorResource(R.drawable.ic_video_icon),
                        title = stringResource(R.string.label_custom_llm_model_supports_video),
                        description = stringResource(R.string.label_custom_llm_model_supports_video_desc),
                        noIconBg = true,
                        iconModifier = Modifier.size(20.dp),
                        trailingContent = {
                            Switch(
                                checked = supportsVideo,
                                onCheckedChange = { supportsVideo = it }
                            )
                        }
                    )
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.surfaceDim
                    )
                    
                    // Supports Streaming switch
                    SectionListItem(
                        icon = ImageVector.vectorResource(R.drawable.ic_streaming_icon),
                        title = stringResource(R.string.label_custom_llm_model_supports_streaming),
                        description = stringResource(R.string.label_custom_llm_model_supports_streaming_desc),
                        noIconBg = true,
                        iconModifier = Modifier.size(20.dp),
                        trailingContent = {
                            Switch(
                                checked = supportsStreaming,
                                onCheckedChange = { supportsStreaming = it }
                            )
                        }
                    )
                }
            }
            
            // Save button
            item {
                Button(
                    onClick = {
                        if (modelId.isBlank()) {
                            val errorMessage = appStringResource(R.string.label_custom_llm_model_id_empty)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(errorMessage)
                            }
                            return@Button
                        }
                        
                        val contextSizeInt = contextSize.toIntOrNull() ?: 4096
                        
                        viewModel.addModelToCurrentProvider(
                            modelId = modelId,
                            modelName = modelName,
                            contextSize = contextSizeInt,
                            supportsImage = supportsImage,
                            supportsVideo = supportsVideo,
                            supportsStreaming = supportsStreaming
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppCustomTheme.colorScheme.primaryAction,
                        contentColor = AppCustomTheme.colorScheme.onPrimaryAction
                    )
                ) {
                    Text(if (isEditMode) stringResource(R.string.label_custom_llm_model_update) else stringResource(R.string.label_custom_llm_model_add))
                }
            }
            
            // Spacer at the bottom
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
} 