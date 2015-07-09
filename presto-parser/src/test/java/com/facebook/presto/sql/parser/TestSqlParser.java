/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.sql.parser;

import com.facebook.presto.sql.tree.*;
import com.facebook.presto.sql.tree.IntervalLiteral.IntervalField;
import com.facebook.presto.sql.tree.IntervalLiteral.Sign;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import org.antlr.runtime.tree.CommonTree;
import org.testng.annotations.Test;

import java.util.Optional;

import static com.facebook.presto.sql.QueryUtil.selectList;
import static com.facebook.presto.sql.QueryUtil.table;
import static com.facebook.presto.sql.SqlFormatter.formatSql;
import static com.facebook.presto.sql.parser.IdentifierSymbol.AT_SIGN;
import static com.facebook.presto.sql.parser.IdentifierSymbol.COLON;
import static java.lang.String.format;
import static java.util.Collections.nCopies;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class TestSqlParser
{
    private static final SqlParser SQL_PARSER = new SqlParser();

    @Test
    public void testPossibleExponentialBacktracking()
            throws Exception
    {
        SQL_PARSER.createExpression("(((((((((((((((((((((((((((true)))))))))))))))))))))))))))");
    }

    @Test
    public void testYulin()
            throws Exception
    {
        processSql("1+sum(@[#abc #ab].asds)");
        processSql("1+sum(@[#abc #ab].asds)");
        processSql("1+sum(@asds)");
        processSql("@m1");
        processSql("@[QBKZ70D4M].[Revenue]");
        processSql("@abc/@dce");
        processSql("@[abc/dce]");
        processSql("current_date");
        processSql("current_date()");
        processSql("D'2015-03-03'");
        processSql("D('111', 222)");
        processSql("D'2015-03-03'");
        processSql("current_date");

        processSql("current_date()");
        processSql("cast(a as int)");
        processSql("cast(a as integer(3, 2))");
        processSql("cast(a as bit)");
        processSql("cast(a as bit varying (3))");
        processSql("cast(a as xxx varying)");
        processSql("cast(a as bigint)");
        processSql("cast(a as bigserial)");
        processSql("cast(a as boolean)");
        processSql("cast(a as box)");
        processSql("cast(a as character)");
        processSql("cast(a as character varying (3))");
        processSql("cast(a as date)");
        processSql("cast(a as double precision)");
        processSql("cast(a as numeric(3))");
        processSql("cast(a as real)");
        processSql("cast(a as real(3, 4))");
        processSql("cast(a as float)");
        processSql("cast(a as float(3, 4))");
        processSql("cast(a as smallint)");
        processSql("cast(a as text)");
        processSql("cast(a as time (3) with time zone)");
        processSql("cast(a as time with time zone)");
        processSql("cast(a as int)");
        processSql("cast(a as varchar)");
        processSql("cast(a as char(30))");
        processSql("cast(a as binary)");
        processSql("cast(a as decimal)");

        processSql("cast(a as varchar2)");
        processSql("cast(a as number)");
        processSql("cast(a as number(7, 2))");
        processSql("cast(a as int2)");

        processSql("cast(a as long varbyte(1))");
        processSql("cast(a as long varbyte)");
        processSql("cast(a as char varying)");
        processSql("cast(a as long varchar)");

        processSql("cast(a as char for bit data)");
        processSql("cast(a as char (3) for bit data)");
        processSql("cast(a as long varchar for bit data)");
        processSql("cast(a as long varchar (3) for bit data)");

        processSql("pass(@[abc] + %%% + @@@::111 + cast(1 as bigint))");
        processSql("pass(@[abc] + %%% + @@@::111 + cast(1 as bigint)) +VPC+ pass(@asdsa)");
    }

    private void processSql(String sql)
    {
        Expression expression = SQL_PARSER.createExpression(sql);
        CommonTree commonTree = SQL_PARSER.parseExpression(sql);
        System.out.println("treetostring -> " + TreePrinter.treeToString(commonTree));
        System.out.println("expression tostring() -> " + expression.toString());
    }

    @Test
    public void testGenericLiteral()
            throws Exception
    {
        assertGenericLiteral("VARCHAR");
        assertGenericLiteral("BIGINT");
        assertGenericLiteral("DOUBLE");
        assertGenericLiteral("BOOLEAN");
        assertGenericLiteral("DATE");
        assertGenericLiteral("foo");
    }

    public static void assertGenericLiteral(String type)
    {
        assertExpression(type + " 'abc'", new GenericLiteral(type, "abc"));
    }

    @Test
    public void testLiterals()
            throws Exception
    {
        assertExpression("TIME" + " 'abc'", new TimeLiteral("abc"));
        assertExpression("TIMESTAMP" + " 'abc'", new TimestampLiteral("abc"));
        assertExpression("INTERVAL '33' day", new IntervalLiteral("33", Sign.POSITIVE, IntervalField.DAY, null));
        assertExpression("INTERVAL '33' day to second", new IntervalLiteral("33", Sign.POSITIVE, IntervalField.DAY, IntervalField.SECOND));
    }

    @Test
    public void testArrayConstructor()
            throws Exception
    {
        assertExpression("ARRAY []", new ArrayConstructor(ImmutableList.<Expression>of()));
        assertExpression("ARRAY [1, 2]", new ArrayConstructor(ImmutableList.<Expression>of(new LongLiteral("1"), new LongLiteral("2"))));
        assertExpression("ARRAY [1.0, 2.5]", new ArrayConstructor(ImmutableList.<Expression>of(new DoubleLiteral("1.0"), new DoubleLiteral("2.5"))));
        assertExpression("ARRAY ['hi']", new ArrayConstructor(ImmutableList.<Expression>of(new StringLiteral("hi"))));
        assertExpression("ARRAY ['hi', 'hello']", new ArrayConstructor(ImmutableList.<Expression>of(new StringLiteral("hi"), new StringLiteral("hello"))));
    }

    @Test
    public void testArraySubscript()
            throws Exception
    {
        assertExpression("ARRAY [1, 2][1]", new SubscriptExpression(
                        new ArrayConstructor(ImmutableList.<Expression>of(new LongLiteral("1"), new LongLiteral("2"))),
                        new LongLiteral("1"))
        );
        try {
            assertExpression("CASE WHEN TRUE THEN ARRAY[1,2] END[1]", null);
            fail();
        }
        catch (RuntimeException e) {
            // Expected
        }
    }

    @Test
    public void testDouble()
            throws Exception
    {
        assertExpression("123.", new DoubleLiteral("123"));
        assertExpression("123.0", new DoubleLiteral("123"));
        assertExpression(".5", new DoubleLiteral(".5"));
        assertExpression("123.5", new DoubleLiteral("123.5"));

        assertExpression("123E7", new DoubleLiteral("123E7"));
        assertExpression("123.E7", new DoubleLiteral("123E7"));
        assertExpression("123.0E7", new DoubleLiteral("123E7"));
        assertExpression("123E+7", new DoubleLiteral("123E7"));
        assertExpression("123E-7", new DoubleLiteral("123E-7"));

        assertExpression("123.456E7", new DoubleLiteral("123.456E7"));
        assertExpression("123.456E+7", new DoubleLiteral("123.456E7"));
        assertExpression("123.456E-7", new DoubleLiteral("123.456E-7"));

        assertExpression(".4E42", new DoubleLiteral(".4E42"));
        assertExpression(".4E+42", new DoubleLiteral(".4E42"));
        assertExpression(".4E-42", new DoubleLiteral(".4E-42"));
    }

    @Test
    public void testCast()
            throws Exception
    {
        assertCast("varchar");
        assertCast("bigint");
        assertCast("double");
        assertCast("boolean");
        assertCast("date");
        assertCast("time");
        assertCast("timestamp");
        assertCast("time with time zone");
        assertCast("timestamp with time zone");
        assertCast("foo");

        assertCast("ARRAY<bigint>");
        assertCast("ARRAY<BIGINT>");
        assertCast("array<bigint>");
        assertCast("array < bigint  >", "ARRAY<bigint>");
        assertCast("array<array<bigint>>");

        assertCast("varchar(100)", "VARCHAR(100)");
        assertCast("bigint(100)", "BIGINT(100)");
        assertCast("blabla(100)", "BLABLA(100)");
        assertCast("blabla(100,2)", "BLABLA(100,2)");
        assertCast("decimal(100, 2)", "DECIMAL(100,2)");
    }

    public static void assertCast(String type, String result)
    {
        assertExpression("cast(123 as " + type + ")", new Cast(new LongLiteral("123"), result));
    }

    public static void assertCast(String type)
    {
        assertCast(type, type);
    }

    @Test
    public void testPositiveSign()
            throws Exception
    {
        assertExpression("9", new LongLiteral("9"));

        assertExpression("+9", new LongLiteral("9"));
        assertExpression("++9", new LongLiteral("9"));
        assertExpression("+++9", new LongLiteral("9"));

        assertExpression("+9", new LongLiteral("9"));
        assertExpression("+ +9", new LongLiteral("9"));
        assertExpression("+ + +9", new LongLiteral("9"));

        assertExpression("+ 9", new LongLiteral("9"));
        assertExpression("+ + 9", new LongLiteral("9"));
        assertExpression("+ + + 9", new LongLiteral("9"));
    }

    @Test
    public void testNegativeSign()
    {
        Expression expression = new LongLiteral("9");
        assertExpression("9", expression);

        expression = new NegativeExpression(expression);
        assertExpression("-9", expression);
        assertExpression("- 9", expression);
        assertExpression("- + 9", expression);
        assertExpression("+ - + 9", expression);
        assertExpression("-+9", expression);
        assertExpression("+-+9", expression);

        expression = new NegativeExpression(expression);
        assertExpression("- -9", expression);
        assertExpression("- - 9", expression);
        assertExpression("- + - + 9", expression);
        assertExpression("+ - + - + 9", expression);
        assertExpression("-+-+9", expression);
        assertExpression("+-+-+9", expression);

        expression = new NegativeExpression(expression);
        assertExpression("- - -9", expression);
        assertExpression("- - - 9", expression);
    }

    @Test
    public void testDoubleInQuery()
    {
        assertStatement("SELECT 123.456E7 FROM DUAL",
                new Query(
                        Optional.empty(),
                        new QuerySpecification(
                                selectList(new DoubleLiteral("123.456E7")),
                                table(QualifiedName.of("DUAL")),
                                Optional.empty(),
                                ImmutableList.<Expression>of(),
                                Optional.empty(),
                                ImmutableList.<SortItem>of(),
                                Optional.empty()),
                        ImmutableList.<SortItem>of(),
                        Optional.empty(),
                        Optional.empty()));
    }

    @Test
    public void testIntersect()
    {
        assertStatement("SELECT 123 INTERSECT DISTINCT SELECT 123 INTERSECT ALL SELECT 123",
                new Query(
                        Optional.empty(),
                        new Intersect(ImmutableList.<Relation>of(
                                new Intersect(ImmutableList.<Relation>of(createSelect123(), createSelect123()), true),
                                createSelect123()
                        ), false),
                        ImmutableList.<SortItem>of(),
                        Optional.empty(),
                        Optional.empty()));
    }

    @Test
    public void testUnion()
    {
        assertStatement("SELECT 123 UNION DISTINCT SELECT 123 UNION ALL SELECT 123",
                new Query(
                        Optional.empty(),
                        new Union(ImmutableList.<Relation>of(
                                new Union(ImmutableList.<Relation>of(createSelect123(), createSelect123()), true),
                                createSelect123()
                        ), false),
                        ImmutableList.<SortItem>of(),
                        Optional.empty(),
                        Optional.empty()));
    }

    private static QuerySpecification createSelect123()
    {
        return new QuerySpecification(
                new Select(false, ImmutableList.<SelectItem>of(new SingleColumn(new LongLiteral("123")))),
                Optional.empty(),
                Optional.empty(),
                ImmutableList.<Expression>of(),
                Optional.empty(),
                ImmutableList.<SortItem>of(),
                Optional.empty()
        );
    }

    @Test
    public void testValues()
    {
        assertStatement("VALUES ('a', 1, 2.2), ('b', 2, 3.3)",
                new Query(
                        Optional.empty(),
                        new Values(ImmutableList.of(
                                new Row(ImmutableList.<Expression>of(
                                        new StringLiteral("a"),
                                        new LongLiteral("1"),
                                        new DoubleLiteral("2.2")
                                )),
                                new Row(ImmutableList.<Expression>of(
                                        new StringLiteral("b"),
                                        new LongLiteral("2"),
                                        new DoubleLiteral("3.3")
                                ))
                        )),
                        ImmutableList.<SortItem>of(),
                        Optional.empty(),
                        Optional.empty()));

        assertStatement("SELECT * FROM (VALUES ('a', 1, 2.2), ('b', 2, 3.3))",
                new Query(
                        Optional.empty(),
                        new QuerySpecification(
                                selectList(new AllColumns()),
                                Optional.<Relation>of(new TableSubquery(
                                                new Query(
                                                        Optional.empty(),
                                                        new Values(ImmutableList.of(
                                                                new Row(ImmutableList.<Expression>of(
                                                                        new StringLiteral("a"),
                                                                        new LongLiteral("1"),
                                                                        new DoubleLiteral("2.2")
                                                                )),
                                                                new Row(ImmutableList.<Expression>of(
                                                                        new StringLiteral("b"),
                                                                        new LongLiteral("2"),
                                                                        new DoubleLiteral("3.3")
                                                                ))
                                                        )),
                                                        ImmutableList.<SortItem>of(),
                                                        Optional.empty(),
                                                        Optional.empty()))
                                ),
                                Optional.empty(),
                                ImmutableList.<Expression>of(),
                                Optional.empty(),
                                ImmutableList.<SortItem>of(),
                                Optional.empty()),
                        ImmutableList.<SortItem>of(),
                        Optional.empty(),
                        Optional.empty()));
    }

    @Test(expectedExceptions = ParsingException.class, expectedExceptionsMessageRegExp = "line 1:1: no viable alternative at input '<EOF>'")
    public void testEmptyExpression()
    {
        SQL_PARSER.createExpression("");
    }

    @Test(expectedExceptions = ParsingException.class, expectedExceptionsMessageRegExp = "line 1:1: no viable alternative at input '<EOF>'")
    public void testEmptyStatement()
    {
        SQL_PARSER.createStatement("");
    }

    @Test(expectedExceptions = ParsingException.class, expectedExceptionsMessageRegExp = "line 1:7: mismatched input 'x' expecting EOF")
    public void testExpressionWithTrailingJunk()
    {
        SQL_PARSER.createExpression("1 + 1 x");
    }

    /*
    @Test(expectedExceptions = ParsingException.class, expectedExceptionsMessageRegExp = "line 1:1: no viable alternative at character '@'")
    public void testTokenizeErrorStartOfLine()
    {
        SQL_PARSER.createStatement("@select");
    }

    @Test(expectedExceptions = ParsingException.class, expectedExceptionsMessageRegExp = "line 1:25: no viable alternative at character '@'")
    public void testTokenizeErrorMiddleOfLine()
    {
        SQL_PARSER.createStatement("select * from foo where @what");
    }
    */

    @Test(expectedExceptions = ParsingException.class, expectedExceptionsMessageRegExp = "line 1:20: mismatched character '<EOF>' expecting '''")
    public void testTokenizeErrorIncompleteToken()
    {
        SQL_PARSER.createStatement("select * from 'oops");
    }

    @Test(expectedExceptions = ParsingException.class, expectedExceptionsMessageRegExp = "line 3:1: mismatched input 'from' expecting EOF")
    public void testParseErrorStartOfLine()
    {
        SQL_PARSER.createStatement("select *\nfrom x\nfrom");
    }

    @Test(expectedExceptions = ParsingException.class, expectedExceptionsMessageRegExp = "line 3:7: no viable alternative at input 'from'")
    public void testParseErrorMiddleOfLine()
    {
        SQL_PARSER.createStatement("select *\nfrom x\nwhere from");
    }

    @Test(expectedExceptions = ParsingException.class, expectedExceptionsMessageRegExp = "line 1:14: no viable alternative at input '<EOF>'")
    public void testParseErrorEndOfInput()
    {
        SQL_PARSER.createStatement("select * from");
    }

    @Test(expectedExceptions = ParsingException.class, expectedExceptionsMessageRegExp = "line 1:16: no viable alternative at input '<EOF>'")
    public void testParseErrorEndOfInputWhitespace()
    {
        SQL_PARSER.createStatement("select * from  ");
    }

    @Test(expectedExceptions = ParsingException.class, expectedExceptionsMessageRegExp = "line 1:15: backquoted identifiers are not supported; use double quotes to quote identifiers")
    public void testParseErrorBackquotes()
    {
        SQL_PARSER.createStatement("select * from `foo`");
    }

    @Test(expectedExceptions = ParsingException.class, expectedExceptionsMessageRegExp = "line 1:19: backquoted identifiers are not supported; use double quotes to quote identifiers")
    public void testParseErrorBackquotesEndOfInput()
    {
        SQL_PARSER.createStatement("select * from foo `bar`");
    }

    @Test(expectedExceptions = ParsingException.class, expectedExceptionsMessageRegExp = "line 1:8: identifiers must not start with a digit; surround the identifier with double quotes")
    public void testParseErrorDigitIdentifiers()
    {
        SQL_PARSER.createStatement("select 1x from dual");
    }

    @Test(expectedExceptions = ParsingException.class, expectedExceptionsMessageRegExp = "line 1:15: identifiers must not contain '@'")
    public void testIdentifierWithAtSign()
    {
        SQL_PARSER.createStatement("select * from foo@bar");
    }

    @Test(expectedExceptions = ParsingException.class, expectedExceptionsMessageRegExp = "line 1:15: identifiers must not contain ':'")
    public void testIdentifierWithColon()
    {
        SQL_PARSER.createStatement("select * from foo:bar");
    }

    @Test(expectedExceptions = ParsingException.class, expectedExceptionsMessageRegExp = "line 1:35: no viable alternative at input 'order'")
    public void testParseErrorDualOrderBy()
    {
        SQL_PARSER.createStatement("select fuu from dual order by fuu order by fuu");
    }

    @Test(expectedExceptions = ParsingException.class, expectedExceptionsMessageRegExp = "line 1:31: mismatched input 'order' expecting EOF")
    public void testParseErrorReverseOrderByLimit()
    {
        SQL_PARSER.createStatement("select fuu from dual limit 10 order by fuu");
    }

    @Test(expectedExceptions = ParsingException.class, expectedExceptionsMessageRegExp = "line 1:1: Invalid numeric literal: 12223222232535343423232435343")
    public void testParseErrorInvalidPositiveLongCast()
    {
        SQL_PARSER.createStatement("select CAST(12223222232535343423232435343 AS BIGINT)");
    }

    @Test(expectedExceptions = ParsingException.class, expectedExceptionsMessageRegExp = "line 1:1: Invalid numeric literal: 12223222232535343423232435343")
    public void testParseErrorInvalidNegativeLongCast()
    {
        SQL_PARSER.createStatement("select CAST(-12223222232535343423232435343 AS BIGINT)");
    }

    @Test
    public void testParsingExceptionPositionInfo()
    {
        try {
            SQL_PARSER.createStatement("select *\nfrom x\nwhere from");
            fail("expected exception");
        }
        catch (ParsingException e) {
            assertEquals(e.getMessage(), "line 3:7: no viable alternative at input 'from'");
            assertEquals(e.getErrorMessage(), "no viable alternative at input 'from'");
            assertEquals(e.getLineNumber(), 3);
            assertEquals(e.getColumnNumber(), 7);
        }
    }

    @Test
    public void testAllowIdentifierColon()
    {
        SqlParser sqlParser = new SqlParser(new SqlParserOptions().allowIdentifierSymbol(COLON));
        sqlParser.createStatement("select * from foo:bar");
    }

    @Test(expectedExceptions = ParsingException.class, expectedExceptionsMessageRegExp = "\\Qline 1:11: mismatched input '(' expecting EOF\\E")
    public void testInvalidArguments()
    {
        SQL_PARSER.createStatement("select foo(,1)");
    }

    @Test(expectedExceptions = ParsingException.class, expectedExceptionsMessageRegExp = "\\Qline 1:20: no viable alternative at input ')'\\E")
    public void testInvalidArguments2()
    {
        SQL_PARSER.createStatement("select foo(DISTINCT)");
    }

    @Test(expectedExceptions = ParsingException.class, expectedExceptionsMessageRegExp = "\\Qline 1:21: no viable alternative at input ','\\E")
    public void testInvalidArguments3()
    {
        SQL_PARSER.createStatement("select foo(DISTINCT ,1)");
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testAllowIdentifierAtSign()
    {
        SqlParser sqlParser = new SqlParser(new SqlParserOptions().allowIdentifierSymbol(AT_SIGN));
        sqlParser.createStatement("select * from foo@bar");
    }

    @Test
    public void testInterval()
            throws Exception
    {
        assertExpression("INTERVAL '123' YEAR", new IntervalLiteral("123", Sign.POSITIVE, IntervalField.YEAR));
        assertExpression("INTERVAL '123-3' YEAR TO MONTH", new IntervalLiteral("123-3", Sign.POSITIVE, IntervalField.YEAR, IntervalField.MONTH));
        assertExpression("INTERVAL '123' MONTH", new IntervalLiteral("123", Sign.POSITIVE, IntervalField.MONTH));
        assertExpression("INTERVAL '123' DAY", new IntervalLiteral("123", Sign.POSITIVE, IntervalField.DAY));
        assertExpression("INTERVAL '123 23:58:53.456' DAY TO SECOND", new IntervalLiteral("123 23:58:53.456", Sign.POSITIVE, IntervalField.DAY, IntervalField.SECOND));
        assertExpression("INTERVAL '123' HOUR", new IntervalLiteral("123", Sign.POSITIVE, IntervalField.HOUR));
        assertExpression("INTERVAL '23:59' HOUR TO MINUTE", new IntervalLiteral("23:59", Sign.POSITIVE, IntervalField.HOUR, IntervalField.MINUTE));
        assertExpression("INTERVAL '123' MINUTE", new IntervalLiteral("123", Sign.POSITIVE, IntervalField.MINUTE));
        assertExpression("INTERVAL '123' SECOND", new IntervalLiteral("123", Sign.POSITIVE, IntervalField.SECOND));
    }

    @Test
    public void testTime()
            throws Exception
    {
        assertExpression("TIME '03:04:05'", new TimeLiteral("03:04:05"));
    }

    @Test
    public void testCurrentTimestamp()
            throws Exception
    {
        assertExpression("CURRENT_TIMESTAMP", new CurrentTime(CurrentTime.Type.TIMESTAMP));
    }

    @Test(expectedExceptions = ParsingException.class, expectedExceptionsMessageRegExp = "line 1:1: expression is too large \\(stack overflow while parsing\\)")
    public void testStackOverflowExpression()
    {
        SQL_PARSER.createExpression(Joiner.on(" OR ").join(nCopies(2000, "x = y")));
    }

    @Test(expectedExceptions = ParsingException.class, expectedExceptionsMessageRegExp = "line 1:1: statement is too large \\(stack overflow while parsing\\)")
    public void testStackOverflowStatement()
    {
        SQL_PARSER.createStatement("SELECT " + Joiner.on(" OR ").join(nCopies(2000, "x = y")));
    }

    private static void assertStatement(String query, Statement expected)
    {
        assertParsed(query, expected, SQL_PARSER.createStatement(query));
    }

    private static void assertExpression(String expression, Expression expected)
    {
        assertParsed(expression, expected, SQL_PARSER.createExpression(expression));
    }

    private static void assertParsed(String input, Node expected, Node parsed)
    {
        if (!parsed.equals(expected)) {
            fail(format("expected\n\n%s\n\nto parse as\n\n%s\n\nbut was\n\n%s\n",
                    indent(input),
                    indent(formatSql(expected)),
                    indent(formatSql(parsed))));
        }
    }

    private static String indent(String value)
    {
        String indent = "    ";
        return indent + value.trim().replaceAll("\n", "\n" + indent);
    }
}
