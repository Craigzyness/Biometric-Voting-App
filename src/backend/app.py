from flask import Flask, request, jsonify, session, current_app
from flask_sqlalchemy import SQLAlchemy
from sqlalchemy.exc import IntegrityError # To catch unique constraint violations
import os
import uuid
from datetime import datetime, timedelta

from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.asymmetric import ec
from cryptography.hazmat.primitives.serialization import load_der_public_key
from cryptography.exceptions import InvalidSignature
import base64

app = Flask(__name__)
basedir = os.path.abspath(os.path.dirname(__file__))
app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///' + os.path.join(basedir, 'voting_app.db')
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
app.config['SECRET_KEY'] = os.environ.get('FLASK_SECRET_KEY', 'dev_secret_key_for_poc_!ChangeMe!')

db = SQLAlchemy(app)

pending_challenges = {}

from .models import User, Poll, Vote
from .auth import register_user as auth_register_user
from .auth import get_user_by_username as auth_get_user_by_username

def serialize_poll(poll):
    return {
        "id": poll.id, "title": poll.title, "description": poll.description,
        "options": poll.options,
        "start_date": poll.start_date.isoformat() if poll.start_date else None,
        "end_date": poll.end_date.isoformat() if poll.end_date else None,
        "created_at": poll.created_at.isoformat() if poll.created_at else None
    }

# --- User and Auth Routes (existing) ---
@app.route('/')
def hello_world(): return 'Hello, Backend World!'

@app.route('/register', methods=['POST'])
def register_route():
    data = request.get_json()
    if not data or not data.get('username') or not data.get('email') or not data.get('password'):
        return jsonify({"success": False, "message": "Missing fields"}), 400
    username = data.get('username')
    email = data.get('email')
    password = data.get('password')
    result = auth_register_user(username, email, password)
    if result["success"]:
        return jsonify(result), 201
    else:
        return jsonify(result), 409 if "already exists" in result["message"] or "already registered" in result["message"] else 500

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
    if not isinstance(biometric_key, str) or len(biometric_key) == 0:
        return jsonify({"success": False, "message": "Invalid biometric_public_key format"}), 400
    user.biometric_public_key = biometric_key
    user.biometrics_enabled = True
    try:
        db.session.commit()
        return jsonify({"success": True, "message": "Biometric key added."}), 200
    except Exception as e:
        db.session.rollback()
        current_app.logger.error(f"DB error adding biometric key: {str(e)}")
        return jsonify({"success": False, "message": "DB error"}), 500


@app.route('/auth/biometric-challenge', methods=['GET'])
def get_biometric_challenge():
    username = request.args.get('username')
    if not username: return jsonify({"success": False, "message": "Username required"}), 400
    user = User.query.filter_by(username=username).first()
    if not user or not user.biometrics_enabled or not user.biometric_public_key:
        return jsonify({"success": False, "message": "User not found, biometrics not enabled, or no public key."}), 404
    challenge = str(uuid.uuid4())
    expiry_time = datetime.utcnow() + timedelta(minutes=2)
    pending_challenges[challenge] = {"user_id": user.id, "expires_at": expiry_time}
    current_app.logger.info(f"Generated challenge: {challenge} for user_id: {user.id}")
    return jsonify({"success": True, "challenge": challenge}), 200

@app.route('/auth/biometric-login', methods=['POST'])
def biometric_login():
    data = request.get_json()
    if not data or not data.get('username') or not data.get('challenge') or not data.get('signature'):
        return jsonify({"success": False, "message": "Missing fields"}), 400
    username = data.get('username')
    challenge = data.get('challenge')
    signature_b64 = data.get('signature')
    challenge_data = pending_challenges.pop(challenge, None)
    if not challenge_data: return jsonify({"success": False, "message": "Invalid/expired challenge (not found)"}), 400
    if datetime.utcnow() > challenge_data["expires_at"]: return jsonify({"success": False, "message": "Challenge expired"}), 400
    user = User.query.filter_by(username=username).first()
    if not user or not user.biometric_public_key: return jsonify({"success": False, "message": "User or key not found"}), 404
    if user.id != challenge_data.get("user_id"): return jsonify({"success": False, "message": "Challenge mismatch"}), 400
    try:
        public_key_bytes = base64.b64decode(user.biometric_public_key)
        public_key = load_der_public_key(public_key_bytes)
        signature_bytes = base64.b64decode(signature_b64)
        challenge_bytes = challenge.encode('utf-8')
        public_key.verify(signature_bytes, challenge_bytes, ec.ECDSA(hashes.SHA256()))
        current_app.logger.info(f"Biometric login successful for user: {username}")
        return jsonify({"success": True, "message": "Biometric login successful"}), 200 # TODO: Issue session token
    except InvalidSignature:
        current_app.logger.warning(f"Invalid signature for user: {username}")
        return jsonify({"success": False, "message": "Invalid signature"}), 401
    except Exception as e:
        current_app.logger.error(f"Signature verification error for {username}: {str(e)}")
        return jsonify({"success": False, "message": "Signature verification error"}), 500

# --- Poll Management APIs (existing) ---
@app.route('/polls', methods=['POST'])
def create_poll():
    data = request.get_json()
    if not data or not data.get('title') or not data.get('options'):
        return jsonify({"success": False, "message": "Missing title or options"}), 400
    title = data.get('title')
    description = data.get('description', '')
    options = data.get('options')
    if not isinstance(options, list) or not all(isinstance(opt, dict) and 'id' in opt and 'text' in opt for opt in options):
        return jsonify({"success": False, "message": "Invalid options format."}), 400
    start_date_str = data.get('start_date')
    end_date_str = data.get('end_date')
    start_date = datetime.fromisoformat(start_date_str) if start_date_str else datetime.utcnow()
    end_date = datetime.fromisoformat(end_date_str) if end_date_str else None
    new_poll = Poll(title=title, description=description, options=options, start_date=start_date, end_date=end_date)
    try:
        db.session.add(new_poll)
        db.session.commit()
        return jsonify({"success": True, "message": "Poll created", "poll": serialize_poll(new_poll)}), 201
    except Exception as e:
        db.session.rollback(); current_app.logger.error(f"Error creating poll: {str(e)}")
        return jsonify({"success": False, "message": "Error creating poll"}), 500

@app.route('/polls', methods=['GET'])
def get_polls():
    try: polls = Poll.query.order_by(Poll.created_at.desc()).all()
    except Exception as e: # More specific exceptions could be caught
        current_app.logger.error(f"Error fetching polls: {str(e)}")
        return jsonify({"success": False, "message": "Error fetching polls"}), 500
    return jsonify({"success": True, "polls": [serialize_poll(p) for p in polls]}), 200

@app.route('/polls/<int:poll_id>', methods=['GET'])
def get_poll_detail(poll_id):
    try: poll = Poll.query.get(poll_id)
    except Exception as e: # More specific exceptions could be caught
        current_app.logger.error(f"Error fetching poll {poll_id}: {str(e)}")
        return jsonify({"success": False, "message": "Error fetching poll details"}), 500
    if not poll: return jsonify({"success": False, "message": "Poll not found"}), 404
    return jsonify({"success": True, "poll": serialize_poll(poll)}), 200

# --- Vote Casting API ---
@app.route('/polls/<int:poll_id>/vote', methods=['POST'])
def cast_vote(poll_id):
    data = request.get_json()
    if not data or 'user_id' not in data or 'selected_option_id' not in data:
        return jsonify({"success": False, "message": "Missing user_id or selected_option_id"}), 400

    user_id = data.get('user_id')
    selected_option_id = data.get('selected_option_id')

    # Validate user
    user = User.query.get(user_id)
    if not user:
        return jsonify({"success": False, "message": "User not found"}), 404

    # Validate poll
    poll = Poll.query.get(poll_id)
    if not poll:
        return jsonify({"success": False, "message": "Poll not found"}), 404

    # Check if poll is active
    now = datetime.utcnow()
    if poll.start_date and now < poll.start_date:
        return jsonify({"success": False, "message": "Poll has not started yet"}), 403
    if poll.end_date and now > poll.end_date:
        return jsonify({"success": False, "message": "Poll has ended"}), 403

    # Validate selected_option_id (ensure it's one of the poll's options)
    valid_option_ids = [opt['id'] for opt in poll.options]
    if selected_option_id not in valid_option_ids:
        return jsonify({"success": False, "message": "Invalid option selected"}), 400

    # Create and save the vote
    new_vote = Vote(user_id=user_id, poll_id=poll_id, selected_option_id=selected_option_id)
    try:
        db.session.add(new_vote)
        db.session.commit()
        return jsonify({"success": True, "message": "Vote cast successfully"}), 201
    except IntegrityError: # Catches the UniqueConstraint violation
        db.session.rollback()
        return jsonify({"success": False, "message": "User has already voted in this poll"}), 409
    except Exception as e:
        db.session.rollback()
        current_app.logger.error(f"Error casting vote for user {user_id} in poll {poll_id}: {str(e)}")
        return jsonify({"success": False, "message": "Error casting vote"}), 500

# --- CLI ---
@app.cli.command('init-db')
def init_db_command():
    db.create_all()
    print('Initialized the database.')

if __name__ == '__main__':
    app.run(debug=True)
