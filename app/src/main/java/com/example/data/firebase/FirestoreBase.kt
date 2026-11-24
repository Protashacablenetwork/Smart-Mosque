package com.example.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal class FirestoreBase(
    val db: FirebaseFirestore,
    val auth: FirebaseAuth
) {
    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun <T> observeCollection(
        collectionName: String,
        clazz: Class<T>,
        requiresAuth: Boolean = true,
        sorter: (List<T>) -> List<T>
    ): Flow<List<T>> = flow {
        val uid = if (requiresAuth) requireUserId() else null
        emitAll(
            db.collection(collectionName)
                .snapshots()
                .map { snapshot ->
                    val items = snapshot.toObjects(clazz, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    sorter(items)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && (!requiresAuth || auth.currentUser?.uid == uid)) {
                        handleFirestoreError(toLog, OperationType.LIST, collectionName)
                    }
                    throw error
                }
        )
    }

    fun <T> observeQuery(
        pathLabel: String,
        query: Query,
        clazz: Class<T>,
        sorter: (List<T>) -> List<T>
    ): Flow<List<T>> = flow {
        val uid = requireUserId()
        emitAll(
            query.snapshots()
                .map { snapshot ->
                    val items = snapshot.toObjects(clazz, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    sorter(items)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, pathLabel)
                    }
                    throw error
                }
        )
    }

    suspend fun logAudit(
        action: String,
        tableName: String,
        recordId: String?,
        summary: String?,
        beforeSummary: String? = null,
        afterSummary: String? = null
    ) {
        try {
            val user = auth.currentUser ?: return
            val payload = mapOf(
                "userId" to user.uid,
                "userEmail" to user.email?.take(120),
                "action" to action.take(100),
                "tableName" to tableName.take(50),
                "recordId" to recordId?.take(128),
                "summary" to summary?.take(250),
                "beforeSummary" to beforeSummary?.take(300),
                "afterSummary" to afterSummary?.take(300),
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            db.collection("audit_logs").document().set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "audit_logs")
        }
    }

    fun todayIso(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}
