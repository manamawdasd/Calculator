package com.example.data

data class CurrencyInfo(
    val code: String,
    val name: String,
    val symbol: String,
    val flag: String,
    val decimals: Int = 2
)

object CurrencyCatalog {

    val allCurrencies: List<CurrencyInfo> = listOf(
        CurrencyInfo("IDR", "Rupiah Indonesia", "Rp", "🇮🇩", decimals = 0),
        CurrencyInfo("USD", "Dolar Amerika Serikat", "$", "🇺🇸", decimals = 2),
        CurrencyInfo("EUR", "Euro", "€", "🇪🇺", decimals = 2),
        CurrencyInfo("SGD", "Dolar Singapura", "S$", "🇸🇬", decimals = 2),
        CurrencyInfo("MYR", "Ringgit Malaysia", "RM", "🇲🇾", decimals = 2),
        CurrencyInfo("JPY", "Yen Jepang", "¥", "🇯🇵", decimals = 0),
        CurrencyInfo("GBP", "Poundsterling Inggris", "£", "🇬🇧", decimals = 2),
        CurrencyInfo("AUD", "Dolar Australia", "A$", "🇦🇺", decimals = 2),
        CurrencyInfo("CNY", "Yuan Tiongkok", "¥", "🇨🇳", decimals = 2),
        CurrencyInfo("KRW", "Won Korea Selatan", "₩", "🇰🇷", decimals = 0),
        CurrencyInfo("SAR", "Riyal Arab Saudi", "﷼", "🇸🇦", decimals = 2),
        CurrencyInfo("THB", "Baht Thailand", "฿", "🇹🇭", decimals = 2),
        CurrencyInfo("CHF", "Franc Swiss", "CHF", "🇨🇭", decimals = 2),
        CurrencyInfo("CAD", "Dolar Kanada", "C$", "🇨🇦", decimals = 2),
        CurrencyInfo("HKD", "Dolar Hong Kong", "HK$", "🇭🇰", decimals = 2),
        CurrencyInfo("INR", "Rupee India", "₹", "🇮🇳", decimals = 2),
        CurrencyInfo("PHP", "Peso Filipina", "₱", "🇵🇭", decimals = 2),
        CurrencyInfo("VND", "Dong Vietnam", "₫", "🇻🇳", decimals = 0),
        CurrencyInfo("BND", "Dolar Brunei", "B$", "🇧🇳", decimals = 2),
        CurrencyInfo("NZD", "Dolar Selandia Baru", "NZ$", "🇳🇿", decimals = 2),
        CurrencyInfo("TWD", "Dolar Baru Taiwan", "NT$", "🇹🇼", decimals = 2),
        CurrencyInfo("AED", "Dirham UEA", "د.إ", "🇦🇪", decimals = 2),
        CurrencyInfo("TRY", "Lira Turki", "₺", "🇹🇷", decimals = 2),
        CurrencyInfo("BRL", "Real Brasil", "R$", "🇧🇷", decimals = 2),
        CurrencyInfo("MXN", "Peso Meksiko", "Mex$", "🇲🇽", decimals = 2),
        CurrencyInfo("SEK", "Krona Swedia", "kr", "🇸🇪", decimals = 2),
        CurrencyInfo("NOK", "Krone Norwegia", "kr", "🇳🇴", decimals = 2),
        CurrencyInfo("DKK", "Krone Denmark", "kr", "🇩🇰", decimals = 2),
        CurrencyInfo("PLN", "Zloty Polandia", "zł", "🇵🇱", decimals = 2),
        CurrencyInfo("ZAR", "Rand Afrika Selatan", "R", "🇿🇦", decimals = 2),
        CurrencyInfo("EGP", "Pound Mesir", "E£", "🇪🇬", decimals = 2),
        CurrencyInfo("QAR", "Riyal Qatar", "﷼", "🇶🇦", decimals = 2)
    )

    private val byCode = allCurrencies.associateBy { it.code }

    fun find(code: String): CurrencyInfo =
        byCode[code] ?: CurrencyInfo(code = code, name = code, symbol = code, flag = "🌐")

    // Bundled reference rates (base: USD = 1.0) used only until live rates or cached rates load
    val fallbackUsdRates: Map<String, Double> = mapOf(
        "USD" to 1.0,
        "IDR" to 15850.0,
        "EUR" to 0.92,
        "SGD" to 1.34,
        "MYR" to 4.45,
        "JPY" to 154.0,
        "GBP" to 0.79,
        "AUD" to 1.53,
        "CNY" to 7.22,
        "KRW" to 1395.0,
        "SAR" to 3.75,
        "THB" to 34.6,
        "CHF" to 0.88,
        "CAD" to 1.39,
        "HKD" to 7.78,
        "INR" to 84.4,
        "PHP" to 58.6,
        "VND" to 25350.0,
        "BND" to 1.34,
        "NZD" to 1.68,
        "TWD" to 32.4,
        "AED" to 3.67,
        "TRY" to 34.3,
        "BRL" to 5.78,
        "MXN" to 20.3,
        "SEK" to 10.9,
        "NOK" to 11.0,
        "DKK" to 6.87,
        "PLN" to 4.07,
        "ZAR" to 18.0,
        "EGP" to 49.2,
        "QAR" to 3.64
    )
}
