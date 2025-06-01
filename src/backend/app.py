from flask import Flask, request, jsonify, session, current_app, Response
from flask_sqlalchemy import SQLAlchemy
from sqlalchemy.exc import IntegrityError
import os
import uuid
from datetime import datetime, timedelta
import traceback # For more detailed error logging if needed

from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.asymmetric import ec
from cryptography.hazmat.primitives.serialization import load_der_public_key
from cryptography.exceptions import InvalidSignature
import base64

from flask_admin import Admin, AdminIndexView, expose
from flask_admin.contrib.sqla import ModelView

from . import blockchain_service

ADMIN_USERNAME = os.environ.get('ADMIN_USERNAME', 'admin')
ADMIN_PASSWORD = os.environ.get('ADMIN_PASSWORD', 'secret')

def check_auth(username, password): return username == ADMIN_USERNAME and password == ADMIN_PASSWORD
def authenticate(): return Response('Login Required', 401, {'WWW-Authenticate': 'Basic realm="Login Required"'})

class SecureAdminIndexView(AdminIndexView):
    def is_accessible(self): auth = request.authorization; return auth and check_auth(auth.username, auth.password)
    def inaccessible_callback(self, name, **kwargs): return authenticate()

class SecureModelView(ModelView):
    def is_accessible(self): auth = request.authorization; return auth and check_auth(auth.username, auth.password)
    def inaccessible_callback(self, name, **kwargs): return authenticate()

app = Flask(__name__)
basedir = os.path.abspath(os.path.dirname(__file__))
app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///' + os.path.join(basedir, 'voting_app.db')
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
app.config['SECRET_KEY'] = os.environ.get('FLASK_SECRET_KEY', 'dev_secret_key_for_poc_!ChangeMe!')
app.config['FLASK_ADMIN_SWATCH'] = 'cerulean'

db = SQLAlchemy(app)
admin = Admin(app, name='BiometricVotingAdmin', template_mode='bootstrap3', index_view=SecureAdminIndexView())

with app.app_context():
    if not blockchain_service.init_blockchain_connection():
        app.logger.error("CRITICAL: Blockchain service failed to initialize.")
    else:
        app.logger.info("Blockchain service initialized successfully.")

pending_challenges = {}

from .models import User, Poll, Vote
from .auth import register_user as auth_register_user
from .auth import get_user_by_username as auth_get_user_by_username

class UserAdminView(SecureModelView):
    column_list = ('id', 'username', 'email', 'biometrics_enabled'); column_searchable_list = ('username', 'email'); column_filters = ('biometrics_enabled',); form_excluded_columns = ('password_hash', 'biometric_public_key', 'votes'); can_create = False; can_delete = False; can_edit = True
class PollAdminView(SecureModelView):
    column_list = ('id', 'title', 'start_date', 'end_date', 'created_at'); column_searchable_list = ('title',); column_filters = ('start_date', 'end_date'); form_columns = ('title', 'description', 'options', 'start_date', 'end_date'); can_create = True; can_edit = True; can_delete = True
class VoteAdminView(SecureModelView):
    column_list = ('id', 'voter.username', 'poll.title', 'selected_option_id', 'timestamp'); column_searchable_list = ('voter.username', 'poll.title'); column_filters = ('timestamp', 'poll_id'); can_create = False; can_edit = False; can_delete = False

admin.add_view(UserAdminView(User, db.session))
admin.add_view(PollAdminView(Poll, db.session))
admin.add_view(VoteAdminView(Vote, db.session))

# --- API Routes ---
@app.route('/')
def hello_world(): return 'Hello, Backend World!'
# ... (User, Auth, Biometric routes - condensed) ...
@app.route('/register', methods=['POST'])
def register_route():
    data = request.get_json();
    if not data or not data.get('username') or not data.get('email') or not data.get('password'): return jsonify({"success": False, "message": "Missing fields"}), 400
    username = data.get('username'); email = data.get('email'); password = data.get('password')
    result = auth_register_user(username, email, password)
    if result["success"]: return jsonify(result), 201
    else: return jsonify(result), 409 if "already exists" in result["message"] or "already registered" in result["message"] else 500
@app.route('/users/<username>', methods=['GET'])
def get_user_route(username):
    user = auth_get_user_by_username(username)
    if user: return jsonify({"id": user.id, "username": user.username, "email": user.email, "biometric_public_key": user.biometric_public_key, "biometrics_enabled": user.biometrics_enabled }), 200
    else: return jsonify({"success": False, "message": "User not found"}), 404
@app.route('/users/<int:user_id>/biometrics', methods=['POST'])
def add_user_biometric_key(user_id):
    user = User.query.get(user_id)
    if not user: return jsonify({"success": False, "message": "User not found"}), 404
    data = request.get_json();
    if not data or not data.get('biometric_public_key'): return jsonify({"success": False, "message": "Missing key"}), 400
    biometric_key = data.get('biometric_public_key')
    if not isinstance(biometric_key, str) or len(biometric_key) == 0: return jsonify({"success": False, "message": "Invalid key format"}), 400
    user.biometric_public_key = biometric_key; user.biometrics_enabled = True
    try: db.session.commit(); return jsonify({"success": True, "message": "Biometric key added."}), 200
    except Exception as e: db.session.rollback(); current_app.logger.error(f"DB error: {str(e)}"); return jsonify({"success": False, "message": "DB error"}), 500
@app.route('/auth/biometric-challenge', methods=['GET'])
def get_biometric_challenge():
    username = request.args.get('username')
    if not username: return jsonify({"success": False, "message": "Username required"}), 400
    user = User.query.filter_by(username=username).first()
    if not user or not user.biometrics_enabled or not user.biometric_public_key: return jsonify({"success": False, "message": "User/key/biometrics issue."}), 404
    challenge = str(uuid.uuid4())
    expiry_time = datetime.utcnow() + timedelta(minutes=2)
    pending_challenges[challenge] = {"user_id": user.id, "expires_at": expiry_time}
    current_app.logger.info(f"Challenge: {challenge} for user: {user.id}")
    return jsonify({"success": True, "challenge": challenge}), 200
@app.route('/auth/biometric-login', methods=['POST'])
def biometric_login():
    data = request.get_json()
    if not data or not data.get('username') or not data.get('challenge') or not data.get('signature'): return jsonify({"success": False, "message": "Missing fields"}), 400
    username = data.get('username'); challenge = data.get('challenge'); signature_b64 = data.get('signature')
    challenge_data = pending_challenges.pop(challenge, None)
    if not challenge_data: return jsonify({"success": False, "message": "Invalid/expired challenge"}), 400
    if datetime.utcnow() > challenge_data["expires_at"]: return jsonify({"success": False, "message": "Challenge expired"}), 400
    user = User.query.filter_by(username=username).first()
    if not user or not user.biometric_public_key: return jsonify({"success": False, "message": "User/key not found"}), 404
    if user.id != challenge_data.get("user_id"): return jsonify({"success": False, "message": "Challenge mismatch"}), 400
    try:
        public_key_bytes = base64.b64decode(user.biometric_public_key)
        public_key = load_der_public_key(public_key_bytes)
        signature_bytes = base64.b64decode(signature_b64)
        challenge_bytes = challenge.encode('utf-8')
        public_key.verify(signature_bytes, challenge_bytes, ec.ECDSA(hashes.SHA256()))
        current_app.logger.info(f"Biometric login successful: {username}")
        return jsonify({"success": True, "message": "Biometric login successful"}), 200 # TODO: Issue session token
    except InvalidSignature: current_app.logger.warning(f"Invalid signature: {username}"); return jsonify({"success": False, "message": "Invalid signature"}), 401
    except Exception as e: current_app.logger.error(f"Sig verification error {username}: {str(e)}"); return jsonify({"success": False, "message": "Sig verification error"}), 500

# --- Poll/Election APIs ---
@app.route('/polls', methods=['POST'])
def create_poll_on_blockchain():
    if not blockchain_service.is_connected(): return jsonify({"success": False, "message": "Blockchain service unavailable."}), 503
    data = request.get_json();
    if not data: return jsonify({"success": False, "message": "Request body must be JSON."}), 400
    title = data.get('title'); description = data.get('description', ''); options_data = data.get('options'); is_active = data.get('is_active', True)
    if not title or not isinstance(title, str): return jsonify({"success": False, "message": "Missing or invalid title."}), 400
    if not options_data or not isinstance(options_data, list) or not all(isinstance(opt, str) for opt in options_data) or len(options_data) < 2:
        return jsonify({"success": False, "message": "Invalid options: Must be list of at least two strings."}), 400
    if not isinstance(is_active, bool): return jsonify({"success": False, "message": "Invalid is_active flag."}), 400
    result, error = blockchain_service.create_election_on_chain(title=title, description=description, options=options_data, is_active=is_active)
    if error:
        current_app.logger.error(f"Error creating poll on blockchain: {error}")
        if "Smart contract error" in error or "Blockchain transaction failed" in error: return jsonify({"success": False, "message": "Blockchain contract/tx error."}), 500
        return jsonify({"success": False, "message": error}), 500
    return jsonify({"success": True, "message": "Poll creation tx sent.", "data": result}), 202

@app.route('/polls', methods=['GET'])
def get_blockchain_polls():
    if not blockchain_service.is_connected(): return jsonify({"success": False, "message": "Blockchain service unavailable."}), 503
    count, error = blockchain_service.get_elections_count_from_chain()
    if error: return jsonify({"success": False, "message": error}), 500
    if count is None: return jsonify({"success": False, "message": "Could not get election count."}), 500
    all_polls_data = []
    for i in range(1, count + 1):
        poll_data, error_detail = blockchain_service.get_election_details_from_chain(i)
        if poll_data: all_polls_data.append(poll_data)
        elif error_detail: current_app.logger.warning(f"Could not fetch details for election ID {i}: {error_detail}")
    return jsonify({"success": True, "polls": all_polls_data}), 200

@app.route('/polls/<int:poll_id>', methods=['GET'])
def get_blockchain_poll_detail(poll_id): # Renamed parameter to poll_id from election_id_on_chain for consistency with route
    if not blockchain_service.is_connected(): return jsonify({"success": False, "message": "Blockchain service unavailable."}), 503
    if poll_id <= 0: return jsonify({"success": False, "message": "Invalid poll ID."}), 400
    poll_data, error = blockchain_service.get_election_details_from_chain(poll_id)
    if error:
        if "Election does not exist" in error or "revert" in error.lower(): return jsonify({"success": False, "message": "Poll not found on blockchain."}), 404
        return jsonify({"success": False, "message": error}), 500
    if poll_data:
        vote_counts, vc_error = blockchain_service.get_all_vote_counts_for_election_from_chain(poll_id)
        if vc_error: current_app.logger.warning(f"Could not fetch vote counts for poll {poll_id}: {vc_error}"); poll_data['vote_counts'] = None
        else: poll_data['vote_counts'] = vote_counts
        return jsonify({"success": True, "poll": poll_data}), 200
    else: return jsonify({"success": False, "message": "Poll not found or error."}), 404

# --- Vote Casting API (Now to Blockchain) ---
@app.route('/polls/<int:election_id_on_chain>/vote', methods=['POST'])
def cast_vote_on_blockchain(election_id_on_chain):
    if not blockchain_service.is_connected():
        return jsonify({"success": False, "message": "Blockchain service not available."}), 503

    data = request.get_json()
    # selected_option_id from request is the 0-based index for the smart contract
    if not data or 'user_id' not in data or 'selected_option_id' not in data:
        return jsonify({"success": False, "message": "Missing user_id or selected_option_id (option index)"}), 400

    app_user_id = data.get('user_id')
    option_index = data.get('selected_option_id')

    user = User.query.get(app_user_id) # Application-level user
    if not user:
        return jsonify({"success": False, "message": "Application user not found."}), 404

    # Validate option_index is an integer (it's used directly with the contract)
    if not isinstance(option_index, int) or option_index < 0:
        return jsonify({"success": False, "message": "Invalid option_index: must be a non-negative integer."}), 400

    # Optional: Fetch election details to validate option_index against options length and check isActive
    # This is good practice but adds an extra read call before the write.
    # The smart contract itself also performs these checks.
    # For PoC, we can rely on smart contract checks for active status and option range.
    # However, it's good to log the attempt with validated data if possible.
    current_app.logger.info(f"Attempting blockchain vote: user {app_user_id}, election {election_id_on_chain}, option_idx {option_index}")

    # Application-level check for double voting (against SQL DB)
    existing_vote_sql = Vote.query.filter_by(user_id=app_user_id, poll_id=election_id_on_chain).first()
    if existing_vote_sql:
        return jsonify({"success": False, "message": "User has already voted in this poll (application record)."}), 409

    tx_info, error = blockchain_service.cast_vote_on_chain(
        election_id=election_id_on_chain,
        option_index=option_index
    )

    if error:
        current_app.logger.error(f"Error casting vote on blockchain: {error}")
        # Smart contract errors (reverts) are often in the error message from blockchain_service
        if "Smart contract error" in error:
             return jsonify({"success": False, "message": error}), 400 # e.g., already voted on chain, inactive poll
        return jsonify({"success": False, "message": "Failed to cast vote on blockchain."}), 500

    # If blockchain transaction was accepted (202), record in SQL DB.
    new_vote_sql = Vote(user_id=app_user_id, poll_id=election_id_on_chain, selected_option_id=option_index) # Storing index
    try:
        db.session.add(new_vote_sql)
        db.session.commit()
        current_app.logger.info(f"Blockchain vote by app_user_id {app_user_id} for election {election_id_on_chain} also recorded in SQL DB.")
    except IntegrityError:
        db.session.rollback()
        current_app.logger.warning(f"SQL IntegrityError after blockchain vote for user {app_user_id}, poll {election_id_on_chain}.")
        # Blockchain vote might have succeeded, but SQL failed (e.g. race condition if not for this check)
        # Return success from blockchain, but acknowledge potential inconsistency.
        return jsonify({"success": True, "message": "Vote sent to blockchain, but local record failed (Integrity).", "data": tx_info}), 207
    except Exception as e_sql:
        db.session.rollback()
        current_app.logger.error(f"SQL Error after successful blockchain vote: {str(e_sql)}. Vote for user {app_user_id}, poll {election_id_on_chain} NOT recorded in SQL.")
        return jsonify({"success": True, "message": "Vote sent to blockchain, but local record failed. Check logs.", "data": tx_info}), 207

    return jsonify({
        "success": True,
        "message": "Vote transaction sent to blockchain and recorded locally.",
        "data": tx_info
    }), 202

@app.cli.command('init-db')
def init_db_command(): db.create_all(); print('Initialized the database.')
if __name__ == '__main__': app.run(debug=True)
