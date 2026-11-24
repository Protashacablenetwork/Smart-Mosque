package com.example.data.firebase

import com.example.data.model.Family
import com.example.data.model.MeatDistribution
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToLong

internal class SocietyCommunityDelegate(
    private val base: FirestoreBase,
    private val finance: SocietyFinanceDelegate
) {
    private val db get() = base.db

    private fun roundKg(value: Double): Double = (value * 1000.0).roundToLong() / 1000.0

    suspend fun createQurbaniBatchAndDistributions(
        eventName: String,
        animalInfo: String?,
        purchaseCost: Double?,
        totalMeatKg: Double,
        eventDate: String,
        activeFamilies: List<Family>,
        autoDistribute: Boolean = true
    ): Result<Unit> {
        val cleanName = eventName.trim().take(120)
        val cleanAnimal = animalInfo?.trim()?.takeIf { it.isNotBlank() }?.take(150)
        val cleanDate = eventDate.trim().ifBlank { base.todayIso() }.take(20)
        return try {
            val uid = base.requireUserId()
            require(cleanName.isNotEmpty()) { "Event name is required" }
            require(totalMeatKg > 0.0) { "Total meat kg must be greater than zero" }

            val targetFamilies = if (autoDistribute) activeFamilies else emptyList()
            val familyCount = targetFamilies.size
            val perFamily = if (familyCount > 0) roundKg(totalMeatKg / familyCount.toDouble()) else 0.0

            val batchRef = db.collection("qurbani_batches").document()
            val batchPayload = mapOf(
                "eventName" to cleanName,
                "animalInfo" to cleanAnimal,
                "purchaseCost" to purchaseCost?.takeIf { it >= 0.0 },
                "totalMeatKg" to totalMeatKg,
                "totalFamilies" to familyCount.toLong(),
                "distributionPerFamily" to perFamily,
                "eventDate" to cleanDate,
                "status" to if (familyCount > 0) "distributing" else "draft",
                "createdBy" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            val writeBatch = db.batch()
            writeBatch.set(batchRef, batchPayload)
            writeBatch.commit().await()

            if (targetFamilies.isNotEmpty()) {
                targetFamilies.chunked(400).forEach { chunk ->
                    val chunkBatch = db.batch()
                    chunk.forEach { fam ->
                        val distRef = db.collection("meat_distributions").document("${batchRef.id}_${fam.id}")
                        val distPayload = mapOf(
                            "batchId" to batchRef.id,
                            "familyId" to fam.id,
                            "familyName" to fam.familyName.take(100),
                            "familyCode" to fam.familyCode.take(30),
                            "allocatedKg" to perFamily,
                            "deliveredKg" to 0.0,
                            "status" to "pending",
                            "createdBy" to uid,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                        chunkBatch.set(distRef, distPayload)
                    }
                    chunkBatch.commit().await()
                }
            }

            base.logAudit(
                "GENERATE_QURBANI",
                "qurbani_batches",
                batchRef.id,
                "$cleanName: ${totalMeatKg}kg / $familyCount families = ${perFamily}kg each"
            )
            finance.refreshPublicSummary()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "qurbani_batches")
            Result.failure(e)
        }
    }

    suspend fun loadSocietyFamiliesForQurbaniYear(
        year: String,
        activeFamilies: List<Family>,
        existingYearBatches: List<com.example.data.model.QurbaniBatch>,
        existingDistributions: List<MeatDistribution>
    ): Result<Unit> {
        return try {
            val uid = base.requireUserId()
            require(activeFamilies.isNotEmpty()) { "No active families found. Please add families first." }

            val targetBatch = existingYearBatches.firstOrNull()
            val batchId: String
            val totalYearMeatKg = existingYearBatches.sumOf { it.totalMeatKg }
            val existingForBatch = if (targetBatch != null) {
                existingDistributions.filter { it.batchId == targetBatch.id }
            } else emptyList()

            val existingFamilyIds = existingForBatch.map { it.familyId }.toSet()
            val missingFamilies = activeFamilies.filter { it.id !in existingFamilyIds }
            val totalCount = existingForBatch.size + missingFamilies.size
            val effectiveMeat = if (targetBatch != null && targetBatch.totalMeatKg > 0.0) {
                targetBatch.totalMeatKg
            } else {
                totalYearMeatKg
            }
            val perFamily = if (effectiveMeat > 0.0 && totalCount > 0) roundKg(effectiveMeat / totalCount.toDouble()) else 0.0

            if (targetBatch == null) {
                val batchRef = db.collection("qurbani_batches").document()
                batchId = batchRef.id
                val batchPayload = mapOf(
                    "eventName" to "কুরবানি বণ্টন $year",
                    "totalMeatKg" to effectiveMeat,
                    "totalFamilies" to totalCount.toLong(),
                    "distributionPerFamily" to perFamily,
                    "eventDate" to "$year-06-17",
                    "status" to "distributing",
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                val firstChunk = missingFamilies.take(390)
                val firstWrite = db.batch()
                firstWrite.set(batchRef, batchPayload)
                firstChunk.forEach { fam ->
                    val distRef = db.collection("meat_distributions").document("${batchId}_${fam.id}")
                    firstWrite.set(
                        distRef,
                        mapOf(
                            "batchId" to batchId,
                            "familyId" to fam.id,
                            "familyName" to fam.familyName.take(100),
                            "familyCode" to fam.familyCode.take(30),
                            "allocatedKg" to perFamily,
                            "deliveredKg" to 0.0,
                            "status" to "pending",
                            "createdBy" to uid,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                firstWrite.commit().await()
                missingFamilies.drop(390).chunked(400).forEach { chunk ->
                    val write = db.batch()
                    chunk.forEach { fam ->
                        val distRef = db.collection("meat_distributions").document("${batchId}_${fam.id}")
                        write.set(
                            distRef,
                            mapOf(
                                "batchId" to batchId,
                                "familyId" to fam.id,
                                "familyName" to fam.familyName.take(100),
                                "familyCode" to fam.familyCode.take(30),
                                "allocatedKg" to perFamily,
                                "deliveredKg" to 0.0,
                                "status" to "pending",
                                "createdBy" to uid,
                                "createdAt" to FieldValue.serverTimestamp(),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                    write.commit().await()
                }
            } else {
                batchId = targetBatch.id
                db.collection("qurbani_batches").document(batchId).update(
                    mapOf(
                        "totalFamilies" to totalCount.toLong(),
                        "distributionPerFamily" to perFamily,
                        "status" to if (existingForBatch.all { it.status == "delivered" } && missingFamilies.isEmpty()) "completed" else "distributing",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                missingFamilies.chunked(400).forEach { chunk ->
                    val write = db.batch()
                    chunk.forEach { fam ->
                        val distRef = db.collection("meat_distributions").document("${batchId}_${fam.id}")
                        write.set(
                            distRef,
                            mapOf(
                                "batchId" to batchId,
                                "familyId" to fam.id,
                                "familyName" to fam.familyName.take(100),
                                "familyCode" to fam.familyCode.take(30),
                                "allocatedKg" to perFamily,
                                "deliveredKg" to 0.0,
                                "status" to "pending",
                                "createdBy" to uid,
                                "createdAt" to FieldValue.serverTimestamp(),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                    write.commit().await()
                }

                if (perFamily > 0.0) {
                    existingForBatch.filter { it.status == "pending" }.chunked(400).forEach { chunk ->
                        val write = db.batch()
                        chunk.forEach { dist ->
                            write.update(
                                db.collection("meat_distributions").document(dist.id),
                                mapOf("allocatedKg" to perFamily, "updatedAt" to FieldValue.serverTimestamp())
                            )
                        }
                        write.commit().await()
                    }
                }
            }

            base.logAudit(
                "LOAD_QURBANI_FAMILIES",
                "qurbani_batches",
                batchId,
                "Loaded ${missingFamilies.size} active families for $year (total $totalCount)"
            )
            finance.refreshPublicSummary()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "qurbani_batches")
            Result.failure(e)
        }
    }

    suspend fun addFamilyToQurbaniDistribution(
        year: String,
        targetBatch: com.example.data.model.QurbaniBatch?,
        family: Family,
        allocatedKg: Double
    ): Result<Unit> {
        return try {
            val uid = base.requireUserId()
            require(allocatedKg >= 0.0) { "Allocated kg cannot be negative" }

            if (targetBatch == null) {
                val batchRef = db.collection("qurbani_batches").document()
                val distRef = db.collection("meat_distributions").document("${batchRef.id}_${family.id}")
                val write = db.batch()
                write.set(
                    batchRef,
                    mapOf(
                        "eventName" to "কুরবানি বণ্টন $year",
                        "totalMeatKg" to allocatedKg,
                        "totalFamilies" to 1L,
                        "distributionPerFamily" to allocatedKg,
                        "eventDate" to "$year-06-17",
                        "status" to "distributing",
                        "createdBy" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                write.set(
                    distRef,
                    mapOf(
                        "batchId" to batchRef.id,
                        "familyId" to family.id,
                        "familyName" to family.familyName.take(100),
                        "familyCode" to family.familyCode.take(30),
                        "allocatedKg" to allocatedKg,
                        "deliveredKg" to 0.0,
                        "status" to "pending",
                        "createdBy" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                write.commit().await()
                base.logAudit("ADD_QURBANI_RECIPIENT", "meat_distributions", distRef.id, "Added ${family.familyCode} (${allocatedKg}kg)")
            } else {
                val distRef = db.collection("meat_distributions").document("${targetBatch.id}_${family.id}")
                val existingSnap = distRef.get().await()
                val write = db.batch()
                if (existingSnap.exists()) {
                    write.update(
                        distRef,
                        mapOf(
                            "allocatedKg" to allocatedKg,
                            "familyName" to family.familyName.take(100),
                            "familyCode" to family.familyCode.take(30),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    write.set(
                        distRef,
                        mapOf(
                            "batchId" to targetBatch.id,
                            "familyId" to family.id,
                            "familyName" to family.familyName.take(100),
                            "familyCode" to family.familyCode.take(30),
                            "allocatedKg" to allocatedKg,
                            "deliveredKg" to 0.0,
                            "status" to "pending",
                            "createdBy" to uid,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    write.update(
                        db.collection("qurbani_batches").document(targetBatch.id),
                        mapOf(
                            "totalFamilies" to (targetBatch.totalFamilies + 1L),
                            "status" to "distributing",
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                write.commit().await()
                base.logAudit("ADD_QURBANI_RECIPIENT", "meat_distributions", distRef.id, "Added ${family.familyCode} (${allocatedKg}kg)")
            }

            finance.refreshPublicSummary()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "meat_distributions")
            Result.failure(e)
        }
    }

    suspend fun recalculateQurbaniEqualShare(
        batch: com.example.data.model.QurbaniBatch,
        batchDistributions: List<MeatDistribution>,
        totalMeatKgOverride: Double? = null
    ): Result<Unit> {
        return try {
            base.requireUserId()
            val effectiveTotalMeat = (totalMeatKgOverride ?: batch.totalMeatKg).coerceAtLeast(0.0)
            val count = batchDistributions.size
            val perFamily = if (count > 0 && effectiveTotalMeat > 0.0) roundKg(effectiveTotalMeat / count.toDouble()) else 0.0

            db.collection("qurbani_batches").document(batch.id).update(
                mapOf(
                    "totalMeatKg" to effectiveTotalMeat,
                    "totalFamilies" to count.toLong(),
                    "distributionPerFamily" to perFamily,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            batchDistributions.filter { it.status == "pending" }.chunked(400).forEach { chunk ->
                val write = db.batch()
                chunk.forEach { dist ->
                    write.update(
                        db.collection("meat_distributions").document(dist.id),
                        mapOf("allocatedKg" to perFamily, "updatedAt" to FieldValue.serverTimestamp())
                    )
                }
                write.commit().await()
            }
            base.logAudit("RECALCULATE_QURBANI", "qurbani_batches", batch.id, "${effectiveTotalMeat}kg / $count = ${perFamily}kg")
            finance.refreshPublicSummary()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "qurbani_batches/${batch.id}")
            Result.failure(e)
        }
    }

    suspend fun deleteQurbaniBatch(batchId: String): Result<Unit> {
        return try {
            base.requireUserId()
            val distSnap = db.collection("meat_distributions").whereEqualTo("batchId", batchId).get().await()
            distSnap.documents.chunked(400).forEach { chunk ->
                val write = db.batch()
                chunk.forEach { doc -> write.delete(doc.reference) }
                write.commit().await()
            }
            db.collection("qurbani_batches").document(batchId).delete().await()
            base.logAudit("DELETE_QURBANI", "qurbani_batches", batchId, "Deleted Qurbani batch $batchId")
            finance.refreshPublicSummary()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "qurbani_batches/$batchId")
            Result.failure(e)
        }
    }

    suspend fun deleteMeatDistribution(distribution: MeatDistribution): Result<Unit> {
        return try {
            base.requireUserId()
            db.collection("meat_distributions").document(distribution.id).delete().await()
            val remaining = db.collection("meat_distributions").whereEqualTo("batchId", distribution.batchId).get().await()
            val batchDoc = db.collection("qurbani_batches").document(distribution.batchId).get().await()
            if (batchDoc.exists()) {
                val totalMeat = batchDoc.getDouble("totalMeatKg") ?: 0.0
                val count = remaining.size()
                val perFamily = if (count > 0 && totalMeat > 0.0) roundKg(totalMeat / count.toDouble()) else 0.0
                db.collection("qurbani_batches").document(distribution.batchId).update(
                    mapOf(
                        "totalFamilies" to count.toLong(),
                        "distributionPerFamily" to perFamily,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            base.logAudit("DELETE_MEAT_DIST", "meat_distributions", distribution.id, "Removed ${distribution.familyCode} from distribution")
            finance.refreshPublicSummary()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "meat_distributions/${distribution.id}")
            Result.failure(e)
        }
    }

    suspend fun markMeatDelivered(
        distribution: MeatDistribution,
        deliveredKg: Double,
        receivedBy: String
    ): Result<Unit> {
        val cleanReceiver = receivedBy.trim().take(100)
        val path = "meat_distributions/${distribution.id}"
        return try {
            base.requireUserId()
            require(cleanReceiver.isNotEmpty()) { "Receiver name is required" }
            require(deliveredKg > 0.0) { "Delivered kg must be positive" }

            db.collection("meat_distributions").document(distribution.id)
                .update(
                    mapOf(
                        "deliveredKg" to deliveredKg,
                        "receivedBy" to cleanReceiver,
                        "receivedAt" to FieldValue.serverTimestamp(),
                        "status" to "delivered",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

            val remainingPending = db.collection("meat_distributions")
                .whereEqualTo("batchId", distribution.batchId)
                .whereEqualTo("status", "pending")
                .get()
                .await()
            if (remainingPending.isEmpty) {
                db.collection("qurbani_batches").document(distribution.batchId)
                    .update(mapOf("status" to "completed", "updatedAt" to FieldValue.serverTimestamp()))
                    .await()
            }

            base.logAudit(
                "DELIVER_MEAT",
                "meat_distributions",
                distribution.id,
                "${distribution.familyCode} received ${deliveredKg}kg ($cleanReceiver)",
                beforeSummary = "status=${distribution.status}",
                afterSummary = "status=delivered (${deliveredKg}kg)"
            )
            finance.refreshPublicSummary()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveMeeting(
        meetingId: String?,
        title: String,
        meetingDate: String,
        meetingTime: String?,
        location: String?,
        agenda: String?,
        attendees: String?,
        decisions: String,
        assignedTo: String?,
        deadline: String?,
        status: String
    ): Result<Unit> {
        val cleanTitle = title.trim().take(120)
        val cleanLocation = location?.trim()?.takeIf { it.isNotBlank() }?.take(150)
        val cleanAgenda = agenda?.trim()?.takeIf { it.isNotBlank() }?.take(1000)
        val cleanDecisions = decisions.trim().ifBlank { cleanAgenda ?: "-" }.take(2000)
        val cleanDate = meetingDate.trim().ifBlank { base.todayIso() }.take(20)
        val cleanTime = meetingTime?.trim()?.takeIf { it.isNotBlank() }?.take(20)
        val cleanAttendees = attendees?.trim()?.takeIf { it.isNotBlank() }?.take(500)
        val cleanAssigned = assignedTo?.trim()?.takeIf { it.isNotBlank() }?.take(100)
        val cleanDeadline = deadline?.trim()?.takeIf { it.isNotBlank() }?.take(20)
        val cleanStatus = if (status in listOf("scheduled", "in_progress", "completed")) status else "scheduled"
        return try {
            val uid = base.requireUserId()
            require(cleanTitle.isNotEmpty()) { "Meeting title is required" }
            require(cleanDecisions.isNotEmpty()) { "Agenda or decisions required" }
            val col = db.collection("meetings")
            val docRef = if (meetingId.isNullOrBlank()) col.document() else col.document(meetingId)
            if (meetingId.isNullOrBlank()) {
                val payload = mapOf(
                    "title" to cleanTitle,
                    "meetingDate" to cleanDate,
                    "meetingTime" to cleanTime,
                    "location" to cleanLocation,
                    "agenda" to cleanAgenda,
                    "attendees" to cleanAttendees,
                    "decisions" to cleanDecisions,
                    "assignedTo" to cleanAssigned,
                    "deadline" to cleanDeadline,
                    "status" to cleanStatus,
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
                base.logAudit("CREATE_MEETING", "meetings", docRef.id, cleanTitle, afterSummary = "status=$cleanStatus")
            } else {
                docRef.update(
                    mapOf(
                        "title" to cleanTitle,
                        "meetingDate" to cleanDate,
                        "meetingTime" to (cleanTime ?: FieldValue.delete()),
                        "location" to (cleanLocation ?: FieldValue.delete()),
                        "agenda" to (cleanAgenda ?: FieldValue.delete()),
                        "attendees" to (cleanAttendees ?: FieldValue.delete()),
                        "decisions" to cleanDecisions,
                        "assignedTo" to (cleanAssigned ?: FieldValue.delete()),
                        "deadline" to (cleanDeadline ?: FieldValue.delete()),
                        "status" to cleanStatus,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                base.logAudit("UPDATE_MEETING", "meetings", docRef.id, cleanTitle, afterSummary = "status=$cleanStatus")
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "meetings/${meetingId ?: "new"}")
            Result.failure(e)
        }
    }

    suspend fun deleteMeeting(meetingId: String): Result<Unit> {
        return try {
            base.requireUserId()
            db.collection("meetings").document(meetingId).delete().await()
            base.logAudit("DELETE_MEETING", "meetings", meetingId, "Deleted meeting $meetingId")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "meetings/$meetingId")
            Result.failure(e)
        }
    }

    suspend fun saveCommitteeMember(
        memberId: String?,
        name: String,
        role: String,
        phone: String?,
        familyId: String?,
        familyName: String?,
        term: String?,
        active: Boolean
    ): Result<Unit> {
        val cleanName = name.trim().take(100)
        val cleanRole = role.trim().take(80)
        val cleanPhone = phone?.trim()?.takeIf { it.isNotBlank() }?.take(30)
        val cleanFamId = familyId?.trim()?.takeIf { it.isNotBlank() }?.take(128)
        val cleanFamName = familyName?.trim()?.takeIf { it.isNotBlank() }?.take(100)
        val cleanTerm = term?.trim()?.takeIf { it.isNotBlank() }?.take(40)
        return try {
            val uid = base.requireUserId()
            require(cleanName.isNotEmpty()) { "Committee member name is required" }
            require(cleanRole.isNotEmpty()) { "Committee role is required" }
            val col = db.collection("committees")
            val docRef = if (memberId.isNullOrBlank()) col.document() else col.document(memberId)
            if (memberId.isNullOrBlank()) {
                val payload = mapOf(
                    "name" to cleanName,
                    "role" to cleanRole,
                    "phone" to cleanPhone,
                    "familyId" to cleanFamId,
                    "familyName" to cleanFamName,
                    "term" to cleanTerm,
                    "active" to active,
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
                base.logAudit("CREATE_COMMITTEE", "committees", docRef.id, "$cleanName ($cleanRole)")
            } else {
                docRef.update(
                    mapOf(
                        "name" to cleanName,
                        "role" to cleanRole,
                        "phone" to (cleanPhone ?: FieldValue.delete()),
                        "familyId" to (cleanFamId ?: FieldValue.delete()),
                        "familyName" to (cleanFamName ?: FieldValue.delete()),
                        "term" to (cleanTerm ?: FieldValue.delete()),
                        "active" to active,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                base.logAudit("UPDATE_COMMITTEE", "committees", docRef.id, "$cleanName ($cleanRole)")
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "committees/${memberId ?: "new"}")
            Result.failure(e)
        }
    }

    suspend fun deleteCommitteeMember(memberId: String): Result<Unit> {
        return try {
            base.requireUserId()
            db.collection("committees").document(memberId).delete().await()
            base.logAudit("DELETE_COMMITTEE", "committees", memberId, "Deleted committee member $memberId")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "committees/$memberId")
            Result.failure(e)
        }
    }

    suspend fun saveEvent(
        eventId: String?,
        title: String,
        eventDate: String,
        eventTime: String?,
        location: String?,
        eventType: String,
        description: String?,
        rsvpEnabled: Boolean
    ): Result<Unit> {
        val cleanTitle = title.trim().take(120)
        val cleanDate = eventDate.trim().ifBlank { base.todayIso() }.take(20)
        val cleanTime = eventTime?.trim()?.takeIf { it.isNotBlank() }?.take(20)
        val cleanLoc = location?.trim()?.takeIf { it.isNotBlank() }?.take(150)
        val cleanType = if (eventType in listOf("religious", "social", "education", "informational")) eventType else "religious"
        val cleanDesc = description?.trim()?.takeIf { it.isNotBlank() }?.take(1500)
        return try {
            val uid = base.requireUserId()
            require(cleanTitle.isNotEmpty()) { "Event title is required" }
            val col = db.collection("events")
            val docRef = if (eventId.isNullOrBlank()) col.document() else col.document(eventId)
            if (eventId.isNullOrBlank()) {
                val payload = mapOf(
                    "title" to cleanTitle,
                    "eventDate" to cleanDate,
                    "eventTime" to cleanTime,
                    "location" to cleanLoc,
                    "eventType" to cleanType,
                    "description" to cleanDesc,
                    "rsvpEnabled" to rsvpEnabled,
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
                base.logAudit("CREATE_EVENT", "events", docRef.id, "$cleanTitle ($cleanDate)")
            } else {
                docRef.update(
                    mapOf(
                        "title" to cleanTitle,
                        "eventDate" to cleanDate,
                        "eventTime" to (cleanTime ?: FieldValue.delete()),
                        "location" to (cleanLoc ?: FieldValue.delete()),
                        "eventType" to cleanType,
                        "description" to (cleanDesc ?: FieldValue.delete()),
                        "rsvpEnabled" to rsvpEnabled,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                base.logAudit("UPDATE_EVENT", "events", docRef.id, "$cleanTitle ($cleanDate)")
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "events/${eventId ?: "new"}")
            Result.failure(e)
        }
    }

    suspend fun deleteEvent(eventId: String): Result<Unit> {
        return try {
            base.requireUserId()
            db.collection("events").document(eventId).delete().await()
            base.logAudit("DELETE_EVENT", "events", eventId, "Deleted event $eventId")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "events/$eventId")
            Result.failure(e)
        }
    }

    suspend fun saveAsset(
        assetId: String?,
        name: String,
        category: String,
        value: Double,
        purchaseDate: String?,
        condition: String,
        location: String?,
        note: String?
    ): Result<Unit> {
        val cleanName = name.trim().take(120)
        val cleanCat = category.trim().ifBlank { "General" }.take(60)
        val cleanDate = purchaseDate?.trim()?.takeIf { it.isNotBlank() }?.take(20)
        val cleanCond = if (condition in listOf("good", "needs_repair", "damaged")) condition else "good"
        val cleanLoc = location?.trim()?.takeIf { it.isNotBlank() }?.take(150)
        val cleanNote = note?.trim()?.takeIf { it.isNotBlank() }?.take(400)
        return try {
            val uid = base.requireUserId()
            require(cleanName.isNotEmpty()) { "Asset name is required" }
            require(value >= 0.0) { "Asset value cannot be negative" }
            val col = db.collection("assets")
            val docRef = if (assetId.isNullOrBlank()) col.document() else col.document(assetId)
            if (assetId.isNullOrBlank()) {
                val payload = mapOf(
                    "name" to cleanName,
                    "category" to cleanCat,
                    "value" to value,
                    "purchaseDate" to cleanDate,
                    "condition" to cleanCond,
                    "location" to cleanLoc,
                    "note" to cleanNote,
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
                base.logAudit("CREATE_ASSET", "assets", docRef.id, "$cleanName ($value)")
            } else {
                docRef.update(
                    mapOf(
                        "name" to cleanName,
                        "category" to cleanCat,
                        "value" to value,
                        "purchaseDate" to (cleanDate ?: FieldValue.delete()),
                        "condition" to cleanCond,
                        "location" to (cleanLoc ?: FieldValue.delete()),
                        "note" to (cleanNote ?: FieldValue.delete()),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                base.logAudit("UPDATE_ASSET", "assets", docRef.id, "$cleanName ($value)")
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "assets/${assetId ?: "new"}")
            Result.failure(e)
        }
    }

    suspend fun deleteAsset(assetId: String): Result<Unit> {
        return try {
            base.requireUserId()
            db.collection("assets").document(assetId).delete().await()
            base.logAudit("DELETE_ASSET", "assets", assetId, "Deleted asset $assetId")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "assets/$assetId")
            Result.failure(e)
        }
    }

    suspend fun saveDocument(
        docId: String?,
        title: String,
        category: String,
        docDate: String,
        fileName: String?,
        note: String?
    ): Result<Unit> {
        val cleanTitle = title.trim().take(120)
        val cleanCat = category.trim().ifBlank { "General" }.take(60)
        val cleanDate = docDate.trim().ifBlank { base.todayIso() }.take(20)
        val cleanFile = fileName?.trim()?.takeIf { it.isNotBlank() }?.take(150)
        val cleanNote = note?.trim()?.takeIf { it.isNotBlank() }?.take(500)
        return try {
            val uid = base.requireUserId()
            require(cleanTitle.isNotEmpty()) { "Document title is required" }
            val col = db.collection("documents")
            val docRef = if (docId.isNullOrBlank()) col.document() else col.document(docId)
            if (docId.isNullOrBlank()) {
                val payload = mapOf(
                    "title" to cleanTitle,
                    "category" to cleanCat,
                    "docDate" to cleanDate,
                    "fileName" to cleanFile,
                    "note" to cleanNote,
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
                base.logAudit("CREATE_DOCUMENT", "documents", docRef.id, cleanTitle)
            } else {
                docRef.update(
                    mapOf(
                        "title" to cleanTitle,
                        "category" to cleanCat,
                        "docDate" to cleanDate,
                        "fileName" to (cleanFile ?: FieldValue.delete()),
                        "note" to (cleanNote ?: FieldValue.delete()),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                base.logAudit("UPDATE_DOCUMENT", "documents", docRef.id, cleanTitle)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "documents/${docId ?: "new"}")
            Result.failure(e)
        }
    }

    suspend fun deleteDocument(docId: String): Result<Unit> {
        return try {
            base.requireUserId()
            db.collection("documents").document(docId).delete().await()
            base.logAudit("DELETE_DOCUMENT", "documents", docId, "Deleted document $docId")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "documents/$docId")
            Result.failure(e)
        }
    }

    suspend fun addWelfareRecord(
        recordType: String,
        beneficiaryName: String,
        phone: String?,
        plotNumber: String?,
        amount: Double,
        recordDate: String,
        note: String?,
        accountId: String? = null,
        accountName: String? = null
    ): Result<Unit> {
        val cleanName = beneficiaryName.trim().take(100)
        val cleanPhone = phone?.trim()?.takeIf { it.isNotBlank() }?.take(30)
        val cleanPlot = plotNumber?.trim()?.takeIf { it.isNotBlank() }?.take(40)
        val cleanNote = note?.trim()?.takeIf { it.isNotBlank() }?.take(400)
        val cleanDate = recordDate.trim().ifBlank { base.todayIso() }.take(20)
        val cleanAccId = accountId?.takeIf { it.isNotBlank() }
        val cleanAccName = accountName?.takeIf { it.isNotBlank() }?.take(100)
        return try {
            val uid = base.requireUserId()
            require(recordType in listOf("medical", "zakat_aid", "emergency_relief", "education", "burial")) { "Invalid record type" }
            require(cleanName.isNotEmpty()) { "Beneficiary name is required" }
            require(amount >= 0.0) { "Amount cannot be negative" }
            val docRef = db.collection("welfare_records").document()
            val payload = mapOf(
                "recordType" to recordType,
                "beneficiaryName" to cleanName,
                "phone" to cleanPhone,
                "plotNumber" to cleanPlot,
                "amount" to amount,
                "accountId" to cleanAccId,
                "accountName" to cleanAccName,
                "recordDate" to cleanDate,
                "note" to cleanNote,
                "createdBy" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            db.runTransaction { tx ->
                if (cleanAccId != null && amount > 0.0) {
                    val accRef = db.collection("bank_accounts").document(cleanAccId)
                    val prevBal = tx.get(accRef).getDouble("balance") ?: 0.0
                    tx.update(accRef, mapOf("balance" to (prevBal - amount), "updatedAt" to FieldValue.serverTimestamp()))
                }
                tx.set(docRef, payload)
            }.await()

            base.logAudit("CREATE_WELFARE", "welfare_records", docRef.id, "$recordType: $cleanName ($amount)")
            finance.refreshPublicSummary()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "welfare_records")
            Result.failure(e)
        }
    }

    suspend fun deleteWelfareRecord(recordId: String): Result<Unit> {
        return try {
            base.requireUserId()
            db.collection("welfare_records").document(recordId).delete().await()
            base.logAudit("DELETE_WELFARE", "welfare_records", recordId, "Deleted welfare record $recordId")
            finance.refreshPublicSummary()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "welfare_records/$recordId")
            Result.failure(e)
        }
    }

    suspend fun submitComplaint(
        authorName: String,
        familyId: String?,
        subject: String,
        message: String
    ): Result<Unit> {
        val cleanSubject = subject.trim().take(120)
        val cleanMsg = message.trim().take(1500)
        return try {
            val uid = base.requireUserId()
            require(cleanSubject.isNotEmpty()) { "Subject is required" }
            require(cleanMsg.isNotEmpty()) { "Message is required" }
            val docRef = db.collection("complaints").document()
            val payload = mapOf(
                "authorUid" to uid,
                "authorName" to authorName.trim().ifBlank { "Member" }.take(80),
                "familyId" to familyId?.takeIf { it.isNotBlank() },
                "subject" to cleanSubject,
                "message" to cleanMsg,
                "status" to "open",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "complaints")
            Result.failure(e)
        }
    }

    suspend fun replyComplaint(
        complaintId: String,
        status: String,
        adminReply: String?
    ): Result<Unit> {
        val cleanStatus = if (status in listOf("open", "reviewed", "resolved")) status else "reviewed"
        val cleanReply = adminReply?.trim()?.takeIf { it.isNotBlank() }?.take(1000)
        return try {
            base.requireUserId()
            db.collection("complaints").document(complaintId).update(
                mapOf(
                    "status" to cleanStatus,
                    "adminReply" to (cleanReply ?: FieldValue.delete()),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            base.logAudit("REPLY_COMPLAINT", "complaints", complaintId, "Status=$cleanStatus", afterSummary = cleanStatus)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "complaints/$complaintId")
            Result.failure(e)
        }
    }

    suspend fun deleteComplaint(complaintId: String): Result<Unit> {
        return try {
            base.requireUserId()
            db.collection("complaints").document(complaintId).delete().await()
            base.logAudit("DELETE_COMPLAINT", "complaints", complaintId, "Deleted complaint $complaintId")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "complaints/$complaintId")
            Result.failure(e)
        }
    }

    suspend fun seedSampleSocietyData(authorName: String): Result<Unit> {
        return try {
            val uid = base.requireUserId()
            val today = base.todayIso()
            val cal = Calendar.getInstance()
            val ymFormat = SimpleDateFormat("yyyy-MM", Locale.US)
            val thisMonth = ymFormat.format(cal.time)
            cal.add(Calendar.MONTH, -1)
            val prevMonth = ymFormat.format(cal.time)
            cal.add(Calendar.MONTH, -1)
            val twoMonthsAgo = ymFormat.format(cal.time)

            val batch = db.batch()
            val familiesData = listOf(
                Triple("fam_sample_01", "আব্দুর রহমান পরিবার", "FAM-001") to ("+8801711000011" to "মসজিদ রোড, বাড়ি ১২"),
                Triple("fam_sample_02", "কামাল উদ্দিন পরিবার", "FAM-002") to ("+8801711000022" to "পূর্ব পাড়া, লেন ৩"),
                Triple("fam_sample_03", "হাজী ইসমাইল পরিবার", "FAM-003") to ("+8801711000033" to "উত্তর মহল্লা, রোড ৫"),
                Triple("fam_sample_04", "মাহমুদুল হাসান পরিবার", "FAM-004") to ("+8801711000044" to "দক্ষিণ পাড়া, ব্লক বি")
            )
            familiesData.forEach { (triple, contact) ->
                batch.set(
                    db.collection("families").document(triple.first),
                    mapOf(
                        "familyName" to triple.second,
                        "phone" to contact.first,
                        "address" to contact.second,
                        "familyCode" to triple.third,
                        "active" to true,
                        "createdBy" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.set(
                db.collection("mosque_schedule").document("main"),
                mapOf(
                    "mosqueName" to "বায়তুল আমান জামে মসজিদ",
                    "fajrTime" to "05:15 AM",
                    "sunriseTime" to "06:22 AM",
                    "dhuhrTime" to "01:15 PM",
                    "asrTime" to "04:15 PM",
                    "maghribTime" to "05:30 PM",
                    "ishaTime" to "07:30 PM",
                    "jumuahTime" to "01:30 PM",
                    "khutbahTopic" to "যাকাত, সততা ও সামাজিক ভ্রাতৃত্ব",
                    "sehriEnd" to "04:55 AM",
                    "iftarTime" to "05:32 PM",
                    "imamName" to "মাওলানা ইউসুফ আলী",
                    "imamPhone" to "+8801711000101",
                    "muazzinName" to "হাফেজ বিল্লাল হোসেন",
                    "muazzinPhone" to "+8801711000102",
                    "updatedBy" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("bank_accounts").document("acc_cash_main"),
                mapOf(
                    "accountName" to "মসজিদ ক্যাশ তহবিল",
                    "accountType" to "cash",
                    "balance" to 18500.0,
                    "active" to true,
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("bank_accounts").document("acc_bkash_main"),
                mapOf(
                    "accountName" to "বিকাশ মসজিদ তহবিল",
                    "accountType" to "bkash",
                    "accountNumber" to "01711-000999",
                    "balance" to 6200.0,
                    "active" to true,
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()

            val batch2 = db.batch()
            val members = listOf(
                Triple("fam_sample_01", "আব্দুর রহমান পরিবার", "মোঃ আব্দুর রহমান") to Triple("প্রধান", "শিক্ষক", true),
                Triple("fam_sample_01", "আব্দুর রহমান পরিবার", "তানভীর রহমান") to Triple("পুত্র", "প্রকৌশলী", false),
                Triple("fam_sample_02", "কামাল উদ্দিন পরিবার", "মোঃ কামাল উদ্দিন") to Triple("প্রধান", "ব্যবসায়ী", true),
                Triple("fam_sample_03", "হাজী ইসমাইল পরিবার", "হাজী ইসমাইল হোসেন") to Triple("প্রধান", "সমাজসেবক", true),
                Triple("fam_sample_04", "মাহমুদুল হাসান পরিবার", "মাহমুদুল হাসান") to Triple("প্রধান", "চাকরিজীবী", true)
            )
            members.forEachIndexed { idx, (triple, meta) ->
                batch2.set(
                    db.collection("members").document("mem_sample_0${idx + 1}"),
                    mapOf(
                        "familyId" to triple.first,
                        "familyName" to triple.second,
                        "memberName" to triple.third,
                        "phone" to "+88017110000${idx + 1}0",
                        "relation" to meta.first,
                        "occupation" to meta.second,
                        "head" to meta.third,
                        "active" to true,
                        "createdBy" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch2.set(
                db.collection("dues").document("due_sample_01"),
                mapOf(
                    "title" to "মাসিক মসজিদ উন্নয়ন চাঁদা", "dueType" to "monthly", "amount" to 500.0,
                    "dueMonth" to 11L, "dueYear" to 2026L, "active" to true,
                    "createdBy" to uid, "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch2.set(
                db.collection("dues").document("due_sample_02"),
                mapOf(
                    "title" to "বার্ষিক সমাজ কল্যাণ তহবিল", "dueType" to "yearly", "amount" to 1000.0,
                    "dueMonth" to 0L, "dueYear" to 2026L, "active" to true,
                    "createdBy" to uid, "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            val samplePayments = listOf(
                listOf("pay_sample_01", "fam_sample_01", "আব্দুর রহমান পরিবার", "due_sample_01", "মাসিক মসজিদ উন্নয়ন চাঁদা", 1500.0, "cash", "RCP-202611-1001", today),
                listOf("pay_sample_02", "fam_sample_02", "কামাল উদ্দিন পরিবার", "due_sample_01", "মাসিক মসজিদ উন্নয়ন চাঁদা", 1000.0, "bkash", "RCP-202611-1002", "$prevMonth-14"),
                listOf("pay_sample_03", "fam_sample_03", "হাজী ইসমাইল পরিবার", "due_sample_02", "বার্ষিক সমাজ কল্যাণ তহবিল", 800.0, "nagad", "RCP-202611-1003", "$twoMonthsAgo-10")
            )
            samplePayments.forEach { p ->
                batch2.set(
                    db.collection("payments").document(p[0] as String),
                    mapOf(
                        "familyId" to p[1], "familyName" to p[2], "dueId" to p[3], "dueTitle" to p[4],
                        "amount" to p[5], "paymentMethod" to p[6], "receiptNo" to p[7], "paymentDate" to p[8],
                        "createdBy" to uid, "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch2.set(
                db.collection("donations").document("don_sample_01"),
                mapOf(
                    "donorName" to "Anonymous", "anonymous" to true, "fundType" to "zakat",
                    "amount" to 5000.0, "paymentMethod" to "bkash", "transactionRef" to "TRX882190",
                    "receiptNo" to "DON-202611-5001", "donationDate" to today,
                    "createdBy" to uid, "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            val sampleExpenses = listOf(
                listOf("exp_sample_01", "ইমাম ও মুয়াজ্জিন সম্মানী", "Imam & Staff", "VCH-202611-101", 1200.0, today),
                listOf("exp_sample_02", "বিদ্যুৎ ও পানি বিল", "Electricity & Utility", "VCH-202610-102", 650.0, "$prevMonth-20"),
                listOf("exp_sample_03", "মসজিদ পরিষ্কার ও মেরামত", "Maintenance", "VCH-202609-103", 400.0, "$twoMonthsAgo-18")
            )
            sampleExpenses.forEach { e ->
                batch2.set(
                    db.collection("expenses").document(e[0] as String),
                    mapOf(
                        "title" to e[1], "category" to e[2], "voucherNo" to e[3], "amount" to e[4], "expenseDate" to e[5],
                        "createdBy" to uid, "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch2.set(
                db.collection("budgets").document("bdg_sample_01"),
                mapOf(
                    "category" to "Imam & Staff", "periodLabel" to thisMonth, "budgetAmount" to 20000.0,
                    "createdBy" to uid, "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch2.set(
                db.collection("meetings").document("mtg_sample_01"),
                mapOf(
                    "title" to "মাসিক মসজিদ কমিটি ও উন্নয়ন সভা", "meetingDate" to today, "meetingTime" to "04:45 PM",
                    "location" to "মসজিদ কনফারেন্স কক্ষ",
                    "agenda" to "১. শীতবস্ত্র বিতরণ ২. অজুখানা সংস্কার ৩. বকেয়া চাঁদা পর্যালোচনা",
                    "attendees" to "সভাপতি, সেক্রেটারি, ইমাম সাহেব ও সদস্যবৃন্দ",
                    "decisions" to "শীতবস্ত্র বিতরণ ও অজুখানার পানির কল মেরামত অনুমোদিত।",
                    "assignedTo" to "কামাল উদ্দিন", "status" to "scheduled",
                    "createdBy" to uid, "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch2.set(
                db.collection("committees").document("com_sample_01"),
                mapOf(
                    "name" to "হাজী ইসমাইল হোসেন", "role" to "সভাপতি", "phone" to "+8801711000033",
                    "familyId" to "fam_sample_03", "familyName" to "হাজী ইসমাইল পরিবার", "term" to "২০২৫–২০২৭", "active" to true,
                    "createdBy" to uid, "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch2.set(
                db.collection("committees").document("com_sample_02"),
                mapOf(
                    "name" to "মোঃ আব্দুর রহমান", "role" to "সাধারণ সম্পাদক", "phone" to "+8801711000011",
                    "familyId" to "fam_sample_01", "familyName" to "আব্দুর রহমান পরিবার", "term" to "২০২৫–২০২৭", "active" to true,
                    "createdBy" to uid, "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch2.set(
                db.collection("events").document("evt_sample_01"),
                mapOf(
                    "title" to "মাসিক তাফসীর ও দোয়া মাহফিল", "eventDate" to today, "eventTime" to "07:45 PM",
                    "location" to "মসজিদ মূল হলরুম", "eventType" to "religious",
                    "description" to "এশার নামাজের পর সমাজের সকল মুসল্লির জন্য উন্মুক্ত তাফসীর ও দোয়া।",
                    "rsvpEnabled" to true,
                    "createdBy" to uid, "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch2.set(
                db.collection("assets").document("ast_sample_01"),
                mapOf(
                    "name" to "ডিজিটাল সাউন্ড সিস্টেম ও মাইক", "category" to "ইলেকট্রনিক্স", "value" to 45000.0,
                    "purchaseDate" to "$twoMonthsAgo-05", "condition" to "good", "location" to "মিম্বার কক্ষ",
                    "createdBy" to uid, "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch2.set(
                db.collection("documents").document("doc_sample_01"),
                mapOf(
                    "title" to "মসজিদ ওয়াকফ দলিল (কপি)", "category" to "আইনি ও দলিল", "docDate" to today,
                    "fileName" to "waqf_deed_copy.pdf", "note" to "মূল দলিল সেক্রেটারির সংরক্ষণে আছে",
                    "createdBy" to uid, "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch2.set(
                db.collection("notices").document("notice_sample_01"),
                mapOf(
                    "title" to "জুম্মার খুতবা ও মাসিক সভা",
                    "content" to "আগামী শুক্রবার দুপুর ১:১৫ মিনিটে জুম্মার খুতবা এবং আসরের পর মসজিদ কমিটির মাসিক সভা অনুষ্ঠিত হবে।",
                    "category" to "prayer", "pinned" to true, "authorUid" to uid,
                    "authorName" to authorName.ifBlank { "Admin" }.take(80),
                    "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch2.set(
                db.collection("qurbani_batches").document("qb_sample_01"),
                mapOf(
                    "eventName" to "ঈদুল আযহা কুরবানি বণ্টন",
                    "animalInfo" to "১টি গরু (সাদাকায় অংশ)",
                    "purchaseCost" to 95000.0,
                    "totalMeatKg" to 120.0, "totalFamilies" to 4L, "distributionPerFamily" to 30.0,
                    "eventDate" to "$thisMonth-01", "status" to "distributing",
                    "createdBy" to uid, "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            listOf(
                Triple("fam_sample_01", "আব্দুর রহমান পরিবার", "FAM-001") to true,
                Triple("fam_sample_02", "কামাল উদ্দিন পরিবার", "FAM-002") to true,
                Triple("fam_sample_03", "হাজী ইসমাইল পরিবার", "FAM-003") to false,
                Triple("fam_sample_04", "মাহমুদুল হাসান পরিবার", "FAM-004") to false
            ).forEach { (fam, delivered) ->
                val payload = mutableMapOf<String, Any>(
                    "batchId" to "qb_sample_01", "familyId" to fam.first, "familyName" to fam.second,
                    "familyCode" to fam.third, "allocatedKg" to 30.0,
                    "deliveredKg" to if (delivered) 30.0 else 0.0,
                    "status" to if (delivered) "delivered" else "pending",
                    "createdBy" to uid, "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
                )
                if (delivered) {
                    payload["receivedBy"] = fam.second.substringBefore(" ")
                    payload["receivedAt"] = FieldValue.serverTimestamp()
                }
                batch2.set(db.collection("meat_distributions").document("qb_sample_01_${fam.first}"), payload)
            }
            batch2.commit().await()

            base.logAudit("SEED_SAMPLE_DATA", "public_summary", "overview", "Loaded initial sample society records")
            finance.refreshPublicSummary()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "seedSampleSocietyData")
            Result.failure(e)
        }
    }

    suspend fun exportBackupJson(): Result<String> {
        return try {
            base.requireUserId()
            val root = JSONObject()
            root.put("exportedAt", base.todayIso())
            root.put("version", 3)

            suspend fun dumpCollection(name: String): JSONArray {
                val arr = JSONArray()
                val snap = db.collection(name).get().await()
                snap.documents.forEach { doc ->
                    val obj = JSONObject()
                    obj.put("id", doc.id)
                    doc.data?.forEach { (k, v) ->
                        if (k != "createdAt" && k != "updatedAt" && k != "receivedAt") {
                            obj.put(k, v)
                        }
                    }
                    arr.put(obj)
                }
                return arr
            }

            listOf(
                "mosque_schedule", "families", "members", "dues", "bank_accounts",
                "budgets", "payments", "donations", "expenses", "qurbani_batches",
                "meat_distributions", "notices", "committees", "meetings", "events",
                "assets", "documents", "welfare_records"
            ).forEach { col -> root.put(col, dumpCollection(col)) }

            base.logAudit("EXPORT_BACKUP", "system", "json", "Exported full JSON backup")
            Result.success(root.toString(2))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "backup/export")
            Result.failure(e)
        }
    }

    suspend fun restoreBackupJson(jsonString: String): Result<Unit> {
        return try {
            val uid = base.requireUserId()
            val root = JSONObject(jsonString)

            suspend fun restoreSimple(collection: String, allowedFields: List<String>, defaults: Map<String, Any> = emptyMap()) {
                val arr = root.optJSONArray(collection) ?: return
                val batch = db.batch()
                var count = 0
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val rawId = obj.optString("id").trim()
                    val docRef = if (rawId.matches(Regex("^[a-zA-Z0-9_\\-]{1,128}$"))) {
                        db.collection(collection).document(rawId)
                    } else {
                        db.collection(collection).document()
                    }
                    val payload = mutableMapOf<String, Any>()
                    allowedFields.forEach { key ->
                        if (obj.has(key) && !obj.isNull(key)) payload[key] = obj.get(key)
                    }
                    defaults.forEach { (k, v) -> payload.putIfAbsent(k, v) }
                    when (collection) {
                        "notices" -> {
                            payload["authorUid"] = uid
                            payload.putIfAbsent("authorName", "Admin")
                            payload["createdAt"] = FieldValue.serverTimestamp()
                        }
                        "mosque_schedule" -> payload["updatedBy"] = uid
                        else -> {
                            payload["createdBy"] = uid
                            payload["createdAt"] = FieldValue.serverTimestamp()
                        }
                    }
                    payload["updatedAt"] = FieldValue.serverTimestamp()
                    batch.set(docRef, payload)
                    count++
                    if (count % 400 == 0) batch.commit().await()
                }
                if (count > 0) batch.commit().await()
            }

            restoreSimple("mosque_schedule", listOf("mosqueName", "fajrTime", "sunriseTime", "dhuhrTime", "asrTime", "maghribTime", "ishaTime", "jumuahTime", "khutbahTopic", "sehriEnd", "iftarTime", "imamName", "imamPhone", "muazzinName", "muazzinPhone"))
            restoreSimple("families", listOf("familyName", "phone", "address", "familyCode", "linkedEmail", "active"), mapOf("active" to true))
            restoreSimple("dues", listOf("title", "dueType", "amount", "dueMonth", "dueYear", "active"), mapOf("dueType" to "monthly", "dueMonth" to 0L, "dueYear" to 2026L, "active" to true))
            restoreSimple("bank_accounts", listOf("accountName", "accountType", "accountNumber", "balance", "active"), mapOf("accountType" to "cash", "balance" to 0.0, "active" to true))
            restoreSimple("budgets", listOf("category", "periodLabel", "budgetAmount"))
            restoreSimple("qurbani_batches", listOf("eventName", "animalInfo", "purchaseCost", "totalMeatKg", "totalFamilies", "distributionPerFamily", "eventDate", "status"), mapOf("status" to "distributing"))
            restoreSimple("members", listOf("familyId", "familyName", "memberName", "phone", "relation", "occupation", "head", "active"), mapOf("head" to false, "active" to true))
            restoreSimple("payments", listOf("familyId", "familyName", "dueId", "dueTitle", "accountId", "accountName", "amount", "paymentMethod", "receiptNo", "paymentDate", "note"), mapOf("paymentMethod" to "cash"))
            restoreSimple("donations", listOf("donorName", "anonymous", "fundType", "amount", "paymentMethod", "transactionRef", "accountId", "accountName", "receiptNo", "donationDate", "note"), mapOf("anonymous" to false, "fundType" to "general", "paymentMethod" to "cash"))
            restoreSimple("expenses", listOf("title", "category", "voucherNo", "accountId", "accountName", "amount", "expenseDate", "note"), mapOf("category" to "Other"))
            restoreSimple("meat_distributions", listOf("batchId", "familyId", "familyName", "familyCode", "allocatedKg", "deliveredKg", "receivedBy", "status"), mapOf("deliveredKg" to 0.0, "status" to "pending"))
            restoreSimple("notices", listOf("title", "content", "category", "pinned", "authorName"), mapOf("category" to "general", "pinned" to false))
            restoreSimple("committees", listOf("name", "role", "phone", "familyId", "familyName", "term", "active"), mapOf("active" to true))
            restoreSimple("meetings", listOf("title", "meetingDate", "meetingTime", "location", "agenda", "attendees", "decisions", "assignedTo", "deadline", "status"), mapOf("status" to "scheduled"))
            restoreSimple("events", listOf("title", "eventDate", "eventTime", "location", "eventType", "description", "rsvpEnabled"), mapOf("eventType" to "religious", "rsvpEnabled" to false))
            restoreSimple("assets", listOf("name", "category", "value", "purchaseDate", "condition", "location", "note"), mapOf("value" to 0.0, "condition" to "good"))
            restoreSimple("documents", listOf("title", "category", "docDate", "fileName", "note"))
            restoreSimple("welfare_records", listOf("recordType", "beneficiaryName", "phone", "plotNumber", "amount", "accountId", "accountName", "recordDate", "note"), mapOf("recordType" to "medical", "amount" to 0.0))

            base.logAudit("RESTORE_BACKUP", "system", "json", "Restored society records from JSON backup")
            finance.refreshPublicSummary()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "backup/restore")
            Result.failure(e)
        }
    }
}
