package com.example.data.repository

import android.content.Context
import com.example.data.curriculum.ParijotoAssessmentsAndGlossary
import com.example.data.curriculum.ParijotoCurriculumPart1
import com.example.data.curriculum.ParijotoCurriculumPart2
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.ClassCodeLookup
import com.example.data.model.ClassRoom
import com.example.data.model.Enrollment
import com.example.data.model.LearningModule
import com.example.data.model.Lesson
import com.example.data.model.StudentLearningProgress
import com.example.data.model.Subject
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class CurriculumAndClassRepository(context: Context) {

    private val db: FirebaseFirestore = FirestoreClientProvider.get(context)

    private val allDefaultLessons: List<Lesson> by lazy {
        ParijotoCurriculumPart1.modul1Lessons + ParijotoCurriculumPart2.modul2Lessons
    }

    fun observePublishedSubjects(): Flow<List<Subject>> =
        db.collection("subjects")
            .whereEqualTo("published", true)
            .observeListFlow<Subject>("subjects")
            .map { remote ->
                if (remote.isEmpty()) listOf(ParijotoAssessmentsAndGlossary.defaultSubject)
                else remote
            }

    fun observePublishedModules(): Flow<List<LearningModule>> =
        db.collection("modules")
            .whereEqualTo("published", true)
            .observeListFlow<LearningModule>("modules")
            .map { remote ->
                val defaults = listOf(ParijotoCurriculumPart1.modul1, ParijotoCurriculumPart1.modul2)
                val merged = defaults.associateBy { it.id }.toMutableMap()
                remote.forEach { merged[it.id] = it }
                merged.values.sortedBy { it.moduleNumber }
            }

    fun observePublishedLessons(): Flow<List<Lesson>> =
        db.collection("lessons")
            .whereEqualTo("published", true)
            .observeListFlow<Lesson>("lessons")
            .map { remote ->
                val merged = allDefaultLessons.associateBy { it.id }.toMutableMap()
                remote.forEach { merged[it.id] = it }
                merged.values.sortedWith(compareBy({ it.moduleId }, { it.orderIndex }))
            }

    fun observeTeacherLessons(teacherUid: String): Flow<List<Lesson>> {
        val publishedFlow = db.collection("lessons")
            .whereEqualTo("published", true)
            .observeListFlow<Lesson>("lessons")
        val ownFlow = db.collection("lessons")
            .whereEqualTo("ownerUid", teacherUid)
            .observeListFlow<Lesson>("lessons")
        return combine(publishedFlow, ownFlow) { published, own ->
            val merged = allDefaultLessons.associateBy { it.id }.toMutableMap()
            published.forEach { merged[it.id] = it }
            own.forEach { merged[it.id] = it }
            merged.values.sortedWith(compareBy({ it.moduleId }, { it.orderIndex }))
        }
    }

    fun observeStudentLearningProgress(studentUid: String): Flow<StudentLearningProgress> =
        db.collection("learning_progress")
            .document(studentUid)
            .observeDocFlow<StudentLearningProgress>("learning_progress/$studentUid")
            .map { doc ->
                doc ?: StudentLearningProgress(id = studentUid, studentUid = studentUid)
            }

    fun observeTeacherClasses(teacherUid: String): Flow<List<ClassRoom>> =
        db.collection("classes")
            .whereEqualTo("teacherUid", teacherUid)
            .observeListFlow<ClassRoom>("classes")

    fun observeStudentEnrollments(studentUid: String): Flow<List<Enrollment>> =
        db.collection("enrollments")
            .whereEqualTo("studentUid", studentUid)
            .observeListFlow<Enrollment>("enrollments")
            .map { list -> list.filter { it.status == "ACTIVE" } }

    fun observeTeacherEnrollments(teacherUid: String): Flow<List<Enrollment>> =
        db.collection("enrollments")
            .whereEqualTo("teacherUid", teacherUid)
            .observeListFlow<Enrollment>("enrollments")
            .map { list ->
                list.sortedWith(
                    compareBy<Enrollment> { if (it.status == "ACTIVE") 0 else 1 }
                        .thenBy { it.studentName.lowercase() }
                )
            }

    suspend fun fetchClassRoomsByIds(classIds: List<String>): Map<String, ClassRoom> {
        val result = mutableMapOf<String, ClassRoom>()
        for (classId in classIds.distinct().filter { it.isNotBlank() }) {
            try {
                val snap = db.collection("classes").document(classId).get().await()
                if (snap.exists()) {
                    snap.toObject(ClassRoom::class.java)?.let { result[classId] = it }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.GET, "classes/$classId")
            }
        }
        return result
    }

    suspend fun saveSubject(subject: Subject, ownerUid: String): Result<Unit> {
        val docId = subject.id.ifBlank { "subj_${UUID.randomUUID().toString().take(8)}" }
        val isCreate = subject.createdAt == null
        return runFirestoreWrite(
            if (isCreate) OperationType.CREATE else OperationType.UPDATE,
            "subjects/$docId"
        ) {
            val payload = mutableMapOf<String, Any>(
                "title" to subject.title.trim().take(150),
                "phase" to subject.phase.trim().take(20),
                "grade" to subject.grade.trim().take(40),
                "academicYear" to subject.academicYear.trim().take(30),
                "ownerUid" to (subject.ownerUid.takeIf { it.isNotBlank() && it != "SYSTEM_DEFAULT" } ?: ownerUid),
                "published" to subject.published,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            subject.description?.takeIf { it.isNotBlank() }?.let { payload["description"] = it.take(1000) }
            if (isCreate) {
                payload["createdAt"] = FieldValue.serverTimestamp()
                db.collection("subjects").document(docId).set(payload).await()
            } else {
                db.collection("subjects").document(docId).set(payload, SetOptions.merge()).await()
            }
        }
    }

    suspend fun saveModule(module: LearningModule, ownerUid: String): Result<Unit> {
        val docId = module.id.ifBlank { "mod_${UUID.randomUUID().toString().take(8)}" }
        val isCreate = module.createdAt == null
        return runFirestoreWrite(
            if (isCreate) OperationType.CREATE else OperationType.UPDATE,
            "modules/$docId"
        ) {
            val existing = db.collection("modules").document(docId).get().await()
            val payload = mutableMapOf<String, Any>(
                "subjectId" to module.subjectId.ifBlank { "subj_bio_x" },
                "moduleNumber" to module.moduleNumber.coerceIn(1, 10),
                "title" to module.title.trim().take(200),
                "subtitle" to module.subtitle.trim().take(200),
                "meetingLabel" to module.meetingLabel.trim().take(60),
                "durationMinutes" to module.durationMinutes.coerceIn(10, 360),
                "ownerUid" to (existing.getString("ownerUid") ?: ownerUid),
                "published" to module.published,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            module.description?.takeIf { it.isNotBlank() }?.let { payload["description"] = it.take(1500) }
            if (!existing.exists()) {
                payload["createdAt"] = FieldValue.serverTimestamp()
                db.collection("modules").document(docId).set(payload).await()
            } else {
                db.collection("modules").document(docId).set(payload, SetOptions.merge()).await()
            }
        }
    }

    suspend fun saveLesson(lesson: Lesson, ownerUid: String): Result<Unit> {
        val docId = lesson.id.ifBlank { "les_${UUID.randomUUID().toString().take(8)}" }
        return runFirestoreWrite(OperationType.WRITE, "lessons/$docId") {
            val existing = db.collection("lessons").document(docId).get().await()
            val payload = mutableMapOf<String, Any>(
                "moduleId" to lesson.moduleId,
                "orderIndex" to lesson.orderIndex.coerceIn(1, 100),
                "title" to lesson.title.trim().take(200),
                "learningObjective" to lesson.learningObjective.trim().take(1000),
                "explanation" to lesson.explanation.trim().take(8000),
                "summary" to lesson.summary.trim().take(3000),
                "ownerUid" to (existing.getString("ownerUid") ?: ownerUid),
                "published" to lesson.published,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            lesson.teenExplanation?.takeIf { it.isNotBlank() }?.let { payload["teenExplanation"] = it.take(3000) }
            lesson.dailyExample?.takeIf { it.isNotBlank() }?.let { payload["dailyExample"] = it.take(2000) }
            lesson.funFact?.takeIf { it.isNotBlank() }?.let { payload["funFact"] = it.take(1500) }
            lesson.sparkQuestion?.takeIf { it.isNotBlank() }?.let { payload["sparkQuestion"] = it.take(1000) }
            lesson.illustrationType?.takeIf { it.isNotBlank() }?.let { payload["illustrationType"] = it.take(60) }
            lesson.citation?.takeIf { it.isNotBlank() }?.let { payload["citation"] = it.take(500) }

            if (!existing.exists()) {
                payload["createdAt"] = FieldValue.serverTimestamp()
                db.collection("lessons").document(docId).set(payload).await()
            } else {
                db.collection("lessons").document(docId).set(payload, SetOptions.merge()).await()
            }
        }
    }

    suspend fun deleteLesson(lessonId: String): Result<Unit> =
        runFirestoreWrite(OperationType.DELETE, "lessons/$lessonId") {
            db.collection("lessons").document(lessonId).delete().await()
        }

    suspend fun createClassRoom(
        teacherUid: String,
        teacherName: String,
        name: String,
        gradeLevel: String,
        academicYear: String,
        description: String?
    ): Result<ClassRoom> {
        val trimmedName = name.trim()
        if (trimmedName.length < 2) {
            return Result.failure(IllegalArgumentException("Nama kelas minimal 2 karakter."))
        }
        val classId = "cls_${UUID.randomUUID().toString().take(8)}"
        return runFirestoreWrite(OperationType.CREATE, "classes/$classId") {
            val code = generateUniqueClassCode()
            val batch = db.batch()
            val classRef = db.collection("classes").document(classId)
            val codeRef = db.collection("class_codes").document(code)

            val classPayload = mutableMapOf<String, Any>(
                "teacherUid" to teacherUid,
                "teacherName" to teacherName.trim().take(100),
                "name" to trimmedName.take(100),
                "gradeLevel" to gradeLevel.trim().take(40),
                "phase" to "E",
                "classCode" to code,
                "academicYear" to academicYear.trim().take(30),
                "subjectId" to ParijotoAssessmentsAndGlossary.defaultSubject.id,
                "archived" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            description?.trim()?.takeIf { it.isNotBlank() }?.let {
                classPayload["description"] = it.take(500)
            }

            val codePayload = mapOf(
                "classId" to classId,
                "className" to trimmedName.take(100),
                "teacherUid" to teacherUid,
                "teacherName" to teacherName.trim().take(100),
                "gradeLevel" to gradeLevel.trim().take(40),
                "academicYear" to academicYear.trim().take(30),
                "active" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            batch.set(classRef, classPayload)
            batch.set(codeRef, codePayload)
            batch.commit().await()

            ClassRoom(
                id = classId,
                teacherUid = teacherUid,
                teacherName = teacherName,
                name = trimmedName,
                gradeLevel = gradeLevel,
                phase = "E",
                classCode = code,
                academicYear = academicYear,
                description = description
            )
        }
    }

    suspend fun toggleArchiveClass(classRoom: ClassRoom): Result<Unit> =
        runFirestoreWrite(OperationType.UPDATE, "classes/${classRoom.id}") {
            val newArchived = !classRoom.archived
            val batch = db.batch()
            batch.update(
                db.collection("classes").document(classRoom.id),
                mapOf("archived" to newArchived, "updatedAt" to FieldValue.serverTimestamp())
            )
            if (classRoom.classCode.isNotBlank()) {
                batch.update(
                    db.collection("class_codes").document(classRoom.classCode),
                    mapOf("active" to !newArchived, "updatedAt" to FieldValue.serverTimestamp())
                )
            }
            batch.commit().await()
        }

    suspend fun joinClassByCode(
        rawCode: String,
        studentUid: String,
        studentName: String
    ): Result<Enrollment> {
        val code = rawCode.trim().uppercase().replace("[^A-Z0-9]".toRegex(), "")
        if (code.length !in 4..16) {
            return Result.failure(IllegalArgumentException("Kode kelas harus terdiri dari 4–16 huruf/angka."))
        }
        return runFirestoreWrite(OperationType.CREATE, "class_codes/$code") {
            val codeSnap = db.collection("class_codes").document(code).get().await()
            if (!codeSnap.exists()) {
                throw IllegalArgumentException("Kode kelas '$code' tidak ditemukan.")
            }
            val lookup = codeSnap.toObject(ClassCodeLookup::class.java)
                ?: throw IllegalStateException("Data kode kelas tidak valid.")
            if (!lookup.active) {
                throw IllegalStateException("Kelas ini sudah diarsipkan oleh guru.")
            }
            val enrollId = "${lookup.classId}_$studentUid"
            val enrollRef = db.collection("enrollments").document(enrollId)
            val existing = enrollRef.get().await()
            if (existing.exists()) {
                val currentStatus = existing.getString("status") ?: "ACTIVE"
                if (currentStatus == "ACTIVE") {
                    throw IllegalStateException("Anda sudah terdaftar di kelas ${lookup.className}.")
                } else {
                    throw IllegalStateException("Keanggotaan Anda di kelas ${lookup.className} sedang dinonaktifkan oleh guru.")
                }
            }

            val payload = mapOf(
                "classId" to lookup.classId,
                "className" to lookup.className,
                "classCode" to code,
                "teacherUid" to lookup.teacherUid,
                "studentUid" to studentUid,
                "studentName" to studentName.trim().take(100),
                "status" to "ACTIVE",
                "progressPercent" to 0,
                "completedLessons" to emptyList<String>(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            enrollRef.set(payload).await()

            db.collection("users").document(studentUid).set(
                mapOf(
                    "classId" to lookup.classId,
                    "className" to lookup.className,
                    "academicYear" to lookup.academicYear,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()

            Enrollment(
                id = enrollId,
                classId = lookup.classId,
                className = lookup.className,
                classCode = code,
                teacherUid = lookup.teacherUid,
                studentUid = studentUid,
                studentName = studentName,
                status = "ACTIVE"
            )
        }
    }

    suspend fun updateEnrollmentStatus(enrollmentId: String, newStatus: String): Result<Unit> {
        val safeStatus = if (newStatus == "REMOVED") "REMOVED" else "ACTIVE"
        return runFirestoreWrite(OperationType.UPDATE, "enrollments/$enrollmentId") {
            db.collection("enrollments").document(enrollmentId).update(
                mapOf(
                    "status" to safeStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    suspend fun removeEnrollment(enrollmentId: String): Result<Unit> =
        runFirestoreWrite(OperationType.DELETE, "enrollments/$enrollmentId") {
            db.collection("enrollments").document(enrollmentId).delete().await()
        }

    suspend fun markLessonCompleted(
        enrollment: Enrollment,
        lessonId: String,
        totalLessonsCount: Int
    ): Result<Unit> {
        val updatedList = (enrollment.completedLessons + lessonId).distinct().take(50)
        val safeTotal = totalLessonsCount.coerceAtLeast(1)
        val pct = ((updatedList.size * 100) / safeTotal).coerceIn(0, 100)
        return runFirestoreWrite(OperationType.UPDATE, "enrollments/${enrollment.id}") {
            db.collection("enrollments").document(enrollment.id).update(
                mapOf(
                    "completedLessons" to updatedList,
                    "progressPercent" to pct,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    suspend fun toggleLessonBookmark(
        studentUid: String,
        lessonId: String,
        current: StudentLearningProgress
    ): Result<Boolean> {
        val currentlyBookmarked = current.bookmarkedLessons.contains(lessonId)
        val nextBookmarks = if (currentlyBookmarked) {
            current.bookmarkedLessons.filterNot { it == lessonId }
        } else {
            (current.bookmarkedLessons + lessonId).distinct().take(100)
        }
        return upsertLearningProgress(
            studentUid = studentUid,
            next = current.copy(bookmarkedLessons = nextBookmarks)
        ).map { !currentlyBookmarked }
    }

    suspend fun updateReaderFontScale(
        studentUid: String,
        fontScale: Double,
        current: StudentLearningProgress
    ): Result<Unit> {
        val clamped = fontScale.coerceIn(0.85, 1.4)
        return upsertLearningProgress(
            studentUid = studentUid,
            next = current.copy(readerFontScale = clamped)
        )
    }

    suspend fun recordLessonReadProgress(
        studentUid: String,
        moduleId: String,
        lessonId: String,
        scrollPercent: Int,
        markComplete: Boolean,
        current: StudentLearningProgress
    ): Result<Unit> {
        val prevPct = current.lessonReadProgress[lessonId] ?: 0
        val nextPct = maxOf(prevPct, scrollPercent.coerceIn(0, 100), if (markComplete) 100 else 0)
        val nextProgressMap = (current.lessonReadProgress + (lessonId to nextPct))
            .entries.take(100).associate { it.key to it.value }
        val nextLastMap = (current.lastLessonByModule + (moduleId to lessonId))
            .entries.take(20).associate { it.key to it.value }
        val nextCompleted = if (markComplete) {
            (current.completedLessons + lessonId).distinct().take(100)
        } else {
            current.completedLessons
        }
        return upsertLearningProgress(
            studentUid = studentUid,
            next = current.copy(
                completedLessons = nextCompleted,
                lessonReadProgress = nextProgressMap,
                lastLessonByModule = nextLastMap
            )
        )
    }

    private suspend fun upsertLearningProgress(
        studentUid: String,
        next: StudentLearningProgress
    ): Result<Unit> = runFirestoreWrite(OperationType.WRITE, "learning_progress/$studentUid") {
        val ref = db.collection("learning_progress").document(studentUid)
        val existing = ref.get().await()
        val payload = mutableMapOf<String, Any>(
            "studentUid" to studentUid,
            "completedLessons" to next.completedLessons.distinct().take(100),
            "bookmarkedLessons" to next.bookmarkedLessons.distinct().take(100),
            "lessonReadProgress" to next.lessonReadProgress.entries.take(100)
                .associate { it.key to it.value.coerceIn(0, 100) },
            "lastLessonByModule" to next.lastLessonByModule.entries.take(20)
                .associate { it.key to it.value.take(128) },
            "readerFontScale" to next.readerFontScale.coerceIn(0.85, 1.4),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!existing.exists()) {
            payload["createdAt"] = FieldValue.serverTimestamp()
            ref.set(payload).await()
        } else {
            ref.set(payload, SetOptions.merge()).await()
        }
    }

    suspend fun seedDefaultCurriculumIfNeeded(ownerUid: String): Result<Unit> {
        return try {
            val subjectRef = db.collection("subjects").document(ParijotoAssessmentsAndGlossary.defaultSubject.id)
            if (!subjectRef.get().await().exists()) {
                saveSubject(ParijotoAssessmentsAndGlossary.defaultSubject, ownerUid)
            }
            listOf(ParijotoCurriculumPart1.modul1, ParijotoCurriculumPart1.modul2).forEach { mod ->
                if (!db.collection("modules").document(mod.id).get().await().exists()) {
                    saveModule(mod, ownerUid)
                }
            }
            ParijotoAssessmentsAndGlossary.defaultRubrics.forEach { rubric ->
                val ref = db.collection("rubrics").document(rubric.id)
                if (!ref.get().await().exists()) {
                    ref.set(
                        mapOf(
                            "ownerUid" to ownerUid,
                            "title" to rubric.title,
                            "category" to rubric.category,
                            "maxTotalScore" to rubric.maxTotalScore,
                            "criteria" to rubric.criteria,
                            "published" to true,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "curriculum_seed")
            Result.failure(e)
        }
    }

    private suspend fun generateUniqueClassCode(): String {
        repeat(6) {
            val candidate = generateClassCode()
            val snap = db.collection("class_codes").document(candidate).get().await()
            if (!snap.exists()) return candidate
        }
        val extra = UUID.randomUUID().toString().uppercase().replace("-", "").take(6)
        return "PRJ$extra"
    }

    private fun generateClassCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return buildString {
            append("PRJ")
            repeat(4) { append(chars.random()) }
        }
    }
}
