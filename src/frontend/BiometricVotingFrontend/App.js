import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import HomeScreen from './src/HomeScreen';
import RegisterScreen from './src/RegisterScreen';
import PollsScreen from './src/PollsScreen';
import PollDetailScreen from './src/PollDetailScreen'; // Import actual PollDetailScreen

const Stack = createNativeStackNavigator();

const App = () => {
  return (
    <NavigationContainer>
      <Stack.Navigator initialRouteName="Home">
        <Stack.Screen name="Home" component={HomeScreen} options={{ title: 'Home' }} />
        <Stack.Screen name="Register" component={RegisterScreen} options={{ title: 'Register New User' }} />
        <Stack.Screen name="Polls" component={PollsScreen} options={{ title: 'Available Polls' }} />
        <Stack.Screen
          name="PollDetail"
          component={PollDetailScreen}
          // Title can be set dynamically by PollDetailScreen itself using navigation.setOptions,
          // or use the passed pollTitle param if available immediately.
          options={({ route }) => ({ title: route.params?.pollTitle || 'Poll Details' })}
        />
      </Stack.Navigator>
    </NavigationContainer>
  );
};

export default App;
