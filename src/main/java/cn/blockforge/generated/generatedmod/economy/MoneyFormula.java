package cn.blockforge.generated.generatedmod.economy;

import java.util.Locale;
import java.util.Map;
import java.util.function.DoubleBinaryOperator;

/** Small expression parser for economy rules; all inputs and results are clamped to safe integers later. */
public final class MoneyFormula {
    private MoneyFormula() { }

    public static int evaluate(String expression, Map<String, Double> variables) {
        Parser parser = new Parser(expression == null ? "0" : expression, variables == null
                ? Map.of() : variables);
        double value = parser.parseExpression();
        if (!parser.end()) throw new IllegalArgumentException("公式含有多余内容: " + expression);
        if (!Double.isFinite(value)) return 0;
        return (int) Math.round(value);
    }

    private static final class Parser {
        private final String text;
        private final Map<String, Double> variables;
        private int position;

        private Parser(String text, Map<String, Double> variables) {
            this.text = text;
            this.variables = variables;
        }

        private boolean end() {
            skipSpace();
            return position >= text.length();
        }

        private double parseExpression() {
            double value = parseTerm();
            while (true) {
                skipSpace();
                if (take('+')) value += parseTerm();
                else if (take('-')) value -= parseTerm();
                else return value;
            }
        }

        private double parseTerm() {
            double value = parseFactor();
            while (true) {
                skipSpace();
                if (take('*')) value *= parseFactor();
                else if (take('/')) {
                    double divisor = parseFactor();
                    value = Math.abs(divisor) < 0.000001D ? 0 : value / divisor;
                } else if (take('%')) {
                    double divisor = parseFactor();
                    value = Math.abs(divisor) < 0.000001D ? 0 : value % divisor;
                } else return value;
            }
        }

        private double parseFactor() {
            skipSpace();
            if (take('+')) return parseFactor();
            if (take('-')) return -parseFactor();
            return parsePrimary();
        }

        private double parsePrimary() {
            skipSpace();
            if (take('(')) {
                double value = parseExpression();
                skipSpace();
                if (!take(')')) throw expected(")");
                return value;
            }
            if (position < text.length() && (Character.isDigit(text.charAt(position))
                    || text.charAt(position) == '.')) {
                int start = position;
                while (position < text.length() && Character.isDigit(text.charAt(position))) position++;
                if (position < text.length() && text.charAt(position) == '.') {
                    position++;
                    while (position < text.length() && Character.isDigit(text.charAt(position))) position++;
                }
                return Double.parseDouble(text.substring(start, position));
            }
            int start = position;
            while (position < text.length() && (Character.isLetterOrDigit(text.charAt(position))
                    || text.charAt(position) == '_')) position++;
            if (position > start) {
                String word = text.substring(start, position).toLowerCase(Locale.ROOT);
                if (take('(')) {
                    double first = parseExpression();
                    double second = 0.0D;
                    skipSpace();
                    if (take(',')) second = parseExpression();
                    skipSpace();
                    if (!take(')')) throw expected(")");
                    return function(word, first, second);
                }
                Double value = variables.get(word);
                if (value == null) throw new IllegalArgumentException("未知公式变量: " + word);
                return value;
            }
            if (position >= text.length()) throw expected("数字或变量");
            throw expected("数字或变量");
        }

        private double function(String name, double first, double second) {
            return switch (name) {
                case "min" -> Math.min(first, second);
                case "max" -> Math.max(first, second);
                case "floor" -> Math.floor(first);
                case "ceil" -> Math.ceil(first);
                case "abs" -> Math.abs(first);
                default -> throw new IllegalArgumentException("未知公式函数: " + name);
            };
        }

        private void skipSpace() {
            while (position < text.length() && Character.isWhitespace(text.charAt(position))) position++;
        }

        private boolean take(char symbol) {
            if (position < text.length() && text.charAt(position) == symbol) {
                position++;
                return true;
            }
            return false;
        }

        private IllegalArgumentException expected(String what) {
            return new IllegalArgumentException("公式语法错误，位置 " + position + "，期望 " + what);
        }
    }
}
