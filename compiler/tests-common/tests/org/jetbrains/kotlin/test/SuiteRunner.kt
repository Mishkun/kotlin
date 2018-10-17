/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import org.junit.internal.runners.JUnit38ClassRunner
import org.junit.runner.Description
import org.junit.runner.Runner
import org.junit.runner.manipulation.Filter
import org.junit.runners.Suite
import org.junit.runners.model.InitializationError
import org.junit.runners.model.RunnerBuilder
import java.io.File
import java.lang.reflect.Modifier
import java.util.*

class ABuilderRunner : RunnerBuilder (){
    override fun runnerForClass(p0: Class<*>?): Runner {
        return JUnit38ClassRunner(p0)
    }
}

class SuiteRunner constructor(klass: Class<*>, builder: RunnerBuilder?) :
    Suite(builder, getAnnotatedClasses(klass).flatMap { collectDeclaredClasses(it, true) }.distinct().toTypedArray()) {

    init {
        filter(object : Filter() {
            override fun shouldRun(description: Description): Boolean {
                if (description.isTest) {
                    description.getAnnotation(TestMetadata::class.java)?.let { methodAnnotation ->
                        description.testClass.getAnnotation(TestMetadata::class.java)?.let {
                            val path = it.value + "/" + methodAnnotation.value
                            return !InTextDirectivesUtils.isDirectiveDefined(File(path).readText(), "// JVM_TARGET:")
                        }
                    }
                }
                return true
            }

            override fun describe(): String {
                return "skipped on JDK 6"
            }
        })
    }

    companion object {
        @Throws(InitializationError::class)
        private fun getAnnotatedClasses(klass: Class<*>): Array<Class<*>> {
            val annotation = klass.getAnnotation(SuiteClasses::class.java)
            return annotation.value.map { it.java }.toTypedArray()
        }
//        private fun getCollectedTests(): Test {
//            val innerClasses = collectDeclaredClasses(klass, false)
//            val unprocessedInnerClasses = unprocessedClasses(innerClasses)
//
//            if (unprocessedInnerClasses.isEmpty()) {
//                if (!innerClasses.isEmpty() && !hasTestMethods(klass)) {
//                    isFakeTest = true
//                    return JUnit3RunnerWithInners.FakeEmptyClassTest(klass)
//                } else {
//                    return TestSuite(klass.asSubclass(TestCase::class.java))
//                }
//            } else return if (unprocessedInnerClasses.size == innerClasses.size) {
//                createTreeTestSuite(klass)
//            } else {
//                TestSuite(klass.asSubclass(TestCase::class.java))
//            }
//        }
//
//        private fun createTreeTestSuite(root: Class<*>): Test {
//            val classes = LinkedHashSet(collectDeclaredClasses(root, true))
//            val classSuites = HashMap<Class<*>, TestSuite>()
//
//            for (aClass in classes) {
//                classSuites[aClass] = if (hasTestMethods(aClass)) TestSuite(aClass) else TestSuite(aClass.canonicalName)
//            }
//
//            for (aClass in classes) {
//                if (aClass.enclosingClass != null && classes.contains(aClass.enclosingClass)) {
//                    classSuites[aClass.enclosingClass].addTest(classSuites[aClass])
//                }
//            }
//
//            return classSuites.get(root)
//        }
//
//        private fun unprocessedClasses(classes: Collection<Class<*>>): Set<Class<*>> {
//            val result = LinkedHashSet<Class<*>>()
//            for (aClass in classes) {
//                if (!requestedRunners.contains(aClass)) {
//                    result.add(aClass)
//                }
//            }
//
//            return result
//        }

        private fun collectDeclaredClasses(klass: Class<*>, withItself: Boolean): List<Class<*>> {
            val result = ArrayList<Class<*>>()
            if (klass.enclosingClass != null && !Modifier.isStatic(klass.modifiers)) return emptyList()

            if (withItself) {
                result.add(klass)
            }

            for (aClass in klass.declaredClasses) {
                result.addAll(collectDeclaredClasses(aClass, true))
            }

            return result
        }

//        private fun hasTestMethods(klass: Class<*>): Boolean {
//            var currentClass: Class<*> = klass
//            while (Test::class.java.isAssignableFrom(currentClass)) {
//                for (each in MethodSorter.getDeclaredMethods(currentClass)) {
//                    if (isTestMethod(each)) return true
//                }
//                currentClass = currentClass.superclass
//            }
//
//            return false
//        }
    }
}
