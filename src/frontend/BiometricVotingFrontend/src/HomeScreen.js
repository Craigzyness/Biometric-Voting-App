import React, { useState } from 'react';
import { View, Text, TextInput, Button, StyleSheet, Alert, Platform, ActivityIndicator } from 'react-native';
import ReactNativeBiometrics from 'react-native-biometrics';

const API_URL = Platform.OS === 'ios' ? 'http://localhost:5000' : 'http://10.0.2.2:5000';

const HomeScreen = ({ navigation }) => { // Ensure navigation prop is received
  const [loading, setLoading] = useState(false);
  const [usernameForBiometricLogin, setUsernameForBiometricLogin] = useState('');
  const MOCK_USER_ID_FOR_SETUP = 1;

  const rnBiometrics = new ReactNativeBiometrics({ allowDeviceCredentials: true });

  const handleSetupBiometrics = async () => {
    if (!usernameForBiometricLogin.trim()) {
      Alert.alert("Username Required", "Please enter your username before setting up biometrics.");
      return;
    }
    Alert.alert(
      "Setup Biometrics",
      `This will create a biometric key for username: ${usernameForBiometricLogin}. Proceed?`,
      [
        { text: "Cancel", style: "cancel" },
        { text: "Proceed", onPress: () => performBiometricSetup(MOCK_USER_ID_FOR_SETUP) }
      ]
    );
  };

  const performBiometricSetup = async (userIdToAssociateKeyWith) => {
    setLoading(true);
    try {
      const { available, biometryType } = await rnBiometrics.isSensorAvailable();
      if (!available) {
        Alert.alert("Biometrics Not Available", `Hardware: ${biometryType || 'N/A'}`);
        return; // setLoading(false) will be called in finally
      }
      Alert.alert("Biometric Sensor", `Type: ${biometryType || 'Biometrics'}`);
      const { publicKey } = await rnBiometrics.createKeys();
      if (!publicKey) {
        Alert.alert("Key Generation Failed", "Ensure biometrics enrolled.");
        return; // setLoading(false) will be called in finally
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
    // setLoading(true); // Loading is managed by the caller (performBiometricSetup)
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
    // finally { setLoading(false); } // Loading is managed by the caller
  };

  const handleBiometricLogin = async () => {
    if (!usernameForBiometricLogin.trim()) {
      Alert.alert("Username Required", "Enter username for biometric login.");
      return;
    }
    setLoading(true);
    try {
      const challengeResponse = await fetch(`${API_URL}/auth/biometric-challenge?username=${usernameForBiometricLogin}`);
      const challengeData = await challengeResponse.json();
      if (!challengeResponse.ok || !challengeData.success) {
        Alert.alert("Challenge Error", challengeData.message || "Could not get challenge.");
        return; // setLoading(false) will be called in finally
      }
      const { challenge } = challengeData;
      Alert.alert("Challenge Received", "Confirm with biometrics to sign.");
      const { success: sigSuccess, signature, error: sigError } = await rnBiometrics.createSignature({
        promptMessage: 'Sign in to Biometric Voting App', payload: challenge
      });
      if (!sigSuccess) {
        Alert.alert("Signature Failed", (sigError && sigError.message) || sigError || "Biometrics failed or cancelled.");
        return; // setLoading(false) will be called in finally
      }
      Alert.alert("Signature Created", "Verifying with server...");
      const loginResponse = await fetch(`${API_URL}/auth/biometric-login`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: usernameForBiometricLogin, challenge: challenge, signature: signature }),
      });
      const loginData = await loginResponse.json();
      if (loginResponse.ok && loginData.success) {
        Alert.alert("Biometric Login Successful", loginData.message || "Logged in!");
      } else {
        Alert.alert("Biometric Login Failed", loginData.message || "Server verification failed.");
      }
    } catch (error) {
      console.error('Biometric login error:', error);
      Alert.alert("Login Process Error", error.message || "An error occurred.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Home Screen</Text>
      <TextInput
        style={styles.input}
        placeholder="Username (for Biometric Ops)"
        value={usernameForBiometricLogin}
        onChangeText={setUsernameForBiometricLogin}
        autoCapitalize="none"
        editable={!loading}
      />
      <View style={styles.buttonContainer}>
        <Button title="Go to Register (New User)" onPress={() => navigation.navigate('Register')} disabled={loading} />
      </View>
      <View style={styles.buttonContainer}>
        <Button title="Setup Biometrics for Username" onPress={handleSetupBiometrics} disabled={loading} />
      </View>
      <View style={styles.buttonContainer}>
        <Button title="Login with Biometrics for Username" onPress={handleBiometricLogin} disabled={loading} />
      </View>
      <View style={styles.buttonContainer}>
        <Button title="View Available Polls" onPress={() => navigation.navigate('Polls')} disabled={loading} />
      </View>
      {loading && <ActivityIndicator style={styles.loader} size="large" color="#0000ff" />}
    </View>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, justifyContent: 'center', alignItems: 'center', padding: 16 },
  title: { fontSize: 24, marginBottom: 20 },
  input: { width: '80%', height: 44, borderColor: 'gray', borderWidth: 1, borderRadius: 5, paddingHorizontal: 10, marginBottom: 20 },
  buttonContainer: { marginVertical: 8, width: '80%' },
  loader: { marginTop: 20 }
});

export default HomeScreen;
