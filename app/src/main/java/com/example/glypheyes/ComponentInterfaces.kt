package com.example.glypheyes

/**
 * コンポーネント間の通信インターフェースと型定義
 * 各コンポーネント間の依存関係を明確にし、型安全性を向上
 */

/**
 * エラーハンドリングのコールバック
 * @param component エラーが発生したコンポーネント名
 * @param error エラーの説明
 * @param exception 発生した例外（オプショナル）
 */
typealias ErrorCallback = (component: String, error: String, exception: Exception?) -> Unit

/**
 * コンポーネントの初期化結果
 */
sealed class InitResult {
    object Success : InitResult()
    data class Failure(val reason: String, val exception: Exception? = null) : InitResult()
}

/**
 * コンポーネントの基本インターフェース
 * すべての管理可能なコンポーネントが実装すべき基本機能
 */
interface ManagedComponent {
    /**
     * コンポーネントを初期化して開始
     * @return 初期化結果
     */
    fun start(): InitResult
    
    /**
     * コンポーネントを安全に停止
     */
    fun stop()
    
    /**
     * コンポーネントが現在アクティブかどうか
     */
    fun isActive(): Boolean
}
