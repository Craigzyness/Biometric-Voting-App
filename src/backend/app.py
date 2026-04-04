from flask import Flask, request, jsonify, session, current_app, Response, flash, redirect, url_for # Added flash, redirect, url_for
from flask_sqlalchemy import SQLAlchemy
from sqlalchemy.exc import IntegrityError
import os
import uuid
from datetime import datetime, timedelta
import traceback

from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.asymmetric import ec
from cryptography.hazmat.primitives.serialization import load_der_public_key
from cryptography.exceptions import InvalidSignature
import base64

from flask_admin import Admin, AdminIndexView, expose, BaseView # Added BaseView
from flask_admin.contrib.sqla import ModelView
# from flask_admin.form import BaseForm # Not directly used in custom view's simple form
# from wtforms import StringField, TextAreaField, BooleanField, FieldList, FormField # Not directly used

from . import blockchain_service

ADMIN_USERNAME = os.environ.get('ADMIN_USERNAME')
ADMIN_PASSWORD = os.environ.get('ADMIN_PASSWORD')
FLASK_SECRET_KEY = os.environ.get('FLASK_SECRET_KEY')

if not ADMIN_USERNAME or not ADMIN_PASSWORD or not FLASK_SECRET_KEY:
    raise RuntimeError("ADMIN_USERNAME, ADMIN_PASSWORD, and FLASK_SECRET_KEY environment variables must be set.")

def check_auth(username, password): return username == ADMIN_USERNAME and password == ADMIN_PASSWORD
def authenticate(): return Response('Login Required', 401, {'WWW-Authenticate': 'Basic realm="Login Required"'})

class SecureViewMixin:
    def is_accessible(self):
        auth = request.authorization
        return auth and check_auth(auth.username, auth.password)
    def inaccessible_callback(self, name, **kwargs):
        return authenticate()

class SecureAdminIndexView(SecureViewMixin, AdminIndexView):
    pass

class SecureModelView(SecureViewMixin, ModelView):
    pass

# --- Custom Admin View for Blockchain Elections ---
class BlockchainPollAdminView(SecureViewMixin, BaseView):
    @expose('/')
    def index(self):
        elections_data = []; error_message = None
        if blockchain_service.is_connected():
            count, error = blockchain_service.get_elections_count_from_chain()
            if error: error_message = f"Error fetching election count: {error}"
            elif count is not None:
                for i in range(1, count + 1):
                    poll_detail, detail_error = blockchain_service.get_election_details_from_chain(i)
                    if poll_detail:
                        vote_counts, vc_error = blockchain_service.get_all_vote_counts_for_election_from_chain(i)
                        if vc_error: poll_detail['vote_counts_str'] = "Error fetching"
                        else: poll_detail['vote_counts_str'] = ", ".join(map(str, vote_counts or []))
                        elections_data.append(poll_detail)
                    elif detail_error: current_app.logger.warning(f"Admin: Could not fetch details for election ID {i}: {detail_error}")
        else: error_message = "Blockchain service not available."
        if error_message: flash(error_message, 'error')
        return self.render('admin/blockchain_poll_list.html', elections=elections_data)

    @expose('/create', methods=('GET', 'POST'))
    def create_view(self):
        if request.method == 'POST':
            title = request.form.get('title'); description = request.form.get('description', '')
            options_str = request.form.get('options', ''); is_active = request.form.get('is_active') == 'on'
            options = [opt.strip() for opt in options_str.split(',') if opt.strip()]
            if not title or len(options) < 2:
                flash('Title and at least two comma-separated options are required.', 'error')
            else:
                result, error = blockchain_service.create_election_on_chain(title, description, options, is_active)
                if error: flash(f'Error creating election on blockchain: {error}', 'error')
                else:
                    tx_hash = result.get("tx_hash", "N/A")
                    new_id_msg = f", New ID (from event): {result.get('election_id')}" if result.get('election_id') is not None else "" # Handle None for ID
                    flash(f'Election creation transaction sent! TxHash: {tx_hash}{new_id_msg}', 'success')
                    return redirect(url_for('.index'))
        return self.render('admin/blockchain_poll_create.html')

    @expose('/toggle_status/<int:election_id>', methods=('POST',))
    def toggle_status_view(self, election_id):
        if not blockchain_service.is_connected():
            flash("Blockchain service not available.", 'error')
            return redirect(url_for('.index'))

        current_app.logger.info(f"Admin attempting to toggle status for election ID: {election_id}")
        result, error = blockchain_service.toggle_election_status_on_chain(election_id)

        if error:
            flash(f"Error toggling status for election {election_id}: {error}", 'error')
            current_app.logger.error(f"Error toggling status for election {election_id} on blockchain: {error}")
        else:
            tx_hash = result.get("tx_hash", "N/A")
            flash(f"Toggle status transaction sent for election {election_id}! TxHash: {tx_hash}. Status may take a moment to reflect.", 'success')
            current_app.logger.info(f"Toggle status transaction for election {election_id} sent. TxHash: {tx_hash}")

        return redirect(url_for('.index'))

    def is_visible(self): return True

# --- App Setup ---
app = Flask(__name__)
basedir = os.path.abspath(os.path.dirname(__file__))
app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///' + os.path.join(basedir, 'voting_app.db')
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
app.config['SECRET_KEY'] = FLASK_SECRET_KEY
app.config['FLASK_ADMIN_SWATCH'] = 'cerulean'

db = SQLAlchemy(app)
admin = Admin(name='BiometricVotingAdmin', template_mode='bootstrap3', index_view=SecureAdminIndexView(url='/admin'))
admin.init_app(app)

with app.app_context():
    if not blockchain_service.init_blockchain_connection():
        app.logger.error("CRITICAL: Blockchain service failed to initialize.")
    else:
        app.logger.info("Blockchain service initialized successfully.")

pending_challenges = {}
from .models import User, Poll, Vote
from .auth import register_user as auth_register_user
from .auth import get_user_by_username as auth_get_user_by_username

class UserAdminView(SecureModelView): # ... (condensed)
    column_list = ('id', 'username', 'email', 'biometrics_enabled')
    column_searchable_list = ('username', 'email')
    column_filters = ('biometrics_enabled',)
    form_excluded_columns = ('password_hash', 'biometric_public_key', 'votes')
    can_create = False
    can_delete = False
    can_edit = True

class SQLPollAdminView(SecureModelView): # ... (condensed)
    column_list = ('id', 'title', 'start_date', 'end_date', 'created_at')
    column_searchable_list = ('title',)
    column_filters = ('start_date', 'end_date')
    form_columns = ('title', 'description', 'options', 'start_date', 'end_date')
    can_create = True
    can_edit = True
    can_delete = True

    def __init__(self, session, **kwargs):
        super(SQLPollAdminView, self).__init__(Poll, session, name="SQL Polls", **kwargs)

class VoteAdminView(SecureModelView): # ... (condensed)
    column_list = ('id', 'voter.username', 'poll.title', 'selected_option_id', 'timestamp')
    column_searchable_list = ('voter.username', 'poll.title')
    column_filters = ('timestamp', 'poll_id')
    can_create = False
    can_edit = False
    can_delete = False # Corrected poll.title filter to poll_id

admin.add_view(UserAdminView(User, db.session))
admin.add_view(SQLPollAdminView(db.session))
admin.add_view(BlockchainPollAdminView(name="Blockchain Elections", endpoint="blockchain-elections"))
admin.add_view(VoteAdminView(Vote, db.session))

# --- API Routes (condensed) ---
@app.route('/')
def hello_world(): return 'Hello, Backend World!'
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
def get_blockchain_poll_detail(poll_id):
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
@app.route('/polls/<int:election_id_on_chain>/vote', methods=['POST'])
def cast_vote_on_blockchain(election_id_on_chain):
    if not blockchain_service.is_connected(): return jsonify({"success": False, "message": "Blockchain service unavailable."}), 503
    data = request.get_json()
    if not data or 'user_id' not in data or 'selected_option_id' not in data: return jsonify({"success": False, "message": "Missing user_id or selected_option_id (option index)"}), 400
    app_user_id = data.get('user_id'); option_index_from_request = data.get('selected_option_id')
    user = User.query.get(app_user_id)
    if not user: return jsonify({"success": False, "message": "Application user not found."}), 404
    election_on_chain, error = blockchain_service.get_election_details_from_chain(election_id_on_chain)
    if error: return jsonify({"success": False, "message": f"Blockchain poll details error: {error}"}), 404
    if not election_on_chain: return jsonify({"success": False, "message": "Election not found on blockchain."}), 404
    if not election_on_chain['isActive']: return jsonify({"success": False, "message": "Election not active on blockchain."}), 403
    if not isinstance(option_index_from_request, int) or option_index_from_request < 0 or option_index_from_request >= len(election_on_chain['options']):
        return jsonify({"success": False, "message": "Invalid option index."}), 400
    existing_vote_sql = Vote.query.filter_by(user_id=app_user_id, poll_id=election_id_on_chain).first()
    if existing_vote_sql: return jsonify({"success": False, "message": "Already voted (application record)."}), 409
    current_app.logger.info(f"Attempting blockchain vote: app_user {app_user_id}, election {election_id_on_chain}, option_idx {option_index_from_request}")
    tx_info, error = blockchain_service.cast_vote_on_chain(election_id=election_id_on_chain, option_index=option_index_from_request)
    if error:
        current_app.logger.error(f"Error casting vote on blockchain: {error}")
        if "Smart contract error" in error: return jsonify({"success": False, "message": f"Blockchain voting error: {error}"}), 400
        return jsonify({"success": False, "message": error}), 500
    new_vote_sql = Vote(user_id=app_user_id, poll_id=election_id_on_chain, selected_option_id=option_index_from_request)
    try:
        db.session.add(new_vote_sql); db.session.commit()
        current_app.logger.info(f"Vote by app_user {app_user_id} for election {election_id_on_chain} also recorded in SQL.")
    except IntegrityError: db.session.rollback(); current_app.logger.warning(f"IntegrityError for SQL vote record user {app_user_id}, poll {election_id_on_chain}.")
    except Exception as e_sql:
        db.session.rollback(); current_app.logger.error(f"SQL Error post-blockchain vote: {str(e_sql)}. User {app_user_id}, poll {election_id_on_chain} NOT in SQL.")
        return jsonify({"success": True, "message": "Vote tx sent, local record failed. Check logs.", "data": tx_info}), 207
    return jsonify({"success": True, "message": "Vote tx sent and recorded locally.", "data": tx_info}), 202

@app.cli.command('init-db')
def init_db_command(): db.create_all(); print('Initialized the database.')
if __name__ == '__main__': app.run(debug=True)
