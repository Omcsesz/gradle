/*
 * Copyright 2021 the original author or authors.
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

package org.gradle.api.plugins.quality.internal;

import groovy.lang.Closure;
import org.apache.tools.ant.Project;
import org.gradle.api.AntBuilder;
import org.gradle.api.GradleException;
import org.gradle.api.internal.project.ant.AntLoggingAdapter;
import org.gradle.api.internal.project.ant.BasicAntBuilder;
import org.gradle.api.internal.project.antbuilder.AntBuilderDelegate;
import org.gradle.internal.classloader.VisitableURLClassLoader;
import org.gradle.internal.classpath.ClassPath;
import org.gradle.internal.jvm.Jvm;
import org.gradle.util.internal.ClosureBackedAction;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AntWorkAction<T extends WorkParameters> implements WorkAction<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AntWorkAction.class);

    @Override
    public void execute() {
        LOGGER.info("Running {} with toolchain '{}'.", getActionName(), Jvm.current().getJavaHome().getAbsolutePath());
        ClassLoader originalLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader classLoader = new VisitableURLClassLoader("ant-work-action-classpath", originalLoader, getClassPath());
        Thread.currentThread().setContextClassLoader(classLoader);
        AntBuilder antBuilder = new BasicAntBuilder();
        AntLoggingAdapter antLogger = new AntLoggingAdapter();
        try {
            configureAntBuilder(antBuilder, antLogger);
            Object delegate = new AntBuilderDelegate(antBuilder, classLoader);
            ClosureBackedAction.execute(delegate, getAntClosure());
        } finally {
            Thread.currentThread().setContextClassLoader(originalLoader);
            disposeBuilder(antBuilder, antLogger);
        }
    }

    protected abstract ClassPath getClassPath();

    protected abstract String getActionName();

    protected abstract Closure<Object> getAntClosure();

    private void configureAntBuilder(AntBuilder antBuilder, AntLoggingAdapter antLogger) {
        try {
            Project project = getProject(antBuilder);
            project.removeBuildListener(project.getBuildListeners().get(0));
            project.addBuildListener(antLogger);
        } catch (Exception ex) {
            throw new GradleException("Unable to configure AntBuilder", ex);
        }
    }

    private void disposeBuilder(Object antBuilder, AntLoggingAdapter antLogger) {
        try {
            Project project = getProject(antBuilder);
            project.removeBuildListener(antLogger);
            antBuilder.getClass().getDeclaredMethod("close").invoke(antBuilder);
        } catch (Exception ex) {
            throw new GradleException("Unable to dispose AntBuilder", ex);
        }
    }

    private Project getProject(Object antBuilder) throws Exception {
        return (Project) antBuilder.getClass().getMethod("getProject").invoke(antBuilder);
    }
}
