package com.ullink.gradle.nunit
import org.gradle.api.Plugin
import org.gradle.api.Project

class NUnitBasePlugin implements Plugin<Project> {
    void apply(Project project) {
        project.apply plugin: 'de.undercouch.download'
        project.tasks.withType(NUnit).whenTaskAdded { NUnit task ->
            applyNunitConventions(task, project)
        }
    }

    def applyNunitConventions(NUnit task, Project project) {
        task.conventionMapping.map "nunitDownloadUrl", { "https://github.com/nunit/${task.isV3 ? 'nunit' : 'nunitv2'}/releases/download" }
        task.conventionMapping.map "nunitVersion", { '2.6.4' }
        task.conventionMapping.map "nunitHome", {
            if (System.getenv()['NUNIT_HOME']) {
                return System.getenv()['NUNIT_HOME']
            }
            downloadNUnit(project, task)
        }
        if (project.plugins.hasPlugin('msbuild')) {
            task.dependsOn project.tasks.msbuild
            task.conventionMapping.map "testAssemblies", {
                project.tasks.msbuild.projects.findAll {
                    it.key =~ 'test' && it.value.properties.TargetPath
                }
                .collect {
                    it.value.getProjectPropertyPath('TargetPath')
                }
            }
        }
    }

    File downloadNUnit(Project project, NUnit task) {
        def version = task.getNunitVersion()
        def nunitDowloadUrl = task.getNunitDownloadUrl()
        def tempDir = task.getTemporaryDir()
        def NUnitName = "NUnit-$version"
        def NUnitZipFile = NUnitName + '.zip'
        def downloadedFile = new File(tempDir, NUnitZipFile)
        def nunitCacheDir = new File(new File(project.gradle.gradleUserHomeDir, 'caches'), 'nunit')
        if (!nunitCacheDir.exists()) {
            nunitCacheDir.mkdirs()
        }
        def nunitCacheDirForVersion = new File(nunitCacheDir, NUnitName)

        // special handling for nunit3 flat zip file
        def zipOutputDir = task.isV3 ? nunitCacheDirForVersion : nunitCacheDir;

        def ret = nunitCacheDirForVersion
        if (!ret.exists()) {
            project.logger.info "Downloading & Unpacking NUnit ${version}"
            project.download {
                src "$nunitDowloadUrl/$version/$NUnitZipFile"
                dest downloadedFile
            }
            project.copy {
                from project.zipTree(downloadedFile)
                into zipOutputDir
            }
        }
        ret
    }
}
