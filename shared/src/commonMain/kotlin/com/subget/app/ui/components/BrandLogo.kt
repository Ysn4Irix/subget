package com.subget.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import subget.shared.generated.resources.Res
import subget.shared.generated.resources.brand_logo
import subget.shared.generated.resources.brand_logo_white

@Composable
fun BrandLogo(
    isDark: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = 26.dp
) {
    val res = if (isDark) Res.drawable.brand_logo_white else Res.drawable.brand_logo
    Image(
        painter = painterResource(res),
        contentDescription = "Subget Logo",
        contentScale = ContentScale.Fit,
        modifier = modifier.height(height)
    )
}
