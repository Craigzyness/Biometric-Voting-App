// scripts/deploy_election_manager.js
async function main() {
  const [deployer] = await ethers.getSigners();

  console.log("Deploying ElectionManager contract with the account:", deployer.address);
  console.log("Account balance:", (await ethers.provider.getBalance(deployer.address)).toString());

  const ElectionManagerFactory = await ethers.getContractFactory("ElectionManager");
  const electionManager = await ElectionManagerFactory.deploy();

  await electionManager.waitForDeployment();

  const contractAddress = await electionManager.getAddress();
  console.log("ElectionManager contract deployed to address:", contractAddress);

  // To get the ABI:
  // 1. After compilation, the ABI can be found in the artifacts directory:
  //    artifacts/contracts/ElectionManager.sol/ElectionManager.json
  // 2. You can also log a part of it here if needed, or save it to a file.
  // For example, to save ABI to a file (optional, can be done manually):
  /*
  const fs = require('fs');
  const path = require('path');
  const contractArtifact = require('../artifacts/contracts/ElectionManager.sol/ElectionManager.json');
  const abiPath = path.join(__dirname, '..', 'abi'); // Create an 'abi' folder in src/blockchain
  if (!fs.existsSync(abiPath)) {
    fs.mkdirSync(abiPath);
  }
  fs.writeFileSync(path.join(abiPath, 'ElectionManager.json'), JSON.stringify(contractArtifact.abi, null, 2));
  console.log("ElectionManager ABI saved to src/blockchain/abi/ElectionManager.json");
  */
}

main()
  .then(() => process.exit(0))
  .catch((error) => {
    console.error(error);
    process.exit(1);
  });
