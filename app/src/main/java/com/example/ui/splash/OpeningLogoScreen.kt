package com.example.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.BrandGold
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandSecondary
import kotlinx.coroutines.delay

@Composable
fun OpeningLogoScreen(
  onSplashFinished: () -> Unit
) {
  val transitionState = remember { MutableTransitionState(false) }
  LaunchedEffect(Unit) {
    transitionState.targetState = true
    delay(1800)
    onSplashFinished()
  }

  val transition = updateTransition(transitionState, label = "SplashTransition")
  val scale by transition.animateFloat(
    transitionSpec = { tween(durationMillis = 1000, easing = FastOutSlowInEasing) },
    label = "Scale"
  ) { state -> if (state) 1f else 0.6f }

  val alpha by transition.animateFloat(
    transitionSpec = { tween(durationMillis = 800) },
    label = "Alpha"
  ) { state -> if (state) 1f else 0f }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .testTag("splash_screen")
      .background(
        Brush.verticalGradient(
          colors = listOf(
            Color(0xFF000000),
            BrandSecondary,
            Color(0xFF000000)
          )
        )
      ),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier
        .scale(scale)
        .alpha(alpha)
        .padding(24.dp)
    ) {
      Image(
        painter = painterResource(id = R.drawable.pixel_logo),
        contentDescription = "Pixel Pathsala Logo",
        contentScale = ContentScale.Fit,
        modifier = Modifier
          .fillMaxWidth(0.9f)
          .height(180.dp)
          .clip(RoundedCornerShape(16.dp))
      )

      Spacer(modifier = Modifier.height(20.dp))

      Text(
        text = "Premier Tuition & ERP System",
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = BrandGold,
        letterSpacing = 1.2.sp,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = "Raina • Purba Bardhaman • West Bengal",
        fontSize = 12.sp,
        color = Color(0xFF94A3B8),
        textAlign = TextAlign.Center
      )
    }
  }
}
