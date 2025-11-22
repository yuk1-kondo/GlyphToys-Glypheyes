package com.example.glypheyes

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.glypheyes.BuildConfig

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GlyphEyesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GlyphEyesScreen(
                        onOpenGlyphSettings = { openGlyphSettings() },
                        onContactDeveloper = { contactDeveloper() }
                    )
                }
            }
        }
    }

    private fun openGlyphSettings() {
        try {
            // Nothing Phone の Glyph 設定画面を開く
            val intent = Intent("android.settings.NOTIFICATION_SETTINGS")
            startActivity(intent)
        } catch (e: Exception) {
            // フォールバック: 通常の設定画面
            val intent = Intent(android.provider.Settings.ACTION_SETTINGS)
            startActivity(intent)
        }
    }

    private fun contactDeveloper() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:yukilab.m@gmail.com")
            putExtra(Intent.EXTRA_SUBJECT, "GlyphEyes アプリについて")
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            // メールアプリがない場合は無視
        }
    }
}

private object NothingPalette {
    val background = Color(0xFF050505)
    val panel = Color(0xFF0F1012)
    val border = Color(0x33FFFFFF)
    val accent = Color(0xFF00F4E6)
    val accentSoft = Color(0x6600F4E6)
    val textPrimary = Color(0xFFF4F4F4)
    val textSecondary = Color(0xFF9EA1A5)
    val grid = Color(0x22FFFFFF)
}

@Composable
fun GlyphEyesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = NothingPalette.accent,
            secondary = NothingPalette.accent,
            background = NothingPalette.background,
            surface = NothingPalette.panel,
            onSurface = NothingPalette.textPrimary,
            onSurfaceVariant = NothingPalette.textSecondary
        ),
        content = content
    )
}

@Composable
fun GlyphEyesScreen(
    onOpenGlyphSettings: () -> Unit,
    onContactDeveloper: () -> Unit
) {
    val scrollState = rememberScrollState()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NothingPalette.background)
    ) {
        NeonGradientOverlay()
        DotGridOverlay()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            HeroSection()

            InfoPanel(title = stringResource(R.string.panel_overview_title)) {
                FeatureGrid()
            }

            InfoPanel(title = stringResource(R.string.panel_setup_title)) {
                Text(
                    text = stringResource(R.string.setup_steps),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = NothingPalette.textSecondary
                )

                OutlinedButton(
                    onClick = onOpenGlyphSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    border = BorderStroke(1.dp, Brush.linearGradient(
                        listOf(NothingPalette.accent, NothingPalette.accentSoft)
                    ))
                ) {
                    Text(
                        text = stringResource(R.string.button_open_glyph_settings),
                        color = NothingPalette.accent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            DeveloperFooter(onContactDeveloper = onContactDeveloper)
        }
    }
}

@Composable
private fun RoundedPanelShape() = RoundedCornerShape(28.dp)

@Composable
private fun HeroSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(NothingPalette.panel.copy(alpha = 0.9f))
                .border(1.dp, NothingPalette.border, RoundedCornerShape(32.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "👀", color = NothingPalette.accent, fontSize = 40.sp)
        }

        Text(
            text = stringResource(R.string.hero_title),
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 4.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Text(
            text = stringResource(R.string.hero_tagline),
            fontSize = 14.sp,
            color = NothingPalette.textSecondary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            letterSpacing = 2.sp
        )

        Box(
            modifier = Modifier
                .height(2.dp)
                .width(120.dp)
                .background(NothingPalette.accent)
        )
    }
}

@Composable
private fun InfoPanel(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedPanelShape())
            .background(NothingPalette.panel.copy(alpha = 0.9f))
            .border(1.dp, NothingPalette.border, RoundedPanelShape())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 4.sp,
            color = NothingPalette.textSecondary
        )
        content()
    }
}

@Composable
private fun FeatureGrid() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        FeatureRow(
            icon = "↔",
            title = stringResource(R.string.feature_tilt_title),
            detail = stringResource(R.string.feature_tilt_desc)
        )
        FeatureRow(
            icon = "🔋",
            title = stringResource(R.string.feature_battery_title),
            detail = stringResource(R.string.feature_battery_desc)
        )
        FeatureRow(
            icon = "◎",
            title = stringResource(R.string.feature_expression_title),
            detail = stringResource(R.string.feature_expression_desc)
        )
        FeatureRow(
            icon = "AOD",
            title = stringResource(R.string.feature_aod_title),
            detail = stringResource(R.string.feature_aod_desc)
        )
    }
}

@Composable
private fun FeatureRow(icon: String, title: String, detail: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(NothingPalette.background)
                .border(1.dp, NothingPalette.border, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, color = NothingPalette.accent, fontSize = 14.sp)
        }
        Column {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = detail,
                fontSize = 13.sp,
                color = NothingPalette.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DeveloperFooter(onContactDeveloper: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedPanelShape())
            .background(NothingPalette.panel.copy(alpha = 0.9f))
            .border(1.dp, NothingPalette.border, RoundedPanelShape())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = stringResource(R.string.footer_creator_label),
            fontSize = 12.sp,
            letterSpacing = 3.sp,
            color = NothingPalette.textSecondary
        )
        Text(text = stringResource(R.string.developer_name), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Button(
            onClick = onContactDeveloper,
            colors = ButtonDefaults.buttonColors(containerColor = NothingPalette.accent, contentColor = NothingPalette.background)
        ) {
            Text(text = stringResource(R.string.footer_contact_button), fontWeight = FontWeight.SemiBold)
        }
        Text(text = stringResource(R.string.footer_email), fontSize = 12.sp, color = NothingPalette.textSecondary)
        Text(
            text = stringResource(R.string.footer_build, BuildConfig.VERSION_NAME),
            fontSize = 12.sp,
            color = NothingPalette.textSecondary,
            letterSpacing = 2.sp
        )
    }
}

@Composable
private fun DotGridOverlay() {
    Canvas(modifier = Modifier.fillMaxSize().alpha(0.35f)) {
        val spacing = 28.dp.toPx()
        val radius = 1.5.dp.toPx()
        var y = 0f
        while (y < size.height) {
            var x = 0f
            while (x < size.width) {
                drawCircle(color = NothingPalette.grid, radius = radius, center = Offset(x, y))
                x += spacing
            }
            y += spacing
        }
    }
}

@Composable
private fun NeonGradientOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(NothingPalette.accentSoft, Color.Transparent),
                    center = Offset.Zero,
                    radius = 600f
                )
            )
    )
}


