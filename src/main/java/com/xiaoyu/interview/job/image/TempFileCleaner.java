package com.xiaoyu.interview.job.image;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class TempFileCleaner {

    private static final String TEMP_DIR = "src/main/resources/resumeImage";

    // 每天凌晨 3 点清理
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanTempFiles() {
        File dir = new File(TEMP_DIR);
        if (dir.exists() && dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                if (file.isFile() && file.getName().endsWith(".png")) {
                    file.delete();
                }
            }
        }
    }
}
