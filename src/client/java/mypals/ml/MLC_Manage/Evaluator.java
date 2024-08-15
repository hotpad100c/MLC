package mypals.ml.MLC_Manage;

import java.util.Stack;

public class Evaluator {

    // Method to evaluate a mathematical expression
    public static Number evaluateExpression(String expression, boolean isInt) throws NumberFormatException {
        // Remove all whitespace from the expression
        expression = expression.replaceAll("\\s+", "");

        // Parse and evaluate the expression
        double result = parseExpression(expression);

        if (isInt) {
            return (int) result; // Convert to integer if needed
        } else {
            return (float) result; // Convert to float if needed
        }
    }

    // Method to parse and evaluate the expression
    private static double parseExpression(String expression) throws NumberFormatException {
        // Use stacks to handle numbers and operators
        Stack<Double> numbers = new Stack<>();
        Stack<Character> operators = new Stack<>();

        int i = 0;
        while (i < expression.length()) {
            char c = expression.charAt(i);

            // If the character is a digit or a decimal point, parse the number
            if (Character.isDigit(c) || c == '.') {
                StringBuilder number = new StringBuilder();
                while (i < expression.length() && (Character.isDigit(expression.charAt(i)) || expression.charAt(i) == '.')) {
                    number.append(expression.charAt(i++));
                }
                numbers.push(Double.parseDouble(number.toString()));
            } else if (c == '(') {
                operators.push(c);
                i++;
            } else if (c == ')') {
                while (operators.peek() != '(') {
                    numbers.push(applyOperator(operators.pop(), numbers.pop(), numbers.pop()));
                }
                operators.pop(); // Remove '('
                i++;
            } else if (c == '+' || c == '-' || c == '*' || c == '/') {
                while (!operators.isEmpty() && precedence(c) <= precedence(operators.peek())) {
                    numbers.push(applyOperator(operators.pop(), numbers.pop(), numbers.pop()));
                }
                operators.push(c);
                i++;
            } else {
                throw new NumberFormatException("Invalid character in expression: " + c);
            }
        }

        // Apply remaining operators
        while (!operators.isEmpty()) {
            numbers.push(applyOperator(operators.pop(), numbers.pop(), numbers.pop()));
        }

        return numbers.pop();
    }

    // Method to apply an operator to two numbers
    private static double applyOperator(char op, double b, double a) {
        switch (op) {
            case '+': return a + b;
            case '-': return a - b;
            case '*': return a * b;
            case '/': return a / b;
            default: throw new UnsupportedOperationException("Unsupported operator: " + op);
        }
    }

    // Method to get the precedence of an operator
    private static int precedence(char op) {
        switch (op) {
            case '+':
            case '-': return 1;
            case '*':
            case '/': return 2;
            default: return -1;
        }
    }
}
