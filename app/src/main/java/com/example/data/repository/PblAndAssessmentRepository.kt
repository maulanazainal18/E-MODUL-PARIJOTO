package com.example.data.repository

import android.content.Context
import com.example.data.curriculum.ParijotoAssessmentsAndGlossary
import com.example.data.firebase.OperationType
import com.example.data.model.Announcement
import com.example.data.model.Assignment
import com.example.data.model.GroupDiscussionMessage
import com.example.data.model.PblGroup
import com.example.data.model.PeerReview
import com.example.data.model.Reflection
import com.example.data.model.Submission
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class PblAndAssessmentRepository(context: Context) {

    private val db: FirebaseFirestore = FirestoreClientProvider.get(context)

    companion object {
        const val MAX_ATTACHMENT_BYTES = 5_242_880L
        val ALLOWED_MIME_PREFIXES = listOf("image/", "application/pdf")
    }

    fun observeGroupsForClass(classId: String): Flow<List<PblGroup>> {
        if (classId.isBlank() || classId == "DEFAULT") return flowOf(emptyList())
        return db.collection("groups")
            .whereEqualTo("classId", classId)
            .whereEqualTo("openToJoin", true)
            .observeListFlow<PblGroup>("groups")
    }

    fun observeTeacherGroups(teacherUid: String): Flow<List<PblGroup>> =
        db.collection("groups")
            .whereEqualTo("teacherUid", teacherUid)
            .observeListFlow<PblGroup>("groups")

    fun observeStudentGroups(studentUid: String): Flow<List<PblGroup>> =
        db.collection("groups")
            .whereArrayContains("memberUids", studentUid)
            .observeListFlow<PblGroup>("groups")

    suspend fun createPblGroup(
        classId: String,
        teacherUid: String,
        name: String,
        topic: String?,
        creatorUid: String,
        creatorName: String,
        createdByTeacher: Boolean
    ): Result<String> {
        val groupId = "grp_${UUID.randomUUID().toString().take(8)}"
        return runFirestoreWrite(OperationType.CREATE, "groups/$groupId") {
            val payload = mutableMapOf<String, Any>(
                "classId" to classId,
                "teacherUid" to teacherUid,
                "name" to name.trim().take(100),
                "memberUids" to if (createdByTeacher) emptyList<String>() else listOf(creatorUid),
                "memberNames" to if (createdByTeacher) emptyList<String>() else listOf(creatorName),
                "memberRoles" to if (createdByTeacher) emptyMap<String, String>() else mapOf(creatorUid to "Ketua Kelompok"),
                "leaderUid" to creatorUid,
                "pblPhase" to 1,
                "openToJoin" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            topic?.trim()?.takeIf { it.isNotBlank() }?.let { payload["topic"] = it.take(200) }
            db.collection("groups").document(groupId).set(payload).await()
            groupId
        }
    }

    suspend fun joinPblGroup(
        group: PblGroup,
        studentUid: String,
        studentName: String,
        roleLabel: String = "Anggota"
    ): Result<Unit> {
        if (studentUid in group.memberUids) return Result.success(Unit)
        if (group.memberUids.size >= 12) {
            return Result.failure(IllegalStateException("Kelompok sudah mencapai batas maksimal 12 anggota."))
        }
        return runFirestoreWrite(OperationType.UPDATE, "groups/${group.id}") {
            val newUids = group.memberUids + studentUid
            val newNames = group.memberNames + studentName.trim().take(100)
            val newRoles = group.memberRoles.toMutableMap().apply {
                put(studentUid, roleLabel.trim().ifBlank { "Anggota" }.take(60))
            }
            val payload = mutableMapOf<String, Any>(
                "memberUids" to newUids,
                "memberNames" to newNames,
                "memberRoles" to newRoles,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("groups").document(group.id).update(payload).await()
        }
    }

    suspend fun updateGroupPblWorkspace(
        groupId: String,
        topic: String?,
        phase: Int,
        rootProblem: String?,
        hypothesis: String?,
        investigationNotes: String?,
        conservationStrategy: String?,
        campaignCaption: String?,
        discussionSummary: String? = null,
        memberRoles: Map<String, String>? = null
    ): Result<Unit> = runFirestoreWrite(OperationType.UPDATE, "groups/$groupId") {
        val payload = mutableMapOf<String, Any>(
            "pblPhase" to phase.coerceIn(1, 5),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        topic?.let { payload["topic"] = it.take(200) }
        rootProblem?.let { payload["rootProblem"] = it.take(3000) }
        hypothesis?.let { payload["hypothesis"] = it.take(3000) }
        investigationNotes?.let { payload["investigationNotes"] = it.take(5000) }
        conservationStrategy?.let { payload["conservationStrategy"] = it.take(4000) }
        campaignCaption?.let { payload["campaignCaption"] = it.take(2000) }
        discussionSummary?.let { payload["discussionSummary"] = it.take(4000) }
        memberRoles?.let { payload["memberRoles"] = it }
        db.collection("groups").document(groupId).update(payload).await()
    }

    suspend fun updateGroupLeaderAndRoles(
        groupId: String,
        leaderUid: String,
        memberRoles: Map<String, String>,
        openToJoin: Boolean
    ): Result<Unit> = runFirestoreWrite(OperationType.UPDATE, "groups/$groupId") {
        db.collection("groups").document(groupId).update(
            mapOf(
                "leaderUid" to leaderUid,
                "memberRoles" to memberRoles,
                "openToJoin" to openToJoin,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun assignMembersToGroup(
        groupId: String,
        leaderUid: String,
        memberUids: List<String>,
        memberNames: List<String>,
        memberRoles: Map<String, String>
    ): Result<Unit> = runFirestoreWrite(OperationType.UPDATE, "groups/$groupId") {
        db.collection("groups").document(groupId).update(
            mapOf(
                "leaderUid" to leaderUid,
                "memberUids" to memberUids.take(12),
                "memberNames" to memberNames.take(12),
                "memberRoles" to memberRoles,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun saveGroupTeacherComment(groupId: String, teacherComment: String): Result<Unit> =
        runFirestoreWrite(OperationType.UPDATE, "groups/$groupId") {
            db.collection("groups").document(groupId).update(
                mapOf(
                    "teacherComment" to teacherComment.trim().take(2000),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

    fun observeGroupDiscussions(groupId: String, classId: String): Flow<List<GroupDiscussionMessage>> {
        if (groupId.isBlank() || classId.isBlank()) return flowOf(emptyList())
        return db.collection("group_discussions")
            .whereEqualTo("groupId", groupId)
            .whereEqualTo("classId", classId)
            .observeListFlow<GroupDiscussionMessage>("group_discussions")
            .map { list ->
                list.sortedBy { it.createdAt?.toDate()?.time ?: Long.MAX_VALUE }
            }
    }

    suspend fun sendGroupDiscussionMessage(
        groupId: String,
        classId: String,
        teacherUid: String,
        senderUid: String,
        senderName: String,
        senderRole: String,
        pblPhase: Int,
        message: String
    ): Result<Unit> {
        val cleanMsg = message.trim().take(2000)
        if (cleanMsg.isBlank()) return Result.failure(IllegalArgumentException("Pesan diskusi tidak boleh kosong."))
        val msgId = "msg_${UUID.randomUUID().toString().take(10)}"
        return runFirestoreWrite(OperationType.CREATE, "group_discussions/$msgId") {
            db.collection("group_discussions").document(msgId).set(
                mapOf(
                    "groupId" to groupId,
                    "classId" to classId,
                    "teacherUid" to teacherUid,
                    "senderUid" to senderUid,
                    "senderName" to senderName.trim().take(100),
                    "senderRole" to if (senderRole == "GURU" || senderRole == "ADMIN") "GURU" else "SISWA",
                    "pblPhase" to pblPhase.coerceIn(1, 5),
                    "message" to cleanMsg,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    fun observePublishedAssignments(): Flow<List<Assignment>> =
        db.collection("assignments")
            .whereEqualTo("published", true)
            .observeListFlow<Assignment>("assignments")
            .map { remote ->
                val merged = ParijotoAssessmentsAndGlossary.defaultAssignments.associateBy { it.id }.toMutableMap()
                remote.forEach { merged[it.id] = it }
                merged.values.sortedWith(compareBy({ it.meetingNumber }, { it.pblPhase }))
            }

    suspend fun createAssignment(assignment: Assignment, teacherUid: String): Result<Unit> {
        val docId = assignment.id.ifBlank { "asg_${UUID.randomUUID().toString().take(8)}" }
        return runFirestoreWrite(OperationType.CREATE, "assignments/$docId") {
            val payload = mutableMapOf<String, Any>(
                "classId" to assignment.classId.ifBlank { "DEFAULT" },
                "teacherUid" to teacherUid,
                "moduleId" to assignment.moduleId,
                "pblPhase" to assignment.pblPhase.coerceIn(1, 5),
                "title" to assignment.title.trim().take(200),
                "instructions" to assignment.instructions.trim().take(5000),
                "type" to assignment.type,
                "meetingNumber" to assignment.meetingNumber.coerceIn(1, 4),
                "published" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            assignment.dueDateLabel?.takeIf { it.isNotBlank() }?.let { payload["dueDateLabel"] = it.take(60) }
            assignment.rubricId?.takeIf { it.isNotBlank() }?.let { payload["rubricId"] = it.take(128) }
            db.collection("assignments").document(docId).set(payload).await()
        }
    }

    fun observeStudentSubmissions(studentUid: String): Flow<List<Submission>> =
        db.collection("submissions")
            .whereEqualTo("studentUid", studentUid)
            .observeListFlow<Submission>("submissions")

    fun observeTeacherSubmissions(teacherUid: String): Flow<List<Submission>> =
        db.collection("submissions")
            .whereEqualTo("teacherUid", teacherUid)
            .observeListFlow<Submission>("submissions")

    fun observeClassPresentations(classId: String): Flow<List<Submission>> {
        if (classId.isBlank() || classId == "DEFAULT") return flowOf(emptyList())
        return db.collection("submissions")
            .whereEqualTo("classId", classId)
            .whereIn("status", listOf("SUBMITTED", "GRADED"))
            .observeListFlow<Submission>("submissions")
    }

    fun observePeerReviewsForClass(classId: String): Flow<List<PeerReview>> {
        if (classId.isBlank() || classId == "DEFAULT") return flowOf(emptyList())
        return db.collection("peer_reviews")
            .whereEqualTo("classId", classId)
            .observeListFlow<PeerReview>("peer_reviews")
    }

    suspend fun submitPeerReview(review: PeerReview): Result<Unit> {
        if (review.reviewerUid == review.targetStudentUid) {
            return Result.failure(IllegalArgumentException("Tidak dapat menilai karya sendiri."))
        }
        if (review.comment.isBlank()) {
            return Result.failure(IllegalArgumentException("Tuliskan komentar penilaian antarteman."))
        }
        val docId = "${review.submissionId}_${review.reviewerUid}"
        return runFirestoreWrite(OperationType.WRITE, "peer_reviews/$docId") {
            val ref = db.collection("peer_reviews").document(docId)
            val existing = ref.get().await()
            val payload = mutableMapOf<String, Any>(
                "submissionId" to review.submissionId,
                "assignmentId" to review.assignmentId,
                "classId" to review.classId,
                "teacherUid" to review.teacherUid,
                "reviewerUid" to review.reviewerUid,
                "reviewerName" to review.reviewerName.trim().take(100),
                "targetStudentUid" to review.targetStudentUid,
                "targetStudentName" to review.targetStudentName.trim().take(100),
                "clarityScore" to review.clarityScore.coerceIn(1, 4),
                "evidenceScore" to review.evidenceScore.coerceIn(1, 4),
                "solutionScore" to review.solutionScore.coerceIn(1, 4),
                "comment" to review.comment.trim().take(1500),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!existing.exists()) {
                payload["createdAt"] = FieldValue.serverTimestamp()
                ref.set(payload).await()
            } else {
                ref.set(payload, SetOptions.merge()).await()
            }
        }
    }

    suspend fun saveSubmission(
        submission: Submission,
        submitFinal: Boolean
    ): Result<Unit> {
        val mime = submission.attachmentMimeType
        val size = submission.attachmentSizeBytes
        if (mime != null && ALLOWED_MIME_PREFIXES.none { mime.startsWith(it, ignoreCase = true) }) {
            return Result.failure(IllegalArgumentException("Tipe lampiran tidak didukung. Gunakan gambar (JPG/PNG/WebP) atau PDF."))
        }
        if (size != null && size > MAX_ATTACHMENT_BYTES) {
            return Result.failure(IllegalArgumentException("Ukuran berkas lampiran melebihi batas maksimal 5 MB."))
        }

        val docId = submission.id.ifBlank { "${submission.assignmentId}_${submission.studentUid}" }
        return runFirestoreWrite(OperationType.WRITE, "submissions/$docId") {
            val existing = db.collection("submissions").document(docId).get().await()
            val payload = mutableMapOf<String, Any>(
                "assignmentId" to submission.assignmentId,
                "classId" to submission.classId.ifBlank { "DEFAULT" },
                "teacherUid" to submission.teacherUid.ifBlank { "SYSTEM_DEFAULT" },
                "studentUid" to submission.studentUid,
                "studentName" to submission.studentName.trim().take(100),
                "pblPhase" to submission.pblPhase.coerceIn(1, 5),
                "status" to if (submitFinal) "SUBMITTED" else "DRAFT",
                "updatedAt" to FieldValue.serverTimestamp()
            )
            submission.groupId?.takeIf { it.isNotBlank() }?.let { payload["groupId"] = it.take(128) }
            submission.problemAnalysis?.takeIf { it.isNotBlank() }?.let { payload["problemAnalysis"] = it.take(4000) }
            submission.hypothesis?.takeIf { it.isNotBlank() }?.let { payload["hypothesis"] = it.take(4000) }
            submission.scientificEvidence?.takeIf { it.isNotBlank() }?.let { payload["scientificEvidence"] = it.take(5000) }
            submission.dataAnalysisTable?.takeIf { it.isNotBlank() }?.let { payload["dataAnalysisTable"] = it.take(4000) }
            submission.conservationSolution?.takeIf { it.isNotBlank() }?.let { payload["conservationSolution"] = it.take(4000) }
            submission.discussionSummary?.takeIf { it.isNotBlank() }?.let { payload["discussionSummary"] = it.take(4000) }
            submission.cerClaim?.takeIf { it.isNotBlank() }?.let { payload["cerClaim"] = it.take(2000) }
            submission.cerEvidence?.takeIf { it.isNotBlank() }?.let { payload["cerEvidence"] = it.take(3000) }
            submission.cerReasoning?.takeIf { it.isNotBlank() }?.let { payload["cerReasoning"] = it.take(3000) }
            submission.campaignCaption?.takeIf { it.isNotBlank() }?.let { payload["campaignCaption"] = it.take(2000) }
            submission.attachmentName?.takeIf { it.isNotBlank() }?.let { payload["attachmentName"] = it.take(200) }
            mime?.takeIf { it.isNotBlank() }?.let { payload["attachmentMimeType"] = it.take(100) }
            if (size != null) payload["attachmentSizeBytes"] = size.coerceIn(0L, MAX_ATTACHMENT_BYTES)
            submission.attachmentUrl?.takeIf { it.isNotBlank() }?.let { payload["attachmentUrl"] = it.take(1000) }

            if (!existing.exists()) {
                payload["createdAt"] = FieldValue.serverTimestamp()
                db.collection("submissions").document(docId).set(payload).await()
            } else {
                db.collection("submissions").document(docId).set(payload, SetOptions.merge()).await()
            }
        }
    }

    suspend fun returnSubmissionForRevision(
        submissionId: String,
        teacherFeedback: String
    ): Result<Unit> = runFirestoreWrite(OperationType.UPDATE, "submissions/$submissionId") {
        db.collection("submissions").document(submissionId).update(
            mapOf(
                "status" to "REVISION_REQUIRED",
                "teacherFeedback" to teacherFeedback.trim().take(2000),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }


    fun observeStudentReflections(studentUid: String): Flow<List<Reflection>> =
        db.collection("reflections")
            .whereEqualTo("studentUid", studentUid)
            .observeListFlow<Reflection>("reflections")

    fun observeTeacherReflections(teacherUid: String): Flow<List<Reflection>> =
        db.collection("reflections")
            .whereEqualTo("teacherUid", teacherUid)
            .observeListFlow<Reflection>("reflections")

    suspend fun submitReflection(reflection: Reflection): Result<Unit> {
        val docId = reflection.id.ifBlank { "ref_${reflection.moduleId}_${reflection.studentUid}" }
        return runFirestoreWrite(OperationType.WRITE, "reflections/$docId") {
            val ref = db.collection("reflections").document(docId)
            val existing = ref.get().await()
            val payload = mutableMapOf<String, Any>(
                "studentUid" to reflection.studentUid,
                "studentName" to reflection.studentName.trim().take(100),
                "teacherUid" to reflection.teacherUid.ifBlank { "SYSTEM_DEFAULT" },
                "classId" to reflection.classId.ifBlank { "DEFAULT" },
                "moduleId" to reflection.moduleId,
                "checklistYesCount" to reflection.answers.count { it },
                "totalChecklist" to reflection.answers.size.coerceAtLeast(1),
                "answers" to reflection.answers,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            reflection.notes?.takeIf { it.isNotBlank() }?.let { payload["notes"] = it.take(2000) }
            reflection.pblReflection?.takeIf { it.isNotBlank() }?.let { payload["pblReflection"] = it.take(3000) }
            if (!existing.exists()) {
                payload["createdAt"] = FieldValue.serverTimestamp()
                ref.set(payload).await()
            } else {
                ref.set(payload, SetOptions.merge()).await()
            }
        }
    }

    fun observePublishedAnnouncements(): Flow<List<Announcement>> =
        db.collection("announcements")
            .whereEqualTo("published", true)
            .observeListFlow<Announcement>("announcements")

    suspend fun createAnnouncement(
        classId: String,
        teacherUid: String,
        teacherName: String,
        title: String,
        message: String,
        pinned: Boolean
    ): Result<Unit> {
        val docId = "ann_${UUID.randomUUID().toString().take(8)}"
        return runFirestoreWrite(OperationType.CREATE, "announcements/$docId") {
            db.collection("announcements").document(docId).set(
                mapOf(
                    "classId" to classId.ifBlank { "ALL" },
                    "teacherUid" to teacherUid,
                    "teacherName" to teacherName.trim().take(100),
                    "title" to title.trim().take(200),
                    "message" to message.trim().take(3000),
                    "pinned" to pinned,
                    "published" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }
}
