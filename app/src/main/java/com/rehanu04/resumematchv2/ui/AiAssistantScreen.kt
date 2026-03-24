package com.rehanu04.resumematchv2.ui

import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rehanu04.resumematchv2.data.UserProfileStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit // ✅ Added for Timeouts

data class ChatMessage(val text: String, val isUser: Boolean, val isLoading: Boolean = false)

data class AiExperience(val company: String = "", val role: String = "", val startMonth: String = "", val startYear: String = "", val endMonth: String = "", val endYear: String = "", val bullets: String = "")
data class AiProject(val name: String = "", val startMonth: String = "", val startYear: String = "", val endMonth: String = "", val endYear: String = "", val bullets: String = "")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(
    onBack: () -> Unit,
    userProfileStore: UserProfileStore,
    apiBaseUrl: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gson = remember { Gson() }
    val listState = rememberLazyListState()

    val userProfile by userProfileStore.userProfileFlow.collectAsState(initial = com.rehanu04.resumematchv2.data.UserProfile())

    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var isInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(userProfile.chatHistoryJson) {
        if (!isInitialized) {
            val type = object : TypeToken<List<ChatMessage>>() {}.type
            val saved: List<ChatMessage>? = gson.fromJson(userProfile.chatHistoryJson, type)
            messages = if (saved.isNullOrEmpty()) {
                listOf(ChatMessage("Hi! I'm your AI Resume Agent. Tell me about your recent projects, and I'll extract the details into your Master Vault.", false))
            } else saved
            isInitialized = true
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    var currentInput by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }

    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        isListening = false
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.getOrNull(0)
            if (!spokenText.isNullOrBlank()) currentInput = spokenText
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            isListening = true
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Tell me about your experience...")
            }
            try { speechLauncher.launch(intent) } catch (e: Exception) { isListening = false; Toast.makeText(context, "Speech not available", Toast.LENGTH_SHORT).show() }
        } else {
            Toast.makeText(context, "Microphone permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendMessage() {
        if (currentInput.isBlank()) return
        val transcript = currentInput
        currentInput = ""

        messages = messages + ChatMessage(transcript, true) + ChatMessage("Thinking...", false, true)

        scope.launch {
            try {
                // ✅ FIX 1: Extended Timeouts for Render Cold Starts
                val client = OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .build()

                val jsonBody = JSONObject().apply { put("transcript", transcript) }.toString()

                // Keep fallback just in case, but rely on apiBaseUrl passed from MainActivity
                val safeBaseUrl = if (apiBaseUrl.isNotBlank()) apiBaseUrl else "http://192.168.1.6:8000"

                val req = Request.Builder()
                    .url(safeBaseUrl.trimEnd('/') + "/v1/ai/parse-dump")
                    .post(jsonBody.toRequestBody("application/json".toMediaType()))
                    .build()

                val (isSuccess, responseStr) = withContext(Dispatchers.IO) {
                    client.newCall(req).execute().use { response -> response.isSuccessful to response.body?.string() }
                }

                if (isSuccess && responseStr != null) {
                    val parsedObj = JSONObject(responseStr)
                    val aiReply = parsedObj.optString("reply", "I've saved your details to the Vault!")

                    val newProjectsJson = parsedObj.optJSONArray("projects")?.toString() ?: "[]"
                    val newExperienceJson = parsedObj.optJSONArray("experience")?.toString() ?: "[]"
                    val newSkillsJson = parsedObj.optJSONArray("skills_suggested")?.toString() ?: "[]"

                    val listTypeProj = object : TypeToken<List<AiProject>>() {}.type
                    val listTypeExp = object : TypeToken<List<AiExperience>>() {}.type
                    val listTypeSkill = object : TypeToken<List<String>>() {}.type

                    val existingProjects: List<AiProject> = gson.fromJson(userProfile.savedProjectsJson, listTypeProj) ?: emptyList()
                    val existingExp: List<AiExperience> = gson.fromJson(userProfile.savedExperienceJson, listTypeExp) ?: emptyList()
                    val existingSkills: List<String> = gson.fromJson(userProfile.savedSkillsJson, listTypeSkill) ?: emptyList()

                    val parsedProjects: List<AiProject> = gson.fromJson(newProjectsJson, listTypeProj) ?: emptyList()
                    val parsedExp: List<AiExperience> = gson.fromJson(newExperienceJson, listTypeExp) ?: emptyList()
                    val parsedSkills: List<String> = gson.fromJson(newSkillsJson, listTypeSkill) ?: emptyList()

                    val mergedProjects = (existingProjects + parsedProjects).groupBy { it.name.lowercase().trim() }.map { it.value.last() }
                    val mergedExp = (existingExp + parsedExp).groupBy { it.company.lowercase().trim() }.map { it.value.last() }
                    val mergedSkills = (existingSkills + parsedSkills).map { it.trim() }.distinctBy { it.lowercase() }

                    val finalMessages = messages.dropLast(1) + ChatMessage(aiReply, false)
                    messages = finalMessages

                    userProfileStore.saveUserProfile(
                        userProfile.copy(
                            savedProjectsJson = gson.toJson(mergedProjects),
                            savedExperienceJson = gson.toJson(mergedExp),
                            savedSkillsJson = gson.toJson(mergedSkills),
                            chatHistoryJson = gson.toJson(finalMessages)
                        )
                    )
                } else {
                    messages = messages.dropLast(1) + ChatMessage("Backend Error: Couldn't parse data.", false)
                }
            } catch (e: Exception) {
                messages = messages.dropLast(1) + ChatMessage("Connection Error: Request timed out. Ensure your Render server is awake!", false)
            }
        }
    }

    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text("AI Assistant") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                actions = {
                    TextButton(onClick = {
                        scope.launch {
                            messages = emptyList()
                            userProfileStore.saveUserProfile(userProfile.copy(chatHistoryJson = "[]"))
                        }
                    }) { Text("Clear Chat") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { msg -> ChatBubble(msg) }
            }

            Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp).navigationBarsPadding().imePadding(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = currentInput,
                        onValueChange = { currentInput = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(if (isListening) "Listening..." else "Type or tap microphone...") },
                        shape = RoundedCornerShape(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FloatingActionButton(
                        onClick = {
                            if (currentInput.isNotBlank()) sendMessage()
                            else permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Icon(if (currentInput.isBlank()) Icons.Filled.Mic else Icons.Filled.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(msg: ChatMessage) {
    val alignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (msg.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
    val textColor = if (msg.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Surface(color = bgColor, shape = RoundedCornerShape(16.dp), modifier = Modifier.widthIn(max = 280.dp)) {
            Text(text = msg.text, modifier = Modifier.padding(16.dp), color = textColor)
        }
    }
}