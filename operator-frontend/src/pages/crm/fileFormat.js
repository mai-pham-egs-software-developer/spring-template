import { FileSpreadsheet, FileText, Image } from "lucide-react";

export function formatBytes(bytes) {
  if (bytes === null || bytes === undefined) return "—";
  if (bytes === 0) return "0 B";
  const units = ["B", "KB", "MB", "GB", "TB"];
  const exponent = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
  const value = bytes / 1024 ** exponent;
  return `${exponent === 0 ? value : value.toFixed(1)} ${units[exponent]}`;
}

export function formatDateTime(isoString) {
  if (!isoString) return "—";
  return new Date(isoString).toLocaleString(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "numeric",
    minute: "2-digit",
  });
}

export function iconForContentType(contentType) {
  if (!contentType) return FileText;
  if (contentType.startsWith("image/")) return Image;
  if (contentType.includes("spreadsheet") || contentType.includes("excel") || contentType === "text/csv") {
    return FileSpreadsheet;
  }
  return FileText;
}
