package cn.blockforge.generated.generatedmod.match;

import cn.blockforge.generated.generatedmod.map.MapRegion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/** Evaluates map region activation rules against the current match context. */
public final class MapRegionActivation {
    public static final Context INACTIVE = new Context(false, GameMode.TEAM_DEATHMATCH, 0, 0, 0);

    private MapRegionActivation() {
    }

    public static boolean isActive(MapRegion region, Context context) {
        if (region == null || context == null) return false;
        return switch (region.activation()) {
            case ALWAYS -> true;
            case MATCH_ONLY -> context.matchActive();
            case MODE -> sameMode(context.mode().name(), region.activationValue());
            case CONDITION -> new ConditionParser(region.activationValue(), context).parse();
        };
    }

    public static List<MapRegion> activeRegions(Collection<MapRegion> regions, Context context) {
        if (regions == null || regions.isEmpty()) return List.of();
        List<MapRegion> result = new ArrayList<>();
        for (MapRegion region : regions) {
            if (isActive(region, context)) result.add(region);
        }
        return List.copyOf(result);
    }

    private static boolean sameMode(String left, String right) {
        if (left == null || right == null) return false;
        if (left.equalsIgnoreCase(right)) return true;
        return modeKey(left).equals(modeKey(right));
    }

    private static String modeKey(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
        return switch (normalized) {
            case "tdm", "team_deathmatch", "团队竞技" -> "team_deathmatch";
            case "sd", "search_destroy", "bomb", "爆破模式" -> "search_destroy";
            case "last_standing", "歼灭竞技" -> "last_standing";
            default -> normalized;
        };
    }

    public record Context(boolean matchActive, GameMode mode, int round,
                          int teamCount, int playerCount) {
        public Context {
            mode = mode == null ? GameMode.TEAM_DEATHMATCH : mode;
            round = Math.max(0, round);
            teamCount = Math.max(0, teamCount);
            playerCount = Math.max(0, playerCount);
        }
    }

    private static final class ConditionParser {
        private final String text;
        private final Context context;
        private int index;

        private ConditionParser(String text, Context context) {
            this.text = text == null ? "" : text;
            this.context = context;
        }

        private boolean parse() {
            if (text.isBlank()) return false;
            try {
                skip();
                Object value = parseOr();
                skip();
                return index >= text.length() && value instanceof Boolean result && result;
            } catch (ParseException ignored) {
                return false;
            }
        }

        private Object parseOr() {
            Object left = parseAnd();
            while (match("||")) {
                Object right = parseAnd();
                left = requireBoolean(left) || requireBoolean(right);
            }
            return left;
        }

        private Object parseAnd() {
            Object left = parseComparison();
            while (match("&&")) {
                Object right = parseComparison();
                left = requireBoolean(left) && requireBoolean(right);
            }
            return left;
        }

        private Object parseComparison() {
            Object left = parseUnary();
            String operator = readOperator();
            if (operator == null) return left;
            Object right = parseUnary();
            return compare(left, operator, right);
        }

        private Object parseUnary() {
            if (match("!")) {
                return !requireBoolean(parseUnary());
            }
            if (match("-")) {
                Object value = parseUnary();
                if (!(value instanceof Integer number)) throw new ParseException();
                return -number;
            }
            return parsePrimary();
        }

        private Object parsePrimary() {
            skip();
            if (match("(")) {
                Object value = parseOr();
                if (!match(")")) throw new ParseException();
                return value;
            }
            if (match("\"")) {
                StringBuilder value = new StringBuilder();
                while (index < text.length() && text.charAt(index) != '"') {
                    value.append(text.charAt(index++));
                }
                if (index >= text.length() || !match("\"")) throw new ParseException();
                return value.toString();
            }
            if (index < text.length() && Character.isDigit(text.charAt(index))) {
                int start = index;
                while (index < text.length() && Character.isDigit(text.charAt(index))) index++;
                try {
                    return Integer.parseInt(text.substring(start, index));
                } catch (NumberFormatException exception) {
                    throw new ParseException();
                }
            }
            if (index < text.length() && Character.isJavaIdentifierStart(text.charAt(index))) {
                int start = index;
                while (index < text.length() && Character.isJavaIdentifierPart(text.charAt(index))) index++;
                return variable(text.substring(start, index));
            }
            throw new ParseException();
        }

        private Object variable(String name) {
            return switch (name.toLowerCase(Locale.ROOT)) {
                case "true" -> Boolean.TRUE;
                case "false" -> Boolean.FALSE;
                case "match", "match_active" -> context.matchActive();
                case "mode" -> context.mode().name();
                case "round" -> context.round();
                case "teams", "team_count" -> context.teamCount();
                case "players", "player_count" -> context.playerCount();
                default -> throw new ParseException();
            };
        }

        private Object compare(Object left, String operator, Object right) {
            if ("==".equals(operator) || "!=".equals(operator)) {
                boolean equal;
                if (left instanceof Boolean && right instanceof Boolean) {
                    equal = left.equals(right);
                } else if (left instanceof Integer && right instanceof Integer) {
                    equal = left.equals(right);
                } else if (left instanceof String && right instanceof String) {
                    equal = sameMode((String) left, (String) right);
                } else {
                    throw new ParseException();
                }
                return "==".equals(operator) == equal;
            }
            if (!(left instanceof Integer leftNumber) || !(right instanceof Integer rightNumber)) {
                throw new ParseException();
            }
            return switch (operator) {
                case "<" -> leftNumber < rightNumber;
                case "<=" -> leftNumber <= rightNumber;
                case ">" -> leftNumber > rightNumber;
                case ">=" -> leftNumber >= rightNumber;
                default -> throw new ParseException();
            };
        }

        private boolean requireBoolean(Object value) {
            if (!(value instanceof Boolean result)) throw new ParseException();
            return result;
        }

        private String readOperator() {
            skip();
            if (match("==")) return "==";
            if (match("!=")) return "!=";
            if (match("<=")) return "<=";
            if (match(">=")) return ">=";
            if (match("<")) return "<";
            if (match(">")) return ">";
            return null;
        }

        private boolean match(String token) {
            skip();
            if (!text.startsWith(token, index)) return false;
            index += token.length();
            skip();
            return true;
        }

        private void skip() {
            while (index < text.length() && Character.isWhitespace(text.charAt(index))) index++;
        }
    }

    private static final class ParseException extends RuntimeException {
    }
}
