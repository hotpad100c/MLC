package mypals.ml.MLC_Manage;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.beans.Expression;
import java.util.HashMap;
import java.util.Map;
import mypals.ml.MLC_Manage.Evaluator;
public class VariableManager {

    private static final Map<String, Integer> intVariables = new HashMap<>();
    private static final Map<String, String> stringVariables = new HashMap<>();
    private static final Map<String, Boolean> boolVariables = new HashMap<>();
    private static final Map<String, Float> floatVariables = new HashMap<>();


    public static void setInt(String name, int value) {
        intVariables.put(name, value);
    }

    public static void setString(String name, String value) {
        stringVariables.put(name, value);
    }

    public static void setBool(String name, boolean value) {
        boolVariables.put(name, value);
    }

    public static void setFloat(String name, float value) {
        floatVariables.put(name, value);
    }

    public static Integer getInt(String name) {
        return intVariables.get(name);
    }

    public static String getString(String name) {
        return stringVariables.get(name);
    }

    public static Boolean getBool(String name) {
        return boolVariables.get(name);
    }

    public static Float getFloat(String name) {
        return floatVariables.get(name);
    }

    public static void parseVariable(String line) {
        try {
            if (line.startsWith("#INT")) {
                String[] parts = line.split("=");
                if (parts.length != 2) {
                    sendErrorMessage("Invalid syntax for #INT line: " + line);
                    return;
                }
                String name = parts[0].split("\\s+")[1].trim();
                String expression = parts[1].trim();
                System.out.println("Parsing INT: " + name + " with expression: " + expression);
                Number result = evaluateExpression(expression, true);
                System.out.println("INT Result: " + result);
                setInt(name, result.intValue()); // Ensure the result is an int
            } else if (line.startsWith("#FLOAT")) {
                String[] parts = line.split("=");
                if (parts.length != 2) {
                    sendErrorMessage("Invalid syntax for #FLOAT line: " + line);
                    return;
                }
                String name = parts[0].split("\\s+")[1].trim();
                String expression = parts[1].trim();
                System.out.println("Parsing FLOAT: " + name + " with expression: " + expression);
                Number result = evaluateExpression(expression, false);
                System.out.println("FLOAT Result: " + result);
                setFloat(name, result.floatValue()); // Ensure the result is a float
            } else if (line.startsWith("#STRING")) {
                String[] parts = line.split("=");
                if (parts.length != 2) {
                    sendErrorMessage("Invalid syntax for #STRING line: " + line);
                    return;
                }
                String name = parts[0].split("\\s+")[1].trim();
                String value = parts[1].trim();
                System.out.println("Parsing STRING: " + name + " with value: " + value);
                setString(name, value);
            } else if (line.startsWith("#BOOL")) {
                String[] parts = line.split("=");
                if (parts.length != 2) {
                    sendErrorMessage("Invalid syntax for #BOOL line: " + line);
                    return;
                }
                String name = parts[0].split("\\s+")[1].trim();
                boolean value = Boolean.parseBoolean(parts[1].trim());
                System.out.println("Parsing BOOL: " + name + " with value: " + value);
                setBool(name, value);
            }
        } catch (NumberFormatException | ScriptException e) {
            sendErrorMessage("Error processing line: " + line + " - " + e.getMessage());
        } catch (Exception e) {
            sendErrorMessage("Error parsing line: " + line);
        }
    }

    private static Number evaluateExpression(String expression, boolean isInt) throws ScriptException {
        // Check if the expression is a simple number
        try {
            if (expression.matches("-?\\d+(\\.\\d+)?")) { // Match integer or floating-point numbers
                if (isInt) {
                    return Integer.parseInt(expression);
                } else {
                    return Float.parseFloat(expression);
                }
            }
        } catch (NumberFormatException e) {
            sendErrorMessage("Invalid number format in expression: " + expression);
            throw new ScriptException("Invalid number format");
        }

        // Replace variable placeholders with actual values
        expression = replaceVariables(expression);

        // Debug: Log the final expression to be evaluated
        System.out.println("Evaluating expression: " + expression);


        // Evaluate the expression
        Number result = Evaluator.evaluateExpression(expression, isInt);

        //Object result = scriptEngine.eval(expression);

        // Debug: Log the result of evaluation
        System.out.println("Evaluation result: " + result);

        if (result instanceof Number) {
            if (isInt) {
                return result.intValue(); // Return integer value
            } else {
                return result.floatValue(); // Return float value
            }
        } else {
            String type = isInt ? "integer" : "float";
            sendErrorMessage("Expression result <" + result + "> is not a " + type + "!");
            throw new ScriptException("Expression result is not a " + type);
        }
    }


    public static String replaceVariables(String line) {
        System.out.println("Original line: " + line);
        for (Map.Entry<String, Integer> entry : intVariables.entrySet()) {
            line = line.replace("${" + entry.getKey() + "}", entry.getValue().toString());
        }
        for (Map.Entry<String, String> entry : stringVariables.entrySet()) {
            line = line.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        for (Map.Entry<String, Boolean> entry : boolVariables.entrySet()) {
            line = line.replace("${" + entry.getKey() + "}", entry.getValue().toString());
        }
        for (Map.Entry<String, Float> entry : floatVariables.entrySet()) {
            line = line.replace("${" + entry.getKey() + "}", entry.getValue().toString());
        }
        System.out.println("After replacement: " + line);
        return line;
    }

    private static void sendErrorMessage(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            Text chatMessage = Text.literal(message).styled(style -> style.withColor(Formatting.RED));
            client.player.sendMessage(chatMessage, false);
        }
    }
}
