package com.example.l4j.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 计算器工具：内置递归下降解析器，
 * 支持 + - * /、小数、括号和一元负号，不依赖脚本引擎。
 */
@Component
public class CalculatorTools {

    @Tool("计算数学表达式的值，支持加(+)、减(-)、乘(*)、除(/)、小数和括号，例如 (1+2)*3。"
            + "输入参数 expression 为数学表达式字符串。")
    public String calculate(@P("数学表达式字符串，例如 (1+2)*3") String expression) {
        try {
            double result = new Parser(expression).parse();
            if (Double.isNaN(result) || Double.isInfinite(result)) {
                return "计算结果无效（可能除数为 0）";
            }
            if (result == Math.rint(result) && !String.valueOf((long) result).contains("E")) {
                return String.valueOf((long) result);
            }
            return String.format(Locale.ROOT, "%.6f", result)
                    .replaceAll("0+$", "")
                    .replaceAll("\\.$", "");
        } catch (RuntimeException e) {
            return "表达式无法计算：" + e.getMessage();
        }
    }

    private static final class Parser {
        private final String input;
        private int pos;

        private Parser(String input) {
            this.input = input == null ? "" : input;
        }

        private double parse() {
            double value = parseExpression();
            skipSpaces();
            if (pos < input.length()) {
                throw new IllegalArgumentException("无法识别的字符: " + input.charAt(pos));
            }
            return value;
        }

        private double parseExpression() {
            double value = parseTerm();
            while (true) {
                skipSpaces();
                if (consume('+')) {
                    value += parseTerm();
                } else if (consume('-')) {
                    value -= parseTerm();
                } else {
                    return value;
                }
            }
        }

        private double parseTerm() {
            double value = parseFactor();
            while (true) {
                skipSpaces();
                if (consume('*')) {
                    value *= parseFactor();
                } else if (consume('/')) {
                    value /= parseFactor();
                } else {
                    return value;
                }
            }
        }

        private double parseFactor() {
            skipSpaces();
            if (consume('+')) {
                return parseFactor();
            }
            if (consume('-')) {
                return -parseFactor();
            }
            if (consume('(')) {
                double value = parseExpression();
                skipSpaces();
                if (!consume(')')) {
                    throw new IllegalArgumentException("缺少右括号");
                }
                return value;
            }
            int start = pos;
            while (pos < input.length()) {
                char c = input.charAt(pos);
                if (Character.isDigit(c) || c == '.') {
                    pos++;
                } else {
                    break;
                }
            }
            if (start == pos) {
                throw new IllegalArgumentException("此处需要数字");
            }
            return Double.parseDouble(input.substring(start, pos));
        }

        private void skipSpaces() {
            while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
                pos++;
            }
        }

        private boolean consume(char expected) {
            skipSpaces();
            if (pos < input.length() && input.charAt(pos) == expected) {
                pos++;
                return true;
            }
            return false;
        }
    }
}
