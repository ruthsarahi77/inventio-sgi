import { Platform } from "react-native";
import { File, Paths } from "expo-file-system";
import * as Sharing from "expo-sharing";
import * as Print from "expo-print";
import { getQuotePdf } from "./quotes";

export async function shareQuotePdf(id: number): Promise<void> {
  const bytes = await getQuotePdf(id);
  if (Platform.OS === "web") {
    const url = URL.createObjectURL(new Blob([new Uint8Array(bytes)], { type: "application/pdf" }));
    const link = document.createElement("a");
    link.href = url;
    link.download = `proforma-${id}.pdf`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    setTimeout(() => URL.revokeObjectURL(url), 60000);
    return;
  }
  const file = new File(Paths.cache, `proforma-${id}-${Date.now()}.pdf`);
  try {
    file.write(bytes);
    if (await Sharing.isAvailableAsync()) {
      await Sharing.shareAsync(file.uri, { mimeType: "application/pdf", UTI: "com.adobe.pdf", dialogTitle: "Abrir o compartir proforma" });
    } else {
      await Print.printAsync({ uri: file.uri });
    }
  } finally {
    if (file.exists) file.delete();
  }
}
