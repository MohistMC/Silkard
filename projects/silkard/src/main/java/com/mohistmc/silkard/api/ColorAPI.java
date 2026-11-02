package com.mohistmc.silkard.api;

import com.mohistmc.tools.ChineseColors;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorAPI {

    private static final Pattern GRADIENT_PATTERN = Pattern.compile("<gradient(?:[:]#?[0-9A-Fa-f]{6}|[:][\\w\\u4e00-\\u9fa5]+)+>(.*?)</gradient>", Pattern.CASE_INSENSITIVE);
    private static final Pattern GRADIENT_COLORS = Pattern.compile("[:]([^:>]+)");
    private static final Pattern SOLID_COLOR_PATTERN = Pattern.compile("<(#?[0-9A-Fa-f]{6}|[a-zA-Z_\\u4e00-\\u9fa5]+)>(.*?)</\\1>", Pattern.CASE_INSENSITIVE);
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([0-9A-Fa-f]{6})");
    private static final Pattern LEGACY_PATTERN = Pattern.compile("&([0-9a-fk-orA-FK-OR])");
    private static final Pattern SHORT_COLOR_PATTERN = Pattern.compile("#([\\w\\u4e00-\\u9fa5]+)");
    private static final Pattern STRIP_GRADIENT = Pattern.compile("<gradient(?:[:]#?[0-9A-Fa-f]{6}|[:][\\w\\u4e00-\\u9fa5]+)+>(.*?)</gradient>", Pattern.CASE_INSENSITIVE);
    private static final Pattern STRIP_SOLID = Pattern.compile("<(#?[0-9A-Fa-f]{6}|[a-zA-Z_\\u4e00-\\u9fa5]+)>(.*?)</\\1>", Pattern.CASE_INSENSITIVE);
    private static final Pattern STRIP_HEX = Pattern.compile("&#([0-9A-Fa-f]{6})");
    private static final Pattern STRIP_LEGACY = Pattern.compile("&([0-9a-fk-orA-FK-OR])");
    private static final Pattern STRIP_SECTION = Pattern.compile("§[0-9a-fk-orA-FK-ORx]");
    private static final char ANSI_ESC = 0x1B;

    private ColorAPI() {}

    /**
     * Process color codes and return a legacy-formatted string ready for Bukkit Player.sendMessage(String).
     */
    public static String string(String text) {
        if (text == null || text.isEmpty()) return text == null ? "" : text;
        String processed = processGradients(text);
        processed = processSolidColors(processed);
        processed = processHexColors(processed);
        processed = processLegacyCodes(processed);
        processed = processShortColorCodes(processed);
        return processed;
    }

    /** Alias of {@link #string(String)}. */
    public static String legacy(String text) {
        return string(text);
    }

    /**
     * Convert a processed string to ANSI 24-bit true-color for console display.
     */
    public static String ansi(String text) {
        return ansi(text, false);
    }

    /**
     * Convert a processed string to ANSI 24-bit true-color, optionally keeping legacy § codes as fallback.
     */
    public static String ansi(String text, boolean keepLegacy) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                char code = text.charAt(i + 1);
                if (code == 'x' && i + 7 < text.length()) {
                    // §x§R§R§G§G§B§B
                    StringBuilder hex = new StringBuilder();
                    boolean ok = true;
                    for (int k = 0; k < 6; k++) {
                        if (text.charAt(i + 2 + k * 2) != '§') { ok = false; break; }
                        hex.append(text.charAt(i + 3 + k * 2));
                    }
                    if (ok) {
                        int r = Integer.parseInt(hex.substring(0, 2), 16);
                        int g = Integer.parseInt(hex.substring(2, 4), 16);
                        int b = Integer.parseInt(hex.substring(4, 6), 16);
                        sb.append(ANSI_ESC).append("[38;2;").append(r).append(';').append(g).append(';').append(b).append('m');
                        i += 14;
                        continue;
                    }
                } else {
                    String legacyToAnsi = legacyToAnsi(code);
                    if (legacyToAnsi != null) {
                        sb.append(legacyToAnsi);
                        i += 2;
                        continue;
                    }
                }
            }
            sb.append(c);
            i++;
        }
        sb.append(ANSI_ESC).append("[0m");
        return sb.toString();
    }

    /**
     * Remove all color codes and return plain text.
     */
    public static String stripColors(String text) {
        if (text == null || text.isEmpty()) return text == null ? "" : text;
        String result = text;
        result = STRIP_GRADIENT.matcher(result).replaceAll("$2");
        result = STRIP_SOLID.matcher(result).replaceAll("$2");
        result = STRIP_HEX.matcher(result).replaceAll("");
        result = STRIP_LEGACY.matcher(result).replaceAll("");
        result = STRIP_SECTION.matcher(result).replaceAll("");
        result = result.replaceAll("§x§[0-9a-fA-F]{6}", "");
        return result;
    }

    private static String legacyToAnsi(char code) {
        return switch (code) {
            case '0' -> ANSI_ESC + "[30m";
            case '1' -> ANSI_ESC + "[34m";
            case '2' -> ANSI_ESC + "[32m";
            case '3' -> ANSI_ESC + "[36m";
            case '4' -> ANSI_ESC + "[31m";
            case '5' -> ANSI_ESC + "[35m";
            case '6' -> ANSI_ESC + "[33m";
            case '7' -> ANSI_ESC + "[37m";
            case '8' -> ANSI_ESC + "[90m";
            case '9' -> ANSI_ESC + "[94m";
            case 'a' -> ANSI_ESC + "[92m";
            case 'b' -> ANSI_ESC + "[96m";
            case 'c' -> ANSI_ESC + "[91m";
            case 'd' -> ANSI_ESC + "[95m";
            case 'e' -> ANSI_ESC + "[93m";
            case 'f' -> ANSI_ESC + "[97m";
            case 'l' -> ANSI_ESC + "[1m";
            case 'o' -> ANSI_ESC + "[3m";
            case 'n' -> ANSI_ESC + "[4m";
            case 'm' -> ANSI_ESC + "[9m";
            case 'k' -> ANSI_ESC + "[8m";
            case 'r' -> ANSI_ESC + "[0m";
            default -> null;
        };
    }

    // --- gradient & helpers ---

    private static String processGradients(String text) {
        Matcher matcher = GRADIENT_PATTERN.matcher(text);
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            String fullTag = matcher.group(0);
            String content = matcher.group(1);
            List<String> colors = new ArrayList<>();
            Matcher colorMatcher = GRADIENT_COLORS.matcher(fullTag);
            while (colorMatcher.find()) {
                String color = colorMatcher.group(1);
                if (color.startsWith("#")) color = color.substring(1);
                colors.add(color);
            }
            String gradientText;
            if (colors.size() >= 2) {
                gradientText = createMultiGradient(content, colors);
            } else if (colors.size() == 1) {
                gradientText = "§x" + convertToHexFormat(resolveColorHex(colors.getFirst())) + content;
            } else {
                gradientText = content;
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(gradientText));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String createMultiGradient(String text, List<String> colorNames) {
        if (text.isEmpty() || colorNames.size() < 2) return text;
        try {
            int[] colors = new int[colorNames.size()];
            for (int i = 0; i < colorNames.size(); i++) {
                String hex = resolveColorHex(colorNames.get(i));
                colors[i] = Integer.parseInt(hex, 16);
            }
            StringBuilder gradientBuilder = new StringBuilder();
            int length = text.length();
            int segments = colors.length - 1;
            if (segments <= 0) return text;
            int charsPerSegment = length / segments;
            int remainingChars = length % segments;
            int currentIndex = 0;
            for (int i = 0; i < segments; i++) {
                int segmentLength = charsPerSegment + (i < remainingChars ? 1 : 0);
                if (currentIndex >= length) break;
                int endIndex = Math.min(currentIndex + segmentLength, length);
                String segmentText = text.substring(currentIndex, endIndex);
                int startColor = colors[i];
                int endColor = colors[i + 1];
                for (int j = 0; j < segmentText.length(); j++) {
                    char c = segmentText.charAt(j);
                    double ratio = segmentText.length() > 1 ? (double) j / (segmentText.length() - 1) : 0.5;
                    int r = Math.clamp((int) (((startColor >> 16) & 0xFF) + ((endColor >> 16) & 0xFF - (startColor >> 16) & 0xFF) * ratio), 0, 255);
                    int g = Math.clamp((int) (((startColor >> 8) & 0xFF) + ((endColor >> 8) & 0xFF - (startColor >> 8) & 0xFF) * ratio), 0, 255);
                    int b = Math.clamp((int) ((startColor & 0xFF) + ((endColor & 0xFF) - (startColor & 0xFF)) * ratio), 0, 255);
                    String hexColor = String.format("%02X%02X%02X", r, g, b);
                    gradientBuilder.append("§x").append(convertToHexFormat(hexColor)).append(c);
                }
                currentIndex = endIndex;
            }
            return gradientBuilder.toString();
        } catch (Exception e) {
            return "§x" + convertToHexFormat(resolveColorHex(colorNames.getFirst())) + text;
        }
    }

    private static String processSolidColors(String text) {
        Matcher matcher = SOLID_COLOR_PATTERN.matcher(text);
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            String color = matcher.group(1);
            String content = matcher.group(2);
            String coloredText;
            if (color.startsWith("#")) {
                coloredText = "§x" + convertToHexFormat(color.substring(1)) + content;
            } else if (color.matches("[0-9A-Fa-f]{6}")) {
                coloredText = "§x" + convertToHexFormat(color) + content;
            } else {
                String hex = resolveColorHex(color);
                if (!hex.equals(color)) {
                    coloredText = "§x" + convertToHexFormat(hex) + content;
                } else {
                    String legacyCode = colorNameToLegacy(color);
                    coloredText = legacyCode + content;
                }
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(coloredText));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String processHexColors(String text) {
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(1);
            matcher.appendReplacement(buffer, "§x" + convertToHexFormat(hex));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String processLegacyCodes(String text) {
        Matcher matcher = LEGACY_PATTERN.matcher(text);
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            String code = matcher.group(1);
            matcher.appendReplacement(buffer, "§" + code);
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String convertToHexFormat(String hex) {
        StringBuilder builder = new StringBuilder();
        for (char c : hex.toCharArray()) {
            builder.append('§').append(Character.toLowerCase(c));
        }
        return builder.toString();
    }

    private static String colorNameToLegacy(String colorName) {
        return switch (colorName.toLowerCase()) {
            case "black" -> "§0";
            case "dark_blue", "darkblue" -> "§1";
            case "dark_green", "darkgreen" -> "§2";
            case "dark_aqua", "darkaqua" -> "§3";
            case "dark_red", "darkred" -> "§4";
            case "dark_purple", "darkpurple" -> "§5";
            case "gold", "orange" -> "§6";
            case "gray", "grey" -> "§7";
            case "dark_gray", "darkgray", "dark_grey", "darkgrey" -> "§8";
            case "blue" -> "§9";
            case "green" -> "§a";
            case "aqua", "cyan" -> "§b";
            case "red" -> "§c";
            case "light_purple", "lightpurple", "pink", "magenta" -> "§d";
            case "yellow" -> "§e";
            case "white" -> "§f";
            case "bold" -> "§l";
            case "italic" -> "§o";
            case "underlined", "underline" -> "§n";
            case "strikethrough", "strike" -> "§m";
            case "obfuscated", "obfuscate" -> "§k";
            case "reset" -> "§r";
            default -> "";
        };
    }

    private static String resolveColorHex(String color) {
        if (color.matches("[0-9A-Fa-f]{6}")) return color;
        ChineseColors chineseColor = ChineseColors.getByName(color);
        if (chineseColor != null) return chineseColor.getHexCode().substring(1);
        return color;
    }

    private static String tryResolveColor(String candidate) {
        if (candidate.matches("[0-9A-Fa-f]{6}")) return "§x" + convertToHexFormat(candidate);
        String hex = resolveColorHex(candidate);
        if (!hex.equals(candidate)) return "§x" + convertToHexFormat(hex);
        String legacy = colorNameToLegacy(candidate);
        if (!legacy.isEmpty()) return legacy;
        return null;
    }

    private static String processShortColorCodes(String text) {
        if (text == null || text.isEmpty()) return text;
        Matcher matcher = SHORT_COLOR_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String rawName = matcher.group(1);
            String replacement = null;
            for (int len = rawName.length(); len >= 1; len--) {
                String candidate = rawName.substring(0, len);
                replacement = tryResolveColor(candidate);
                if (replacement != null) {
                    if (len < rawName.length()) replacement += rawName.substring(len);
                    break;
                }
            }
            if (replacement == null) replacement = matcher.group(0);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
