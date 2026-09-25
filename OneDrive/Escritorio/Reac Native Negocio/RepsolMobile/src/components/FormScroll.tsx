import { KeyboardAvoidingView, Platform, ScrollView, type ScrollViewProps } from "react-native";

export default function FormScroll({ style, contentContainerStyle, ...props }: ScrollViewProps) {
  return <KeyboardAvoidingView style={[{ flex: 1 }, style]} behavior={Platform.OS === "ios" ? "padding" : "height"}>
    <ScrollView {...props} style={{ flex: 1 }} contentContainerStyle={[{ flexGrow: 1, paddingBottom: 32 }, contentContainerStyle]}
      keyboardShouldPersistTaps="handled" keyboardDismissMode={Platform.OS === "ios" ? "interactive" : "on-drag"} />
  </KeyboardAvoidingView>;
}
