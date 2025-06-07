package com.example.biometricvotingapp.domain.usecase

import com.example.biometricvotingapp.domain.security.AnonymizedIdGenerator // Corrected path
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every // Use 'every' for non-suspend functions
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

// Define UserNotRegisteredException here as it's closely tied to this use case's logic
// If used elsewhere, it could be moved to a common domain exceptions file.
class UserNotRegisteredException(message: String) : Exception(message)

@ExperimentalCoroutinesApi
class LoginUserUseCaseTest {

    private lateinit var loginUserUseCase: LoginUserUseCase
    // AnonymizedIdGenerator is an object, but we pass its instance to the use case.
    // So, we mock the instance that would be passed.
    private val mockAnonymizedIdGenerator: AnonymizedIdGenerator = mockk()
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        // If AnonymizedIdGenerator methods were static-like calls on the object itself within the use case
        // (which they are not, as an instance is injected), then mockkObject(AnonymizedIdGenerator) would be needed.
        // Since an instance is injected, standard mocking on that instance is sufficient.
        loginUserUseCase = LoginUserUseCase(mockAnonymizedIdGenerator, testDispatcher)
    }

    // No @After needed for unmockkObject if not used. clearAllMocks() could be used if desired.

    @Test
    fun `invoke success - ID found`() = runTest(testDispatcher) {
        val fakeId = "registeredUserId123"
        // Mock the parameterless getRegisteredAnonymizedId() that the UseCase expects
        // This relies on the placeholder extension function for testing, as discussed.
        every { mockAnonymizedIdGenerator.getRegisteredAnonymizedId() } returns fakeId

        val result = loginUserUseCase()

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(fakeId)
        verify { mockAnonymizedIdGenerator.getRegisteredAnonymizedId() }
    }

    @Test
    fun `invoke failure - ID not found (null from generator)`() = runTest(testDispatcher) {
        every { mockAnonymizedIdGenerator.getRegisteredAnonymizedId() } returns null

        val result = loginUserUseCase()

        assertThat(result.isFailure).isTrue()
        val exception = result.exceptionOrNull()
        assertThat(exception).isInstanceOf(UserNotRegisteredException::class.java)
        assertThat(exception?.message).isEqualTo("User is not registered or ID not found.")
        verify { mockAnonymizedIdGenerator.getRegisteredAnonymizedId() }
    }

    @Test
    fun `invoke failure - generator throws exception`() = runTest(testDispatcher) {
        val exception = RuntimeException("Error accessing secure storage")
        every { mockAnonymizedIdGenerator.getRegisteredAnonymizedId() } throws exception

        val result = loginUserUseCase()

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isEqualTo(exception)
        verify { mockAnonymizedIdGenerator.getRegisteredAnonymizedId() }
    }
}

// Placeholder extension for AnonymizedIdGenerator.getRegisteredAnonymizedId() for test compilation,
// matching the one in LoginUserUseCase.kt.
// This is needed because the actual AnonymizedIdGenerator.getRegisteredAnonymizedId(context) takes context.
@Deprecated("Test-only placeholder. Do not use in production.")
fun com.example.biometricvotingapp.domain.security.AnonymizedIdGenerator.getRegisteredAnonymizedId(): String? {
    // This is a stub for mocking behavior. It will be controlled by `every { ... } returns ...` in tests.
    return "default-mock-id-from-test-extension-for-login-uc-test"
}
