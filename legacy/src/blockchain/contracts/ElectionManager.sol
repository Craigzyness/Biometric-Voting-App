// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "hardhat/console.sol"; // For debugging, can be removed for production

contract ElectionManager {
    address public owner;

    struct Election {
        uint256 id;
        string title;
        string description;
        string[] options; // Array of option names
        bool isActive;
    }

    // --- State Variables ---
    mapping(uint256 => Election) public elections;
    uint256[] public electionIds;
    uint256 private nextElectionId = 1;

    // Stores vote counts for each election's options
    // mapping: electionId -> optionIndex -> count
    mapping(uint256 => mapping(uint256 => uint256)) public voteCounts;

    // Tracks if an address has voted in a specific election
    // mapping: electionId -> voterAddress -> hasVoted (boolean)
    mapping(uint256 => mapping(address => bool)) public voterHasVoted;

    // --- Events ---
    event ElectionCreated(
        uint256 indexed id,
        string title,
        uint256 numOptions,
        address indexed createdBy
    );

    event VoteCast(
        uint256 indexed electionId,
        address indexed voter,
        uint256 indexed optionIndex,
        string optionText
    );

    // --- Modifiers ---
    modifier onlyOwner() {
        require(msg.sender == owner, "Only owner can call this function");
        _;
    }

    // --- Constructor ---
    constructor() {
        owner = msg.sender;
    }

    // --- Election Management Functions ---
    function createElection(
        string memory _title,
        string memory _description,
        string[] memory _options,
        bool _isActive
    ) public onlyOwner {
        require(bytes(_title).length > 0, "Title cannot be empty");
        require(_options.length >= 2, "Must have at least two options");

        uint256 currentId = nextElectionId;
        elections[currentId] = Election({
            id: currentId,
            title: _title,
            description: _description,
            options: _options,
            isActive: _isActive
        });
        electionIds.push(currentId);

        // Initialize vote counts for this new election's options to 0
        for (uint256 i = 0; i < _options.length; i++) {
            voteCounts[currentId][i] = 0;
        }

        emit ElectionCreated(currentId, _title, _options.length, msg.sender);
        nextElectionId++;
    }

    function getElectionDetails(uint256 _electionId)
        public view
        returns (uint256 id, string memory title, string memory description, string[] memory options_, bool isActive_)
    {
        Election storage election = elections[_electionId];
        require(election.id != 0, "Election ID is not valid");
        return (election.id, election.title, election.description, election.options, election.isActive);
    }

    function toggleElectionStatus(uint256 _electionId) public onlyOwner {
        Election storage election = elections[_electionId];
        require(election.id != 0, "Election ID is not valid");
        election.isActive = !election.isActive;
    }

    function getElectionsCount() public view returns (uint256) {
        return electionIds.length;
    }

    // --- Vote Casting Function ---
    /**
     * @dev Casts a vote for a specific option in an election.
     * @param _electionId The ID of the election to vote in.
     * @param _optionIndex The index of the selected option (0-based).
     */
    function castVote(uint256 _electionId, uint256 _optionIndex) public {
        // Retrieve the election; this also checks if electionId is valid via the struct's default id (0)
        Election storage election = elections[_electionId];
        require(election.id != 0, "Election does not exist.");
        require(election.isActive, "This election is not currently active.");
        require(_optionIndex < election.options.length, "Invalid option index.");
        require(!voterHasVoted[_electionId][msg.sender], "You have already voted in this election.");

        // Record the vote
        voterHasVoted[_electionId][msg.sender] = true;
        voteCounts[_electionId][_optionIndex]++;

        emit VoteCast(_electionId, msg.sender, _optionIndex, election.options[_optionIndex]);
        console.log("Vote cast by %s in election %s for option %s ('%s')", msg.sender, _electionId, _optionIndex, election.options[_optionIndex]);
    }

    // --- Vote Count Retrieval Function ---
    /**
     * @dev Retrieves the vote count for a specific option in an election.
     * @param _electionId The ID of the election.
     * @param _optionIndex The index of the option.
     * @return The number of votes for that option.
     */
    function getVoteCount(uint256 _electionId, uint256 _optionIndex) public view returns (uint256) {
        // Check if election exists and option index is valid before returning
        Election storage election = elections[_electionId];
        require(election.id != 0, "Election does not exist.");
        require(_optionIndex < election.options.length, "Invalid option index.");
        return voteCounts[_electionId][_optionIndex];
    }

    /**
     * @dev Retrieves all vote counts for a given election.
     * @param _electionId The ID of the election.
     * @return An array of vote counts, corresponding to the order of options.
     */
    function getAllVoteCountsForElection(uint256 _electionId) public view returns (uint256[] memory) {
        Election storage election = elections[_electionId];
        require(election.id != 0, "Election does not exist.");

        uint256[] memory counts = new uint256[](election.options.length);
        for (uint256 i = 0; i < election.options.length; i++) {
            counts[i] = voteCounts[_electionId][i];
        }
        return counts;
    }
}
