@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.rehanu04.resumematchv2.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rehanu04.resumematchv2.data.UserProfileStore
import kotlinx.coroutines.launch
import java.util.Calendar

// --- ISOLATED DATA CLASSES TO PREVENT CRASHES ---
data class VaultProject(val name: String = "", val startMonth: String = "", val startYear: String = "", val endMonth: String = "", val endYear: String = "", val bullets: String = "")
data class VaultExperience(val company: String = "", val role: String = "", val startMonth: String = "", val startYear: String = "", val endMonth: String = "", val endYear: String = "", val bullets: String = "")

// --- DATE DROPDOWN CONSTANTS ---
private val MONTH_OPTIONS = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
private val YEAR_OPTIONS = (Calendar.getInstance().get(Calendar.YEAR) + 5 downTo 1980).map { it.toString() }

@Composable
fun MasterVaultScreen(
    onBack: () -> Unit,
    userProfileStore: UserProfileStore
) {
    val scope = rememberCoroutineScope()
    val gson = remember { Gson() }
    val userProfile by userProfileStore.userProfileFlow.collectAsState(initial = com.rehanu04.resumematchv2.data.UserProfile())

    val snackbarHostState = remember { SnackbarHostState() }

    // --- BULLETPROOF DATA PARSING ---
    val vaultProjects: List<VaultProject> = try {
        val listTypeProj = object : TypeToken<List<VaultProject>>() {}.type
        val parsed: List<VaultProject>? = gson.fromJson(userProfile.savedProjectsJson, listTypeProj)
        parsed?.filterNotNull()?.map {
            VaultProject(
                name = it.name ?: "Unknown Project",
                startMonth = it.startMonth ?: "", startYear = it.startYear ?: "",
                endMonth = it.endMonth ?: "", endYear = it.endYear ?: "",
                bullets = it.bullets ?: ""
            )
        } ?: emptyList()
    } catch (e: Exception) { emptyList() }

    val vaultExperience: List<VaultExperience> = try {
        val listTypeExp = object : TypeToken<List<VaultExperience>>() {}.type
        val parsed: List<VaultExperience>? = gson.fromJson(userProfile.savedExperienceJson, listTypeExp)
        parsed?.filterNotNull()?.map {
            VaultExperience(
                company = it.company ?: "Unknown Company", role = it.role ?: "Unknown Role",
                startMonth = it.startMonth ?: "", startYear = it.startYear ?: "",
                endMonth = it.endMonth ?: "", endYear = it.endYear ?: "",
                bullets = it.bullets ?: ""
            )
        } ?: emptyList()
    } catch (e: Exception) { emptyList() }

    val vaultSkills: List<String> = try {
        val listTypeSkill = object : TypeToken<List<String>>() {}.type
        val parsed: List<String>? = gson.fromJson(userProfile.savedSkillsJson, listTypeSkill)
        parsed?.filterNotNull()?.filter { it.isNotBlank() } ?: emptyList()
    } catch (e: Exception) { emptyList() }

    var editingProjectIndex by remember { mutableIntStateOf(-1) }
    var editingExperienceIndex by remember { mutableIntStateOf(-1) }
    var tempProject by remember { mutableStateOf(VaultProject()) }
    var tempExperience by remember { mutableStateOf(VaultExperience()) }

    var isSkillsExpanded by remember { mutableStateOf(false) }

    val isVaultEmpty = vaultSkills.isEmpty() && vaultProjects.isEmpty() && vaultExperience.isEmpty()

    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("My Master Vault", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->

        if (isVaultEmpty) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Filled.Info, contentDescription = "Empty Vault", modifier = Modifier.size(100.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                Spacer(Modifier.height(24.dp))
                Text("Your Vault is Empty", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Text("Tap the microphone in the AI Assistant to start building your career profile!", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- SKILLS SECTION (CRASH PROOF GRID) ---
                if (vaultSkills.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Star, contentDescription = "Skills", tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Saved Skills (${vaultSkills.size})", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        }
                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        val displayedSkills = if (isSkillsExpanded) vaultSkills else vaultSkills.take(6)

                        // We break the skills into rows of 2 so it forms a perfect grid without FlowRow!
                        val chunkedSkills = displayedSkills.chunked(2)

                        Card(
                            modifier = Modifier.fillMaxWidth().animateContentSize(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                chunkedSkills.forEach { rowSkills ->
                                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        rowSkills.forEach { skill ->
                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(skill, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Icon(
                                                        Icons.Filled.Close, contentDescription = "Remove",
                                                        modifier = Modifier.size(16.dp).clickable {
                                                            val oldList = vaultSkills
                                                            val newList = vaultSkills.filter { it != skill }
                                                            scope.launch {
                                                                userProfileStore.saveUserProfile(userProfile.copy(savedSkillsJson = gson.toJson(newList)))
                                                                val result = snackbarHostState.showSnackbar("'$skill' removed", "UNDO", duration = SnackbarDuration.Short)
                                                                if (result == SnackbarResult.ActionPerformed) {
                                                                    userProfileStore.saveUserProfile(userProfile.copy(savedSkillsJson = gson.toJson(oldList)))
                                                                }
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                        // If odd number of items, fill the empty space
                                        if (rowSkills.size == 1) { Spacer(modifier = Modifier.weight(1f)) }
                                    }
                                }

                                if (vaultSkills.size > 6) {
                                    Divider()
                                    TextButton(
                                        onClick = { isSkillsExpanded = !isSkillsExpanded },
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    ) { Text(if (isSkillsExpanded) "Show Less" else "Show All ${vaultSkills.size} Skills") }
                                }
                            }
                        }
                    }
                }

                // --- PROJECTS SECTION ---
                if (vaultProjects.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(24.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Star, contentDescription = "Projects", tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Saved Projects (${vaultProjects.size})", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        }
                        Divider(modifier = Modifier.padding(vertical = 12.dp))
                    }
                    itemsIndexed(vaultProjects) { index, proj ->
                        ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                            Column(Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(proj.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { editingProjectIndex = index; tempProject = proj }) { Icon(Icons.Filled.Edit, "Edit", tint = MaterialTheme.colorScheme.primary) }
                                    IconButton(onClick = {
                                        val oldList = vaultProjects
                                        val newList = vaultProjects.filterIndexed { i, _ -> i != index }
                                        scope.launch {
                                            userProfileStore.saveUserProfile(userProfile.copy(savedProjectsJson = gson.toJson(newList)))
                                            val result = snackbarHostState.showSnackbar("Project deleted", "UNDO", duration = SnackbarDuration.Short)
                                            if (result == SnackbarResult.ActionPerformed) {
                                                userProfileStore.saveUserProfile(userProfile.copy(savedProjectsJson = gson.toJson(oldList)))
                                            }
                                        }
                                    }) { Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
                                }
                                Text("${proj.startMonth} ${proj.startYear} — ${proj.endMonth} ${proj.endYear}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                Spacer(Modifier.height(8.dp))
                                Text(proj.bullets, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // --- EXPERIENCE SECTION ---
                if (vaultExperience.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(24.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Person, contentDescription = "Experience", tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Saved Experience (${vaultExperience.size})", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        }
                        Divider(modifier = Modifier.padding(vertical = 12.dp))
                    }
                    itemsIndexed(vaultExperience) { index, exp ->
                        ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                            Column(Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(exp.company, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { editingExperienceIndex = index; tempExperience = exp }) { Icon(Icons.Filled.Edit, "Edit", tint = MaterialTheme.colorScheme.primary) }
                                    IconButton(onClick = {
                                        val oldList = vaultExperience
                                        val newList = vaultExperience.filterIndexed { i, _ -> i != index }
                                        scope.launch {
                                            userProfileStore.saveUserProfile(userProfile.copy(savedExperienceJson = gson.toJson(newList)))
                                            val result = snackbarHostState.showSnackbar("Experience deleted", "UNDO", duration = SnackbarDuration.Short)
                                            if (result == SnackbarResult.ActionPerformed) {
                                                userProfileStore.saveUserProfile(userProfile.copy(savedExperienceJson = gson.toJson(oldList)))
                                            }
                                        }
                                    }) { Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
                                }
                                Text(exp.role, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("${exp.startMonth} ${exp.startYear} — ${exp.endMonth} ${exp.endYear}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                Spacer(Modifier.height(8.dp))
                                Text(exp.bullets, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(40.dp)) }
            }
        }
    }

    // --- DIALOG FOR EDITING PROJECT ---
    if (editingProjectIndex != -1) {
        Dialog(onDismissRequest = { editingProjectIndex = -1 }) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                    Text("Edit Vault Project", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(value = tempProject.name, onValueChange = { tempProject = tempProject.copy(name = it) }, label = { Text("Project Name") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))

                    VaultMonthYearRangeInputs(
                        sM = tempProject.startMonth, sY = tempProject.startYear, eM = tempProject.endMonth, eY = tempProject.endYear,
                        onSM = { tempProject = tempProject.copy(startMonth = it) }, onSY = { tempProject = tempProject.copy(startYear = it) },
                        onEM = { tempProject = tempProject.copy(endMonth = it) }, onEY = { tempProject = tempProject.copy(endYear = it) }
                    )

                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = tempProject.bullets, onValueChange = { tempProject = tempProject.copy(bullets = it) }, label = { Text("Bullets") }, minLines = 4, modifier = Modifier.fillMaxWidth())

                    Spacer(Modifier.height(24.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { editingProjectIndex = -1 }) { Text("Cancel") }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = {
                            val newList = vaultProjects.toMutableList()
                            newList[editingProjectIndex] = tempProject
                            scope.launch { userProfileStore.saveUserProfile(userProfile.copy(savedProjectsJson = gson.toJson(newList))) }
                            editingProjectIndex = -1
                        }) { Text("Save") }
                    }
                }
            }
        }
    }

    // --- DIALOG FOR EDITING EXPERIENCE ---
    if (editingExperienceIndex != -1) {
        Dialog(onDismissRequest = { editingExperienceIndex = -1 }) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                    Text("Edit Vault Experience", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(value = tempExperience.company, onValueChange = { tempExperience = tempExperience.copy(company = it) }, label = { Text("Company") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = tempExperience.role, onValueChange = { tempExperience = tempExperience.copy(role = it) }, label = { Text("Role") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))

                    VaultMonthYearRangeInputs(
                        sM = tempExperience.startMonth, sY = tempExperience.startYear, eM = tempExperience.endMonth, eY = tempExperience.endYear,
                        onSM = { tempExperience = tempExperience.copy(startMonth = it) }, onSY = { tempExperience = tempExperience.copy(startYear = it) },
                        onEM = { tempExperience = tempExperience.copy(endMonth = it) }, onEY = { tempExperience = tempExperience.copy(endYear = it) }
                    )

                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = tempExperience.bullets, onValueChange = { tempExperience = tempExperience.copy(bullets = it) }, label = { Text("Bullets") }, minLines = 4, modifier = Modifier.fillMaxWidth())

                    Spacer(Modifier.height(24.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { editingExperienceIndex = -1 }) { Text("Cancel") }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = {
                            val newList = vaultExperience.toMutableList()
                            newList[editingExperienceIndex] = tempExperience
                            scope.launch { userProfileStore.saveUserProfile(userProfile.copy(savedExperienceJson = gson.toJson(newList))) }
                            editingExperienceIndex = -1
                        }) { Text("Save") }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VaultSelectionField(label: String, value: String, options: List<String>, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = if (value.isBlank()) "Not set" else value,
            onValueChange = {}, readOnly = true, label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Not set") }, onClick = { onValueChange(""); expanded = false })
            options.forEach { o -> DropdownMenuItem(text = { Text(o) }, onClick = { onValueChange(o); expanded = false }) }
        }
    }
}

@Composable
private fun VaultMonthYearRangeInputs(sM: String, sY: String, eM: String, eY: String, onSM: (String) -> Unit, onSY: (String) -> Unit, onEM: (String) -> Unit, onEY: (String) -> Unit) {
    Text("Start Date", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(4.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        VaultSelectionField("Month", sM, MONTH_OPTIONS, onSM, Modifier.weight(1f))
        VaultSelectionField("Year", sY, YEAR_OPTIONS, onSY, Modifier.weight(1f))
    }
    Spacer(Modifier.height(12.dp))
    Text("End Date", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(4.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        VaultSelectionField("Month", eM, listOf("Present") + MONTH_OPTIONS, onEM, Modifier.weight(1f))
        VaultSelectionField("Year", eY, listOf("Present") + YEAR_OPTIONS, onEY, Modifier.weight(1f))
    }
}