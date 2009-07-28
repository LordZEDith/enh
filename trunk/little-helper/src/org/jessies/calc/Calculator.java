package org.jessies.calc;

/*
 * This file is part of LittleHelper.
 * Copyright (C) 2009 Elliott Hughes <enh@jessies.org>.
 * 
 * LittleHelper is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Talc is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.math.*;
import java.util.*;
import org.jessies.test.*;

// FIXME: Mac OS' calculator offers -d variants of all the trig functions for degrees. that, or offer constants to multiply by to convert to degrees/radians?
// FIXME: higher-order built-in functions like http://www.vitanuova.com/inferno/man/1/calc.html (sum, product, integral, differential, solve).
// FIXME: integer division (//).
// FIXME: logical not (prefix !).
public class Calculator {
    private final Map<String, CalculatorAstNode> constants;
    private final Map<String, CalculatorFunction> functions;
    private final Map<String, CalculatorAstNode> variables;
    
    private CalculatorLexer lexer;
    
    public Calculator() {
        this.constants = new HashMap<String, CalculatorAstNode>();
        this.functions = new HashMap<String, CalculatorFunction>();
        this.variables = new HashMap<String, CalculatorAstNode>();
        
        initBuiltInConstants();
        initBuiltInFunctions();
    }
    
    private void initBuiltInConstants() {
        // FIXME: use higher-precision string forms?
        constants.put("e", new CalculatorNumberNode(new BigDecimal(Math.E)));
        
        final CalculatorAstNode pi = new CalculatorNumberNode(new BigDecimal(Math.PI));
        constants.put("pi", pi);
        constants.put("\u03c0", pi);
    }
    
    private void initBuiltInFunctions() {
        // FIXME: acosh, asinh, atanh, chop, clip, sign(um), int(eger_part), frac(tional_part)
        functions.put("abs",       new CalculatorFunctions.Abs());
        functions.put("acos",      new CalculatorFunctions.Acos());
        functions.put("asin",      new CalculatorFunctions.Asin());
        functions.put("atan",      new CalculatorFunctions.Atan());
        functions.put("atan2",     new CalculatorFunctions.Atan2());
        functions.put("BitAnd",    new CalculatorFunctions.BitAnd());
        functions.put("BitNot",    new CalculatorFunctions.BitNot());
        functions.put("BitOr",     new CalculatorFunctions.BitOr());
        functions.put("BitXor",    new CalculatorFunctions.BitXor());
        functions.put("cbrt",      new CalculatorFunctions.Cbrt());
        final CalculatorFunction ceiling = new CalculatorFunctions.Ceiling();
        functions.put("ceil",      ceiling);
        functions.put("ceiling",   ceiling);
        functions.put("cos",       new CalculatorFunctions.Cos());
        functions.put("cosh",      new CalculatorFunctions.Cosh());
        functions.put("exp",       new CalculatorFunctions.Exp());
        functions.put("factorial", new CalculatorFunctions.Factorial());
        functions.put("floor",     new CalculatorFunctions.Floor());
        functions.put("hypot",     new CalculatorFunctions.Hypot());
        functions.put("is_prime",  new CalculatorFunctions.IsPrime());
        functions.put("log",       new CalculatorFunctions.Log());
        functions.put("log10",     new CalculatorFunctions.Log10());
        functions.put("log2",      new CalculatorFunctions.Log2());
        functions.put("logE",      new CalculatorFunctions.LogE());
        final CalculatorFunction random = new CalculatorFunctions.Random();
        functions.put("rand",      random);
        functions.put("random",    random);
        functions.put("round",     new CalculatorFunctions.Round());
        functions.put("sin",       new CalculatorFunctions.Sin());
        functions.put("sinh",      new CalculatorFunctions.Sinh());
        functions.put("sqrt",      new CalculatorFunctions.Sqrt());
        functions.put("tan",       new CalculatorFunctions.Tan());
        functions.put("tanh",      new CalculatorFunctions.Tanh());
        
        final CalculatorFunction sum = new CalculatorFunctions.Sum();
        functions.put("sum",       sum);
        functions.put("\u03a3",    sum); // Unicode Greek capital letter sigma.
        functions.put("\u2211",    sum); // Unicode summation sign.
        
        final CalculatorFunction product = new CalculatorFunctions.Product();
        functions.put("product",   product);
        functions.put("\u03a0",    product); // Unicode Greek capital letter pi.
        functions.put("\u220f",    product); // Unicode product sign.
    }
    
    public String evaluate(String expression) throws CalculatorError {
        this.lexer = new CalculatorLexer(expression);
        
        CalculatorAstNode ast = parseExpr();
        expect(CalculatorToken.END_OF_INPUT);
        
        //System.err.println(ast);
        BigDecimal value = ast.value(this);
        setVariable("Ans", new CalculatorNumberNode(value));
        return value.toString();
    }
    
    public CalculatorAstNode getVariable(String name) {
        return variables.get(name);
    }
    
    public void setVariable(String name, CalculatorAstNode newValue) {
        variables.put(name, newValue);
    }
    
    private CalculatorAstNode parseExpr() {
        return parseAssignmentExpression();
    }
    
    // Mathematica operator precedence: http://reference.wolfram.com/mathematica/tutorial/OperatorInputForms.html
    
    // = (assignment)
    private CalculatorAstNode parseAssignmentExpression() {
        CalculatorAstNode result = parseOrExpression();
        if (lexer.token() == CalculatorToken.ASSIGN) {
            CalculatorToken op = lexer.token();
            lexer.nextToken();
            result = new CalculatorOpNode(op, result, parseOrExpression());
        }
        return result;
        
    }
    
    // |
    private CalculatorAstNode parseOrExpression() {
        CalculatorAstNode result = parseAndExpression();
        while (lexer.token() == CalculatorToken.B_OR) {
            lexer.nextToken();
            // FIXME: make BitOr varargs.
            result = new CalculatorFunctionApplicationNode(functions.get("BitOr"), Arrays.asList(result, parseAndExpression()));
        }
        return result;
    }
    
    // &
    private CalculatorAstNode parseAndExpression() {
        CalculatorAstNode result = parseNotExpression();
        while (lexer.token() == CalculatorToken.B_AND) {
            lexer.nextToken();
            // FIXME: make BitAnd varargs.
            result = new CalculatorFunctionApplicationNode(functions.get("BitAnd"), Arrays.asList(result, parseNotExpression()));
        }
        return result;
    }
    
    // !
    private CalculatorAstNode parseNotExpression() {
        if (lexer.token() == CalculatorToken.PLING) {
            lexer.nextToken();
            return new CalculatorOpNode(CalculatorToken.L_NOT, parseNotExpression(), null);
        } else {
            return parseRelationalExpression();
        }
    }
    
    // == >= > <= < !=
    private CalculatorAstNode parseRelationalExpression() {
        CalculatorAstNode result = parseShiftExpression();
        while (lexer.token() == CalculatorToken.EQ || lexer.token() == CalculatorToken.GE || lexer.token() == CalculatorToken.GT || lexer.token() == CalculatorToken.LE || lexer.token() == CalculatorToken.LT || lexer.token() == CalculatorToken.NE) {
            CalculatorToken op = lexer.token();
            lexer.nextToken();
            result = new CalculatorOpNode(op, result, parseShiftExpression());
        }
        return result;
    }
    
    // << >>
    private CalculatorAstNode parseShiftExpression() {
        CalculatorAstNode result = parseAdditiveExpression();
        while (lexer.token() == CalculatorToken.SHL || lexer.token() == CalculatorToken.SHR) {
            CalculatorToken op = lexer.token();
            lexer.nextToken();
            result = new CalculatorOpNode(op, result, parseAdditiveExpression());
        }
        return result;
    }
    
    // + -
    private CalculatorAstNode parseAdditiveExpression() {
        CalculatorAstNode result = parseMultiplicativeExpression();
        while (lexer.token() == CalculatorToken.PLUS || lexer.token() == CalculatorToken.MINUS) {
            CalculatorToken op = lexer.token();
            lexer.nextToken();
            result = new CalculatorOpNode(op, result, parseMultiplicativeExpression());
        }
        return result;
    }
    
    // * / %
    private CalculatorAstNode parseMultiplicativeExpression() {
        CalculatorAstNode result = parseUnaryExpression();
        while (lexer.token() == CalculatorToken.MUL || lexer.token() == CalculatorToken.DIV || lexer.token() == CalculatorToken.MOD) {
            CalculatorToken op = lexer.token();
            lexer.nextToken();
            result = new CalculatorOpNode(op, result, parseUnaryExpression());
        }
        return result;
    }
    
    // ~ -
    private CalculatorAstNode parseUnaryExpression() {
        if (lexer.token() == CalculatorToken.MINUS) {
            lexer.nextToken();
            // Convert (-f) to (0-f) for simplicity.
            return new CalculatorOpNode(CalculatorToken.MINUS, new CalculatorNumberNode(BigDecimal.ZERO), parseUnaryExpression());
        } else if (lexer.token() == CalculatorToken.B_NOT) {
            lexer.nextToken();
            return new CalculatorFunctionApplicationNode(functions.get("BitNot"), Collections.singletonList(parseUnaryExpression()));
        }
        return parseSqrtExpression();
    }
    
    // sqrt
    private CalculatorAstNode parseSqrtExpression() {
        if (lexer.token() == CalculatorToken.SQRT) {
            lexer.nextToken();
            return new CalculatorFunctionApplicationNode(functions.get("sqrt"), Collections.singletonList(parseSqrtExpression()));
        } else {
            return parseExponentiationExpression();
        }
    }
    
    // ^
    private CalculatorAstNode parseExponentiationExpression() {
        CalculatorAstNode result = parseFactorialExpression();
        if (lexer.token() == CalculatorToken.POW) {
            CalculatorToken op = lexer.token();
            lexer.nextToken();
            result = new CalculatorOpNode(op, result, parseExponentiationExpression());
        }
        return result;
    }
    
    // postfix-!
    private CalculatorAstNode parseFactorialExpression() {
        CalculatorAstNode result = parseFactor();
        if (lexer.token() == CalculatorToken.PLING) {
            expect(CalculatorToken.PLING);
            result = new CalculatorOpNode(CalculatorToken.FACTORIAL, result, null);
        }
        return result;
    }
    
    private CalculatorAstNode parseFactor() {
        if (lexer.token() == CalculatorToken.OPEN_PARENTHESIS) {
            expect(CalculatorToken.OPEN_PARENTHESIS);
            CalculatorAstNode result = parseExpr();
            expect(CalculatorToken.CLOSE_PARENTHESIS);
            return result;
        } else if (lexer.token() == CalculatorToken.NUMBER) {
            CalculatorAstNode result = new CalculatorNumberNode(lexer.number());
            expect(CalculatorToken.NUMBER);
            return result;
        } else if (lexer.token() == CalculatorToken.IDENTIFIER) {
            final String identifier = lexer.identifier();
            expect(CalculatorToken.IDENTIFIER);
            CalculatorAstNode result = constants.get(identifier);
            if (result == null) {
                final CalculatorFunction fn = functions.get(identifier);
                if (fn != null) {
                    result = new CalculatorFunctionApplicationNode(fn, parseArgs());
                } else {
                    result = new CalculatorVariableNode(identifier);
                }
            }
            return result;
        } else {
            throw new CalculatorError("unexpected " + quoteTokenForErrorMessage(lexer.token()));
        }
    }
    
    // '(' expr [ ',' expr ] ')'
    private List<CalculatorAstNode> parseArgs() {
        final List<CalculatorAstNode> result = new LinkedList<CalculatorAstNode>();
        expect(CalculatorToken.OPEN_PARENTHESIS);
        while (lexer.token() != CalculatorToken.CLOSE_PARENTHESIS) {
            result.add(parseExpr());
            if (lexer.token() == CalculatorToken.COMMA) {
                expect(CalculatorToken.COMMA);
                continue;
            }
        }
        expect(CalculatorToken.CLOSE_PARENTHESIS);
        return result;
    }
    
    private void expect(CalculatorToken what) {
        if (lexer.token() != what) {
            throw new CalculatorError("expected " + quoteTokenForErrorMessage(what) + ", got " + quoteTokenForErrorMessage(lexer.token()) + " instead");
        }
        lexer.nextToken();
    }
    
    private static String quoteTokenForErrorMessage(CalculatorToken token) {
        String result = token.toString();
        if (result.length() > 2) {
            // We probably already have something usable like "end of input".
            return result;
        }
        // Quote operators.
        return "'" + result + "'";
    }
    
    @Test private static void testArithmetic() {
        Assert.equals(new Calculator().evaluate("0"), "0");
        Assert.equals(new Calculator().evaluate("1"), "1");
        Assert.equals(new Calculator().evaluate("-1"), "-1");
        Assert.equals(new Calculator().evaluate("--1"), "1");
        Assert.equals(new Calculator().evaluate("1.00"), "1.00");
        
        Assert.equals(new Calculator().evaluate(".2"), "0.2");
        
        Assert.equals(new Calculator().evaluate("1.2E3"), "1.2E+3");
        Assert.equals(new Calculator().evaluate("1E3"), "1E+3");
        Assert.equals(new Calculator().evaluate("1.E3"), "1E+3");
        Assert.equals(new Calculator().evaluate(".1E3"), "1E+2");
        
        Assert.equals(new Calculator().evaluate("1+2+3"), "6");
        Assert.equals(new Calculator().evaluate("1+-2"), "-1");
        Assert.equals(new Calculator().evaluate("3-2-1"), "0");
        Assert.equals(new Calculator().evaluate("10000+0.001"), "10000.001");
        Assert.equals(new Calculator().evaluate("0.001+10000"), "10000.001");
        Assert.equals(new Calculator().evaluate("10000-0.001"), "9999.999");
        Assert.equals(new Calculator().evaluate("0.001-10000"), "-9999.999");
        
        Assert.equals(new Calculator().evaluate("3*4"), "12");
        Assert.equals(new Calculator().evaluate("-3*4"), "-12");
        Assert.equals(new Calculator().evaluate("3*-4"), "-12");
        Assert.equals(new Calculator().evaluate("-3*-4"), "12");
        
        Assert.equals(new Calculator().evaluate("1+2*3"), "7");
        Assert.equals(new Calculator().evaluate("(1+2)*3"), "9");
        
        Assert.equals(new Calculator().evaluate("1/2"), "0.5");
        
        Assert.equals(new Calculator().evaluate("3%4"), "3");
        Assert.equals(new Calculator().evaluate("4%4"), "0");
        Assert.equals(new Calculator().evaluate("5%4"), "1");
    }
    
    @Test private static void testRelationalOperations() {
        Assert.equals(new Calculator().evaluate("1<2"), "1");
        Assert.equals(new Calculator().evaluate("2<2"), "0");
        Assert.equals(new Calculator().evaluate("2<1"), "0");
        Assert.equals(new Calculator().evaluate("1<=2"), "1");
        Assert.equals(new Calculator().evaluate("2<=2"), "1");
        Assert.equals(new Calculator().evaluate("2<=1"), "0");
        Assert.equals(new Calculator().evaluate("1>2"), "0");
        Assert.equals(new Calculator().evaluate("2>2"), "0");
        Assert.equals(new Calculator().evaluate("2>1"), "1");
        Assert.equals(new Calculator().evaluate("1>=2"), "0");
        Assert.equals(new Calculator().evaluate("2>=2"), "1");
        Assert.equals(new Calculator().evaluate("2>=1"), "1");
        Assert.equals(new Calculator().evaluate("1==2"), "0");
        Assert.equals(new Calculator().evaluate("2==2"), "1");
        Assert.equals(new Calculator().evaluate("2==1"), "0");
        Assert.equals(new Calculator().evaluate("1!=2"), "1");
        Assert.equals(new Calculator().evaluate("2!=2"), "0");
        Assert.equals(new Calculator().evaluate("2!=1"), "1");
    }
    
    @Test private static void testNot() {
        Assert.equals(new Calculator().evaluate("!(1==2)"), "1");
        Assert.equals(new Calculator().evaluate("!(2==2)"), "0");
        Assert.equals(new Calculator().evaluate("!!(2==2)"), "1");
    }
    
    @Test private static void testShifts() {
        Assert.equals(new Calculator().evaluate("1<<4"), "16");
        Assert.equals(new Calculator().evaluate("(12<<3)>>3"), "12");
    }
    
    @Test private static void testBitOperations() {
        Assert.equals(new Calculator().evaluate("(0x1234 & 0xff0) == 0x230"), "1");
        Assert.equals(new Calculator().evaluate("(0x1200 | 0x34) == 0x1234"), "1");
        Assert.equals(new Calculator().evaluate("BitXor(5, 3)"), "6");
        Assert.equals(new Calculator().evaluate("((0x1234 & ~0xff) | 0x56) == 0x1256"), "1");
        Assert.equals(new Calculator().evaluate("~3"), "-4");
        Assert.equals(new Calculator().evaluate("~~3"), "3");
    }
    
    @Test private static void testExponentiation() {
        Assert.equals(new Calculator().evaluate("2^3"), "8");
        Assert.equals(new Calculator().evaluate("2^3^4"), "2417851639229258349412352");
        Assert.equals(new Calculator().evaluate("4^0.5"), "2");
        Assert.equals(new Calculator().evaluate("-10^2"), "-100");
        Assert.equals(new Calculator().evaluate("(-10)^2"), "100");
    }
    
    @Test private static void testConstants() {
        Assert.equals(Double.valueOf(new Calculator().evaluate("e")), Math.E, 0.000001);
        Assert.equals(Double.valueOf(new Calculator().evaluate("pi")), Math.PI, 0.000001);
        Assert.equals(new Calculator().evaluate("pi == \u03c0"), "1");
    }
    
    @Test private static void testFunctions() {
        // FIXME: better tests?
        Assert.equals(new Calculator().evaluate("abs(2)"), "2");
        Assert.equals(new Calculator().evaluate("abs(-2)"), "2");
        Assert.equals(new Calculator().evaluate("acos(1)"), "0");
        Assert.equals(new Calculator().evaluate("asin(0)"), "0");
        Assert.equals(new Calculator().evaluate("acos(0) == asin(1)"), "1");
        Assert.equals(new Calculator().evaluate("atan(0)"), "0");
        Assert.equals(new Calculator().evaluate("cbrt(27)"), "3");
        Assert.equals(new Calculator().evaluate("ceil(1.2)"), "2");
        Assert.equals(new Calculator().evaluate("cos(0)"), "1");
        Assert.equals(new Calculator().evaluate("cos(pi)"), "-1");
        Assert.equals(new Calculator().evaluate("cosh(0)"), "1");
        Assert.equals(Double.valueOf(new Calculator().evaluate("exp(1)/e")), 1.0, 0.000001);
        Assert.equals(new Calculator().evaluate("factorial(5)"), "120");
        Assert.equals(new Calculator().evaluate("factorial(5) == 5!"), "1");
        Assert.equals(new Calculator().evaluate("floor(1.2)"), "1");
        Assert.equals(new Calculator().evaluate("hypot(3, 4)"), "5");
        
        Assert.equals(new Calculator().evaluate("is_prime(0)"), "0");
        Assert.equals(new Calculator().evaluate("is_prime(1)"), "0");
        Assert.equals(new Calculator().evaluate("is_prime(2)"), "1");
        Assert.equals(new Calculator().evaluate("is_prime(3)"), "1");
        Assert.equals(new Calculator().evaluate("is_prime(4)"), "0");
        Assert.equals(new Calculator().evaluate("is_prime(5)"), "1");
        Assert.equals(new Calculator().evaluate("is_prime(-4)"), "0");
        Assert.equals(new Calculator().evaluate("is_prime(-5)"), "1");
        
        Assert.equals(new Calculator().evaluate("log(2, 1024)"), "10");
        Assert.equals(new Calculator().evaluate("log2(1024)"), "10");
        Assert.equals(new Calculator().evaluate("logE(exp(4))"), "4");
        Assert.equals(new Calculator().evaluate("log10(1000)"), "3");
        Assert.equals(new Calculator().evaluate("round(1.2)"), "1");
        Assert.equals(new Calculator().evaluate("round(1.8)"), "2");
        Assert.equals(new Calculator().evaluate("sin(0)"), "0");
        Assert.equals(new Calculator().evaluate("sin(pi/2)"), "1");
        Assert.equals(new Calculator().evaluate("sinh(0)"), "0");
        Assert.equals(new Calculator().evaluate("sqrt(81)"), "9");
        Assert.equals(new Calculator().evaluate("tan(0)"), "0");
        Assert.equals(new Calculator().evaluate("tanh(0)"), "0");
    }
    
    @Test private static void testSqrt() {
        Assert.equals(new Calculator().evaluate("\u221a4"), "2");
        // Check /3*2 == 2*/3 (where / is ASCII-safe \u221a).
        Assert.startsWith(new Calculator().evaluate("\u221a3*2"), "3.464");
    }
    
    @Test private static void testSum() {
        Assert.equals(new Calculator().evaluate("sum(0, 10, i)"), "55");
        Assert.equals(new Calculator().evaluate("sum(0, 10.2, i)"), "55");
        Assert.equals(new Calculator().evaluate("sum(0, 10, i^2)"), "385");
        Assert.equals(Double.valueOf(new Calculator().evaluate("sum(0,30,1/i!)-e")), 0.0, 0.000001);
        // FIXME: failure test for min > max.
    }
    
    @Test private static void testProduct() {
        Assert.equals(new Calculator().evaluate("product(1, 10, i)"), "3628800");
        Assert.equals(new Calculator().evaluate("product(1, 10.2, i)"), "3628800");
        Assert.equals(new Calculator().evaluate("product(1, 6, i^2)"), "518400");
        // FIXME: failure test for min > max.
    }
    
    @Test private static void testAns() {
        final Calculator calculator = new Calculator();
        Assert.equals(calculator.evaluate("0"), "0");
        Assert.equals(calculator.evaluate("1+Ans"), "1");
        Assert.equals(calculator.evaluate("1+Ans"), "2");
        Assert.equals(calculator.evaluate("Ans*2"), "4");
    }
    
    @Test private static void testVariables() {
        final Calculator calculator = new Calculator();
        Assert.equals(calculator.evaluate("a = 2"), "2");
        Assert.equals(calculator.evaluate("a"), "2");
        Assert.equals(calculator.evaluate("2*a"), "4");
    }
}
