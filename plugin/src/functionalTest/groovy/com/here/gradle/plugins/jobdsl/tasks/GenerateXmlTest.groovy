package com.here.gradle.plugins.jobdsl.tasks

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

class GenerateXmlTest extends Specification {

    @Rule
    final TemporaryFolder testProjectDir = new TemporaryFolder()
    File buildFile
    List<File> pluginClasspath
    GradleRunner gradleRunner

    def classpathString() {
        pluginClasspath
                .collect { it.absolutePath.replace('\\', '\\\\') } // escape backslashes in Windows paths
                .collect { "'$it'" }
                .join(", ")
    }

    def readResource(String path) {
        getClass().classLoader.getResource(path).text
    }

    def readBuildGradle(String path) {
        readResource(path).replace('CLASSPATH_STRING', classpathString())
    }

    def copyResourceToTestDir(String from, String to = 'src/jobdsl/jobdsl.groovy') {
        testProjectDir.newFile(to) << readResource(from)
    }

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
        testProjectDir.newFolder('src', 'jobdsl')

        gradleRunner = GradleRunner.create().withProjectDir(testProjectDir.root)

        def resource = getClass().classLoader.findResource('plugin-under-test-metadata.properties')
        if (resource == null) {
            throw new IllegalStateException('Could not find metadata')
        }

        pluginClasspath = resource.readLines()[0].split('=')[1].split('\\\\:').collect { new File(it) }
    }

    def 'empty freestyle job is generated correctly'() {
        given:
        buildFile << readBuildGradle('generateXml/build.gradle')
        copyResourceToTestDir('generateXml/empty-freestyle-job.groovy')

        when:
        def result = gradleRunner
                .withArguments('dslGenerateXml')
                .build()

        then:
        result.task(':dslGenerateXml').outcome == TaskOutcome.SUCCESS

        def generatedFile = new File(testProjectDir.root, 'build/jobdsl/xml/job.xml')
        generatedFile.isFile()
        generatedFile.text == readResource('generateXml/empty-freestyle-job.xml')
    }

    def 'empty list view is generated correctly'() {
        given:
        buildFile << readBuildGradle('generateXml/build.gradle')
        copyResourceToTestDir('generateXml/empty-list-view.groovy')

        when:
        def result = gradleRunner
                .withArguments('dslGenerateXml')
                .build()

        then:
        result.task(':dslGenerateXml').outcome == TaskOutcome.SUCCESS

        def generatedFile = new File(testProjectDir.root, 'build/jobdsl/xml/view.xml')
        generatedFile.isFile()
        generatedFile.text == readResource('generateXml/empty-list-view.xml')
    }

    def 'folder is generated correctly'() {
        given:
        buildFile << readBuildGradle('generateXml/build.gradle')
        copyResourceToTestDir('generateXml/folder.groovy')

        when:
        def result = gradleRunner
                .withArguments('dslGenerateXml')
                .build()

        then:
        result.task(':dslGenerateXml').outcome == TaskOutcome.SUCCESS

        def generatedFile = new File(testProjectDir.root, 'build/jobdsl/xml/folder.xml')
        generatedFile.isFile()
        generatedFile.text == readResource('generateXml/folder.xml')
    }

    def 'job in folder is generated in the right subfolder'() {
        given:
        buildFile << readBuildGradle('generateXml/build.gradle')
        copyResourceToTestDir('generateXml/job-in-folder.groovy')

        when:
        def result = gradleRunner
                .withArguments('dslGenerateXml')
                .build()

        then:
        result.task(':dslGenerateXml').outcome == TaskOutcome.SUCCESS

        new File(testProjectDir.root, 'build/jobdsl/xml/folder.xml').isFile()
        new File(testProjectDir.root, 'build/jobdsl/xml/folder/job.xml').isFile()
    }

    @Unroll
    def 'filter applies to folders'() {
        given:
        buildFile << readBuildGradle('generateXml/build.gradle')
        copyResourceToTestDir('generateXml/filter-folders.groovy')

        when:
        def result = gradleRunner
                .withArguments('dslGenerateXml', '--filter=.*unfiltered.*')
                .build()

        then:
        result.task(':dslGenerateXml').outcome == TaskOutcome.SUCCESS

        new File(testProjectDir.root, 'build/jobdsl/xml/folder-unfiltered.xml').isFile()
        new File(testProjectDir.root, 'build/jobdsl/xml/folder-unfiltered/subfolder.xml').isFile()
        !new File(testProjectDir.root, 'build/jobdsl/xml/folder-filtered.xml').isFile()
        !new File(testProjectDir.root, 'build/jobdsl/xml/folder-filtered/subfolder.xml').isFile()
    }

    def 'filter applies to jobs'() {
        given:
        buildFile << readBuildGradle('generateXml/build.gradle')
        copyResourceToTestDir('generateXml/filter-jobs.groovy')

        when:
        def result = gradleRunner
                .withArguments('dslGenerateXml', '--filter=.*unfiltered.*')
                .build()

        then:
        result.task(':dslGenerateXml').outcome == TaskOutcome.SUCCESS

        new File(testProjectDir.root, 'build/jobdsl/xml/job-unfiltered.xml').isFile()
        new File(testProjectDir.root, 'build/jobdsl/xml/folder/job-unfiltered.xml').isFile()
        !new File(testProjectDir.root, 'build/jobdsl/xml/job-filtered.xml').isFile()
        !new File(testProjectDir.root, 'build/jobdsl/xml/folder/job-filtered.xml').isFile()
    }

    def 'filter applies to views'() {
        given:
        buildFile << readBuildGradle('generateXml/build.gradle')
        copyResourceToTestDir('generateXml/filter-views.groovy')

        when:
        def result = gradleRunner
                .withArguments('dslGenerateXml', '--filter=.*unfiltered.*')
                .build()

        then:
        result.task(':dslGenerateXml').outcome == TaskOutcome.SUCCESS

        new File(testProjectDir.root, 'build/jobdsl/xml/view-unfiltered.xml').isFile()
        new File(testProjectDir.root, 'build/jobdsl/xml/folder/view-unfiltered.xml').isFile()
        !new File(testProjectDir.root, 'build/jobdsl/xml/view-filtered.xml').isFile()
        !new File(testProjectDir.root, 'build/jobdsl/xml/folder/view-filtered.xml').isFile()
    }

    def 'can create job in filtered folder'() {
        given:
        buildFile << readBuildGradle('generateXml/build.gradle')
        copyResourceToTestDir('generateXml/job-in-filtered-folder.groovy')

        when:
        def result = gradleRunner
                .withArguments('dslGenerateXml', '--filter=.*job.*')
                .build()

        then:
        result.task(':dslGenerateXml').outcome == TaskOutcome.SUCCESS

        !new File(testProjectDir.root, 'build/jobdsl/xml/folder.xml').isFile()
        new File(testProjectDir.root, 'build/jobdsl/xml/folder/job.xml').isFile()
    }

    def 'global configuration is available in DSL scripts'() {
        given:
        buildFile << readBuildGradle('generateXml/build-with-configuration.gradle')
        copyResourceToTestDir('generateXml/global-configuration.groovy')

        when:
        def result = gradleRunner
                .withArguments('dslGenerateXml')
                .build()

        then:
        result.task(':dslGenerateXml').outcome == TaskOutcome.SUCCESS
        result.output.contains('global configuration')
    }

    def 'server specific configuration is available in DSL scripts'() {
        given:
        buildFile << readBuildGradle('generateXml/build-with-configuration.gradle')
        copyResourceToTestDir('generateXml/server-specific-configuration.groovy')

        when:
        def result = gradleRunner
                .withArguments('dslGenerateXml', '--server=localhost')
                .build()

        then:
        result.task(':dslGenerateXml').outcome == TaskOutcome.SUCCESS
        result.output.contains('server specific configuration')
    }

    def 'server specific configuration overrides global configuration'() {
        given:
        buildFile << readBuildGradle('generateXml/build-with-configuration.gradle')
        copyResourceToTestDir('generateXml/server-specific-configuration-overrides-global-configuration.groovy')

        when:
        def result = gradleRunner
                .withArguments('dslGenerateXml', '--server=localhost')
                .build()

        then:
        result.task(':dslGenerateXml').outcome == TaskOutcome.SUCCESS
        result.output.contains('server specific value')
    }

    def 'adding source set works'() {
        given:
        buildFile << readBuildGradle('generateXml/build-add-custom-source-set.gradle')
        testProjectDir.newFile('src/jobdsl/dsl.groovy') << 'println "default source set"'
        testProjectDir.newFolder('src', 'custom')
        testProjectDir.newFile('src/custom/dsl.groovy') << 'println "custom source set"'

        when:
        def result = gradleRunner
                .withArguments('dslGenerateXml')
                .build()

        then:
        result.task(':dslGenerateXml').outcome == TaskOutcome.SUCCESS
        result.output.contains('custom source set')
        result.output.contains('default source set')
    }

    def 'replacing source set works'() {
        given:
        buildFile << readBuildGradle('generateXml/build-replace-source-set.gradle')
        testProjectDir.newFile('src/jobdsl/dsl.groovy') << 'println "default source set"'
        testProjectDir.newFolder('src', 'custom')
        testProjectDir.newFile('src/custom/dsl.groovy') << 'println "custom source set"'

        when:
        def result = gradleRunner
                .withArguments('dslGenerateXml')
                .build()

        then:
        result.task(':dslGenerateXml').outcome == TaskOutcome.SUCCESS
        result.output.contains('custom source set')
        !result.output.contains('default source set')
    }

    def 'task fails on compile error in DSL script'() {
        given:
        buildFile << readBuildGradle('generateXml/build.gradle')
        testProjectDir.newFile('src/jobdsl/jobdsl.groovy') << 'a'

        when:
        def result = gradleRunner
                .withArguments('dslGenerateXml')
                .buildAndFail()

        then:
        result.task(':dslGenerateXml').outcome == TaskOutcome.FAILED
    }

    def 'task fails when no DSL scripts are found'() {
        given:
        buildFile << readBuildGradle('generateXml/build.gradle')

        when:
        def result = gradleRunner
                .withArguments('dslGenerateXml')
                .buildAndFail()

        then:
        result.task(':dslGenerateXml').outcome == TaskOutcome.FAILED
    }

}