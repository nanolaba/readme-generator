package com.nanolaba.nrg.core;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        private static final long serialVersionUID = 1L;

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

    /**
     * {@code a && b && c …} as one flat node. A left-leaning chain of binary nodes would make
     * evaluation recurse once per operator, so a long enough chain overflowed the stack even
     * though nothing in it is nested.
     */
    private static final class AndNode implements Node {
        private final List<Node> operands;

        AndNode(List<Node> operands) {
            this.operands = operands;
        }

        @Override
        public boolean eval(GeneratorConfig config, String language) {
            for (Node operand : operands) {
                if (!operand.eval(config, language)) return false;
            }
            return true;
        }
    }

    /** {@code a || b || c …} as one flat node, for the same reason as {@link AndNode}. */
    private static final class OrNode implements Node {
        private final List<Node> operands;

        OrNode(List<Node> operands) {
            this.operands = operands;
        }

        @Override
        public boolean eval(GeneratorConfig config, String language) {
            for (Node operand : operands) {
                if (operand.eval(config, language)) return true;
            }
            return false;
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
        /** Alternatives are tried in order, so {@code !=} wins over {@code !}. */
        private static final Pattern OPERATOR = Pattern.compile("&&|\\|\\||==|!=|[!(),]");
        /** A function name only counts as one when the next non-blank character opens its argument list. */
        private static final Pattern FUNCTION_CALL = Pattern.compile("(startsWith|endsWith)\\p{javaWhitespace}*\\(");
        private static final Map<String, TokenKind> OPERATORS = new HashMap<>();

        static {
            OPERATORS.put("&&", TokenKind.AND);
            OPERATORS.put("||", TokenKind.OR);
            OPERATORS.put("==", TokenKind.EQ);
            OPERATORS.put("!=", TokenKind.NEQ);
            OPERATORS.put("!", TokenKind.NOT);
            OPERATORS.put("(", TokenKind.LPAREN);
            OPERATORS.put(")", TokenKind.RPAREN);
            OPERATORS.put(",", TokenKind.COMMA);
        }

        private final String src;
        private final Matcher operator;
        private final Matcher functionCall;
        private int pos;

        Lexer(String src) {
            this.src = src;
            this.operator = OPERATOR.matcher(src);
            this.functionCall = FUNCTION_CALL.matcher(src);
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
            if (matchesAtPos(operator)) {
                pos = operator.end();
                return new Token(OPERATORS.get(operator.group()), operator.group());
            }
            if (src.startsWith("${", pos)) return readPlaceholder();
            char c = src.charAt(pos);
            if (c == '\'' || c == '"') return readQuoted(c);
            if (matchesAtPos(functionCall)) {
                pos = functionCall.end(1);
                return new Token(TokenKind.FUNCTION, functionCall.group(1));
            }
            return readBare();
        }

        private boolean matchesAtPos(Matcher matcher) {
            return matcher.region(pos, src.length()).lookingAt();
        }

        private Token readPlaceholder() {
            int start = pos;
            pos += 2; // skip ${
            for (int depth = 1; pos < src.length(); pos++) {
                char c = src.charAt(pos);
                if (c == '{') {
                    depth++;
                } else if (c == '}' && --depth == 0) {
                    pos++;
                    return new Token(TokenKind.PLACEHOLDER, src.substring(start, pos));
                }
            }
            throw new ParseException("Unterminated placeholder starting at position " + start);
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

        /**
         * Reached only where {@link #nextToken} found no other token, so the first character
         * always belongs to the bare string; it runs to the next place where one does start.
         */
        private Token readBare() {
            int start = pos;
            do {
                pos++;
            } while (pos < src.length() && !isTokenBoundary());
            return new Token(TokenKind.BARE, src.substring(start, pos).trim());
        }

        /** Whether one of the tokens {@link #nextToken} reads before falling back to a bare string starts at {@code pos}. */
        private boolean isTokenBoundary() {
            char c = src.charAt(pos);
            return matchesAtPos(operator) || src.startsWith("${", pos) || c == '\'' || c == '"' || matchesAtPos(functionCall);
        }

        private void skipWhitespace() {
            while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
                pos++;
            }
        }
    }

    // -------- Parser ----------

    private static final class Parser {
        /** Far beyond any hand-written condition, far below what the thread stack can take. */
        private static final int MAX_NESTING = 64;

        private final List<Token> tokens;
        private int idx;
        private int depth;

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
            Node first = parseAnd();
            if (peekKind() != TokenKind.OR) {
                return first;
            }
            List<Node> operands = new ArrayList<>();
            operands.add(first);
            while (peekKind() == TokenKind.OR) {
                consume();
                operands.add(parseAnd());
            }
            return new OrNode(operands);
        }

        private Node parseAnd() {
            Node first = parseUnary();
            if (peekKind() != TokenKind.AND) {
                return first;
            }
            List<Node> operands = new ArrayList<>();
            operands.add(first);
            while (peekKind() == TokenKind.AND) {
                consume();
                operands.add(parseUnary());
            }
            return new AndNode(operands);
        }

        private Node parseUnary() {
            if (peekKind() == TokenKind.NOT) {
                consume();
                enterNesting();
                try {
                    return new NotNode(parseUnary());
                } finally {
                    depth--;
                }
            }
            return parsePrimary();
        }

        /**
         * Both recursive productions — {@code '(' expression ')'} and {@code '!' unary} — go
         * through here. Without a bound, a condition of a few thousand {@code (} or {@code !}
         * ended the run with a StackOverflowError instead of an "invalid condition" message.
         */
        private void enterNesting() {
            if (++depth > MAX_NESTING) {
                throw new ParseException("Condition is nested too deeply (limit " + MAX_NESTING + ")");
            }
        }

        private Node parsePrimary() {
            TokenKind kind = peekKind();
            if (kind == null) {
                throw new ParseException("Unexpected end of condition");
            }
            if (kind == TokenKind.LPAREN) {
                consume();
                enterNesting();
                try {
                    Node inner = parseExpression();
                    expect(TokenKind.RPAREN);
                    return inner;
                } finally {
                    depth--;
                }
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
