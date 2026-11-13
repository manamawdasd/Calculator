package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

private val Context.kalkuDataStore: DataStore<Preferences> by preferencesDataStore(name = "kalku_prefs")

enum class RateSource {
    LIVE,
    CACHED,
    FALLBACK
}

data class CalcHistoryItem(
    val id: Long,
    val expression: String,
    val result: String,
    val plainResult: String
)

data class CachedRatesSnapshot(
    val rates: Map<String, Double>,
    val updatedAtMillis: Long?,
    val source: RateSource
)

class CurrencyRepository(private val context: Context) {

    private object Keys {
        val RATES_JSON = stringPreferencesKey("cached_rates_json")
        val RATES_UPDATED_AT = longPreferencesKey("rates_updated_at")
        val FROM_CURRENCY = stringPreferencesKey("from_currency")
        val TO_CURRENCY = stringPreferencesKey("to_currency")
        val DARK_THEME = booleanPreferencesKey("dark_theme_override")
        val HAS_DARK_OVERRIDE = booleanPreferencesKey("has_dark_override")
        val HISTORY_JSON = stringPreferencesKey("calc_history_json")
    }

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val ratesMapAdapter = moshi.adapter<Map<String, Double>>(
        Types.newParameterizedType(Map::class.java, String::class.java, Double::class.javaObjectType)
    )

    private val historyListAdapter = moshi.adapter<List<CalcHistoryItem>>(
        Types.newParameterizedType(List::class.java, CalcHistoryItem::class.java)
    )

    private val api: ExchangeRateApi by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl("https://open.er-api.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ExchangeRateApi::class.java)
    }

    val selectedPairFlow: Flow<Pair<String, String>> = context.kalkuDataStore.data.map { prefs ->
        val from = prefs[Keys.FROM_CURRENCY] ?: "USD"
        val to = prefs[Keys.TO_CURRENCY] ?: "IDR"
        from to to
    }

    val darkThemeOverrideFlow: Flow<Boolean?> = context.kalkuDataStore.data.map { prefs ->
        if (prefs[Keys.HAS_DARK_OVERRIDE] == true) prefs[Keys.DARK_THEME] else null
    }

    val historyFlow: Flow<List<CalcHistoryItem>> = context.kalkuDataStore.data.map { prefs ->
        val raw = prefs[Keys.HISTORY_JSON] ?: return@map emptyList()
        runCatching { historyListAdapter.fromJson(raw) }.getOrNull().orEmpty()
    }

    suspend fun loadInitialRates(): CachedRatesSnapshot {
        val prefs = context.kalkuDataStore.data.first()
        val cachedJson = prefs[Keys.RATES_JSON]
        val updatedAt = prefs[Keys.RATES_UPDATED_AT]
        if (!cachedJson.isNullOrBlank()) {
            val parsed = runCatching { ratesMapAdapter.fromJson(cachedJson) }.getOrNull()
            if (!parsed.isNullOrEmpty()) {
                return CachedRatesSnapshot(
                    rates = parsed,
                    updatedAtMillis = updatedAt,
                    source = RateSource.CACHED
                )
            }
        }
        return CachedRatesSnapshot(
            rates = CurrencyCatalog.fallbackUsdRates,
            updatedAtMillis = null,
            source = RateSource.FALLBACK
        )
    }

    suspend fun fetchLiveRates(): Result<CachedRatesSnapshot> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.getLatestUsdRates()
            val remoteRates = response.rates
            if (response.result != "success" || remoteRates.isNullOrEmpty()) {
                error("Gagal memuat kurs mata uang")
            }
            val nowMillis = System.currentTimeMillis()
            val json = ratesMapAdapter.toJson(remoteRates)
            context.kalkuDataStore.edit { prefs ->
                prefs[Keys.RATES_JSON] = json
                prefs[Keys.RATES_UPDATED_AT] = nowMillis
            }
            CachedRatesSnapshot(
                rates = remoteRates,
                updatedAtMillis = nowMillis,
                source = RateSource.LIVE
            )
        }
    }

    suspend fun saveCurrencyPair(fromCode: String, toCode: String) {
        context.kalkuDataStore.edit { prefs ->
            prefs[Keys.FROM_CURRENCY] = fromCode
            prefs[Keys.TO_CURRENCY] = toCode
        }
    }

    suspend fun saveDarkThemeOverride(isDark: Boolean) {
        context.kalkuDataStore.edit { prefs ->
            prefs[Keys.HAS_DARK_OVERRIDE] = true
            prefs[Keys.DARK_THEME] = isDark
        }
    }

    suspend fun saveHistory(items: List<CalcHistoryItem>) {
        val trimmed = items.take(30)
        val json = historyListAdapter.toJson(trimmed)
        context.kalkuDataStore.edit { prefs ->
            prefs[Keys.HISTORY_JSON] = json
        }
    }
}
