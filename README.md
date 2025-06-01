# Biometric Voting App

A secure, privacy-respecting voting platform that leverages biometric authentication to ensure election integrity. Designed for extensibility, developer productivity, and robust code quality.

---

## Vision

Create a modern, user-friendly voting system that uses biometrics (fingerprint, facial recognition, etc.) to guarantee one person, one vote—securely and transparently.

---

## Key Features

- **Biometric Authentication:** Integrate device-level biometrics for voter verification.
- **One Voter, One Vote:** Prevent duplicate votes; ensure anonymity and auditability.
- **End-to-End Security:** Encryption, secure storage, and privacy by design.
- **Extensible:** Built for adding new features (e.g., multi-factor auth, real-time tally).
- **AI-Augmented Development:** Leverage AI coding partner for productivity and code quality.

---

## Recommended Stack

- **Frontend:** React (Web) or React Native (Mobile)
- **Backend:** Python (FastAPI) or Node.js (Express)
- **Biometric Integration:** Platform-specific APIs (WebAuthn, mobile SDKs)
- **Database:** PostgreSQL
- **Testing:** Pytest/Jest, CI with GitHub Actions

---

## Quick Start

1. **Clone the repository:**
   ```sh
   git clone https://github.com/Craigzyness/biometric-voting-app.git
   cd biometric-voting-app
   ```
2. **Choose your stack** (see recommendations above)
3. **Install dependencies** for chosen backend/frontend.
4. **Run the app:**
   - Backend: `python main.py` or `npm run dev`
   - Frontend: `npm start` or `expo start`

---

## Directory Structure

```
biometric-voting-app/
│
├── backend/                # API, business logic, DB models
│   └── tests/              # Backend tests (e.g., Jest, Pytest)
├── app/                    # Android mobile client
│   ├── src/main/           # Main application source code
│   ├── src/test/           # Unit tests
│   └── src/androidTest/    # Instrumented tests
├── docs/                   # Documentation, architecture, diagrams
├── .gitignore
├── README.md
├── PROJECT_REMITS.md
├── CONTRIBUTING.md
├── SECURITY.md
├── LICENSE
└── CODE_OF_CONDUCT.md
```

---

## AI Coding Partner

This project is AI-augmented. See [PROJECT_REMITS.md](PROJECT_REMITS.md) for goals and remit of the AI assistant.

---

## Considerations & Best Practices

- **Security:** Always use encrypted channels and secure credential storage.
- **Privacy:** Never store raw biometric data; use only hashes or device tokens.
- **Testing:** Prioritize automated testing for all critical paths.
- **Accessibility:** Ensure usability for all potential voters.
- **Transparency:** Document design and decisions in `docs/`.

---

## Next Steps

- Decide on your stack and scaffold the first components.
- Implement minimum viable product (MVP): registration, biometric login, vote casting, results view.
- Expand iteratively, guided by test coverage and code reviews.

---

## License

See [LICENSE](LICENSE) for details.
