package com.rehanu04.resumematchv2.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rehanu04.resumematchv2.data.UserProfile
import com.rehanu04.resumematchv2.data.UserProfileStore
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import kotlin.math.min

private val ROLE_SUGGESTIONS = listOf(
    "Software Engineer (Backend)", "Backend Engineer (Python/FastAPI)", "API Engineer (FastAPI)",
    "Python Backend Engineer", "Software Engineer – Platform", "AI Engineer (Backend)",
    "Machine Learning Engineer", "Android Engineer (Kotlin)", "Full Stack Engineer"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    userProfileStore: UserProfileStore,
    onBack: () -> Unit,
    onGoMasterVault: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val userProfile by userProfileStore.userProfileFlow.collectAsState(initial = UserProfile())

    var firstName by remember(userProfile) { mutableStateOf(userProfile.firstName) }
    var lastName by remember(userProfile) { mutableStateOf(userProfile.lastName) }
    var targetRole by remember(userProfile) { mutableStateOf(userProfile.targetRole) }
    var location by remember(userProfile) { mutableStateOf(userProfile.location) }
    var email by remember(userProfile) { mutableStateOf(userProfile.email) }
    var phone by remember(userProfile) { mutableStateOf(userProfile.phone) }
    var linkedin by remember(userProfile) { mutableStateOf(userProfile.linkedin) }
    var github by remember(userProfile) { mutableStateOf(userProfile.github) }
    var portfolio by remember(userProfile) { mutableStateOf(userProfile.portfolio) }
    var summary by remember(userProfile) { mutableStateOf(userProfile.summary) }
    var profileImageB64 by remember(userProfile) { mutableStateOf(userProfile.profileImageB64) }
    var profileImageName by remember(userProfile) { mutableStateOf(userProfile.profileImageName) }

    var roleFocused by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val encoded = readImageAsBase64(context, uri)
            if (encoded != null) {
                profileImageB64 = encoded.first
                profileImageName = encoded.second
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My AI Profile") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        userProfileStore.saveUserProfile(
                            userProfile.copy(
                                firstName = firstName, lastName = lastName, targetRole = targetRole,
                                location = location, email = email, phone = phone,
                                linkedin = linkedin, github = github, portfolio = portfolio,
                                summary = summary, profileImageB64 = profileImageB64, profileImageName = profileImageName
                            )
                        )
                        Toast.makeText(context, "Profile Saved for AI!", Toast.LENGTH_SHORT).show()
                        onBack()
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) { Icon(Icons.Filled.Save, contentDescription = "Save", tint = MaterialTheme.colorScheme.onPrimary) }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Set it once. Use it everywhere.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider()

            Text("Personal Information", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

            Text("Profile Photo", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Text(if (profileImageB64.isBlank()) "Choose Photo" else "Replace Photo")
                }
                if (profileImageB64.isNotBlank()) {
                    OutlinedButton(onClick = { profileImageB64 = ""; profileImageName = "" }) { Text("Remove") }
                }
            }

            OutlinedTextField(value = firstName, onValueChange = { firstName = it }, label = { Text("First Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = lastName, onValueChange = { lastName = it }, label = { Text("Last Name") }, modifier = Modifier.fillMaxWidth())

            OutlinedTextField(
                value = targetRole,
                onValueChange = { targetRole = it },
                label = { Text("Target Role") },
                modifier = Modifier.fillMaxWidth().onFocusChanged { roleFocused = it.isFocused }
            )
            if (roleFocused && targetRole.isNotEmpty()) {
                val matches = ROLE_SUGGESTIONS.filter { it.contains(targetRole, true) }.take(5)
                if (matches.isNotEmpty()) {
                    Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Suggestions", style = MaterialTheme.typography.titleSmall)
                            matches.forEach { s ->
                                Text(s, modifier = Modifier.fillMaxWidth().clickable { targetRole = s; roleFocused = false }.padding(vertical = 10.dp))
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }

            OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = summary, onValueChange = { summary = it }, label = { Text("About Me (Summary)") }, minLines = 4, modifier = Modifier.fillMaxWidth())

            HorizontalDivider()
            Text("Contact & Links", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = linkedin, onValueChange = { linkedin = it }, label = { Text("LinkedIn URL") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = github, onValueChange = { github = it }, label = { Text("GitHub URL") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = portfolio, onValueChange = { portfolio = it }, label = { Text("Portfolio URL") }, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onGoMasterVault,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Filled.Storage, contentDescription = "Vault")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open AI Master Vault", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

private fun readImageAsBase64(ctx: Context, uri: Uri): Pair<String, String>? {
    return try {
        val s = ctx.contentResolver.openInputStream(uri) ?: return null
        val b = BitmapFactory.decodeStream(s); s.close()
        val size = min(b.width, b.height); val sq = Bitmap.createBitmap(b, (b.width - size)/2, (b.height - size)/2, size, size)
        val sc = if (size > 512) Bitmap.createScaledBitmap(sq, 512, 512, true) else sq
        val os = ByteArrayOutputStream(); sc.compress(Bitmap.CompressFormat.JPEG, 85, os)
        val name = ctx.contentResolver.query(uri, null, null, null, null)?.use { c -> val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME); if (c.moveToFirst() && i >= 0) c.getString(i) else "photo.jpg" } ?: "photo.jpg"
        Base64.encodeToString(os.toByteArray(), Base64.NO_WRAP) to name
    } catch (_: Exception) { null }
}