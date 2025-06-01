import React, { useState } from 'react';
import { View, Text, TextInput, Button, StyleSheet, Alert, Platform, ActivityIndicator } from 'react-native';
import ReactNativeBiometrics from 'react-native-biometrics';

const API_URL = Platform.OS === 'ios' ? 'http://localhost:5000' : 'http://10.0.2.2:5000';

const HomeScreen = ({ navigation }) => {
  const [loading, setLoading] = useState(false);
  const [usernameForBiometricLogin, setUsernameForBiometricLogin] = useState('');

  // MOCK_USER_ID is used for *setting up* biometrics (associating a key with a user ID).
  // For *logging in* with biometrics, we usually identify the user first (e.g., by username),
  // then the server knows which public key to use for verification.
  const MOCK_USER_ID_FOR_SETUP = 1; // Used when calling /users/<id>/biometrics

  const rnBiometrics = new ReactNativeBiometrics({ allowDeviceCredentials: true });

  // --- Biometric Key Setup Functions (from previous steps) ---
  const handleSetupBiometrics = async () => {
    if (!usernameForBiometricLogin.trim()) {
      Alert.alert("Username Required", "Please enter your username before setting up biometrics.");
      return;
    }
    // In a real app, you'd use the actual logged-in user's ID or a token to identify them
    // For PoC, we'll use MOCK_USER_ID_FOR_SETUP when sending the key.
    // But the username entered could be used to display "Setting up for user X"

    Alert.alert(
      "Setup Biometrics",
      `This will attempt to create a new biometric key pair for username: ${usernameForBiometricLogin}. This key will be protected by your device biometrics. Proceed?`,
      [
        { text: "Cancel", style: "cancel" },
        { text: "Proceed", onPress: () => performBiometricSetup(MOCK_USER_ID_FOR_SETUP) } // Pass the ID for key association
      ]
    );
  };

  const performBiometricSetup = async (userIdToAssociateKeyWith) => {
    setLoading(true);
    try {
      const { available, biometryType } = await rnBiometrics.isSensorAvailable();
      if (!available) {
        Alert.alert("Biometrics Not Available", `Biometric hardware not available. Type: ${biometryType || 'N/A'}`);
        // setLoading(false); // Handled in finally
        return;
      }
      Alert.alert("Biometric Sensor", `Sensor available. Type: ${biometryType || 'Biometrics'}`);
      const { publicKey } = await rnBiometrics.createKeys();
      if (!publicKey) {
        Alert.alert("Key Generation Failed", "Could not generate keys. Ensure biometrics enrolled.");
        // setLoading(false); // Handled in finally
        return;
      }
      Alert.alert("Biometric Key Generated", `Public Key: ${publicKey.substring(0,30)}...`);
      await sendPublicKeyToBackend(publicKey, userIdToAssociateKeyWith);
    } catch (error) {
      console.error('Biometric setup error:', error);
      Alert.alert("Biometric Setup Error", error.message || "An error occurred.");
    } finally {
      setLoading(false);
    }
  };

  const sendPublicKeyToBackend = async (publicKey, userId) => {
    // setLoading(true); // setLoading is managed by caller (performBiometricSetup)
    try {
      const response = await fetch(`${API_URL}/users/${userId}/biometrics`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ biometric_public_key: publicKey }),
      });
      const responseData = await response.json();
      if (response.ok) {
        Alert.alert("Biometric Setup Successful", responseData.message || "Key registered.");
      } else {
        Alert.alert("Backend Error (Key Setup)", responseData.message || `Status: ${response.status}`);
      }
    } catch (error) {
      console.error("Send public key API error:", error);
      Alert.alert("Network Error (Key Setup)", "Unable to send key to server.");
    }
    // finally { // setLoading is managed by caller (performBiometricSetup) }
  };

  // --- Biometric Login Functions ---
  const handleBiometricLogin = async () => {
    if (!usernameForBiometricLogin.trim()) {
      Alert.alert("Username Required", "Please enter the username to login with biometrics.");
      return;
    }
    setLoading(true);
    try {
      // 1. Get challenge from backend
      const challengeResponse = await fetch(`${API_URL}/auth/biometric-challenge?username=${usernameForBiometricLogin}`);
      const challengeData = await challengeResponse.json();

      if (!challengeResponse.ok || !challengeData.success) {
        Alert.alert("Challenge Error", challengeData.message || "Could not get challenge from server.");
        // setLoading(false); // Handled in finally
        return;
      }
      const { challenge } = challengeData;
      Alert.alert("Challenge Received", "Please confirm with biometrics to sign the challenge.");

      // 2. Create signature using biometrics
      const { success: sigSuccess, signature, error: sigError } = await rnBiometrics.createSignature({
        promptMessage: 'Sign in to Biometric Voting App',
        payload: challenge
      });

      if (!sigSuccess) {
        Alert.alert("Signature Failed", (sigError && sigError.message) || sigError || "Could not create signature. Biometrics might have failed or been cancelled.");
        // setLoading(false); // Handled in finally
        return;
      }

      Alert.alert("Signature Created", "Signature created successfully. Verifying with server...");

      // 3. Send challenge and signature to backend for verification
      const loginResponse = await fetch(`${API_URL}/auth/biometric-login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username: usernameForBiometricLogin,
          challenge: challenge,
          signature: signature
        }),
      });
      const loginData = await loginResponse.json();

      if (loginResponse.ok && loginData.success) {
        Alert.alert("Biometric Login Successful", loginData.message || "Successfully logged in!");
        // TODO: Handle successful login (e.g., store token, navigate to main app area)
      } else {
        Alert.alert("Biometric Login Failed", loginData.message || "Server verification failed.");
      }
    } catch (error) {
      console.error('Biometric login process error:', error);
      Alert.alert("Login Process Error", error.message || "An unexpected error occurred during login.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Home Screen</Text>
      <TextInput
        style={styles.input}
        placeholder="Enter Username (for Biometric Setup/Login)"
        value={usernameForBiometricLogin}
        onChangeText={setUsernameForBiometricLogin}
        autoCapitalize="none"
        editable={!loading}
      />
      <View style={styles.buttonContainer}>
        <Button
          title="Go to Register (New User)"
          onPress={() => navigation.navigate('Register')}
          disabled={loading}
        />
      </View>
      <View style={styles.buttonContainer}>
        <Button
            title="Setup Biometrics for Entered Username"
            onPress={handleSetupBiometrics}
            disabled={loading}
        />
      </View>
      <View style={styles.buttonContainer}>
        <Button
            title="Login with Biometrics for Entered Username"
            onPress={handleBiometricLogin}
            disabled={loading}
        />
      </View>
      {loading && <ActivityIndicator style={styles.loader} size="large" color="#0000ff" />}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 16,
  },
  title: {
    fontSize: 24,
    marginBottom: 20,
  },
  input: {
    width: '80%',
    height: 44,
    borderColor: 'gray',
    borderWidth: 1,
    borderRadius: 5,
    paddingHorizontal: 10,
    marginBottom: 20,
  },
  buttonContainer: {
    marginVertical: 8,
    width: '80%',
  },
  loader: {
    marginTop: 20,
  }
});

export default HomeScreen;
