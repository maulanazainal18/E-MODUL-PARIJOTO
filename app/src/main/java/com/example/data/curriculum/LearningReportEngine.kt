package com.example.data.curriculum

import com.example.data.model.Assignment
import com.example.data.model.ClassRoom
import com.example.data.model.Enrollment
import com.example.data.model.GradeRecord
import com.example.data.model.LearningModule
import com.example.data.model.Lesson
import com.example.data.model.Quiz
import com.example.data.model.StudentLearningProgress
import com.example.data.model.Submission
import kotlin.math.roundToInt

enum class ReportRoleMode {
    ADMIN,
    GURU,
    SISWA
}

enum class ReportComponentType(val label: String, val weight: Double) {
    QUIZ("Kuis Modul", 0.20),
    POST_TEST("Post-Test", 0.25),
    SKILL("Keterampilan", 0.20),
    PROJECT("Proyek CER", 0.20),
    ATTITUDE("Sikap", 0.15)
}

data class ReportFilter(
    val classId: String? = null,
    val meetingNumber: Int? = null,
    val moduleId: String? = null,
    val studentUid: String? = null
)

data class ReportComponentTrace(
    val gradeId: String,
    val componentType: ReportComponentType,
    val sourceId: String,
    val sourceTitle: String,
    val moduleId: String?,
    val meetingNumber: Int?,
    val score: Double,
    val weightPercent: Int,
    val formulaDetail: String,
    val published: Boolean,
    val includedInAverage: Boolean
)

data class AssignmentSubmissionStatus(
    val assignmentId: String,
    val title: String,
    val type: String,
    val moduleId: String,
    val meetingNumber: Int,
    val status: String,
    val submissionId: String?
)

data class ProgressPoint(
    val label: String,
    val score: Double,
    val category: String
)

data class StudentReportSummary(
    val studentUid: String,
    val studentName: String,
    val classId: String,
    val className: String,
    val classCode: String,
    val quizScore: Double?,
    val postTestScore: Double?,
    val skillScore: Double?,
    val attitudeScore: Double?,
    val projectScore: Double?,
    val finalReportScore: Double,
    val activeWeightPercent: Int,
    val predicate: String,
    val completedLessonsCount: Int,
    val totalLessonsCount: Int,
    val materialCompletionPercent: Double,
    val submittedAssignmentsCount: Int,
    val gradedAssignmentsCount: Int,
    val revisionAssignmentsCount: Int,
    val draftAssignmentsCount: Int,
    val totalAssignmentsCount: Int,
    val assignmentCompletionPercent: Double,
    val progressionPoints: List<ProgressPoint>,
    val traces: List<ReportComponentTrace>,
    val assignmentDetails: List<AssignmentSubmissionStatus>,
    val unpublishedSuppressedCount: Int = 0
)

data class ClassReportSummary(
    val classId: String,
    val className: String,
    val classCode: String,
    val teacherName: String,
    val academicYear: String,
    val activeStudentCount: Int,
    val avgQuizScore: Double?,
    val avgPostTestScore: Double?,
    val avgSkillScore: Double?,
    val avgAttitudeScore: Double?,
    val avgProjectScore: Double?,
    val avgFinalScore: Double,
    val avgMaterialCompletionPercent: Double,
    val avgAssignmentCompletionPercent: Double,
    val studentReports: List<StudentReportSummary>
)

data class ReportComputationResult(
    val filter: ReportFilter,
    val publishedOnlyApplied: Boolean,
    val classReports: List<ClassReportSummary>,
    val studentReports: List<StudentReportSummary>,
    val overallAvgQuiz: Double?,
    val overallAvgPostTest: Double?,
    val overallAvgSkill: Double?,
    val overallAvgAttitude: Double?,
    val overallAvgProject: Double?,
    val overallAvgFinalScore: Double,
    val overallMaterialCompletionPercent: Double,
    val overallAssignmentCompletionPercent: Double,
    val totalSubmittedTasks: Int,
    val totalExpectedTasks: Int,
    val overallProgressionPoints: List<ProgressPoint>
)

object LearningReportEngine {

    private data class ResolvedGradeMeta(
        val componentType: ReportComponentType,
        val moduleId: String?,
        val meetingNumber: Int?,
        val formulaDetail: String
    )

    fun compute(
        filter: ReportFilter,
        publishedOnly: Boolean,
        classes: List<ClassRoom>,
        enrollments: List<Enrollment>,
        modules: List<LearningModule>,
        lessons: List<Lesson>,
        assignments: List<Assignment>,
        submissions: List<Submission>,
        quizzes: List<Quiz>,
        grades: List<GradeRecord>,
        studentPersonalProgress: StudentLearningProgress? = null
    ): ReportComputationResult {
        val activeEnrollments = enrollments
            .filter { it.status == "ACTIVE" }
            .filter { filter.classId.isNullOrBlank() || it.classId == filter.classId }
            .filter { filter.studentUid.isNullOrBlank() || it.studentUid == filter.studentUid }

        val classMap = classes.associateBy { it.id }
        val moduleNumberById = modules.associate { it.id to it.moduleNumber }
        val quizById = quizzes.associateBy { it.id }
        val assignmentById = assignments.associateBy { it.id }

        // Scope lessons by module / meeting filter
        val scopedLessons = lessons.filter { lesson ->
            val modMatches = filter.moduleId.isNullOrBlank() || lesson.moduleId == filter.moduleId
            val meetingMatches = filter.meetingNumber == null ||
                inferMeetingForModule(lesson.moduleId, moduleNumberById) == filter.meetingNumber
            modMatches && meetingMatches
        }

        // Scope assignments by module / meeting filter
        val scopedAssignments = assignments.filter { asg ->
            val classMatches = filter.classId.isNullOrBlank() || asg.classId == "DEFAULT" || asg.classId == filter.classId
            val modMatches = filter.moduleId.isNullOrBlank() || asg.moduleId == filter.moduleId
            val meetingMatches = filter.meetingNumber == null || asg.meetingNumber == filter.meetingNumber
            classMatches && modMatches && meetingMatches
        }

        // Fallback identity rows if student hasn't enrolled yet in Student mode
        val effectiveRows: List<Enrollment> = if (activeEnrollments.isNotEmpty()) {
            activeEnrollments
        } else if (!filter.studentUid.isNullOrBlank() && filter.classId.isNullOrBlank()) {
            val inferredName = grades.firstOrNull { it.studentUid == filter.studentUid }?.studentName
                ?: submissions.firstOrNull { it.studentUid == filter.studentUid }?.studentName
                ?: "Siswa"
            listOf(
                Enrollment(
                    id = "DEFAULT_${filter.studentUid}",
                    classId = "DEFAULT",
                    className = "Belajar Mandiri",
                    classCode = "-",
                    teacherUid = "SYSTEM_DEFAULT",
                    studentUid = filter.studentUid,
                    studentName = inferredName,
                    status = "ACTIVE",
                    completedLessons = studentPersonalProgress?.completedLessons ?: emptyList()
                )
            )
        } else {
            emptyList()
        }

        val studentReports = effectiveRows.map { enr ->
            val cls = classMap[enr.classId]
            val studentRawGrades = grades.filter { g ->
                g.studentUid == enr.studentUid &&
                    (enr.classId == "DEFAULT" || g.classId == enr.classId || g.classId == "DEFAULT")
            }
            val suppressedCount = if (publishedOnly) studentRawGrades.count { !it.published } else 0
            val visibleGrades = if (publishedOnly) studentRawGrades.filter { it.published } else studentRawGrades

            // Resolve every grade with metadata & apply module/meeting filters cleanly
            val resolvedTraces = visibleGrades.mapNotNull { g ->
                val meta = resolveGradeMeta(g, quizById, assignmentById, moduleNumberById)
                val modMatches = filter.moduleId.isNullOrBlank() ||
                    meta.moduleId == null ||
                    meta.moduleId == filter.moduleId
                val meetingMatches = filter.meetingNumber == null ||
                    meta.meetingNumber == null ||
                    meta.meetingNumber == filter.meetingNumber
                if (!modMatches || !meetingMatches) {
                    null
                } else {
                    g to meta
                }
            }

            // Pick best score per sourceId within each component type so multi-attempt quizzes don't skew averages
            val includedBestIds = mutableSetOf<String>()
            val componentScores = mutableMapOf<ReportComponentType, Double?>()

            ReportComponentType.entries.forEach { compType ->
                val matching = resolvedTraces.filter { it.second.componentType == compType }
                if (matching.isEmpty()) {
                    componentScores[compType] = null
                } else {
                    val bestPerSource = matching
                        .groupBy { it.first.sourceId.ifBlank { it.first.id } }
                        .values
                        .map { group ->
                            val best = group.maxByOrNull { it.first.finalScore }!!
                            includedBestIds += best.first.id
                            best.first.finalScore
                        }
                    componentScores[compType] = bestPerSource.average()
                }
            }

            val traces = resolvedTraces.map { (g, meta) ->
                ReportComponentTrace(
                    gradeId = g.id,
                    componentType = meta.componentType,
                    sourceId = g.sourceId,
                    sourceTitle = g.sourceTitle,
                    moduleId = meta.moduleId,
                    meetingNumber = meta.meetingNumber,
                    score = round1Decimal(g.finalScore),
                    weightPercent = (meta.componentType.weight * 100).roundToInt(),
                    formulaDetail = meta.formulaDetail,
                    published = g.published,
                    includedInAverage = g.id in includedBestIds
                )
            }

            // Consistent weighted final score normalized across active graded components
            val activeEntries = ReportComponentType.entries.mapNotNull { type ->
                componentScores[type]?.let { score -> type to score }
            }
            val activeWeightSum = activeEntries.sumOf { it.first.weight }
            val finalReportScore = if (activeWeightSum > 0.0) {
                round1Decimal(
                    activeEntries.sumOf { (type, score) -> score * type.weight } / activeWeightSum
                )
            } else {
                0.0
            }

            // Material completion percentage (9)
            val completedIds = if (studentPersonalProgress != null && studentPersonalProgress.studentUid == enr.studentUid) {
                (enr.completedLessons + studentPersonalProgress.completedLessons).toSet()
            } else {
                enr.completedLessons.toSet()
            }
            val scopedLessonIds = scopedLessons.map { it.id }.toSet()
            val completedScopedCount = completedIds.count { it in scopedLessonIds }
            val totalScopedLessons = scopedLessons.size
            val materialPct = if (totalScopedLessons > 0) {
                round1Decimal((completedScopedCount * 100.0) / totalScopedLessons)
            } else {
                0.0
            }

            // Assignment submission recap (10)
            val studentClassSubs = submissions.filter { s ->
                s.studentUid == enr.studentUid &&
                    (enr.classId == "DEFAULT" || s.classId == enr.classId || s.classId == "DEFAULT")
            }
            val relevantAssignments = scopedAssignments.filter {
                it.classId == "DEFAULT" || it.classId == enr.classId
            }
            val assignmentDetails = relevantAssignments.map { asg ->
                val sub = studentClassSubs.firstOrNull { it.assignmentId == asg.id }
                AssignmentSubmissionStatus(
                    assignmentId = asg.id,
                    title = asg.title,
                    type = asg.type,
                    moduleId = asg.moduleId,
                    meetingNumber = asg.meetingNumber,
                    status = sub?.status ?: "BELUM",
                    submissionId = sub?.id
                )
            }
            val submittedCount = assignmentDetails.count { it.status in listOf("SUBMITTED", "GRADED", "REVISION_REQUIRED") }
            val gradedAsgCount = assignmentDetails.count { it.status == "GRADED" }
            val revisionCount = assignmentDetails.count { it.status == "REVISION_REQUIRED" }
            val draftCount = assignmentDetails.count { it.status == "DRAFT" }
            val asgPct = if (assignmentDetails.isNotEmpty()) {
                round1Decimal((submittedCount * 100.0) / assignmentDetails.size)
            } else {
                0.0
            }

            val progressionPoints = buildStudentProgression(resolvedTraces)

            StudentReportSummary(
                studentUid = enr.studentUid,
                studentName = enr.studentName,
                classId = enr.classId,
                className = cls?.name ?: enr.className,
                classCode = cls?.classCode ?: enr.classCode,
                quizScore = componentScores[ReportComponentType.QUIZ]?.let { round1Decimal(it) },
                postTestScore = componentScores[ReportComponentType.POST_TEST]?.let { round1Decimal(it) },
                skillScore = componentScores[ReportComponentType.SKILL]?.let { round1Decimal(it) },
                attitudeScore = componentScores[ReportComponentType.ATTITUDE]?.let { round1Decimal(it) },
                projectScore = componentScores[ReportComponentType.PROJECT]?.let { round1Decimal(it) },
                finalReportScore = finalReportScore,
                activeWeightPercent = (activeWeightSum * 100).roundToInt(),
                predicate = predicateLabel(finalReportScore, activeWeightSum > 0.0),
                completedLessonsCount = completedScopedCount,
                totalLessonsCount = totalScopedLessons,
                materialCompletionPercent = materialPct,
                submittedAssignmentsCount = submittedCount,
                gradedAssignmentsCount = gradedAsgCount,
                revisionAssignmentsCount = revisionCount,
                draftAssignmentsCount = draftCount,
                totalAssignmentsCount = assignmentDetails.size,
                assignmentCompletionPercent = asgPct,
                progressionPoints = progressionPoints,
                traces = traces,
                assignmentDetails = assignmentDetails,
                unpublishedSuppressedCount = suppressedCount
            )
        }.sortedWith(compareBy({ it.className.lowercase() }, { it.studentName.lowercase() }))

        val classReports = buildClassReports(
            classes = classes,
            filter = filter,
            studentReports = studentReports
        )

        val gradedStudents = studentReports.filter { it.activeWeightPercent > 0 }
        val overallAvgQuiz = studentReports.mapNotNull { it.quizScore }.averageOrNull()
        val overallAvgPostTest = studentReports.mapNotNull { it.postTestScore }.averageOrNull()
        val overallAvgSkill = studentReports.mapNotNull { it.skillScore }.averageOrNull()
        val overallAvgAttitude = studentReports.mapNotNull { it.attitudeScore }.averageOrNull()
        val overallAvgProject = studentReports.mapNotNull { it.projectScore }.averageOrNull()
        val overallAvgFinal = if (gradedStudents.isNotEmpty()) {
            round1Decimal(gradedStudents.map { it.finalReportScore }.average())
        } else {
            0.0
        }
        val overallMaterialPct = studentReports.map { it.materialCompletionPercent }.averageOrNull() ?: 0.0
        val overallAssignmentPct = studentReports.map { it.assignmentCompletionPercent }.averageOrNull() ?: 0.0

        val overallProgression = buildOverallProgression(studentReports)

        return ReportComputationResult(
            filter = filter,
            publishedOnlyApplied = publishedOnly,
            classReports = classReports,
            studentReports = studentReports,
            overallAvgQuiz = overallAvgQuiz,
            overallAvgPostTest = overallAvgPostTest,
            overallAvgSkill = overallAvgSkill,
            overallAvgAttitude = overallAvgAttitude,
            overallAvgProject = overallAvgProject,
            overallAvgFinalScore = overallAvgFinal,
            overallMaterialCompletionPercent = overallMaterialPct,
            overallAssignmentCompletionPercent = overallAssignmentPct,
            totalSubmittedTasks = studentReports.sumOf { it.submittedAssignmentsCount },
            totalExpectedTasks = studentReports.sumOf { it.totalAssignmentsCount },
            overallProgressionPoints = overallProgression
        )
    }

    private fun buildClassReports(
        classes: List<ClassRoom>,
        filter: ReportFilter,
        studentReports: List<StudentReportSummary>
    ): List<ClassReportSummary> {
        val reportsByClass = studentReports.groupBy { it.classId }
        val targetClasses = classes
            .filter { filter.classId.isNullOrBlank() || it.id == filter.classId }
        val knownIds = targetClasses.map { it.id }.toSet()

        val result = targetClasses.map { cls ->
            val rows = reportsByClass[cls.id].orEmpty()
            buildSingleClassSummary(
                classId = cls.id,
                className = cls.name,
                classCode = cls.classCode,
                teacherName = cls.teacherName,
                academicYear = cls.academicYear,
                rows = rows
            )
        }.toMutableList()

        reportsByClass.entries
            .filter { it.key !in knownIds && it.value.isNotEmpty() }
            .forEach { (cid, rows) ->
                val first = rows.first()
                result += buildSingleClassSummary(
                    classId = cid,
                    className = first.className.ifBlank { cid },
                    classCode = first.classCode,
                    teacherName = "Guru Pengampu",
                    academicYear = "2026/2027",
                    rows = rows
                )
            }

        return result.sortedBy { it.className.lowercase() }
    }

    private fun buildSingleClassSummary(
        classId: String,
        className: String,
        classCode: String,
        teacherName: String,
        academicYear: String,
        rows: List<StudentReportSummary>
    ): ClassReportSummary {
        val graded = rows.filter { it.activeWeightPercent > 0 }
        return ClassReportSummary(
            classId = classId,
            className = className,
            classCode = classCode,
            teacherName = teacherName,
            academicYear = academicYear,
            activeStudentCount = rows.size,
            avgQuizScore = rows.mapNotNull { it.quizScore }.averageOrNull(),
            avgPostTestScore = rows.mapNotNull { it.postTestScore }.averageOrNull(),
            avgSkillScore = rows.mapNotNull { it.skillScore }.averageOrNull(),
            avgAttitudeScore = rows.mapNotNull { it.attitudeScore }.averageOrNull(),
            avgProjectScore = rows.mapNotNull { it.projectScore }.averageOrNull(),
            avgFinalScore = if (graded.isNotEmpty()) round1Decimal(graded.map { it.finalReportScore }.average()) else 0.0,
            avgMaterialCompletionPercent = rows.map { it.materialCompletionPercent }.averageOrNull() ?: 0.0,
            avgAssignmentCompletionPercent = rows.map { it.assignmentCompletionPercent }.averageOrNull() ?: 0.0,
            studentReports = rows
        )
    }

    private fun resolveGradeMeta(
        grade: GradeRecord,
        quizById: Map<String, Quiz>,
        assignmentById: Map<String, Assignment>,
        moduleNumberById: Map<String, Int>
    ): ResolvedGradeMeta {
        return when (grade.assessmentType.uppercase()) {
            "PENGETAHUAN" -> {
                val quiz = quizById[grade.sourceId]
                val isPostTest = quiz?.category == "POST_TEST" ||
                    grade.sourceId.contains("post_test", ignoreCase = true) ||
                    grade.sourceTitle.contains("Post-Test", ignoreCase = true) ||
                    grade.sourceTitle.contains("Evaluasi Akhir", ignoreCase = true)
                val modId = quiz?.moduleId ?: when {
                    grade.sourceId.contains("modul_1", ignoreCase = true) -> ParijotoCurriculumPart1.modul1.id
                    grade.sourceId.contains("modul_2", ignoreCase = true) || isPostTest -> ParijotoCurriculumPart1.modul2.id
                    else -> null
                }
                val meeting = inferMeetingForModule(modId, moduleNumberById)
                val compType = if (isPostTest) ReportComponentType.POST_TEST else ReportComponentType.QUIZ
                val detail = buildString {
                    if (grade.knowledgeScore != null) {
                        append("PG: ${QuizEngineUtils.roundScore(grade.knowledgeScore)}")
                    }
                    if (grade.essayScore != null) {
                        if (isNotEmpty()) append(" • ")
                        append("Uraian: ${QuizEngineUtils.roundScore(grade.essayScore)}")
                    }
                    if (isEmpty()) {
                        append("Skor Evaluasi: ${QuizEngineUtils.roundScore(grade.finalScore)}")
                    } else {
                        append(" -> ${QuizEngineUtils.roundScore(grade.finalScore)}")
                    }
                }
                ResolvedGradeMeta(compType, modId, meeting, detail)
            }

            "PROYEK" -> {
                val asg = assignmentById[grade.sourceId]
                val modId = asg?.moduleId ?: ParijotoCurriculumPart1.modul2.id
                val meeting = asg?.meetingNumber ?: 2
                ResolvedGradeMeta(
                    componentType = ReportComponentType.PROJECT,
                    moduleId = modId,
                    meetingNumber = meeting,
                    formulaDetail = buildSkillFormulaDetail(grade)
                )
            }

            "KETERAMPILAN" -> {
                val asg = assignmentById[grade.sourceId]
                val isProject = asg?.type == "PROYEK_CER" ||
                    grade.sourceId.contains("proyek", ignoreCase = true) ||
                    grade.sourceTitle.contains("Proyek", ignoreCase = true) ||
                    grade.sourceTitle.contains("CER", ignoreCase = true)
                val modId = asg?.moduleId ?: when {
                    grade.sourceId.contains("elkm_1", ignoreCase = true) -> ParijotoCurriculumPart1.modul1.id
                    grade.sourceId.contains("elkm_2", ignoreCase = true) || isProject -> ParijotoCurriculumPart1.modul2.id
                    else -> null
                }
                val meeting = asg?.meetingNumber ?: inferMeetingForModule(modId, moduleNumberById)
                ResolvedGradeMeta(
                    componentType = if (isProject) ReportComponentType.PROJECT else ReportComponentType.SKILL,
                    moduleId = modId,
                    meetingNumber = meeting,
                    formulaDetail = buildSkillFormulaDetail(grade)
                )
            }

            else -> {
                // SIKAP (cross-meeting attitude observation)
                val c1 = grade.attitudeCollaboration
                val c2 = grade.attitudePietyEcology
                val c3 = grade.attitudeResponsibility
                val c4 = grade.attitudeCare
                val detail = if (c1 != null && c2 != null && c3 != null && c4 != null) {
                    "Kolaborasi($c1) + Ekologi($c2) + TanggungJawab($c3) + Peduli($c4) = ${c1 + c2 + c3 + c4}/16 (${QuizEngineUtils.roundScore(grade.finalScore)})"
                } else {
                    "Observasi Sikap: ${QuizEngineUtils.roundScore(grade.finalScore)}"
                }
                ResolvedGradeMeta(
                    componentType = ReportComponentType.ATTITUDE,
                    moduleId = null,
                    meetingNumber = null,
                    formulaDetail = detail
                )
            }
        }
    }

    private fun buildSkillFormulaDetail(grade: GradeRecord): String {
        val s1 = grade.skillInvestigationScore
        val s2 = grade.skillSolutionScore
        val s3 = grade.skillSynthesisScore
        return if (s1 != null && s2 != null && s3 != null) {
            "Penyelidikan($s1) + Solusi($s2) + Sintesis($s3) = ${s1 + s2 + s3}/12 (${QuizEngineUtils.roundScore(grade.finalScore)})"
        } else {
            "Rubrik Keterampilan/Proyek: ${QuizEngineUtils.roundScore(grade.finalScore)}"
        }
    }

    private fun inferMeetingForModule(moduleId: String?, moduleNumberById: Map<String, Int>): Int? {
        if (moduleId.isNullOrBlank()) return null
        return moduleNumberById[moduleId] ?: when {
            moduleId.contains("1") -> 1
            moduleId.contains("2") -> 2
            else -> null
        }
    }

    private fun buildStudentProgression(resolved: List<Pair<GradeRecord, ResolvedGradeMeta>>): List<ProgressPoint> {
        if (resolved.isEmpty()) return emptyList()
        data class Bucket(val key: String, val label: String, val order: Int, val category: String, val score: Double)
        val buckets = resolved.map { (g, meta) ->
            val (key, label, order) = when {
                meta.componentType == ReportComponentType.QUIZ && meta.meetingNumber == 1 ->
                    Triple("kuis_1", "Kuis M1", 1)
                meta.componentType == ReportComponentType.SKILL && meta.meetingNumber == 1 ->
                    Triple("lkm_1", "E-LKM 1", 2)
                meta.componentType == ReportComponentType.QUIZ && meta.meetingNumber == 2 ->
                    Triple("kuis_2", "Kuis M2", 3)
                meta.componentType == ReportComponentType.SKILL && meta.meetingNumber == 2 ->
                    Triple("lkm_2", "E-LKM 2", 4)
                meta.componentType == ReportComponentType.PROJECT ->
                    Triple("proyek", "Proyek CER", 5)
                meta.componentType == ReportComponentType.POST_TEST ->
                    Triple("post_test", "Post-Test", 6)
                meta.componentType == ReportComponentType.ATTITUDE ->
                    Triple("sikap", "Sikap", 7)
                else ->
                    Triple(g.sourceId.ifBlank { g.id }, g.sourceTitle.take(12), 8)
            }
            Bucket(key, label, order, meta.componentType.label, g.finalScore)
        }
        return buckets
            .groupBy { it.key }
            .values
            .map { grp ->
                val first = grp.first()
                ProgressPoint(
                    label = first.label,
                    score = round1Decimal(grp.map { it.score }.average()),
                    category = first.category
                ) to first.order
            }
            .sortedBy { it.second }
            .map { it.first }
    }

    private fun buildOverallProgression(studentReports: List<StudentReportSummary>): List<ProgressPoint> {
        val allPoints = studentReports.flatMap { it.progressionPoints }
        if (allPoints.isEmpty()) return emptyList()
        val orderMap = mapOf(
            "Kuis M1" to 1,
            "E-LKM 1" to 2,
            "Kuis M2" to 3,
            "E-LKM 2" to 4,
            "Proyek CER" to 5,
            "Post-Test" to 6,
            "Sikap" to 7
        )
        return allPoints
            .groupBy { it.label }
            .values
            .map { list ->
                val first = list.first()
                ProgressPoint(
                    label = first.label,
                    score = round1Decimal(list.map { it.score }.average()),
                    category = first.category
                )
            }
            .sortedBy { orderMap[it.label] ?: 99 }
    }

    fun predicateLabel(score: Double, hasActiveGrades: Boolean): String {
        if (!hasActiveGrades) return "Belum Ada Nilai"
        return when {
            score >= 90.0 -> "Sangat Baik (A)"
            score >= 80.0 -> "Baik (B)"
            score >= 75.0 -> "Tuntas KKM (C)"
            else -> "Perlu Bimbingan (D)"
        }
    }

    private fun round1Decimal(value: Double): Double =
        ((value * 10.0).roundToInt() / 10.0).coerceIn(0.0, 100.0)

    private fun List<Double>.averageOrNull(): Double? =
        if (isEmpty()) null else round1Decimal(average())
}
