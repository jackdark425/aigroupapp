package com.aigroup.aigroupmobile.connect.chat

import android.content.Context
import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import kotlinx.parcelize.Parcelize

/**
 * 通用服务提供商接口，作为 ChatServiceProvider 和 CustomChatServiceProvider 的共同祖先
 */
@Parcelize
sealed interface ServiceProvider : Parcelable {
    /**
     * 提供商唯一标识符
     */
    val id: String
    
    /**
     * API 基础 URL
     */
    val apiBase: String
    
    /**
     * 提供商描述
     */
    @get:Composable
    val description: String
    
    /**
     * 提供商显示名称
     */
    val displayName: String
    
    /**
     * 提供商图标资源 ID
     */
    @get:DrawableRes
    val logoIconId: Int
    
    /**
     * 提供商背景颜色
     */
    val backColor: Color?
    
    /**
     * 提供商是否启用
     */
    val isEnabled: Boolean
} 