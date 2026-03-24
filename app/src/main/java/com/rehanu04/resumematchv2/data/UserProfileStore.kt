package com.rehanu04.resumematchv2.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.userProfileDataStore by preferencesDataStore(name = "user_profile")

data class UserProfile(
    val firstName: String = "", val lastName: String = "", val targetRole: String = "",
    val location: String = "", val email: String = "", val phone: String = "",
    val linkedin: String = "", val github: String = "", val portfolio: String = "",
    val summary: String = "", val profileImageB64: String = "", val profileImageName: String = "",
    val savedProjectsJson: String = "[]",
    val savedExperienceJson: String = "[]",
    val savedSkillsJson: String = "[]",
    val chatHistoryJson: String = "[]" // ✅ New! Saves your conversation
) {
    val isComplete: Boolean get() = firstName.isNotBlank() && lastName.isNotBlank()
}

class UserProfileStore(private val context: Context) {
    companion object {
        val FIRST_NAME = stringPreferencesKey("first_name")
        val LAST_NAME = stringPreferencesKey("last_name")
        val TARGET_ROLE = stringPreferencesKey("target_role")
        val LOCATION = stringPreferencesKey("location")
        val EMAIL = stringPreferencesKey("email")
        val PHONE = stringPreferencesKey("phone")
        val LINKEDIN = stringPreferencesKey("linkedin")
        val GITHUB = stringPreferencesKey("github")
        val PORTFOLIO = stringPreferencesKey("portfolio")
        val SUMMARY = stringPreferencesKey("summary")
        val IMAGE_B64 = stringPreferencesKey("image_b64")
        val IMAGE_NAME = stringPreferencesKey("image_name")
        val SAVED_PROJECTS = stringPreferencesKey("saved_projects")
        val SAVED_EXPERIENCE = stringPreferencesKey("saved_experience")
        val SAVED_SKILLS = stringPreferencesKey("saved_skills")
        val CHAT_HISTORY = stringPreferencesKey("chat_history")
    }

    val userProfileFlow: Flow<UserProfile> = context.userProfileDataStore.data.map { p ->
        UserProfile(
            firstName = p[FIRST_NAME] ?: "", lastName = p[LAST_NAME] ?: "", targetRole = p[TARGET_ROLE] ?: "",
            location = p[LOCATION] ?: "", email = p[EMAIL] ?: "", phone = p[PHONE] ?: "",
            linkedin = p[LINKEDIN] ?: "", github = p[GITHUB] ?: "", portfolio = p[PORTFOLIO] ?: "",
            summary = p[SUMMARY] ?: "", profileImageB64 = p[IMAGE_B64] ?: "", profileImageName = p[IMAGE_NAME] ?: "",
            savedProjectsJson = p[SAVED_PROJECTS] ?: "[]", savedExperienceJson = p[SAVED_EXPERIENCE] ?: "[]",
            savedSkillsJson = p[SAVED_SKILLS] ?: "[]", chatHistoryJson = p[CHAT_HISTORY] ?: "[]"
        )
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        context.userProfileDataStore.edit { p ->
            p[FIRST_NAME] = profile.firstName; p[LAST_NAME] = profile.lastName; p[TARGET_ROLE] = profile.targetRole
            p[LOCATION] = profile.location; p[EMAIL] = profile.email; p[PHONE] = profile.phone
            p[LINKEDIN] = profile.linkedin; p[GITHUB] = profile.github; p[PORTFOLIO] = profile.portfolio
            p[SUMMARY] = profile.summary; p[IMAGE_B64] = profile.profileImageB64; p[IMAGE_NAME] = profile.profileImageName
            p[SAVED_PROJECTS] = profile.savedProjectsJson; p[SAVED_EXPERIENCE] = profile.savedExperienceJson
            p[SAVED_SKILLS] = profile.savedSkillsJson; p[CHAT_HISTORY] = profile.chatHistoryJson
        }
    }
}