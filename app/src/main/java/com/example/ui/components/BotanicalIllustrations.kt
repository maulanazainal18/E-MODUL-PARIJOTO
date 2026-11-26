package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.ForestGreenDark
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.LightSageGreen
import com.example.ui.theme.ParijotoPurple
import com.example.ui.theme.ParijotoPurpleLight

@Composable
fun ParijotoBotanicalBanner(
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(if (compact) 110.dp else 156.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(ForestGreenDark, ForestGreen, ParijotoPurple)
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Soft botanical arcs
            drawCircle(
                color = LightSageGreen.copy(alpha = 0.16f),
                radius = h * 0.85f,
                center = Offset(w * 0.88f, h * 0.2f)
            )
            drawCircle(
                color = GoldAccent.copy(alpha = 0.14f),
                radius = h * 0.55f,
                center = Offset(w * 0.94f, h * 0.82f)
            )

            // Arched Parijoto leaves
            val leafPath = Path().apply {
                moveTo(w * 0.72f, h * 0.85f)
                quadraticTo(w * 0.82f, h * 0.12f, w * 0.96f, h * 0.35f)
                quadraticTo(w * 0.86f, h * 0.78f, w * 0.72f, h * 0.85f)
            }
            drawPath(leafPath, color = LightSageGreen.copy(alpha = 0.38f))

            // Hanging berry panicle stem
            drawLine(
                color = ParijotoPurpleLight,
                start = Offset(w * 0.82f, h * 0.14f),
                end = Offset(w * 0.86f, h * 0.72f),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )

            // Berry cluster circles
            val berries = listOf(
                Offset(w * 0.79f, h * 0.38f) to 11f,
                Offset(w * 0.85f, h * 0.36f) to 12f,
                Offset(w * 0.81f, h * 0.50f) to 13f,
                Offset(w * 0.88f, h * 0.49f) to 11f,
                Offset(w * 0.83f, h * 0.63f) to 12f,
                Offset(w * 0.89f, h * 0.62f) to 10f,
                Offset(w * 0.86f, h * 0.74f) to 9f
            )
            berries.forEach { (center, r) ->
                drawCircle(color = ParijotoPurpleContainerTint, radius = r, center = center)
                drawCircle(
                    color = GoldAccent.copy(alpha = 0.7f),
                    radius = r * 0.32f,
                    center = Offset(center.x - r * 0.25f, center.y - r * 0.25f)
                )
            }
        }
    }
}

@Composable
fun ModuleHeroArtwork(
    moduleNumber: Int,
    modifier: Modifier = Modifier
) {
    val drawableRes = if (moduleNumber == 2) R.drawable.img_parijoto_modul2 else R.drawable.img_parijoto_modul1
    val caption = if (moduleNumber == 2) {
        "Ilustrasi Konservasi In-Situ (Ornitokori Muria) & Ex-Situ (Kultur Jaringan)"
    } else {
        "Ilustrasi Botani Epifit Medinilla speciosa — Lereng Gunung Muria"
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(148.dp)
            .clip(RoundedCornerShape(14.dp))
    ) {
        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = caption,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.62f))
                    )
                )
        )
        Text(
            text = caption,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

private val ParijotoPurpleContainerTint = Color(0xFFE598CA)

@Composable
fun LessonIllustrationCard(
    illustrationType: String?,
    modifier: Modifier = Modifier
) {
    if (illustrationType.isNullOrBlank()) return
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            when (illustrationType) {
                "IG_FLAVONOID_CHART", "GEOGRAPHICAL_INDICATION_CHART" -> {
                    Text(
                        text = "Grafik Perbandingan Kadar Flavonoid Total (Vifta dkk., 2021)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(10.dp))
                    ComparisonBarRow("Lereng Gunung Muria (Kudus)", 81.60f, 100f, "81,60 mg QE/g", ParijotoPurple)
                    Spacer(Modifier.height(8.dp))
                    ComparisonBarRow("Pegunungan Kab. Semarang", 76.48f, 100f, "76,48 mg QE/g", ForestGreen)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Cekaman abiotik spesifik Lereng Muria memacu akumulasi metabolit sekunder optimal -> dasar perlindungan Indikasi Geografis (IG).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                "ECOLOGICAL_TREND_CHART", "PARADOKS_CHART", "DEFORESTATION_TREND", "CONSERVATION_PARADOX" -> {
                    Text(
                        text = "Tren Ekologi Epifit Muria (2019–2023)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(Modifier.height(10.dp))
                    ComparisonBarRow("2019 (Kanopi 479 ha • Lembap 84%)", 42f, 50f, "42 rumpun/ha", ForestGreen)
                    Spacer(Modifier.height(6.dp))
                    ComparisonBarRow("2021 (Kanopi 355 ha • Lembap 69%)", 27f, 50f, "27 rumpun/ha", GoldAccent)
                    Spacer(Modifier.height(6.dp))
                    ComparisonBarRow("2023 (Kanopi 240 ha • Lembap 52%)", 11f, 50f, "11 rumpun/ha", ParijotoPurple)
                }

                "EVIDENCE_PYRAMID", "SCIENTIFIC_EVIDENCE_LEVELS" -> {
                    Text(
                        text = "Hierarki Validasi Bukti Ilmiah (Etnosains -> Sains)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    EvidenceTierChip("1. Pengetahuan Tradisional (Mitoni/Mapati)", "Hipotesis budaya masyarakat Muria")
                    EvidenceTierChip("2. Uji Komputasi (In-Silico)", "Simulasi ikatan molekul (misal Flavonoid-PP1)")
                    EvidenceTierChip("3. Uji Praklinis (In-Vivo Hewan Uji)", "β-sitosterol & stigmasterol stimulasi LH/FSH")
                    EvidenceTierChip("4. Uji Klinis Manusia (Belum Tersedia)", "Bukan obat penyembuh mutlak atau penjamin kehamilan")
                }

                "TISSUE_CULTURE", "CONSERVATION_INSITU", "ORNITHOCORY", "OVER_HARVESTING", "BIOPROSPECTING", "BIOECONOMY_PRODUCTS", "CER_CAMPAIGN" -> {
                    Text(
                        text = "Visualisasi Pelestarian & Budidaya Parijoto Muria",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    ModuleHeroArtwork(moduleNumber = 2)
                }

                "PARIJOTO_PLANT", "PARIJOTO_MORPHOLOGY", "EPIPHYTE_HABITAT", "ECOSYSTEM_MURIA", "MITONI_CULTURE", "ETHNOSCIENCE_BRIDGE", "TRADITIONAL_USE", "BIODIVERSITY_LEVELS", "GENETIC_VARIATION", "SPECIES_COMPARISON" -> {
                    Text(
                        text = "Ilustrasi Botani: Medinilla speciosa (Parijoto Muria)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    ModuleHeroArtwork(moduleNumber = 1)
                }

                "OSMOSIS_PLASMOLYSIS" -> {
                    Text(
                        text = "Diagram Respon Seluler Epifit terhadap Defisit Air",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CellStateBox(
                            title = "Turgid (Naungan 84%)",
                            detail = "Vakuola penuh, sel sehat",
                            fillColor = ForestGreen,
                            innerScale = 0.82f,
                            modifier = Modifier.weight(1f)
                        )
                        CellStateBox(
                            title = "Plasmolisis (52%)",
                            detail = "Membran lepas dari dinding",
                            fillColor = ParijotoPurple,
                            innerScale = 0.45f,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                "PHOTOINHIBITION_DIAGRAM" -> {
                    Text(
                        text = "Alur Stres Cahaya Berlebih (Fotoinhibisi -> ROS)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(Modifier.height(8.dp))
                    EvidenceTierChip("Pohon Inang Ditebang", "Hilangnya naungan mikroklimat hutan")
                    EvidenceTierChip("Paparan Cahaya & Suhu Tinggi (+6,9°C)", "Kerusakan Fotosistem II (PSII)")
                    EvidenceTierChip("Akumulasi ROS & Klorosis", "Daun menguning/kemerahan, fotosintesis turun")
                }

                else -> {
                    Text(
                        text = "Ilustrasi Botani: Medinilla speciosa (Parijoto Muria)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(92.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CreamBackground)
                    ) {
                        val w = size.width
                        val h = size.height
                        // Stem & Epifit host bark
                        drawRoundRect(
                            color = Color(0xFF7B6754),
                            topLeft = Offset(18f, h * 0.25f),
                            size = Size(w * 0.16f, h * 0.65f),
                            cornerRadius = CornerRadius(12f, 12f)
                        )
                        // Leaves with 3-5 curved veins
                        val leaf = Path().apply {
                            moveTo(w * 0.18f, h * 0.48f)
                            quadraticTo(w * 0.36f, h * 0.10f, w * 0.56f, h * 0.35f)
                            quadraticTo(w * 0.38f, h * 0.62f, w * 0.18f, h * 0.48f)
                        }
                        drawPath(leaf, ForestGreen)
                        drawPath(
                            leaf,
                            color = LightSageGreen,
                            style = Stroke(width = 2.5f)
                        )
                        // Hanging berry panicle
                        drawLine(
                            color = ParijotoPurple,
                            start = Offset(w * 0.52f, h * 0.36f),
                            end = Offset(w * 0.72f, h * 0.78f),
                            strokeWidth = 4f
                        )
                        listOf(
                            Offset(w * 0.62f, h * 0.50f),
                            Offset(w * 0.69f, h * 0.48f),
                            Offset(w * 0.66f, h * 0.64f),
                            Offset(w * 0.74f, h * 0.63f),
                            Offset(w * 0.72f, h * 0.77f)
                        ).forEach { pt ->
                            drawCircle(ParijotoPurple, radius = 9f, center = pt)
                            drawCircle(Color.White.copy(alpha = 0.5f), radius = 2.5f, center = Offset(pt.x - 2f, pt.y - 2f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparisonBarRow(
    label: String,
    value: Float,
    maxValue: Float,
    valueText: String,
    barColor: Color
) {
    val fraction = (value / maxValue).coerceIn(0.05f, 1f)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            Text(valueText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = barColor)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(10.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(barColor)
            )
        }
    }
}

@Composable
private fun EvidenceTierChip(title: String, subtitle: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CellStateBox(
    title: String,
    detail: String,
    fillColor: Color,
    innerScale: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Canvas(modifier = Modifier.height(54.dp).fillMaxWidth()) {
                val cw = size.width * 0.66f
                val ch = size.height * 0.85f
                val left = (size.width - cw) / 2f
                val top = (size.height - ch) / 2f
                drawRoundRect(
                    color = ForestGreenDark,
                    topLeft = Offset(left, top),
                    size = Size(cw, ch),
                    cornerRadius = CornerRadius(12f, 12f),
                    style = Stroke(width = 4f)
                )
                val iw = cw * innerScale
                val ih = ch * innerScale
                drawRoundRect(
                    color = fillColor.copy(alpha = 0.55f),
                    topLeft = Offset((size.width - iw) / 2f, (size.height - ih) / 2f),
                    size = Size(iw, ih),
                    cornerRadius = CornerRadius(10f, 10f)
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
