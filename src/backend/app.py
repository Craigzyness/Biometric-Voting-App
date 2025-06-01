from flask import Flask, request, jsonify, session, current_app # Added current_app for logger
from flask_sqlalchemy import SQLAlchemy
import os
import uuid
from datetime import datetime, timedelta

# --- Cryptography Imports (Placeholder - ensure library is installed) ---
# These are typical imports from the 'cryptography' library for ECC P-256 (secp256r1)
# which is commonly used by device keychains for biometric-protected keys.
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.asymmetric import ec
# from cryptography.hazmat.primitives.serialization import load_pem_public_key, Encoding, PublicFormat # If key is PEM
# Updated import for SPKI DER format as per react-native-biometrics common output
from cryptography.hazmat.primitives.serialization import load_der_public_key
from cryptography.exceptions import InvalidSignature
import base64 # If signature or key is base64 encoded

app = Flask(__name__)
basedir = os.path.abspath(os.path.dirname(__file__))
app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///' + os.path.join(basedir, 'voting_app.db')
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
app.config['SECRET_KEY'] = os.environ.get('FLASK_SECRET_KEY', 'dev_secret_key_for_poc_!ChangeMe!')

db = SQLAlchemy(app)

pending_challenges = {} # Global dict for PoC.

from .models import User
from .auth import register_user as auth_register_user
from .auth import get_user_by_username as auth_get_user_by_username

@app.route('/')
def hello_world():
    return 'Hello, Backend World!'

@app.route('/register', methods=['POST'])
def register_route():
    data = request.get_json()
    if not data or not data.get('username') or not data.get('email') or not data.get('password'):
        return jsonify({"success": False, "message": "Missing username, email, or password"}), 400
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
    if user:
        return jsonify({
            "id": user.id, "username": user.username, "email": user.email,
            "biometric_public_key": user.biometric_public_key,
            "biometrics_enabled": user.biometrics_enabled
        }), 200
    else:
        return jsonify({"success": False, "message": "User not found"}), 404

@app.route('/users/<int:user_id>/biometrics', methods=['POST'])
def add_user_biometric_key(user_id):
    user = User.query.get(user_id)
    if not user: return jsonify({"success": False, "message": "User not found"}), 404
    data = request.get_json()
    if not data or not data.get('biometric_public_key'):
        return jsonify({"success": False, "message": "Missing biometric_public_key"}), 400
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
    username = request.args.get('username') # Expect username to associate challenge
    if not username:
        return jsonify({"success": False, "message": "Username query parameter is required"}), 400

    user = User.query.filter_by(username=username).first()
    if not user or not user.biometrics_enabled or not user.biometric_public_key:
        return jsonify({"success": False, "message": "User not found, or biometrics not enabled, or no public key registered."}), 404

    challenge = str(uuid.uuid4())
    expiry_time = datetime.utcnow() + timedelta(minutes=2)
    pending_challenges[challenge] = {"user_id": user.id, "expires_at": expiry_time}
    current_app.logger.info(f"Generated challenge: {challenge} for user_id: {user.id}")
    return jsonify({"success": True, "challenge": challenge}), 200

@app.route('/auth/biometric-login', methods=['POST'])
def biometric_login():
    data = request.get_json()
    if not data or not data.get('username') or not data.get('challenge') or not data.get('signature'):
        return jsonify({"success": False, "message": "Missing username, challenge, or signature"}), 400

    username = data.get('username')
    challenge = data.get('challenge')
    signature_b64 = data.get('signature') # Assuming signature is base64 encoded string from client

    # 1. Validate challenge
    challenge_data = pending_challenges.pop(challenge, None) # Remove challenge after use
    if not challenge_data:
        return jsonify({"success": False, "message": "Invalid or expired challenge (not found)"}), 400 # Potentially 401 or 403
    if datetime.utcnow() > challenge_data["expires_at"]:
        return jsonify({"success": False, "message": "Challenge expired"}), 400 # Potentially 401 or 403

    # 2. Get user and public key
    user = User.query.filter_by(username=username).first()
    if not user or not user.biometric_public_key:
        return jsonify({"success": False, "message": "User not found or no biometric public key registered"}), 404

    if user.id != challenge_data.get("user_id"):
        return jsonify({"success": False, "message": "Challenge not intended for this user"}), 400 # Potentially 401 or 403


    # 3. Cryptographic Signature Verification
    try:
        public_key_b64 = user.biometric_public_key
        public_key_bytes = base64.b64decode(public_key_b64)

        # Load the public key. Assuming SPKI DER format based on react-native-biometrics.
        public_key = load_der_public_key(public_key_bytes)

        signature_bytes = base64.b64decode(signature_b64)
        challenge_bytes = challenge.encode('utf-8')

        public_key.verify(
            signature_bytes,
            challenge_bytes,
            ec.ECDSA(hashes.SHA256())
        )
        current_app.logger.info(f"Biometric login successful for user: {username}")
        # TODO: Issue a session token / JWT for the user here
        return jsonify({"success": True, "message": "Biometric login successful"}), 200

    except InvalidSignature:
        current_app.logger.warning(f"Invalid signature for user: {username}")
        return jsonify({"success": False, "message": "Invalid signature"}), 401
    except Exception as e:
        current_app.logger.error(f"Error during signature verification for user {username}: {str(e)}")
        return jsonify({"success": False, "message": "Signature verification error"}), 500


@app.cli.command('init-db')
def init_db_command():
    db.create_all()
    print('Initialized the database.')

if __name__ == '__main__':
    app.run(debug=True)
