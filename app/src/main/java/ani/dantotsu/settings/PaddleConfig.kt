package ani.dantotsu.settings

/**
 * Paddle Payment Configuration
 * 
 * Using Product IDs for direct checkout links
 */
object PaddleConfig {
    
    // Set to false for production
    const val SANDBOX_MODE = false
    
    // Product IDs from Paddle Dashboard (Catalog → Products)
    object ProductIds {
        const val SMALL_TEA = "pro_01kfmfbvpdct304d8zaq7ktres"    // $1
        const val MEDIUM_TEA = "pro_01kfmmxshhpx7cjkjq7t04e50h"  // $3
        const val LARGE_TEA = "pro_01kfmmzerq3tmpdsppt6w5b9b2"    // $5
        const val CUSTOM = "pro_01kfmn17m83qbehdr3g27j7rty"       // Custom
    }
    
    /**
     * Generates a Paddle product checkout URL
     */
    fun getCheckoutUrl(productId: String): String {
        val baseUrl = if (SANDBOX_MODE) {
            "https://sandbox-buy.paddle.com/product"
        } else {
            "https://buy.paddle.com/product"
        }
        return "$baseUrl/$productId"
    }
    
    /**
     * For custom amounts
     */
    fun getCustomAmountUrl(): String {
        return getCheckoutUrl(ProductIds.CUSTOM)
    }
}
