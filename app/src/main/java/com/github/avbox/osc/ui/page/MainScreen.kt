                            .padding(horizontal = 16.dp)
                            .padding(bottom = NavMetrics.MARGIN_DP.dp)
                    } else {
                        Modifier
                            .align(Alignment.CenterStart)
                            .windowInsetsPadding(WindowInsets.systemBars)
                            .padding(vertical = 16.dp)
                            .padding(start = NavMetrics.MARGIN_DP.dp)
                    },
                ) {
                    if (navAxis == NavAxis.Horizontal) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FloatingNavBar(
                                backdrop = liquidBackdrop,
                                axis = navAxis,
                                selectedTabIndex = { pagerState.targetPage },
                                onTabSelected = selectTab,
                                tabs = glassTabs,
                                config = liquidGlassConfig,
                                interactive = { true },
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            NavSearchButton(
                                label = searchActionLabel,
                                glassEnabled = liquidGlassConfig.navbarEnabled,
                                alphaScale = liquidGlassConfig.containerAlphaScale,
                                onSearchClick = openSearch,
                            )
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            FloatingNavBar(
                                backdrop = liquidBackdrop,
                                axis = navAxis,
                                selectedTabIndex = { pagerState.targetPage },
                                onTabSelected = selectTab,
                                tabs = glassTabs,
                                config = liquidGlassConfig,
                                interactive = { true },
                            )
                            NavSearchButton(
                                label = searchActionLabel,
                                glassEnabled = liquidGlassConfig.navbarEnabled,
                                alphaScale = liquidGlassConfig.containerAlphaScale,
                                onSearchClick = openSearch,
                            )
                        }
                    }
                }
            }
            if (surfaceNavVisible && railMode) {
                NavigationRail(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxHeight()
                        .windowInsetsPadding(WindowInsets.systemBars),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    AppTab.entries.forEachIndexed { index, tab ->
                        NavigationRailItem(
                            selected = pagerState.currentPage == index,
                            onClick = { selectTab(index) },
                            icon = {
                                Icon(
                                    painterResource(tab.icon),
                                    contentDescription = stringResource(tab.labelRes),
                                )
                            },
                            label = { Text(stringResource(tab.labelRes)) },
                        )
                    }
                }
            }
            SheetHost(sheetHost)
        }
    }
}

@Composable
private fun NavSearchButton(
    label: String,
    glassEnabled: Boolean,
    alphaScale: Float,
    onSearchClick: () -> Unit,
) {
    val containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(
        alpha = if (glassEnabled) 0.4f * alphaScale else 1f,
    )
    Box(
        modifier = Modifier
            .size(NavMetrics.BAR_CROSS_DP.dp)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(onClick = onSearchClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(R.drawable.ic_tab_search),
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp),
        )
    }
}
