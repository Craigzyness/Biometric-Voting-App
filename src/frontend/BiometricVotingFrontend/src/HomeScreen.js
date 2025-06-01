import React from 'react';
import { View, Text, Button, StyleSheet, Alert } from 'react-native';

// Assume API_URL is defined or imported if needed for actual API calls later
// For now, this is mostly UI and placeholder logic
// const API_URL = 'http://10.0.2.2:5000'; // Or your backend URL

const HomeScreen = ({ navigation }) => {
  // Simulate a logged-in user's ID and token for placeholder
  // In a real app, this would come from auth state (e.g., after login)
  const MOCK_USER_ID = 1;
  // const MOCK_USER_TOKEN = "dummy_auth_token_for_poc";

  const handleSetupBiometrics = async () => {
    Alert.alert(
      "Setup Biometrics",
      "This will simulate biometric capture and registration. Proceed?",
      [
        {
          text: "Cancel",
          style: "cancel"
        },
        {
          text: "Proceed",
          onPress: async () => _Real_Biometrics_Setup_Function()
        }
      ]
    );
  };

  const _Real_Biometrics_Setup_Function = async () => {
    // 1. Simulate Biometric Capture & Key Generation
    // In a real app, use react-native-biometrics or similar:
    // - Check for hardware/enrollment
    // - Generate public/private key pair
    // - Private key stored in Keychain, public key returned to app
    console.log("Simulating biometric capture and public key generation...");
    const dummyPublicKey = "simulated_public_key_" + Date.now();
    Alert.alert("Biometric Capture (Simulated)", `Generated Public Key: ${dummyPublicKey}`);

    // 2. Send Public Key to Backend (placeholder function for now)
    // The actual API endpoint /users/<user_id>/biometrics will be built in the backend next.
    // And this function will make a fetch POST request.
    try {
      console.log(`Sending public key for user ID ${MOCK_USER_ID} to backend (placeholder)...`);
      // Placeholder for the actual API call:
      // const response = await fetch(`${API_URL}/users/${MOCK_USER_ID}/biometrics`, {
      //   method: 'POST',
      //   headers: {
      //     'Content-Type': 'application/json',
      //     // 'Authorization': `Bearer ${MOCK_USER_TOKEN}`, // If auth is implemented
      //   },
      //   body: JSON.stringify({ biometric_public_key: dummyPublicKey }),
      // });
      // const responseData = await response.json();
      // if (response.ok) {
      //   Alert.alert("Biometric Setup", "Biometric key successfully sent to backend.");
      // } else {
      //   Alert.alert("Backend Error", responseData.message || "Failed to send key.");
      // }

      // For this placeholder step, we just simulate success:
      Alert.alert("Biometric Setup (Simulated)", "Public key would be sent to backend.");
      // TODO: Implement actual API call in a subsequent step.

    } catch (error) {
      console.error("Biometric setup error:", error);
      Alert.alert("Error", "An error occurred during biometric setup simulation.");
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Home Screen</Text>
      <View style={styles.buttonContainer}>
        <Button
          title="Go to Register"
          onPress={() => navigation.navigate('Register')}
        />
      </View>
      <View style={styles.buttonContainer}>
        <Button
          title="Setup Biometrics (Simulated)"
          onPress={handleSetupBiometrics}
        />
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
    marginVertical: 10, // Add vertical margin to space out buttons
    width: '80%', // Make buttons take more width
  }
});

export default HomeScreen;
