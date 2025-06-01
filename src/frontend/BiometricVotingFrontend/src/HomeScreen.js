import React, { useState } from 'react'; // Added useState for loading indicator
import { View, Text, Button, StyleSheet, Alert, Platform, ActivityIndicator } from 'react-native';
import ReactNativeBiometrics from 'react-native-biometrics';

// Define backend URL. For Android emulator, 10.0.2.2 typically points to host machine's localhost.
// For iOS simulator, localhost or 127.0.0.1 usually works directly.
const API_URL = Platform.OS === 'ios' ? 'http://localhost:5000' : 'http://10.0.2.2:5000';

const HomeScreen = ({ navigation }) => {
  const MOCK_USER_ID = 1; // Simulate a logged-in user's ID for placeholder
  // In a real app, user ID and auth token would come from auth state.
  // const MOCK_AUTH_TOKEN = "some_valid_session_token";
  const [loading, setLoading] = useState(false);

  const rnBiometrics = new ReactNativeBiometrics({ allowDeviceCredentials: true });

  const handleSetupBiometrics = async () => {
    Alert.alert(
      "Setup Biometrics",
      "This will attempt to create a new biometric key pair. This key will be protected by your device biometrics. Proceed?",
      [
        { text: "Cancel", style: "cancel" },
        { text: "Proceed", onPress: () => performBiometricSetup() }
      ]
    );
  };

  const performBiometricSetup = async () => {
    setLoading(true);
    try {
      const { available, biometryType } = await rnBiometrics.isSensorAvailable();

      if (!available) {
        Alert.alert("Biometrics Not Available", `Biometric hardware not available or not configured on this device. Type: ${biometryType || 'N/A'}`);
        setLoading(false); // Ensure loading is stopped
        return;
      }

      Alert.alert("Biometric Sensor", `Sensor available. Type: ${biometryType || 'Biometrics'}`);

      // Generate public/private key pair
      const { publicKey } = await rnBiometrics.createKeys();

      if (!publicKey) {
        Alert.alert("Key Generation Failed", "Could not generate biometric keys. Please ensure biometrics are enrolled and try again.");
        setLoading(false); // Ensure loading is stopped
        return;
      }

      Alert.alert("Biometric Key Generated", `Public Key: ${publicKey.substring(0,30)}...`);

      // Send the publicKey to the backend
      await sendPublicKeyToBackend(publicKey);

    } catch (error) {
      console.error('Biometric setup error:', error);
      Alert.alert("Biometric Setup Error", error.message || "An unexpected error occurred during biometric setup.");
      // setLoading(false); // setLoading is handled in finally
    } finally {
      // setLoading(false); // Moved to sendPublicKeyToBackend's finally for more granular control
      // If sendPublicKeyToBackend is awaited, this finally will execute after it.
      // If sendPublicKeyToBackend handles its own loading, this might be redundant or premature.
      // For now, let sendPublicKeyToBackend manage its own loading state for the API call part.
      // If key gen succeeds but API call part is separate, then this setLoading(false) might be needed here.
      // Based on current structure, it's better to have setLoading(false) in the API call function's finally.
      // The setLoading(true) at the start of this function will be turned off by sendPublicKeyToBackend's finally.
    }
  };

  const sendPublicKeyToBackend = async (publicKey) => {
    // setLoading(true); // setLoading(true) is already called in performBiometricSetup
    try {
      console.log(`Sending public key for user ID ${MOCK_USER_ID} to backend...`);
      console.log(`Public Key: ${publicKey}`);

      const response = await fetch(`${API_URL}/users/${MOCK_USER_ID}/biometrics`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          // 'Authorization': `Bearer ${MOCK_AUTH_TOKEN}`, // Add if your endpoint is protected
        },
        body: JSON.stringify({ biometric_public_key: publicKey }),
      });

      const responseData = await response.json();

      if (response.ok) {
        Alert.alert("Biometric Setup Successful", responseData.message || "Biometric key registered with backend.");
      } else {
        Alert.alert("Backend Error", responseData.message || `Failed to send key. Status: ${response.status}`);
      }
    } catch (error) {
      console.error("Send public key API error:", error);
      Alert.alert("Network Error", "Unable to send public key to server. Please try again later.");
    } finally {
      setLoading(false); // Ensure loading is turned off after API call
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Home Screen</Text>
      <View style={styles.buttonContainer}>
        <Button
          title="Go to Register"
          onPress={() => navigation.navigate('Register')}
          disabled={loading}
        />
      </View>
      <View style={styles.buttonContainer}>
        {loading ? (
          <ActivityIndicator size="large" color="#0000ff" />
        ) : (
          <Button
            title="Setup Biometrics"
            onPress={handleSetupBiometrics}
          />
        )}
      </View>
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
  buttonContainer: {
    marginVertical: 10,
    width: '80%',
  }
});

export default HomeScreen;
