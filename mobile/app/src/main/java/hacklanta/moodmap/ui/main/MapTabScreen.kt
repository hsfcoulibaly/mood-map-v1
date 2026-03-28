package hacklanta.moodmap.ui.main

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import hacklanta.moodmap.BuildConfig
import hacklanta.moodmap.util.CampusUtils
import hacklanta.moodmap.viewmodel.MapViewModel

@Composable
fun MapTabScreen(mapVm: MapViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var hasFineLocation by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasFineLocation = granted }

    LaunchedEffect(Unit) {
        if (!hasFineLocation) {
            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    val gsu = LatLng(CampusUtils.GSU_LAT, CampusUtils.GSU_LNG)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(gsu, 14.5f)
    }

    Box(modifier.fillMaxSize()) {
        if (BuildConfig.MAPS_API_KEY.isBlank()) {
            Text(
                "Add MAPS_API_KEY to local.properties (Google Cloud Maps SDK for Android) to see the campus map.",
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = hasFineLocation),
                uiSettings = MapUiSettings(zoomControlsEnabled = true, mapToolbarEnabled = false),
                onMapLongClick = { latLng ->
                    mapVm.requestDropPin(latLng.latitude, latLng.longitude)
                },
            ) {
                mapVm.pins.forEach { pin ->
                    key(pin.id) {
                        val pos = LatLng(pin.lat, pin.lng)
                        val state = remember { MarkerState(position = pos) }
                        Marker(
                            state = state,
                            title = "${pin.emoji} ${pin.mood}",
                            snippet = pin.timeLabel,
                            onClick = {
                                mapVm.openCompanion(pin)
                                true
                            },
                        )
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp),
            tonalElevation = 2.dp,
            shadowElevation = 2.dp,
        ) {
            AssistChip(
                onClick = {},
                enabled = false,
                label = {
                    Text("Recent (5 min): ${mapVm.recentPinCount()}")
                },
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }

        if (mapVm.stressedClusterNearLibrary()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 72.dp, start = 16.dp, end = 16.dp),
                color = MaterialTheme.colorScheme.errorContainer,
            ) {
                Text(
                    "High stress near library zone — check Insights & Crisis resources in the menu.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}
