from .app import db # Import db instance from app.py
from datetime import datetime

class User(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String(80), unique=True, nullable=False)
    email = db.Column(db.String(120), unique=True, nullable=False)
    password_hash = db.Column(db.String(128), nullable=False)
    biometric_public_key = db.Column(db.String(512), nullable=True)
    biometrics_enabled = db.Column(db.Boolean, default=False, nullable=False)

    # Relationship to Votes cast by the user
    votes = db.relationship('Vote', backref='voter', lazy=True)

    def __repr__(self):
        return f'<User {self.username}>'

class Poll(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    title = db.Column(db.String(200), nullable=False)
    description = db.Column(db.Text, nullable=True)
    # Options will be stored as a list of dictionaries, e.g.,
    # [{"id": 1, "text": "Option A"}, {"id": 2, "text": "Option B"}]
    options = db.Column(db.JSON, nullable=False)
    start_date = db.Column(db.DateTime, nullable=True, default=datetime.utcnow)
    end_date = db.Column(db.DateTime, nullable=True)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)

    # Relationship to Votes cast in this poll
    votes = db.relationship('Vote', backref='poll', lazy=True)

    def __repr__(self):
        return f'<Poll {self.title}>'

class Vote(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    user_id = db.Column(db.Integer, db.ForeignKey('user.id'), nullable=False)
    poll_id = db.Column(db.Integer, db.ForeignKey('poll.id'), nullable=False)
    # selected_option_id refers to the 'id' within the Poll.options JSON list
    selected_option_id = db.Column(db.Integer, nullable=False)
    timestamp = db.Column(db.DateTime, default=datetime.utcnow)

    # To prevent a user from voting multiple times in the same poll
    __table_args__ = (db.UniqueConstraint('user_id', 'poll_id', name='uq_user_poll_vote'),)

    def __repr__(self):
        return f'<Vote by User {self.user_id} in Poll {self.poll_id} for Option {self.selected_option_id}>'
