package com.example.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.delay

@Composable
fun OpeningLogoScreen(
  onSplashFinished: () -> Unit
) {
  val alphaAnim = remember { Animatable(0f) }
  val scaleAnim = remember { Animatable(0.85f) }

  LaunchedEffect(Unit) {
    alphaAnim.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
    )
    scaleAnim.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
    )
    delay(1400)
    onSplashFinished()
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black)
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null
      ) {
        onSplashFinished()
      },
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier
        .padding(24.dp)
        .scale(scaleAnim.value)
        .alpha(alphaAnim.value)
    ) {
      // Opening Logo Graphic (Uploaded Official Brand Image)
      Box(
        modifier = Modifier
          .fillMaxWidth(0.85f)
          .aspectRatio(1f)
          .clip(RoundedCornerShape(24.dp))
          .background(Color.Black),
        contentAlignment = Alignment.Center
      ) {
        Image(
          painter = painterResource(id = R.drawable.ic_pixel_pathsala_logo),
          contentDescription = "Pixel Pathsala Opening Logo",
          contentScale = ContentScale.Fit,
          modifier = Modifier.fillMaxSize()
        )
      }

      Spacer(modifier = Modifier.height(28.dp))

      // Minimal gold progress spinner
      CircularProgressIndicator(
        color = Color(0xFFD4AF37),
        strokeWidth = 2.5.dp,
        modifier = Modifier.size(24.dp)
      )

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = "Loading Coaching Portal...",
        color = Color(0xFFA3A3A3),
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.5.sp
      )
    }
  }
}
