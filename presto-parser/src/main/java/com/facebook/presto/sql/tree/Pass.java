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
    public <R, C> R accept(AstVisitor<R, C> visitor, C context)
    {
        return visitor.visitPass(this, context);
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