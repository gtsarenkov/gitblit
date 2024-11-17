import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectories
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.InvokeDynamicInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode

import java.nio.file.FileSystems
import java.nio.file.PathMatcher
import java.util.zip.ZipFile
import java.util.zip.ZipEntry

@CacheableTask
class AddFilesToOutputTask extends DefaultTask {
    @Input
    final Property<String> mainClass = project.objects.property(String)

    @Input
    final ListProperty<String> classes = project.objects.listProperty(String)

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    final ConfigurableFileCollection runtimeClasspath = project.objects.fileCollection()

    @OutputDirectories
    final ConfigurableFileCollection outputDirs = project.objects.fileCollection()

    @Input
    final ListProperty<String> excludePatterns = project.objects.listProperty(String)

    private final Provider<ConfigurableFileCollection> outputFiles = project.provider { project.objects.fileCollection() }

    @OutputFiles
    Provider<ConfigurableFileCollection> getOutputFiles() {
        return outputFiles;
    }

    @TaskAction
    void addFiles() {
        if (runtimeClasspath.isEmpty()) {
            throw new GradleException("runtimeClasspath for task ${getName()} must not be empty")
        }
        runtimeClasspath.each { it -> logger.info("runtimeClasspath: ${it.absolutePath}") }
        Set<ResolutionResult> classFileData = findClassFiles([mainClass.get()] + this.classes.get())
        Set<ResolutionResult> resolvedFiles = resolveImportedClasses(classFileData)

        processOutputFiles(resolvedFiles)
    }

    protected Set<ResolutionResult> findClassFiles(Collection<String> classNames) {
        Set<ResolutionResult> classFileData = new HashSet<>()
        Set<File> jarFiles = new TreeSet<>()

        classNames.each { className ->
            runtimeClasspath.findAll { file -> !outputDirs.contains(file) }.each { file ->
                String classFilePath = className.replace('.', '/') + ".class"
                if (file.isDirectory()) {
                    File potentialFile = new File(file, classFilePath)
                    if (potentialFile.exists() && !isExcluded(potentialFile)) {
                        classFileData.add(new ResolutionResult(className, file, potentialFile, potentialFile.path - file.path))
                    }
                } else if (file.name.endsWith('.jar')) {
                    ZipFile zipFile = new ZipFile(file)
                    if (zipFile.entries().any { ZipEntry entry -> entry.name == classFilePath && !isExcluded(entry) }) {
                        jarFiles.add(file)
                    }
                }
            }
        }

        jarFiles.each { file -> outputFiles.get().from(file) }

        classFileData.removeAll { result -> outputFiles.get().contains(result.file) }

        return classFileData
    }


    protected Set<ResolutionResult> resolveImportedClasses(Set<ResolutionResult> classFileData) {
        Set<ResolutionResult> resolvedFiles = new TreeSet()
        Set<ResolutionResult> newClasses = new TreeSet(classFileData)

        while (!newClasses.isEmpty()) {
            Set<ResolutionResult> currentClassFiles = new HashSet<>(newClasses)
            newClasses.clear()

            currentClassFiles.each { result ->
                if (!resolvedFiles.contains(result)) {
                    resolvedFiles.add(result)

                    Set<String> importedClasses = new TreeSet(findImportedClasses(result.file))
                    Set<ResolutionResult> resolvedImportedFiles = findClassFiles(importedClasses)

                    newClasses.addAll(resolvedImportedFiles.findAll { !resolvedFiles.contains(it) })
                    resolvedFiles.addAll(resolvedImportedFiles)
                }
            }
        }

        return resolvedFiles
    }

    protected Set<String> findImportedClasses(File classFile) {
        ClassNode classNode = new ClassNode()
        ClassReader classReader = new ClassReader(classFile.bytes)
        classReader.accept(classNode, 0)

        Set<String> importedClasses = new TreeSet()

        String relativePath = project.buildDir.toPath().relativize(classFile.toPath()).toString()
        // Collect method references
        classNode.methods.each { method ->
            method.instructions.each { instruction ->
                if (instruction instanceof MethodInsnNode) {
                    def value = instruction.owner.replace('/', '.')
                    importedClasses.add(value)
                    logger.lifecycle("Method instruction found ${relativePath}: ${value} ${instruction.owner}.${instruction.name}")
                } else if (instruction instanceof FieldInsnNode) {
                    def value = Type.getType(instruction.desc).className.replace('/', '.')
                    importedClasses.add(value)
                    logger.lifecycle("Field instruction found ${relativePath}: ${value} ${instruction.owner}.${instruction.name}")
                } else if (instruction instanceof LdcInsnNode && instruction.cst instanceof Type) {
                    def value = instruction.cst.className.replace('/', '.')
                    importedClasses.add(value)
                    logger.lifecycle("LDC Type instruction found ${relativePath}: ${value} ${instruction.cst.className}")
                } else if (instruction instanceof InvokeDynamicInsnNode) {
                    def value = instruction.bsm.owner.replace('/', '.')
                    importedClasses.add(value)
                    logger.lifecycle("InvokeDynamic instruction found ${relativePath}: ${value} ${instruction.bsm.name}")
                }
            }
        }
        // Collect field references
        classNode.fields.each { field ->
            def value = Type.getType(field.desc).className.replace('/', '.')
            importedClasses.add(value)
            logger.lifecycle("Field found ${relativePath}: ${value} ${field.name}")
        }

        // Collect inner classes
        classNode.innerClasses.each { innerClass ->
            if (innerClass.name) {
                def value = innerClass.name.replace('/', '.')
                importedClasses.add(value)
                logger.lifecycle("Inner class found ${relativePath}: ${value}")
            }
        }

        // Collect outer classes
        collectOuterClasses(classNode) {
            def outerClass = classNode.outerClass.replace('/', '.')
            if (!importedClasses.contains(outerClass)) {
                logger.lifecycle("Outer class found ${relativePath}: ${outerClass}")

                def classes = findImportedClasses(outerClassNode)
                importedClasses.addAll(classes)
            }
        }

        def uniqueClasses = importedClasses

        return uniqueClasses.findAll { className ->
            !isExcluded(new File(className.replace('.', '/') + ".class"))
        }
    }

    protected void collectOuterClasses(ClassNode classNode, Closure closure) {
        if (classNode.outerClass != null) {
            closure(outerClass)
        }
    }

    protected boolean isExcluded(File file) {
        def path = file.toPath()
        boolean excluded = excludePatterns.get().any { pattern -> getPathMatcher("glob:$pattern").matches(path) }
        if (excluded) {
            logger.lifecycle("Excluded file: ${file.path}")
        }
        return excluded
    }

    protected boolean isExcluded(ZipEntry entry) {
        def path = FileSystems.getDefault().getPath(entry.name)
        excludePatterns.get().any { pattern -> getPathMatcher("glob:$pattern").matches(path) }
    }

    protected PathMatcher getPathMatcher(String pattern) {
        FileSystems.getDefault().getPathMatcher(pattern)
    }

    protected void processOutputFiles(Set<ResolutionResult> resolvedFiles) {
        Set<File> jarsInClasspath = runtimeClasspath.files.findAll { it.name.endsWith('.jar') }
        Set<String> usedClasses = resolvedFiles.collect { it.file.name.replace('.class', '').replace('/', '.') }

        jarsInClasspath.each { jarFile ->
            ZipFile zipFile = new ZipFile(jarFile)
            zipFile.entries().each { entry ->
                if (usedClasses.contains(entry.name.replace('.class', '').replace('/', '.'))) {
                    outputFiles.get().from(jarFile)
                    return
                }
            }
        }

        outputDirs.each { dir ->
            resolvedFiles.each { result ->
                if (result.file.exists()) {
                    def relativePath = result.relativePath
                    project.copy {
                        from result.file
                        into new File(dir, relativePath).parentFile
                    }
                } else {
                    logger.lifecycle("File does not exist: ${result.file}")
                }
            }
        }
    }
}