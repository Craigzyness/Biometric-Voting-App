package com.example.biometricvotingapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.biometricvotingapp.domain.model.Election // Import the Election data class
import com.example.biometricvotingapp.ui.screens.electionlist.ElectionListUiState
import com.example.biometricvotingapp.ui.screens.electionlist.ElectionListViewModel
// import androidx.compose.ui.tooling.preview.Preview // Uncomment for preview

/**
 * ElectionListScreen.kt
 *
 * Purpose: Displays a list of available elections to the user.
 * For MVP, this list is hardcoded.
 */

// For MVP, this list is fetched from the backend via VotingRepository.
// TODO: In a full app, this would ideally involve a ViewModel.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElectionListScreen(
    onElectionClicked: (Election) -> Unit, // Callback when an election is clicked
    // TODO: Add callbacks for other actions like logout, refresh, etc.
    // onLogoutClicked: () -> Unit
    viewModel: ElectionListViewModel = viewModel() // Obtain ViewModel instance
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Available Elections") })
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is ElectionListUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is ElectionListUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                }
            }
            is ElectionListUiState.Empty -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No elections available at the moment.")
                }
            }
            is ElectionListUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(top = 8.dp, bottom = 8.dp)
                ) {
                    items(state.elections, key = { election -> election.id }) { domainElection ->
                        ElectionListItem(
                            election = domainElection, // Already a domain model from VM
                            onClicked = { onElectionClicked(domainElection) }
                        )
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
fun ElectionListItem(
    election: Election,
    onClicked: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClicked)
            .padding(horizontal = 16.dp, vertical = 8.dp), // Padding for each card
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp) // Padding inside the card
        ) {
            Text(
                text = election.title,
                style = MaterialTheme.typography.titleMedium
            )
            // Optionally display description if it's short and relevant for the list view
            if (election.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = election.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2, // Limit description lines
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            // We don't display options here, that's for the detail/voting screen.
        }
    }
}

// Sample Data for Preview and MVP (will be moved or replaced by ViewModel logic)
fun getSampleElections(): List<Election> {
    return listOf(
        Election(
            id = "election_1",
            title = "Student Council President 2024",
            description = "Vote for the next Student Council President. Make your voice heard!",
            options = listOf("Alice Wonderland", "Bob The Builder", "Charlie Brown")
        ),
        Election(
            id = "election_2",
            title = "Referendum: New Cafeteria Menu",
            description = "Should the cafeteria menu be updated with more healthy options?",
            options = listOf("Yes, update the menu", "No, keep the current menu")
        ),
        Election(
            id = "election_3",
            title = "Mascot Naming Contest",
            description = "Choose the new official mascot name for our institution.",
            options = listOf("Sparky the Dragon", "Captain Comet", "Wally the Wombat")
        )
    )
}

// TODO: Implement Preview function if needed, after setting up theme.
// @Preview(showBackground = true)
// @Composable
// fun PreviewElectionListScreen() {
//     // YourAppTheme { // Replace with your app's theme
//         ElectionListScreen(
//             elections = getSampleElections(),
//             onElectionClicked = { election -> println("Clicked on: ${election.title}") }
//         )
//     // }
// }

// @Preview(showBackground = true)
// @Composable
// fun PreviewElectionListItem() {
//    // YourAppTheme {
//         ElectionListItem(
//             election = Election(title = "Sample Election Title", description = "This is a sample description for the election item.", options = listOf()),
//             onClicked = {}
//         )
//    // }
// }
