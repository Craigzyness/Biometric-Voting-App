require("@nomicfoundation/hardhat-toolbox");

/** @type import('hardhat/config').HardhatUserConfig */
module.exports = {
  solidity: "0.8.20", // Default or common version
  networks: {
    // Configuration for local Ganache instance (example)
    ganache: {
      url: "http://127.0.0.1:7545", // Default Ganache UI port
      // accounts: ['YOUR_GANACHE_PRIVATE_KEY_HERE'] // Optional
    },
    // Hardhat Network (default, in-memory) is also available
    // localhost: { // Often used for 'npx hardhat node' instance
    //   url: "http://127.0.0.1:8545"
    // }
  }
};
