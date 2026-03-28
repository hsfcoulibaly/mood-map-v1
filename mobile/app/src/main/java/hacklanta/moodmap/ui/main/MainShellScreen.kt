package hacklanta.moodmap.ui.main

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.lifecycle.ViewModelProvider
import hacklanta.moodmap.ui.screens.ChatScreen
import hacklanta.moodmap.viewmodel.MapViewModel

private enum class MainTab(val label: String) {
    Map("Map"),
    Insights("Insights"),
    Chat("AI chat"),
    Journal("Journal"),
    Recovery("Stories"),
    Menu("Menu"),
}

@Composable
fun MainShellScreen(navController: NavController) {
    val app = LocalContext.current.applicationContext as Application
    val mapVm: MapViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(app),
    )
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = {
                            Icon(
                                when (item) {
                                    MainTab.Map -> Icons.Default.Place
                                    MainTab.Insights -> Icons.Default.Star
                                    MainTab.Chat -> Icons.Default.Person
                                    MainTab.Journal -> Icons.Default.Edit
                                    MainTab.Recovery -> Icons.Default.Favorite
                                    MainTab.Menu -> Icons.Default.MoreVert
                                },
                                contentDescription = item.label,
                            )
                        },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            when (MainTab.entries[tab]) {
                MainTab.Map -> MapTabScreen(mapVm, Modifier.fillMaxSize())
                MainTab.Insights -> InsightsTabScreen(mapVm)
                MainTab.Chat -> ChatScreen(navController, Modifier.fillMaxSize())
                MainTab.Journal -> JournalTabScreen(mapVm)
                MainTab.Recovery -> RecoveryTabScreen()
                MainTab.Menu -> MenuTabScreen(navController)
            }
            if (MainTab.entries[tab] == MainTab.Map) {
                MoodPickerSheet(mapVm)
                CompanionBottomSheet(
                    pin = mapVm.selectedPin,
                    onDismiss = { mapVm.dismissCompanion() },
                )
            }
        }
    }
}
