import { createNativeStackNavigator } from '@react-navigation/native-stack';

import LoginScreen from "../screens/LoginScreen"
import HomeScreen from '../screens/HomeScreen';
import ClientesScreen from '../screens/ClientesScreen';
import StockScreen from '../screens/StockScreen';
import ProformasScreen from '../screens/ProformasScreen';
import VentasScreen from '../screens/VentasScreen';
import RecibosScreen from '../screens/RecibosScreen';
import MisVentasScreen from '../screens/MisVentasScreen';

const Stack = createNativeStackNavigator();

export default function StackNavigator() {
  return (
    <Stack.Navigator
      initialRouteName="Login"
      screenOptions={{
        headerShown: false,
      }}
    >
      <Stack.Screen name="Login" component={LoginScreen} />
      <Stack.Screen name="Home" component={HomeScreen} />
      <Stack.Screen name="Clientes" component={ClientesScreen} />
      <Stack.Screen name="Stock" component={StockScreen} />
      <Stack.Screen name="Proformas" component={ProformasScreen} />
      <Stack.Screen name="Ventas" component={VentasScreen} />
      <Stack.Screen name="Recibos" component={RecibosScreen} />
      <Stack.Screen name="MisVentas" component={MisVentasScreen} />
    </Stack.Navigator>
  );
}