package main.java.com.complaint.util;

import java.util.*;

public class SimpleJSON {

    public static String toJSON(Object obj) {
        if (obj == null) {
            return "null";
        }
        if (obj instanceof String) {
            return "\"" + escape((String) obj) + "\"";
        }
        if (obj instanceof Number) {
            return obj.toString();
        }
        if (obj instanceof Boolean) {
            return obj.toString();
        }
        if (obj instanceof Map) {
            return mapToJSON((Map<?, ?>) obj);
        }
        if (obj instanceof List) {
            return listToJSON((List<?>) obj);
        }
        if (obj instanceof JSONable) {
            return ((JSONable) obj).toJSON();
        }
        if (obj instanceof Object[]) {
            return arrayToJSON((Object[]) obj);
        }
        return "\"" + escape(obj.toString()) + "\"";
    }

    private static String mapToJSON(Map<?, ?> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            first = false;

            sb.append("\"").append(escape(entry.getKey().toString())).append("\":");
            sb.append(toJSON(entry.getValue()));
        }

        sb.append("}");
        return sb.toString();
    }

    private static String listToJSON(List<?> list) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;

        for (Object obj : list) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append(toJSON(obj));
        }

        sb.append("]");
        return sb.toString();
    }

    private static String arrayToJSON(Object[] array) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;

        for (Object obj : array) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append(toJSON(obj));
        }

        sb.append("]");
        return sb.toString();
    }

    private static String escape(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public static Map<String, Object> parse(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new HashMap<>();
        }
        json = json.trim();

        if (json.startsWith("{") && json.endsWith("}")) {
            return parseObject(json.substring(1, json.length() - 1));
        }

        throw new IllegalArgumentException("Invalid JSON: Expected object");
    }

    public static List<Object> parseArray(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        json = json.trim();

        if (json.startsWith("[") && json.endsWith("]")) {
            return parseArrayInternal(json.substring(1, json.length() - 1));
        }

        throw new IllegalArgumentException("Invalid JSON: Expected array");
    }

    private static Map<String, Object> parseObject(String json) {
        Map<String, Object> result = new HashMap<>();
        List<String> tokens = tokenize(json, '{', '}');

        for (String token : tokens) {
            String[] parts = token.split(":", 2);
            if (parts.length == 2) {
                String key = parts[0].trim().replaceAll("^\"|\"$", "");
                String value = parts[1].trim();
                result.put(key, parseValue(value));
            }
        }

        return result;
    }

    private static List<Object> parseArrayInternal(String json) {
        List<Object> result = new ArrayList<>();
        List<String> tokens = tokenize(json, '[', ']');

        for (String token : tokens) {
            result.add(parseValue(token.trim()));
        }

        return result;
    }

    private static Object parseValue(String value) {
        value = value.trim();

        if (value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1)
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                    .replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\t", "\t")
                    .replace("\\b", "\b")
                    .replace("\\f", "\f");
        } else if (value.equals("true")) {
            return true;
        } else if (value.equals("false")) {
            return false;
        } else if (value.equals("null")) {
            return null;
        } else if (value.startsWith("{") && value.endsWith("}")) {
            return parseObject(value.substring(1, value.length() - 1));
        } else if (value.startsWith("[") && value.endsWith("]")) {
            return parseArrayInternal(value.substring(1, value.length() - 1));
        } else {
            try {
                if (value.contains(".") || value.contains("e") || value.contains("E")) {
                    return Double.parseDouble(value);
                } else {
                    return Integer.parseInt(value);
                }
            } catch (NumberFormatException e) {
                return value;
            }
        }
    }

    private static List<String> tokenize(String json, char openChar, char closeChar) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        boolean inString = false;
        char quoteChar = '\0';

        for (char c : json.toCharArray()) {
            if ((c == '"' || c == '\'') && (current.length() == 0 || current.charAt(current.length() - 1) != '\\')) {
                if (!inString) {
                    inString = true;
                    quoteChar = c;
                } else if (c == quoteChar) {
                    inString = false;
                }
            }

            if (!inString) {
                if (c == openChar || c == '[') {
                    depth++;
                } else if (c == closeChar || c == ']') {
                    depth--;
                } else if (c == ',' && depth == 0) {
                    if (current.length() > 0) {
                        tokens.add(current.toString());
                    }
                    current = new StringBuilder();
                    continue;
                }
            }

            current.append(c);
        }

        if (current.length() > 0) {
            tokens.add(current.toString());
        }

        return tokens;
    }

    public static String prettyPrint(Object obj, int indent) {
        return prettyPrint(obj, indent, 0);
    }

    private static String prettyPrint(Object obj, int indent, int currentIndent) {
        String spaces = " ".repeat(currentIndent);

        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            if (map.isEmpty()) {
                return "{}";
            }

            StringBuilder sb = new StringBuilder("{\n");
            boolean first = true;

            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    sb.append(",\n");
                }
                first = false;

                sb.append(spaces).append(" ".repeat(indent))
                        .append("\"").append(escape(entry.getKey().toString())).append("\": ")
                        .append(prettyPrint(entry.getValue(), indent, currentIndent + indent));
            }

            sb.append("\n").append(spaces).append("}");
            return sb.toString();

        } else if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            if (list.isEmpty()) {
                return "[]";
            }

            StringBuilder sb = new StringBuilder("[\n");
            boolean first = true;

            for (Object item : list) {
                if (!first) {
                    sb.append(",\n");
                }
                first = false;

                sb.append(spaces).append(" ".repeat(indent))
                        .append(prettyPrint(item, indent, currentIndent + indent));
            }

            sb.append("\n").append(spaces).append("]");
            return sb.toString();

        } else {
            return toJSON(obj);
        }
    }

    public interface JSONable {
        String toJSON();
    }
}