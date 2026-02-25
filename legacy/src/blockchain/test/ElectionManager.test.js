const { expect } = require("chai");
const { ethers } = require("hardhat");

describe("ElectionManager", function () {
    let ElectionManager;
    let electionManager;
    let owner;
    let addr1;
    let addr2;
    let addrs;

    beforeEach(async function () {
        // Get the ContractFactory and Signers here.
        ElectionManager = await ethers.getContractFactory("ElectionManager");
        [owner, addr1, addr2, ...addrs] = await ethers.getSigners();

        // Deploy a new ElectionManager contract before each test.
        electionManager = await ElectionManager.deploy();
        await electionManager.waitForDeployment();
    });

    describe("Deployment", function () {
        it("Should set the right owner", async function () {
            expect(await electionManager.owner()).to.equal(owner.address);
        });
    });

    describe("Election Creation", function () {
        it("Should allow owner to create an election", async function () {
            const electionTitle = "Test Election 1";
            const electionDesc = "Description for Test Election 1";
            const options = ["Option A", "Option B", "Option C"];

            await expect(electionManager.connect(owner).createElection(electionTitle, electionDesc, options, true))
                .to.emit(electionManager, "ElectionCreated")
                .withArgs(1, electionTitle, options.length, owner.address); // electionId is 1 (nextElectionId)

            const electionDetails = await electionManager.getElectionDetails(1);
            expect(electionDetails.id).to.equal(1);
            expect(electionDetails.title).to.equal(electionTitle);
            expect(electionDetails.options.length).to.equal(options.length);
            expect(electionDetails.isActive).to.be.true;
        });

        it("Should not allow non-owner to create an election", async function () {
            await expect(
                electionManager.connect(addr1).createElection("Non-owner Election", "Desc", ["Opt1", "Opt2"], true)
            ).to.be.revertedWith("Only owner can call this function");
        });

        it("Should require at least two options", async function () {
            await expect(
                electionManager.connect(owner).createElection("Single Option Election", "Desc", ["Opt1"], true)
            ).to.be.revertedWith("Must have at least two options");
        });

        it("Should increment election IDs", async function () {
            await electionManager.connect(owner).createElection("Election One", "D1", ["O1A", "O1B"], true);
            await electionManager.connect(owner).createElection("Election Two", "D2", ["O2A", "O2B"], true);

            const election2Details = await electionManager.getElectionDetails(2);
            expect(election2Details.title).to.equal("Election Two");
            expect(await electionManager.getElectionsCount()).to.equal(2);
        });
    });

    describe("Vote Casting", function () {
        beforeEach(async function () {
            // Create a sample election for vote casting tests
            await electionManager.connect(owner).createElection("Voting Test Election", "Desc", ["Yes", "No"], true);
        });

        it("Should allow a user to cast a vote in an active election", async function () {
            const electionId = 1;
            const optionIndex = 0; // Vote for "Yes"

            await expect(electionManager.connect(addr1).castVote(electionId, optionIndex))
                .to.emit(electionManager, "VoteCast")
                .withArgs(electionId, addr1.address, optionIndex, "Yes");

            expect(await electionManager.getVoteCount(electionId, optionIndex)).to.equal(1);
            expect(await electionManager.voterHasVoted(electionId, addr1.address)).to.be.true;
        });

        it("Should prevent voting if election is not active", async function () {
            await electionManager.connect(owner).toggleElectionStatus(1); // Deactivate election 1
            await expect(
                electionManager.connect(addr1).castVote(1, 0)
            ).to.be.revertedWith("This election is not currently active.");
        });

        it("Should prevent voting with an invalid option index", async function () {
            await expect(
                electionManager.connect(addr1).castVote(1, 5) // Option index 5 is out of bounds
            ).to.be.revertedWith("Invalid option index.");
        });

        it("Should prevent a user from voting twice in the same election", async function () {
            await electionManager.connect(addr1).castVote(1, 0); // First vote
            await expect(
                electionManager.connect(addr1).castVote(1, 1) // Second attempt by same user
            ).to.be.revertedWith("You have already voted in this election.");
        });

        it("Should correctly report all vote counts for an election", async function () {
            const electionId = 1;
            await electionManager.connect(addr1).castVote(electionId, 0); // addr1 votes for "Yes"
            await electionManager.connect(addr2).castVote(electionId, 1); // addr2 votes for "No"
            await electionManager.connect(addrs[0]).castVote(electionId, 0); // addrs[0] votes for "Yes"

            const counts = await electionManager.getAllVoteCountsForElection(electionId);
            expect(counts.length).to.equal(2);
            expect(counts[0]).to.equal(2); // 2 votes for "Yes"
            expect(counts[1]).to.equal(1); // 1 vote for "No"
        });
    });

    describe("Election Status Toggle", function() {
        it("Should allow owner to toggle election status", async function() {
            await electionManager.connect(owner).createElection("Toggle Test", "Desc", ["A", "B"], true);
            const electionId = 1;

            let details = await electionManager.getElectionDetails(electionId);
            expect(details.isActive).to.be.true;

            await electionManager.connect(owner).toggleElectionStatus(electionId);
            details = await electionManager.getElectionDetails(electionId);
            expect(details.isActive).to.be.false;

            await electionManager.connect(owner).toggleElectionStatus(electionId);
            details = await electionManager.getElectionDetails(electionId);
            expect(details.isActive).to.be.true;
        });

        it("Should not allow non-owner to toggle election status", async function() {
            await electionManager.connect(owner).createElection("Toggle Test", "Desc", ["A", "B"], true);
            await expect(
                electionManager.connect(addr1).toggleElectionStatus(1)
            ).to.be.revertedWith("Only owner can call this function");
        });
    });
});
