package org.sspiderd.testsplitter

import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest
import java.util.*
import kotlin.streams.asSequence

class TestSplitterPlugin : Plugin<Project> {

    fun randomString(): String {
        val source = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        return java.util.Random().ints(8, 0, source.length)
                .asSequence()
                .map(source::get)
                .joinToString("")
    }

    // Gets back with this:

    // @Blah
    // @Blah
    // @Test
    // Blah
    // blah abvlkajdf lakdfj sldf {
    val testMethodPrefixRegex = Regex("(@(?!(Parameterized)?Test\\b)\\w+\n?)*@(Parameterized)?Test\\b.*?\\{\\s*\n", setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL))

    fun String.md5(): String {
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(toByteArray())).toString(16).padStart(32, '0')
    }

    fun writeToFileUnlessContentsAreTheSame(file: File, content: String, project: Project) {
        if (!file.exists()) {
            file.createNewFile()
        }
        val currentContentOfFile = file.readText()
        val currentMd5 = currentContentOfFile.md5()

        val newMd5 = content.md5()

        when (newMd5 == currentMd5) {
            true -> project.logger.info("md5 hashes are the same for $file. not writing file...")
            false -> {
                file.writeText(content)
                project.logger.info("Wrote new file $file")
            }
        }
    }

    fun locateEncompassingTestRange(jRange: IntRange, content: String): IntRange {
        var currentCharIndex = jRange.last + 1
        var currentChar = content[currentCharIndex]
        val stack = Stack<Int>()
        stack.push(0)
        while (true) {
            when (currentChar) {
                '{' -> {
                    stack.push(0)
                }
                '}' -> {
                    stack.pop()
                    if (stack.size == 0) {
                        return IntRange(jRange.start, currentCharIndex)
                    }
                }
            }
            currentCharIndex++
            currentChar = content[currentCharIndex]
        }
    }

    fun splitTests(project: Project) {
        val dir = File("${project.projectDir}/src/test")
        val generatedDir = File("${project.projectDir}/gen/test")
        dir.walkTopDown().forEach { srcFile ->
            val relativeLocation = srcFile.toRelativeString(dir)
            val destFile = File("$generatedDir/$relativeLocation")
            if (srcFile.isDirectory) {
                if (!destFile.exists()) {
                    destFile.mkdirs()
                }
                return@forEach
            }

            val content = srcFile.readText()

            if (relativeLocation.startsWith("resources")) {
                destFile.writeText(content)
                return@forEach
            }

            val indexRangesOfTestAnnotaionsInTheFile = testMethodPrefixRegex.findAll(content).map { it.range }

            project.logger.info("Found ${indexRangesOfTestAnnotaionsInTheFile.toList().size} Tests for '$relativeLocation' ")

            val indexRangesForTheWholeTests = indexRangesOfTestAnnotaionsInTheFile.map { locateEncompassingTestRange(it, content) }.toList()

            (0 until indexRangesForTheWholeTests.size).forEach { i ->
                var newContent = content
                indexRangesForTheWholeTests.forEachIndexed J@{ j: Int, jRange: IntRange? ->
                    if (i == j) return@J
                    if (jRange != null) {
                        newContent = newContent.replaceRange(jRange, " ".repeat(jRange.last - jRange.first + 1))
                    }
                }

                val fixedString = "VI"
                val numberedFile = File(destFile.absolutePath.replace(".", "_$i."))
                if (!numberedFile.exists()) {
                    numberedFile.createNewFile()
                }
                val currentContent = numberedFile.readText()
                val currentRawContent = currentContent.replace(Regex("${fixedString}_\\w+?_${fixedString}_(\\w+)_\\d+"), "$1")
                val md5OfCurrentRawContent = currentRawContent.md5()
                val md5OfNewRawContent = newContent.md5()

                when (md5OfCurrentRawContent == md5OfNewRawContent) {
                    true -> project.logger.info("md5 hashes are the same for $numberedFile. not writing file...")
                    false -> {
                        val randomString = randomString()
                        val pseudorandomString = "${fixedString}_${randomString}_$fixedString"
                        newContent = newContent.replaceFirst(Regex("class\\s+?(\\w+)?\\s+"), "class ${pseudorandomString}_$1_$i ")
                        // println(newContent)
                        numberedFile.writeText(newContent)
                        project.logger.info("Wrote new file  $numberedFile...")
                    }
                }
            }
            val fileWithoutTests: String = indexRangesForTheWholeTests.fold(content) { newContent, range ->
                newContent.replaceRange(range, " ".repeat(range.last - range.first + 1))
            }
            writeToFileUnlessContentsAreTheSame(destFile, fileWithoutTests, project)
        }
    }

    override fun apply(project: Project) {
        project.task("splitTests") {
            it.doLast {
                splitTests(project)
            }
        }
    }
}