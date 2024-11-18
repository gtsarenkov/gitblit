import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.moxie.ant.ClassFilter;
import org.moxie.ant.ClassUtil;
import org.gradle.api.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsInAnyOrder;

import org.mockito.Mockito;

class AddFilesToOutputTaskTest {
    private final File probeFile = new File("../build-gradled/classes/java/main/com/gitblit/FederationClient.class");
    private final List<String> expectedDependencies = ClassUtil.getDependencies(probeFile);
    private Logger logger;
    private AddFilesToOutputTask addFilesToOutputTask;

    static {
        System.out.println("AAAAAAAAAAAAAAA " + new File(".").getAbsolutePath());
    }

    AddFilesToOutputTaskTest() throws IOException {
    }

    @BeforeEach
    void setup() throws IOException {
        Project project = ProjectBuilder.builder().build();
        File buildDir = new File("../build-gradled");
        project.getLayout().getBuildDirectory().set(buildDir);
        addFilesToOutputTask = project.getTasks().create("addFilesToOutputTask", AddFilesToOutputTask.class);
        logger = addFilesToOutputTask.getLogger();
//        expectedDependencies = ClassUtil.getDependencies(new File(buildDir, probeFile.toString()));
    }

    @Test
    void findImportedClasses() throws IOException {
        logger.lifecycle("BBBBBBBBBBBBBB " + probeFile.toPath().normalize().toAbsolutePath() + ":" + probeFile.exists());
        ClassFilter classFilter = new ClassFilter(new org.moxie.ant.Logger(Mockito.mock(org.apache.tools.ant.Project.class)));
        Set<String> dependencies = new TreeSet<>(expectedDependencies.stream().filter(classFilter::include).map(s -> s.replaceAll("^\\[L(.*?);?$", "$1").replace("/", ".")).collect(Collectors.toList()));
        Set<String> importedClasses = new TreeSet<>(addFilesToOutputTask.findImportedClasses(probeFile));
//        if (!importedClasses.equals(dependencies)) {
//            Set<String> union = new TreeSet(importedClasses);
//            union.addAll(dependencies);
//            Set<String> intersection = new TreeSet(importedClasses);
//            intersection.retainAll(dependencies);
//            union.removeAll(intersection);
//            union.each { dd -> logger.error("error difference ${project.buildDir.toPath().relativize(result.file.toPath())} ${dd} new=${importedClasses.contains(dd)} old=${dependencies.contains(dd)}") }
//        }
        MatcherAssert.assertThat(importedClasses, containsInAnyOrder(dependencies.toArray()));
    }
}