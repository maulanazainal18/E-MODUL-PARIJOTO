package com.example.data.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class AiEvidenceAnalysis(
    val ringkasanIlmiah: String,
    val tingkatBukti: String,
    val senyawaDanMekanisme: String,
    val dampakKonservasi: String,
    val catatanEtisMedis: String
)

class ParijotoAiRepository {

    companion object {
        private const val TAG = "ParijotoAiRepo"
        private const val MODEL_NAME = "gemini-3.8-flash"
        private const val TIMEOUT_MS = 60_000L

        private val SYSTEM_INSTRUCTION = """
            Anda adalah Asisten Riset Biologi SMA Kelas X untuk aplikasi PARIJOTO LEARNING,
            berdasarkan E-Modul "Keanekaragaman Hayati: Parijoto Eksplorasi Etnosains" karya Ahzami, S.Pd., Gr. (2026).
            Aturan ilmiah WAJIB:
            1. Jawab dalam Bahasa Indonesia yang jelas, ramah siswa SMA Fase E, dan akurat secara biologi.
            2. Bedakan secara tegas antara bukti praklinis (in-vivo pada hewan uji / in-vitro), hasil komputasi (in-silico, misal penghambatan PP1 oleh flavonoid), dan bukti klinis pada manusia.
            3. JANGAN PERNAH menyatakan Parijoto (Medinilla speciosa) sebagai obat yang terbukti menyembuhkan penyakit atau menjamin kesehatan kehamilan manusia. Jelaskan bahwa penelitian fertilitas β-sitosterol & stigmasterol baru pada tahap praklinis hewan uji, sehingga buah ini dipahami sebagai pangan/nutrisi alami pendukung, bukan obat mutlak.
            4. Jangan mengarang angka penelitian atau sitasi palsu. Jika tidak tahu, nyatakan keterbatasan bukti secara jujur.
            5. Kaitkan penjelasan dengan konsep biologi SMA: keanekaragaman gen/spesies/ekosistem, epifit, fotoinhibisi, plasmolisis/desikasi, ornitokori, kultur jaringan (totipotensi), Indikasi Geografis, atau argumen CER.
        """.trimIndent()
    }

    private val chatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            systemInstruction = content { text(SYSTEM_INSTRUCTION) }
        )
    }

    private val structuredSchema = Schema.obj(
        properties = mapOf(
            "ringkasanIlmiah" to Schema.string("Ringkasan konsep biologi yang menjawab pertanyaan siswa."),
            "tingkatBukti" to Schema.string("Klasifikasi bukti: Praklinis / Komputasi / Ekologi Lapangan / Belum Ada Uji Klinis Manusia."),
            "senyawaDanMekanisme" to Schema.string("Penjelasan senyawa bioaktif atau mekanisme fisiologis seluler terkait."),
            "dampakKonservasi" to Schema.string("Keterkaitan dengan konservasi in-situ, ex-situ, atau Indikasi Geografis Gunung Muria."),
            "catatanEtisMedis" to Schema.string("Pengingat kritis bahwa Parijoto bukan obat penyembuh mutlak atau penjamin kehamilan.")
        )
    )

    private val structuredModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = structuredSchema
            },
            systemInstruction = content { text(SYSTEM_INSTRUCTION) }
        )
    }

    fun streamBiologyTutor(question: String, lessonContext: String): Flow<String> = flow {
        val prompt = buildString {
            if (lessonContext.isNotBlank()) {
                append("Konteks Materi Saat Ini:\n$lessonContext\n\n")
            }
            append("Pertanyaan / Topik Penyelidikan Siswa:\n$question")
        }
        val sb = StringBuilder()
        withTimeout(TIMEOUT_MS) {
            chatModel.generateContentStream(prompt).collect { chunk ->
                val part = chunk.text.orEmpty()
                if (part.isNotEmpty()) {
                    sb.append(part)
                    emit(sb.toString())
                }
            }
        }
        if (sb.isBlank()) {
            throw IllegalStateException("Asisten AI tidak mengembalikan jawaban.")
        }
    }

    suspend fun analyzeEvidenceStructured(query: String): Result<AiEvidenceAnalysis> {
        return try {
            val response = withTimeout(TIMEOUT_MS) {
                structuredModel.generateContent(
                    "Bantu siswa menganalisis bukti ilmiah berikut untuk lembar E-LKM PBL Fase 3:\n$query"
                )
            }
            val raw = response.text ?: throw IllegalStateException("Respons AI kosong.")
            val json = JSONObject(raw)
            Result.success(
                AiEvidenceAnalysis(
                    ringkasanIlmiah = json.getString("ringkasanIlmiah"),
                    tingkatBukti = json.getString("tingkatBukti"),
                    senyawaDanMekanisme = json.getString("senyawaDanMekanisme"),
                    dampakKonservasi = json.getString("dampakKonservasi"),
                    catatanEtisMedis = json.getString("catatanEtisMedis")
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI analysis timed out", e)
            Result.failure(Exception("Permintaan analisis AI melebihi batas waktu 60 detik. Coba lagi."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI analysis failed", e)
            Result.failure(e)
        }
    }
}
