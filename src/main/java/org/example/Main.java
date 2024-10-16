package org.example;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Main {
    //todo рефакторинг
    static final Path PATH_TO_FIELDSETS = Paths.get("/home/dinodafor/IdeaProjects/cynteka_main/app/helpers/core/fieldsets");
    static final Path PATH_TO_WRITE_FILE = Paths.get("result.txt");
    static final String PREFIX = "protected static final String ";
    static final String EQUALS = " = ";
    static final String POSTFIX = ";";
    static final int MAX_WHITE_SPACES = 112;
    //TODO ИЗБАВИТЬСЯ?
    static String result = "";
    static TreeSet<String> variables = new TreeSet<>();
    static TreeSet<String> resultStringsTS = new TreeSet<>();
    static Pattern pattern = Pattern.compile("(?<=\").*?(?=\")", Pattern.CASE_INSENSITIVE);
    //\".*\"

    public static void main(String[] args) {

        listFilesJavaNIO(PATH_TO_FIELDSETS);
        replacementVariables(PATH_TO_FIELDSETS);

    }



    static void listFilesJavaNIO(Path dir) {
        try (Stream<Path> stream = Files.walk(dir)) {


            stream.filter(Files::isRegularFile)
                    .forEach(path -> {

                        //пропускаем определенные файлы
                        if (path.endsWith("FieldSet.java")
                                || path.endsWith("EdiStateRegistryFieldSet.java")
                                || path.endsWith("DIFieldSet.java")) {
                            return;
                        }

                        try (BufferedReader reader = Files.newBufferedReader(path)) {
                            String line;
                            //resultStringsTS.add("\033[0;36m" + path + "\033[0m");
                            while ((line = reader.readLine()) != null) {

                                if (line.contains("addAll") && line.contains("\"")) {
                                    Matcher matcher = pattern.matcher(line);
                                    if (matcher.find()) {

                                        String string = line.substring(matcher.start(), matcher.end());
                                        String varString = createVarString(string);
//                                        System.out.println("\u001B[32m" + varString + "\033[0m");
                                        variables.add(varString);
                                        result = createResultStringFromVarString(varString);
//                                        System.out.println("result \u001B[32m" + result + "\033[0m");
                                        resultStringsTS.add(result);
                                    }
                                }
                                if (line.contains("\"")
                                        && !line.contains("addAll")
                                        && !line.contains("/")
                                        && !line.contains("getFields")
                                        && !line.contains("Builder")
                                ) {
                                    //todo вот тут нужен цикл по распарсу, разрезаем по запятой и проходим по каждой createvar()
                                    String[] strings = line.split(",");
                                    for (String s : strings) {
                                        String varString = createVarString(s);
                                        if (!variables.contains(varString)) {
                                            variables.add(varString);
                                            result = createResultString(varString, s);
                                            resultStringsTS.add(result);
                                        }
                                    }

                                }
                            }

                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });

            try (BufferedWriter writer = Files.newBufferedWriter(PATH_TO_WRITE_FILE)) {
                resultStringsTS.forEach(strVar -> {
                    try {
                        writer.write(strVar + "\n");
                        System.out.println(strVar);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void replacementVariables(Path dir) {


        try (Stream<Path> stream = Files.walk(dir)) {

            stream.filter(Files::isRegularFile)
                    .forEach(path -> {
                        //пропускаем определенные файлы
                        if (path.endsWith("FieldSet.java")
                                || path.endsWith("EdiStateRegistryFieldSet.java")
                                || path.endsWith("DIFieldSet.java")) {
                            return;
                        }
                        //resultStringsTS.add("\u001B[35m" + path);

                        try {

                            //(\"(\w+(\.)\w+)+\")
                            //\"?\w+(\.)\w+\"?
                            //\"(\w+(\.)\w+)+\"
                            List<String> lines = Files.readAllLines(path);

                            for (int i = 0; i < lines.size(); i++) {
                                String line = lines.get(i);
                                if (line.contains("addAll") && line.contains("\"")) {
                                    Matcher matcher = pattern.matcher(line);
                                    if (matcher.find()) {
                                        String string = line.substring(matcher.start(), matcher.end());
                                        String varString = createVarString(string);
                                        if (variables.contains(varString)) {
                                            String replace = line.substring(matcher.start() - 1, matcher.end() + 1);
                                            lines.set(i, line.replace(replace, varString));

                                        }
                                    }

                                }
                                if (line.contains("\"")
                                        && !line.contains("addAll")
                                        && !line.contains("/")) {

                                    Matcher matcher = pattern.matcher(line);
                                    if (matcher.find()) {
                                        //todo тут обрабатывать цикл по вставке статиков вместо строк
                                        String string = line.substring(matcher.start(), matcher.end());
                                        String varString = createVarString(string);
                                        if (variables.contains(varString)) {
                                            String replace = line.substring(matcher.start() - 1, matcher.end() + 1);
                                            lines.set(i, line.replace(replace, varString));
                                        }
                                    }
                                }
                            }
                            Files.write(path, lines);

                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                    });

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
    static String createWhiteSpace(int max, int current) {
        StringBuilder sb = new StringBuilder();
        int nado = max - current;
        for (int i = 0; i < nado; i++) {
            sb.append(" ");
        }
        return sb.toString();
    }
    static String createVarString(String line) {
        line = line
                .replace("\"", "")
                .replace(".add(", "")
                .replace(".addAll(", "")
                .replace(")", "")
                .replace(".", "_")
                .replace(",", "")
                .replace("*", "ASTERISK").trim();

        return line;
    }

    static String createResultStringFromVarString(String varString) {
        String varStringWithQuotationMarks = "\"" + varString + "\"";

        result = PREFIX
                + varString
                + EQUALS;

        String ws = createWhiteSpace(MAX_WHITE_SPACES, result.length());

        result += ws
                + varStringWithQuotationMarks.trim()
                + POSTFIX;


        return result;
    }

    static String createResultString(String varString, String line) {

        result = PREFIX
                + varString
                + EQUALS;

        String ws = createWhiteSpace(MAX_WHITE_SPACES, result.length());

        result += ws +
                line
                        .replace(",", "")
                        .replace(".add(", "")
                        .replace(")", "").trim()
                + POSTFIX;


        return result;
    }


}