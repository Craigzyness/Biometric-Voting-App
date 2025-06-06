package com.example.biometricvotingapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.biometricvotingapp.domain.model.Election // Import the Election data class
// import androidx.compose.ui.tooling.preview.Preview // Uncomment for preview

/**
 * ElectionListScreen.kt
 *
 * Purpose: Displays a list of available elections to the user.
 * For MVP, this list is hardcoded.
 */

// TODO: In a real app, this list would come from a ViewModel, which fetches it from a repository/backend.
// For MVP, we'll define sample data directly here or pass it as a parameter.

@Composable
fun ElectionListScreen(
    elections: List<Election>, // The list of elections to display
    onElectionClicked: (Election) -> Unit, // Callback when an election is clicked
    // TODO: Add callbacks for other actions like logout, refresh, etc.
    // onLogoutClicked: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Available Elections") })
        }
    ) { paddingValues ->
        if (elections.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text("No elections available at the moment.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(top = 8.dp, bottom = 8.dp) // Add some padding around the list
            ) {
                items(elections, key = { election -> election.id }) { election ->
                    ElectionListItem(
                        election = election,
                        onClicked = { onElectionClicked(election) }
                    )
                    Divider() // Adds a line between items
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
