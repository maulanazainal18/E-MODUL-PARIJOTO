package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

object FirestoreClientProvider {
    @Volatile
    private var instance: FirebaseFirestore? = null

    fun get(context: Context): FirebaseFirestore {
        return instance ?: synchronized(this) {
            instance ?: FirebaseFirestore.getInstance(
                context.applicationContext.getString(R.string.firestore_database_id)
            ).also { db ->
                runCatching {
                    val cacheSettings = PersistentCacheSettings.newBuilder()
                        .setSizeBytes(100L * 1024L * 1024L)
                        .build()
                    db.firestoreSettings = FirebaseFirestoreSettings.Builder()
                        .setLocalCacheSettings(cacheSettings)
                        .build()
                }
                instance = db
            }
        }
    }
}

inline fun <reified T : Any> Query.observeListFlow(
    path: String
): Flow<List<T>> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) {
            handleFirestoreError(error, OperationType.LIST, path)
            close(error)
            return@addSnapshotListener
        }
        val items = snapshot?.documents?.mapNotNull { doc ->
            runCatching {
                doc.toObject(T::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }.getOrNull()
        } ?: emptyList()
        trySend(items)
    }
    awaitClose { registration.remove() }
}

inline fun <reified T : Any> com.google.firebase.firestore.DocumentReference.observeDocFlow(
    path: String
): Flow<T?> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) {
            handleFirestoreError(error, OperationType.GET, path)
            close(error)
            return@addSnapshotListener
        }
        val item = snapshot?.let { doc ->
            if (doc.exists()) {
                runCatching {
                    doc.toObject(T::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.getOrNull()
            } else {
                null
            }
        }
        trySend(item)
    }
    awaitClose { registration.remove() }
}

suspend inline fun <T> runFirestoreWrite(
    operationType: OperationType,
    path: String,
    crossinline block: suspend () -> T
): Result<T> {
    return try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, operationType, path)
        Result.failure(e)
    }
}
