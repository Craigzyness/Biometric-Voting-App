import React, { useState } from 'react';
import { View, Text, TextInput, Button, StyleSheet, Alert, ActivityIndicator } from 'react-native';

// Define backend URL. For Android emulator, 10.0.2.2 typically points to host machine's localhost.
// For iOS simulator, localhost or 127.0.0.1 usually works directly.
// This should ideally be in a config file.
const API_URL = 'http://10.0.2.2:5000'; // For Android Emulator
// const API_URL = 'http://localhost:5000'; // For iOS Simulator or if backend is accessible via localhost

const RegisterScreen = ({ navigation }) => {
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);

  const handleRegister = async () => {
    if (!username.trim() || !email.trim() || !password.trim()) {
      Alert.alert('Validation Error', 'All fields are required.');
      return;
    }
    if (!email.includes('@')) {
      Alert.alert('Validation Error', 'Please enter a valid email address.');
      return;
    }
    if (password.length < 6) {
      Alert.alert('Validation Error', 'Password must be at least 6 characters long.');
      return;
    }

    setLoading(true);

    try {
      const response = await fetch(`${API_URL}/register`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          username: username,
          email: email,
          password: password,
        }),
      });

      const responseData = await response.json();

      if (response.ok) { // status code 200-299
        Alert.alert('Registration Successful', responseData.message || 'You have been registered!');
        // Clear form or navigate
        setUsername('');
        setEmail('');
        setPassword('');
        // navigation.navigate('Home'); // Or a Login screen
      } else {
        Alert.alert('Registration Failed', responseData.message || 'An error occurred.');
      }
    } catch (error) {
      console.error('Registration API error:', error);
      Alert.alert('Network Error', 'Unable to connect to the server. Please try again later.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Register</Text>

      <TextInput
        style={styles.input}
        placeholder="Username"
        value={username}
        onChangeText={setUsername}
        autoCapitalize="none"
        editable={!loading}
      />

      <TextInput
        style={styles.input}
        placeholder="Email"
        value={email}
        onChangeText={setEmail}
        keyboardType="email-address"
        autoCapitalize="none"
        editable={!loading}
      />

      <TextInput
        style={styles.input}
        placeholder="Password"
        value={password}
        onChangeText={setPassword}
        secureTextEntry
        editable={!loading}
      />

      {loading ? (
        <ActivityIndicator size="large" color="#0000ff" />
      ) : (
        <Button
          title="Register"
          onPress={handleRegister}
        />
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
    backgroundColor: '#f0f0f0', // Light background color
  },
  title: {
    fontSize: 28,
    fontWeight: 'bold',
    marginBottom: 24,
    color: '#333',
  },
  input: {
    width: '100%',
    height: 48, // Slightly taller inputs
    backgroundColor: '#fff',
    borderWidth: 1,
    borderColor: '#ccc', // Lighter border
    borderRadius: 8,
    paddingHorizontal: 15, // More padding
    marginBottom: 16,
    fontSize: 16, // Larger font size
  },
  // Button style can be further customized if not using default Button component styling
});

export default RegisterScreen;
