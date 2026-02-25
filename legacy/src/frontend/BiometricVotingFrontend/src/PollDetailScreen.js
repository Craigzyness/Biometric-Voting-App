import React, { useEffect, useState, useCallback } from 'react';
import { View, Text, TouchableOpacity, StyleSheet, ActivityIndicator, Alert, ScrollView, Platform, Button } from 'react-native'; // Added Button
import { useRoute, useFocusEffect } from '@react-navigation/native';

const API_URL = Platform.OS === 'ios' ? 'http://localhost:5000' : 'http://10.0.2.2:5000';
const MOCK_USER_ID = 1; // For PoC, replace with actual authenticated user ID

const PollDetailScreen = ({ navigation }) => {
  const route = useRoute();
  const { pollId, pollTitle } = route.params;

  const [poll, setPoll] = useState(null);
  const [selectedOptionId, setSelectedOptionId] = useState(null); // This stores the option's 'id' field
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  const fetchPollDetails = async () => {
    setLoading(true);
    try {
      const response = await fetch(`${API_URL}/polls/${pollId}`);
      const data = await response.json();
      if (response.ok && data.success) {
        setPoll(data.poll);
        // console.log("Fetched poll details with vote counts:", data.poll);
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
      setSelectedOptionId(null);
    }, [pollId])
  );

  const handleCastVote = async () => {
    if (selectedOptionId === null) {
      Alert.alert("No Option Selected", "Please select an option to vote.");
      return;
    }
    setSubmitting(true);

    let selectedOptionIndex = -1;
    if (poll && poll.options) {
        // Assuming poll.options is an array of objects like [{id: 1, text: "A"}, {id: 2, text: "B"}]
        // And selectedOptionId stores the 'id' of the chosen option.
        // The smart contract expects the 0-based index of the option in its 'options' array.
        selectedOptionIndex = poll.options.findIndex(opt => opt.id === selectedOptionId);
    }

    if (selectedOptionIndex === -1) {
        Alert.alert("Error", "Selected option is invalid or not found. Please refresh.");
        setSubmitting(false);
        return;
    }

    try {
      const response = await fetch(`${API_URL}/polls/${pollId}/vote`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          user_id: MOCK_USER_ID,
          selected_option_id: selectedOptionIndex, // Send option INDEX to backend
        }),
      });
      const responseData = await response.json();

      if (response.status === 202 && responseData.success) { // Check for 202 Accepted
        Alert.alert(
          "Vote Submitted",
          responseData.message || "Your vote has been submitted. Results will update after blockchain confirmation.",
          [{ text: "OK", onPress: () => fetchPollDetails() }]
        );
      } else {
        Alert.alert("Voting Failed", responseData.message || `Could not cast your vote. Status: ${response.status}`);
      }
    } catch (error) {
      console.error("Cast vote API error:", error);
      Alert.alert("Network Error", "Unable to cast your vote due to a network issue.");
    } finally {
      setSubmitting(false);
    }
  };

  if (loading && !poll) {
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
        <Text style={styles.errorText}>Could not load poll details. Please try again.</Text>
        <Button title="Retry" onPress={fetchPollDetails} />
      </View>
    );
  }

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.contentContainer}>
      <Text style={styles.title}>{poll.title}</Text>
      <Text style={styles.description}>{poll.description || 'No description.'}</Text>

      <Text style={styles.optionsTitle}>Options:</Text>
      {poll.options && poll.options.map((option, index) => ( // Ensure poll.options exists
        <TouchableOpacity
          key={option.id}
          style={[
            styles.optionButton,
            selectedOptionId === option.id && styles.selectedOptionButton
          ]}
          onPress={() => setSelectedOptionId(option.id)}
          disabled={submitting}
        >
          <View style={styles.optionRow}>
            <Text style={[
              styles.optionText,
              selectedOptionId === option.id && styles.selectedOptionText
            ]}>
              {option.text}
            </Text>
            {poll.vote_counts && poll.vote_counts[index] !== undefined && (
              <Text style={[
                styles.voteCountText,
                selectedOptionId === option.id && styles.selectedOptionVoteCountText
              ]}>
                Votes: {poll.vote_counts[index]}
              </Text>
            )}
          </View>
        </TouchableOpacity>
      ))}

      <View style={styles.buttonSpacer} />

      <TouchableOpacity
        style={[styles.castVoteButton, (submitting || selectedOptionId === null) && styles.disabledButton]}
        onPress={handleCastVote}
        disabled={submitting || selectedOptionId === null}
      >
        {submitting ? (
          <ActivityIndicator color="#fff" />
        ) : (
          <Text style={styles.castVoteButtonText}>Cast Vote</Text>
        )}
      </TouchableOpacity>
       <View style={styles.buttonSpacer} />
       <Button title="Refresh Details" onPress={fetchPollDetails} disabled={loading || submitting} />
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f4f6f8' },
  contentContainer: { padding: 20, paddingBottom: 40 },
  centered: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  errorText: { fontSize: 16, color: 'red', marginBottom: 10 },
  title: { fontSize: 24, fontWeight: 'bold', color: '#333', marginBottom: 10 },
  description: { fontSize: 16, color: '#555', marginBottom: 20, lineHeight: 22 },
  optionsTitle: { fontSize: 18, fontWeight: '600', color: '#333', marginBottom: 10 },
  optionButton: {
    backgroundColor: '#fff', paddingVertical: 12, paddingHorizontal: 15,
    borderRadius: 8, borderWidth: 1, borderColor: '#ddd', marginBottom: 12,
  },
  selectedOptionButton: { backgroundColor: '#007bff', borderColor: '#0056b3' },
  optionRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  optionText: { fontSize: 16, color: '#333', flexShrink: 1 },
  selectedOptionText: { color: '#fff', fontWeight: '500' },
  voteCountText: { fontSize: 14, color: '#007bff', fontWeight: 'bold' },
  selectedOptionVoteCountText: { color: '#e0e0e0' },
  buttonSpacer: { height: 20 },
  castVoteButton: {
    backgroundColor: '#28a745', paddingVertical: 15, borderRadius: 8,
    alignItems: 'center', justifyContent: 'center', minHeight: 50,
  },
  castVoteButtonText: { color: '#fff', fontSize: 18, fontWeight: 'bold' },
  disabledButton: { backgroundColor: '#ced4da', borderColor: '#adb5bd' }
});

export default PollDetailScreen;
