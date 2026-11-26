package com.example.data.repository

import android.content.Context
import com.example.data.auth.AuthRepository
import com.example.data.firebase.OperationType
import com.example.data.model.ClassRoom
import com.example.data.model.Enrollment
import com.example.data.model.GradeRecord
import com.example.data.model.QuizAttempt
import com.example.data.model.Reflection
import com.example.data.model.Submission
import com.example.data.model.SystemSetting
import com.example.data.model.UserAccount
import com.example.data.model.UserRole
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class AdminAndUserRepository(context: Context) {

    private val db: FirebaseFirestore = FirestoreClientProvider.get(context)

    fun observeUserAccount(uid: String): Flow<UserAccount?> =
        db.collection("users").document(uid).observeDocFlow<UserAccount>("users/$uid")

    fun observeSystemSetting(): Flow<SystemSetting> =
        db.collection("system_settings")
            .document("default")
            .observeDocFlow<SystemSetting>("system_settings/default")
            .map { it ?: SystemSetting(id = "default") }

    suspend fun provisionUserProfile(
        firebaseUser: FirebaseUser,
        nama: String,
        requestedRole: UserRole
    ): Result<UserAccount> {
        val uid = firebaseUser.uid
        val email = firebaseUser.email.orEmpty().trim()
        val isBootstrapAdmin =
            email.equals(AuthRepository.BOOTSTRAPPED_ADMIN_EMAIL, ignoreCase = true) &&
                firebaseUser.isEmailVerified
        val assignedRole = if (isBootstrapAdmin) {
            UserRole.ADMIN
        } else if (requestedRole == UserRole.ADMIN) {
            UserRole.SISWA
        } else {
            requestedRole
        }

        return runFirestoreWrite(OperationType.CREATE, "users/$uid") {
            val cleanName = nama.trim().ifBlank { firebaseUser.displayName ?: "Pengguna" }.take(100)
            val payload = mutableMapOf<String, Any>(
                "uid" to uid,
                "nama" to cleanName,
                "email" to email.take(150),
                "role" to assignedRole.name,
                "accountStatus" to "ACTIVE",
                "academicYear" to "2026/2027",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firebaseUser.photoUrl?.toString()?.take(500)?.let { payload["photoUrl"] = it }

            db.collection("users").document(uid).set(payload).await()

            if (isBootstrapAdmin) {
                db.collection("admins").document(uid).set(
                    mapOf(
                        "email" to email,
                        "grantedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                val sysRef = db.collection("system_settings").document("default")
                if (!sysRef.get().await().exists()) {
                    sysRef.set(
                        mapOf(
                            "academicYear" to "2026/2027",
                            "semester" to "Ganjil",
                            "schoolName" to "SMA/MA Kelas X - Lereng Muria",
                            "maintenanceMode" to false,
                            "allowRegistration" to true,
                            "updatedBy" to uid,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }

            UserAccount(
                id = uid,
                uid = uid,
                nama = cleanName,
                email = email,
                role = assignedRole.name,
                accountStatus = "ACTIVE",
                academicYear = "2026/2027"
            )
        }
    }

    suspend fun updateOwnDisplayName(uid: String, newName: String): Result<Unit> {
        val clean = newName.trim().take(100)
        if (clean.isBlank()) return Result.failure(IllegalArgumentException("Nama tidak boleh kosong."))
        return runFirestoreWrite(OperationType.UPDATE, "users/$uid") {
            db.collection("users").document(uid).update(
                mapOf(
                    "nama" to clean,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    fun observeAllUsersAdmin(): Flow<List<UserAccount>> =
        db.collection("users").observeListFlow<UserAccount>("users")

    fun observeAllClassesAdmin(): Flow<List<ClassRoom>> =
        db.collection("classes").observeListFlow<ClassRoom>("classes")

    fun observeAllEnrollmentsAdmin(): Flow<List<Enrollment>> =
        db.collection("enrollments").observeListFlow<Enrollment>("enrollments")

    fun observeAllSubmissionsAdmin(): Flow<List<Submission>> =
        db.collection("submissions").observeListFlow<Submission>("submissions")

    fun observeAllAttemptsAdmin(): Flow<List<QuizAttempt>> =
        db.collection("quiz_attempts").observeListFlow<QuizAttempt>("quiz_attempts")

    fun observeAllGradesAdmin(): Flow<List<GradeRecord>> =
        db.collection("grades").observeListFlow<GradeRecord>("grades")

    fun observeAllReflectionsAdmin(): Flow<List<Reflection>> =
        db.collection("reflections").observeListFlow<Reflection>("reflections")

    fun observeAllAssignmentsAdmin(): Flow<List<com.example.data.model.Assignment>> =
        db.collection("assignments").observeListFlow<com.example.data.model.Assignment>("assignments")
            .map { remote ->
                val merged = com.example.data.curriculum.ParijotoAssessmentsAndGlossary.defaultAssignments
                    .associateBy { it.id }.toMutableMap()
                remote.forEach { merged[it.id] = it }
                merged.values.toList()
            }

    fun observeAllQuizzesAdmin(): Flow<List<com.example.data.model.Quiz>> =
        db.collection("quizzes").observeListFlow<com.example.data.model.Quiz>("quizzes")
            .map { remote ->
                val defaults = listOf(
                    com.example.data.curriculum.ParijotoAssessmentsAndGlossary.quizModul1,
                    com.example.data.curriculum.ParijotoAssessmentsAndGlossary.quizModul2,
                    com.example.data.curriculum.ParijotoAssessmentsAndGlossary.quizPostTest
                )
                val merged = defaults.associateBy { it.id }.toMutableMap()
                remote.forEach { merged[it.id] = it }
                merged.values.toList()
            }

    suspend fun updateUserAccountAdmin(
        targetUser: UserAccount,
        newRole: UserRole,
        newStatus: String
    ): Result<Unit> = runFirestoreWrite(OperationType.UPDATE, "users/${targetUser.uid}") {
        db.collection("users").document(targetUser.uid).update(
            mapOf(
                "role" to newRole.name,
                "accountStatus" to newStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        val adminRef = db.collection("admins").document(targetUser.uid)
        if (newRole == UserRole.ADMIN) {
            adminRef.set(
                mapOf("email" to targetUser.email, "grantedAt" to FieldValue.serverTimestamp())
            ).await()
        } else {
            val snap = adminRef.get().await()
            if (snap.exists()) adminRef.delete().await()
        }
    }

    suspend fun updateSystemSettingAdmin(setting: SystemSetting, adminUid: String): Result<Unit> =
        runFirestoreWrite(OperationType.WRITE, "system_settings/default") {
            val ref = db.collection("system_settings").document("default")
            val existing = ref.get().await()
            val payload = mutableMapOf<String, Any>(
                "academicYear" to setting.academicYear.trim().take(30),
                "semester" to setting.semester.trim().take(30),
                "schoolName" to setting.schoolName.trim().take(120),
                "maintenanceMode" to setting.maintenanceMode,
                "allowRegistration" to setting.allowRegistration,
                "updatedBy" to adminUid,
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
