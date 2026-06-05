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

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class WrapLog4j1MdcPutValueInStringValueOfTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new WrapLog4j1MdcPutValueInStringValueOf())
          .parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "log4j-1.2.+"));
    }

    @DocumentExample
    @Test
    void wrapsNonStringValues() {
        //language=java
        rewriteRun(
          java(
            """
              import org.apache.log4j.MDC;

              import java.util.Map;
              import java.util.function.Supplier;

              class Test {
                  void method(Map<String, String> map, Object obj, Throwable t, Supplier<String> supplier) {
                      MDC.put("map", map);
                      MDC.put("obj", obj);
                      MDC.put("throwable", t);
                      MDC.put("supplier", supplier);
                  }
              }
              """,
            """
              import org.apache.log4j.MDC;

              import java.util.Map;
              import java.util.function.Supplier;

              class Test {
                  void method(Map<String, String> map, Object obj, Throwable t, Supplier<String> supplier) {
                      MDC.put("map", String.valueOf(map));
                      MDC.put("obj", String.valueOf(obj));
                      MDC.put("throwable", String.valueOf(t));
                      MDC.put("supplier", String.valueOf(supplier));
                  }
              }
              """
          )
        );
    }

    @Test
    void untouchedStringLiteral() {
        //language=java
        rewriteRun(
          java(
            """
              import org.apache.log4j.MDC;

              class Test {
                  void method() {
                      MDC.put("key", "value");
                  }
              }
              """
          )
        );
    }

    @Test
    void untouchedTypedString() {
        //language=java
        rewriteRun(
          java(
            """
              import org.apache.log4j.MDC;

              class Test {
                  void method(String value) {
                      MDC.put("key", value);
                  }
              }
              """
          )
        );
    }

    @Test
    void untouchedNull() {
        //language=java
        rewriteRun(
          java(
            """
              import org.apache.log4j.MDC;

              class Test {
                  void method() {
                      MDC.put("key", null);
                  }
              }
              """
          )
        );
    }

    @Test
    void untouchedAlreadyWrapped() {
        //language=java
        rewriteRun(
          java(
            """
              import org.apache.log4j.MDC;

              class Test {
                  void method(Object value) {
                      MDC.put("key", String.valueOf(value));
                  }
              }
              """
          )
        );
    }
}
