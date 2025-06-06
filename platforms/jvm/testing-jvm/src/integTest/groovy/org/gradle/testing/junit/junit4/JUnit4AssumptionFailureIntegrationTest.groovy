/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.testing.junit.junit4

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.fixtures.DefaultTestExecutionResult

class JUnit4AssumptionFailureIntegrationTest extends AbstractIntegrationSpec {
    def "captures assumption failures"() {
        buildFile << """
            plugins {
                id 'java-library'
            }
            ${mavenCentralRepository()}

            testing {
                suites {
                    test {
                        useJUnit()
                        targets {
                            all {
                                testTask.configure {
                                    addTestListener(new TestListener() {
                                        void beforeSuite(TestDescriptor suite) {}
                                        void afterSuite(TestDescriptor suite, TestResult result) {}
                                        void beforeTest(TestDescriptor testDescriptor) {}
                                        void afterTest(TestDescriptor testDescriptor, TestResult result) {
                                            assert result.assumptionFailure != null
                                            println("Assumption failure: " + result.assumptionFailure.details.message)
                                        }
                                    })
                                }
                            }
                        }
                    }
                }
            }
        """
        file("src/test/java/com/example/MyTest.java") << """
            package com.example;

            import org.junit.Assume;
            import org.junit.Test;

            public class MyTest {
                @Test
                public void theTest() {
                    Assume.assumeTrue("skipped reason", false);
                }
            }
        """
        when:
        succeeds("test")
        then:
        outputContains("Assumption failure: skipped reason")
        def testResult = new DefaultTestExecutionResult(testDirectory)
        testResult.testClass("com.example.MyTest").assertTestSkipped("theTest") {
            assert it.message == "skipped reason"
            assert it.type == "org.junit.AssumptionViolatedException"
            assert it.text.contains("skipped reason")
        }
    }

    def "does not capture ignored tests as assumption failures"() {
        buildFile << """
            plugins {
                id 'java-library'
            }
            ${mavenCentralRepository()}

            testing {
                suites {
                    test {
                        useJUnit()
                        targets {
                            all {
                                testTask.configure {
                                    addTestListener(new TestListener() {
                                        void beforeSuite(TestDescriptor suite) {}
                                        void afterSuite(TestDescriptor suite, TestResult result) {}
                                        void beforeTest(TestDescriptor testDescriptor) {}
                                        void afterTest(TestDescriptor testDescriptor, TestResult result) {
                                            println("No assumption failure")
                                            assert result.assumptionFailure == null
                                        }
                                    })
                                }
                            }
                        }
                    }
                }
            }
        """
        file("src/test/java/com/example/MyTest.java") << """
            package com.example;

            import org.junit.Ignore;
            import org.junit.Test;

            public class MyTest {
                @Test
                @Ignore
                public void theTest() {
                    // fail the test if it actually runs
                    assert false;
                }
            }
        """
        when:
        succeeds("test")
        then:
        outputContains("No assumption failure")
        def testResult = new DefaultTestExecutionResult(testDirectory)
        testResult.testClass("com.example.MyTest").assertTestSkipped("theTest") {
            assert it.message.isEmpty()
            assert it.type.isEmpty()
            assert it.text.isEmpty()
        }
    }
}
