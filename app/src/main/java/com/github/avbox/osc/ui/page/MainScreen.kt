            )

            if (liquidGlassEnabled) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .layerBackdrop(liquidBackdrop),
                ) {
                    if (railMode) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .fillMaxHeight()
                                .width(navBandExtent)
                                .windowInsetsPadding(WindowInsets.systemBars)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.10f),
                                        ),
                                    ),
                                ),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(navBandExtent)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.10f),
                                        ),
                                    ),
                                ),
                        )
                    }
                }
            }
        }
    }
}
