# Security Policy

The security of our project is a top priority. We appreciate your efforts to responsibly disclose any security vulnerabilities you may find. This document outlines our security policy, including supported versions, how to report vulnerabilities, and our commitment to addressing them.

## Supported Versions

We are committed to providing security updates for the following versions:

- **Latest Release:** The most recent stable release of the project will receive security updates.
- **Previous Major Version:** Security updates may be provided for the previous major version for a limited time, as determined by the project maintainers.

Please ensure you are using a supported version to benefit from the latest security patches.

## Reporting a Vulnerability

If you discover a security vulnerability, please report it to us privately to prevent potential exploitation. Do **not** report security vulnerabilities through public GitHub issues.

**How to Report:**
- **Email:** Send an email to `security@craigzyness.com` with the subject line "Security Vulnerability Report".
- **Details:** In your report, please include:
    - A clear description of the vulnerability.
    - Steps to reproduce the vulnerability, including any specific configurations or conditions.
    - The potential impact of the vulnerability.
    - Any suggested mitigations or fixes, if known.

We will acknowledge receipt of your report within 48 hours and work with you to understand and address the issue. We kindly ask that you do not disclose the vulnerability publicly until we have had a chance to investigate and implement a fix.

## Security Best Practices

While we strive to maintain a secure codebase, users and contributors should also follow security best practices:

- **No Raw Biometric Data:** This project is not designed to handle or store raw biometric data. Ensure any biometric data is processed and secured according to industry best practices before interacting with this project.
- **Use HTTPS:** Always use HTTPS when interacting with any web-based components of this project to protect data in transit.
- **Dependency Audits:** Regularly audit project dependencies for known vulnerabilities. Update dependencies as needed.
- **Credential Review:** Ensure that no sensitive credentials (API keys, passwords, etc.) are hardcoded or committed to the repository. Use environment variables or secure credential management systems.

## Disclosure Policy

- **Private Disclosure:** We will investigate and address vulnerabilities reported privately.
- **Public Announcement:** Once a fix is developed and verified, we will release a security advisory or patch notes detailing the vulnerability and the steps taken to address it.
- **Credit:** We will credit reporters who responsibly disclose vulnerabilities, unless they prefer to remain anonymous.

We are dedicated to maintaining the security and integrity of our project. Thank you for your help in keeping it secure.
