package edu.yu.compilers.intermediate.util;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.Vocabulary;
import org.antlr.v4.runtime.tree.ParseTree;
import org.json.JSONArray;
import org.json.JSONObject;

import antlr4.JavanaParser;

public class ParseTreePrinter {
    private final JavanaParser parser;
    private final Vocabulary vocabulary;

    public ParseTreePrinter(JavanaParser parser) {
        this.parser = parser;
        this.vocabulary = parser.getVocabulary();
    }

    public void printParseTree(ParseTree node) {
        printParseTree(node, 0);
    }

    public void printParseTreeToJson(ParseTree node) {
        var json = buildJson(node);
        System.out.println(json.toString(2));
    }

    private void printParseTree(ParseTree node, int level) {
        // Indentation
        for (int i = 0; i < level; ++i) {
            System.out.print("  ");
        }

        if (node instanceof ParserRuleContext) {
            // It's a rule context
            ParserRuleContext context = (ParserRuleContext) node;
            String ruleName = parser.getRuleNames()[context.getRuleIndex()];
            System.out.println(ruleName);

            // Recursively print all children
            for (int i = 0; i < node.getChildCount(); ++i) {
                printParseTree(node.getChild(i), level + 1);
            }
        } else if (node.getChildCount() == 0) {
            // It's a leaf node (token)
            System.out.println("'" + node.getText() + "'");
        }
    }

    private JSONObject buildJson(ParseTree node) {
        JSONObject jsonNode = new JSONObject();

        if (node instanceof ParserRuleContext) {
            ParserRuleContext context = (ParserRuleContext) node;
            String ruleName = parser.getRuleNames()[context.getRuleIndex()];

            JSONArray childrenArray = new JSONArray();
            for (int i = 0; i < node.getChildCount(); ++i) {
                ParseTree child = node.getChild(i);
                JSONObject childJson = buildJson(child); // Recursively build JSON for children
                if (childJson.length() > 0) { // Ensure we don't add empty JSONObjects
                    childrenArray.put(childJson);
                }
            }

            if (childrenArray.length() > 0) {
                jsonNode.put(ruleName, childrenArray);
            } else {
                // For rules that have no children, we simply note their presence.
                jsonNode.put(ruleName, new JSONArray());
            }
        } else {
            // It's a leaf node (token), we'll return its text directly.
            var tokenTypeName = vocabulary.getSymbolicName(((Token) node.getPayload()).getType());
            jsonNode.put("token", node.getText());
            jsonNode.put("type", tokenTypeName);
        }

        return jsonNode;
    }

}
