package hacklanta.moodmap.ui.screens

import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import hacklanta.moodmap.BuildConfig
import hacklanta.moodmap.viewmodel.AuthViewModel
import hacklanta.moodmap.viewmodel.LoginState

@Composable
fun SignInScreen(
    navController: NavController,
    viewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val loginState by viewModel.loginState.collectAsState()
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (!idToken.isNullOrBlank()) {
                viewModel.signInWithGoogle(idToken)
            } else {
                viewModel.onGoogleSignInPresentationFailure(
                    "No ID token from Google. Confirm GOOGLE_WEB_CLIENT_ID is your OAuth Web client ID in local.properties."
                )
            }
        } catch (e: ApiException) {
            if (e.statusCode == GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                return@rememberLauncherForActivityResult
            }
            viewModel.onGoogleSignInPresentationFailure(
                e.message ?: "Google Sign-In failed (${e.statusCode})"
            )
        }
    }

    LaunchedEffect(loginState) {
        if (loginState is LoginState.Success) {
            navController.navigate("main") {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Sign In", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.signInWithEmail(email, password) },
            enabled = loginState !is LoginState.Loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (loginState is LoginState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Login")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        when {
            webClientId.isBlank() -> {
                Text(
                    text = "Google Sign-In: add GOOGLE_WEB_CLIENT_ID (Web client ID) to local.properties.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            activity == null -> {
                Text(
                    text = "Google Sign-In requires an activity context.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
            else -> {
                OutlinedButton(
                    onClick = {
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestEmail()
                            .requestIdToken(webClientId)
                            .build()
                        val client = GoogleSignIn.getClient(activity, gso)
                        googleLauncher.launch(client.signInIntent)
                    },
                    enabled = loginState !is LoginState.Loading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Continue with Google")
                }
            }
        }

        when (val s = loginState) {
            is LoginState.Error -> {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = s.message,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp
                )
            }
            else -> Unit
        }

        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = { navController.navigate("register") }) {
            Text("New here? Create an account")
        }
    }
}
