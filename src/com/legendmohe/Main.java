package com.legendmohe;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Main {

    private int threshold;

    public Main(int threshold) {
        this.threshold = threshold;
    }

    public static void main(String[] args) {
//        args = new String[]{"F:\\logs"};

        List<String> argsList = new ArrayList<String>();
        List<Option> optsList = new ArrayList<Option>();
        List<String> doubleOptsList = new ArrayList<String>();

        for (int i = 0; i < args.length; i++) {
            switch (args[i].charAt(0)) {
                case '-':
                    if (args[i].length() < 2)
                        throw new IllegalArgumentException("Not a valid argument: " + args[i]);
                    if (args[i].charAt(1) == '-') {
                        if (args[i].length() < 3)
                            throw new IllegalArgumentException("Not a valid argument: " + args[i]);
                        // --opt
                        doubleOptsList.add(args[i].substring(2, args[i].length()));
                    } else {
                        if (args.length - 1 == i)
                            throw new IllegalArgumentException("Expected arg after: " + args[i]);
                        // -opt
                        optsList.add(new Option(args[i], args[i + 1]));
                        i++;
                    }
                    break;
                default:
                    // arg
                    argsList.add(args[i]);
                    break;
            }
        }

        if (argsList.size() == 0) {
            System.err.println("empty args");
            return;
        }

        int threshold = 90;
        if (optsList.size() > 0) {
            for (Option option : optsList) {
                if (option.opt.equals("-t")) {
                    threshold = Integer.parseInt(option.flag);
                }
            }
        }

        String pathArg = argsList.get(0);
        if (pathArg.length() == 0) {
            System.err.println("invalid path:" + pathArg);
            return;
        }

        new Main(threshold).run(pathArg);
    }

    private void run(String pathArg) {
        File file = new File(pathArg);
        if (!file.exists()) {
            System.err.println("file not found:" + pathArg);
            return;
        }

        if (!file.isDirectory()) {
            System.err.println("file is not Directory:" + pathArg);
            return;
        }

        List<File> result = new ArrayList<>();
        fillFilesRecursively(new File(pathArg), result);
        System.out.println("processing file size:" + result.size());

        List<SimResult> simResults = doSim(result);

        Collections.sort(simResults, new Comparator<SimResult>() {
            @Override
            public int compare(SimResult o1, SimResult o2) {
                if (o1 == o2)
                    return 0;
                return o2.similarity - o1.similarity;
            }
        });

        for (SimResult simResult : simResults) {
            System.out.println(simResult.similarity + "% " + simResult.path1 + " " + simResult.path2);
        }

//        System.out.println("complete!");
    }

    private void fillFilesRecursively(File file, List<File> resultFiles) {
        if (file.isFile()) {
            resultFiles.add(file);
        } else {
            for (File child : file.listFiles()) {
                fillFilesRecursively(child, resultFiles);
            }
        }
    }

    private List<SimResult> doSim(List<File> inputFiles) {
        StringBuilder sb = new StringBuilder();
        for (File file : inputFiles) {
            sb.append(file.getAbsolutePath()).append("\n");
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }

        // result
        List<SimResult> results = new ArrayList<>();

        final String cmd = "vendor" + File.separator + "sim_java.exe -p -T -s -t" + threshold + " -i";
        runCmd(cmd, new RunCmdCallback() {
            @Override
            public void onFetchLine(String line) {
//                System.out.println(line);
                int beginIdx = line.indexOf("consists for");
                if (beginIdx != -1) {
                    String path1 = line.substring(0, beginIdx - 1);
                    int i = line.indexOf("%");
                    String path2 = line.substring(i + 5, line.length() - 9);

                    String simValue = line.substring(path1.length() + 13 + 1, i - 1);
                    int similarity = Integer.valueOf(simValue);

                    results.add(new SimResult(path1, path2, similarity));
                }
            }

            @Override
            public void onOutputStream(OutputStream outputStream) {
                PrintWriter writer = new PrintWriter(outputStream);
                writer.print(sb.toString() + "\n");
                writer.flush();
                writer.close();
            }

            @Override
            public void onComplete(Exception e) {

            }
        });
        return results;
    }

    ///////////////////////////////////////////////////////////////////////////////////

    private void runCmd(final String cmd, final RunCmdCallback callback) {
        try {
            ProcessBuilder builder = new ProcessBuilder(
                    cmd.split("\\s+"));
            builder.redirectErrorStream(true);
            Process p = builder.start();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            OutputStream outputStream = p.getOutputStream();
            if (callback != null) {
                callback.onOutputStream(outputStream);
            }
            while (true) {
                String line = r.readLine();
                if (line == null) {
                    break;
                }
                if (callback != null) {
                    callback.onFetchLine(line);
                }
            }
            if (callback != null) {
                callback.onComplete(null);
            }
        } catch (IOException e) {
            e.printStackTrace();
            if (callback != null) {
                callback.onComplete(e);
            }
        }
    }

    private interface RunCmdCallback {
        void onFetchLine(String line);

        void onOutputStream(OutputStream outputStream);

        void onComplete(Exception e);
    }

    private static class SimResult {
        String path1;
        String path2;
        int similarity;

        public SimResult(String path1, String path2, int similarity) {
            this.path1 = path1;
            this.path2 = path2;
            this.similarity = similarity;
        }
    }

    /**
     * convenient "-flag opt" combination
     */
    private static class Option {
        String flag, opt;

        Option(String flag, String opt) {
            this.flag = flag;
            this.opt = opt;
        }
    }
}
