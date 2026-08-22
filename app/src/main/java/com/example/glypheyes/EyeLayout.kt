package com.example.glypheyes

import android.util.Log
import com.nothing.ketchum.Glyph

/**
 * マトリクス長（デバイス）に応じた目のジオメトリ
 * Phone (3) は 25×25、Phone (4a) Pro は 13×13 と解像度が違うため、
 * 比例計算ではなく各デバイスでチューニングしたプリセットを用意する
 */
data class EyeLayout(
    /** マトリクスの一辺（ビットマップも size×size） */
    val size: Int,
    /** 左目の中心座標 */
    val leftCenter: Pair<Int, Int>,
    /** 右目の中心座標 */
    val rightCenter: Pair<Int, Int>,
    /** 目の横半径 */
    val radiusX: Int,
    /** 目の縦半径（縦長の楕円） */
    val radiusY: Int,
    /** 瞳の半径 */
    val pupilRadius: Int,
    /** 瞳が移動できる最大距離 */
    val pupilRange: Int,
    /** 瞳の移動制限（右/左/下/上） */
    val limitRight: Int,
    val limitLeft: Int,
    val limitDown: Int,
    val limitUp: Int,
    /** 目の輪郭線の太さ */
    val outlineWidth: Int
) {
    companion object {
        private const val TAG = "EyeLayout"

        /** Phone (3) (DEVICE_23112): 25×25 マトリクス用 */
        val PHONE_3 = EyeLayout(
            size = 25,
            leftCenter = 8 to 12,
            rightCenter = 17 to 12,
            radiusX = 4,
            radiusY = 6,
            pupilRadius = 2,
            pupilRange = 4,
            limitRight = 4,
            limitLeft = -2,
            limitDown = 2,
            limitUp = -5,
            outlineWidth = 1
        )

        /** Phone (4a) Pro (DEVICE_25111p): 13×13 マトリクス用 */
        val PHONE_4A_PRO = EyeLayout(
            size = 13,
            leftCenter = 4 to 6,
            rightCenter = 9 to 6,
            radiusX = 2,
            radiusY = 3,
            pupilRadius = 1,
            pupilRange = 2,
            limitRight = 2,
            limitLeft = -1,
            limitDown = 1,
            limitUp = -3,
            outlineWidth = 1
        )

        /** マトリクス長からレイアウトを選択（未知の長さは Phone (3) にフォールバック） */
        fun forMatrixLength(length: Int): EyeLayout = when (length) {
            Glyph.DEVICE_25111p_MATRIX_LENGTH -> PHONE_4A_PRO
            else -> {
                if (length != Glyph.DEVICE_23112_MATRIX_LENGTH) {
                    Log.w(TAG, "Unknown matrix length: $length, falling back to Phone (3) layout")
                }
                PHONE_3
            }
        }
    }
}
