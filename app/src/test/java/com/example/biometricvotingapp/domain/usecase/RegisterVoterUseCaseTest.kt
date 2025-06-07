package com.example.biometricvotingapp.domain.usecase

import com.example.biometricvotingapp.data.network.dto.RegistrationRequest // Assuming this DTO path
import com.example.biometricvotingapp.domain.repository.AuthRepository // Assuming this interface path
import com.example.biometricvotingapp.domain.security.AnonymizedIdGenerator // Corrected path
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class RegisterVoterUseCaseTest {

    private lateinit var registerVoterUseCase: RegisterVoterUseCase
    private val mockAnonymizedIdGenerator: AnonymizedIdGenerator = mockk()
    private val mockAuthRepository: AuthRepository = mockk()
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        // Mock the AnonymizedIdGenerator object as it's used by the use case
        // The actual AnonymizedIdGenerator.generate() takes (Context, AuthResult)
        // The UseCase prompt implies a parameterless version. We mock this parameterless version.
        // This requires either AnonymizedIdGenerator to have such a method, or for us to mock the object
        // and define this behavior.
        mockkObject(AnonymizedIdGenerator) // Mock the object itself to control its static-like calls if needed, or its instance methods if passed.
                                         // In our UseCase, an instance is passed.

        registerVoterUseCase = RegisterVoterUseCase(mockAnonymizedIdGenerator, mockAuthRepository, testDispatcher)
    }

    @After
    fun tearDown() {
        unmockkObject(AnonymizedIdGenerator) // Important if mockkObject was used on the actual object
    }

    @Test
    fun `invoke success - generates ID and registers voter successfully`() = runTest(testDispatcher) {
        val fakeId = "testAnonymizedId123"
        // Mock the parameterless generate() that the UseCase expects (as per prompt for UseCase structure)
        every { mockAnonymizedIdGenerator.generate() } returns fakeId
        coEvery { mockAuthRepository.registerVoter(RegistrationRequest(fakeId)) } returns Result.success(Unit) // Assuming Result<Unit> for success

        val result = registerVoterUseCase()

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(fakeId)
        coVerify { mockAnonymizedIdGenerator.generate() }
        coVerify { mockAuthRepository.registerVoter(RegistrationRequest(fakeId)) }
    }

    @Test
    fun `invoke failure - ID generation returns null`() = runTest(testDispatcher) {
        every { mockAnonymizedIdGenerator.generate() } returns null

        val result = registerVoterUseCase()

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo("Failed to generate anonymized ID.")
        coVerify { mockAnonymizedIdGenerator.generate() }
        coVerify(exactly = 0) { mockAuthRepository.registerVoter(any()) }
    }

    @Test
    fun `invoke failure - repository registration fails`() = runTest(testDispatcher) {
        val fakeId = "testAnonymizedId123"
        val exception = Exception("Network error")
        every { mockAnonymizedIdGenerator.generate() } returns fakeId
        coEvery { mockAuthRepository.registerVoter(RegistrationRequest(fakeId)) } returns Result.failure(exception)

        val result = registerVoterUseCase()

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isEqualTo(exception)
    }

    @Test
    fun `invoke failure - repository throws exception`() = runTest(testDispatcher) {
        val fakeId = "testAnonymizedId123"
        val exception = RuntimeException("DB down")
        every { mockAnonymizedIdGenerator.generate() } returns fakeId
        // Use coAnswers to throw an exception from a suspend function
        coEvery { mockAuthRepository.registerVoter(RegistrationRequest(fakeId)) } coAnswers { throw exception }


        val result = registerVoterUseCase()

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isEqualTo(exception)
    }
}

// Parameterless generate() extension for AnonymizedIdGenerator mock as assumed by UseCase and its test
// This is only for the test environment to align with the provided UseCase structure.
// In real code, the AnonymizedIdGenerator or UseCase signature would be harmonized.
@Deprecated("Test-only placeholder. Do not use in production.")
fun com.example.biometricvotingapp.domain.security.AnonymizedIdGenerator.generate(): String? {
    // This is a stub for mocking behavior. It will be controlled by `every { ... } returns ...` in tests.
    // If not mocked, it would return "default-mock-id" or throw if not relaxed.
    return "default-mock-id-from-test-extension"
}
