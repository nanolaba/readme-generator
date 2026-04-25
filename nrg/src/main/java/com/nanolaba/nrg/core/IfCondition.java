package com.nanolaba.nrg.core;

import java.util.*;

/**
 * Parser and evaluator for the {@code ${widget:if(cond='...')}} expression DSL.
 *
 * <p>Grammar (precedence low → high):
 * <pre>
 *   expression   := or_expr
 *   or_expr      := and_expr ( '||' and_expr )*
 *   and_expr     := unary_expr ( '&amp;&amp;' unary_expr )*
 *   unary_expr   := '!' unary_expr | primary
 *   primary      := '(' expression ')' | function_call | comparison_or_truthy
 *   function_call:= ('startsWith' | 'endsWith') '(' operand ',' operand ')'
 *   comparison_or_truthy := operand ( ('==' | '!=') operand )?
 *   operand      := placeholder | quoted_string | bare_string
 *   placeholder  := '${' ... '}'
 * </pre>
 *
 * <p>Two-phase: tokenisation captures {@code ${...}} as a single opaque token, so values
 * containing {@code &&}, {@code ||}, etc. cannot be reinterpreted as operators. Resolution
 * happens at evaluation time only.
 */
public final class IfCondition {

    private static final Set<String> FUNCTIONS = Collections.unmodifiableSet(
            new java.util.HashSet<>(Arrays.asList("startsWith", "endsWith")));

    private IfCondition() {/* utility */}

    public static boolean evaluate(String cond, GeneratorConfig config, String language) {
        if (cond == null) {
            return false;
        }
        String trimmed = cond.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        List<Token> tokens = new Lexer(cond).lex();
        Parser parser = new Parser(tokens);
        Node ast = parser.parseExpression();
        if (parser.hasMore()) {
            throw new ParseException("Unexpected trailing input in condition: '" + cond + "'");
        }
        return ast.eval(config, language);
    }

    public static final class ParseException extends RuntimeException {
        public ParseException(String message) {
            super(message);
        }
    }

    // -------- AST ----------

    private interface Node {
        boolean eval(GeneratorConfig config, String language);
    }

    private interface OperandNode {
        String resolve(GeneratorConfig config, String language);

        /**
         * Whether the value should be trimmed when participating in equality. Quoted operands return false.
         */
        boolean trimForComparison();
    }

    private static final class TruthyNode implements Node {
        private final OperandNode operand;

        TruthyNode(OperandNode operand) {
            this.operand = operand;
        }

        @Override
        public boolean eval(GeneratorConfig config, String language) {
            return !operand.resolve(config, language).trim().isEmpty();
        }
    }

    private static final class NotNode implements Node {
        private final Node inner;

        NotNode(Node inner) {
            this.inner = inner;
        }

        @Override
        public boolean eval(GeneratorConfig config, String language) {
            return !inner.eval(config, language);
        }
    }

    private static final class AndNode implements Node {
        private final Node left, right;

        AndNode(Node left, Node right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public boolean eval(GeneratorConfig config, String language) {
            if (!left.eval(config, language)) return false;
            return right.eval(config, language);
        }
    }

    private static final class OrNode implements Node {
        private final Node left, right;

        OrNode(Node left, Node right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public boolean eval(GeneratorConfig config, String language) {
            if (left.eval(config, language)) return true;
            return right.eval(config, language);
        }
    }

    private static final class EqNode implements Node {
        private final OperandNode left, right;
        private final boolean negate;

        EqNode(OperandNode left, OperandNode right, boolean negate) {
            this.left = left;
            this.right = right;
            this.negate = negate;
        }

        @Override
        public boolean eval(GeneratorConfig config, String language) {
            boolean trim = left.trimForComparison() && right.trimForComparison();
            String l = left.resolve(config, language);
            String r = right.resolve(config, language);
            if (trim) {
                l = l.trim();
                r = r.trim();
            }
            boolean equal = l.equals(r);
            return negate != equal;
        }
    }

    private static final class FunctionNode implements Node {
        private final String name;
        private final OperandNode haystack, needle;

        FunctionNode(String name, OperandNode haystack, OperandNode needle) {
            this.name = name;
            this.haystack = haystack;
            this.needle = needle;
        }

        @Override
        public boolean eval(GeneratorConfig config, String language) {
            String h = haystack.resolve(config, language);
            String n = needle.resolve(config, language);
            switch (name) {
                case "startsWith":
                    return h.startsWith(n);
                case "endsWith":
                    return h.endsWith(n);
                default:
                    throw new ParseException("Unknown function: " + name);
            }
        }
    }

    private static final class LiteralOperand implements OperandNode {
        private final String text;

        LiteralOperand(String text) {
            this.text = text;
        }

        @Override
        public String resolve(GeneratorConfig config, String language) {
            return text;
        }

        @Override
        public boolean trimForComparison() {
            return true;
        }
    }

    private static final class PlaceholderOperand implements OperandNode {
        private final String raw;       // including surrounding ${...}

        PlaceholderOperand(String raw) {
            this.raw = raw;
        }

        @Override
        public String resolve(GeneratorConfig config, String language) {
            TemplateLine tl = new TemplateLine(config, raw, 0);
            String result = tl.fillLineWithProperties(language);
            return result == null ? "" : result;
        }

        @Override
        public boolean trimForComparison() {
            return true;
        }
    }

    /**
     * Quoted operand: substitutes {@code ${…}} references inside but keeps everything else verbatim, including whitespace.
     */
    private static final class QuotedOperand implements OperandNode {
        private final String text;

        QuotedOperand(String text) {
            this.text = text;
        }

        @Override
        public String resolve(GeneratorConfig config, String language) {
            if (text.indexOf("${") < 0) {
                return text;
            }
            TemplateLine tl = new TemplateLine(config, text, 0);
            String result = tl.fillLineWithProperties(language);
            return result == null ? text : result;
        }

        @Override
        public boolean trimForComparison() {
            return false;
        }
    }

    // -------- Lexer ----------

    private enum TokenKind {
        OR, AND, NOT, EQ, NEQ, LPAREN, RPAREN, COMMA,
        FUNCTION, PLACEHOLDER, QUOTED, BARE, EOF
    }

    private static final class Token {
        final TokenKind kind;
        final String text;

        Token(TokenKind kind, String text) {
            this.kind = kind;
            this.text = text;
        }
    }

    private static final class Lexer {
        private final String src;
        private int pos;

        Lexer(String src) {
            this.src = src;
            this.pos = 0;
        }

        List<Token> lex() {
            List<Token> tokens = new ArrayList<>();
            while (true) {
                skipWhitespace();
                if (pos >= src.length()) break;
                Token t = nextToken();
                tokens.add(t);
            }
            return tokens;
        }

        private Token nextToken() {
            char c = src.charAt(pos);
            char n = peek(1);

            if (c == '(') {
                pos++;
                return new Token(TokenKind.LPAREN, "(");
            }
            if (c == ')') {
                pos++;
                return new Token(TokenKind.RPAREN, ")");
            }
            if (c == ',') {
                pos++;
                return new Token(TokenKind.COMMA, ",");
            }
            if (c == '&' && n == '&') {
                pos += 2;
                return new Token(TokenKind.AND, "&&");
            }
            if (c == '|' && n == '|') {
                pos += 2;
                return new Token(TokenKind.OR, "||");
            }
            if (c == '=' && n == '=') {
                pos += 2;
                return new Token(TokenKind.EQ, "==");
            }
            if (c == '!' && n == '=') {
                pos += 2;
                return new Token(TokenKind.NEQ, "!=");
            }
            if (c == '!') {
                pos++;
                return new Token(TokenKind.NOT, "!");
            }
            if (c == '$' && n == '{') return readPlaceholder();
            if (c == '\'' || c == '"') return readQuoted(c);

            // function name (only if directly followed by '(' modulo whitespace)
            for (String fn : FUNCTIONS) {
                if (matchesIdent(fn)) {
                    int after = pos + fn.length();
                    int afterWS = after;
                    while (afterWS < src.length() && Character.isWhitespace(src.charAt(afterWS))) afterWS++;
                    if (afterWS < src.length() && src.charAt(afterWS) == '(') {
                        pos = after;
                        return new Token(TokenKind.FUNCTION, fn);
                    }
                }
            }

            return readBare();
        }

        private boolean matchesIdent(String name) {
            if (!src.regionMatches(pos, name, 0, name.length())) return false;
            int end = pos + name.length();
            if (end >= src.length()) return true;
            char after = src.charAt(end);
            return !(Character.isLetterOrDigit(after) || after == '_');
        }

        private Token readPlaceholder() {
            int start = pos;
            pos += 2; // skip ${
            int depth = 1;
            while (pos < src.length() && depth > 0) {
                char c = src.charAt(pos);
                if (c == '{') depth++;
                else if (c == '}') depth--;
                if (depth == 0) break;
                pos++;
            }
            if (pos >= src.length()) {
                throw new ParseException("Unterminated placeholder starting at position " + start);
            }
            pos++; // consume closing }
            return new Token(TokenKind.PLACEHOLDER, src.substring(start, pos));
        }

        private Token readQuoted(char quote) {
            int start = pos;
            pos++; // skip opening quote
            StringBuilder sb = new StringBuilder();
            while (pos < src.length()) {
                char c = src.charAt(pos);
                if (c == quote) {
                    if (pos + 1 < src.length() && src.charAt(pos + 1) == quote) {
                        sb.append(quote);
                        pos += 2;
                        continue;
                    }
                    pos++; // consume closing quote
                    return new Token(TokenKind.QUOTED, sb.toString());
                }
                sb.append(c);
                pos++;
            }
            throw new ParseException("Unterminated quoted string starting at position " + start);
        }

        private Token readBare() {
            int start = pos;
            while (pos < src.length() && !isTokenBoundary()) {
                pos++;
            }
            String raw = src.substring(start, pos);
            return new Token(TokenKind.BARE, raw.trim());
        }

        private boolean isTokenBoundary() {
            char c = src.charAt(pos);
            char n = peek(1);
            if (c == '(' || c == ')' || c == ',') return true;
            if (c == '&' && n == '&') return true;
            if (c == '|' && n == '|') return true;
            if (c == '=' && n == '=') return true;
            if (c == '!' && n == '=') return true;
            if (c == '!') return true;
            if (c == '$' && n == '{') return true;
            if (c == '\'' || c == '"') return true;
            for (String fn : FUNCTIONS) {
                if (matchesIdent(fn)) {
                    int after = pos + fn.length();
                    int afterWS = after;
                    while (afterWS < src.length() && Character.isWhitespace(src.charAt(afterWS))) afterWS++;
                    if (afterWS < src.length() && src.charAt(afterWS) == '(') {
                        return true;
                    }
                }
            }
            return false;
        }

        private void skipWhitespace() {
            while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
                pos++;
            }
        }

        private char peek(int offset) {
            int idx = pos + offset;
            return idx < src.length() ? src.charAt(idx) : '\0';
        }
    }

    // -------- Parser ----------

    private static final class Parser {
        private final List<Token> tokens;
        private int idx;

        Parser(List<Token> tokens) {
            this.tokens = tokens;
            this.idx = 0;
        }

        boolean hasMore() {
            return idx < tokens.size();
        }

        Node parseExpression() {
            return parseOr();
        }

        private Node parseOr() {
            Node left = parseAnd();
            while (peekKind() == TokenKind.OR) {
                consume();
                Node right = parseAnd();
                left = new OrNode(left, right);
            }
            return left;
        }

        private Node parseAnd() {
            Node left = parseUnary();
            while (peekKind() == TokenKind.AND) {
                consume();
                Node right = parseUnary();
                left = new AndNode(left, right);
            }
            return left;
        }

        private Node parseUnary() {
            if (peekKind() == TokenKind.NOT) {
                consume();
                return new NotNode(parseUnary());
            }
            return parsePrimary();
        }

        private Node parsePrimary() {
            TokenKind kind = peekKind();
            if (kind == null) {
                throw new ParseException("Unexpected end of condition");
            }
            if (kind == TokenKind.LPAREN) {
                consume();
                Node inner = parseExpression();
                expect(TokenKind.RPAREN);
                return inner;
            }
            if (kind == TokenKind.FUNCTION) {
                return parseFunctionCall();
            }
            return parseComparisonOrTruthy();
        }

        private Node parseFunctionCall() {
            Token name = consume();
            expect(TokenKind.LPAREN);
            OperandNode a = parseOperand();
            expect(TokenKind.COMMA);
            OperandNode b = parseOperand();
            expect(TokenKind.RPAREN);
            return new FunctionNode(name.text, a, b);
        }

        private Node parseComparisonOrTruthy() {
            OperandNode left = parseOperand();
            TokenKind next = peekKind();
            if (next == TokenKind.EQ) {
                consume();
                OperandNode right = parseOperand();
                return new EqNode(left, right, false);
            }
            if (next == TokenKind.NEQ) {
                consume();
                OperandNode right = parseOperand();
                return new EqNode(left, right, true);
            }
            return new TruthyNode(left);
        }

        private OperandNode parseOperand() {
            TokenKind kind = peekKind();
            if (kind == null) {
                throw new ParseException("Expected operand, got end of input");
            }
            Token t = consume();
            switch (kind) {
                case PLACEHOLDER:
                    return new PlaceholderOperand(t.text);
                case QUOTED:
                    return new QuotedOperand(t.text);
                case BARE:
                    if (t.text.isEmpty()) {
                        throw new ParseException("Empty operand");
                    }
                    return new LiteralOperand(t.text);
                default:
                    throw new ParseException("Expected operand, got " + kind + " ('" + t.text + "')");
            }
        }

        private TokenKind peekKind() {
            return idx < tokens.size() ? tokens.get(idx).kind : null;
        }

        private Token consume() {
            return tokens.get(idx++);
        }

        private void expect(TokenKind kind) {
            if (idx >= tokens.size() || tokens.get(idx).kind != kind) {
                String got = idx >= tokens.size() ? "end of input" : tokens.get(idx).kind + " ('" + tokens.get(idx).text + "')";
                throw new ParseException("Expected " + kind + " but got " + got);
            }
            idx++;
        }
    }
}
