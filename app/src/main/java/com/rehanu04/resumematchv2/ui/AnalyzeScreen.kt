package com.rehanu04.resumematchv2.ui

import android.content.Context
import android.content.Intent
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

private const val MAX_MATCHED_CHIPS = 50
private const val MAX_MISSING_CHIPS = 50
private const val PREVIEW_CHIP_LIMIT = 12

// -----------------------------
// Models
// -----------------------------
private data class AnalyzeResult(
    val score: Int,
    val matchedCount: Int,
    val missingCount: Int,
    val matchedTop: List<String>,
    val missingTop: List<String>,
    val resumeTextLength: Int,
    val resumeText: String? = null
)

// -----------------------------
// Known skills dictionary
// -----------------------------
private val KNOWN_SKILLS: Map<String, String> = linkedMapOf(
    "python" to "Python", "kotlin" to "Kotlin", "java" to "Java", "javascript" to "JavaScript",
    "typescript" to "TypeScript", "sql" to "SQL", "bash" to "Bash", "c" to "C", "cpp" to "C++",
    "csharp" to "C#", "go" to "Go", "rust" to "Rust", "fastapi" to "FastAPI", "uvicorn" to "Uvicorn",
    "flask" to "Flask", "django" to "Django", "node" to "Node.js", "node.js" to "Node.js",
    "express" to "Express", "spring" to "Spring", "spring boot" to "Spring Boot", "rest" to "REST",
    "rest apis" to "REST APIs", "http" to "HTTP", "openapi" to "OpenAPI", "swagger" to "Swagger",
    "swagger openapi" to "Swagger/OpenAPI", "grpc" to "gRPC", "postman" to "Postman",
    "render" to "Render", "regex" to "Regex", "text normalization" to "Text Normalization",
    "normalization" to "Normalization", "pypdf" to "PyPDF", "multipart" to "Multipart",
    "async" to "Async", "concurrency" to "Concurrency", "android" to "Android", "okhttp" to "OkHttp",
    "retrofit" to "Retrofit", "coroutines" to "Coroutines", "jetpack compose" to "Jetpack Compose",
    "compose" to "Jetpack Compose", "postgresql" to "PostgreSQL", "postgres" to "PostgreSQL",
    "mysql" to "MySQL", "mongodb" to "MongoDB", "redis" to "Redis", "docker" to "Docker",
    "kubernetes" to "Kubernetes", "k8s" to "Kubernetes", "terraform" to "Terraform",
    "linux" to "Linux", "aws" to "AWS", "azure" to "Azure", "gcp" to "GCP",
    "google cloud" to "Google Cloud", "google cloud platform" to "Google Cloud Platform",
    "ci/cd" to "CI/CD", "github actions" to "GitHub Actions", "machine learning" to "Machine Learning",
    "deep learning" to "Deep Learning", "nlp" to "NLP", "ai" to "AI", "ml" to "ML",
    "ai/ml" to "AI/ML", "distributed systems" to "Distributed Systems", "system design" to "System Design",
    "data structures" to "Data Structures", "algorithms" to "Algorithms",
    "multithreaded programming" to "Multithreaded Programming", "virtualization" to "Virtualization"
)

private val ALIASES: Map<String, String> = mapOf(
    "c++" to "cpp", "cplusplus" to "cpp", "c#" to "csharp", ".net" to "csharp", "dotnet" to "csharp",
    "nodejs" to "node.js", "open api" to "swagger openapi", "openapi" to "swagger openapi",
    "swagger" to "swagger openapi", "swagger/openapi" to "swagger openapi",
    "swaggeropenapi" to "swagger openapi", "rest api" to "rest", "rest apis" to "rest",
    "restful" to "rest", "ok http" to "okhttp", "git hub" to "github", "k8s" to "kubernetes",
    "ci cd" to "ci/cd", "cicd" to "ci/cd", "text normalization" to "normalization",
    "ai ml" to "ai/ml", "aiml" to "ai/ml"
)

private val STOPWORDS = setOf(
    "the","and","or","to","of","in","for","with","on","at","as","a","an","by","from","is","are","was","were",
    "this","that","these","those","it","its","be","been","being","will","would","should","can","could","may","might",
    "role","about","job","description","responsibilities","requirements","required","preferred","qualification","qualifications",
    "experience","strong","ability","skills","skill","knowledge","familiarity","hands","work","working","team","teams",
    "collaborate","collaboration","collaborative","communication","well","documented","clean","efficient","design","develop",
    "build","optimize","scalable","systems","system","across","ensure","high","availability","reliability","performance",
    "projects","project","architecture","reviews","technical","documentation","plus","bonus"
)

private val SKILL_BLACKLIST = setOf(
    "backend", "engineer", "software", "title", "role", "deploy", "deployment", "implement",
    "implementation", "integration", "handling", "write", "build", "design", "develop",
    "optimize", "create", "request", "response", "requests", "responses", "api", "apis",
    "concepts", "nice", "required", "skills", "failures", "timeouts", "timeout", "failure",
    "analysis", "scoring", "documents", "document"
)

private fun isValidSkillCandidate(canonical: String): Boolean {
    val s = canonical.trim().lowercase()
    if (s.isBlank() || s in STOPWORDS || s in SKILL_BLACKLIST) return false
    val badPhrases = listOf("request response", "failures timeouts", "pdf documents", "okhttp http", "regex text", "python fastapi", "scoring analysis")
    if (badPhrases.contains(s)) return false
    if (s !in KNOWN_SKILLS.keys) {
        val hasTechPunct = s.contains("+") || s.contains("#") || s.contains(".") || s.contains("/")
        val isMultiWord = s.contains(" ")
        val longEnough = s.length >= 4
        if (!hasTechPunct && !isMultiWord && !longEnough) return false
        if (Regex("^[a-z]+$").matches(s) && s.length <= 10) return false
    }
    return true
}

private fun clamp0to100(v: Int): Int = v.coerceIn(0, 100)

private fun deriveAtsReadiness(resumeTextLength: Int, resumeText: String?, matchedCount: Int, missingCount: Int, fileName: String?): Pair<Int, String?> {
    val x = resumeTextLength.coerceAtLeast(0)
    val base = when {
        x < 600 -> 20; x < 1000 -> 35 + ((x - 600) / 12.0).roundToInt(); x < 1800 -> 55 + ((x - 1000) / 20.0).roundToInt(); x < 3200 -> 72 + ((x - 1800) / 60.0).roundToInt(); else -> 90
    }.coerceIn(0, 95)
    val (likelihood, _) = estimateResumeLikelihood(resumeText, x, matchedCount, missingCount, fileName)
    var ats = base
    val warning: String? = when {
        likelihood < 0.25f -> "This PDF doesn't look like a resume (low confidence). Please upload a resume PDF with sections like Skills/Experience/Education."
        likelihood < 0.45f -> "Low resume confidence: results may be unreliable. Make sure you selected the correct resume PDF."
        x < 500 -> "Low extractable text detected. If your resume is image/table heavy, export a text-based PDF for better ATS parsing."
        else -> null
    }
    if (likelihood < 0.25f) ats = (minOf(ats, 40) * (0.55f + 0.45f * likelihood)).roundToInt()
    else if (likelihood < 0.45f) ats = (minOf(ats, 65) * (0.70f + 0.30f * likelihood)).roundToInt()
    if (!fileName.isNullOrBlank() && fileName.lowercase().contains("resume") && warning != null) ats = (ats + 5).coerceAtMost(80)
    return clamp0to100(ats) to warning
}

private fun estimateResumeLikelihood(resumeText: String?, resumeTextLength: Int, matchedCount: Int, missingCount: Int, fileName: String?): Pair<Float, String?> {
    if (!resumeText.isNullOrBlank()) {
        val t = resumeText.lowercase()
        val hasEmail = Regex("\\b[\\w.%+-]+@[\\w.-]+\\.[a-z]{2,}\\b").containsMatchIn(t)
        val hasPhone = Regex("\\b(\\+?\\d{1,3}[- .]?)?(\\(?\\d{2,4}\\)?[- .]?)?\\d{3,4}[- .]?\\d{3,4}\\b").containsMatchIn(t)
        val hasLinkedIn = t.contains("linkedin.com") || t.contains("linkedin")
        val hasGithub = t.contains("github.com") || t.contains("github")
        val headers = listOf("experience", "education", "skills", "projects", "summary", "certifications", "work experience")
        val headerHits = headers.count { h -> Regex("\\b" + Regex.escape(h) + "\\b").containsMatchIn(t) }
        val bulletHits = Regex("(^|\\n)\\s*[•\\u2022\\-\\*]\\s+").findAll(t).take(10).count()
        val speechMarkers = listOf("ladies and gentlemen", "good morning", "good evening", "thank you", "respected", "annual day", "speech")
        val speechHit = speechMarkers.any { t.contains(it) }

        var score = 0.0f
        if (hasEmail) score += 0.25f; if (hasPhone) score += 0.20f; if (hasLinkedIn) score += 0.10f; if (hasGithub) score += 0.10f
        score += (headerHits.coerceAtMost(4) * 0.10f); if (bulletHits >= 3) score += 0.10f; if (speechHit) score -= 0.45f
        return score.coerceIn(0.0f, 1.0f) to (if (speechHit) "Speech-like wording detected." else null)
    }

    val denom = (matchedCount + missingCount).coerceAtLeast(1)
    val coverage = matchedCount.toFloat() / denom.toFloat()
    var likelihood = 0.60f
    var reason: String? = null

    if (resumeTextLength < 400) { likelihood = 0.20f; reason = "Very low extracted text." }
    if (matchedCount <= 1 && missingCount >= 4 && coverage < 0.15f) { likelihood = minOf(likelihood, 0.25f); reason = "Very low JD overlap; possibly wrong document." }
    else if (matchedCount <= 2 && coverage < 0.20f) { likelihood = minOf(likelihood, 0.40f); reason = "Low JD overlap." }
    if (missingCount.toFloat() / denom.toFloat() > 0.70f && matchedCount <= 4 && coverage < 0.30f) { likelihood = minOf(likelihood, 0.30f); reason = "Very low JD overlap; possibly wrong document." }
    if (!fileName.isNullOrBlank() && (fileName.lowercase().contains("speech") || fileName.lowercase().contains("annual") || fileName.lowercase().contains("event"))) { likelihood = minOf(likelihood, 0.20f); reason = "Filename suggests non-resume document." }
    return likelihood.coerceIn(0.0f, 1.0f) to reason
}

private fun labelForScore(v: Int): String = when { v >= 80 -> "Strong"; v >= 60 -> "Good"; v >= 40 -> "Fair"; v >= 20 -> "Low"; else -> "Very Low" }

private fun parseAnalyzeJson(json: String): AnalyzeResult {
    val o = JSONObject(json)
    fun arrToList(a: org.json.JSONArray?): List<String> {
        if (a == null) return emptyList()
        val out = ArrayList<String>(a.length())
        for (i in 0 until a.length()) out.add(a.optString(i))
        return out
    }
    val resumeText = when { o.has("resume_text") -> o.optString("resume_text", null); o.has("resume_excerpt") -> o.optString("resume_excerpt", null); else -> null }
    return AnalyzeResult(o.optInt("score", 0), o.optInt("matched_count", 0), o.optInt("missing_count", 0), arrToList(o.optJSONArray("matched_top")), arrToList(o.optJSONArray("missing_top")), o.optInt("resume_text_length", 0), resumeText)
}

private fun looksLikeUrl(s: String): Boolean { val t = s.trim(); return t.startsWith("http://", true) || t.startsWith("https://", true) || t.startsWith("www.", true) }
private fun normalizeUrl(s: String): String { val t = s.trim(); return when { t.startsWith("http://", true) || t.startsWith("https://", true) -> t; t.startsWith("www.", true) -> "https://$t"; else -> t } }

private fun resolveJobDescriptionText(client: OkHttpClient, input: String): String {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return ""

    if (!looksLikeUrl(trimmed)) return trimmed

    val url = normalizeUrl(trimmed)

    // ✅ NEW: Explicitly block LinkedIn URLs since they block AI scraping
    if (url.contains("linkedin.com", ignoreCase = true)) {
        throw IllegalStateException("Cannot access LinkedIn securely. Please copy and paste the job description text directly.")
    }

    val req = Request.Builder()
        .url(url)
        .get()
        .addHeader("User-Agent", "ResumeMatchV2/1.0")
        .build()

    client.newCall(req).execute().use { resp ->
        val body = resp.body?.string()
        if (!resp.isSuccessful || body.isNullOrBlank()) {
            throw IllegalStateException("Could not fetch JD URL (HTTP ${resp.code}).")
        }

        val ct = resp.header("Content-Type").orEmpty()
        val extracted = if (ct.contains("text/html", ignoreCase = true)) {
            htmlToReadableText(body)
        } else {
            body
        }

        return if (extracted.length < 200) body else extracted
    }
}

private fun isLikelyJobDescription(text: String): Boolean {
    if (text.trim().length < 50) return true // Let short manual inputs pass
    val t = text.lowercase()

    // Check for build error junk
    val errorWords = listOf("exception", "build failed", "stacktrace", "compilation error", "gradlew", "execution failed")
    val errorHits = errorWords.count { t.contains(it) }

    // Check for real JD words
    val jdWords = listOf("experience", "requirement", "responsibility", "qualification", "skill", "apply", "role", "job", "salary", "team")
    val jdHits = jdWords.count { t.contains(it) }

    // If it looks heavily like an error log and not a JD
    if (errorHits > 0 && jdHits < 2) return false

    // If it's a long block of text but has zero JD keywords
    if (t.length > 200 && jdHits == 0) return false

    return true
}

private fun htmlToReadableText(html: String): String {
    var s = html
    s = s.replace(Regex("(?is)<script.*?>.*?</script>"), " ").replace(Regex("(?is)<style.*?>.*?</style>"), " ").replace(Regex("(?is)"), " ")
    s = s.replace(Regex("(?is)<br\\s*/?>"), "\n").replace(Regex("(?is)</p\\s*>"), "\n").replace(Regex("(?is)</div\\s*>"), "\n").replace(Regex("(?is)</li\\s*>"), "\n").replace(Regex("(?is)<li\\s*>"), " • ").replace(Regex("(?is)<[^>]+>"), " ")
    return s.replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'").replace(Regex("[\\t\\r]+"), " ").replace(Regex("\\s+"), " ").trim()
}

private fun getDisplayName(context: Context, uri: Uri): String? {
    val cursor = context.contentResolver.query(uri, null, null, null, null) ?: return null
    cursor.use { val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME); if (it.moveToFirst() && nameIndex >= 0) return it.getString(nameIndex) }
    return null
}

private suspend fun copyContentUriToTempFile(context: Context, uri: Uri): File {
    return withContext(Dispatchers.IO) {
        val tmp = File.createTempFile("resume_", ".pdf", context.cacheDir)
        context.contentResolver.openInputStream(uri).use { input -> FileOutputStream(tmp).use { output -> if (input != null) input.copyTo(output) } }
        tmp
    }
}

private fun canonicalize(raw: String): String {
    val t = raw.trim().lowercase()
    if (t.isBlank()) return ""
    val cleaned = t.replace("\u00A0", " ").replace("/", " ").replace("_", " ").replace("-", " ").replace(Regex("[()\\[\\]{}:;,\\.\\|]"), " ").replace(Regex("\\s+"), " ").trim()
    val direct = when (cleaned) { "c++" -> "cpp"; "c#" -> "csharp"; else -> cleaned }
    return ALIASES[direct] ?: ALIASES[direct.replace(" ", "")] ?: direct
}

private fun expandCompositeSkills(canonical: String): List<String> {
    val s = canonical.trim()
    if (s.isBlank()) return emptyList()
    if (s in KNOWN_SKILLS.keys) return listOf(s)
    val tokens = s.split(" ").filter { it.isNotBlank() }
    if (tokens.size <= 1) return listOf(s)
    val out = LinkedHashSet<String>()
    for (t in tokens) if (t in KNOWN_SKILLS.keys) out.add(t)
    for (i in 0 until tokens.size - 1) { val bi = tokens[i] + " " + tokens[i + 1]; if (bi in KNOWN_SKILLS.keys) out.add(bi) }
    for (i in 0 until tokens.size - 2) { val tri = tokens[i] + " " + tokens[i + 1] + " " + tokens[i + 2]; if (tri in KNOWN_SKILLS.keys) out.add(tri) }
    return if (out.isNotEmpty()) out.toList() else listOf(s)
}

private fun prettySkill(canonical: String): String {
    val k = canonical.trim().lowercase()
    return KNOWN_SKILLS[k] ?: k.split(" ").joinToString(" ") { part -> if (part.length <= 2) part.uppercase() else part.replaceFirstChar { it.uppercase() } }
}

private fun extractSkillsGlobal(text: String): Set<String> {
    if (text.isBlank()) return emptySet()
    val norm = text.replace("\u00A0", " ").replace(Regex("[•·]"), " ").replace(Regex("\\s+"), " ").trim()
    val lowered = norm.lowercase()
    val found = LinkedHashSet<String>()
    val dictKeys = KNOWN_SKILLS.keys.sortedByDescending { it.length }

    fun containsPhrase(phrase: String): Boolean {
        val parts = phrase.split(" ").filter { it.isNotBlank() }.map { Regex.escape(it) }
        return Regex("\\b" + parts.joinToString("\\s+") + "\\b", RegexOption.IGNORE_CASE).containsMatchIn(lowered)
    }

    for (k in dictKeys) {
        val canonical = canonicalize(k)
        if (canonical.isBlank()) continue
        if (k == "ci/cd") { if (lowered.contains("ci/cd") || lowered.contains("ci cd") || lowered.contains("cicd")) found.add("ci/cd"); continue }
        if (k == "node.js") { if (lowered.contains("node.js") || lowered.contains("nodejs") || containsPhrase("node js")) found.add("node.js"); continue }
        if (containsPhrase(k)) found.add(canonical)
        val collapsed = k.replace(" ", ""); if (collapsed != k && lowered.replace(" ", "").contains(collapsed)) found.add(canonical)
    }

    val tokenRegex = Regex("""[A-Za-z][A-Za-z0-9\+\#\./]{1,}""")
    tokenRegex.findAll(norm).map { it.value }.toList().forEach { t ->
        val s = t.trim(); if (s.length < 2) return@forEach
        val low = s.lowercase()
        if (low in STOPWORDS || low.all { it.isDigit() }) return@forEach
        if (low.length <= 3 && low !in setOf("ai","ml","sql","aws","gcp","api","ui")) return@forEach
        val isValid = s.contains("+") || s.contains("#") || s.contains("/") || s.contains(".") || (s.length in 2..6 && s.all { it.isUpperCase() }) || (s.any { it.isUpperCase() } && s.any { it.isLowerCase() }) || low in KNOWN_SKILLS.keys
        if (!isValid) return@forEach
        val c = canonicalize(t); if (c.isNotBlank() && c !in STOPWORDS) found.add(c)
    }

    val words = lowered.replace(Regex("[^a-z0-9+/# ]"), " ").replace(Regex("\\s+"), " ").trim().split(" ").filter { it.isNotBlank() }
    val valid = words.filter { w -> w.length >= 2 && w !in STOPWORDS && !w.all { it.isDigit() } }
    for (i in 0 until valid.size) {
        if (i + 1 < valid.size) {
            val bi = "${valid[i]} ${valid[i + 1]}"; val c = canonicalize(bi)
            if (c in KNOWN_SKILLS.keys) found.add(c)
            if (bi in setOf("machine learning", "deep learning", "distributed systems", "system design", "data structures")) found.add(c)
        }
        if (i + 2 < valid.size) { val tri = "${valid[i]} ${valid[i + 1]} ${valid[i + 2]}"; val c = canonicalize(tri); if (c in KNOWN_SKILLS.keys) found.add(c) }
    }

    if (Regex("(?<![a-z0-9])c\\+\\+(?![a-z0-9])", RegexOption.IGNORE_CASE).containsMatchIn(norm)) found.add("cpp")
    if (Regex("(?<![a-z0-9])c#(?![a-z0-9])", RegexOption.IGNORE_CASE).containsMatchIn(norm)) found.add("csharp")
    if (Regex("\\bai/ml\\b", RegexOption.IGNORE_CASE).containsMatchIn(norm)) found.add("ai/ml")

    return found.map { canonicalize(it) }.filter { isValidSkillCandidate(it) }.toLinkedHashSet()
}

private fun <T> Iterable<T>.toLinkedHashSet(): LinkedHashSet<T> { val s = LinkedHashSet<T>(); for (x in this) s.add(x); return s }

private val SKILL_PRIORITY: Map<String, Int> = run { val ordered = listOf("python", "fastapi", "uvicorn", "rest", "swagger openapi", "okhttp", "http", "regex", "pypdf", "docker", "render", "postman", "multipart", "async", "concurrency", "normalization", "android", "coroutines"); ordered.withIndex().associate { it.value to it.index } }
private fun skillPriority(canonical: String): Int = SKILL_PRIORITY[canonical.trim().lowercase()] ?: 10_000

private fun applyInference(matched: MutableSet<String>, missing: Set<String>) {
    fun addIfOk(k: String) { val c = canonicalize(k); if (c.isNotBlank() && c !in missing) matched.add(c) }
    val m = matched.map { it.lowercase() }.toSet()
    if ("fastapi" in m || "uvicorn" in m) addIfOk("python")
    if ("openapi" in m || "swagger" in m) addIfOk("swagger openapi")
    if ("okhttp" in m) addIfOk("http")
    if ("coroutines" in m) { addIfOk("async"); addIfOk("concurrency") }
    if ("async" in m) addIfOk("concurrency")
    if ("rest apis" in m) addIfOk("rest")
}

private enum class Bucket { LANG, API, ANDROID, CLOUD, AIML, SYSTEMS, OTHER }
private fun bucketOf(s: String): Bucket {
    val low = s.lowercase()
    return when {
        low in listOf("python","java","kotlin","c","cpp","csharp","sql","javascript","typescript","go","rust","bash") -> Bucket.LANG
        low.contains("fastapi") || low.contains("uvicorn") || low.contains("rest") || low.contains("openapi") || low.contains("swagger") || low.contains("grpc") || low.contains("http") -> Bucket.API
        low.contains("jetpack") || low.contains("compose") || low.contains("retrofit") || low.contains("okhttp") || low.contains("coroutines") -> Bucket.ANDROID
        low.contains("docker") || low.contains("kubernetes") || low.contains("terraform") || low.contains("aws") || low.contains("gcp") || low.contains("google cloud") || low.contains("azure") || low.contains("linux") || low.contains("ci/cd") -> Bucket.CLOUD
        low.contains("machine learning") || low == "ai" || low == "ml" || low.contains("ai/ml") || low.contains("nlp") -> Bucket.AIML
        low.contains("distributed") || low.contains("system design") || low.contains("data structures") || low.contains("algorithms") || low.contains("multithread") || low.contains("virtual") -> Bucket.SYSTEMS
        else -> Bucket.OTHER
    }
}

private fun buildStrongSuggestions(jobScore: Int, atsScore: Int, matched: List<String>, missing: List<String>): List<String> {
    val out = mutableListOf<String>(); val js = jobScore.coerceIn(0, 100); val ats = atsScore.coerceIn(0, 100)
    out.add("Tailor your Summary to the JD: target role + 2 strengths + 3 JD keywords (exact wording).")
    if (js < 40) { out.add("Job match is low: add 2 JD-aligned bullets in Projects/Experience that explicitly mention the top missing skills + results (numbers)."); out.add("Move relevance up: put a dedicated Skills section directly below Summary. Don’t hide keywords in the bottom.") } else if (js < 70) { out.add("Job match is mid: improve keyword coverage naturally—ensure JD skills appear in Summary + Skills + at least 2 project bullets.") } else { out.add("Job match is strong: focus on proof—add metrics (latency, cost, users, accuracy) for the bullets that mention matched skills.") }
    if (missing.isNotEmpty()) {
        val topMissing = missing.take(12); val grouped = topMissing.groupBy { bucketOf(it) }
        grouped[Bucket.LANG]?.takeIf { it.isNotEmpty() }?.let { out.add("Languages gap: prove with 1 bullet per language (e.g., “Built X in ${it.take(3).joinToString(", ")}”).") }
        grouped[Bucket.API]?.takeIf { it.isNotEmpty() }?.let { out.add("Backend/API gap: add 1 bullet showing endpoint design + auth/error handling + OpenAPI docs (${it.take(4).joinToString(", ")}).") }
        grouped[Bucket.CLOUD]?.takeIf { it.isNotEmpty() }?.let { out.add("Cloud/DevOps gap: add a deployment bullet (Docker + platform) and mention monitoring/timeouts/retries if applicable (${it.take(4).joinToString(", ")}).") }
        grouped[Bucket.SYSTEMS]?.takeIf { it.isNotEmpty() }?.let { out.add("Systems gap: add a bullet about scalability/concurrency/perf (throughput, time saved, latency reduced) (${it.take(3).joinToString(", ")}).") }
        grouped[Bucket.AIML]?.takeIf { it.isNotEmpty() }?.let { out.add("AI/ML gap: include 1 credible ML bullet (dataset size + metric + model type). Don’t claim frameworks you haven’t used.") }
        out.add("Top missing skills to address first: ${topMissing.take(6).joinToString(", ")}.")
    } else { out.add("Missing skills list is empty: validate by ensuring your resume explicitly names the same skills as the JD (ATS keyword matching).") }
    if (ats < 60) { out.add("ATS is weak: use standard headings (Summary, Skills, Experience, Projects, Education). Avoid tables/columns/images.") } else if (ats < 80) { out.add("ATS is okay: keep formatting simple and consistent. Ensure dates/roles are clearly readable.") } else { out.add("ATS is strong: keep formatting simple; now prioritize relevance + proof mapped directly to JD.") }
    out.add("Checklist: (1) Summary has 3 JD keywords, (2) Skills section has top 8–12 relevant skills, (3) 2 bullets prove those skills with metrics.")
    return out.distinct()
}

// -----------------------------
// UI - AnalyzeScreen
// -----------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyzeScreen(
    isDark: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    onGoCreate: () -> Unit,
    onGoProfile: () -> Unit,
    apiBaseUrl: String,
    apiAppKey: String,
    userProfileStore: com.rehanu04.resumematchv2.data.UserProfileStore // ✅ Added DataStore Parameter
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    // ✅ State flow for user profile
    val userProfile by userProfileStore.userProfileFlow.collectAsState(initial = com.rehanu04.resumematchv2.data.UserProfile())

    var jdInput by rememberSaveable { mutableStateOf("") }
    var selectedPdfUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var selectedPdfName by rememberSaveable { mutableStateOf<String?>(null) }

    var loading by remember { mutableStateOf(false) }
    var rawJson by remember { mutableStateOf<String?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }

    var jobAnalyzed by rememberSaveable { mutableStateOf(false) }
    var jobSkillsAfterClick by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var jobSkillKeysAfterClick by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var lastEffectiveJdText by rememberSaveable { mutableStateOf<String?>(null) }

    val pickPdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { selectedPdfUri = uri; selectedPdfName = getDisplayName(ctx, uri) ?: "selected.pdf" }
    }

    val jdText = jdInput.trim()

    fun buttonLabel(): String = when { jdText.isBlank() -> "Enter JD to analyze"; selectedPdfUri == null -> "Analyze Job"; else -> "Analyze Match" }
    fun buttonEnabled(): Boolean = when { loading -> false; jdText.isBlank() -> false; selectedPdfUri == null -> true; else -> apiBaseUrl.isNotBlank() && apiAppKey.isNotBlank() }

    fun runAnalyze() {
        errorText = null; rawJson = null; jobAnalyzed = false
        if (jdText.isBlank()) return
        loading = true

        scope.launch {
            try {
                val client = OkHttpClient()
                val effectiveJd = withContext(Dispatchers.IO) { resolveJobDescriptionText(client, jdText) }
                lastEffectiveJdText = effectiveJd

                if (!isLikelyJobDescription(effectiveJd)) {
                    errorText = "The provided text does not look like a Job Description (e.g., build errors or junk). Please paste a valid JD."
                    loading = false
                    return@launch
                }

                lastEffectiveJdText = effectiveJd

                val uri = selectedPdfUri ?: return@launch
                if (apiBaseUrl.isBlank() || apiAppKey.isBlank()) { errorText = "Missing API base URL or app key."; return@launch }

                val tmp = copyContentUriToTempFile(ctx, uri)
                val body = MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("resume", tmp.name, tmp.asRequestBody("application/pdf".toMediaType())).addFormDataPart("jd_text", effectiveJd).addFormDataPart("debug", "true").build()
                val req = Request.Builder().url(apiBaseUrl.trimEnd('/') + "/v1/analyze/pdf").post(body).addHeader("x-app-key", apiAppKey).build()

                val resp = withContext(Dispatchers.IO) { client.newCall(req).execute() }
                val txt = resp.body?.string()

                if (!resp.isSuccessful || txt == null) errorText = "HTTP ${resp.code}: ${txt ?: "No response body"}" else rawJson = txt
            } catch (e: Exception) { errorText = e.message ?: "Unknown error" } finally { loading = false }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ResumeMatch", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            IconButton(onClick = onGoProfile) { Icon(Icons.Filled.Person, contentDescription = "AI Profile") }
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), tonalElevation = 0.dp) {
                IconButton(onClick = { onToggleTheme(!isDark) }) { Icon(imageVector = if (isDark) Icons.Default.WbSunny else Icons.Default.DarkMode, contentDescription = "Toggle theme") }
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ✅ THE NUDGE BANNER
            if (!userProfile.isComplete) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().clickable { onGoProfile() },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Info, contentDescription = "Info", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Set up your AI Profile", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("Tap here to save your details so the AI Assistant knows who you are.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }

            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Resume (PDF)", style = MaterialTheme.typography.titleMedium)
                    Button(onClick = { pickPdf.launch(arrayOf("application/pdf")) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(999.dp)) { Text(if (selectedPdfUri == null) "Select PDF" else "Change PDF") }
                    Text(text = "Selected: ${selectedPdfName ?: "None"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (selectedPdfUri != null) { TextButton(onClick = { selectedPdfUri = null; selectedPdfName = null; rawJson = null; errorText = null }) { Text("Clear selected PDF") } }
                }
            }

            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Job description", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(value = jdInput, onValueChange = { jdInput = it }, modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 240.dp), placeholder = { Text("Paste JD URL or JD text") }, singleLine = false, maxLines = 12)
                    Text("Tip: paste Requirements + Responsibilities for best results.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Button(onClick = { runAnalyze() }, enabled = buttonEnabled(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(999.dp)) { Text(if (loading) "Analyzing..." else buttonLabel()) }
            TextButton(onClick = onGoCreate, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Don’t have a resume? Create one tailored for this job →") }

            if (errorText != null) {
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f))) {
                    Column(modifier = Modifier.padding(16.dp)) { Text("Error", fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(6.dp)); Text(errorText ?: "") }
                }
            }

            if (selectedPdfUri == null && jobAnalyzed && jdText.isNotBlank()) {
                JobOnlyResultsCard(isDark = isDark, skills = jobSkillsAfterClick)
                JobOnlySuggestionsCard(skills = jobSkillsAfterClick)
            }

            val parsed = remember(rawJson) { rawJson?.let { runCatching { parseAnalyzeJson(it) }.getOrNull() } }

            if (parsed != null) {
                val jobMatch = clamp0to100(parsed.score)
                val (ats, resumeWarn) = deriveAtsReadiness(parsed.resumeTextLength, parsed.resumeText, parsed.matchedCount, parsed.missingCount, selectedPdfName)
                val resumeLikelihood = estimateResumeLikelihood(parsed.resumeText, parsed.resumeTextLength, parsed.matchedCount, parsed.missingCount, selectedPdfName).first

                val effectiveJdUsed = (lastEffectiveJdText ?: jdText).trim()
                val jdKeysUsed = if (jobSkillKeysAfterClick.isNotEmpty()) {
                    jobSkillKeysAfterClick.asSequence().map { canonicalize(it) }.filter { isValidSkillCandidate(it) }.toCollection(LinkedHashSet<String>())
                } else {
                    extractSkillsGlobal(effectiveJdUsed).asSequence().map { canonicalize(it) }.flatMap { expandCompositeSkills(it).asSequence() }.filter { isValidSkillCandidate(it) }.toCollection(LinkedHashSet<String>())
                }

                val backendMatchedKeys = parsed.matchedTop.asSequence().map { canonicalize(it) }.flatMap { expandCompositeSkills(it).asSequence() }.filter { isValidSkillCandidate(it) }.toCollection(LinkedHashSet<String>())
                val backendMissingKeys = parsed.missingTop.asSequence().map { canonicalize(it) }.flatMap { expandCompositeSkills(it).asSequence() }.filter { isValidSkillCandidate(it) }.toCollection(LinkedHashSet<String>())

                val missingTarget = parsed.missingCount.coerceAtLeast(0)
                val missingFromBackend = LinkedHashSet<String>(backendMissingKeys)

                val missingDesiredSize = when {
                    parsed.missingTop.isNotEmpty() && backendMissingKeys.isEmpty() -> 0
                    else -> maxOf(missingTarget, backendMissingKeys.size)
                }

                val resumeEvidenceKeys = LinkedHashSet<String>().apply {
                    addAll(backendMatchedKeys)
                    if (!parsed.resumeText.isNullOrBlank()) {
                        val extra = extractSkillsGlobal(parsed.resumeText).asSequence().map { canonicalize(it) }.flatMap { expandCompositeSkills(it).asSequence() }.filter { isValidSkillCandidate(it) }.toCollection(LinkedHashSet<String>())
                        addAll(extra)
                    }
                }

                val derivedMissingCandidates: List<String> = if (resumeEvidenceKeys.isNotEmpty()) { jdKeysUsed.filter { it !in resumeEvidenceKeys } } else { jdKeysUsed.filter { it !in backendMatchedKeys } }

                val finalMissingKeys: LinkedHashSet<String> = LinkedHashSet<String>().apply {
                    addAll(backendMissingKeys)
                    if (missingDesiredSize > 0) {
                        for (k in derivedMissingCandidates) { if (size >= missingDesiredSize) break; add(k) }
                        if (size < missingDesiredSize) {
                            for (k in jdKeysUsed) { if (size >= missingDesiredSize) break; if (k !in backendMatchedKeys) add(k) }
                        }
                    }
                }

                val finalMatchedKeys = LinkedHashSet<String>().apply {
                    for (k in jdKeysUsed) if (k !in finalMissingKeys) add(k)
                    if (jdKeysUsed.isEmpty()) { addAll(backendMatchedKeys) } else { for (k in backendMatchedKeys) if (k in jdKeysUsed) add(k) }
                }

                applyInference(finalMatchedKeys, missingFromBackend)

                for (k in finalMatchedKeys) { if (k !in missingFromBackend) finalMissingKeys.remove(k) }

                val matched = finalMatchedKeys.toList().sortedWith(compareBy({ skillPriority(it) }, { it })).map { prettySkill(it) }.distinctBy { it.lowercase() }.take(MAX_MATCHED_CHIPS)
                val missing = finalMissingKeys.toList().sortedWith(compareBy({ skillPriority(it) }, { it })).map { prettySkill(it) }.distinctBy { it.lowercase() }.take(MAX_MISSING_CHIPS)

                ResultsCard(isDark = isDark, atsScore = ats, jobScore = jobMatch, resumeWarning = resumeWarn, resumeLikelihood = resumeLikelihood, jdTotalCount = jdKeysUsed.size, matchedCount = parsed.matchedCount, missingCount = parsed.missingCount, matchedTopCount = parsed.matchedTop.size, missingTopCount = parsed.missingTop.size, matched = matched, missing = missing)
                SuggestionsCard(jobScore = jobMatch, atsScore = ats, matched = matched, missing = missing)
            } else if (rawJson != null) {
                val maybeErr = runCatching { JSONObject(rawJson!!).optString("detail") }.getOrNull()
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))) {
                    Column(modifier = Modifier.padding(16.dp)) { Text("Response received", fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(8.dp)); Text(text = if (!maybeErr.isNullOrBlank()) "Backend error: $maybeErr" else "Could not parse the backend response into match fields.", style = MaterialTheme.typography.bodyMedium) }
                }
            }
        }
    }
}

// -----------------------------
// UI components
// -----------------------------
@Composable
private fun JobOnlyResultsCard(isDark: Boolean, skills: List<String>) {
    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Skills found in JD", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            SkillsChips(isDark = isDark, chips = skills, positive = true)
        }
    }
}

@Composable
private fun JobOnlySuggestionsCard(skills: List<String>) {
    val suggestions = buildList {
        add("Mirror the top JD skills in your resume Skills section (only if honest).")
        add("Add 2 JD-aligned bullets in Projects/Experience proving these skills with metrics.")
        if (skills.isNotEmpty()) add("Top skills detected: ${skills.take(10).joinToString(", ")}.")
        add("Upload your resume PDF to calculate match score and missing skills.")
    }.distinct()
    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Next steps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            suggestions.forEach { Text("• $it") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResultsCard(isDark: Boolean, atsScore: Int, jobScore: Int, resumeWarning: String?, resumeLikelihood: Float, jdTotalCount: Int, matchedCount: Int, missingCount: Int, matchedTopCount: Int, missingTopCount: Int, matched: List<String>, missing: List<String>) {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var query by rememberSaveable { mutableStateOf("") }
    var grouped by rememberSaveable { mutableStateOf(false) }
    var showAllMatched by rememberSaveable { mutableStateOf(false) }
    var showAllMissing by rememberSaveable { mutableStateOf(false) }
    var showWhySheet by remember { mutableStateOf(false) }
    val q = query.trim().lowercase()
    val matchedFiltered = remember(matched, q) { if (q.isBlank()) matched else matched.filter { it.lowercase().contains(q) } }
    val missingFiltered = remember(missing, q) { if (q.isBlank()) missing else missing.filter { it.lowercase().contains(q) } }
    val matchedToShow = remember(matchedFiltered, showAllMatched, q) { if (q.isNotBlank() || showAllMatched) matchedFiltered else matchedFiltered.take(PREVIEW_CHIP_LIMIT) }
    val missingToShow = remember(missingFiltered, showAllMissing, q) { if (q.isNotBlank() || showAllMissing) missingFiltered else missingFiltered.take(PREVIEW_CHIP_LIMIT) }
    val jdCoverage = if (jdTotalCount > 0) { val cov = (jdTotalCount - missingCount).coerceIn(0, jdTotalCount); "$cov/$jdTotalCount" } else { "—" }
    val matchedIsTopList = matchedTopCount in 1 until matchedCount
    val missingIsTopList = missingTopCount in 1 until missingCount

    fun shareText(): String {
        val sb = StringBuilder()
        sb.appendLine("ResumeMatch results\nATS: $atsScore/100\nJob Match: $jobScore/100")
        if (jdTotalCount > 0) sb.appendLine("JD coverage: $jdCoverage")
        sb.appendLine("Matched: $matchedCount" + (if (matchedIsTopList) " (top $matchedTopCount shown by backend)" else ""))
        sb.appendLine("Missing: $missingCount" + (if (missingIsTopList) " (top $missingTopCount shown by backend)" else "") + "\n")
        if (matched.isNotEmpty()) sb.appendLine("Matched skills:\n${matched.joinToString(", ")}\n")
        if (missing.isNotEmpty()) sb.appendLine("Missing skills:\n${missing.joinToString(", ")}\n")
        if (!resumeWarning.isNullOrBlank()) sb.appendLine("Note: $resumeWarning")
        return sb.toString().trim()
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Results", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(10.dp))
                if (resumeLikelihood < 0.70f) ConfidenceBadge(likelihood = resumeLikelihood)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, shareText()) }; ctx.startActivity(Intent.createChooser(intent, "Share results")) }) { Icon(Icons.Default.Share, contentDescription = "Share") }
            }
            if (!resumeWarning.isNullOrBlank()) { Surface(color = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer, shape = RoundedCornerShape(12.dp)) { Text(resumeWarning, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall) } }
            ScoreBar(label = "ATS Readiness", score = atsScore, color = Color(0xFF42A5F5))
            ScoreBar(label = "Job Match", score = jobScore, color = Color(0xFF66BB6A))
            Text(text = "Matched: $matchedCount • Missing: $missingCount • JD coverage: $jdCoverage", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (matchedIsTopList || missingIsTopList) Text(text = "Note: Backend returns top skill lists; counts reflect the full estimate.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Search skills…") })
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { FilterChip(selected = grouped, onClick = { grouped = !grouped }, label = { Text(if (grouped) "Grouped" else "Compact") }) }

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Matched Skills", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                TextButton(onClick = { showAllMatched = !showAllMatched }) { Text(if (showAllMatched || q.isNotBlank()) "Top ${PREVIEW_CHIP_LIMIT}" else "All (${matchedFiltered.size})") }
            }
            if (matchedFiltered.isEmpty()) { Text("No matched skills detected from available signals.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } else {
                if (grouped) GroupedSkillsChips(isDark = isDark, chips = matchedToShow, positive = true) else SkillsChips(isDark = isDark, chips = matchedToShow, positive = true)
                if (!showAllMatched && q.isBlank() && matchedFiltered.size > PREVIEW_CHIP_LIMIT) TextButton(onClick = { showAllMatched = true }) { Text("Show all matched (${matchedFiltered.size})") }
            }

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Missing Skills", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                TextButton(onClick = { showWhySheet = true }) { Icon(Icons.Default.Info, contentDescription = "Why missing", modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Why?") }
                TextButton(onClick = { showAllMissing = !showAllMissing }) { Text(if (showAllMissing || q.isNotBlank()) "Top ${PREVIEW_CHIP_LIMIT}" else "All (${missingFiltered.size})") }
            }
            if (missingFiltered.isEmpty()) { Text("No missing skills detected.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } else {
                if (grouped) GroupedSkillsChips(isDark = isDark, chips = missingToShow, positive = false) else SkillsChips(isDark = isDark, chips = missingToShow, positive = false)
                if (!showAllMissing && q.isBlank() && missingFiltered.size > PREVIEW_CHIP_LIMIT) TextButton(onClick = { showAllMissing = true }) { Text("Show all missing (${missingFiltered.size})") }
                val topMissing = missingFiltered.firstOrNull()
                val suggested = topMissing?.let { suggestedBulletFor(it) }
                if (!suggested.isNullOrBlank()) {
                    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Suggested resume bullet", fontWeight = FontWeight.SemiBold)
                            Text(suggested, style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { TextButton(onClick = { clipboard.setText(AnnotatedString(suggested)) }) { Text("Copy") } }
                        }
                    }
                }
            }
        }
    }
    if (showWhySheet) {
        ModalBottomSheet(onDismissRequest = { showWhySheet = false }, shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("How 'Missing Skills' works", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("• Missing skills are JD skills not detected in the resume text extraction.", style = MaterialTheme.typography.bodyMedium)
                Text("• ATS scanners depend on exact wording — include key skills in Summary/Skills/Project bullets (honestly).", style = MaterialTheme.typography.bodyMedium)
                Text("• If your resume is image/table heavy, export a text-based PDF for better parsing.", style = MaterialTheme.typography.bodyMedium)
                if (matchedIsTopList || missingIsTopList) Text("• Backend returns top lists for UI; counts may exceed the list sizes.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Button(onClick = { showWhySheet = false }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(999.dp)) { Text("Got it") }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun SuggestionsCard(jobScore: Int, atsScore: Int, matched: List<String>, missing: List<String>) {
    val suggestions = remember(jobScore, atsScore, matched, missing) { buildStrongSuggestions(jobScore, atsScore, matched, missing) }
    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Suggestions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AssistChip(onClick = {}, label = { Text("Job Match: ${labelForScore(jobScore)}") })
                AssistChip(onClick = {}, label = { Text("ATS: ${labelForScore(atsScore)}") })
            }
            suggestions.forEach { Text("• $it") }
        }
    }
}

@Composable
private fun ScoreBar(label: String, score: Int, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text("$score/100", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        LinearProgressIndicator(progress = (score.coerceIn(0, 100) / 100f), modifier = Modifier.fillMaxWidth().height(10.dp), color = color, trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    }
}

// -----------------------------
// Chips layout
// -----------------------------
@Composable
private fun SkillsChips(isDark: Boolean, chips: List<String>, positive: Boolean) {
    if (chips.isEmpty()) return
    FlowWrap(modifier = Modifier.fillMaxWidth(), horizontalSpacing = 10.dp, verticalSpacing = 10.dp) {
        chips.forEach { chip -> SkillChip(isDark = isDark, label = chip, positive = positive) }
    }
}

@Composable
private fun FlowWrap(modifier: Modifier = Modifier, horizontalSpacing: Dp = 8.dp, verticalSpacing: Dp = 8.dp, content: @Composable () -> Unit) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val hSpacePx = with(density) { horizontalSpacing.toPx().toInt() }
    val vSpacePx = with(density) { verticalSpacing.toPx().toInt() }

    Layout(modifier = modifier, content = content) { measurables, constraints ->
        val maxWidth = constraints.maxWidth
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0)) }
        var x = 0; var y = 0; var lineHeight = 0
        val positions = ArrayList<Pair<Int, Int>>(placeables.size)

        placeables.forEach { p ->
            if (x != 0 && x + p.width > maxWidth) { x = 0; y += lineHeight + vSpacePx; lineHeight = 0 }
            positions.add(x to y)
            x += p.width + hSpacePx
            lineHeight = maxOf(lineHeight, p.height)
        }
        val height = (y + lineHeight).coerceIn(constraints.minHeight, constraints.maxHeight)

        layout(width = maxWidth, height = height) {
            placeables.forEachIndexed { i, p ->
                val (px, py) = positions[i]
                p.placeRelative(px, py)
            }
        }
    }
}

@Composable
private fun SkillChip(isDark: Boolean, label: String, positive: Boolean) {
    val bg = when {
        positive && isDark -> Color(0xFF0B3D2E).copy(alpha = 0.65f)
        !positive && isDark -> Color(0xFF4A1010).copy(alpha = 0.65f)
        positive && !isDark -> Color(0xFFD6F5EA).copy(alpha = 0.95f)
        else -> Color(0xFFFFE0E0).copy(alpha = 0.95f)
    }
    val border = when { positive -> Color(0xFF2ECC71).copy(alpha = 0.55f); else -> Color(0xFFE74C3C).copy(alpha = 0.55f) }
    val textColor = if (isDark) Color.White else Color(0xFF111111)

    Surface(shape = RoundedCornerShape(999.dp), color = bg, border = BorderStroke(1.dp, border)) {
        Text(text = label, modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp), color = textColor, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// -----------------------------
// UI Enhancements helpers
// -----------------------------
@Composable
private fun ConfidenceBadge(likelihood: Float) {
    val (label, color) = when {
        likelihood < 0.25f -> "Resume confidence: Low" to MaterialTheme.colorScheme.error
        likelihood < 0.45f -> "Resume confidence: Medium" to MaterialTheme.colorScheme.tertiary
        else -> "Resume confidence: Medium" to MaterialTheme.colorScheme.tertiary
    }
    AssistChip(onClick = {}, enabled = false, label = { Text(label) }, colors = AssistChipDefaults.assistChipColors(containerColor = color.copy(alpha = 0.18f), labelColor = MaterialTheme.colorScheme.onSurface))
}

@Composable
private fun GroupedSkillsChips(isDark: Boolean, chips: List<String>, positive: Boolean) {
    if (chips.isEmpty()) return
    val groups = remember(chips) { chips.groupBy { bucketOf(it) } }
    val order = listOf(Bucket.LANG, Bucket.API, Bucket.ANDROID, Bucket.CLOUD, Bucket.SYSTEMS, Bucket.AIML, Bucket.OTHER)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        order.forEach { b ->
            val list = groups[b].orEmpty()
            if (list.isNotEmpty()) {
                Text(text = bucketLabel(b), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                SkillsChips(isDark = isDark, chips = list, positive = positive)
            }
        }
    }
}

private fun bucketLabel(b: Bucket): String = when (b) { Bucket.LANG -> "Languages"; Bucket.API -> "Backend / APIs"; Bucket.ANDROID -> "Android"; Bucket.CLOUD -> "Cloud / DevOps"; Bucket.AIML -> "AI/ML"; Bucket.SYSTEMS -> "Systems"; Bucket.OTHER -> "Other" }

private fun suggestedBulletFor(skillLabel: String): String? {
    val s = skillLabel.lowercase().trim()
    return when {
        s.contains("multipart") -> "• Implemented multipart/form-data file upload for PDF resumes in FastAPI, with validation and robust error handling."
        s.contains("swagger") || s.contains("openapi") -> "• Designed REST endpoints in FastAPI and documented API contracts using Swagger/OpenAPI (interactive docs)."
        s == "rest" || s.contains("rest api") -> "• Built and maintained REST APIs with clear request/response schemas, status codes, and structured error responses."
        s.contains("uvicorn") -> "• Deployed FastAPI services with Uvicorn, tuning timeouts and logging for production-style debugging."
        s.contains("pypdf") || s.contains("pdf") -> "• Implemented PDF parsing and text extraction using PyPDF, handling noisy outputs with normalization."
        s.contains("regex") -> "• Built a regex-driven text normalization pipeline to clean and standardize extracted resume/JD text."
        s.contains("render") -> "• Deployed Dockerized FastAPI services to Render free tier and monitored failures/timeouts with retries."
        s.contains("postman") -> "• Used Postman to test and debug API workflows (auth, file uploads, error cases) with saved collections."
        s == "python" -> "• Built API-first backend services in Python using FastAPI with clean architecture and testable components."
        s.contains("docker") -> "• Containerized the backend using Docker for consistent local and cloud deployments."
        else -> null
    }
}