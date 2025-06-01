import json
import os
from web3 import Web3
from web3.exceptions import ContractLogicError, TransactionNotFound
from hexbytes import HexBytes
import traceback # For detailed error logging

# --- Configuration ---
DEPLOYED_CONTRACT_ADDRESS = os.environ.get('CONTRACT_ADDRESS', 'YOUR_DEPLOYED_CONTRACT_ADDRESS_HERE')
GANACHE_URL = os.environ.get('GANACHE_URL', "http://127.0.0.1:7545")
ABI_FILE_PATH = os.path.join(
    os.path.dirname(__file__), '..', 'blockchain', 'artifacts', 'contracts',
    'ElectionManager.sol', 'ElectionManager.json'
)

w3 = None
election_manager_contract = None
contract_abi = None
TRANSACTION_SIGNER_PRIVATE_KEY = os.environ.get('GANACHE_PRIVATE_KEY_FOR_TX')
SIGNER_ACCOUNT_ADDRESS = None

def is_connected(): return w3 and w3.is_connected()

def load_contract_abi():
    global contract_abi
    if contract_abi: return contract_abi
    try:
        with open(ABI_FILE_PATH, 'r') as f: artifact = json.load(f); contract_abi = artifact['abi']
        # print("Contract ABI loaded successfully.") # Less verbose
        return contract_abi
    except FileNotFoundError: print(f"ERROR: ABI file not found: {ABI_FILE_PATH}")
    except Exception as e: print(f"Error loading ABI: {e}")
    return None

def init_blockchain_connection():
    global w3, election_manager_contract, contract_abi, SIGNER_ACCOUNT_ADDRESS
    if election_manager_contract and w3 and w3.is_connected(): return True
    if DEPLOYED_CONTRACT_ADDRESS == 'YOUR_DEPLOYED_CONTRACT_ADDRESS_HERE':
        print("ERROR: DEPLOYED_CONTRACT_ADDRESS not set")
        return False
    if not contract_abi: contract_abi = load_contract_abi();
    if not contract_abi: return False # Ensure ABI was loaded
    try:
        w3 = Web3(Web3.HTTPProvider(GANACHE_URL))
        if not w3.is_connected(): print(f"ERROR: Failed to connect to Ganache: {GANACHE_URL}"); w3 = None; return False

        try:
            checksum_address = w3.to_checksum_address(DEPLOYED_CONTRACT_ADDRESS)
        except ValueError as ve:
            print(f"ERROR: Invalid DEPLOYED_CONTRACT_ADDRESS format: {DEPLOYED_CONTRACT_ADDRESS} - {ve}")
            w3 = None; return False

        election_manager_contract = w3.eth.contract(address=checksum_address, abi=contract_abi)
        # print(f"Blockchain connected: {GANACHE_URL}. Contract at: {checksum_address}") # Less verbose

        if TRANSACTION_SIGNER_PRIVATE_KEY:
            try:
                account = w3.eth.account.from_key(TRANSACTION_SIGNER_PRIVATE_KEY)
                SIGNER_ACCOUNT_ADDRESS = account.address
                w3.eth.default_account = SIGNER_ACCOUNT_ADDRESS
                # print(f"Transaction signing account loaded: {SIGNER_ACCOUNT_ADDRESS}") # Less verbose
            except Exception as e: print(f"WARNING: Bad GANACHE_PRIVATE_KEY_FOR_TX: {e}"; SIGNER_ACCOUNT_ADDRESS = None) # Ensure None on bad key
        else: print("WARNING: GANACHE_PRIVATE_KEY_FOR_TX not set. Transactions will fail if gas is required from a specific account.")
        return True
    except Exception as e: print(f"Error initializing blockchain connection/contract: {e}"); w3=None;election_manager_contract=None;SIGNER_ACCOUNT_ADDRESS=None;return False

def get_contract():
    if not w3 or not w3.is_connected() or not election_manager_contract:
        if not init_blockchain_connection(): return None
    return election_manager_contract

# --- Read Functions (existing, condensed) ---
def get_election_details_from_chain(election_id):
    contract = get_contract();
    if not contract: return None, "Blockchain service unavailable."
    try:
        details_tuple = contract.functions.getElectionDetails(election_id).call()
        return {"id":details_tuple[0], "title":details_tuple[1], "description":details_tuple[2],
                "options":[{"id":i, "text":details_tuple[3][i]} for i in range(len(details_tuple[3]))],
                "isActive":details_tuple[4], "source":"blockchain"}, None
    except ContractLogicError as e: return None, f"Blockchain Error: {e.message if hasattr(e, 'message') else str(e)}"
    except Exception as e: print(f"Err getElectionDetails {election_id}: {e}"); return None, f"Tech err fetch details: {e}"

def get_elections_count_from_chain():
    contract = get_contract();
    if not contract: return None, "Blockchain service unavailable."
    try: return contract.functions.getElectionsCount().call(), None
    except Exception as e: print(f"Err getElectionsCount: {e}"); return None, f"Tech err fetch count: {e}"

def get_all_vote_counts_for_election_from_chain(election_id):
    contract = get_contract();
    if not contract: return None, "Blockchain service unavailable."
    try: return contract.functions.getAllVoteCountsForElection(election_id).call(), None
    except ContractLogicError as e: return None, f"Blockchain Error: {e.message if hasattr(e, 'message') else str(e)}"
    except Exception as e: print(f"Err getAllVoteCounts {election_id}: {e}"); return None, f"Tech err fetch counts: {e}"

# --- Write Functions (existing create_election_on_chain, new cast_vote_on_chain) ---
def create_election_on_chain(title: str, description: str, options: list[str], is_active: bool):
    contract = get_contract()
    if not contract: return None, "Blockchain service unavailable."
    if not SIGNER_ACCOUNT_ADDRESS or not TRANSACTION_SIGNER_PRIVATE_KEY: return None, "Backend signer account not configured."
    try:
        if not all(isinstance(opt, str) for opt in options): return None, "Invalid options format: All options must be strings."
        nonce = w3.eth.get_transaction_count(SIGNER_ACCOUNT_ADDRESS)
        txn_params = {'from': SIGNER_ACCOUNT_ADDRESS, 'nonce': nonce}
        unsent_tx = contract.functions.createElection(title, description, options, is_active).build_transaction(txn_params)
        signed_tx = w3.eth.account.sign_transaction(unsent_tx, private_key=TRANSACTION_SIGNER_PRIVATE_KEY)
        tx_hash_hexbytes = w3.eth.send_raw_transaction(signed_tx.rawTransaction)
        tx_hash = HexBytes(tx_hash_hexbytes).hex()
        # print(f"Create election transaction sent. Hash: {tx_hash}") # Less verbose
        tx_receipt = w3.eth.wait_for_transaction_receipt(tx_hash_hexbytes, timeout=120)
        if tx_receipt.status == 1:
            logs = []
            try: logs = contract.events.ElectionCreated().process_receipt(tx_receipt)
            except Exception as e_event: print(f"Warning: Could not process ElectionCreated event: {e_event}")
            new_election_id = logs[0]['args']['id'] if logs and len(logs) > 0 and 'id' in logs[0]['args'] else None
            msg = "Event parsing failed for ElectionCreated" if new_election_id is None and logs is not None else None
            return {"tx_hash": tx_hash, "status":"success", "receipt": dict(tx_receipt), "election_id": new_election_id}, msg
        else: return None, f"Blockchain transaction failed with status {tx_receipt.status}."
    except ContractLogicError as e: return None, f"Smart contract error: {e.message if hasattr(e, 'message') else str(e)}"
    except TransactionNotFound: return None, f"Transaction with hash {tx_hash if 'tx_hash' in locals() else 'unknown'} not found after timeout."
    except Exception as e: print(f"Error calling createElection on chain: {e}\n{traceback.format_exc()}"); return None, f"Technical error: {e}"

def cast_vote_on_chain(election_id: int, option_index: int):
    contract = get_contract()
    if not contract:
        return None, "Blockchain service not available or contract not loaded."
    if not SIGNER_ACCOUNT_ADDRESS or not TRANSACTION_SIGNER_PRIVATE_KEY:
        return None, "Backend signer account not configured. Cannot send transaction."

    try:
        nonce = w3.eth.get_transaction_count(SIGNER_ACCOUNT_ADDRESS)
        txn_params = {
            'from': SIGNER_ACCOUNT_ADDRESS,
            'nonce': nonce,
        }

        unsent_tx = contract.functions.castVote(election_id, option_index).build_transaction(txn_params)
        signed_tx = w3.eth.account.sign_transaction(unsent_tx, private_key=TRANSACTION_SIGNER_PRIVATE_KEY)
        tx_hash_hexbytes = w3.eth.send_raw_transaction(signed_tx.rawTransaction)

        tx_hash = HexBytes(tx_hash_hexbytes).hex()
        # print(f"Cast vote transaction sent for election {election_id}, option {option_index}. Hash: {tx_hash}") # Less verbose

        tx_receipt = w3.eth.wait_for_transaction_receipt(tx_hash_hexbytes, timeout=120)

        if tx_receipt.status == 1:
            # print(f"Vote transaction successful. Block: {tx_receipt.blockNumber}") # Less verbose
            event_data_dict = None
            try:
                logs = contract.events.VoteCast().process_receipt(tx_receipt)
                if logs and len(logs) > 0 and logs[0]['args']:
                    event_data_dict = dict(logs[0]['args'])
                    # print(f"VoteCast event: {event_data_dict}") # Less verbose
            except Exception as e_event:
                print(f"Warning: Could not process VoteCast event: {e_event}")

            msg = "Event parsing failed for VoteCast" if event_data_dict is None and logs is not None else None
            return {"tx_hash": tx_hash, "status":"success", "receipt": dict(tx_receipt), "event_data": event_data_dict}, msg
        else:
            # print(f"Vote transaction failed. Receipt: {tx_receipt}") # Less verbose
            return None, f"Blockchain transaction failed with status {tx_receipt.status}."

    except ContractLogicError as e:
        print(f"Contract revert during castVote for election {election_id}: {e}")
        return None, f"Smart contract error: {e.message if hasattr(e, 'message') else str(e)}"
    except TransactionNotFound:
        print(f"Transaction with hash {tx_hash if 'tx_hash' in locals() else 'unknown'} not found after timeout for castVote.")
        return None, "Transaction not found after timeout, it might have been dropped."
    except Exception as e:
        print(f"Error calling castVote on chain for election {election_id}: {e}\n{traceback.format_exc()}")
        return None, f"Technical error casting vote on blockchain: {str(e)}"
