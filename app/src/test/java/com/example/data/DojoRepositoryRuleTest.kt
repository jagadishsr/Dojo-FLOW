package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.example.model.Dojo
import com.example.model.Organisation
import com.example.model.Student
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class DojoRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveOrganisation_authenticatedOwner_createsDocument() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = DojoRepository(firestore)

        val org = Organisation(
            ownerUid = aliceUid,
            name = "Shotokan India Federation",
            shortName = "SIF",
            martialArtStyle = "Shotokan Karate",
            country = "India"
        )

        val orgId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveOrganisation(org)
        }
        assertTrue(orgId.isNotEmpty())
    }

    @Test
    fun observeDojos_authenticatedOwner_emitsCreatedDojo() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = DojoRepository(firestore)

        val uniqueDojoName = "Chennai Central ${UUID.randomUUID().toString().take(6)}"
        val dojo = Dojo(
            orgId = "org_test",
            ownerUid = aliceUid,
            name = uniqueDojoName,
            status = "active"
        )

        val dojoId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveDojo(dojo)
        }
        assertTrue(dojoId.isNotEmpty())

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeDojos().first { list -> list.any { it.name == uniqueDojoName } }
        }
        assertTrue(emitted.any { it.name == uniqueDojoName })
    }

    @Test
    fun observeStudents_unauthenticated_failsWithPermissionDenied() = runBlocking {
        // Sign out to test unauthenticated access
        auth.signOut()
        val repository = DojoRepository(firestore)

        var caughtException: Exception? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeStudents().first()
            }
        } catch (e: Exception) {
            caughtException = e
        }
        assertNotNull(caughtException)
        val firestoreException = caughtException!!.unwrapFirestoreException()
        if (firestoreException != null) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
        } else {
            // IllegalStateException from requireUserId() before Firestore call
            assertTrue(caughtException is IllegalStateException)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "sensei_alice@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
