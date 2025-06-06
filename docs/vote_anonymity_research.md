# Vote Anonymity Approaches for Blockchain-Based Voting - Initial Research

## 1. Introduction
The core promise of using blockchain for the Biometric Voting App includes enhancing security and anonymity. Currently, our proof-of-concept casts votes on the blockchain using a single backend-controlled address. While the application user's identity is not directly on the chain with the vote, all votes originate from this known backend address. This does not provide strong on-chain anonymity for individual votes against a determined adversary who might try to link voting patterns or timing back to off-chain user activity.

This document explores several high-level approaches to enhance voter anonymity for votes cast on an Ethereum-based blockchain.

## 2. Desired Properties for Anonymity
- **Unlinkability:** It should be infeasible to link a cast vote back to the specific eligible voter who cast it.
- **Receipt-Freeness (Optional but Ideal):** Voters should not be able to prove to a third party how they voted, to prevent vote buying or coercion.
- **Eligibility & Uniqueness:** Only eligible voters can vote, and only once per election. (This is already partially handled by our app-level SQL checks and smart contract's `voterHasVoted` for the *backend's address*).

## 3. Potential Anonymity Approaches

### 3.1. Backend-Managed Anonymity (Proxy/Relay System)

-   **Concept:** The backend maintains a pool of pre-funded, anonymous Ethereum addresses. When a user casts a vote through the app:
    1.  The user authenticates to the backend (e.g., with biometrics).
    2.  The backend verifies eligibility and ensures the user hasn't voted before (using its off-chain database).
    3.  The backend randomly selects an address from its pool that has not yet been used for this specific election (or uses a fresh one).
    4.  This selected anonymous address is then used to submit the actual vote transaction to the smart contract.
-   **Pros:**
    -   Relatively simpler to implement compared to advanced cryptographic schemes.
    -   Does not require significant changes to the current `ElectionManager` smart contract's `castVote` function (as `msg.sender` would be one of these pool addresses).
    -   UX for the voter remains simple.
-   **Cons:**
    -   **Trust in Backend:** Relies heavily on the backend to correctly manage the pool and not keep logs linking the application user to the anonymous address used for their vote. A compromised or malicious backend operator could break anonymity.
    -   **Scalability/Management:** Managing a large pool of addresses, ensuring they are funded for gas, and ensuring they are used in a way that doesn't create new linkable patterns can be complex.
    -   **Limited On-Chain Anonymity:** While it breaks the link from *one* backend address, if the pool is small or usage patterns emerge, it might still be possible to make statistical inferences.
-   **Applicability:** High for a PoC or an initial version where trust in the backend operator is accepted. It's a pragmatic first step beyond a single backend signer.
-   **Complexity:** Medium. Involves backend logic for address pool management, secure private key storage for pool addresses, and transaction dispatch.
-   **Impact:**
    -   Smart Contract: Minimal changes to `castVote` itself, but the `voterHasVoted` mapping in the SC would now track these pool addresses, not the end-users. The application-level (SQL) check for user double-voting remains critical.
    -   Backend: Significant new logic for address management and dispatch.

### 3.2. Commit-Reveal Schemes

-   **Concept:**
    1.  **Commit Phase:** Voters submit a "commitment" to their vote (e.g., a hash of their chosen option plus a secret nonce: `hash(option, nonce)`). These commitments are recorded on-chain.
    2.  **Reveal Phase:** After the commit phase ends, voters reveal their chosen option and the secret nonce. The smart contract verifies that the revealed data matches the previously submitted commitment hash. If it matches, the vote is counted.
-   **Pros:**
    -   Can prevent voters from changing their vote based on others' revealed votes (if commitments are binding).
    -   Can offer a degree of privacy during the voting process as the actual vote is not known until the reveal phase.
-   **Cons:**
    -   **Two-Step Process:** More complex UX, as voters need to participate in two phases.
    -   **Voter Drop-off:** Voters who commit might not reveal, leading to uncounted votes.
    -   **Anonymity Limitations:** If the reveal transaction is sent from an address linkable to the voter, anonymity is lost at that point. Needs to be combined with other techniques (like Approach 3.1) for the reveal transaction.
    -   Does not inherently provide anonymity of the final vote count if not combined with other mixing or encryption techniques.
-   **Applicability:** Moderate. Can be useful, but the two-phase UX and the need for another layer for anonymous reveals are drawbacks.
-   **Complexity:** Medium to High. Requires significant smart contract changes and careful management of phases.
-   **Impact:**
    -   Smart Contract: Major redesign to handle commit and reveal phases, commitment storage, and verification.
    -   Backend: Needs to manage the two phases and potentially assist users.
    -   Frontend: Needs UI for both commit and reveal.

### 3.3. Ring Signatures

-   **Concept:** A voter signs a message (their vote) with their private key, but the signature is constructed in such a way that it proves the signer is a member of a predefined "ring" of eligible voters, without revealing which specific member signed it.
-   **Pros:**
    -   Strong anonymity within the ring.
    -   One-step voting process for the user (after initial setup/key registration).
-   **Cons:**
    -   **Complexity:** Implementing ring signatures correctly is cryptographically complex. Finding robust, audited libraries for Solidity can be challenging.
    -   **Ring Management:** Defining and managing the "ring" of public keys for eligible voters on-chain can be complex and potentially gas-intensive.
    -   **Scalability:** Signature size and verification cost can increase with the size of the ring.
    -   Does not inherently prevent double voting without additional mechanisms (e.g., nullifiers, linkable ring signatures).
-   **Applicability:** Potentially high for strong anonymity, but the implementation complexity is a major hurdle for a PoC or early versions.
-   **Complexity:** Very High.
-   **Impact:**
    -   Smart Contract: Needs to store ring member public keys and verify ring signatures.
    -   Backend/Frontend: Users need to manage keys capable of ring signing; complex client-side operations.

### 3.4. Zero-Knowledge Proofs (zk-SNARKs/STARKS)

-   **Concept:** Voters prove cryptographically that they are eligible to vote and that their vote was cast correctly according to the rules, *without revealing any information about their identity or their specific vote*.
    1.  Voter generates a proof off-chain.
    2.  The proof and an encrypted/committed vote are submitted to the smart contract.
    3.  The smart contract verifies the proof. If valid, the vote is accepted.
-   **Pros:**
    -   **Strongest Anonymity & Verifiability:** Offers excellent privacy and ensures votes are valid without revealing sensitive data.
    -   Can handle complex voting rules.
-   **Cons:**
    -   **Extreme Complexity:** Designing, implementing, and auditing ZKP systems is highly specialized and resource-intensive.
    -   **Gas Costs:** On-chain ZKP verification can be gas-intensive, though newer ZKP schemes are improving this.
    -   **Trusted Setup (for some zk-SNARKs):** Some schemes require a trusted setup ceremony.
    -   **Prover Time:** Generating proofs can take time on the user's device.
-   **Applicability:** Ideal for achieving very strong anonymity and verifiability, but likely too complex for initial implementation in this project unless specialized expertise is available. Considered a long-term goal.
-   **Complexity:** Extremely High.
-   **Impact:**
    -   Smart Contract: Needs verifier contracts for the ZKPs.
    -   Backend: May need to support parts of the proof generation or interaction.
    -   Frontend: User's device needs to generate the ZKPs, requiring significant computational resources and specialized libraries.

## 4. Phased Approach Recommendation for This Project

Given the project's current stage and the complexity of these solutions:

1.  **Short-Term (Next Iteration after current PoC): Implement Backend-Managed Anonymity (Proxy/Relay System - Approach 3.1).**
    -   **Rationale:** This provides a tangible improvement over the single backend signer account, offering a basic level of vote unlikability from a casual on-chain observer, without requiring immediate deep cryptographic expertise or major smart contract overhauls.
    -   **Focus:** Securely manage a pool of addresses on the backend. Ensure the application-level (SQL) check for "user has voted" remains robust.
    -   The `voterHasVoted` mapping in the smart contract would then track these backend-controlled pool addresses for each election.

2.  **Medium-Term (Post-PoC, Pre-Production): Explore Commit-Reveal with Backend-Managed Anonymous Reveals OR investigate more robust Linkable Ring Signatures (if feasible libraries emerge).**
    -   **Rationale:** Layering commit-reveal can add resistance to vote selling/coercion if the reveal is also anonymized. Linkable ring signatures could offer stronger guarantees if practical.

3.  **Long-Term (Future Enhancements): Evaluate Zero-Knowledge Proofs.**
    -   **Rationale:** For the highest level of trustless anonymity and verifiability, ZKPs are the gold standard. This would be a significant R&D effort.

## 5. Conclusion
Achieving strong, trustless vote anonymity on a transparent ledger like Ethereum is challenging. A phased approach, starting with simpler backend-managed techniques and progressively exploring more advanced cryptographic solutions, is recommended. The immediate next step should focus on implementing a proxy/relay system to break the link from a single backend signing address.
