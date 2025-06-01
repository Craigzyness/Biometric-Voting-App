from .app import db # Import db instance from app.py

class User(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String(80), unique=True, nullable=False)
    email = db.Column(db.String(120), unique=True, nullable=False)
    password_hash = db.Column(db.String(128), nullable=False)
    # Renamed from biometric_template_id to biometric_public_key for clarity
    # Increased length for public key storage
    biometric_public_key = db.Column(db.String(512), nullable=True)
    # We can also add a flag to indicate if biometrics are enabled by the user
    biometrics_enabled = db.Column(db.Boolean, default=False, nullable=False)


    def __repr__(self):
        return f'<User {self.username}>'
