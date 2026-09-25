import { useState } from "react";
import { TextInput, TouchableOpacity, View, type TextInputProps } from "react-native";
import { Ionicons } from "@expo/vector-icons";

export default function PasswordInput({ style, ...props }: TextInputProps) {
  const [visible, setVisible] = useState(false);
  return <View style={{ width: "100%", position: "relative" }}>
    <TextInput {...props} style={[style, { paddingRight: 52 }]} secureTextEntry={!visible} />
    <TouchableOpacity accessibilityRole="button" accessibilityLabel={visible ? "Ocultar contraseña" : "Mostrar contraseña"}
      onPress={() => setVisible(value => !value)} style={{ position: "absolute", right: 4, top: 0, width: 44, height: 48, alignItems: "center", justifyContent: "center" }}>
      <Ionicons name={visible ? "eye-off-outline" : "eye-outline"} size={22} color="#F58220" />
    </TouchableOpacity>
  </View>;
}
