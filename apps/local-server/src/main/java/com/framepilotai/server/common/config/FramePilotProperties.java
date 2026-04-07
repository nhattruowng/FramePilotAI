package com.framepilotai.server.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "framepilot")
public class FramePilotProperties {

    private final Storage storage = new Storage();
    private final Ffmpeg ffmpeg = new Ffmpeg();
    private final Runtime runtime = new Runtime();
    private final Render render = new Render();

    public Storage getStorage() {
        return storage;
    }

    public Ffmpeg getFfmpeg() {
        return ffmpeg;
    }

    public Runtime getRuntime() {
        return runtime;
    }

    public Render getRender() {
        return render;
    }

    public static class Storage {
        private String root = "../../storage";
        private String projectsDir = "projects";
        private String cacheDir = "cache";
        private String exportsDir = "exports/generated";
        private String demoAssetsDir = "projects/demo-assets";

        public String getRoot() {
            return root;
        }

        public void setRoot(String root) {
            this.root = root;
        }

        public String getProjectsDir() {
            return projectsDir;
        }

        public void setProjectsDir(String projectsDir) {
            this.projectsDir = projectsDir;
        }

        public String getCacheDir() {
            return cacheDir;
        }

        public void setCacheDir(String cacheDir) {
            this.cacheDir = cacheDir;
        }

        public String getExportsDir() {
            return exportsDir;
        }

        public void setExportsDir(String exportsDir) {
            this.exportsDir = exportsDir;
        }

        public String getDemoAssetsDir() {
            return demoAssetsDir;
        }

        public void setDemoAssetsDir(String demoAssetsDir) {
            this.demoAssetsDir = demoAssetsDir;
        }
    }

    public static class Ffmpeg {
        private String executable = "";

        public String getExecutable() {
            return executable;
        }

        public void setExecutable(String executable) {
            this.executable = executable;
        }
    }

    public static class Runtime {
        private int memoryPressureThresholdPercent = 82;
        private int cpuPressureThresholdPercent = 90;
        private int vramPressureThresholdPercent = 85;

        public int getMemoryPressureThresholdPercent() {
            return memoryPressureThresholdPercent;
        }

        public void setMemoryPressureThresholdPercent(int memoryPressureThresholdPercent) {
            this.memoryPressureThresholdPercent = memoryPressureThresholdPercent;
        }

        public int getCpuPressureThresholdPercent() {
            return cpuPressureThresholdPercent;
        }

        public void setCpuPressureThresholdPercent(int cpuPressureThresholdPercent) {
            this.cpuPressureThresholdPercent = cpuPressureThresholdPercent;
        }

        public int getVramPressureThresholdPercent() {
            return vramPressureThresholdPercent;
        }

        public void setVramPressureThresholdPercent(int vramPressureThresholdPercent) {
            this.vramPressureThresholdPercent = vramPressureThresholdPercent;
        }
    }

    public static class Render {
        private int maxRetryPerShot = 2;
        private int queueConcurrency = 1;
        private int pausePollMillis = 400;

        public int getMaxRetryPerShot() {
            return maxRetryPerShot;
        }

        public void setMaxRetryPerShot(int maxRetryPerShot) {
            this.maxRetryPerShot = maxRetryPerShot;
        }

        public int getQueueConcurrency() {
            return queueConcurrency;
        }

        public void setQueueConcurrency(int queueConcurrency) {
            this.queueConcurrency = queueConcurrency;
        }

        public int getPausePollMillis() {
            return pausePollMillis;
        }

        public void setPausePollMillis(int pausePollMillis) {
            this.pausePollMillis = pausePollMillis;
        }
    }
}
