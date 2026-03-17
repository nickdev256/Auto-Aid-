package Components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.project.auto_aid.R
import kotlinx.coroutines.delay

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HeroImageSlider() {

    val images = listOf(
        R.drawable.total_1,
        R.drawable.fuel1,
        R.drawable.logo14,
        R.drawable.amb1,
        R.drawable.ambu2,
        R.drawable.shell_2,
        R.drawable.gara1,
        R.drawable.towi1,
        R.drawable.amb1,
        R.drawable.fuel,
        R.drawable.towi1,
        R.drawable.fuel33,

        )

    var index by remember { mutableIntStateOf(0) }

    // Auto-slide every 4 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(4000)
            index = (index + 1) % images.size
        }
    }

    AnimatedContent(
        targetState = index,
        transitionSpec = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(600)
            ) togetherWith
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> -fullWidth },
                        animationSpec = tween(600)
                    )
        },
        label = "HeroImageSlide"
    ) { targetIndex ->
        AsyncImage(
            model = images[targetIndex],
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        )
    }
}