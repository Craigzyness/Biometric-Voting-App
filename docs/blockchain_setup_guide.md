# Blockchain Development Environment Setup Guide

## 1. Introduction
This guide outlines the setup for the blockchain development environment for the Biometric Voting App. We will be using:
-   **Blockchain:** A local Ethereum development network provided by Ganache.
-   **Smart Contract Language:** Solidity.
-   **Development Framework:** Hardhat.

Ganache provides a personal Ethereum blockchain for development, allowing you to deploy contracts, develop applications, and run tests. Hardhat is a development environment that facilitates compiling, deploying, testing, and debugging Ethereum software.

## 2. Prerequisites
-   **Node.js and npm:** Required for Hardhat and its dependencies. Download from [https://nodejs.org/](https://nodejs.org/) (LTS version recommended).
-   **Git:** For version control.

## 3. Tools Installation

### 3.1. Ganache
   -   Ganache provides a UI for managing your local blockchain, accounts, and blocks.
   -   Download and install Ganache from [https://trufflesuite.com/ganache/](https://trufflesuite.com/ganache/).
   -   After installation, run Ganache. It will typically start a local Ethereum node (e.g., at `HTTP://127.0.0.1:7545` or `HTTP://127.0.0.1:8545`). Note this RPC server address.

### 3.2. Hardhat (Project-Local Installation)
   Hardhat will be installed locally within our blockchain project directory. This is handled when initializing the Hardhat project.

## 4. Initializing the Hardhat Project
   The Hardhat project will reside in the `src/blockchain/` directory of our main application.

   1.  **Navigate to the directory:**
       ```bash
       cd src/blockchain
       ```
       (If this directory doesn't exist, it will be created by the Hardhat initialization process or can be created manually first).

   2.  **Initialize Hardhat Project:**
       Run the following command in the `src/blockchain/` directory:
       ```bash
       npx hardhat
       ```
       -   You will be prompted with a few questions:
           -   "What do you want to do?" -> Select "Create a JavaScript project" (or TypeScript if preferred, but we'll assume JS for this guide).
           -   "Hardhat project root:" -> Confirm the default (`src/blockchain`).
           -   "Do you want to add a .gitignore?" -> Yes.
           -   "Do you want to install sample project's dependencies with npm (@nomicfoundation/hardhat-toolbox)?" -> Yes.
       -   This will create several files and directories, including:
           -   `hardhat.config.js`: Hardhat configuration file.
           -   `contracts/`: Directory for your Solidity smart contracts.
           -   `scripts/`: Directory for deployment and interaction scripts.
           -   `test/`: Directory for test files.
           -   `package.json`: For managing project dependencies.
           -   `node_modules/`: Created after dependencies are installed.

   3.  **Configure Hardhat for Ganache (Optional but Recommended):**
       Open `hardhat.config.js`. You can add a network configuration for Ganache:
       ```javascript
       require("@nomicfoundation/hardhat-toolbox");

       /** @type import('hardhat/config').HardhatUserConfig */
       module.exports = {
         solidity: "0.8.20", // Specify your Solidity compiler version
         networks: {
           ganache: {
             url: "http://127.0.0.1:7545", // Or whatever port Ganache is running on
             // accounts: [privateKey1, privateKey2, ...] // Optional: if you want to use specific Ganache accounts
           },
           // Hardhat also provides a default in-memory network 'hardhat'
         }
       };
       ```
       Replace the URL if your Ganache runs on a different port. Using specific accounts is optional; Hardhat can use default Ganache accounts if not specified.

## 5. Next Steps
With Ganache running and Hardhat initialized, you can now:
-   Write Solidity smart contracts in the `contracts/` directory.
-   Create deployment scripts in the `scripts/` directory.
-   Write tests for your contracts in the `test/` directory.
-   Compile contracts using `npx hardhat compile`.
-   Run tests using `npx hardhat test`.
-   Deploy contracts using `npx hardhat run scripts/deploy.js --network ganache`.

Always refer to the official [Hardhat Documentation](https://hardhat.org/docs) for detailed information.
