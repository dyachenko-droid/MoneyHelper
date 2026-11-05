package ru.denpom.finance.app.data.launch

import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import ru.denpom.finance.app.domain.launch.PrivacyLinkRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebasePrivacyRepository @Inject constructor() : PrivacyLinkRepository {
    override suspend fun read(field: String): String? {
        if (FirebaseApp.getInstance().options.projectId == IntegrationConfig.PLACEHOLDER_FIREBASE_PROJECT) return null
        return suspendCancellableCoroutine { continuation ->
            FirebaseDatabase.getInstance().reference.child(field).get()
                .addOnSuccessListener { snapshot ->
                    if (continuation.isActive) continuation.resume(snapshot.value as? String)
                }
                .addOnFailureListener { error ->
                    if (continuation.isActive) continuation.resumeWithException(error)
                }
        }
    }
}
