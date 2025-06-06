from .models import User
from .app import db
import bcrypt

def register_user(username, email, password):
    if User.query.filter_by(username=username).first():
        return {"success": False, "message": "Username already exists."}
    if User.query.filter_by(email=email).first():
        return {"success": False, "message": "Email already registered."}

    hashed_password = bcrypt.hashpw(password.encode('utf-8'), bcrypt.gensalt())
    new_user = User(username=username, email=email, password_hash=hashed_password.decode('utf-8'))

    try:
        db.session.add(new_user)
        db.session.commit()
        return {"success": True, "message": "User registered successfully.", "user_id": new_user.id}
    except Exception as e:
        db.session.rollback()
        return {"success": False, "message": f"Database error: {str(e)}"}

def verify_user(username, password):
    user = User.query.filter_by(username=username).first()
    if user and bcrypt.checkpw(password.encode('utf-8'), user.password_hash.encode('utf-8')):
        return {"success": True, "user_id": user.id}
    return {"success": False, "message": "Invalid username or password."}

def get_user_by_username(username):
    return User.query.filter_by(username=username).first()

def get_user_by_id(user_id):
    return User.query.get(int(user_id))

def init_db():
    with db.app.app_context():
        db.create_all()
