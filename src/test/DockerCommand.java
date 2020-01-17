package test;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class DockerCommand {

    private static final boolean CRASH_ENABLED = false;
    private static final int ACTION_ON_OUT_OF_MEMORY = 1024;

    public static void main(String[] args) throws IOException, InterruptedException {
        int round = 100;
        /**
         * Checkpoint for the image oversky710/xdn-demo-app
         *
         */

        String name = "3ffc7fe7f53a";
        List<String> command = new ArrayList<>();

        command.add("docker");
        command.add("");
        command.add("checkpoint");
        command.add("ls");
        command.add(name);

        ProcessBuilder builder = new ProcessBuilder(command);
        // builder.directory(new File(System.getProperty("user.dir")));

        builder.redirectError(ProcessBuilder.Redirect.INHERIT);
        builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        //builder.redirectInput(Redirect.INHERIT);

        Process process = builder.start();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {

            String line;

            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }

        int ret = process.waitFor();
        System.out.println("Done:"+ret);
    }
}
