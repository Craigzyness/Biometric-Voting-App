package com.example.biometricvotingapp.domain.model

import java.util.UUID // For generating unique default IDs if needed

/**
 * Election.kt
 *
 * Purpose: Represents an election with its details and voting options.
 */
data class Election(
    val id: String = UUID.randomUUID().toString(), // Default to a random UUID for simplicity
    val title: String,
    val description: String,
    val options: List<String>,
    val hasVoted: Boolean = false // Default to false
)

/**
 * Example Usage (for documentation or testing):
 *
 * val sampleElection1 = Election(
 *     title = "Presidential Election 2024",
 *     description = "Choose the next president.",
 *     options = listOf("Candidate A", "Candidate B", "Candidate C")
 * )
 *
 * val sampleElection2 = Election(
 *     id = "prop-101",
 *     title = "Proposition 101: Park Funding Initiative",
 *     description = "Vote on the proposal to increase funding for city parks.",
 *     options = listOf("Yes on Prop 101", "No on Prop 101")
 * )
 */
