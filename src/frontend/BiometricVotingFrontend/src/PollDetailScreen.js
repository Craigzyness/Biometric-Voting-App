import React, { useEffect, useState, useCallback } from 'react';
import { View, Text, TouchableOpacity, StyleSheet, ActivityIndicator, Alert, ScrollView, Platform } from 'react-native';
import { useRoute, useFocusEffect } from '@react-navigation/native';

const API_URL = Platform.OS === 'ios' ? 'http://localhost:5000' : 'http://10.0.2.2:5000';
const MOCK_USER_ID = 1; // For PoC, replace with actual authenticated user ID

const PollDetailScreen = ({ navigation }) => {
  const route = useRoute();
  const { pollId, pollTitle } = route.params;

  const [poll, setPoll] = useState(null);
  const [selectedOptionId, setSelectedOptionId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  const fetchPollDetails = async () => {
    setLoading(true);
    try {
      const response = await fetch(`${API_URL}/polls/${pollId}`);
      const data = await response.json();
      if (response.ok && data.success) {
        setPoll(data.poll);
      } else {
        Alert.alert("Error", data.message || "Failed to fetch poll details.");
        setPoll(null);
      }
    } catch (error) {
      console.error("Fetch poll detail error:", error);
      Alert.alert("Network Error", "Unable to connect to server for poll details.");
      setPoll(null);
    } finally {
      setLoading(false);
    }
  };

  useFocusEffect(
    useCallback(() => {
      fetchPollDetails();
      // Reset selected option when screen focuses
      setSelectedOptionId(null);
    }, [pollId])
  );

  // Set screen title dynamically (alternative to App.js options if needed, but App.js is preferred)
  // useEffect(() => {
  //   if (poll?.title) {
  //     navigation.setOptions({ title: poll.title });
  //   } else if (pollTitle) {
  //      navigation.setOptions({ title: pollTitle });
  //   }
  // }, [poll, pollTitle, navigation]);


  const handleCastVote = async () => {
    if (selectedOptionId === null) {
      Alert.alert("No Option Selected", "Please select an option to vote.");
      return;
    }
    setSubmitting(true);
    try {
      // CONCEPTUAL: Biometric confirmation could happen here
      // 1. Prepare vote data: { poll_id: pollId, selected_option_id: selectedOptionId, user_id: MOCK_USER_ID }
      // 2. Get challenge from backend (new endpoint or use existing biometric challenge)
      // 3. Sign vote data (or challenge) using ReactNativeBiometrics.createSignature()
      // 4. Send vote data + signature to a modified backend vote endpoint that verifies signature

      const response = await fetch(`${API_URL}/polls/${pollId}/vote`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          user_id: MOCK_USER_ID, // Replace with actual authenticated user ID
          selected_option_id: selectedOptionId,
        }),
      });
      const responseData = await response.json();

      if (response.ok && responseData.success) {
        Alert.alert("Vote Cast Successfully", responseData.message || "Your vote has been recorded.");
        // Optionally navigate back or disable further voting on this poll
        navigation.goBack();
      } else {
        Alert.alert("Voting Failed", responseData.message || "Could not cast your vote.");
      }
    } catch (error) {
      console.error("Cast vote API error:", error);
      Alert.alert("Network Error", "Unable to cast your vote due to a network issue.");
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <View style={styles.centered}>
        <ActivityIndicator size="large" color="#007bff" />
        <Text>Loading poll details...</Text>
      </View>
    );
  }

  if (!poll) {
    return (
      <View style={styles.centered}>
        <Text style={styles.errorText}>Could not load poll details.</Text>
      </View>
    );
  }

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.contentContainer}>
      <Text style={styles.title}>{poll.title}</Text>
      <Text style={styles.description}>{poll.description || 'No description.'}</Text>

      <Text style={styles.optionsTitle}>Options:</Text>
      {poll.options.map((option) => (
        <TouchableOpacity
          key={option.id}
          style={[
            styles.optionButton,
            selectedOptionId === option.id && styles.selectedOptionButton
          ]}
          onPress={() => setSelectedOptionId(option.id)}
          disabled={submitting}
        >
          <Text style={[
            styles.optionText,
            selectedOptionId === option.id && styles.selectedOptionText
          ]}>
            {option.text}
          </Text>
        </TouchableOpacity>
      ))}

      <View style={styles.buttonSpacer} />

      <TouchableOpacity
        style={[styles.castVoteButton, submitting && styles.disabledButton, selectedOptionId === null && styles.disabledButton]}
        onPress={handleCastVote}
        disabled={submitting || selectedOptionId === null}
      >
        {submitting ? (
          <ActivityIndicator color="#fff" />
        ) : (
          <Text style={styles.castVoteButtonText}>Cast Vote</Text>
        )}
      </TouchableOpacity>
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f4f6f8',
  },
  contentContainer: {
    padding: 20,
  },
  centered: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  errorText: {
    fontSize: 16,
    color: 'red',
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 10,
  },
  description: {
    fontSize: 16,
    color: '#555',
    marginBottom: 20,
    lineHeight: 22,
  },
  optionsTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#333',
    marginBottom: 10,
  },
  optionButton: {
    backgroundColor: '#fff',
    paddingVertical: 15,
    paddingHorizontal: 20,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#ddd',
    marginBottom: 12,
  },
  selectedOptionButton: {
    backgroundColor: '#007bff',
    borderColor: '#0056b3',
  },
  optionText: {
    fontSize: 16,
    color: '#333',
  },
  selectedOptionText: {
    color: '#fff',
    fontWeight: '500',
  },
  buttonSpacer: {
    height: 20,
  },
  castVoteButton: {
    backgroundColor: '#28a745',
    paddingVertical: 15,
    borderRadius: 8,
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: 50, // Ensure consistent height with ActivityIndicator
  },
  castVoteButtonText: {
    color: '#fff',
    fontSize: 18,
    fontWeight: 'bold',
  },
  disabledButton: {
    backgroundColor: '#aaa',
  }
});

export default PollDetailScreen;
