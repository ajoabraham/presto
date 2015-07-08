package com.facebook.presto.sql.tree;

import com.google.common.base.Objects;

public class Pass extends Literal {
    private final String literal;

    public Pass(String literal) {
        this.literal = literal;
    }

    public String getLiteral() {
        return literal;
    }

    public String getActualContents() {
        if (literal.length() > 0) {
            return literal.substring(1, literal.length() - 1); // trim lft & rgt parens
        } else {
            return literal;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getLiteral());
    }

    @Override
    public boolean equals(Object obj) {
        return Objects.equal(this, obj);
    }
}
