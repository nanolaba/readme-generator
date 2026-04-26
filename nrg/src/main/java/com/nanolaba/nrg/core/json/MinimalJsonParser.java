package com.nanolaba.nrg.core.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal JSON parser producing nested {@link Map}/{@link List} trees with
 * {@link String}, {@link Long}, {@link Double}, {@link Boolean}, or {@code null} leaves.
 *
 * <p>Scope: just enough to parse {@code package.json} and resolve dotted-path lookups.
 * Strict about syntax — trailing commas and unknown literals throw
 * {@link IllegalArgumentException}. Object key order is preserved.
 */
public final class MinimalJsonParser {

    private MinimalJsonParser() {/**/}

    public static Object parse(String text) {
        Cursor c = new Cursor(text);
        c.skipWhitespace();
        Object value = parseValue(c);
        c.skipWhitespace();
        if (!c.eof()) {
            throw c.error("unexpected trailing content");
        }
        return value;
    }

    private static Object parseValue(Cursor c) {
        c.skipWhitespace();
        if (c.eof()) {
            throw c.error("unexpected end of input");
        }
        char ch = c.peek();
        switch (ch) {
            case '{':
                return parseObject(c);
            case '[':
                return parseArray(c);
            case '"':
                return parseString(c);
            case 't':
            case 'f':
                return parseBoolean(c);
            case 'n':
                return parseNull(c);
            default:
                if (ch == '-' || (ch >= '0' && ch <= '9')) {
                    return parseNumber(c);
                }
                throw c.error("unexpected character '" + ch + "'");
        }
    }

    private static Map<String, Object> parseObject(Cursor c) {
        c.expect('{');
        Map<String, Object> map = new LinkedHashMap<>();
        c.skipWhitespace();
        if (c.tryConsume('}')) {
            return map;
        }
        while (true) {
            c.skipWhitespace();
            String key = parseString(c);
            c.skipWhitespace();
            c.expect(':');
            Object value = parseValue(c);
            map.put(key, value);
            c.skipWhitespace();
            if (c.tryConsume(',')) {
                c.skipWhitespace();
                if (!c.eof() && c.peek() == '}') {
                    throw c.error("trailing comma in object");
                }
                continue;
            }
            c.expect('}');
            return map;
        }
    }

    private static List<Object> parseArray(Cursor c) {
        c.expect('[');
        List<Object> list = new ArrayList<>();
        c.skipWhitespace();
        if (c.tryConsume(']')) {
            return list;
        }
        while (true) {
            list.add(parseValue(c));
            c.skipWhitespace();
            if (c.tryConsume(',')) {
                c.skipWhitespace();
                if (!c.eof() && c.peek() == ']') {
                    throw c.error("trailing comma in array");
                }
                continue;
            }
            c.expect(']');
            return list;
        }
    }

    private static String parseString(Cursor c) {
        c.expect('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (c.eof()) {
                throw c.error("unterminated string");
            }
            char ch = c.next();
            if (ch == '"') {
                return sb.toString();
            }
            if (ch == '\\') {
                if (c.eof()) {
                    throw c.error("dangling escape");
                }
                char esc = c.next();
                switch (esc) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'u':
                        if (c.remaining() < 4) {
                            throw c.error("short unicode escape");
                        }
                        String hex = c.take(4);
                        try {
                            sb.append((char) Integer.parseInt(hex, 16));
                        } catch (NumberFormatException e) {
                            throw c.error("bad unicode escape: " + hex);
                        }
                        break;
                    default:
                        throw c.error("unknown escape: \\" + esc);
                }
            } else {
                sb.append(ch);
            }
        }
    }

    private static Object parseNumber(Cursor c) {
        int start = c.pos();
        if (c.peek() == '-') {
            c.next();
        }
        boolean fraction = false;
        boolean exponent = false;
        while (!c.eof()) {
            char ch = c.peek();
            if (ch >= '0' && ch <= '9') {
                c.next();
            } else if (ch == '.') {
                fraction = true;
                c.next();
            } else if (ch == 'e' || ch == 'E') {
                exponent = true;
                c.next();
                if (!c.eof() && (c.peek() == '+' || c.peek() == '-')) {
                    c.next();
                }
            } else {
                break;
            }
        }
        String raw = c.slice(start, c.pos());
        try {
            if (fraction || exponent) {
                return Double.parseDouble(raw);
            }
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw c.error("bad number: " + raw);
        }
    }

    private static Boolean parseBoolean(Cursor c) {
        if (c.tryLiteral("true")) {
            return Boolean.TRUE;
        }
        if (c.tryLiteral("false")) {
            return Boolean.FALSE;
        }
        throw c.error("expected true/false");
    }

    private static Object parseNull(Cursor c) {
        if (c.tryLiteral("null")) {
            return null;
        }
        throw c.error("expected null");
    }

    private static final class Cursor {
        private final String text;
        private int pos;

        Cursor(String text) { this.text = text; }

        boolean eof() { return pos >= text.length(); }
        int pos() { return pos; }
        int remaining() { return text.length() - pos; }
        char peek() { return text.charAt(pos); }
        char next() { return text.charAt(pos++); }
        String take(int n) { String s = text.substring(pos, pos + n); pos += n; return s; }
        String slice(int from, int to) { return text.substring(from, to); }

        void skipWhitespace() {
            while (pos < text.length()) {
                char ch = text.charAt(pos);
                if (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r') {
                    pos++;
                } else {
                    break;
                }
            }
        }

        void expect(char ch) {
            if (eof() || text.charAt(pos) != ch) {
                throw error("expected '" + ch + "'");
            }
            pos++;
        }

        boolean tryConsume(char ch) {
            if (!eof() && text.charAt(pos) == ch) {
                pos++;
                return true;
            }
            return false;
        }

        boolean tryLiteral(String lit) {
            if (text.regionMatches(pos, lit, 0, lit.length())) {
                pos += lit.length();
                return true;
            }
            return false;
        }

        IllegalArgumentException error(String msg) {
            return new IllegalArgumentException("JSON parse error at offset " + pos + ": " + msg);
        }
    }
}
