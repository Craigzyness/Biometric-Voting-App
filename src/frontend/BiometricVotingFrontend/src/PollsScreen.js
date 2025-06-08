import React, { useEffect, useState, useCallback } from 'react';
import { View, Text, FlatList, TouchableOpacity, StyleSheet, ActivityIndicator, RefreshControl, Alert, Platform, Button } from 'react-native'; // Added Platform and Button
import { useFocusEffect } from '@react-navigation/native';

const API_URL = Platform.OS === 'ios' ? 'http://localhost:5000' : 'http://10.0.2.2:5000';

const PollsScreen = ({ navigation }) => {
  const [polls, setPolls] = useState([]);
  const [loading, setLoading] = useState(true); // Start with loading true
  const [refreshing, setRefreshing] = useState(false);

  const fetchPolls = async () => {
    // setLoading(true); // setLoading(true) is handled by useFocusEffect or onRefresh
    try {
      const response = await fetch(`${API_URL}/polls`);
      const data = await response.json();
      if (response.ok && data.success) {
        setPolls(data.polls);
      } else {
        Alert.alert("Error Fetching Polls", data.message || "Failed to fetch polls from server.");
        setPolls([]);
      }
    } catch (error) {
      console.error("Fetch polls API error:", error);
      Alert.alert("Network Error", "Unable to connect to the server to fetch polls.");
      setPolls([]);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useFocusEffect(
    useCallback(() => {
      setLoading(true); // Set loading true when screen focuses to show indicator
      fetchPolls();
    }, [])
  );

  const onRefresh = useCallback(() => {
    setRefreshing(true);
    fetchPolls();
  }, []);

  const renderPollItem = ({ item }) => (
    <TouchableOpacity
      style={styles.pollItem}
      onPress={() => navigation.navigate('PollDetail', { pollId: item.id, pollTitle: item.title })}
    >
      <Text style={styles.pollTitle}>{item.title}</Text>
      <Text style={styles.pollDescription} numberOfLines={2}>{item.description || 'No description.'}</Text>
      <Text style={styles.pollDate}>
        Created: {new Date(item.created_at).toLocaleDateString()}
        {item.end_date ? ` | Ends: ${new Date(item.end_date).toLocaleDateString()}` : ''}
      </Text>
    </TouchableOpacity>
  );

  if (loading && !refreshing && polls.length === 0) { // Show initial loading indicator only if polls array is empty
    return (
      <View style={styles.centered}>
        <ActivityIndicator size="large" color="#007bff" />
        <Text>Loading polls...</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      {polls.length === 0 && !loading ? ( // Changed condition to ensure loading is false
        <View style={styles.centered}>
          <Text style={styles.emptyText}>No polls are currently available.</Text>
          <Button title="Refresh Polls" onPress={fetchPolls} color="#007bff"/>
        </View>
      ) : (
        <FlatList
          data={polls}
          renderItem={renderPollItem}
          keyExtractor={(item) => item.id.toString()}
          contentContainerStyle={styles.listContentContainer}
          refreshControl={
            <RefreshControl refreshing={refreshing} onRefresh={onRefresh} colors={["#007bff"]} tintColor={"#007bff"}/>
          }
        />
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f4f6f8' },
  centered: { flex: 1, justifyContent: 'center', alignItems: 'center', padding: 20 },
  listContentContainer: { paddingVertical: 8, paddingHorizontal: 10 },
  pollItem: {
    backgroundColor: '#ffffff',
    padding: 16,
    marginVertical: 8,
    borderRadius: 10,
    shadowColor: '#000000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  pollTitle: { fontSize: 18, fontWeight: '600', color: '#333', marginBottom: 6 },
  pollDescription: { fontSize: 14, color: '#555', marginBottom: 10 },
  pollDate: { fontSize: 12, color: '#777' },
  emptyText: { fontSize: 16, color: '#666', textAlign: 'center', marginBottom: 15, }
});

export default PollsScreen;
