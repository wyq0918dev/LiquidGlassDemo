package com.wyq0918dev.liquidglassdemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.twotone.Home
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.kyant.liquidglass.liquidGlassProvider
import com.kyant.liquidglass.rememberLiquidGlassProviderState
import com.wyq0918dev.liquidglassdemo.ui.theme.LiquidGlassDemoTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LiquidGlassDemoTheme {
                ActivityMain()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityMain() {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(
        state = rememberTopAppBarState()
    )

    val coroutineScope = rememberCoroutineScope()

    val tabs = arrayListOf(
        NavigationItem(
            label = "0",
            icon = Icons.TwoTone.Home,
        ),
        NavigationItem(
            label = "1",
            icon = Icons.TwoTone.Home,
        ),
        NavigationItem(
            label = "2",
            icon = Icons.TwoTone.Home,
        ),
        NavigationItem(
            label = "3",
            icon = Icons.TwoTone.Home,
        ),
    )

    val pagerState = rememberPagerState(
        pageCount = { tabs.size },
        initialPage = 0,
    )

    val targetPage = remember { mutableIntStateOf(value = pagerState.currentPage) }

    LaunchedEffect(key1 = pagerState) {
        snapshotFlow {
            pagerState.currentPage
        }.collectLatest {
            targetPage.intValue = pagerState.currentPage
        }
    }

    val providerState = rememberLiquidGlassProviderState(
        backgroundColor = MaterialTheme.colorScheme.background
    )


    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                modifier = Modifier.fillMaxWidth(),
                title = {
                    Text(text = "Liquid Glass Demo")
                },
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            LiquidGlassNavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                liquidGlassProviderState = providerState,
                tabs = tabs,
                selectedIndexState = targetPage,
                onTabSelected = { index ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(page = index)
                    }
                },
            )
        },
    ) { innerPadding ->
        HorizontalPager(
            modifier = Modifier
                .liquidGlassProvider(state = providerState)
                .background(color = MaterialTheme.colorScheme.background),
            state = pagerState,
            userScrollEnabled = true,
            pageContent = { page ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = when (page) {
                                0 -> Color.Red
                                1 -> Color.Green
                                2 -> Color.Blue
                                3 -> Color.Magenta
                                else -> Color.Black
                            }
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        modifier = Modifier.padding(paddingValues = innerPadding),
                        text = page.toString(),
                    )
                }
            },
        )
    }
}