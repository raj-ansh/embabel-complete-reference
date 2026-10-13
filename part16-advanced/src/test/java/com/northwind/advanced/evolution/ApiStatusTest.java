package com.northwind.advanced.evolution;

import org.junit.jupiter.api.Test;
import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.ClassReader;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Chapter 115: which Embabel types and methods are marked @ApiStatus.Experimental, @ApiStatus.Internal or
 * deprecated? The ApiStatus annotations are kept in class files but not at runtime, so reflection can't see
 * them; this test reads the bytecode of every Embabel class on the classpath instead.
 */
class ApiStatusTest {

    static final String EXPERIMENTAL = "Lorg/jetbrains/annotations/ApiStatus$Experimental;";
    static final String INTERNAL = "Lorg/jetbrains/annotations/ApiStatus$Internal;";
    static final List<String> DEPRECATED = List.of("Lkotlin/Deprecated;", "Ljava/lang/Deprecated;");

    record Marked(String status, String owner, String member, StringBuilder message) {

        String display() {
            String type = owner.replace('$', '.');
            return member.isEmpty() ? type : type + "." + member + "()";
        }
    }

    @Test
    void experimentalInternalAndDeprecatedApis() throws IOException {
        List<Marked> marked = new ArrayList<>();
        Map<String, Integer> classesByArea = new TreeMap<>();
        Resource[] classes = new PathMatchingResourcePatternResolver().getResources("classpath*:com/embabel/**/*.class");
        for (Resource resource : classes) {
            try (InputStream in = resource.getInputStream()) {
                new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9) {
                    String name;

                    @Override
                    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                        this.name = name.replace('/', '.');
                        if ((access & Opcodes.ACC_PUBLIC) != 0 && !name.contains("$")) {
                            classesByArea.merge(area(this.name), 1, Integer::sum);
                        }
                    }

                    @Override
                    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                        return record(descriptor, name, "");
                    }

                    @Override
                    public MethodVisitor visitMethod(int access, String method, String descriptor, String signature, String[] exceptions) {
                        String owner = name;
                        return new MethodVisitor(Opcodes.ASM9) {
                            @Override
                            public AnnotationVisitor visitAnnotation(String annotation, boolean visible) {
                                return record(annotation, owner, method);
                            }
                        };
                    }

                    private AnnotationVisitor record(String descriptor, String owner, String member) {
                        if (descriptor.equals(EXPERIMENTAL)) {
                            marked.add(new Marked("Experimental", owner, member, new StringBuilder()));
                        } else if (descriptor.equals(INTERNAL)) {
                            marked.add(new Marked("Internal", owner, member, new StringBuilder()));
                        } else if (DEPRECATED.contains(descriptor)) {
                            var deprecated = new Marked("Deprecated", owner, member, new StringBuilder());
                            marked.add(deprecated);
                            return new AnnotationVisitor(Opcodes.ASM9) {          // Kotlin's message and replaceWith
                                @Override
                                public void visit(String key, Object value) {
                                    if (key.equals("message")) {
                                        deprecated.message().append(value);
                                    }
                                }

                                @Override
                                public AnnotationVisitor visitAnnotation(String key, String nested) {
                                    return new AnnotationVisitor(Opcodes.ASM9) {
                                        @Override
                                        public void visit(String k, Object value) {
                                            if (k.equals("expression") && !String.valueOf(value).isEmpty()) {
                                                deprecated.message().append(" (use ").append(value).append(")");
                                            }
                                        }
                                    };
                                }
                            };
                        }
                        return null;
                    }
                }, ClassReader.SKIP_CODE);
            }
        }
        System.out.println("public top-level Embabel classes by area: " + classesByArea);
        Map<String, Integer> counts = new TreeMap<>();
        marked.forEach(m -> counts.merge(m.status() + (m.member().isEmpty() ? " classes" : " methods"), 1, Integer::sum));
        System.out.println("marked: " + counts);
        for (String status : List.of("Experimental", "Internal")) {
            marked.stream().filter(m -> m.status().equals(status))
                    .map(m -> "  @" + status + " " + m.display()).distinct().sorted().forEach(System.out::println);
        }
        System.out.println("deprecated in agent.api:");
        marked.stream().filter(m -> m.status().equals("Deprecated") && m.owner().startsWith("com.embabel.agent.api."))
                .filter(m -> !m.member().endsWith("$annotations"))
                .map(m -> "  " + m.display() + (m.message().isEmpty() ? "" : ": " + m.message()))
                .distinct().sorted().forEach(System.out::println);
        assertThat(classes).isNotEmpty();
    }

    static String area(String className) {
        String[] parts = className.split("\\.");
        if (parts.length > 3 && parts[1].equals("embabel") && parts[2].equals("agent")) {
            return "agent." + parts[3];
        }
        return parts.length > 2 ? parts[1] + "." + parts[2] : className;
    }
}
