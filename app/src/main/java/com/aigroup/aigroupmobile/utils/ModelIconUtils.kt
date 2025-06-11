package com.aigroup.aigroupmobile.utils

import androidx.annotation.DrawableRes
import com.aigroup.aigroupmobile.R

/**
 * Utility functions for getting model-specific icons
 */
object ModelIconUtils {
    
    /**
     * Get the icon resource ID for a specific custom model
     * @param modelId The ID of the model
     * @return The drawable resource ID, or null if no specific icon is found
     */
    @DrawableRes
    fun getCustomModelIcon(modelId: String): Int? {
        return when (modelId) {
            "Enterprise Insight Pro" -> R.drawable.ic_enterprise_insight_pro_icon
            "FinWise Sprite" -> R.drawable.ic_finwise_sprite_icon
            "ProfitPulse" -> R.drawable.ic_profitpulse_icon
            else -> null
        }
    }
    
    /**
     * Get the icon resource ID for a custom model, falling back to default if not found
     * @param modelId The ID of the model
     * @param defaultIcon The default icon to use if no specific icon is found
     * @return The drawable resource ID
     */
    @DrawableRes
    fun getCustomModelIconWithFallback(modelId: String, @DrawableRes defaultIcon: Int = R.drawable.ic_custom_bot_icon): Int {
        return getCustomModelIcon(modelId) ?: defaultIcon
    }
}