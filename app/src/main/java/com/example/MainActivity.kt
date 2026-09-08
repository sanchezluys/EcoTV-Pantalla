package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.manager.BrightnessManager
import com.example.manager.EcoState
import com.example.service.ScreenOffOverlayService
import com.example.ui.ScreenOffActivity
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EcoFocusGlow
import com.example.ui.theme.EcoPrimary
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.WarningAmber
import java.util.Calendar

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkBackground),
                    containerColor = DarkBackground
                ) { innerPadding ->
                    EcoTVScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun EcoTVScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Observe active overlay state
    val isScreenOffActive by EcoState.isScreenOffActive.collectAsState()

    // Permissions state, refreshed on lifecycle resume
    var hasOverlayPermission by remember { mutableStateOf(BrightnessManager.canDrawOverlays(context)) }
    var hasWriteSettingsPermission by remember { mutableStateOf(BrightnessManager.canModifySystemSettings(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = BrightnessManager.canDrawOverlays(context)
                hasWriteSettingsPermission = BrightnessManager.canModifySystemSettings(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Remote control focus management: primary button is focused by default
    val primaryFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        // Automatically give focus to the primary "Apagar pantalla" action for instant D-pad selection
        try {
            primaryFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    // Dynamic current year and version
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val appVersion = BuildConfig.VERSION_NAME
    val developerEmail = "sanchezluys@gmail.com"

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 900.dp)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // App Header
            HeaderSection()

            // Brief Description
            Text(
                text = stringResource(R.string.app_description),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 17.sp,
                    lineHeight = 24.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(horizontal = 16.dp)
            )

            // Primary D-pad Action: "Apagar pantalla"
            DpadActionButton(
                focusRequester = primaryFocusRequester,
                isScreenOff = isScreenOffActive,
                onClick = {
                    if (hasOverlayPermission) {
                        // Launch system-wide overlay covering YouTube/Spotify
                        ScreenOffOverlayService.startOverlay(context)
                    } else {
                        // Direct in-app blackout fallback with remote detection & brightness reduction
                        ScreenOffActivity.start(context)
                    }
                }
            )

            // Technology explanation: OLED vs LCD/LED
            EnergySavingsInfoCard()

            // Permission status cards (if any permission is missing on Google TV)
            if (!hasOverlayPermission || !hasWriteSettingsPermission) {
                PermissionsSection(
                    hasOverlayPermission = hasOverlayPermission,
                    hasWriteSettingsPermission = hasWriteSettingsPermission,
                    onGrantOverlay = {
                        try {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            val fallback = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                            context.startActivity(fallback)
                        }
                    },
                    onGrantWriteSettings = {
                        try {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            val fallback = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                            context.startActivity(fallback)
                        }
                    }
                )
            } else {
                ReadyBadge()
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Footer: Developer name, version, and dynamic year
            FooterSection(
                developerEmail = developerEmail,
                version = appVersion,
                year = currentYear
            )
        }
    }
}

@Composable
private fun HeaderSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(EcoPrimary.copy(alpha = 0.25f), Color.Transparent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Tv,
                    contentDescription = null,
                    tint = EcoPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 34.sp,
                    color = TextPrimary
                )
            )
        }

        Text(
            text = stringResource(R.string.app_subtitle),
            style = MaterialTheme.typography.titleMedium.copy(
                color = EcoPrimary,
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp
            )
        )
    }
}

/**
 * Large dedicated button with TV D-pad focus highlight.
 */
@Composable
fun DpadActionButton(
    focusRequester: FocusRequester,
    isScreenOff: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1.0f,
        animationSpec = tween(durationMillis = 150),
        label = "scale"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) EcoFocusGlow else EcoPrimary.copy(alpha = 0.4f),
        animationSpec = tween(durationMillis = 150),
        label = "border"
    )

    Card(
        modifier = modifier
            .fillMaxWidth(0.85f)
            .scale(scale)
            .focusRequester(focusRequester)
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag("btn_turn_off_screen")
            .shadow(
                elevation = if (isFocused) 20.dp else 6.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = EcoPrimary
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) DarkSurfaceElevated else DarkSurface
        ),
        border = BorderStroke(if (isFocused) 3.5.dp else 1.5.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        if (isFocused) EcoPrimary else EcoPrimary.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = stringResource(R.string.btn_turn_off_screen),
                    tint = if (isFocused) Color.Black else EcoPrimary,
                    modifier = Modifier.size(42.dp)
                )
            }

            Text(
                text = stringResource(R.string.btn_turn_off_screen),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isFocused) EcoPrimary else TextPrimary
                ),
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(R.string.btn_reactivate_hint),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

/**
 * Explains how real energy savings occur on OLED and LCD/LED panels.
 */
@Composable
private fun EnergySavingsInfoCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .testTag("energy_savings_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkSurfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = EcoPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Ahorro real según tu pantalla",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        fontSize = 18.sp
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TechBenefitItem(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.oled_benefit_title),
                    description = stringResource(R.string.oled_benefit_desc),
                    accentColor = EcoPrimary
                )
                TechBenefitItem(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.led_benefit_title),
                    description = stringResource(R.string.led_benefit_desc),
                    accentColor = InfoBlue
                )
            }
        }
    }
}

@Composable
private fun TechBenefitItem(
    title: String,
    description: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = DarkSurfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    fontSize = 15.sp
                )
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            )
        }
    }
}

@Composable
private fun ReadyBadge() {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SuccessGreen.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.4f)),
        modifier = Modifier.padding(top = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = SuccessGreen,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Permisos concedidos • Listo para cubrir apps de música y video",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = SuccessGreen,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            )
        }
    }
}

@Composable
private fun PermissionsSection(
    hasOverlayPermission: Boolean,
    hasWriteSettingsPermission: Boolean,
    onGrantOverlay: () -> Unit,
    onGrantWriteSettings: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .testTag("permissions_section"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = WarningAmber,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Configuración para Smart TV",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 18.sp
                    )
                )
            }

            if (!hasOverlayPermission) {
                PermissionActionRow(
                    title = stringResource(R.string.perm_overlay_title),
                    description = stringResource(R.string.perm_overlay_needed),
                    buttonText = stringResource(R.string.btn_grant_overlay),
                    onAction = onGrantOverlay,
                    testTag = "btn_grant_overlay"
                )
            }

            if (!hasWriteSettingsPermission) {
                PermissionActionRow(
                    title = stringResource(R.string.perm_brightness_title),
                    description = stringResource(R.string.perm_brightness_needed),
                    buttonText = stringResource(R.string.btn_grant_brightness),
                    onAction = onGrantWriteSettings,
                    testTag = "btn_grant_brightness"
                )
            }
        }
    }
}

@Composable
private fun PermissionActionRow(
    title: String,
    description: String,
    buttonText: String,
    onAction: () -> Unit,
    testTag: String
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceVariant)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    fontSize = 15.sp
                )
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Surface(
            modifier = Modifier
                .focusable(interactionSource = interactionSource)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onAction
                )
                .testTag(testTag),
            shape = RoundedCornerShape(8.dp),
            color = if (isFocused) EcoPrimary else DarkSurfaceElevated,
            border = BorderStroke(
                if (isFocused) 2.dp else 1.dp,
                if (isFocused) EcoFocusGlow else DarkSurfaceVariant
            )
        ) {
            Text(
                text = buttonText,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge.copy(
                    color = if (isFocused) Color.Black else EcoPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            )
        }
    }
}

/**
 * Bottom section displaying dynamic year, app version, and developer information.
 */
@Composable
private fun FooterSection(
    developerEmail: String,
    version: String,
    year: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(R.string.footer_developer),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 13.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
        )
        Text(
            text = stringResource(R.string.footer_version, version, year),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                color = TextTertiary
            )
        )
    }
}
