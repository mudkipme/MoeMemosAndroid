package me.mudkip.moememos.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * In-app language preference.
 *
 * The default is [AppLanguage.SYSTEM]: no locale override is applied, so Android
 * keeps resolving resources from the system language (including the system
 * per-app language setting on Android 13+), exactly as before this setting
 * existed. Once the user picks a language, the choice is persisted and takes
 * precedence.
 *
 * Kept in SharedPreferences (rather than DataStore) because the value has to be
 * read synchronously from [android.app.Application.attachBaseContext], before
 * any application Context or UI has been created.
 */
data class LanguageOption(
    val tag: String,
    /** The language name written in that language itself; never translated. */
    val displayName: String,
)

object AppLanguage {
    const val SYSTEM = "system"
    const val ENGLISH = "en"
    const val SIMPLIFIED_CHINESE = "zh-Hans"

    private const val PREFS_NAME = "app_language"
    private const val KEY_LANGUAGE = "language_tag"

    /** Every locale declared in res/xml/locales_config.xml and the values-* resources. */
    val options = listOf(
        LanguageOption(ENGLISH, "English"),
        LanguageOption(SIMPLIFIED_CHINESE, "简体中文"),
        LanguageOption("zh-Hant", "繁體中文"),
        LanguageOption("ru", "Русский"),
        LanguageOption("uk", "Українська"),
        LanguageOption("fr", "Français"),
        LanguageOption("tr", "Türkçe"),
        LanguageOption("it", "Italiano"),
        LanguageOption("de", "Deutsch"),
        LanguageOption("ja", "日本語"),
        LanguageOption("pt-BR", "Português (Brasil)"),
        LanguageOption("vi", "Tiếng Việt"),
    )

    private val tags = options.map { it.tag }

    @Volatile
    private var currentTag: String = SYSTEM

    @Volatile
    private var cachedLocalizedContext: Context? = null

    /** Called once from Application.attachBaseContext, before any UI is created. */
    fun initialize(context: Context) {
        val saved = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, null)
        currentTag = if (saved != null && saved in tags) saved else SYSTEM
        cachedLocalizedContext = null
    }

    fun currentTag(): String = currentTag

    /** Persists the choice (or clears it for [SYSTEM]) and makes it effective immediately. */
    fun setLanguage(context: Context, tag: String) {
        if (tag != SYSTEM && tag !in tags) return
        if (tag == currentTag) return
        val editor = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
        if (tag == SYSTEM) editor.remove(KEY_LANGUAGE) else editor.putString(KEY_LANGUAGE, tag)
        editor.commit()
        currentTag = tag
        cachedLocalizedContext = null
    }

    /**
     * Returns a Context whose resources resolve against the selected language.
     * In [SYSTEM] mode the Context is returned untouched, so a system language
     * change still updates resources and recreates activities as usual.
     */
    fun wrap(context: Context): Context {
        if (currentTag == SYSTEM) return context
        val configuration = Configuration(context.resources.configuration)
        val locale = localeFor(currentTag)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        return context.createConfigurationContext(configuration)
    }

    /** Context used by the `R.string.xxx.string` extension; rebuilt after a language change. */
    fun localized(context: Context): Context {
        if (currentTag == SYSTEM) return context.applicationContext ?: context
        cachedLocalizedContext?.let { return it }
        return synchronized(this) {
            cachedLocalizedContext ?: wrap(context.applicationContext ?: context)
                .also { cachedLocalizedContext = it }
        }
    }

    private fun localeFor(tag: String): Locale = when (tag) {
        SIMPLIFIED_CHINESE -> Locale.SIMPLIFIED_CHINESE
        "zh-Hant" -> Locale.TRADITIONAL_CHINESE
        "pt-BR" -> Locale.forLanguageTag("pt-BR")
        else -> Locale.forLanguageTag(tag)
    }
}
