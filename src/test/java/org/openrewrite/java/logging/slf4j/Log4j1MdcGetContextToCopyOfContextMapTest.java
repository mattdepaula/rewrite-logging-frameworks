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

class Log4j1MdcGetContextToCopyOfContextMapTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new Log4j1MdcGetContextToCopyOfContextMap())
          .parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "log4j-1.2.+"));
    }

    @DocumentExample
    @Test
    void retypesHashtableReceiverToMap() {
        //language=java
        rewriteRun(
          java(
            """
              import org.apache.log4j.MDC;

              import java.util.Hashtable;

              class Test {
                  static void method() {
                      Hashtable context = MDC.getContext();
                  }
              }
              """,
            """
              import org.apache.log4j.MDC;

              import java.util.Map;

              class Test {
                  static void method() {
                      Map<String, String> context = MDC.getCopyOfContextMap();
                  }
              }
              """
          )
        );
    }

    @Test
    void renamesGetContext() {
        //language=java
        rewriteRun(
          java(
            """
              import org.apache.log4j.MDC;

              import java.util.Map;

              class Test {
                  Map method() {
                      return MDC.getContext();
                  }
              }
              """,
            """
              import org.apache.log4j.MDC;

              import java.util.Map;

              class Test {
                  Map method() {
                      return MDC.getCopyOfContextMap();
                  }
              }
              """
          )
        );
    }

    @Test
    void retypesHashtableReceiverPreservingModifiers() {
        //language=java
        rewriteRun(
          java(
            """
              import org.apache.log4j.MDC;

              import java.util.Hashtable;

              class Test {
                  static void method() {
                      final Hashtable context = MDC.getContext();
                  }
              }
              """,
            """
              import org.apache.log4j.MDC;

              import java.util.Map;

              class Test {
                  static void method() {
                      final Map<String, String> context = MDC.getCopyOfContextMap();
                  }
              }
              """
          )
        );
    }
}
