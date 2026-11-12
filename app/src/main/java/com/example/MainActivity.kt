package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.example.data.DojoRepository
import com.example.i18n.AppLanguage
import com.example.ui.AuthScreen
import com.example.ui.MainAppScreen
import com.example.ui.theme.DojoFlowTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DojoFlowTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    val context = LocalContext.current
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var currentLanguage by remember { mutableStateOf(AppLanguage.ENGLISH) }
    var isSkippedPreview by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    if (currentUser == null && !isSkippedPreview) {
        AuthScreen(
            currentLanguage = currentLanguage,
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            },
            onSkipSignIn = {
                isSkippedPreview = true
            }
        )
    } else {
        val effectiveUserId = currentUser?.uid ?: DojoRepository.PREVIEW_USER_ID
        val repository = remember(context) { DojoRepository(context) }
        val credentialManager = remember(context) { CredentialManager.create(context) }
        val scope = rememberCoroutineScope()

        key(effectiveUserId) {
            MainAppScreen(
                currentUserId = effectiveUserId,
                repository = repository,
                currentLanguage = currentLanguage,
                onLanguageChange = { currentLanguage = it },
                onSignOut = {
                    scope.launch {
                        withContext(NonCancellable) {
                            try {
                                credentialManager.clearCredentialState(ClearCredentialStateRequest())
                            } catch (e: Exception) {
                                Log.w("Auth", "Failed to clear credential state: ${e.message}")
                            } finally {
                                Firebase.auth.signOut()
                                currentUser = null
                                isSkippedPreview = false
                            }
                        }
                    }
                }
            )
        }
    }
}
