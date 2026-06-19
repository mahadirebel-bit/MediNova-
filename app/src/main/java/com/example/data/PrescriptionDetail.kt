package com.example.data

import org.json.JSONObject

data class PrescriptionDetail(
    val doctorName: String,
    val doctorSpecialty: String,
    val date: String,
    val medicines: List<MedicineDetail>,
    val recommendations: List<RecommendationDetail>,
    val symptomsAndDiagnosis: String,
    val followUpDate: String,
    val adviceEng: String,
    val adviceBen: String,
    val adviceHi: String = "",
    val adviceEs: String = "",
    val adviceAr: String = "",
    val adviceFr: String = ""
)

data class MedicineDetail(
    val name: String,
    val dosage: String,
    val frequency: String,
    val timing: String,
    val duration: String,
    val purposeBen: String,
    val purposeEng: String,
    val purposeHi: String = "",
    val purposeEs: String = "",
    val purposeAr: String = "",
    val purposeFr: String = ""
)

data class RecommendationDetail(
    val title: String,
    val descBen: String,
    val descEng: String,
    val descHi: String = "",
    val descEs: String = "",
    val descAr: String = "",
    val descFr: String = ""
)

object PrescriptionParser {
    fun parseJson(rawJson: String): PrescriptionDetail? {
        return try {
            val json = JSONObject(rawJson)
            val doctorName = json.optString("doctorName", "Unknown Doctor")
            val doctorSpecialty = json.optString("doctorSpecialty", "Physician")
            val date = json.optString("date", "")
            val symptomsAndDiagnosis = json.optString("symptomsAndDiagnosis", "")
            val followUpDate = json.optString("followUpDate", "")
            val adviceEng = json.optString("adviceEng", "")
            val adviceBen = json.optString("adviceBen", "")
            val adviceHi = json.optString("adviceHi", "")
            val adviceEs = json.optString("adviceEs", "")
            val adviceAr = json.optString("adviceAr", "")
            val adviceFr = json.optString("adviceFr", "")

            val medicines = mutableListOf<MedicineDetail>()
            val medsArray = json.optJSONArray("medicines")
            if (medsArray != null) {
                for (i in 0 until medsArray.length()) {
                    val m = medsArray.getJSONObject(i)
                    medicines.add(
                        MedicineDetail(
                            name = m.optString("name", m.optString("parameter", "Unknown")),
                            dosage = m.optString("dosage", ""),
                            frequency = m.optString("frequency", ""),
                            timing = m.optString("timing", ""),
                            duration = m.optString("duration", ""),
                            purposeBen = m.optString("purposeBen", ""),
                            purposeEng = m.optString("purposeEng", ""),
                            purposeHi = m.optString("purposeHi", ""),
                            purposeEs = m.optString("purposeEs", ""),
                            purposeAr = m.optString("purposeAr", ""),
                            purposeFr = m.optString("purposeFr", "")
                        )
                    )
                }
            }

            val recommendations = mutableListOf<RecommendationDetail>()
            val recsArray = json.optJSONArray("recommendations")
            if (recsArray != null) {
                for (i in 0 until recsArray.length()) {
                    val r = recsArray.getJSONObject(i)
                    recommendations.add(
                        RecommendationDetail(
                            title = r.optString("title", ""),
                            descBen = r.optString("descBen", ""),
                            descEng = r.optString("descEng", ""),
                            descHi = r.optString("descHi", ""),
                            descEs = r.optString("descEs", ""),
                            descAr = r.optString("descAr", ""),
                            descFr = r.optString("descFr", "")
                        )
                    )
                }
            }

            PrescriptionDetail(
                doctorName,
                doctorSpecialty,
                date,
                medicines,
                recommendations,
                symptomsAndDiagnosis,
                followUpDate,
                adviceEng,
                adviceBen,
                adviceHi,
                adviceEs,
                adviceAr,
                adviceFr
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
