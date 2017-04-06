
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2017 Arno Unkrig. All rights reserved.
 * Copyright (c) 2015-2016 TIBCO Software Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *       following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *       following disclaimer in the documentation and/or other materials provided with the distribution.
 *    3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote
 *       products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.codehaus.janino.tests;

import java.util.Arrays;
import java.util.Map;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.IClassBodyEvaluator;
import org.codehaus.commons.nullanalysis.Nullable;
import org.codehaus.janino.ClassBodyEvaluator;
import org.codehaus.janino.ExpressionEvaluator;
import org.codehaus.janino.Java.CompilationUnit;
import org.codehaus.janino.ScriptEvaluator;
import org.codehaus.janino.SimpleCompiler;
import org.junit.Assert;
import org.junit.Test;

/**
 * @see <a href="https://github.com/janino-compiler/janino/issues/19">GITHUB issue #19</a>: Get bytecode after compile
 */
public
class GithubIssuesTest {

    /**
     * A "degenerated" {@link ExpressionEvaluator} that suppresses the loading of the generated bytecodes into a
     * class loader.
     * <p>
     *   {@link ScriptEvaluator}, {@link ClassBodyEvaluator} and {@link SimpleCompiler} should be adaptable in very
     *   much the same way.
     * </p>
     *
     * @see #getBytecodes()
     */
    public static
    class ExpressionCompiler extends ExpressionEvaluator {

        @Nullable private Map<String, byte[]> classes;

        /**
         * @return The bytecodes that were generated when {@link #cook(String)} was invoked
         */
        public Map<String, byte[]>
        getBytecodes() {

            Map<String, byte[]> result = this.classes;
            if (result == null) throw new IllegalStateException("Must only be called after \"cook()\"");

            return result;
        }

        // --------------------------------------------------------------------

        // Override this method to prevent the loading of the class files into a ClassLoader.
        @Override public void
        cook(Map<String, byte[]> classes) {

            // Instead of loading the bytecodes into a ClassLoader, store the bytecodes in "this.classes".
            this.classes = classes;
        }

        // Override this method to prevent the retrieval of the generated java.lang.Classes.
        @Override protected void
        cook2(int count, CompilationUnit compilationUnit) throws CompileException {
            this.cook(compilationUnit);
        }
    }

    /**
     * <a href="https://github.com/janino-compiler/janino/pull/10">Replace if condition with
     * literal if possible to simplify if statement</a>
     */
    @Test public void
    testCompileToBytecode() throws CompileException {

        // Set up an ExpressionCompiler and cook the expression.
        ExpressionCompiler ec = new ExpressionCompiler();
        ec.cook("7");

        // Retrieve the generated bytecode from the ExpressionCompiler. The result is a map from class name
        // to the class's bytecode.
        Map<String, byte[]> result = ec.getBytecodes();
        Assert.assertNotNull(result);

        // verify that exactly _one_ class was generated.
        Assert.assertEquals(1, result.size());

        // Verify the class's name.
        byte[] ba = result.get(IClassBodyEvaluator.DEFAULT_CLASS_NAME);
        Assert.assertNotNull(ba);

        // Verify that the generated bytecode looks "reasonable", i.e. starts with the charcteristic
        // "magic bytes" and has an approximate size.
        Assert.assertArrayEquals(
            new byte[] { (byte) 0xca, (byte) 0xfe, (byte) 0xba, (byte) 0xbe },
            Arrays.copyOf(ba, 4)
        );
        Assert.assertTrue(Integer.toString(ba.length), ba.length > 200);
        Assert.assertTrue(Integer.toString(ba.length), ba.length < 300);
    }
}
