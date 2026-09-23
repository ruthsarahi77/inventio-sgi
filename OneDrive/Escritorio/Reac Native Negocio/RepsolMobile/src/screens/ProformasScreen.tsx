import { View, Text, StyleSheet } from 'react-native';

export default function ProformasScreen() {
  return (
    <View style={styles.container}>
      <Text style={styles.title}>Proformas</Text>
      <Text style={styles.subtitle}>Gestión de proformas</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F5F5F5',
  },
  title: {
    fontSize: 28,
    fontWeight: 'bold',
    color: '#F58220',
  },
  subtitle: {
    marginTop: 8,
    fontSize: 16,
    color: '#777777',
  },
});