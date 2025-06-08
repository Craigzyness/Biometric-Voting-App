# System Architecture for Biometric Voting App

## 1. Overview
This document outlines the proposed system architecture for the Biometric Voting App. It covers technology choices, application structure, database design considerations, and component interactions.

## 2. Technology Stack Choices

### 2.1. Frontend (Voter Interface & Admin Panel)
    - **Mobile App (Primary Voter Interface):**
        - React Native or Flutter: For cross-platform development (iOS & Android), allowing for a wider reach and faster development.
        - Justification: Access to device hardware (camera, fingerprint sensor) is crucial for biometrics.
    - **Web App (Admin Panel & Fallback/Alternative Voter Interface):**
        - React, Angular, or Vue.js: For a responsive and modern web interface.
        - Justification: Easier access for administrators and potentially for voters who prefer or require a web-based option.

### 2.2. Backend
    - **Programming Language & Framework:**
        - Python (Django/Flask) or Node.js (Express.js): Both offer robust ecosystems, scalability, and are well-suited for API development.
        - Justification: Strong community support, many available libraries for tasks like database interaction, cryptography, and API development.
    - **Database:**
        - PostgreSQL or MySQL: For storing user registration data (excluding raw biometrics), election configurations, and audit logs.
        - Justification: Reliable, feature-rich SQL databases.
    - **Biometric Data Handling:**
        - Secure Biometric SDK/API: Use a well-vetted SDK (e.g., from a reputable vendor or an open-source solution with strong security audits) for biometric template generation and matching.
        - **Important:** Raw biometric data will NOT be stored. Only secure templates or hashes will be stored.

### 2.3. Blockchain
    - **Platform:**
        - Hyperledger Fabric, Ethereum (private network or Layer 2 solution), or a purpose-built voting blockchain.
        - Justification:
            - Hyperledger Fabric: Permissioned network, suitable for controlled environments, offers fine-grained access control.
            - Ethereum: Large developer community, smart contract capabilities. A private network or Layer 2 solution would be needed to manage transaction costs and privacy.
    - **Smart Contracts:**
        - To manage the voting process (e.g., registering candidates, opening/closing voting, recording votes, tallying results).

### 2.4. Communication
    - **APIs:** RESTful APIs or GraphQL for communication between frontend, backend, and potentially the blockchain interface.
    - **Security:** HTTPS/TLS for all communications.

## 3. Application Structure

### 3.1. Voter Mobile Application
    - User Registration Module (with biometric capture).
    - Authentication Module (biometric matching).
    - Ballot Display Module.
    - Secure Voting Module (interacts with backend to cast vote to blockchain).
    - Local secure storage for any session-related tokens.

### 3.2. Backend Services
    - **User Management Service:** Handles registration, profile data (excluding raw biometrics), and interfaces with the Biometric SDK.
    - **Election Management Service:** Manages election setup, candidate data, and ballot information.
    - **Voting Service:**
        - Receives vote requests from authenticated users.
        - Implements cryptographic measures for anonymity (e.g., blind signatures before sending to blockchain).
        - Interfaces with the blockchain to record votes.
    - **Blockchain Interface Service:** Abstracts direct communication with the blockchain node/network.
    - **Results Service:** Queries the blockchain for vote counts and prepares results for display.

### 3.3. Admin Web Portal
    - Dashboard for election monitoring.
    - Election creation and configuration tools.
    - Candidate management.
    - User oversight (e.g., view registration statistics, manage issues - without access to sensitive PII or votes).
    - Results publication management.

### 3.4. Blockchain Network
    - Nodes participating in the consensus mechanism.
    - Smart contracts deployed for election logic and vote storage.

## 4. Database Schema (Conceptual)

### 4.1. Users Table
    - `user_id` (Primary Key)
    - `voter_id_hash` (Hashed version of a national/voter ID for uniqueness checks)
    - `demographic_data_encrypted` (Encrypted PII like name, DOB - if stored, subject to strict access controls)
    - `biometric_template_id` (Pointer to a secure biometric template/hash, potentially stored in a separate, highly secured system or managed by the Biometric SDK)
    - `public_key_for_voting` (If using certain crypto schemes for anonymity)
    - `registration_date`
    - `last_login_date`
    - `is_active`

### 4.2. Elections Table
    - `election_id` (Primary Key)
    - `election_name`
    - `description`
    - `start_date`
    - `end_date`
    - `status` (e.g., upcoming, open, closed, tallied)

### 4.3. Candidates Table
    - `candidate_id` (Primary Key)
    - `election_id` (Foreign Key to Elections)
    - `candidate_name`
    - `party_affiliation` (Optional)
    - `candidate_details_encrypted`

### 4.4. Ballots Table (Conceptual - details depend on blockchain implementation)
    - This might not be a traditional SQL table if votes are directly on the blockchain.
    - If a reference is needed:
        - `ballot_id`
        - `election_id`
        - `voter_anonymous_id` (A one-time anonymous ID used for voting)
        - `vote_transaction_hash` (Reference to the blockchain transaction)
        - `timestamp`

**Note on Biometric Data:** Actual biometric templates would be managed by the chosen Biometric SDK, often in its own secure storage or returned as a template to be stored by the application (encrypted). The `biometric_template_id` would be a reference.

## 5. Component Interactions (High-Level)

1.  **Registration:**
    - Voter uses Mobile App to submit demographic info & capture biometrics.
    - App sends data to Backend User Management Service.
    - Backend uses Biometric SDK to generate a template/hash.
    - User info (with biometric template ID) stored in DB.
2.  **Authentication:**
    - Voter uses Mobile App to provide biometrics.
    - App sends biometric data to Backend Authentication Service.
    - Backend uses Biometric SDK to match against stored templates.
3.  **Voting:**
    - Authenticated voter receives ballot on Mobile App (from Election Management Service).
    - Voter makes selection.
    - App sends encrypted/anonymized vote to Backend Voting Service.
    - Voting Service performs necessary cryptographic operations (e.g., blinding) and sends vote to Blockchain Interface Service.
    - Blockchain Interface Service submits the vote as a transaction to the Blockchain.
4.  **Tallying:**
    - Admin uses Admin Web Portal to initiate tallying (after election closes).
    - Backend Results Service queries the Blockchain (via Blockchain Interface Service) for all valid vote transactions.
    - Results are compiled and made available.

This architecture provides a starting point and will be refined as development progresses and specific technologies are finalized.
