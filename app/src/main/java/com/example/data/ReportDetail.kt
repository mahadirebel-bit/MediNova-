package com.example.data

import org.json.JSONObject

data class ReportDetail(
    val reportType: String,
    val detectedParameters: List<ParameterDetail>,
    val summaryEng: String,
    val summaryBen: String,
    val summaryHi: String = "",
    val summaryEs: String = "",
    val summaryAr: String = "",
    val summaryFr: String = ""
)

data class ParameterDetail(
    val name: String,
    val value: String,
    val unit: String,
    val referenceRange: String,
    val status: String, // HIGH, LOW, NORMAL
    val explanationEng: String,
    val explanationBen: String,
    val explanationHi: String = "",
    val explanationEs: String = "",
    val explanationAr: String = "",
    val explanationFr: String = ""
) {
    val isNormal: Boolean get() = status.equals("NORMAL", ignoreCase = true)
    val isHigh: Boolean get() = status.equals("HIGH", ignoreCase = true)
    val isLow: Boolean get() = status.equals("LOW", ignoreCase = true)
}

object ReportParser {
    fun parseJson(rawJson: String): ReportDetail? {
        return try {
            val json = JSONObject(rawJson)
            val reportType = json.optString("reportType", "Unknown")
            val summaryEng = json.optString("summaryEng", "")
            val summaryBen = json.optString("summaryBen", "")
            val summaryHi = json.optString("summaryHi", "")
            val summaryEs = json.optString("summaryEs", "")
            val summaryAr = json.optString("summaryAr", "")
            val summaryFr = json.optString("summaryFr", "")
            
            val parameters = mutableListOf<ParameterDetail>()
            val paramsArray = json.optJSONArray("detectedParameters")
            if (paramsArray != null) {
                for (i in 0 until paramsArray.length()) {
                    val p = paramsArray.getJSONObject(i)
                    parameters.add(
                        ParameterDetail(
                            name = p.optString("name", p.optString("parameter", "Unknown")),
                            value = p.optString("value", ""),
                            unit = p.optString("unit", ""),
                            referenceRange = p.optString("referenceRange", ""),
                            status = p.optString("status", "NORMAL").uppercase(),
                            explanationEng = p.optString("explanationEng", ""),
                            explanationBen = p.optString("explanationBen", ""),
                            explanationHi = p.optString("explanationHi", ""),
                            explanationEs = p.optString("explanationEs", ""),
                            explanationAr = p.optString("explanationAr", ""),
                            explanationFr = p.optString("explanationFr", "")
                        )
                    )
                }
            }
            ReportDetail(reportType, parameters, summaryEng, summaryBen, summaryHi, summaryEs, summaryAr, summaryFr)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
