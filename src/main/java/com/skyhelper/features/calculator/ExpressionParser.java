package com.skyhelper.features.calculator;

/**
 * Recursive-descent parser for basic math expressions.
 * Supports: +, -, *, /, parentheses, decimal numbers, unary +/-.
 * Respects standard operator precedence.
 */
public final class ExpressionParser {

    private final String input;
    private int pos;

    private ExpressionParser(String input) {
        this.input = input.replaceAll("\\s+", "");
        this.pos = 0;
    }

    /**
     * Evaluates the given expression string.
     * Returns null if the expression is invalid, incomplete, or produces Infinity/NaN.
     */
    public static Double evaluate(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return null;
        }
        try {
            ExpressionParser parser = new ExpressionParser(expression);
            double result = parser.parseExpression();
            if (parser.pos != parser.input.length()) {
                return null;
            }
            if (Double.isInfinite(result) || Double.isNaN(result)) {
                return null;
            }
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns true if the given string looks like a math expression
     * (contains at least one digit and at least one operator or parenthesis).
     */
    public static boolean isMathExpression(String input) {
        if (input == null || input.trim().isEmpty()) return false;
        String trimmed = input.trim();
        boolean hasDigit = false;
        boolean hasOperator = false;
        for (char c : trimmed.toCharArray()) {
            if (Character.isDigit(c)) hasDigit = true;
            if (c == '+' || c == '-' || c == '*' || c == '/' || c == '(' || c == ')') hasOperator = true;
        }
        return hasDigit && hasOperator;
    }

    private double parseExpression() {
        double result = parseTerm();
        while (pos < input.length()) {
            char op = input.charAt(pos);
            if (op == '+' || op == '-') {
                pos++;
                double right = parseTerm();
                result = (op == '+') ? result + right : result - right;
            } else {
                break;
            }
        }
        return result;
    }

    private double parseTerm() {
        double result = parseFactor();
        while (pos < input.length()) {
            char op = input.charAt(pos);
            if (op == '*' || op == '/') {
                pos++;
                double right = parseFactor();
                result = (op == '*') ? result * right : result / right;
            } else {
                break;
            }
        }
        return result;
    }

    private double parseFactor() {
        if (pos < input.length() && input.charAt(pos) == '(') {
            pos++;
            double result = parseExpression();
            if (pos < input.length() && input.charAt(pos) == ')') {
                pos++;
            } else {
                throw new RuntimeException("Missing ')'");
            }
            return result;
        }

        if (pos < input.length() && (input.charAt(pos) == '-' || input.charAt(pos) == '+')) {
            char sign = input.charAt(pos);
            pos++;
            double value = parseFactor();
            return sign == '-' ? -value : value;
        }

        return parseNumber();
    }

    private double parseNumber() {
        int start = pos;
        while (pos < input.length() && (Character.isDigit(input.charAt(pos)) || input.charAt(pos) == '.')) {
            pos++;
        }
        if (start == pos) {
            throw new RuntimeException("Expected number at position " + pos);
        }
        return Double.parseDouble(input.substring(start, pos));
    }
}
