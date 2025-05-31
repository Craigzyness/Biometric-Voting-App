// backend/server.js
const express = require('express');
const app = express();
const PORT = process.env.PORT || 3000; // Port for the server to listen on

// Middleware to parse JSON bodies
app.use(express.json());

// Basic route for testing if the server is up
app.get('/', (req, res) => {
    res.send('Biometric Voting App Backend (MVP Placeholder) is running!');
});

// --- In-Memory Data Stores (Placeholders for MVP) ---
let registeredVoters = []; // Stores objects like { anonymizedVoterId: "id_string", registrationDate: new Date() }
let elections = [
    {
        id: "election_1",
        title: "Student Council President 2024",
        description: "Vote for the next Student Council President. Make your voice heard!",
        options: ["Alice Wonderland", "Bob The Builder", "Charlie Brown"],
        startDate: "2024-08-01T00:00:00Z", // Example ISO date string
        endDate: "2024-08-15T23:59:59Z"   // Example ISO date string
    },
    {
        id: "election_2",
        title: "Referendum: New Cafeteria Menu",
        description: "Should the cafeteria menu be updated with more healthy options?",
        options: ["Yes, update the menu", "No, keep the current menu"],
        startDate: "2024-08-05T00:00:00Z",
        endDate: "2024-08-10T23:59:59Z"
    },
    {
        id: "election_3",
        title: "Mascot Naming Contest",
        description: "Choose the new official mascot name for our institution.",
        options: ["Sparky the Dragon", "Captain Comet", "Wally the Wombat"],
        startDate: "2024-07-20T00:00:00Z",
        endDate: "2024-08-05T23:59:59Z"
    }
];
// Stores objects like { voteId: "uuid", anonymizedVoterId: "id_string", electionId: "election_id_string", selectedOption: "option_string", timestamp: new Date() }
let submittedVotes = [];
// --- End of In-Memory Data Stores ---

// --- API Routes ---

// POST /register
// Registers a new voter using their anonymized ID.
app.post('/register', (req, res) => {
    const { anonymizedVoterId } = req.body;

    if (!anonymizedVoterId || typeof anonymizedVoterId !== 'string' || anonymizedVoterId.trim() === '') {
        return res.status(400).json({ message: "Invalid or missing anonymizedVoterId." });
    }

    // Check if voter already exists
    const existingVoter = registeredVoters.find(voter => voter.anonymizedVoterId === anonymizedVoterId.trim());
    if (existingVoter) {
        return res.status(409).json({ message: "This anonymizedVoterId is already registered." }); // 409 Conflict
    }

    const newVoter = {
        anonymizedVoterId: anonymizedVoterId.trim(),
        registrationDate: new Date().toISOString()
    };
    registeredVoters.push(newVoter);

    console.log(`New voter registered: ${newVoter.anonymizedVoterId}`);
    res.status(201).json({ message: "Voter registered successfully.", voter: newVoter });
});

// GET /elections
// Retrieves the list of available elections.
app.get('/elections', (req, res) => {
    // For MVP, we simply return the hardcoded list.
    // In a real app, this would fetch from a database and could include filtering, pagination, etc.
    if (elections && elections.length > 0) {
        res.status(200).json(elections);
    } else {
        // This case might occur if the elections array was somehow cleared or not loaded.
        res.status(404).json({ message: "No elections found at this time." });
    }
});

// POST /submitVote
// Submits a vote for a given election.
app.post('/submitVote', (req, res) => {
    const { anonymizedVoterId, electionId, selectedOption } = req.body;

    // 1. Validate input
    if (!anonymizedVoterId || !electionId || !selectedOption) {
        return res.status(400).json({ message: "Missing required fields: anonymizedVoterId, electionId, selectedOption." });
    }
    if (typeof anonymizedVoterId !== 'string' || typeof electionId !== 'string' || typeof selectedOption !== 'string') {
        return res.status(400).json({ message: "Invalid data types for fields." });
    }

    // 2. Check if voter is registered
    const voter = registeredVoters.find(v => v.anonymizedVoterId === anonymizedVoterId);
    if (!voter) {
        return res.status(403).json({ message: "Voter not registered." }); // 403 Forbidden
    }

    // 3. Check if election is valid
    const election = elections.find(e => e.id === electionId);
    if (!election) {
        return res.status(404).json({ message: "Election not found." });
    }

    // 4. Check if the selected option is valid for the given election
    if (!election.options.includes(selectedOption)) {
        return res.status(400).json({ message: "Invalid option selected for this election." });
    }

    // 5. Prevent Double Voting
    const existingVote = submittedVotes.find(
        vote => vote.anonymizedVoterId === anonymizedVoterId && vote.electionId === electionId
    );
    if (existingVote) {
        return res.status(409).json({ message: "Already voted in this election." }); // 409 Conflict
    }

    // 6. If all checks pass, record the vote (in-memory for MVP)
    const newVote = {
        voteId: `vote_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`, // Simple unique ID
        anonymizedVoterId,
        electionId,
        selectedOption, // In a real system, consider hashing or encrypting this if vote content privacy is paramount on the server before blockchain
        timestamp: new Date().toISOString()
    };
    submittedVotes.push(newVote);

    console.log(`Vote submitted: Voter ${anonymizedVoterId}, Election ${electionId}, Option ${selectedOption}`);
    // This is where Action Item 3.7 (Simulate Blockchain Recording) will enhance logic.
    // For now, just confirming it's added to our in-memory log.
    res.status(201).json({ message: "Vote submitted successfully.", vote: newVote });
});

// --- End of API Routes ---

app.listen(PORT, () => {
    console.log(`Backend server (MVP Placeholder) listening on port ${PORT}`);
    console.log('This is a placeholder backend for the Biometric Voting App MVP.');
    console.log('It uses in-memory data stores and does not persist data.');
    console.log('Endpoints for /register, /elections, and /submitVote will be added.');
});

module.exports = app; // For potential testing or modularization later
