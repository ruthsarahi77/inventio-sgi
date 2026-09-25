import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
} from 'react-native';

import { router } from 'expo-router';
import type { ReactNode } from 'react';

interface ScreenLayoutProps {
  title: string;
  subtitle: string;
  children: ReactNode;
  showBack?: boolean;
}

export default function ScreenLayout({
  title,
  subtitle,
  children,
  showBack = true,
}: ScreenLayoutProps) {
  return (
    <View style={styles.container}>
      {/* ENCABEZADO */}
      <View style={styles.header}>
        <View style={styles.headerTop}>
          {showBack && (
            <TouchableOpacity
              style={styles.backButton}
              onPress={() => router.canGoBack() ? router.back() : router.replace("/")}
            >
              <Text style={styles.backText}>‹</Text>
            </TouchableOpacity>
          )}

          <View style={styles.headerTextContainer}>
            <Text style={styles.headerTitle}>{title}</Text>
            <Text style={styles.headerSubtitle}>{subtitle}</Text>
          </View>
        </View>
      </View>

      {/* CONTENIDO */}
      <ScrollView
        style={styles.scroll}
        contentContainerStyle={styles.content}
        showsVerticalScrollIndicator={false}
      >
        {children}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F5F5F5',
  },

  header: {
    backgroundColor: '#F58220',
    paddingTop: 55,
    paddingBottom: 25,
    paddingHorizontal: 20,
  },

  headerTop: {
    flexDirection: 'row',
    alignItems: 'center',
  },

  backButton: {
    width: 40,
    height: 40,
    borderRadius: 12,
    backgroundColor: '#FFFFFF',
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 12,
  },

  backText: {
    fontSize: 32,
    color: '#F58220',
    marginTop: -4,
  },

  headerTextContainer: {
    flex: 1,
  },

  headerTitle: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#FFFFFF',
  },

  headerSubtitle: {
    fontSize: 13,
    color: '#FFF3E8',
    marginTop: 4,
  },

  scroll: {
    flex: 1,
  },

  content: {
    padding: 20,
    paddingBottom: 40,
  },
});
