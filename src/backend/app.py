from flask import Flask, request, jsonify
from flask_sqlalchemy import SQLAlchemy
import os

app = Flask(__name__)
basedir = os.path.abspath(os.path.dirname(__file__))
app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///' + os.path.join(basedir, 'voting_app.db')
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
db = SQLAlchemy(app)

# Import models and auth functions AFTER db is initialized and app is created
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
        if "already exists" in result["message"] or "already registered" in result["message"]:
            return jsonify(result), 409 # Conflict
        else:
            return jsonify(result), 500 # Internal Server Error

@app.route('/users/<username>', methods=['GET'])
def get_user_route(username):
    user = auth_get_user_by_username(username)
    if user:
        return jsonify({
            "id": user.id,
            "username": user.username,
            "email": user.email,
            "biometric_template_id": user.biometric_template_id
        }), 200
    else:
        return jsonify({"success": False, "message": "User not found"}), 404

# CLI command to initialize the database
@app.cli.command('init-db')
def init_db_command():
    """Creates the database tables."""
    db.create_all()
    # print('Initialized the database.') # Optional: Add print for CLI feedback

if __name__ == '__main__':
    app.run(debug=True)
