const playIntegrityVerifier = require('../play_integrity_verifier');
const logger = require('../logger');

// Mock googleapis
jest.mock('googleapis', () => ({
    google: {
        auth: {
            GoogleAuth: jest.fn().mockImplementation(() => ({})),
        },
        playintegrity: jest.fn().mockReturnValue({
            decodeIntegrityToken: jest.fn().mockResolvedValue({
                data: {
                    tokenPayloadExternal: {
                        requestDetails: { nonce: 'test-nonce' },
                        deviceIntegrity: { deviceRecognitionVerdict: ['MEETS_DEVICE_INTEGRITY'] },
                        appIntegrity: { appRecognitionVerdict: 'PLAY_RECOGNIZED' },
                        accountDetails: { appLicensingVerdict: 'LICENSED' }
                    }
                }
            })
        })
    }
}));

// Mock logger
jest.mock('../logger', () => ({
    info: jest.fn(),
    warn: jest.fn(),
    error: jest.fn(),
    debug: jest.fn(),
}));

describe('play_integrity_verifier', () => {
    it('should verify a valid token', async () => {
        const result = await playIntegrityVerifier.verifyToken('valid-token', 'test-nonce');
        expect(result.isValid).toBe(true);
        expect(result.error).toBeNull();
    });

    it('should fail if token is missing', async () => {
        const result = await playIntegrityVerifier.verifyToken(null, 'test-nonce');
        expect(result.isValid).toBe(false);
        expect(result.error).toBe('Integrity token is missing.');
    });
});
