/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.logging.slf4j;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.ChangeMethodName;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JContainer;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Markers;

import java.util.Arrays;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;

public class Log4j1MdcGetContextToCopyOfContextMap extends Recipe {

    private static final String GET_CONTEXT_PATTERN = "org.apache.log4j.MDC getContext()";
    private static final MethodMatcher GET_CONTEXT = new MethodMatcher(GET_CONTEXT_PATTERN);

    private static final JavaType MAP_TYPE = JavaType.ShallowClass.build("java.util.Map");
    private static final JavaType STRING_TYPE = JavaType.ShallowClass.build("java.lang.String");

    @Override
    public String getDisplayName() {
        return "Convert Log4j 1.x `MDC.getContext()` to `getCopyOfContextMap()`";
    }

    @Override
    public String getDescription() {
        return "Renames Log4j 1.x `org.apache.log4j.MDC.getContext()` (returns `Hashtable`) to " +
               "`getCopyOfContextMap()` (returns `Map`), and retypes a local variable declared as " +
               "`Hashtable` and initialized directly from it to `Map<String, String>`, since `Map` is " +
               "not assignable to `Hashtable`. Only directly-initialized local variable declarations are " +
               "retyped; fields and method-parameter flows are left unchanged. Does not change the " +
               "`org.apache.log4j.MDC` type; compose with a `ChangeType` to complete the migration.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(GET_CONTEXT), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                // Delegate the rename to the stock ChangeMethodName, which keeps the method's name and
                // type metadata consistent. It matches while the receiver is still org.apache.log4j.MDC.
                doAfterVisit(new ChangeMethodName(GET_CONTEXT_PATTERN, "getCopyOfContextMap", null, null).getVisitor());
                return super.visitCompilationUnit(cu, ctx);
            }

            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                J.VariableDeclarations mv = super.visitVariableDeclarations(multiVariable, ctx);
                // getContext() returns Hashtable but getCopyOfContextMap() returns Map; a Hashtable-typed
                // local initialized from it would no longer compile, so retype only the declaration's type.
                if (mv.getVariables().size() == 1 && TypeUtils.isOfClassType(mv.getType(), "java.util.Hashtable")) {
                    J.VariableDeclarations.NamedVariable nv = mv.getVariables().get(0);
                    if (nv.getInitializer() instanceof J.MethodInvocation &&
                        GET_CONTEXT.matches((J.MethodInvocation) nv.getInitializer())) {
                        maybeAddImport("java.util.Map");
                        maybeRemoveImport("java.util.Hashtable");
                        // Replace only the type expression so modifiers, annotations, the variable name,
                        // the initializer, and surrounding formatting are preserved. The variable's own
                        // type attribution is retyped to Map too, so the Hashtable import is seen as unused.
                        JavaType.Variable variableType = nv.getVariableType() == null ? null :
                                nv.getVariableType().withType(MAP_TYPE);
                        mv = mv.withTypeExpression(mapStringString(mv.getTypeExpression().getPrefix()))
                                .withVariables(singletonList(nv
                                        .withVariableType(variableType)
                                        .withName(nv.getName().withType(MAP_TYPE).withFieldType(variableType))));
                    }
                }
                return mv;
            }
        });
    }

    /**
     * Builds a {@code Map<String, String>} type expression, taking on the leading whitespace of the
     * type it replaces.
     */
    private static J.ParameterizedType mapStringString(Space prefix) {
        J.Identifier map = identifier("Map", Space.EMPTY, MAP_TYPE);
        J.Identifier firstArg = identifier("String", Space.EMPTY, STRING_TYPE);
        J.Identifier secondArg = identifier("String", Space.format(" "), STRING_TYPE);
        JContainer<Expression> typeParameters = JContainer.build(Space.EMPTY,
                Arrays.asList(JRightPadded.build((Expression) firstArg), JRightPadded.build((Expression) secondArg)),
                Markers.EMPTY);
        return new J.ParameterizedType(randomId(), prefix, Markers.EMPTY, map, typeParameters, MAP_TYPE);
    }

    private static J.Identifier identifier(String name, Space prefix, JavaType type) {
        return new J.Identifier(randomId(), prefix, Markers.EMPTY, emptyList(), name, type, null);
    }
}
