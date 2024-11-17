import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.moxie.ant.ClassUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.hamcrest.Matchers.containsInAnyOrder;

class AddFilesToOutputTaskTest {
    private final File probeFile = new File("../../build-gradle/classes/java/main/FederationClient.class");
    private final List<String> expectedDependencies = ClassUtil.getDependencies(probeFile);

    AddFilesToOutputTaskTest() throws IOException {
    }

    @Test
    void findImportedClasses() throws IOException {
        AddFilesToOutputTask addFilesToOutputTask = new AddFilesToOutputTask();
        Set<String> dependencies = new TreeSet(expectedDependencies.stream().map(s -> s.replaceAll("^[L(.*?);?$", "$1").replace("/", ".")).collect(Collectors.toList()));
        Set<String> importedClasses = new TreeSet(addFilesToOutputTask.findImportedClasses(probeFile));
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