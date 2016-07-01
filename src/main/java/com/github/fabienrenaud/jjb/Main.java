package com.github.fabienrenaud.jjb;

import io.airlift.airline.*;
import io.airlift.airline.Cli.CliBuilder;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.*;

/**
 *
 * @author Fabien Renaud
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws RunnerException {
        CliBuilder<Runnable> builder = Cli.<Runnable>builder("bench")
            .withDescription("Benchmark JSON libraries")
            .withDefaultCommand(Help.class)
            .withCommands(Help.class, SerializationCommand.class, DeserializationCommand.class);

        Cli<Runnable> gitParser = builder.build();
        gitParser.parse(args).run();
    }

    public static abstract class AbstractCommand implements Runnable {

        private static final Set<String> LIBRARIES = new HashSet<>(Arrays.asList("jackson", "jackson_afterburner", "genson", "fastjson", "gson", "orgjson", "jsonp", "jsonio"));

        @Option(type = OptionType.GLOBAL, name = "--libraries", description = "Libraries to test (csv). Defaults to all. Available: jackson, jackson_afterburner, genson, fastjson, gson, orgjson, jsonp, jsonio")
        public String libraries;
        @Option(type = OptionType.GLOBAL, name = "--types", description = "Type of APIs to benchmark (csv). Available: stream, databind")
        public String types;

        @Override
        public void run() {
            ChainedOptionsBuilder b = new OptionsBuilder()
                .forks(1)
                .warmupIterations(5)
                .measurementIterations(5);
//                .addProfiler(StackProfiler.class);

            for (String i : includes()) {
                b.include(i);
            }

            Options opt = b.build();
            try {
                new Runner(opt).run();
            } catch (RunnerException ex) {
                throw new RuntimeException(ex);
            }
        }

        protected abstract String mode();

        private List<String> includes() {
            List<String> l = new ArrayList<>();
            switch (mode()) {
                case "ser":
                    for (String p : prefixes()) {
                        for (String s : suffixes()) {
                            l.add(".*" + p + "Serialization" + s);
                        }
                    }
                    break;
                case "deser":
                    for (String p : prefixes()) {
                        for (String s : suffixes()) {
                            l.add(".*" + p + "Deserialization" + s);
                        }
                    }
                    break;
                default:
                    throw new IllegalStateException("Invalid mode: " + mode());
            }
            return l;
        }

        private List<String> prefixes() {
            List<String> l = new ArrayList<>();
            if (types == null) {
                types = "stream,databind";
            }
            for (String t : types.split(",")) {
                switch (t) {
                    case "stream":
                        l.add("Stream");
                        break;
                    case "databind":
                        l.add("Databind");
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid value: " + t);
                }
            }
            return l;
        }

        private List<String> suffixes() {
            if (libraries == null) {
                return Arrays.asList(".*");
            }

            List<String> list = new ArrayList<>();
            for (String l : libraries.split(",")) {
                if (!LIBRARIES.contains(l)) {
                    throw new IllegalArgumentException("Invalid value: " + l);
                }
                list.add("." + l + "*");
            }
            return list;
        }
    }

    @Command(name = "deser", description = "Runs the deserialization benchmarks")
    public static final class DeserializationCommand extends AbstractCommand {

        @Override
        protected String mode() {
            return "deser";
        }

    }

    @Command(name = "ser", description = "Runs the serialization benchmarks")
    public static final class SerializationCommand extends AbstractCommand {

        @Override
        protected String mode() {
            return "ser";
        }

    }
}