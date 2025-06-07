import json
import os
from web3 import Web3
from web3.exceptions import ContractLogicError, TransactionNotFound
# from web3.auto import w3 as web3_auto # Not actually used, can be removed
from eth_account.signers.local import LocalAccount
from hexbytes import HexBytes
import traceback
import random

# --- Configuration & Globals ---
DEPLOYED_CONTRACT_ADDRESS = os.environ.get('CONTRACT_ADDRESS', 'YOUR_DEPLOYED_CONTRACT_ADDRESS_HERE')
GANACHE_URL = os.environ.get('GANACHE_URL', "http://127.0.0.1:7545")
ABI_FILE_PATH = os.path.join(
    os.path.dirname(__file__), '..', 'blockchain', 'artifacts', 'contracts',
    'ElectionManager.sol', 'ElectionManager.json'
)
POOL_KEYS_FILE_PATH = os.path.join(os.path.dirname(__file__), 'pool_keys.json')

w3 = None
election_manager_contract = None
contract_abi = None

GANACHE_DEFAULT_SIGNER_KEY = os.environ.get('GANACHE_DEFAULT_SIGNER_KEY')
DEFAULT_SIGNER_ACCOUNT: LocalAccount | None = None # Type hint for LocalAccount or None

PROXY_VOTER_POOL: list[LocalAccount] = []
current_proxy_voter_index = 0

def _load_and_initialize_proxy_pool():
    global PROXY_VOTER_POOL, current_proxy_voter_index
    PROXY_VOTER_POOL = []
    current_proxy_voter_index = 0
    if not w3:
        print("ERROR (Proxy Pool): Web3 not initialized.")
        return False
    try:
        if not os.path.exists(POOL_KEYS_FILE_PATH):
            print(f"WARNING (Proxy Pool): File not found: {POOL_KEYS_FILE_PATH}. Proxy voting disabled.")
            return False
        with open(POOL_KEYS_FILE_PATH, 'r') as f: keys_data = json.load(f)
        pool_private_keys_hex = keys_data.get("pool_private_keys", [])
        if not pool_private_keys_hex or not isinstance(pool_private_keys_hex, list):
            print("WARNING (Proxy Pool): 'pool_private_keys' invalid in pool_keys.json. Proxy voting disabled.")
            return False
        for pk_hex in pool_private_keys_hex:
            try: PROXY_VOTER_POOL.append(w3.eth.account.from_key(pk_hex))
            except Exception as e_key: print(f"WARNING (Proxy Pool): Failed to load key {pk_hex[:10]}...: {e_key}")
        if PROXY_VOTER_POOL: print(f"Proxy voter pool loaded with {len(PROXY_VOTER_POOL)} accounts.")
        else: print("WARNING (Proxy Pool): No valid keys in pool. Proxy voting disabled.")
        return bool(PROXY_VOTER_POOL) # Return True if pool is not empty
    except Exception as e: print(f"ERROR (Proxy Pool): Failed to load pool_keys.json: {e}"); traceback.print_exc(); return False

def get_next_proxy_voter_account() -> LocalAccount | None:
    global current_proxy_voter_index
    if not PROXY_VOTER_POOL: return None
    account_to_use = PROXY_VOTER_POOL[current_proxy_voter_index]
    current_proxy_voter_index = (current_proxy_voter_index + 1) % len(PROXY_VOTER_POOL)
    return account_to_use

def is_connected(): return w3 and w3.is_connected()

def load_contract_abi():
    global contract_abi
    if contract_abi: return contract_abi
    try:
        with open(ABI_FILE_PATH, 'r') as f: artifact = json.load(f); contract_abi = artifact['abi']
        return contract_abi
    except FileNotFoundError: print(f"ERROR: ABI file not found: {ABI_FILE_PATH}")
    except Exception as e: print(f"Error loading ABI: {e}")
    return None

def init_blockchain_connection():
    global w3, election_manager_contract, contract_abi, DEFAULT_SIGNER_ACCOUNT
    if election_manager_contract and w3 and w3.is_connected(): return True
    if DEPLOYED_CONTRACT_ADDRESS == 'YOUR_DEPLOYED_CONTRACT_ADDRESS_HERE': print("ERROR: DEPLOYED_CONTRACT_ADDRESS not set"); return False
    if not contract_abi: contract_abi = load_contract_abi();
    if not contract_abi: return False
    try:
        w3 = Web3(Web3.HTTPProvider(GANACHE_URL))
        if not w3.is_connected(): print(f"ERROR: Failed to connect to Ganache: {GANACHE_URL}"); w3 = None; return False
        try: checksum_address = w3.to_checksum_address(DEPLOYED_CONTRACT_ADDRESS)
        except ValueError as ve: print(f"ERROR: Invalid DEPLOYED_CONTRACT_ADDRESS: {DEPLOYED_CONTRACT_ADDRESS} - {ve}"); w3 = None; return False
        election_manager_contract = w3.eth.contract(address=checksum_address, abi=contract_abi)
        if GANACHE_DEFAULT_SIGNER_KEY:
            try:
                DEFAULT_SIGNER_ACCOUNT = w3.eth.account.from_key(GANACHE_DEFAULT_SIGNER_KEY)
                print(f"Default transaction signing account loaded: {DEFAULT_SIGNER_ACCOUNT.address}")
            except Exception as e: print(f"WARNING: Bad GANACHE_DEFAULT_SIGNER_KEY: {e}")
        else: print("WARNING: GANACHE_DEFAULT_SIGNER_KEY not set for admin actions.")
        _load_and_initialize_proxy_pool()
        print(f"Blockchain connected: {GANACHE_URL}. Contract: {checksum_address}")
        return True
    except Exception as e: print(f"Error initializing blockchain connection: {e}"); traceback.print_exc(); w3=None;election_manager_contract=None;DEFAULT_SIGNER_ACCOUNT=None; global PROXY_VOTER_POOL; PROXY_VOTER_POOL=[]; return False

def get_contract():
    if not w3 or not w3.is_connected() or not election_manager_contract:
        if not init_blockchain_connection(): return None
    return election_manager_contract

# --- Read Functions ---
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

# --- Write Functions ---
def create_election_on_chain(title: str, description: str, options: list[str], is_active: bool):
    contract = get_contract()
    if not contract: return None, "Blockchain service unavailable."
    if not DEFAULT_SIGNER_ACCOUNT: return None, "Backend default signer for admin actions not configured."
    try:
        if not all(isinstance(opt, str) for opt in options): return None, "Invalid options format."
        nonce = w3.eth.get_transaction_count(DEFAULT_SIGNER_ACCOUNT.address)
        txn_params = {'from': DEFAULT_SIGNER_ACCOUNT.address, 'nonce': nonce}
        unsent_tx = contract.functions.createElection(title, description, options, is_active).build_transaction(txn_params)
        signed_tx = w3.eth.account.sign_transaction(unsent_tx, private_key=DEFAULT_SIGNER_ACCOUNT.key) # Use .key for LocalAccount
        tx_hash_hexbytes = w3.eth.send_raw_transaction(signed_tx.rawTransaction)
        tx_hash = HexBytes(tx_hash_hexbytes).hex()
        tx_receipt = w3.eth.wait_for_transaction_receipt(tx_hash_hexbytes, timeout=120)
        if tx_receipt.status == 1:
            logs = []; new_election_id = None; msg = None
            try: logs = contract.events.ElectionCreated().process_receipt(tx_receipt)
            except Exception as e_event: print(f"Warning: Could not process ElectionCreated event: {e_event}")
            if logs and len(logs) > 0 and 'id' in logs[0]['args']: new_election_id = logs[0]['args']['id']
            else: msg = "Event parsing failed for ElectionCreated" if logs is not None else "No logs found for ElectionCreated"
            return {"tx_hash": tx_hash, "status":"success", "receipt": dict(tx_receipt), "election_id": new_election_id}, msg
        else: return None, f"Blockchain transaction failed with status {tx_receipt.status}."
    except ContractLogicError as e: return None, f"Smart contract error: {e.message if hasattr(e, 'message') else str(e)}"
    except TransactionNotFound: return None, f"Transaction with hash {tx_hash if 'tx_hash' in locals() else 'unknown'} not found."
    except Exception as e: print(f"Error createElection: {e}\n{traceback.format_exc()}"); return None, f"Technical error: {e}"

def cast_vote_on_chain(election_id: int, option_index: int):
    contract = get_contract()
    if not contract:
        return None, "Blockchain service not available or contract not loaded."

    proxy_account = get_next_proxy_voter_account()
    if not proxy_account:
        return None, "Proxy voter pool exhausted or not configured. Cannot send transaction."

    print(f"INFO (cast_vote_on_chain): Using proxy account {proxy_account.address} to cast vote.")

    try:
        nonce = w3.eth.get_transaction_count(proxy_account.address)
        txn_params = {
            'from': proxy_account.address,
            'nonce': nonce,
        }
        unsent_tx = contract.functions.castVote(election_id, option_index).build_transaction(txn_params)
        signed_tx = w3.eth.account.sign_transaction(unsent_tx, private_key=proxy_account.key) # Use proxy's private key
        tx_hash_hexbytes = w3.eth.send_raw_transaction(signed_tx.rawTransaction)
        tx_hash = HexBytes(tx_hash_hexbytes).hex()
        # print(f"Cast vote transaction sent from proxy {proxy_account.address} for election {election_id}, option {option_index}. Hash: {tx_hash}")
        tx_receipt = w3.eth.wait_for_transaction_receipt(tx_hash_hexbytes, timeout=120)

        if tx_receipt.status == 1:
            # print(f"Vote transaction successful from proxy {proxy_account.address}. Block: {tx_receipt.blockNumber}")
            logs = []; event_data_dict = None; msg = None
            try:
                logs = contract.events.VoteCast().process_receipt(tx_receipt)
                if logs and len(logs) > 0 and logs[0]['args']:
                    event_data_dict = dict(logs[0]['args'])
            except Exception as e_event:
                print(f"Warning: Could not process VoteCast event: {e_event}")

            if event_data_dict is None and logs is not None : msg = "Event parsing failed for VoteCast" # Check if logs was None before trying len
            return {"tx_hash": tx_hash, "status":"success", "receipt": dict(tx_receipt), "event_data": event_data_dict, "proxy_address_used": proxy_account.address}, msg
        else:
            # print(f"Vote transaction FAILED from proxy {proxy_account.address}. Receipt: {tx_receipt}")
            return None, f"Blockchain transaction failed with status {tx_receipt.status} (from proxy {proxy_account.address})."
    except ContractLogicError as e:
        print(f"Contract revert during castVote from proxy {proxy_account.address} for election {election_id}: {e}")
        return None, f"Smart contract error: {e.message if hasattr(e, 'message') else str(e)} (tx from {proxy_account.address})"
    except TransactionNotFound:
        print(f"Transaction with hash {tx_hash if 'tx_hash' in locals() else 'unknown'} not found (castVote from proxy).")
        return None, "Transaction not found after timeout."
    except Exception as e:
        print(f"Error calling castVote on chain from proxy {proxy_account.address} for election {election_id}: {e}\n{traceback.format_exc()}")
        return None, f"Technical error casting vote on blockchain: {str(e)}"

def toggle_election_status_on_chain(election_id: int):
    contract = get_contract()
    if not contract: return None, "Blockchain service unavailable."
    if not DEFAULT_SIGNER_ACCOUNT: return None, "Backend default signer for admin actions not configured."
    try:
        nonce = w3.eth.get_transaction_count(DEFAULT_SIGNER_ACCOUNT.address)
        txn_params = {'from': DEFAULT_SIGNER_ACCOUNT.address, 'nonce': nonce}
        unsent_tx = contract.functions.toggleElectionStatus(election_id).build_transaction(txn_params)
        signed_tx = w3.eth.account.sign_transaction(unsent_tx, private_key=DEFAULT_SIGNER_ACCOUNT.key)
        tx_hash_hexbytes = w3.eth.send_raw_transaction(signed_tx.rawTransaction)
        tx_hash = HexBytes(tx_hash_hexbytes).hex()
        tx_receipt = w3.eth.wait_for_transaction_receipt(tx_hash_hexbytes, timeout=120)
        if tx_receipt.status == 1:
            return {"tx_hash": tx_hash, "status": "success", "receipt": dict(tx_receipt)}, None
        else: return None, f"Blockchain transaction failed with status {tx_receipt.status}."
    except ContractLogicError as e: return None, f"Smart contract error: {e.message if hasattr(e, 'message') else str(e)}"
    except TransactionNotFound: return None, f"Transaction with hash {tx_hash if 'tx_hash' in locals() else 'unknown'} not found."
    except Exception as e: print(f"Error toggleElectionStatus: {e}\n{traceback.format_exc()}"); return None, f"Technical error: {e}"
