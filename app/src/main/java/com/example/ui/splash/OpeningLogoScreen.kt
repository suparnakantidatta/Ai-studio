package com.example.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            BrandSecondary,
            BrandPrimary,
            Color(0xFF090D16)
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
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(110.dp)
          .background(
            Brush.radialGradient(
              colors = listOf(BrandGold, Color(0xFFD97706), Color.Transparent)
            ),
            shape = CircleShape
          )
          .padding(4.dp)
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .fillMaxSize()
            .background(BrandSecondary, shape = CircleShape)
        ) {
          Icon(
            imageVector = Icons.Default.School,
            contentDescription = "Pixel Pathsala Logo",
            tint = BrandGold,
            modifier = Modifier.size(56.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = "PIXEL PATHSALA",
          fontSize = 28.sp,
          fontWeight = FontWeight.ExtraBold,
          letterSpacing = 2.sp,
          color = Color.White
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
          imageVector = Icons.Default.Stars,
          contentDescription = null,
          tint = BrandGold,
          modifier = Modifier.size(20.dp)
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = "Premier Tuition & Coaching ERP",
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = BrandGold,
        letterSpacing = 1.sp,
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
